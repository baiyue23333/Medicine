package com.baiyue.medicine.ui.manage

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import com.baiyue.medicine.R
import com.baiyue.medicine.data.entity.Medication
import com.baiyue.medicine.model.DoseType
import com.baiyue.medicine.model.UsageType
import com.baiyue.medicine.prediction.parseTimes
import com.baiyue.medicine.prediction.predict
import com.baiyue.medicine.ui.MedicationImage
import com.baiyue.medicine.ui.doseLabel
import com.baiyue.medicine.ui.usageLabel
import com.baiyue.medicine.ui.theme.CheckGreen
import com.baiyue.medicine.ui.theme.OccasionalOrange
import com.baiyue.medicine.ui.theme.PrimaryGreen
import com.baiyue.medicine.ui.theme.TextDark
import com.baiyue.medicine.ui.theme.TextSub
import com.baiyue.medicine.ui.theme.WarningRed
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID
import kotlin.math.ceil

@Composable
fun ManageScreen(vm: ManageViewModel = viewModel()) {
    val meds by vm.medications.collectAsState()
    val defaultWarningDays by vm.defaultWarningDays.collectAsState()
    val showImage by vm.showImage.collectAsState()
    var editing by remember { mutableStateOf<Medication?>(null) }
    var showForm by remember { mutableStateOf(false) }
    var deleting by remember { mutableStateOf<Medication?>(null) }

    Scaffold(
        containerColor = Color.Transparent,
        floatingActionButton = {
            FloatingActionButton(
                onClick = { editing = null; showForm = true },
                containerColor = PrimaryGreen
            ) {
                Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.manage_add_medication), tint = Color.White)
            }
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    stringResource(R.string.manage_title),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = TextDark
                )
                Spacer(Modifier.weight(1f))
                Text(stringResource(R.string.manage_show_image), color = TextSub, fontSize = 12.sp)
                Switch(checked = showImage, onCheckedChange = { vm.setShowImage(it) })
            }
            Spacer(Modifier.height(12.dp))
            if (meds.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(stringResource(R.string.manage_empty_hint), color = TextSub)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 96.dp)
                ) {
                    items(meds, key = { it.id }) { med ->
                        MedicationCard(
                            med = med,
                            showImage = showImage,
                            onEdit = { editing = med; showForm = true },
                            onDelete = { deleting = med },
                            onAdjustQuantity = { vm.adjustQuantity(med, it) },
                            modifier = Modifier.animateItem()
                        )
                    }
                }
            }
        }
    }

    if (showForm) {
        MedicationFormDialog(
            initial = editing,
            defaultWarningDays = defaultWarningDays,
            onSave = { vm.upsert(it); showForm = false },
            onDismiss = { showForm = false }
        )
    }

    deleting?.let { med ->
        AlertDialog(
            onDismissRequest = { deleting = null },
            shape = RoundedCornerShape(28.dp),
            title = { Text(stringResource(R.string.manage_delete_title), fontWeight = FontWeight.Bold, color = TextDark) },
            text = { Text(stringResource(R.string.manage_delete_message, med.name)) },
            confirmButton = {
                TextButton(onClick = { vm.delete(med); deleting = null }) {
                    Text(stringResource(R.string.action_delete), color = WarningRed, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { deleting = null }) { Text(stringResource(R.string.action_cancel)) }
            }
        )
    }
}

