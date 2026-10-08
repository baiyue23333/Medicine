package com.baiyue.medicine.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val LightColors = lightColorScheme(
    primary = PrimaryGreen,
    onPrimary = Color.White,
    primaryContainer = CheckGreen,
    onPrimaryContainer = TextDark,
    secondary = SecondaryTeal,
    onSecondary = Color.White,
    background = BackgroundMint,
    onBackground = TextDark,
    surface = CardWhite,
    onSurface = TextDark,
    surfaceVariant = SurfaceVariantMint,
    onSurfaceVariant = TextSub,
    error = WarningRed,
    onError = Color.White
)

private val AppShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun MedicineTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightColors,
        typography = Typography,
        shapes = AppShapes,
        content = content
    )
}