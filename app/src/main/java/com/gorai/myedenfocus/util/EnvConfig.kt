package com.gorai.myedenfocus.util

import android.content.Context
import android.util.Log
import java.io.BufferedReader
import java.io.File
import java.io.FileInputStream
import java.io.InputStreamReader
import java.util.Properties

object EnvConfig {
    private var properties: Properties? = null
    private const val TAG = "EnvConfig"

    fun init(context: Context) {
        if (properties == null) {
            properties = Properties()
            try {
                Log.d(TAG, "Initializing EnvConfig")
                
                // Try to load from assets first
                try {
                    Log.d(TAG, "Attempting to load .env from assets")
                    val inputStream = context.assets.open(".env")
                    BufferedReader(InputStreamReader(inputStream)).use { reader ->
                        properties?.load(reader)
                    }
                    Log.d(TAG, "Successfully loaded .env from assets")
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to load from assets: ${e.message}")
                    
                    // If not found in assets, try to load from root directory
                    Log.d(TAG, "Attempting to load .env from root directory")
                    
                    // Try multiple approaches to find the .env file
                    val potentialPaths = listOf(
                        File(context.applicationInfo.dataDir).parentFile?.parentFile?.parentFile, // Original attempt
                        context.filesDir.parentFile?.parentFile, // Internal storage
                        context.getExternalFilesDir(null)?.parentFile?.parentFile, // External storage app directory
                        File("/sdcard"), // External storage root
                        File("/storage/emulated/0") // Another common path
                    )
                    
                    var loaded = false
                    for (path in potentialPaths) {
                        if (path == null) continue
                        
                        val envFile = File(path, ".env")
                        Log.d(TAG, "Checking for .env at: ${envFile.absolutePath}")
                        
                        if (envFile.exists()) {
                            Log.d(TAG, "Found .env at: ${envFile.absolutePath}")
                            try {
                                FileInputStream(envFile).use { inputStream ->
                                    BufferedReader(InputStreamReader(inputStream)).use { reader ->
                                        properties?.load(reader)
                                    }
                                }
                                loaded = true
                                Log.d(TAG, "Successfully loaded .env from: ${envFile.absolutePath}")
                                break
                            } catch (e: Exception) {
                                Log.e(TAG, "Error loading .env from ${envFile.absolutePath}: ${e.message}")
                            }
                        }
                    }
                    
                    if (!loaded) {
                        // Direct approach - try to load from the codebase root
                        try {
                            val rootEnvFile = File(".env")
                            if (rootEnvFile.exists()) {
                                Log.d(TAG, "Found .env in current directory: ${rootEnvFile.absolutePath}")
                                FileInputStream(rootEnvFile).use { inputStream ->
                                    BufferedReader(InputStreamReader(inputStream)).use { reader ->
                                        properties?.load(reader)
                                    }
                                }
                                loaded = true
                                Log.d(TAG, "Successfully loaded .env from current directory")
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "Error loading .env from current directory: ${e.message}")
                        }
                    }
                    
                    if (!loaded) {
                        Log.e(TAG, "Could not find .env file in any location")
                    }
                }
                
                // Log all loaded properties (without showing actual values)
                properties?.let { props ->
                    Log.d(TAG, "Loaded properties: ${props.stringPropertyNames().joinToString()}")
                    Log.d(TAG, "GEMINI_API_KEY exists: ${props.getProperty("GEMINI_API_KEY") != null}")
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "Error in EnvConfig initialization: ${e.message}")
                e.printStackTrace()
            }
        }
    }

    fun get(key: String): String? {
        val value = properties?.getProperty(key)
        Log.d(TAG, "Getting property '$key': ${if (value != null) "Found" else "Not found"}")
        return value
    }
} 