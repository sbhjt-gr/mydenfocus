package com.gorai.myedenfocus.util

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader

/**
 * Utility class for parsing documents and extracting their content
 */
object DocumentParser {
    
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
                
                // Read text content from document
                val inputStream = context.contentResolver.openInputStream(uri)
                val text = inputStream?.use { stream ->
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
                
                return@withContext text
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
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