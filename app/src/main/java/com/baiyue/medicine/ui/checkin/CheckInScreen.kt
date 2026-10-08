package com.baiyue.medicine.ui.checkin

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.baiyue.medicine.R
import com.baiyue.medicine.data.entity.Medication
import com.baiyue.medicine.ui.MedicationImage
import com.baiyue.medicine.ui.intakeLabel
import com.baiyue.medicine.ui.localizedDate
import com.baiyue.medicine.ui.usageLabel
import com.baiyue.medicine.ui.theme.CheckGreen
import com.baiyue.medicine.ui.theme.PrimaryGreen
import com.baiyue.medicine.ui.theme.TextDark
import com.baiyue.medicine.ui.theme.TextSub
import com.baiyue.medicine.ui.theme.WarningRed
import kotlinx.coroutines.delay
import java.time.LocalDate

private class Feedback(val color: Color, val message: String)

private val pastelColors = listOf(
    Color(0xFFB2DFDB), Color(0xFFB3E5FC), Color(0xFFC5E1A5), Color(0xFFFFF9C4),
    Color(0xFFFFCCBC), Color(0xFFD1C4E9), Color(0xFFF8BBD0), Color(0xFFC8E6C9)
)

@Composable
fun CheckInScreen(vm: CheckInViewModel = viewModel()) {
    val dueItems by vm.dueItems.collectAsState()
    val missedItems by vm.missedItems.collectAsState()
    val occasional by vm.occasionalMeds.collectAsState()
    val nextDose by vm.nextDose.collectAsState()
    val warningMeds by vm.warningMeds.collectAsState()
    val closeDoseWarnings by vm.closeDoseWarnings.collectAsState()
    val canUndo by vm.canUndo.collectAsState()
    val showImage by vm.showImage.collectAsState()

    // 在 Composable 作用域内解析，供非 Composable 回调（如按钮 onClick）使用
    val praiseMessages = stringArrayResource(R.array.checkin_praise_messages)
    val context = LocalContext.current

    var showOccasionalDialog by remember { mutableStateOf(false) }
    var showBackfillDialog by remember { mutableStateOf(false) }
    var selectedOccasional by remember { mutableStateOf<List<Medication>>(emptyList()) }
    var feedback by remember { mutableStateOf<Feedback?>(null) }
    var warningDismissed by remember { mutableStateOf(false) }

    LaunchedEffect(feedback) {
        if (feedback != null) {
            delay(900)
            feedback = null
        }
    }

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            val pending = dueItems.size + selectedOccasional.size
            Column(
                Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
            ) {
                Column(Modifier.padding(20.dp)) {
                    val today = LocalDate.now()
                    Text(
                        localizedDate(today, R.string.common_date_header),
                        color = TextSub,
                        fontSize = 13.sp
                    )
                    Spacer(Modifier.height(2.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            stringResource(R.string.checkin_title),
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = TextDark
                        )
                        Spacer(Modifier.weight(1f))
                        if (pending > 0) {
                            Box(
                                Modifier
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(CheckGreen.copy(alpha = 0.25f))
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text(stringResource(R.string.checkin_pending_count, pending), color = PrimaryGreen, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    if (missedItems.isNotEmpty()) {
                        MissedBanner(count = missedItems.size, onClick = { showBackfillDialog = true })
                        Spacer(Modifier.height(12.dp))
                    }

                    if (closeDoseWarnings.isNotEmpty()) {
                        CloseDoseBanner(closeDoseWarnings.first())
                        Spacer(Modifier.height(12.dp))
                    }

                    if (dueItems.isEmpty() && selectedOccasional.isEmpty()) {
                        val nd = nextDose
                        if (nd != null) {
                            NextDoseCard(
                                nd,
                                showImage = showImage,
                                onEarly = if (!nd.missed && !nd.tomorrow) {
                                    {
                                        vm.checkInEarly(nd.time, nd.items)
                                        feedback = Feedback(pastelColors.random(), context.getString(R.string.checkin_early_done))
                                    }
                                } else null
                            )
                        } else {
                            Column(
                                Modifier.fillMaxWidth().padding(vertical = 32.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Box(
                                    Modifier.size(72.dp).clip(CircleShape).background(CheckGreen.copy(alpha = 0.2f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Filled.Check, contentDescription = null, tint = PrimaryGreen, modifier = Modifier.size(36.dp))
                                }
                                Spacer(Modifier.height(12.dp))
                                Text(stringResource(R.string.checkin_no_meds_now), color = TextSub)
                            }
                        }
                    } else {
                        Text(stringResource(R.string.checkin_meds_due_header), color = TextSub, fontSize = 13.sp)
                        Spacer(Modifier.height(8.dp))
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            dueItems.forEach { item -> DueMedicationRow(item, showImage) }
                            selectedOccasional.forEach { med -> OccasionalSelectedRow(med, showImage) }
                        }
                    }
                }
            }

            Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp)) {
                if (canUndo) {
                    TextButton(
                        onClick = { vm.undoLastCheckIn() },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(stringResource(R.string.checkin_undo), color = TextSub)
                    }
                }
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val interactionSource = remember { MutableInteractionSource() }
                    val pressed by interactionSource.collectIsPressedAsState()
                    val scale by animateFloatAsState(
                        targetValue = if (pressed) 0.95f else 1f,
                        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                        label = "checkInScale"
                    )
                    Button(
                        onClick = {
                            feedback = Feedback(pastelColors.random(), praiseMessages.random())
                            if (selectedOccasional.isNotEmpty()) {
                                vm.checkInOccasional(selectedOccasional)
                                selectedOccasional = emptyList()
                            } else {
                                vm.checkInNormal(dueItems)
                            }
                        },
                        interactionSource = interactionSource,
                        enabled = pending > 0,
                        modifier = Modifier
                            .weight(1f)
                            .height(60.dp)
                            .testTag("checkInButton")
                            .graphicsLayer {
                                scaleX = scale
                                scaleY = scale
                            },
                        shape = RoundedCornerShape(20.dp),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
                    ) {
                        Icon(Icons.Filled.Check, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text(
                            if (selectedOccasional.isNotEmpty()) stringResource(R.string.checkin_record_occasional) else stringResource(R.string.checkin_action_check_in),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    OutlinedButton(
                        onClick = { showOccasionalDialog = true },
                        modifier = Modifier.height(60.dp),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Text(stringResource(R.string.checkin_occasional))
                    }
                }
            }
        }

        feedback?.let { fb ->
            CelebrationOverlay(fb)
        }
    }

    if (showOccasionalDialog) {
        OccasionalDialog(
            occasional = occasional,
            selected = selectedOccasional,
            onConfirm = { sel ->
                selectedOccasional = sel
                showOccasionalDialog = false
            },
            onDismiss = { showOccasionalDialog = false }
        )
    }

    if (showBackfillDialog) {
        BackfillDialog(
                missed = missedItems,
                todayDate = LocalDate.now().toString(),
                onConfirm = { sel ->
                    showBackfillDialog = false
                    if (sel.isNotEmpty()) {
                        feedback = Feedback(pastelColors.random(), context.getString(R.string.checkin_backfill_done, sel.size))
                        vm.checkInBackfill(sel)
                    }
                },
            onDelete = { item -> vm.skipMissed(listOf(item)) },
            onDismiss = { showBackfillDialog = false }
        )
    }

    if (warningMeds.isNotEmpty() && !warningDismissed) {
        AlertDialog(
            onDismissRequest = { warningDismissed = true },
            shape = RoundedCornerShape(28.dp),
            title = { Text(stringResource(R.string.checkin_refill_alert_title), fontWeight = FontWeight.Bold, color = TextDark) },
            text = {
                Column {
                    Text(stringResource(R.string.checkin_refill_alert_message))
                    Spacer(Modifier.height(8.dp))
                    warningMeds.forEach { med ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(6.dp).clip(CircleShape).background(WarningRed))
                            Spacer(Modifier.width(8.dp))
                            MedNameToggle(med, fontSize = 14.sp)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { warningDismissed = true }) { Text(stringResource(R.string.checkin_got_it)) }
            }
        )
    }
}

@Composable
private fun CelebrationOverlay(fb: Feedback) {
    val scale = remember { Animatable(0.3f) }
    LaunchedEffect(fb) {
        scale.animateTo(
            1f,
            spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow)
        )
    }
    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.35f)),
        contentAlignment = Alignment.Center
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = fb.color),
            shape = RoundedCornerShape(24.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            modifier = Modifier
                .padding(40.dp)
                .fillMaxWidth()
                .graphicsLayer {
                    scaleX = scale.value
                    scaleY = scale.value
                }
        ) {
            Column(
                Modifier.fillMaxWidth().padding(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    Modifier.size(64.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.6f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Filled.Check,
                        contentDescription = null,
                        tint = TextDark,
                        modifier = Modifier.size(36.dp)
                    )
                }
                Spacer(Modifier.height(14.dp))
                Text(fb.message, color = TextDark, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Spacer(Modifier.height(4.dp))
                Text(stringResource(R.string.checkin_celebration_subtitle), color = TextDark.copy(alpha = 0.7f), fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun NextDoseCard(nd: NextDose, showImage: Boolean = true, onEarly: (() -> Unit)? = null) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(52.dp).clip(RoundedCornerShape(14.dp)).background(if (nd.missed) WarningRed.copy(alpha = 0.15f) else CheckGreen.copy(alpha = 0.25f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(nd.time, color = if (nd.missed) WarningRed else PrimaryGreen, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        stringResource(if (nd.missed) R.string.checkin_missed_next_title else R.string.checkin_next_dose_title),
                        color = TextDark,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    Text(
                        when {
                            nd.missed -> stringResource(R.string.checkin_missed_next_subtitle)
                            nd.tomorrow -> stringResource(R.string.checkin_tomorrow)
                            else -> stringResource(R.string.checkin_today)
                        },
                        color = TextSub,
                        fontSize = 12.sp
                    )
                }
            }
            Spacer(Modifier.height(14.dp))
            nd.items.forEach { med ->
                NextDoseItemRow(med, showImage)
            }
            if (onEarly != null) {
                Spacer(Modifier.height(12.dp))
                OutlinedButton(
                    onClick = onEarly,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text(stringResource(R.string.checkin_early), fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun NextDoseItemRow(med: Medication, showImage: Boolean = true) {
    var expanded by remember { mutableStateOf(false) }
    val display = if (expanded || med.name.length <= 6) med.name
    else med.name.take(4) + "…"
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .clickable(enabled = med.name.length > 6) { expanded = !expanded },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(6.dp).clip(CircleShape).background(PrimaryGreen))
        Spacer(Modifier.width(10.dp))
        if (showImage) {
            MedicationImage(med.imageUri, 40.dp)
            Spacer(Modifier.width(10.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(display, color = TextDark, fontWeight = FontWeight.Medium)
            Text(stringResource(R.string.label_usage, usageLabel(med)), color = TextSub, fontSize = 12.sp)
            Text(intakeLabel(med), color = TextSub, fontSize = 12.sp)
            if (med.note.isNotBlank()) {
                Text(med.note, color = TextSub, fontSize = 12.sp)
            }
        }
        if (med.name.length > 6) {
            Text(if (expanded) stringResource(R.string.checkin_collapse) else stringResource(R.string.checkin_view_full_name), color = PrimaryGreen, fontSize = 11.sp)
        }
    }
}

@Composable
private fun DueMedicationRow(item: DueItem, showImage: Boolean = true) {
    var expanded by remember { mutableStateOf(false) }
    val med = item.medication
    val display = if (expanded || med.name.length <= 6) med.name
    else med.name.take(4) + "…"

    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize()
            .clickable { expanded = !expanded }
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(52.dp).clip(RoundedCornerShape(14.dp)).background(CheckGreen.copy(alpha = 0.25f)),
                contentAlignment = Alignment.Center
            ) {
                Text(item.time, color = PrimaryGreen, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.width(14.dp))
            if (showImage) {
                MedicationImage(med.imageUri, 40.dp)
                Spacer(Modifier.width(12.dp))
            }
            Column(Modifier.weight(1f)) {
                Text(display, color = TextDark, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                if (expanded && display != med.name) {
                    Text(med.name, color = TextSub, fontSize = 12.sp)
                }
                Text(stringResource(R.string.label_usage_short, usageLabel(med)), color = TextSub, fontSize = 12.sp)
                Text(intakeLabel(med), color = TextSub, fontSize = 12.sp)
                if (med.note.isNotBlank()) {
                    Text(med.note, color = TextSub, fontSize = 12.sp)
                }
            }
            if (med.name.length > 6) {
                Text(if (expanded) stringResource(R.string.checkin_collapse) else stringResource(R.string.checkin_view_full_name), color = PrimaryGreen, fontSize = 11.sp)
            }
        }
    }
}

@Composable
private fun OccasionalSelectedRow(med: Medication, showImage: Boolean = true) {
    var expanded by remember { mutableStateOf(false) }
    val display = if (expanded || med.name.length <= 6) med.name
    else med.name.take(4) + "…"
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3E0)),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize()
            .clickable { expanded = !expanded }
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(52.dp).clip(RoundedCornerShape(14.dp)).background(Color(0xFFFFB74D).copy(alpha = 0.3f)),
                contentAlignment = Alignment.Center
            ) {
                Text(stringResource(R.string.checkin_occasional_short), color = Color(0xFFE6892E), fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.width(14.dp))
            if (showImage) {
                MedicationImage(med.imageUri, 40.dp)
                Spacer(Modifier.width(12.dp))
            }
            Column(Modifier.weight(1f)) {
                Text(display, color = TextDark, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                if (expanded && display != med.name) {
                    Text(med.name, color = TextSub, fontSize = 12.sp)
                }
                Text(stringResource(R.string.checkin_will_record_occasional), color = TextSub, fontSize = 12.sp)
                Text(intakeLabel(med), color = TextSub, fontSize = 12.sp)
                if (med.note.isNotBlank()) {
                    Text(med.note, color = TextSub, fontSize = 12.sp)
                }
            }
            if (med.name.length > 6) {
                Text(if (expanded) stringResource(R.string.checkin_collapse) else stringResource(R.string.checkin_view_full_name), color = PrimaryGreen, fontSize = 11.sp)
            }
        }
    }
}

/** 长名称默认显示缩写，点击切换全称 */
@Composable
private fun MedNameToggle(med: Medication, fontSize: TextUnit = 15.sp) {
    var expanded by remember { mutableStateOf(false) }
    val long = med.name.length > 6
    val display = if (expanded || !long) med.name else med.name.take(4) + "…"
    Text(
        display,
        color = TextDark,
        fontSize = fontSize,
        modifier = if (long) Modifier.clickable { expanded = !expanded } else Modifier
    )
}

@Composable
private fun OccasionalDialog(
    occasional: List<Medication>,
    selected: List<Medication>,
    onConfirm: (List<Medication>) -> Unit,
    onDismiss: () -> Unit
) {
    val checked = remember(occasional) {
        mutableStateListOf<Long>().apply { addAll(selected.map { it.id }) }
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(28.dp),
        title = { Text(stringResource(R.string.checkin_occasional_dialog_title), fontWeight = FontWeight.Bold, color = TextDark) },
        text = {
            if (occasional.isEmpty()) {
                Text(stringResource(R.string.checkin_occasional_empty))
            } else {
                Column(
                    Modifier
                        .heightIn(max = 320.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    occasional.forEachIndexed { index, med ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth().clickable {
                                if (checked.contains(med.id)) checked.remove(med.id) else checked.add(med.id)
                            }
                        ) {
                            Text("${index + 1}.", modifier = Modifier.width(24.dp), color = TextDark)
                            Checkbox(
                                checked = checked.contains(med.id),
                                onCheckedChange = {
                                    if (checked.contains(med.id)) checked.remove(med.id) else checked.add(med.id)
                                }
                            )
                            MedNameToggle(med, fontSize = 15.sp)
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(occasional.filter { checked.contains(it.id) }) }) {
                Text(stringResource(R.string.action_ok))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        }
    )
}

/** 已错过时段的提示条，点击进入补签 */
@Composable
private fun MissedBanner(count: Int, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(WarningRed.copy(alpha = 0.12f))
            .clickable(onClick = onClick)
            .testTag("missedBanner")
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(6.dp).clip(CircleShape).background(WarningRed))
        Spacer(Modifier.width(8.dp))
        Text(stringResource(R.string.checkin_missed_count, count), color = TextDark, fontSize = 13.sp)
        Spacer(Modifier.weight(1f))
        Text(stringResource(R.string.checkin_backfill), color = PrimaryGreen, fontSize = 13.sp, fontWeight = FontWeight.Bold)
    }
}

/** 两次服用过近的提醒条 */
@Composable
private fun CloseDoseBanner(warning: CloseDoseWarning) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(WarningRed.copy(alpha = 0.12f))
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(6.dp).clip(CircleShape).background(WarningRed))
        Spacer(Modifier.width(8.dp))
        Text(
            stringResource(R.string.checkin_close_dose_warning, warning.medName, warning.minutes),
            color = TextDark,
            fontSize = 13.sp
        )
    }
}

private fun dueKey(item: DueItem): String = "${item.date}|${item.medication.id}|${item.time}"

/** 时段标签：昨天的项额外标注「昨天」，避免与今天的同名时段混淆 */
@Composable
private fun MissedTimeLabel(item: DueItem, todayDate: String) {
    if (item.date != todayDate) {
        Text(stringResource(R.string.checkin_yesterday), color = TextSub, fontSize = 12.sp)
        Spacer(Modifier.width(4.dp))
    }
    Text(item.time, color = PrimaryGreen, fontSize = 13.sp, fontWeight = FontWeight.Bold)
}

/** 补签弹窗：勾选需要补记的已错过时段，或进入「管理」删除确实未服的时段 */
@Composable
private fun BackfillDialog(
    missed: List<DueItem>,
    todayDate: String,
    onConfirm: (List<DueItem>) -> Unit,
    onDelete: (DueItem) -> Unit,
    onDismiss: () -> Unit
) {
    var manage by remember { mutableStateOf(false) }
    var pendingDelete by remember { mutableStateOf<DueItem?>(null) }
    val checked = remember(missed) {
        mutableStateListOf<String>().apply { addAll(missed.map { dueKey(it) }) }
    }

    // 全部删除后自动关闭
    LaunchedEffect(missed.isEmpty()) {
        if (missed.isEmpty()) onDismiss()
    }

    // 删除前二次确认，避免误触后无法恢复
    pendingDelete?.let { item ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            shape = RoundedCornerShape(28.dp),
            title = { Text(stringResource(R.string.checkin_backfill_delete_title), fontWeight = FontWeight.Bold, color = TextDark) },
            text = {
                Column {
                    Text(stringResource(R.string.checkin_backfill_delete_message), color = TextSub, fontSize = 13.sp)
                    Spacer(Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        MissedTimeLabel(item, todayDate)
                        Spacer(Modifier.width(10.dp))
                        MedNameToggle(item.medication, fontSize = 15.sp)
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        pendingDelete = null
                        onDelete(item)
                    },
                    modifier = Modifier.testTag("backfillDeleteConfirm")
                ) { Text(stringResource(R.string.action_delete), color = WarningRed, fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) { Text(stringResource(R.string.action_cancel)) }
            }
        )
        return
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(28.dp),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.checkin_backfill_dialog_title), fontWeight = FontWeight.Bold, color = TextDark)
                Spacer(Modifier.weight(1f))
                TextButton(
                    onClick = { manage = !manage },
                    modifier = Modifier.testTag("backfillManage")
                ) {
                    Text(if (manage) stringResource(R.string.checkin_done) else stringResource(R.string.checkin_manage), fontSize = 13.sp)
                }
            }
        },
        text = {
            Column {
                Text(
                    if (manage) stringResource(R.string.checkin_backfill_manage_hint)
                    else stringResource(R.string.checkin_backfill_pick_hint),
                    color = TextSub,
                    fontSize = 13.sp
                )
                Spacer(Modifier.height(8.dp))
                Column(
                    Modifier
                        .heightIn(max = 320.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    missed.forEach { item ->
                        val key = dueKey(item)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(enabled = !manage) {
                                    if (checked.contains(key)) checked.remove(key) else checked.add(key)
                                }
                        ) {
                            Checkbox(
                                checked = checked.contains(key),
                                enabled = !manage,
                                onCheckedChange = {
                                    if (checked.contains(key)) checked.remove(key) else checked.add(key)
                                }
                            )
                            MissedTimeLabel(item, todayDate)
                            Spacer(Modifier.width(10.dp))
                            Box(Modifier.weight(1f)) {
                                MedNameToggle(item.medication, fontSize = 15.sp)
                            }
                            if (manage) {
                                TextButton(
                                    onClick = { pendingDelete = item },
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                                ) {
                                    Text(stringResource(R.string.action_delete), color = WarningRed, fontSize = 13.sp)
                                }
                            }
                        }
                    }
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    if (manage) stringResource(R.string.checkin_backfill_delete_note)
                    else stringResource(R.string.checkin_backfill_confirm_note),
                    color = TextSub,
                    fontSize = 11.sp
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(missed.filter { checked.contains(dueKey(it)) }) },
                enabled = !manage && checked.isNotEmpty(),
                modifier = Modifier.testTag("backfillConfirm")
            ) { Text(stringResource(R.string.checkin_backfill)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        }
    )
}
