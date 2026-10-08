package com.baiyue.medicine.ui.card

import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
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
import com.baiyue.medicine.data.entity.Medication
import com.baiyue.medicine.ui.MedicationImage
import com.baiyue.medicine.ui.doseLabel
import com.baiyue.medicine.ui.intakeLabel
import com.baiyue.medicine.ui.localizedDate
import com.baiyue.medicine.ui.usageLabel
import com.baiyue.medicine.ui.theme.CheckGreen
import com.baiyue.medicine.ui.theme.OccasionalOrange
import com.baiyue.medicine.ui.theme.PrimaryGreen
import com.baiyue.medicine.ui.theme.TextDark
import com.baiyue.medicine.ui.theme.TextSub
import java.time.LocalDate

@Composable
fun CardScreen(vm: CardViewModel = viewModel()) {
    val todayEntries by vm.todayEntries.collectAsState()
    val prescriptions by vm.prescriptions.collectAsState()
    val periodCounts by vm.periodCounts.collectAsState()
    val showImage by vm.showImage.collectAsState()
    var showRx by remember { mutableStateOf(false) }
    var easterEgg by remember { mutableStateOf(false) }
    var summaryTaps by remember { mutableStateOf(0) }

    val totalDoses = todayEntries.sumOf { it.totalCount }
    val today = LocalDate.now()

    val summaryColor by animateColorAsState(
        targetValue = if (showRx) PrimaryGreen else CheckGreen,
        animationSpec = tween(400),
        label = "summaryColor"
    )

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                stringResource(R.string.card_title),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = TextDark
            )
            Spacer(Modifier.weight(1f))
            FilterChip(
                selected = !showRx,
                onClick = { showRx = false },
                label = { Text(stringResource(R.string.card_filter_today)) }
            )
            Spacer(Modifier.width(8.dp))
            FilterChip(
                selected = showRx,
                onClick = { showRx = true },
                label = { Text(stringResource(R.string.card_filter_prescription)) }
            )
        }
        Spacer(Modifier.height(12.dp))

        Card(
            colors = CardDefaults.cardColors(containerColor = summaryColor),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    summaryTaps += 1
                    if (summaryTaps >= 10) {
                        summaryTaps = 0
                        easterEgg = true
                    }
                }
        ) {
            Column(Modifier.padding(16.dp)) {
                Text(
                    if (showRx) stringResource(R.string.card_my_prescription)
                    else stringResource(
                        R.string.card_summary_title,
                        localizedDate(today, R.string.common_date_day)
                    ),
                    color = Color.White,
                    fontWeight = FontWeight.Medium
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    if (showRx) stringResource(R.string.card_rx_count, prescriptions.size)
                    else stringResource(R.string.card_today_summary, totalDoses, todayEntries.size),
                    color = Color.White,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                )
                if (!showRx) {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        stringResource(R.string.card_week_month_taken, periodCounts.week, periodCounts.month),
                        color = Color.White.copy(alpha = 0.9f),
                        fontSize = 13.sp
                    )
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        Crossfade(targetState = showRx, animationSpec = tween(300), label = "listCrossfade") { rx ->
            if (rx) {
                if (prescriptions.isEmpty()) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(stringResource(R.string.card_empty_prescriptions), color = TextSub)
                    }
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(prescriptions, key = { it.id }) { med ->
                            RxEntryCard(med, showImage)
                        }
                    }
                }
            } else {
                if (todayEntries.isEmpty()) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(stringResource(R.string.card_empty_today), color = TextSub)
                    }
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(todayEntries, key = { it.medication.id }) { entry ->
                            EntryCard(entry, showImage)
                        }
                    }
                }
            }
        }
    }

    if (easterEgg) {
        AlertDialog(
            onDismissRequest = { easterEgg = false },
            text = { Text(stringResource(R.string.card_easter_egg), color = TextDark, fontWeight = FontWeight.Bold) },
            confirmButton = {
                TextButton(onClick = { easterEgg = false }) { Text(stringResource(R.string.action_ok)) }
            }
        )
    }
}

@Composable
private fun RxEntryCard(med: Medication, showImage: Boolean) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            if (showImage) {
                MedicationImage(med.imageUri, 48.dp)
                Spacer(Modifier.width(12.dp))
            }
            Column(Modifier.weight(1f)) {
                Text(med.name, color = TextDark, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(4.dp))
                Text(stringResource(R.string.label_usage, usageLabel(med)), color = TextSub, fontSize = 12.sp)
                Text(stringResource(R.string.label_frequency, doseLabel(med)), color = TextSub, fontSize = 12.sp)
                Text(intakeLabel(med), color = TextSub, fontSize = 12.sp)
                if (med.note.isNotBlank()) {
                    Text(med.note, color = TextSub, fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
private fun EntryCard(entry: CardEntry, showImage: Boolean) {
    val med = entry.medication
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            if (showImage) {
                MedicationImage(med.imageUri, 48.dp)
                Spacer(Modifier.width(12.dp))
            }
            Column(Modifier.weight(1f)) {
                Text(med.name, color = TextDark, fontWeight = FontWeight.Bold)
                Text(usageLabel(med), color = TextSub, fontSize = 12.sp)
                Text(intakeLabel(med), color = TextSub, fontSize = 12.sp)
                if (med.note.isNotBlank()) {
                    Text(med.note, color = TextSub, fontSize = 12.sp)
                }
                Spacer(Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (entry.normalCount > 0) DotLabel(CheckGreen, stringResource(R.string.card_normal_count, entry.normalCount))
                    if (entry.occasionalCount > 0) DotLabel(OccasionalOrange, stringResource(R.string.card_occasional_count, entry.occasionalCount))
                }
            }
            Spacer(Modifier.width(8.dp))
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("${entry.totalCount}", color = PrimaryGreen, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                Text(stringResource(R.string.card_unit_times), color = TextSub, fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun DotLabel(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(8.dp).clip(RoundedCornerShape(2.dp)).background(color))
        Spacer(Modifier.width(4.dp))
        Text(label, color = TextSub, fontSize = 12.sp)
    }
}