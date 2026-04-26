package com.rusian.app.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color

@Immutable
data class ExtendedColors(
    val card: Color,
    val border: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val success: Color,
    val warning: Color,
    val info: Color,
    val danger: Color,
)

val LocalExtendedColors = compositionLocalOf {
    ExtendedColors(
        card = CardLight,
        border = BorderLight,
        textPrimary = TextPrimaryLight,
        textSecondary = TextSecondaryLight,
        success = SuccessLight,
        warning = WarningLight,
        info = InfoLight,
        danger = ErrorLight,
    )
}

object AppTheme {
    val extendedColors: ExtendedColors
        @Composable
        @ReadOnlyComposable
        get() = LocalExtendedColors.current
}
