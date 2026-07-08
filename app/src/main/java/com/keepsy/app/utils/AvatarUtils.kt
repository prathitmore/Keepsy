package com.keepsy.app.utils

import java.util.Locale

object AvatarUtils {
    fun getInitials(fullName: String?): String {
        if (fullName == null || fullName.isEmpty()) return "U"
        
        val name = fullName.trim()
        if (name.isEmpty()) return "U"
        
        val firstChar = name[0].toString()
        var lastChar = ""
        
        // Find first space and get next char
        val spaceIndex = name.indexOf(' ')
        if (spaceIndex != -1 && spaceIndex + 1 < name.length) {
            lastChar = name[spaceIndex + 1].toString()
        }
        
        return (firstChar + lastChar).uppercase(Locale.ROOT)
    }
}
