package com.example.util

import androidx.compose.ui.graphics.Color

object ColorUtils {
    fun parseHexColor(hexString: String?, defaultColor: Color = Color(0xFF1E1F22)): Color {
        if (hexString.isNullOrBlank()) return defaultColor
        return try {
            val cleanHex = hexString.trim().removePrefix("#")
            val colorInt = when (cleanHex.length) {
                6 -> (0xFF000000 or cleanHex.toLong(16)).toInt()
                8 -> cleanHex.toLong(16).toInt()
                3 -> {
                    val r = cleanHex[0].toString().repeat(2)
                    val g = cleanHex[1].toString().repeat(2)
                    val b = cleanHex[2].toString().repeat(2)
                    (0xFF000000 or "$r$g$b".toLong(16)).toInt()
                }
                else -> return defaultColor
            }
            Color(colorInt)
        } catch (_: Exception) {
            defaultColor
        }
    }
}
