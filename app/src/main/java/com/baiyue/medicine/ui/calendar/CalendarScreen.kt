package com.baiyue.medicine.ui.calendar

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
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
import com.baiyue.medicine.data.entity.DoseRecord
import com.baiyue.medicine.model.RecordCategory
import com.baiyue.medicine.model.RecordFlag
import com.baiyue.medicine.ui.components.AppCard
import com.baiyue.medicine.ui.components.AppGlyph
import com.baiyue.medicine.ui.components.AppPill
import com.baiyue.medicine.ui.components.EmptyState
import com.baiyue.medicine.ui.components.GlyphIcon
import com.baiyue.medicine.ui.components.ScreenHeader
import com.baiyue.medicine.ui.components.TintedGlyph
import com.baiyue.medicine.ui.formatNum
import com.baiyue.medicine.ui.localizedDate
import com.baiyue.medicine.ui.theme.BorderSoft
import com.baiyue.medicine.ui.theme.CheckGreen
import com.baiyue.medicine.ui.theme.MintTint
import com.baiyue.medicine.ui.theme.MintWash
import com.baiyue.medicine.ui.theme.OccasionalOrange
import com.baiyue.medicine.ui.theme.PrimaryGreen
import com.baiyue.medicine.ui.theme.TextDark
import com.baiyue.medicine.ui.theme.TextFaint
import com.baiyue.medicine.ui.theme.TextSub
import com.baiyue.medicine.ui.theme.WarningRed
import com.baiyue.medicine.ui.theme.WarningSoft
import com.baiyue.medicine.ui.theme.WarmTint
import java.time.LocalDate
import java.time.YearMonth

@Composable
fun CalendarScreen(
    onOpenSettings: () -> Unit = {},
    vm: CalendarViewModel = viewModel()
) {
    val flagsMap by vm.flags.collectAsState()
    val nameMap by vm.medNameMap.collectAsState()

    var yearMonth by remember { mutableStateOf(YearMonth.now()) }
    var selected by remember { mutableStateOf(LocalDate.now()) }
    var recordToDelete by remember { mutableStateOf<DoseRecord?>(null) }

    val records by remember(selected) { vm.recordsFor(selected.toString()) }
        .collectAsState(initial = emptyList())

    val monthPrefix = yearMonth.toString()
    val daysWithRecords = flagsMap.count { (key, flags) ->
        key.startsWith(monthPrefix) && (flags.medication || flags.occasional)
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 14.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item(key = "header") {
            ScreenHeader(
                eyebrow = localizedDate(LocalDate.now(), R.string.common_date_header),
                title = stringResource(R.string.calendar_title),
                subtitle = stringResource(R.string.ui_calendar_subtitle),
                onOpenSettings = onOpenSettings
            )
        }

        item(key = "month") {
            MonthCard(
                yearMonth = yearMonth,
                selected = selected,
                flagsMap = flagsMap,
                daysWithRecords = daysWithRecords,
                onPrevious = {
                    val next = yearMonth.minusMonths(1)
                    yearMonth = next
                    selected = if (next == YearMonth.now()) LocalDate.now() else next.atDay(1)
                },
                onNext = {
                    val next = yearMonth.plusMonths(1)
                    yearMonth = next
                    selected = if (next == YearMonth.now()) LocalDate.now() else next.atDay(1)
                },
                onSelect = { selected = it }
            )
        }

        item(key = "legend") {
            LegendRow()
        }

        item(key = "detail") {
            AnimatedContent(
                targetState = selected,
                transitionSpec = { fadeIn(tween(220)) togetherWith fadeOut(tween(150)) },
                label = "dayDetail"
            ) { date ->
                DayDetail(
                    date = date,
                    records = if (date == selected) records.filter { it.category != RecordCategory.SKIPPED }
                    else emptyList(),
                    nameMap = nameMap,
                    flags = flagsMap[date.toString()],
                    onDelete = { recordToDelete = it }
                )
            }
        }
    }

    recordToDelete?.let { record ->
        AlertDialog(
            onDismissRequest = { recordToDelete = null },
            shape = RoundedCornerShape(30.dp),
            containerColor = Color.White,
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TintedGlyph(AppGlyph.Undo, tint = WarningRed, container = WarningSoft, size = 38.dp)
                    Spacer(Modifier.width(10.dp))
                    Text(
                        stringResource(R.string.calendar_undo_title),
                        fontWeight = FontWeight.Bold,
                        color = TextDark,
                        fontSize = 19.sp
                    )
                }
            },
            text = { Text(stringResource(R.string.calendar_undo_message), color = TextSub) },
            confirmButton = {
                TextButton(onClick = {
                    vm.deleteRecord(record)
                    recordToDelete = null
                }) {
                    Text(
                        stringResource(R.string.calendar_undo_confirm),
                        color = WarningRed,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { recordToDelete = null }) { Text(stringResource(R.string.action_cancel)) }
            }
        )
    }
}

