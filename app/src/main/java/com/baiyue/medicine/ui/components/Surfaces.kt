package com.baiyue.medicine.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.baiyue.medicine.R
import com.baiyue.medicine.ui.MedicationImage
import com.baiyue.medicine.ui.theme.BorderSoft
import com.baiyue.medicine.ui.theme.CardWhite
import com.baiyue.medicine.ui.theme.MintTint
import com.baiyue.medicine.ui.theme.MintWash
import com.baiyue.medicine.ui.theme.PrimaryGreen
import com.baiyue.medicine.ui.theme.TextDark
import com.baiyue.medicine.ui.theme.TextSub

val AppCardShape = RoundedCornerShape(24.dp)
val AppSmallShape = RoundedCornerShape(18.dp)

/** Soft botanical wash with two barely-there glows behind every screen. */
@Composable
fun AppBackdrop(modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit) {
    Box(
        modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFFFAFCF8), Color(0xFFF3F8F2), Color(0xFFF6FAF5))
                )
            )
    ) {
        Canvas(Modifier.fillMaxSize()) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0x556FC7A4), Color(0x006FC7A4)),
                    center = Offset(size.width * 0.98f, size.height * 0.04f),
                    radius = size.width * 0.78f
                ),
                radius = size.width * 0.78f,
                center = Offset(size.width * 0.98f, size.height * 0.04f)
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0x44F0B98E), Color(0x00F0B98E)),
                    center = Offset(size.width * 0.02f, size.height * 0.82f),
                    radius = size.width * 0.65f
                ),
                radius = size.width * 0.65f,
                center = Offset(size.width * 0.02f, size.height * 0.82f)
            )
        }
        content()
    }
}

@Composable
fun ScreenHeader(
    eyebrow: String,
    title: String,
    subtitle: String? = null,
    onOpenSettings: (() -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null
) {
    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                GlyphIcon(AppGlyph.Leaf, tint = PrimaryGreen, size = 13.dp)
                Spacer(Modifier.width(6.dp))
                Text(
                    eyebrow.uppercase(),
                    color = PrimaryGreen,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.1.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(Modifier.height(7.dp))
            Text(
                title,
                style = MaterialTheme.typography.displaySmall,
                color = TextDark,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            if (!subtitle.isNullOrBlank()) {
                Spacer(Modifier.height(7.dp))
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSub,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        if (trailing != null) {
            Spacer(Modifier.width(10.dp))
            trailing()
        } else if (onOpenSettings != null) {
            Spacer(Modifier.width(10.dp))
            SettingsButton(onClick = onOpenSettings)
        }
    }
}

@Composable
fun SettingsButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    val description = stringResource(R.string.ui_settings)
    Box(
        modifier
            .size(46.dp)
            .shadow(8.dp, CircleShape, ambientColor = Color(0x221D6A55), spotColor = Color(0x221D6A55))
            .clip(CircleShape)
            .background(CardWhite.copy(alpha = 0.88f))
            .border(1.dp, BorderSoft, CircleShape)
            .clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            Icons.Filled.Settings,
            contentDescription = description,
            tint = TextDark,
            modifier = Modifier.size(21.dp)
        )
    }
}

@Composable
fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    actionText: String? = null,
    onAction: (() -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null
) {
    Row(
        modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            title,
            style = MaterialTheme.typography.titleMedium,
            color = TextDark,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.weight(1f))
        if (trailing != null) {
            trailing()
        } else if (actionText != null && onAction != null) {
            Text(
                actionText,
                color = PrimaryGreen,
                fontSize = 12.5.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .clickable(onClick = onAction)
                    .padding(horizontal = 10.dp, vertical = 5.dp)
            )
        }
    }
}

@Composable
fun AppCard(
    modifier: Modifier = Modifier,
    shape: RoundedCornerShape = AppCardShape,
    containerColor: Color = CardWhite,
    contentPadding: PaddingValues = PaddingValues(18.dp),
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val clickable = if (onClick != null) {
        Modifier.clickable(onClick = onClick)
    } else {
        Modifier
    }
    Card(
        modifier = modifier.then(clickable),
        shape = shape,
        colors = CardDefaults.cardColors(containerColor = containerColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        border = BorderStroke(1.dp, BorderSoft)
    ) {
        Column(Modifier.padding(contentPadding), content = content)
    }
}

@Composable
fun ClickableSurface(
    modifier: Modifier = Modifier,
    shape: RoundedCornerShape = AppSmallShape,
    color: Color = CardWhite,
    borderColor: Color = BorderSoft,
    onClick: () -> Unit,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier
            .clip(shape)
            .background(color)
            .border(1.dp, borderColor, shape)
            .clickable(onClick = onClick),
        content = content
    )
}

@Composable
fun AppPill(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = PrimaryGreen,
    containerColor: Color = MintTint,
    glyph: AppGlyph? = null
) {
    Row(
        modifier
            .clip(RoundedCornerShape(50))
            .background(containerColor)
            .padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (glyph != null) {
            GlyphIcon(glyph, tint = color, size = 12.dp)
            Spacer(Modifier.width(4.dp))
        }
        Text(
            text,
            color = color,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1
        )
    }
}

