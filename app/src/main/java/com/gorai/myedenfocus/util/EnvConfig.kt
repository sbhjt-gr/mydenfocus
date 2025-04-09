package com.gorai.myedenfocus.util

import android.content.Context
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.Properties

object EnvConfig {
    private var properties: Properties? = null

    fun init(context: Context) {
        if (properties == null) {
            properties = Properties()
            try {
                val inputStream = context.assets.open(".env")
                BufferedReader(InputStreamReader(inputStream)).use { reader ->
                    properties?.load(reader)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun get(key: String): String? {
        return properties?.getProperty(key)
    }
} 