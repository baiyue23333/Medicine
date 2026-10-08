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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.baiyue.medicine.R
import com.baiyue.medicine.data.entity.DoseRecord
import com.baiyue.medicine.model.RecordCategory
import com.baiyue.medicine.model.RecordFlag
import com.baiyue.medicine.ui.formatNum
import com.baiyue.medicine.ui.localizedDate
import com.baiyue.medicine.ui.theme.CheckGreen
import com.baiyue.medicine.ui.theme.OccasionalOrange
import com.baiyue.medicine.ui.theme.PrimaryGreen
import com.baiyue.medicine.ui.theme.TextDark
import com.baiyue.medicine.ui.theme.TextSub
import com.baiyue.medicine.ui.theme.WarningRed
import java.time.LocalDate
import java.time.YearMonth

@Composable
fun CalendarScreen(vm: CalendarViewModel = viewModel()) {
    val flagsMap by vm.flags.collectAsState()
    val nameMap by vm.medNameMap.collectAsState()

    var yearMonth by remember { mutableStateOf(YearMonth.now()) }
    var selected by remember { mutableStateOf(LocalDate.now()) }
    var recordToDelete by remember { mutableStateOf<DoseRecord?>(null) }

    val records by remember(selected) { vm.recordsFor(selected.toString()) }
        .collectAsState(initial = emptyList())

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text(
            stringResource(R.string.calendar_title),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = TextDark
        )

        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { yearMonth = yearMonth.minusMonths(1) }) { Text("◀", color = PrimaryGreen) }
            AnimatedContent(
                targetState = yearMonth,
                transitionSpec = {
                    if (targetState > initialState) {
                        (slideInHorizontally { it / 2 } + fadeIn()) togetherWith
                            (slideOutHorizontally { -it / 2 } + fadeOut())
                    } else {
                        (slideInHorizontally { -it / 2 } + fadeIn()) togetherWith
                            (slideOutHorizontally { it / 2 } + fadeOut())
                    }
                },
                modifier = Modifier.weight(1f),
                label = "monthTitle"
            ) { ym ->
                Text(
                    localizedDate(ym.atDay(1), R.string.common_month_header),
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    color = TextDark,
                    fontWeight = FontWeight.Medium
                )
            }
            IconButton(onClick = { yearMonth = yearMonth.plusMonths(1) }) { Text("▶", color = PrimaryGreen) }
        }

        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            LegendDot(CheckGreen, stringResource(R.string.calendar_legend_medication))
            LegendDot(WarningRed, stringResource(R.string.calendar_legend_visit))
            LegendDot(OccasionalOrange, stringResource(R.string.calendar_legend_occasional))
        }

        Spacer(Modifier.height(8.dp))

        val weekdays = stringResource(R.string.common_weekdays).split(",")
        Row(Modifier.fillMaxWidth()) {
            weekdays.forEach {
                Text(
                    it,
                    modifier = Modifier.weight(1f),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    color = TextSub,
                    fontSize = 12.sp
                )
            }
        }

        val firstDay = yearMonth.atDay(1)
        val blanks = firstDay.dayOfWeek.value % 7
        val cells: List<Int?> = List(blanks) { null } + (1..yearMonth.lengthOfMonth()).map { it }

        cells.chunked(7).forEach { week ->
            Row(Modifier.fillMaxWidth()) {
                week.forEach { d ->
                    DayCell(
                        day = d,
                        yearMonth = yearMonth,
                        flagsMap = flagsMap,
                        selected = selected,
                        onSelect = { selected = it },
                        modifier = Modifier.weight(1f)
                    )
                }
                repeat(7 - week.size) { Spacer(Modifier.weight(1f)) }
            }
        }

        Spacer(Modifier.height(16.dp))

        AnimatedContent(
            targetState = selected,
            transitionSpec = { fadeIn(tween(200)) togetherWith fadeOut(tween(150)) },
            label = "dayDetail"
        ) { date ->
            DayDetail(
                date = date,
                // SKIPPED 只是「确认未服」的占位记录，不作为服药记录展示
                records = if (date == selected) records.filter { it.category != RecordCategory.SKIPPED }
                else emptyList(),
                nameMap = nameMap,
                flags = flagsMap[date.toString()],
                onDelete = { recordToDelete = it }
            )
        }
    }

    recordToDelete?.let { record ->
        AlertDialog(
            onDismissRequest = { recordToDelete = null },
            shape = RoundedCornerShape(28.dp),
            title = { Text(stringResource(R.string.calendar_undo_title), fontWeight = FontWeight.Bold, color = TextDark) },
            text = { Text(stringResource(R.string.calendar_undo_message)) },
            confirmButton = {
                TextButton(onClick = {
                    vm.deleteRecord(record)
                    recordToDelete = null
                }) { Text(stringResource(R.string.calendar_undo_confirm), color = WarningRed, fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { recordToDelete = null }) { Text(stringResource(R.string.action_cancel)) }
            }
        )
    }
}