@Composable
fun MedicationAvatar(
    name: String,
    imageUri: String,
    showImage: Boolean,
    modifier: Modifier = Modifier,
    size: Dp = 54.dp
) {
    if (showImage && imageUri.isNotBlank()) {
        MedicationImage(
            uri = imageUri,
            size = size,
            modifier = modifier
        )
    } else {
        Box(
            modifier
                .size(size)
                .clip(RoundedCornerShape(size * 0.32f))
                .background(
                    Brush.linearGradient(
                        listOf(MintTint, Color(0xFFD4EBDD))
                    )
                )
                .border(1.dp, Color(0x66FFFFFF), RoundedCornerShape(size * 0.32f)),
            contentAlignment = Alignment.Center
        ) {
            if (name.isBlank()) {
                GlyphIcon(AppGlyph.Pill, tint = PrimaryGreen, size = size * 0.42f)
            } else {
                Text(
                    name.trim().take(1),
                    color = PrimaryGreen,
                    fontSize = (size.value * 0.36f).sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun EmptyState(
    glyph: AppGlyph,
    title: String,
    body: String? = null,
    modifier: Modifier = Modifier,
    action: (@Composable () -> Unit)? = null
) {
    Column(
        modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 34.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            Modifier
                .size(78.dp)
                .clip(RoundedCornerShape(28.dp))
                .background(
                    Brush.linearGradient(listOf(MintTint, MintWash))
                ),
            contentAlignment = Alignment.Center
        ) {
            GlyphIcon(glyph, tint = PrimaryGreen, size = 32.dp)
        }
        Spacer(Modifier.height(16.dp))
        Text(
            title,
            style = MaterialTheme.typography.titleMedium,
            color = TextDark,
            textAlign = TextAlign.Center
        )
        if (!body.isNullOrBlank()) {
            Spacer(Modifier.height(6.dp))
            Text(
                body,
                style = MaterialTheme.typography.bodySmall,
                color = TextSub,
                textAlign = TextAlign.Center
            )
        }
        if (action != null) {
            Spacer(Modifier.height(16.dp))
            action()
        }
    }
}

@Composable
fun AppSegmentedControl(
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier
            .clip(RoundedCornerShape(50))
            .background(Color(0xFFE8F1EA))
            .padding(4.dp)
    ) {
        options.forEachIndexed { index, label ->
            val selected = selectedIndex == index
            val background by animateColorAsState(
                if (selected) CardWhite else Color.Transparent,
                animationSpec = tween(220),
                label = "segmentBackground"
            )
            val foreground by animateColorAsState(
                if (selected) TextDark else TextSub,
                animationSpec = tween(220),
                label = "segmentForeground"
            )
            Box(
                Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(50))
                    .background(background)
                    .clickable { onSelect(index) }
                    .padding(vertical = 9.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    label,
                    color = foreground,
                    fontSize = 12.5.sp,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
fun AppBottomBar(
    labels: List<String>,
    glyphs: List<AppGlyph>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .navigationBarsPadding()
            .padding(horizontal = 14.dp, vertical = 8.dp)
            .shadow(16.dp, RoundedCornerShape(26.dp), ambientColor = Color(0x331D6A55), spotColor = Color(0x331D6A55)),
        shape = RoundedCornerShape(26.dp),
        color = CardWhite.copy(alpha = 0.96f),
        border = BorderStroke(1.dp, BorderSoft)
    ) {
        Row(Modifier.padding(7.dp)) {
            labels.forEachIndexed { index, label ->
                val selected = selectedIndex == index
                val background by animateColorAsState(
                    if (selected) PrimaryGreen else Color.Transparent,
                    animationSpec = tween(240),
                    label = "navBackground"
                )
                val foreground by animateColorAsState(
                    if (selected) Color.White else TextSub,
                    animationSpec = tween(240),
                    label = "navForeground"
                )
                Column(
                    Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(19.dp))
                        .background(background)
                        .semantics {
                            this.selected = selected
                            role = Role.Tab
                        }
                        .clickable { onSelect(index) }
                        .padding(vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    GlyphIcon(glyphs[index], tint = foreground, size = 21.dp)
                    Spacer(Modifier.height(3.dp))
                    Text(
                        label,
                        color = foreground,
                        fontSize = 10.sp,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

/** Faint decorative circles used inside gradient hero cards. */
@Composable
fun HeroDecorations(modifier: Modifier = Modifier, accent: Color = Color.White) {
    Canvas(modifier) {
        drawCircle(accent.copy(alpha = 0.08f), radius = size.width * 0.40f, center = Offset(size.width * 1.02f, -size.height * 0.15f))
        drawCircle(accent.copy(alpha = 0.06f), radius = size.width * 0.26f, center = Offset(size.width * 0.86f, size.height * 0.92f))
        drawCircle(accent.copy(alpha = 0.05f), radius = size.width * 0.16f, center = Offset(size.width * 0.12f, size.height * 1.02f))
    }
}

/** A compact icon-in-a-tinted-square used by settings and section rows. */
@Composable
fun TintedGlyph(
    glyph: AppGlyph,
    modifier: Modifier = Modifier,
    tint: Color = PrimaryGreen,
    container: Color = MintTint,
    size: Dp = 38.dp
) {
    Box(
        modifier
            .size(size)
            .clip(RoundedCornerShape(size * 0.34f))
            .background(container),
        contentAlignment = Alignment.Center
    ) {
        GlyphIcon(glyph, tint = tint, size = size * 0.48f)
    }
}