@Composable
private fun MedicationCard(
    med: Medication,
    showImage: Boolean,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onAdjustQuantity: (Double) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier.fillMaxWidth().clickable { onEdit() }
    ) {
        var showActions by remember { mutableStateOf(false) }
        Column(Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (showImage) {
                    MedicationImage(med.imageUri, 44.dp)
                    Spacer(Modifier.width(10.dp))
                } else if (med.imageUri.isNotEmpty()) {
                    LetterAvatar(med.name)
                    Spacer(Modifier.width(10.dp))
                }
                Column(Modifier.weight(1f)) {
                    Text(med.name, color = TextDark, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }
                if (med.isOccasional) {
                    Spacer(Modifier.width(6.dp))
                    OccasionalBadge()
                }
                if (med.quantity >= 0.0 && med.quantity <= med.minKeepQuantity) {
                    Spacer(Modifier.width(6.dp))
                    LowStockBadge(med.quantity <= 0.0)
                }
                TextButton(
                    onClick = { showActions = !showActions },
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                ) {
                    Text(
                        if (showActions) stringResource(R.string.manage_action_collapse) else stringResource(R.string.manage_action_manage),
                        color = PrimaryGreen,
                        fontSize = 13.sp
                    )
                }
            }
            Spacer(Modifier.height(6.dp))
            InfoLine(stringResource(R.string.manage_label_route), usageLabel(med))
            InfoLine(stringResource(R.string.manage_label_frequency), doseLabel(med))
            InfoLine(stringResource(R.string.manage_label_dose_per_intake), formatNum(med.dosePerIntake))
            InfoLine(
                stringResource(R.string.manage_label_total),
                if (med.quantity < 0) stringResource(R.string.value_not_set) else formatNum(med.quantity)
            )
            Spacer(Modifier.height(2.dp))
            medPredictionLine(med)
            if (med.note.isNotBlank()) {
                Text(stringResource(R.string.manage_note, med.note), color = TextSub, fontSize = 12.sp)
            }
            if (showActions) {
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(stringResource(R.string.manage_label_total), color = TextSub, fontSize = 12.sp)
                    Text(
                        if (med.quantity < 0) stringResource(R.string.value_not_set) else formatNum(med.quantity),
                        color = TextDark,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.weight(1f))
                    QuantityStepButton("−", Modifier) { onAdjustQuantity(-1.0) }
                    Spacer(Modifier.width(8.dp))
                    QuantityStepButton("+", Modifier) { onAdjustQuantity(1.0) }
                }
                Spacer(Modifier.height(4.dp))
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDelete) {
                        Text(stringResource(R.string.action_delete), color = WarningRed, fontSize = 13.sp)
                    }
                    TextButton(onClick = onEdit) {
                        Text(stringResource(R.string.action_edit), color = PrimaryGreen, fontSize = 13.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun OccasionalBadge() {
    Box(
        Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(OccasionalOrange.copy(alpha = 0.25f))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(stringResource(R.string.manage_occasional_badge), color = Color(0xFFE6892E), fontSize = 10.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun LowStockBadge(outOfStock: Boolean) {
    val textRes = if (outOfStock) R.string.manage_out_of_stock else R.string.manage_low_stock
    val color = if (outOfStock) WarningRed else OccasionalOrange
    Box(
        Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(color.copy(alpha = 0.2f))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(stringResource(textRes), color = color, fontSize = 10.sp, fontWeight = FontWeight.Bold)
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun QuantityStepButton(
    label: String,
    modifier: Modifier = Modifier,
    onLongPress: (() -> Unit)? = null,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(10.dp)
    Box(
        modifier
            .clip(shape)
            .border(1.dp, MaterialTheme.colorScheme.outline, shape)
            .combinedClickable(onClick = onClick, onLongClick = onLongPress)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(label, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = PrimaryGreen)
    }
}

@Composable
private fun LetterAvatar(name: String) {
    Box(
        Modifier.size(56.dp).clip(RoundedCornerShape(12.dp)).background(Color(0xFFEAF4EE)),
        contentAlignment = Alignment.Center
    ) {
        Text(name.take(1), color = PrimaryGreen, fontWeight = FontWeight.Bold, fontSize = 20.sp)
    }
}

@Composable
private fun InfoLine(label: String, value: String) {
    Row {
        Text(label, color = TextSub, fontSize = 12.sp)
        Text(value, color = TextDark, fontSize = 12.sp)
    }
}

@Composable
private fun medPredictionLine(med: Medication) {
    if (!med.isScheduled) {
        Text(stringResource(R.string.manage_take_as_needed), color = TextSub, fontSize = 12.sp)
        return
    }
    val p = predict(med)
    if (p == null) {
        Text(
            if (med.quantity < 0) stringResource(R.string.manage_no_quantity_no_prediction)
            else stringResource(R.string.manage_total_quantity, formatNum(med.quantity)),
            color = TextSub,
            fontSize = 12.sp
        )
        return
    }
    val days = ceil(p.daysRemaining).toInt()
    val text = if (p.needVisit) {
        stringResource(R.string.manage_remaining_visit_reminder, formatNum(med.quantity))
    } else {
        stringResource(R.string.manage_remaining_days, formatNum(med.quantity), days)
    }
    Text(text, color = if (p.needVisit) WarningRed else CheckGreen, fontSize = 12.sp)
}

private fun formatNum(d: Double): String =
    if (d == Math.floor(d) && !d.isInfinite()) d.toInt().toString() else d.toString()

private fun fmtTime(p: Pair<Int, Int>): String = "%02d:%02d".format(p.first, p.second)

private fun parseHm(value: String): Pair<Int, Int> {
    val parts = value.split(":")
    val h = (parts.getOrNull(0)?.toIntOrNull() ?: 8).coerceIn(0, 23)
    val m = (parts.getOrNull(1)?.toIntOrNull() ?: 0).coerceIn(0, 59)
    return h to m
}

@Composable
private fun MedicationFormDialog(
    initial: Medication?,
    defaultWarningDays: Int,
    onSave: (Medication) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf(initial?.name ?: "") }
    var imageUri by remember { mutableStateOf(initial?.imageUri ?: "") }
    var usageType by remember { mutableStateOf(initial?.usageType ?: UsageType.AFTER_MEAL) }
    var customUsage by remember { mutableStateOf(initial?.customUsage ?: "") }
    var doseType by remember { mutableStateOf(initial?.doseType ?: DoseType.MORNING_NOON_NIGHT) }
    var isScheduled by remember { mutableStateOf(initial?.isScheduled ?: true) }
    var isOccasional by remember { mutableStateOf(initial?.isOccasional ?: false) }

    var times by remember { mutableStateOf(initTimeList(initial)) }
    var interval by remember { mutableStateOf((initial?.intervalHours ?: 8).toString()) }
    var intervalStart by remember { mutableStateOf(initial?.intervalStartTime ?: "08:00") }
    var timesPerDay by remember { mutableStateOf((initial?.timesPerDay ?: 1).toString()) }

    var dosePerIntake by remember { mutableStateOf(if (initial != null && initial.dosePerIntake > 0.0) formatNum(initial.dosePerIntake) else "1") }
    var quantity by remember { mutableStateOf(if (initial != null && initial.quantity >= 0) formatNum(initial.quantity) else "") }
    var minKeep by remember { mutableStateOf(if (initial != null && initial.minKeepQuantity > 0) formatNum(initial.minKeepQuantity) else "") }
    var warningDays by remember { mutableStateOf((initial?.warningDays ?: defaultWarningDays).toString()) }
    var note by remember { mutableStateOf(initial?.note ?: "") }
    var reminderText by remember { mutableStateOf(initial?.reminderText ?: "") }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            scope.launch {
                // 复制进应用私有目录，避免长期持有相册照片的访问授权
                val saved = withContext(Dispatchers.IO) { copyImageToPrivate(context, uri) }
                imageUri = saved ?: uri.toString()
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(dismissOnClickOutside = false)
    ) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = Color.White,
            modifier = Modifier.fillMaxWidth().fillMaxHeight(0.92f)
        ) {
            Column(Modifier.padding(16.dp)) {
                Text(
                    if (initial == null) stringResource(R.string.manage_add_medication) else stringResource(R.string.manage_edit_medication),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = TextDark
                )
                Spacer(Modifier.height(12.dp))

                Column(
                    Modifier.weight(1f).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text(stringResource(R.string.manage_field_name)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        MedicationImage(imageUri, 56.dp)
                        Spacer(Modifier.width(12.dp))
                        OutlinedButton(onClick = {
                            picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                        }) {
                            Text(
                                if (imageUri.isEmpty()) stringResource(R.string.manage_action_pick_image)
                                else stringResource(R.string.manage_action_change_image)
                            )
                        }
                        if (imageUri.isNotEmpty()) {
                            Spacer(Modifier.width(8.dp))
                            TextButton(onClick = { imageUri = "" }) { Text(stringResource(R.string.manage_action_remove)) }
                        }
                    }

                    Text(stringResource(R.string.manage_section_usage), color = TextDark, style = MaterialTheme.typography.labelLarge)
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        UsageChip(stringResource(R.string.usage_before_meal), usageType == UsageType.BEFORE_MEAL) { usageType = UsageType.BEFORE_MEAL }
                        UsageChip(stringResource(R.string.usage_with_meal), usageType == UsageType.WITH_MEAL) { usageType = UsageType.WITH_MEAL }
                        UsageChip(stringResource(R.string.usage_after_meal), usageType == UsageType.AFTER_MEAL) { usageType = UsageType.AFTER_MEAL }
                        UsageChip(stringResource(R.string.usage_patch), usageType == UsageType.PATCH) { usageType = UsageType.PATCH }
                        UsageChip(stringResource(R.string.usage_oral), usageType == UsageType.ORAL) { usageType = UsageType.ORAL }
                        UsageChip(stringResource(R.string.usage_injection), usageType == UsageType.INJECTION) { usageType = UsageType.INJECTION }
                        UsageChip(stringResource(R.string.usage_inhalation), usageType == UsageType.INHALATION) { usageType = UsageType.INHALATION }
                        UsageChip(stringResource(R.string.usage_external), usageType == UsageType.EXTERNAL) { usageType = UsageType.EXTERNAL }
                        UsageChip(stringResource(R.string.usage_custom), usageType == UsageType.CUSTOM) { usageType = UsageType.CUSTOM }
                    }
                    if (usageType == UsageType.CUSTOM) {
                        OutlinedTextField(
                            value = customUsage,
                            onValueChange = { customUsage = it },
                            label = { Text(stringResource(R.string.manage_field_custom_usage)) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(stringResource(R.string.dose_scheduled), color = TextDark)
                        Spacer(Modifier.weight(1f))
                        Switch(checked = isScheduled, onCheckedChange = { isScheduled = it })
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(stringResource(R.string.dose_occasional), color = TextDark)
                        Spacer(Modifier.weight(1f))
                        Switch(checked = isOccasional, onCheckedChange = { isOccasional = it })
                    }

                    if (isScheduled) {
                        Text(stringResource(R.string.manage_section_frequency), color = TextDark, style = MaterialTheme.typography.labelLarge)
                        Row(
                            modifier = Modifier.horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            UsageChip(stringResource(R.string.manage_dose_type_morning_noon_night), doseType == DoseType.MORNING_NOON_NIGHT) { doseType = DoseType.MORNING_NOON_NIGHT }
                            UsageChip(stringResource(R.string.manage_dose_type_every_hours), doseType == DoseType.EVERY_HOURS) { doseType = DoseType.EVERY_HOURS }
                            UsageChip(stringResource(R.string.manage_dose_type_times_per_day), doseType == DoseType.TIMES_PER_DAY) { doseType = DoseType.TIMES_PER_DAY }
                            UsageChip(stringResource(R.string.usage_custom), doseType == DoseType.CUSTOM) { doseType = DoseType.CUSTOM }
                        }

                        when (doseType) {
                            DoseType.MORNING_NOON_NIGHT -> {
                                val labels = listOf(
                                    stringResource(R.string.manage_label_time_morning),
                                    stringResource(R.string.manage_label_time_noon),
                                    stringResource(R.string.manage_label_time_night)
                                )
                                labels.forEachIndexed { i, label ->
                                    WheelTimeField(label, times.getOrElse(i) { "08:00" }) { v ->
                                        times = setAt(times, i, v)
                                    }
                                }
                            }
                            DoseType.EVERY_HOURS -> {
                                NumberField(stringResource(R.string.manage_field_every_hours), interval, { interval = it })
                                WheelTimeField(stringResource(R.string.manage_field_start_time), intervalStart) { intervalStart = it }
                            }
                            DoseType.TIMES_PER_DAY -> {
                                NumberField(stringResource(R.string.manage_field_times_per_day), timesPerDay, { v ->
                                    timesPerDay = v
                                    times = resizeList(times, v.toIntOrNull() ?: 1)
                                })
                                val count = (timesPerDay.toIntOrNull() ?: 1).coerceIn(1, 12)
                                (0 until count).forEach { i ->
                                    WheelTimeField(stringResource(R.string.manage_field_time_index, i + 1), times.getOrElse(i) { defaultTimeList(count)[i] }) { v ->
                                        times = setAt(times, i, v)
                                    }
                                }
                            }
                            DoseType.CUSTOM -> {
                                times.forEachIndexed { i, t ->
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(Modifier.weight(1f)) {
                                            WheelTimeField(stringResource(R.string.manage_field_time_number, i + 1), t) { v -> times = setAt(times, i, v) }
                                        }
                                        IconButton(onClick = { times = times.filterIndexed { idx, _ -> idx != i } }) {
                                            Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.action_delete), tint = WarningRed)
                                        }
                                    }
                                }
                                OutlinedButton(onClick = { times = times + "08:00" }) {
                                    Text(stringResource(R.string.manage_action_add_time))
                                }
                            }
                        }
                    }

                    NumberField(stringResource(R.string.manage_field_dose_per_intake), dosePerIntake, { dosePerIntake = it }, decimal = true)
                    QuantityStepperField(stringResource(R.string.manage_field_total_quantity), quantity, { quantity = it })
                    NumberField(stringResource(R.string.manage_field_min_keep), minKeep, { minKeep = it }, decimal = true)
                    NumberField(stringResource(R.string.manage_field_warning_days), warningDays, { warningDays = it })
                    OutlinedTextField(
                        value = note,
                        onValueChange = { note = it },
                        label = { Text(stringResource(R.string.manage_field_note)) },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = reminderText,
                        onValueChange = { reminderText = it },
                        label = { Text(stringResource(R.string.manage_field_reminder_text)) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) { Text(stringResource(R.string.action_cancel)) }
                    Button(
                        onClick = {
                            onSave(
                                buildMedication(
                                    initial, name, imageUri, usageType, customUsage,
                                    doseType, isScheduled, isOccasional, interval, intervalStart, timesPerDay, times,
                                    dosePerIntake, quantity, minKeep, warningDays, note, reminderText
                                )
                            )
                        },
                        modifier = Modifier.weight(1f),
                        enabled = name.isNotBlank()
                    ) { Text(stringResource(R.string.manage_action_save)) }
                }
            }
        }
    }
}

@Composable
private fun UsageChip(label: String, selected: Boolean, onClick: () -> Unit) {
    FilterChip(selected = selected, onClick = onClick, label = { Text(label) })
}

@Composable
private fun NumberField(label: String, value: String, onChange: (String) -> Unit, decimal: Boolean = false) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
        keyboardOptions = KeyboardOptions(keyboardType = if (decimal) KeyboardType.Decimal else KeyboardType.Number)
    )
}

/** 药品总量输入：文本框两侧带 −/+ 快捷增减按钮（点按步进 1，长按弹出自定义增减量） */
@Composable
private fun QuantityStepperField(label: String, value: String, onChange: (String) -> Unit) {
    var stepPrompt by remember { mutableStateOf<Boolean?>(null) } // null=关闭，true=增加，false=减少
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            QuantityStepButton("−", onLongPress = { stepPrompt = false }) {
                val current = value.toDoubleOrNull()
                if (current != null) onChange(formatNum((current - 1.0).coerceAtLeast(0.0)))
            }
            Spacer(Modifier.width(8.dp))
            OutlinedTextField(
                value = value,
                onValueChange = onChange,
                label = { Text(label) },
                singleLine = true,
                modifier = Modifier.weight(1f),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
            )
            Spacer(Modifier.width(8.dp))
            QuantityStepButton("+", onLongPress = { stepPrompt = true }) {
                val current = value.toDoubleOrNull() ?: 0.0
                onChange(formatNum(current + 1.0))
            }
        }
        Spacer(Modifier.height(2.dp))
        Text(
            stringResource(R.string.manage_quantity_step_hint),
            color = TextSub,
            fontSize = 11.sp
        )
    }
    stepPrompt?.let { increasing ->
        QuantityStepDialog(
            increasing = increasing,
            onConfirm = { amount ->
                val current = value.toDoubleOrNull() ?: 0.0
                val next = if (increasing) current + amount else (current - amount).coerceAtLeast(0.0)
                onChange(formatNum(next))
                stepPrompt = null
            },
            onDismiss = { stepPrompt = null }
        )
    }
}

