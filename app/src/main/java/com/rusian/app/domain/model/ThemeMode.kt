package com.rusian.app.domain.model

enum class ThemeMode {
    SYSTEM,
    LIGHT,
    DARK;

    fun resolveDark(systemDark: Boolean): Boolean {
        return when (this) {
            SYSTEM -> systemDark
            LIGHT -> false
            DARK -> true
        }
    }
}