@Composable
private fun MonthCard(
    yearMonth: YearMonth,
    selected: LocalDate,
    flagsMap: Map<String, DayFlags>,
    daysWithRecords: Int,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onSelect: (LocalDate) -> Unit
) {
    AppCard(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 15.dp, vertical = 16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            MonthNavButton(AppGlyph.ChevronLeft, onPrevious)
            AnimatedContent(
                targetState = yearMonth,
                transitionSpec = {
                    if (targetState > initialState) {
                        (slideInHorizontally { it / 3 } + fadeIn()) togetherWith
                            (slideOutHorizontally { -it / 3 } + fadeOut())
                    } else {
                        (slideInHorizontally { -it / 3 } + fadeIn()) togetherWith
                            (slideOutHorizontally { it / 3 } + fadeOut())
                    }
                },
                modifier = Modifier.weight(1f),
                label = "monthTitle"
            ) { ym ->
                Text(
                    localizedDate(ym.atDay(1), R.string.common_month_header),
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                    color = TextDark,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            }
            MonthNavButton(AppGlyph.ChevronRight, onNext)
        }

        Spacer(Modifier.height(14.dp))

        val weekdays = stringResource(R.string.common_weekdays).split(",")
        Row(Modifier.fillMaxWidth()) {
            weekdays.forEachIndexed { index, label ->
                Text(
                    label,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    color = if (index == 0 || index == 6) TextFaint else TextSub,
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        Spacer(Modifier.height(5.dp))

        val firstDay = yearMonth.atDay(1)
        val blanks = firstDay.dayOfWeek.value % 7
        val cells: List<Int?> = List(blanks) { null } + (1..yearMonth.lengthOfMonth()).map { it }
        cells.chunked(7).forEach { week ->
            Row(Modifier.fillMaxWidth()) {
                week.forEach { day ->
                    DayCell(
                        day = day,
                        yearMonth = yearMonth,
                        flagsMap = flagsMap,
                        selected = selected,
                        onSelect = onSelect,
                        modifier = Modifier.weight(1f)
                    )
                }
                repeat(7 - week.size) { Spacer(Modifier.weight(1f)) }
            }
        }

        Spacer(Modifier.height(12.dp))
        Row(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(MintWash)
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            GlyphIcon(AppGlyph.Chart, tint = PrimaryGreen, size = 14.dp)
            Spacer(Modifier.width(6.dp))
            Text(
                pluralStringResource(
                    R.plurals.ui_calendar_month_summary_plural,
                    daysWithRecords,
                    daysWithRecords
                ),
                color = PrimaryGreen,
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun MonthNavButton(glyph: AppGlyph, onClick: () -> Unit) {
    Box(
        Modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(MintTint.copy(alpha = 0.72f))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        GlyphIcon(glyph, tint = PrimaryGreen, size = 17.dp)
    }
}

@Composable
private fun DayCell(
    day: Int?,
    yearMonth: YearMonth,
    flagsMap: Map<String, DayFlags>,
    selected: LocalDate,
    onSelect: (LocalDate) -> Unit,
    modifier: Modifier = Modifier
) {
    if (day == null) {
        Box(modifier.aspectRatio(1f))
        return
    }
    val date = yearMonth.atDay(day)
    val flags = flagsMap[date.toString()]
    val isSelected = date == selected
    val isToday = date == LocalDate.now()
    val numberBackground by animateColorAsState(
        targetValue = if (isSelected) PrimaryGreen else Color.Transparent,
        animationSpec = tween(180),
        label = "dayCellBg"
    )
    Box(
        modifier
            .aspectRatio(1f),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(numberBackground)
                    .then(
                        if (isToday && !isSelected) {
                            Modifier.border(1.dp, PrimaryGreen.copy(alpha = 0.55f), CircleShape)
                        } else {
                            Modifier
                        }
                    )
                    .clickable { onSelect(date) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "$day",
                    color = when {
                        isSelected -> Color.White
                        isToday -> PrimaryGreen
                        date.dayOfWeek.value >= 6 -> TextSub
                        else -> TextDark
                    },
                    fontSize = 13.sp,
                    fontWeight = if (isSelected || isToday) FontWeight.Bold else FontWeight.Normal
                )
            }
            Row(
                Modifier.height(9.dp),
                horizontalArrangement = Arrangement.spacedBy(2.5.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (flags?.medication == true) Dot(CheckGreen)
                if (flags?.visit == true) Dot(WarningRed)
                if (flags?.occasional == true) Dot(OccasionalOrange)
            }
        }
    }
}

@Composable
private fun Dot(color: Color) {
    Box(Modifier.size(5.dp).clip(CircleShape).background(color))
}

@Composable
private fun LegendRow() {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White.copy(alpha = 0.55f))
            .padding(horizontal = 12.dp, vertical = 9.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        LegendItem(CheckGreen, stringResource(R.string.calendar_legend_medication))
        LegendItem(WarningRed, stringResource(R.string.calendar_legend_visit))
        LegendItem(OccasionalOrange, stringResource(R.string.calendar_legend_occasional))
    }
}

@Composable
private fun LegendItem(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Dot(color)
        Spacer(Modifier.width(5.dp))
        Text(
            label,
            color = TextSub,
            fontSize = 10.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun DayDetail(
    date: LocalDate,
    records: List<DoseRecord>,
    nameMap: Map<Long, MedNames>,
    flags: DayFlags?,
    onDelete: (DoseRecord) -> Unit
) {
    AppCard(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(18.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    localizedDate(date, R.string.common_date_header),
                    color = TextDark,
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    pluralStringResource(R.plurals.ui_day_records_plural, records.size, records.size),
                    color = TextSub,
                    fontSize = 11.5.sp
                )
            }
            if (date == LocalDate.now()) {
                AppPill(stringResource(R.string.ui_today), glyph = AppGlyph.Sun)
            }
        }

        if (flags?.visit == true) {
            Spacer(Modifier.height(12.dp))
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(WarningSoft)
                    .padding(horizontal = 11.dp, vertical = 9.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                GlyphIcon(AppGlyph.Bell, tint = WarningRed, size = 15.dp)
                Spacer(Modifier.width(7.dp))
                Text(
                    stringResource(R.string.ui_visit_period),
                    color = WarningRed,
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        if (records.isEmpty()) {
            EmptyState(
                glyph = AppGlyph.Calendar,
                title = stringResource(R.string.calendar_no_records),
                modifier = Modifier.padding(vertical = 4.dp)
            )
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                records.forEach { record ->
                    CalendarRecordRow(
                        record = record,
                        nameMap = nameMap,
                        onDelete = { onDelete(record) }
                    )
                }
            }
        }
    }
}

@Composable
private fun CalendarRecordRow(
    record: DoseRecord,
    nameMap: Map<Long, MedNames>,
    onDelete: () -> Unit
) {
    val names = nameMap[record.medicationId]
    val full = record.medicationName.ifBlank {
        names?.full ?: stringResource(R.string.calendar_medication_fallback)
    }
    val dose = names?.dose ?: 1.0
    val tag = when {
        record.flag == RecordFlag.EARLY -> stringResource(R.string.calendar_tag_early)
        record.flag == RecordFlag.BACKFILL -> stringResource(R.string.calendar_tag_backfill)
        record.category == RecordCategory.OCCASIONAL -> stringResource(R.string.calendar_tag_occasional)
        else -> null
    }
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MintWash.copy(alpha = 0.8f))
            .padding(start = 11.dp, end = 4.dp, top = 9.dp, bottom = 9.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .width(46.dp)
                .clip(RoundedCornerShape(11.dp))
                .background(Color.White.copy(alpha = 0.8f))
                .padding(vertical = 6.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                record.time,
                color = PrimaryGreen,
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(
                full,
                color = TextDark,
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.5.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(3.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    stringResource(R.string.label_intake, formatNum(dose)),
                    color = TextSub,
                    fontSize = 10.5.sp
                )
                if (tag != null) {
                    Spacer(Modifier.width(6.dp))
                    AppPill(
                        tag,
                        color = if (record.flag == RecordFlag.EARLY) PrimaryGreen else OccasionalOrange,
                        containerColor = if (record.flag == RecordFlag.EARLY) MintTint else WarmTint
                    )
                }
            }
        }
        Box(
            Modifier
                .size(34.dp)
                .clip(CircleShape)
                .clickable(onClick = onDelete),
            contentAlignment = Alignment.Center
        ) {
            GlyphIcon(AppGlyph.Trash, tint = TextFaint, size = 16.dp)
        }
    }
}