@Composable
private fun QuantityStepDialog(
    increasing: Boolean,
    onConfirm: (Double) -> Unit,
    onDismiss: () -> Unit
) {
    var amount by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(28.dp),
        title = {
            Text(
                stringResource(if (increasing) R.string.manage_quantity_step_increase else R.string.manage_quantity_step_decrease),
                fontWeight = FontWeight.Bold,
                color = TextDark
            )
        },
        text = {
            OutlinedTextField(
                value = amount,
                onValueChange = { amount = it },
                label = { Text(stringResource(R.string.manage_quantity_step_amount)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
            )
        },
        confirmButton = {
            TextButton(
                onClick = { amount.toDoubleOrNull()?.takeIf { it > 0.0 }?.let(onConfirm) },
                enabled = amount.toDoubleOrNull()?.let { it > 0.0 } == true
            ) { Text(stringResource(R.string.action_ok)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        }
    )
}

private val wheelItemHeight = 40.dp

@Composable
private fun WheelTimeField(label: String, value: String, onChange: (String) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Box(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFFF1F8F4))
            .clickable { open = true }
            .padding(14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(label, color = TextSub)
            Spacer(Modifier.weight(1f))
            Text(value, color = TextDark, fontWeight = FontWeight.Bold)
        }
    }
    if (open) {
        val (h, m) = parseHm(value)
        WheelTimeDialog(h, m, onConfirm = { onChange(it); open = false }, onDismiss = { open = false })
    }
}

@Composable
private fun WheelTimeDialog(
    initialHour: Int,
    initialMinute: Int,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var hour by remember { mutableStateOf(initialHour) }
    var minute by remember { mutableStateOf(initialMinute) }
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(28.dp),
        title = { Text(stringResource(R.string.manage_dialog_pick_time), fontWeight = FontWeight.Bold, color = TextDark) },
        text = {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                WheelPicker((0..23).toList(), hour) { hour = it }
                Text(
                    ":",
                    color = TextDark,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    modifier = Modifier.padding(horizontal = 6.dp)
                )
                WheelPicker((0..59).toList(), minute) { minute = it }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm("%02d:%02d".format(hour, minute)) }) { Text(stringResource(R.string.action_ok)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        }
    )
}

