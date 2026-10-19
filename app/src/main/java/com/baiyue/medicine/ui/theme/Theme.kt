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
    primaryContainer = MintTint,
    onPrimaryContainer = PrimaryDeep,
    secondary = SecondaryTeal,
    onSecondary = Color.White,
    secondaryContainer = MintWash,
    onSecondaryContainer = PrimaryDeep,
    tertiary = OccasionalOrange,
    onTertiary = Color.White,
    tertiaryContainer = WarmTint,
    onTertiaryContainer = Color(0xFF6E3D16),
    background = BackgroundMint,
    onBackground = TextDark,
    surface = CardWhite,
    onSurface = TextDark,
    surfaceVariant = SurfaceVariantMint,
    onSurfaceVariant = TextSub,
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color(0xFFFBFDFB),
    surfaceContainer = Color(0xFFF2F8F3),
    surfaceContainerHigh = Color(0xFFECF5EF),
    surfaceContainerHighest = Color(0xFFE6F0E9),
    outline = BorderStrong,
    outlineVariant = BorderSoft,
    error = WarningRed,
    onError = Color.White,
    errorContainer = WarningSoft,
    onErrorContainer = Color(0xFF6A241E)
)

private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(30.dp)
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