@Composable
private fun LegendDot(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Dot(color)
        Spacer(Modifier.width(4.dp))
        Text(label, color = TextSub, fontSize = 12.sp)
    }
}

@Composable
private fun Dot(color: Color) {
    Box(Modifier.size(8.dp).clip(CircleShape).background(color))
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
    val bgColor by animateColorAsState(
        targetValue = if (isSelected) PrimaryGreen.copy(alpha = 0.15f) else Color.Transparent,
        animationSpec = tween(200),
        label = "dayCellBg"
    )
    Box(
        modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(10.dp))
            .background(bgColor)
            .clickable { onSelect(date) },
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                "$day",
                color = when {
                    isSelected -> PrimaryGreen
                    isToday -> PrimaryGreen
                    else -> TextDark
                },
                fontSize = 14.sp,
                fontWeight = if (isToday || isSelected) FontWeight.Bold else FontWeight.Normal
            )
            Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                if (flags?.medication == true) Dot(CheckGreen)
                if (flags?.visit == true) Dot(WarningRed)
                if (flags?.occasional == true) Dot(OccasionalOrange)
            }
        }
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
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(
                localizedDate(date, R.string.common_date_day),
                color = TextDark,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(8.dp))
            if (records.isEmpty()) {
                Text(stringResource(R.string.calendar_no_records), color = TextSub)
            } else {
                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier
                        .heightIn(max = 220.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    records.forEach { r ->
                        val names = nameMap[r.medicationId]
                        val full = names?.full ?: stringResource(R.string.calendar_medication_fallback)
                        val short = names?.short ?: full
                        val dose = names?.dose ?: 1.0
                        val tag = when {
                            r.flag == RecordFlag.EARLY -> stringResource(R.string.calendar_tag_early)
                            r.flag == RecordFlag.BACKFILL -> stringResource(R.string.calendar_tag_backfill)
                            r.category == RecordCategory.OCCASIONAL -> stringResource(R.string.calendar_tag_occasional)
                            else -> ""
                        }
                        CalendarRecordRow(time = r.time, full = full, short = short, dose = dose, tag = tag, onDelete = { onDelete(r) })
                    }
                }
            }
            if (flags?.visit == true) {
                Spacer(Modifier.height(8.dp))
                Text(stringResource(R.string.calendar_visit_warning), color = WarningRed, fontSize = 13.sp)
            }
        }
    }
}

@Composable
private fun CalendarRecordRow(time: String, full: String, short: String, dose: Double, tag: String, onDelete: () -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val display = if (expanded || full.length <= 6) full else short
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Text(
            stringResource(R.string.calendar_record_line, time, display, tag, formatNum(dose)),
            color = TextDark,
            fontSize = 14.sp,
            modifier = Modifier.weight(1f).let { if (full.length > 6) it.clickable { expanded = !expanded } else it }
        )
        IconButton(onClick = onDelete) {
            Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.action_delete), tint = TextSub)
        }
    }
}