@Composable
private fun WheelPicker(items: List<Int>, initial: Int, modifier: Modifier = Modifier, onSelect: (Int) -> Unit) {
    val initIndex = items.indexOf(initial).takeIf { it >= 0 } ?: 0
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = initIndex)
    val fling = rememberSnapFlingBehavior(lazyListState = listState)

    Box(modifier.width(72.dp).height(wheelItemHeight * 3)) {
        LazyColumn(state = listState, flingBehavior = fling, modifier = Modifier.fillMaxSize()) {
            item { Spacer(Modifier.height(wheelItemHeight)) }
            items(items.size) { i ->
                val isCenter = listState.firstVisibleItemIndex == i
                val color by animateColorAsState(
                    targetValue = if (isCenter) PrimaryGreen else TextSub,
                    animationSpec = tween(150),
                    label = "wheelItemColor"
                )
                val size by animateFloatAsState(
                    targetValue = if (isCenter) 20f else 16f,
                    animationSpec = tween(150),
                    label = "wheelItemSize"
                )
                Box(Modifier.height(wheelItemHeight).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text(
                        "%02d".format(items[i]),
                        fontSize = size.sp,
                        color = color,
                        fontWeight = if (isCenter) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
            item { Spacer(Modifier.height(wheelItemHeight)) }
        }
        Box(
            Modifier
                .align(Alignment.Center)
                .fillMaxWidth()
                .height(wheelItemHeight)
                .clip(RoundedCornerShape(8.dp))
                .background(PrimaryGreen.copy(alpha = 0.12f))
        )
    }
    val centerIndex = listState.firstVisibleItemIndex.coerceIn(0, items.size - 1)
    LaunchedEffect(centerIndex) { onSelect(items[centerIndex]) }
}

private fun buildMedication(
    initial: Medication?,
    name: String,
    imageUri: String,
    usageType: UsageType,
    customUsage: String,
    doseType: DoseType,
    isScheduled: Boolean,
    isOccasional: Boolean,
    interval: String,
    intervalStart: String,
    timesPerDay: String,
    times: List<String>,
    dosePerIntake: String,
    quantity: String,
    minKeep: String,
    warningDays: String,
    note: String,
    reminderText: String
): Medication {
    val doseTimes = when (doseType) {
        DoseType.MORNING_NOON_NIGHT -> times.take(3).joinToString(",")
        DoseType.TIMES_PER_DAY -> times.take((timesPerDay.toIntOrNull() ?: 1).coerceAtLeast(1)).joinToString(",")
        DoseType.CUSTOM -> times.joinToString(",")
        DoseType.EVERY_HOURS -> ""
    }
    return Medication(
        id = initial?.id ?: 0,
        name = name.trim(),
        imageUri = imageUri,
        usageType = usageType,
        customUsage = customUsage.trim(),
        doseType = doseType,
        doseTimes = doseTimes,
        intervalHours = interval.toIntOrNull() ?: 0,
        intervalStartTime = intervalStart,
        timesPerDay = timesPerDay.toIntOrNull() ?: 1,
        isScheduled = isScheduled,
        isOccasional = isOccasional,
        dosePerIntake = dosePerIntake.toDoubleOrNull() ?: 1.0,
        quantity = quantity.toDoubleOrNull() ?: Medication.UNKNOWN_QUANTITY,
        minKeepQuantity = minKeep.toDoubleOrNull() ?: 0.0,
        warningDays = warningDays.toIntOrNull() ?: 3,
        enabled = initial?.enabled ?: true,
        note = note.trim(),
        reminderText = reminderText.trim(),
        createdAt = initial?.createdAt ?: 0L
    )
}

/** 把选中的相册图片复制到应用私有目录，返回文件 URI；失败返回 null */
private fun copyImageToPrivate(context: Context, source: Uri): String? = runCatching {
    val dir = File(context.filesDir, "med_images").apply { mkdirs() }
    val dest = File(dir, "${UUID.randomUUID()}.jpg")
    context.contentResolver.openInputStream(source)?.use { input ->
        dest.outputStream().use { output -> input.copyTo(output) }
    } ?: return@runCatching null
    Uri.fromFile(dest).toString()
}.getOrNull()

private fun initTimeList(initial: Medication?): List<String> {
    if (initial == null) return listOf("08:00", "12:00", "18:00")
    val parsed = parseTimes(initial.doseTimes).map { fmtTime(it) }
    if (parsed.isNotEmpty()) return parsed
    return when (initial.doseType) {
        DoseType.CUSTOM -> emptyList()
        DoseType.TIMES_PER_DAY -> defaultTimeList(initial.timesPerDay.coerceAtLeast(1))
        else -> listOf("08:00", "12:00", "18:00")
    }
}

private fun defaultTimeList(n: Int): List<String> {
    val count = n.coerceAtLeast(1)
    val start = 8 * 60
    val step = if (count > 1) (24 * 60) / count else 0
    return (0 until count).map { i ->
        val total = (start + i * step) % (24 * 60)
        "%02d:%02d".format(total / 60, total % 60)
    }
}

private fun resizeList(list: List<String>, n: Int): List<String> {
    val count = n.coerceAtLeast(1)
    if (list.size >= count) return list.take(count)
    return list + (list.size until count).map { i -> defaultTimeList(count)[i] }
}

private fun setAt(list: List<String>, i: Int, v: String): List<String> =
    list.toMutableList().also {
        if (i < it.size) it[i] = v else it.add(v)
    }