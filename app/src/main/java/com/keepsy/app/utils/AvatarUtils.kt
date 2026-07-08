package com.keepsy.app.utils

import java.util.Locale

object AvatarUtils {
    fun getInitials(fullName: String?): String {
        if (fullName == null || fullName.isEmpty()) return "U"
        
        val trimmed = fullName.trim()
        if (trimmed.isEmpty()) return "U"
        
        val parts = trimmed.split(" ")
        val filtered = parts.filter { it.isNotEmpty() }
        
        return when {
            filtered.size >= 2 -> {
                val first = filtered[0].substring(0, 1).uppercase(Locale.getDefault())
                val last = filtered[filtered.size - 1].substring(0, 1).uppercase(Locale.getDefault())
                "$first$last"
            }
            filtered.size == 1 -> {
                val name = filtered[0]
                if (name.length >= 2) {
                    name.substring(0, 2).uppercase(Locale.getDefault())
                } else {
                    name.uppercase(Locale.getDefault())
                }
            }
            else -> "U"
        }
    }
}
