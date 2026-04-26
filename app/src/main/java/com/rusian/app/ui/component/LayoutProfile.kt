package com.rusian.app.ui.component

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration

@Composable
fun rememberIsCompactHeight(): Boolean {
    return LocalConfiguration.current.screenHeightDp <= 760
}

@Composable
fun rememberIsDenseHeight(): Boolean {
    return LocalConfiguration.current.screenHeightDp <= 700
}
