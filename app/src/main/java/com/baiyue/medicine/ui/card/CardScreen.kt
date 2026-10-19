package com.baiyue.medicine.ui.card

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.togetherWith
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.baiyue.medicine.R
import com.baiyue.medicine.data.entity.Medication
import com.baiyue.medicine.ui.components.AppCard
import com.baiyue.medicine.ui.components.AppGlyph
import com.baiyue.medicine.ui.components.AppPill
import com.baiyue.medicine.ui.components.AppSegmentedControl
import com.baiyue.medicine.ui.components.EmptyState
import com.baiyue.medicine.ui.components.GlyphIcon
import com.baiyue.medicine.ui.components.HeroDecorations
import com.baiyue.medicine.ui.components.MedicationAvatar
import com.baiyue.medicine.ui.components.ScreenHeader
import com.baiyue.medicine.ui.components.SectionHeader
import com.baiyue.medicine.ui.doseLabel
import com.baiyue.medicine.ui.intakeLabel
import com.baiyue.medicine.ui.localizedDate
import com.baiyue.medicine.ui.usageLabel
import com.baiyue.medicine.ui.theme.CardWhite
import com.baiyue.medicine.ui.theme.CheckGreen
import com.baiyue.medicine.ui.theme.HeroEnd
import com.baiyue.medicine.ui.theme.HeroGlow
import com.baiyue.medicine.ui.theme.HeroStart
import com.baiyue.medicine.ui.theme.MintTint
import com.baiyue.medicine.ui.theme.MintWash
import com.baiyue.medicine.ui.theme.OccasionalOrange
import com.baiyue.medicine.ui.theme.PrimaryGreen
import com.baiyue.medicine.ui.theme.TextDark
import com.baiyue.medicine.ui.theme.TextFaint
import com.baiyue.medicine.ui.theme.TextSub
import com.baiyue.medicine.ui.theme.WarmTint
import java.time.LocalDate

