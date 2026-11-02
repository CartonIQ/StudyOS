package com.cartoniq.studyos

import android.app.Activity
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat

val ACCENT_NAMES = listOf("Gold", "Blue", "Green", "Coral", "Violet")
val ACCENTS = listOf(0xFFD6AD55, 0xFF6C9BEF, 0xFF54B58A, 0xFFE27676, 0xFFB58CE0).map { Color(it) }

@Composable
fun StudyTheme(mode: Int, amoled: Boolean, accent: Int, fontPct: Int, content: @Composable () -> Unit) {
    val dark = when (mode) { 1 -> false; 2 -> true; else -> isSystemInDarkTheme() }
    val a = ACCENTS[accent.coerceIn(0, ACCENTS.lastIndex)]
    val scheme = if (dark) {
        val base = darkColorScheme(
            primary = a, onPrimary = Color(0xFF101827),
            background = Color(0xFF101827), onBackground = Color(0xFFF5F7FA),
            surface = Color(0xFF18243A), onSurface = Color(0xFFF5F7FA),
            surfaceVariant = Color(0xFF202F49), onSurfaceVariant = Color(0xFFB7C1D1),
            secondaryContainer = Color(0xFF2B3B59), onSecondaryContainer = a,
            outline = Color(0xFF2D3F5F), error = Color(0xFFE27676), tertiary = Color(0xFF54B58A)
        )
        if (amoled) base.copy(background = Color.Black, surface = Color(0xFF0D0D0D), surfaceVariant = Color(0xFF1A1A1A),
            secondaryContainer = Color(0xFF222222), outline = Color(0xFF2A2A2A)) else base
    } else lightColorScheme(
        primary = lerp(a, Color.Black, 0.45f), onPrimary = Color.White,
        background = Color(0xFFF6F7FA), onBackground = Color(0xFF202938),
        surface = Color.White, onSurface = Color(0xFF202938),
        surfaceVariant = Color(0xFFEDEFF4), onSurfaceVariant = Color(0xFF667085),
        secondaryContainer = lerp(a, Color.White, 0.75f), onSecondaryContainer = lerp(a, Color.Black, 0.6f),
        outline = Color(0xFFDCE2EA), error = Color(0xFFB3261E), tertiary = Color(0xFF2E7D57)
    )
    val view = LocalView.current
    if (!view.isInEditMode) SideEffect {
        val w = (view.context as Activity).window
        w.statusBarColor = scheme.background.toArgb()
        w.navigationBarColor = scheme.surface.toArgb()
        val c = WindowCompat.getInsetsController(w, view)
        c.isAppearanceLightStatusBars = !dark
        c.isAppearanceLightNavigationBars = !dark
    }
    val d = LocalDensity.current
    CompositionLocalProvider(LocalDensity provides Density(d.density, d.fontScale * fontPct / 100f)) {
        MaterialTheme(
            colorScheme = scheme,
            shapes = Shapes(
                small = RoundedCornerShape(8.dp), medium = RoundedCornerShape(12.dp),
                large = RoundedCornerShape(18.dp), extraLarge = RoundedCornerShape(24.dp)
            ),
            content = content
        )
    }
}

@Composable
fun Card2(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) = Surface(
    modifier.fillMaxWidth(), shape = MaterialTheme.shapes.large,
    color = MaterialTheme.colorScheme.surface,
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
) { Column(Modifier.padding(16.dp), content = content) }

@Composable
fun EmptyState(text: String, modifier: Modifier = Modifier) = Box(modifier.fillMaxWidth().padding(24.dp), Alignment.Center) {
    Text(text, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
}
