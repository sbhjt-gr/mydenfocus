package com.gorai.myedenfocus.util

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.text.PDFTextStripper
import org.apache.poi.xwpf.extractor.XWPFWordExtractor
import org.apache.poi.xwpf.usermodel.XWPFDocument
import org.apache.poi.hwpf.HWPFDocument
import org.apache.poi.hwpf.extractor.WordExtractor
import java.io.BufferedReader
import java.io.InputStreamReader

/**
 * Utility class for parsing documents and extracting their content
 */
object DocumentParser {
    private const val TAG = "DocumentParser"
    
    /**
     * Extract text content from a document URI
     * 
     * @param context The application context
     * @param uri The URI of the document to parse
     * @return The text content of the document or null if extraction failed
     */
    suspend fun extractTextFromDocument(context: Context, uri: Uri): String? {
        return withContext(Dispatchers.IO) {
            try {
                // Get document name for logging
                val documentName = getDocumentName(context, uri)
                Log.d(TAG, "Extracting text from document: $documentName")
                
                // Get file extension from name and MIME type
                val extension = documentName.substringAfterLast('.', "").lowercase()
                val mimeType = context.contentResolver.getType(uri)
                Log.d(TAG, "File extension: $extension, MIME type: $mimeType")
                
                // Open input stream
                val inputStream = context.contentResolver.openInputStream(uri)
                
                // Determine file type based on extension and MIME type
                val fileType = when {
                    extension == "pdf" || mimeType?.contains("pdf") == true -> "pdf"
                    extension == "docx" || mimeType?.contains("officedocument.wordprocessingml") == true -> "docx"
                    extension == "doc" || mimeType?.contains("msword") == true -> "doc"
                    extension == "txt" || mimeType?.contains("text/plain") == true -> "txt"
                    else -> "unknown"
                }
                
                Log.d(TAG, "Determined file type: $fileType")
                
                // Process based on file type
                val text = when (fileType) {
                    "pdf" -> {
                        Log.d(TAG, "Processing PDF file")
                        inputStream?.use { stream ->
                            val document = PDDocument.load(stream)
                            val stripper = PDFTextStripper()
                            val text = stripper.getText(document)
                            document.close()
                            text
                        }
                    }
                    "docx" -> {
                        Log.d(TAG, "Processing DOCX file")
                        inputStream?.use { stream ->
                            val document = XWPFDocument(stream)
                            val extractor = XWPFWordExtractor(document)
                            val text = extractor.text
                            extractor.close()
                            document.close()
                            text
                        }
                    }
                    "doc" -> {
                        Log.d(TAG, "Processing DOC file")
                        inputStream?.use { stream ->
                            val document = HWPFDocument(stream)
                            val extractor = WordExtractor(document)
                            val text = extractor.text
                            extractor.close()
                            document.close()
                            text
                        }
                    }
                    "txt" -> {
                        Log.d(TAG, "Processing TXT file")
                        inputStream?.use { stream ->
                            BufferedReader(InputStreamReader(stream)).use { reader ->
                                val stringBuilder = StringBuilder()
                                var line: String?
                                while (reader.readLine().also { line = it } != null) {
                                    stringBuilder.append(line)
                                    stringBuilder.append("\n")
                                }
                                stringBuilder.toString()
                            }
                        }
                    }
                    else -> {
                        // Try to determine file type from content or process as text
                        Log.d(TAG, "Unknown file type, attempting to extract as text")
                        extractTextFromUnknownFormat(inputStream)
                    }
                }
                
                if (text.isNullOrBlank()) {
                    Log.e(TAG, "Failed to extract text from document: $documentName")
                } else {
                    Log.d(TAG, "Successfully extracted ${text.length} characters from $documentName")
                }
                
                return@withContext text
            } catch (e: Exception) {
                Log.e(TAG, "Error extracting text from document", e)
                e.printStackTrace()
                null
            }
        }
    }
    
    /**
     * Attempt to extract text from an unknown file format by trying different parsers
     */
    private fun extractTextFromUnknownFormat(inputStream: java.io.InputStream?): String? {
        return try {
            // Try as PDF first
            inputStream?.let { stream ->
                try {
                    val document = PDDocument.load(stream)
                    val stripper = PDFTextStripper()
                    val text = stripper.getText(document)
                    document.close()
                    return text
                } catch (e: Exception) {
                    // Reset stream and try as DOCX
                    stream.reset()
                    try {
                        val document = XWPFDocument(stream)
                        val extractor = XWPFWordExtractor(document)
                        val text = extractor.text
                        extractor.close()
                        document.close()
                        return text
                    } catch (e: Exception) {
                        // Reset stream and try as DOC
                        stream.reset()
                        try {
                            val document = HWPFDocument(stream)
                            val extractor = WordExtractor(document)
                            val text = extractor.text
                            extractor.close()
                            document.close()
                            return text
                        } catch (e: Exception) {
                            // Finally try as plain text
                            stream.reset()
                            BufferedReader(InputStreamReader(stream)).use { reader ->
                                val stringBuilder = StringBuilder()
                                var line: String?
                                while (reader.readLine().also { line = it } != null) {
                                    stringBuilder.append(line)
                                    stringBuilder.append("\n")
                                }
                                return stringBuilder.toString()
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to extract text from unknown format", e)
            null
        }
    }
    
    /**
     * Get the name of a document from its URI
     * 
     * @param context The application context
     * @param uri The URI of the document
     * @return The name of the document or "Unknown Document" if not available
     */
    private fun getDocumentName(context: Context, uri: Uri): String {
        var documentName = "Unknown Document"
        
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (nameIndex != -1) {
                    documentName = cursor.getString(nameIndex)
                }
            }
        }
        
        return documentName
    }
} 