@Composable
fun CardScreen(
    onOpenSettings: () -> Unit = {},
    vm: CardViewModel = viewModel()
) {
    val todayEntries by vm.todayEntries.collectAsState()
    val prescriptions by vm.prescriptions.collectAsState()
    val periodCounts by vm.periodCounts.collectAsState()
    val showImage by vm.showImage.collectAsState()
    var showRx by remember { mutableStateOf(false) }
    var easterEgg by remember { mutableStateOf(false) }
    var summaryTaps by remember { mutableStateOf(0) }

    val totalDoses = todayEntries.sumOf { it.totalCount }
    val today = LocalDate.now()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 14.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item(key = "header") {
            ScreenHeader(
                eyebrow = localizedDate(today, R.string.common_date_header),
                title = stringResource(R.string.card_title),
                subtitle = stringResource(R.string.ui_card_subtitle),
                onOpenSettings = onOpenSettings
            )
        }

        item(key = "segments") {
            AppSegmentedControl(
                options = listOf(
                    stringResource(R.string.card_filter_today),
                    stringResource(R.string.card_filter_prescription)
                ),
                selectedIndex = if (showRx) 1 else 0,
                onSelect = { showRx = it == 1 },
                modifier = Modifier.fillMaxWidth()
            )
        }

        item(key = "hero") {
            SummaryHero(
                showRx = showRx,
                totalDoses = totalDoses,
                medicineCount = if (showRx) prescriptions.size else todayEntries.size,
                week = periodCounts.week,
                month = periodCounts.month,
                date = today,
                onTap = {
                    summaryTaps += 1
                    if (summaryTaps >= 10) {
                        summaryTaps = 0
                        easterEgg = true
                    }
                }
            )
        }

        item(key = "listHeader") {
            SectionHeader(
                title = stringResource(if (showRx) R.string.card_filter_prescription else R.string.card_filter_today),
                trailing = {
                    AppPill(
                        if (showRx) {
                            pluralStringResource(
                                R.plurals.ui_medicines_count,
                                prescriptions.size,
                                prescriptions.size
                            )
                        } else {
                            pluralStringResource(R.plurals.ui_day_records_plural, todayEntries.size, todayEntries.size)
                        },
                        glyph = if (showRx) AppGlyph.Pill else AppGlyph.Chart
                    )
                }
            )
        }

        if (showRx) {
            if (prescriptions.isEmpty()) {
                item(key = "emptyRx") {
                    EmptyState(
                        glyph = AppGlyph.Pill,
                        title = stringResource(R.string.card_empty_prescriptions),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            } else {
                items(prescriptions, key = { "rx-${it.id}" }) { med ->
                    RxEntryCard(med = med, showImage = showImage)
                }
            }
        } else {
            if (todayEntries.isEmpty()) {
                item(key = "emptyToday") {
                    EmptyState(
                        glyph = AppGlyph.Check,
                        title = stringResource(R.string.card_empty_today),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            } else {
                items(todayEntries, key = { "entry-${it.medication.id}" }) { entry ->
                    EntryCard(entry = entry, showImage = showImage)
                }
            }
        }
    }

    if (easterEgg) {
        AlertDialog(
            onDismissRequest = { easterEgg = false },
            shape = RoundedCornerShape(30.dp),
            containerColor = CardWhite,
            text = {
                Column(
                    Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        Modifier
                            .size(58.dp)
                            .clip(CircleShape)
                            .background(MintTint),
                        contentAlignment = Alignment.Center
                    ) {
                        GlyphIcon(AppGlyph.Heart, tint = PrimaryGreen, size = 28.dp)
                    }
                    Spacer(Modifier.height(14.dp))
                    Text(
                        stringResource(R.string.card_easter_egg),
                        color = TextDark,
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        textAlign = TextAlign.Center
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { easterEgg = false }) { Text(stringResource(R.string.action_ok)) }
            }
        )
    }
}

@Composable
private fun SummaryHero(
    showRx: Boolean,
    totalDoses: Int,
    medicineCount: Int,
    week: Int,
    month: Int,
    date: LocalDate,
    onTap: () -> Unit
) {
    Box(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(30.dp))
            .background(Brush.linearGradient(listOf(HeroStart, HeroEnd)))
            .clickable(onClick = onTap)
    ) {
        HeroDecorations(Modifier.matchParentSize(), accent = Color.White)
        Canvas(Modifier.matchParentSize()) {
            drawCircle(
                color = HeroGlow.copy(alpha = 0.18f),
                radius = size.width * 0.3f,
                center = Offset(size.width * 0.9f, size.height * 0.06f)
            )
        }
        Column(Modifier.padding(22.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                GlyphIcon(
                    if (showRx) AppGlyph.Pill else AppGlyph.Sparkle,
                    tint = Color.White.copy(alpha = 0.85f),
                    size = 14.dp
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    if (showRx) stringResource(R.string.card_my_prescription)
                    else stringResource(R.string.card_summary_title, localizedDate(date, R.string.common_date_day)),
                    color = Color.White.copy(alpha = 0.82f),
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                if (!showRx) {
                    AppPill(
                        stringResource(R.string.ui_today),
                        color = Color.White,
                        containerColor = Color.White.copy(alpha = 0.15f),
                        glyph = AppGlyph.Sun
                    )
                }
            }

            Spacer(Modifier.height(18.dp))

            Row(verticalAlignment = Alignment.Bottom) {
                AnimatedContent(
                    targetState = if (showRx) medicineCount else totalDoses,
                    transitionSpec = { fadeIn(tween(180)) togetherWith fadeOut(tween(140)) },
                    label = "heroNumber"
                ) { number ->
                    Text(
                        "$number",
                        color = Color.White,
                        fontSize = 48.sp,
                        fontWeight = FontWeight.Bold,
                        lineHeight = 50.sp
                    )
                }
                Spacer(Modifier.width(7.dp))
                Text(
                    stringResource(if (showRx) R.string.ui_kind_unit else R.string.ui_doses_unit),
                    color = Color.White.copy(alpha = 0.72f),
                    fontSize = 13.sp,
                    modifier = Modifier.padding(bottom = 7.dp)
                )
                Spacer(Modifier.weight(1f))
                if (!showRx) {
                    Text(
                        pluralStringResource(R.plurals.ui_medicines_count, medicineCount, medicineCount),
                        color = Color.White.copy(alpha = 0.72f),
                        fontSize = 11.sp,
                        textAlign = TextAlign.End,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(Color.White.copy(alpha = 0.12f))
                    .padding(vertical = 11.dp)
            ) {
                HeroStat(
                    value = week,
                    label = stringResource(R.string.ui_week_short),
                    modifier = Modifier.weight(1f)
                )
                Box(
                    Modifier
                        .width(1.dp)
                        .height(28.dp)
                        .background(Color.White.copy(alpha = 0.16f))
                )
                HeroStat(
                    value = month,
                    label = stringResource(R.string.ui_month_short),
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun HeroStat(value: Int, label: String, modifier: Modifier = Modifier) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            "$value",
            color = Color.White,
            fontSize = 19.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            label,
            color = Color.White.copy(alpha = 0.66f),
            fontSize = 10.sp
        )
    }
}

@Composable
private fun RxEntryCard(med: Medication, showImage: Boolean) {
    AppCard(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(15.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            MedicationAvatar(
                name = med.name,
                imageUri = med.imageUri,
                showImage = showImage,
                size = 52.dp
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    med.name,
                    color = TextDark,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(5.dp))
                Text(
                    doseLabel(med),
                    color = TextSub,
                    fontSize = 11.5.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(5.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    AppPill(usageLabel(med), glyph = AppGlyph.Leaf)
                    AppPill(intakeLabel(med), glyph = AppGlyph.Pill)
                }
                if (med.note.isNotBlank()) {
                    Spacer(Modifier.height(5.dp))
                    Text(
                        med.note,
                        color = TextFaint,
                        fontSize = 10.5.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
private fun EntryCard(entry: CardEntry, showImage: Boolean) {
    val med = entry.medication
    AppCard(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(15.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            MedicationAvatar(
                name = med.name,
                imageUri = med.imageUri,
                showImage = showImage,
                size = 52.dp
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    med.name,
                    color = TextDark,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "${usageLabel(med)} · ${intakeLabel(med)}",
                    color = TextSub,
                    fontSize = 11.5.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(7.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    if (entry.normalCount > 0) {
                        AppPill(
                            stringResource(R.string.card_normal_count, entry.normalCount),
                            color = PrimaryGreen,
                            containerColor = MintTint
                        )
                    }
                    if (entry.occasionalCount > 0) {
                        AppPill(
                            stringResource(R.string.card_occasional_count, entry.occasionalCount),
                            color = OccasionalOrange,
                            containerColor = WarmTint
                        )
                    }
                }
            }
            Spacer(Modifier.width(10.dp))
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    Modifier
                        .size(54.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(listOf(MintTint, MintWash))
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "${entry.totalCount}",
                        color = PrimaryGreen,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(Modifier.height(3.dp))
                Text(
                    stringResource(R.string.card_unit_times),
                    color = TextFaint,
                    fontSize = 9.5.sp
                )
            }
        }
    }
}
