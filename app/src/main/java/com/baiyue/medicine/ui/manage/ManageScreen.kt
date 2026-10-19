package com.baiyue.medicine.ui.manage

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import com.baiyue.medicine.R
import com.baiyue.medicine.data.entity.Medication
import com.baiyue.medicine.model.DoseType
import com.baiyue.medicine.model.UsageType
import com.baiyue.medicine.prediction.parseTimes
import com.baiyue.medicine.prediction.predict
import com.baiyue.medicine.ui.components.AppCard
import com.baiyue.medicine.ui.components.AppGlyph
import com.baiyue.medicine.ui.components.AppPill
import com.baiyue.medicine.ui.components.EmptyState
import com.baiyue.medicine.ui.components.GlyphIcon
import com.baiyue.medicine.ui.components.MedicationAvatar
import com.baiyue.medicine.ui.components.ScreenHeader
import com.baiyue.medicine.ui.components.SectionHeader
import com.baiyue.medicine.ui.components.TintedGlyph
import com.baiyue.medicine.ui.doseLabel
import com.baiyue.medicine.ui.formatNum
import com.baiyue.medicine.ui.usageLabel
import com.baiyue.medicine.ui.theme.BorderSoft
import com.baiyue.medicine.ui.theme.BorderStrong
import com.baiyue.medicine.ui.theme.CardWhite
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID
import kotlin.math.ceil

@Composable
fun ManageScreen(
    onOpenSettings: () -> Unit = {},
    vm: ManageViewModel = viewModel()
) {
    val meds by vm.medications.collectAsState()
    val defaultWarningDays by vm.defaultWarningDays.collectAsState()
    val showImage by vm.showImage.collectAsState()
    var editing by remember { mutableStateOf<Medication?>(null) }
    var showForm by remember { mutableStateOf(false) }
    var deleting by remember { mutableStateOf<Medication?>(null) }

    Box(
        Modifier
            .fillMaxSize()
            .statusBarsPadding()
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 14.dp, bottom = 116.dp),
            verticalArrangement = Arrangement.spacedBy(13.dp)
        ) {
            item(key = "header") {
                ScreenHeader(
                    eyebrow = stringResource(R.string.ui_manage_eyebrow),
                    title = stringResource(R.string.manage_title),
                    subtitle = stringResource(R.string.ui_manage_subtitle),
                    onOpenSettings = onOpenSettings
                )
            }

            item(key = "images") {
                ImageVisibilityRow(
                    checked = showImage,
                    onCheckedChange = { vm.setShowImage(it) }
                )
            }

            if (meds.isEmpty()) {
                item(key = "empty") {
                    AppCard(
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        EmptyState(
                            glyph = AppGlyph.Pill,
                            title = stringResource(R.string.manage_empty_hint),
                            action = {
                                Button(
                                    onClick = { editing = null; showForm = true },
                                    shape = RoundedCornerShape(15.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen)
                                ) {
                                    GlyphIcon(AppGlyph.Plus, tint = Color.White, size = 16.dp)
                                    Spacer(Modifier.width(6.dp))
                                    Text(stringResource(R.string.ui_add_first_medicine), fontWeight = FontWeight.Bold)
                                }
                            }
                        )
                    }
                }
            } else {
                item(key = "listHeader") {
                    SectionHeader(
                        title = stringResource(R.string.ui_manage_list),
                        trailing = {
                            AppPill(
                                stringResource(R.string.card_rx_count, meds.size),
                                glyph = AppGlyph.Pill
                            )
                        }
                    )
                }

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

        FloatingActionButton(
            onClick = { editing = null; showForm = true },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 20.dp, bottom = 20.dp),
            containerColor = PrimaryGreen,
            contentColor = Color.White,
            shape = RoundedCornerShape(19.dp),
            elevation = FloatingActionButtonDefaults.elevation(
                defaultElevation = 6.dp,
                pressedElevation = 2.dp
            )
        ) {
            GlyphIcon(
                AppGlyph.Plus,
                tint = Color.White,
                size = 24.dp,
                contentDescription = stringResource(R.string.manage_add_medication)
            )
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
            shape = RoundedCornerShape(30.dp),
            containerColor = CardWhite,
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TintedGlyph(AppGlyph.Trash, tint = WarningRed, container = WarningSoft, size = 38.dp)
                    Spacer(Modifier.width(10.dp))
                    Text(
                        stringResource(R.string.manage_delete_title),
                        fontWeight = FontWeight.Bold,
                        color = TextDark,
                        fontSize = 19.sp
                    )
                }
            },
            text = { Text(stringResource(R.string.manage_delete_message, med.name), color = TextSub) },
            confirmButton = {
                TextButton(onClick = { vm.delete(med); deleting = null }) {
                    Text(
                        stringResource(R.string.action_delete),
                        color = WarningRed,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { deleting = null }) { Text(stringResource(R.string.action_cancel)) }
            }
        )
    }
}

@Composable
private fun ImageVisibilityRow(checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(CardWhite.copy(alpha = 0.62f))
            .border(1.dp, BorderSoft, RoundedCornerShape(18.dp))
            .padding(horizontal = 13.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        TintedGlyph(AppGlyph.Image, size = 34.dp)
        Spacer(Modifier.width(10.dp))
        Text(
            stringResource(R.string.ui_show_image),
            color = TextDark,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(1f)
        )
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MedicationCard(
    med: Medication,
    showImage: Boolean,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onAdjustQuantity: (Double) -> Unit,
    modifier: Modifier = Modifier
) {
    var showActions by remember { mutableStateOf(false) }
    val lowStock = med.quantity >= 0.0 && med.quantity <= med.minKeepQuantity
    val outOfStock = med.quantity <= 0.0 && med.quantity >= 0.0
    val prediction = predictionText(med)
    val predictionColor = when {
        med.quantity < 0 -> TextSub
        !med.isScheduled -> TextSub
        lowStock -> WarningRed
        else -> CheckGreen
    }

    AppCard(
        modifier = modifier
            .fillMaxWidth()
            .animateContentSize(),
        contentPadding = PaddingValues(0.dp)
    ) {
        Column(Modifier.padding(start = 15.dp, end = 13.dp, top = 14.dp, bottom = 14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                MedicationAvatar(
                    name = med.name,
                    imageUri = med.imageUri,
                    showImage = showImage,
                    size = 56.dp
                )
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        med.name,
                        color = TextDark,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (med.isOccasional || lowStock) {
                        Spacer(Modifier.height(5.dp))
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(5.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            if (med.isOccasional) {
                                AppPill(
                                    stringResource(R.string.manage_occasional_badge),
                                    color = OccasionalOrange,
                                    containerColor = WarmTint
                                )
                            }
                            if (lowStock) {
                                AppPill(
                                    stringResource(if (outOfStock) R.string.manage_out_of_stock else R.string.manage_low_stock),
                                    color = WarningRed,
                                    containerColor = WarningSoft
                                )
                            }
                        }
                    }
                }
                Spacer(Modifier.width(6.dp))
                Box(
                    Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(if (showActions) MintTint else MintWash)
                        .clickable { showActions = !showActions },
                    contentAlignment = Alignment.Center
                ) {
                    GlyphIcon(
                        if (showActions) AppGlyph.ChevronUp else AppGlyph.ChevronDown,
                        tint = PrimaryGreen,
                        size = 18.dp,
                        contentDescription = stringResource(
                            if (showActions) R.string.manage_action_collapse else R.string.manage_action_manage
                        )
                    )
                }
            }

            Spacer(Modifier.height(10.dp))

            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(13.dp))
                    .background(MintWash.copy(alpha = 0.75f))
                    .padding(horizontal = 11.dp, vertical = 8.dp)
            ) {
                GlyphIcon(AppGlyph.Clock, tint = PrimaryGreen, size = 13.dp)
                Spacer(Modifier.width(6.dp))
                Text(
                    doseLabel(med),
                    color = TextSub,
                    fontSize = 11.5.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(Modifier.height(7.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(5.dp),
                verticalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                AppPill(usageLabel(med), glyph = AppGlyph.Leaf)
                AppPill(stringResource(R.string.label_intake, formatNum(med.dosePerIntake)), glyph = AppGlyph.Pill)
            }

            if (med.note.isNotBlank()) {
                Spacer(Modifier.height(9.dp))
                Text(
                    "“${med.note}”",
                    color = TextSub,
                    fontSize = 11.5.sp,
                    fontStyle = FontStyle.Italic,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(Modifier.height(10.dp))

            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(15.dp))
                    .background(
                        if (lowStock) {
                            Brush.linearGradient(listOf(WarningSoft, WarningSoft.copy(alpha = 0.55f)))
                        } else {
                            Brush.linearGradient(listOf(MintWash, Color(0xFFF7FBF7)))
                        }
                    )
                    .padding(horizontal = 12.dp, vertical = 9.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        stringResource(R.string.ui_stock_left),
                        color = TextSub,
                        fontSize = 9.5.sp,
                        letterSpacing = 0.3.sp
                    )
                    Text(
                        if (med.quantity < 0) stringResource(R.string.value_not_set)
                        else formatNum(med.quantity),
                        color = if (lowStock) WarningRed else TextDark,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Text(
                    prediction,
                    color = predictionColor,
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.End,
                    maxLines = 2,
                    modifier = Modifier.fillMaxWidth(0.58f)
                )
            }

            AnimatedVisibility(
                visible = showActions,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column {
                    Spacer(Modifier.height(12.dp))
                    HorizontalDivider(color = BorderSoft)
                    Spacer(Modifier.height(10.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                stringResource(R.string.manage_label_total),
                                color = TextSub,
                                fontSize = 10.5.sp
                            )
                            Text(
                                if (med.quantity < 0) stringResource(R.string.value_not_set)
                                else formatNum(med.quantity),
                                color = TextDark,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        QuantityStepButton("−") { onAdjustQuantity(-1.0) }
                        Spacer(Modifier.width(8.dp))
                        QuantityStepButton("+") { onAdjustQuantity(1.0) }
                    }
                    Spacer(Modifier.height(6.dp))
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(onClick = onDelete) {
                            GlyphIcon(AppGlyph.Trash, tint = WarningRed, size = 14.dp)
                            Spacer(Modifier.width(4.dp))
                            Text(
                                stringResource(R.string.action_delete),
                                color = WarningRed,
                                fontSize = 12.5.sp
                            )
                        }
                        Spacer(Modifier.width(4.dp))
                        Button(
                            onClick = onEdit,
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 7.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen)
                        ) {
                            GlyphIcon(AppGlyph.Edit, tint = Color.White, size = 14.dp)
                            Spacer(Modifier.width(5.dp))
                            Text(
                                stringResource(R.string.action_edit),
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun predictionText(med: Medication): String {
    if (!med.isScheduled) return stringResource(R.string.manage_take_as_needed)
    val prediction = predict(med)
    if (prediction == null) {
        return if (med.quantity < 0) {
            stringResource(R.string.manage_no_quantity_no_prediction)
        } else {
            stringResource(R.string.manage_total_quantity, formatNum(med.quantity))
        }
    }
    val days = ceil(prediction.daysRemaining).toInt()
    return if (prediction.needVisit) {
        stringResource(R.string.manage_remaining_visit_reminder, formatNum(med.quantity))
    } else if (days <= 1) {
        stringResource(R.string.manage_remaining_day, formatNum(med.quantity))
    } else {
        stringResource(R.string.manage_remaining_days, formatNum(med.quantity), days)
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
    val shape = CircleShape
    Box(
        modifier
            .size(40.dp)
            .clip(shape)
            .background(MintTint)
            .border(1.dp, BorderStrong.copy(alpha = 0.7f), shape)
            .combinedClickable(onClick = onClick, onLongClick = onLongPress),
        contentAlignment = Alignment.Center
    ) {
        Text(label, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = PrimaryGreen)
    }
}

private fun fmtTime(p: Pair<Int, Int>): String = "%02d:%02d".format(p.first, p.second)

private fun parseHm(value: String): Pair<Int, Int> {
    val parts = value.split(":")
    val h = (parts.getOrNull(0)?.toIntOrNull() ?: 8).coerceIn(0, 23)
    val m = (parts.getOrNull(1)?.toIntOrNull() ?: 0).coerceIn(0, 59)
    return h to m
}

@OptIn(ExperimentalLayoutApi::class)
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

    var dosePerIntake by remember {
        mutableStateOf(if (initial != null && initial.dosePerIntake > 0.0) formatNum(initial.dosePerIntake) else "1")
    }
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
            shape = RoundedCornerShape(30.dp),
            color = Color(0xFFFBFDFB),
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.93f)
                .imePadding()
        ) {
            Column(Modifier.fillMaxSize()) {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 12.dp)
                ) {
                    Box(
                        Modifier
                            .width(38.dp)
                            .height(4.dp)
                            .clip(CircleShape)
                            .background(BorderStrong)
                            .align(Alignment.CenterHorizontally)
                    )
                    Spacer(Modifier.height(13.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        TintedGlyph(AppGlyph.Pill, size = 40.dp)
                        Spacer(Modifier.width(11.dp))
                        Text(
                            stringResource(
                                if (initial == null) R.string.manage_add_medication
                                else R.string.manage_edit_medication
                            ),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = TextDark,
                            modifier = Modifier.weight(1f)
                        )
                        Box(
                            Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(MintWash)
                                .clickable(onClick = onDismiss),
                            contentAlignment = Alignment.Center
                        ) {
                            GlyphIcon(
                                AppGlyph.Close,
                                tint = TextSub,
                                size = 17.dp,
                                contentDescription = stringResource(R.string.action_close)
                            )
                        }
                    }
                }

                HorizontalDivider(color = BorderSoft)

                Column(
                    Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    FormSection(
                        glyph = AppGlyph.Info,
                        title = stringResource(R.string.ui_section_basic)
                    ) {
                        FormTextField(
                            label = stringResource(R.string.manage_field_name),
                            value = name,
                            onValueChange = { name = it }
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            MedicationAvatar(
                                name = name,
                                imageUri = imageUri,
                                showImage = true,
                                size = 64.dp
                            )
                            Spacer(Modifier.width(13.dp))
                            Column(Modifier.weight(1f)) {
                                OutlinedButton(
                                    onClick = {
                                        picker.launch(
                                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                        )
                                    },
                                    shape = RoundedCornerShape(13.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 7.dp)
                                ) {
                                    GlyphIcon(AppGlyph.Image, tint = PrimaryGreen, size = 15.dp)
                                    Spacer(Modifier.width(5.dp))
                                    Text(
                                        stringResource(
                                            if (imageUri.isEmpty()) R.string.manage_action_pick_image
                                            else R.string.manage_action_change_image
                                        ),
                                        fontSize = 11.5.sp
                                    )
                                }
                                if (imageUri.isNotEmpty()) {
                                    TextButton(
                                        onClick = { imageUri = "" },
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            stringResource(R.string.manage_action_remove),
                                            color = TextSub,
                                            fontSize = 11.sp
                                        )
                                    }
                                }
                            }
                        }
                    }

                    HorizontalDivider(color = BorderSoft)

                    FormSection(
                        glyph = AppGlyph.Leaf,
                        title = stringResource(R.string.manage_section_usage)
                    ) {
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(7.dp),
                            verticalArrangement = Arrangement.spacedBy(7.dp)
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
                            FormTextField(
                                label = stringResource(R.string.manage_field_custom_usage),
                                value = customUsage,
                                onValueChange = { customUsage = it }
                            )
                        }
                    }

                    HorizontalDivider(color = BorderSoft)

                    FormSection(
                        glyph = AppGlyph.Clock,
                        title = stringResource(R.string.manage_section_frequency)
                    ) {
                        SettingSwitchRow(
                            title = stringResource(R.string.dose_scheduled),
                            checked = isScheduled,
                            onCheckedChange = { isScheduled = it }
                        )
                        SettingSwitchRow(
                            title = stringResource(R.string.dose_occasional),
                            checked = isOccasional,
                            onCheckedChange = { isOccasional = it }
                        )

                        if (isScheduled) {
                            Spacer(Modifier.height(2.dp))
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(7.dp),
                                verticalArrangement = Arrangement.spacedBy(7.dp)
                            ) {
                                UsageChip(
                                    stringResource(R.string.manage_dose_type_morning_noon_night),
                                    doseType == DoseType.MORNING_NOON_NIGHT
                                ) { doseType = DoseType.MORNING_NOON_NIGHT }
                                UsageChip(
                                    stringResource(R.string.manage_dose_type_every_hours),
                                    doseType == DoseType.EVERY_HOURS
                                ) { doseType = DoseType.EVERY_HOURS }
                                UsageChip(
                                    stringResource(R.string.manage_dose_type_times_per_day),
                                    doseType == DoseType.TIMES_PER_DAY
                                ) { doseType = DoseType.TIMES_PER_DAY }
                                UsageChip(
                                    stringResource(R.string.usage_custom),
                                    doseType == DoseType.CUSTOM
                                ) { doseType = DoseType.CUSTOM }
                            }

                            when (doseType) {
                                DoseType.MORNING_NOON_NIGHT -> {
                                    val labels = listOf(
                                        stringResource(R.string.manage_label_time_morning),
                                        stringResource(R.string.manage_label_time_noon),
                                        stringResource(R.string.manage_label_time_night)
                                    )
                                    labels.forEachIndexed { index, label ->
                                        WheelTimeField(label, times.getOrElse(index) { "08:00" }) { value ->
                                            times = setAt(times, index, value)
                                        }
                                    }
                                }

                                DoseType.EVERY_HOURS -> {
                                    NumberField(
                                        stringResource(R.string.manage_field_every_hours),
                                        interval,
                                        { interval = it }
                                    )
                                    WheelTimeField(stringResource(R.string.manage_field_start_time), intervalStart) {
                                        intervalStart = it
                                    }
                                }

                                DoseType.TIMES_PER_DAY -> {
                                    NumberField(
                                        stringResource(R.string.manage_field_times_per_day),
                                        timesPerDay,
                                        { value ->
                                            timesPerDay = value
                                            times = resizeList(times, value.toIntOrNull() ?: 1)
                                        }
                                    )
                                    val count = (timesPerDay.toIntOrNull() ?: 1).coerceIn(1, 12)
                                    (0 until count).forEach { index ->
                                        WheelTimeField(
                                            stringResource(R.string.manage_field_time_index, index + 1),
                                            times.getOrElse(index) { defaultTimeList(count)[index] }
                                        ) { value ->
                                            times = setAt(times, index, value)
                                        }
                                    }
                                }

                                DoseType.CUSTOM -> {
                                    times.forEachIndexed { index, time ->
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Box(Modifier.weight(1f)) {
                                                WheelTimeField(
                                                    stringResource(R.string.manage_field_time_number, index + 1),
                                                    time
                                                ) { value -> times = setAt(times, index, value) }
                                            }
                                            Spacer(Modifier.width(7.dp))
                                            Box(
                                                Modifier
                                                    .size(38.dp)
                                                    .clip(CircleShape)
                                                    .background(WarningSoft)
                                                    .clickable {
                                                        times = times.filterIndexed { idx, _ -> idx != index }
                                                    },
                                                contentAlignment = Alignment.Center
                                            ) {
                                                GlyphIcon(AppGlyph.Trash, tint = WarningRed, size = 16.dp)
                                            }
                                        }
                                    }
                                    OutlinedButton(
                                        onClick = { times = times + "08:00" },
                                        shape = RoundedCornerShape(13.dp)
                                    ) {
                                        GlyphIcon(AppGlyph.Plus, tint = PrimaryGreen, size = 15.dp)
                                        Spacer(Modifier.width(5.dp))
                                        Text(stringResource(R.string.manage_action_add_time), fontSize = 12.sp)
                                    }
                                }
                            }
                        }
                    }

                    HorizontalDivider(color = BorderSoft)

                    FormSection(
                        glyph = AppGlyph.Pill,
                        title = stringResource(R.string.ui_section_stock)
                    ) {
                        NumberField(
                            stringResource(R.string.manage_field_dose_per_intake),
                            dosePerIntake,
                            { dosePerIntake = it },
                            decimal = true
                        )
                        QuantityStepperField(
                            stringResource(R.string.manage_field_total_quantity),
                            quantity,
                            { quantity = it }
                        )
                        NumberField(
                            stringResource(R.string.manage_field_min_keep),
                            minKeep,
                            { minKeep = it },
                            decimal = true
                        )
                        NumberField(
                            stringResource(R.string.manage_field_warning_days),
                            warningDays,
                            { warningDays = it }
                        )
                    }

                    HorizontalDivider(color = BorderSoft)

                    FormSection(
                        glyph = AppGlyph.Edit,
                        title = stringResource(R.string.ui_form_notes)
                    ) {
                        FormTextField(
                            label = stringResource(R.string.manage_field_note),
                            value = note,
                            onValueChange = { note = it },
                            singleLine = false,
                            minLines = 2
                        )
                        FormTextField(
                            label = stringResource(R.string.manage_field_reminder_text),
                            value = reminderText,
                            onValueChange = { reminderText = it },
                            singleLine = false,
                            minLines = 2
                        )
                    }
                }

                HorizontalDivider(color = BorderSoft)

                Row(
                    Modifier
                        .fillMaxWidth()
                        .background(CardWhite)
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp),
                        shape = RoundedCornerShape(15.dp)
                    ) {
                        Text(stringResource(R.string.action_cancel), fontWeight = FontWeight.SemiBold)
                    }
                    Button(
                        onClick = {
                            onSave(
                                buildMedication(
                                    initial, name, imageUri, usageType, customUsage,
                                    doseType, isScheduled, isOccasional, interval, intervalStart,
                                    timesPerDay, times, dosePerIntake, quantity, minKeep,
                                    warningDays, note, reminderText
                                )
                            )
                        },
                        modifier = Modifier
                            .weight(1.35f)
                            .height(50.dp),
                        shape = RoundedCornerShape(15.dp),
                        enabled = name.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen)
                    ) {
                        GlyphIcon(AppGlyph.Check, tint = Color.White, size = 17.dp)
                        Spacer(Modifier.width(6.dp))
                        Text(
                            stringResource(R.string.manage_action_save),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FormSection(
    glyph: AppGlyph,
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            TintedGlyph(glyph, size = 30.dp)
            Spacer(Modifier.width(9.dp))
            Text(
                title,
                color = TextDark,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(Modifier.height(10.dp))
        Column(verticalArrangement = Arrangement.spacedBy(9.dp), content = content)
    }
}

@Composable
private fun UsageChip(label: String, selected: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(12.dp)
    Box(
        Modifier
            .clip(shape)
            .background(if (selected) MintTint else CardWhite)
            .border(1.dp, if (selected) PrimaryGreen.copy(alpha = 0.55f) else BorderStrong, shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            label,
            color = if (selected) PrimaryGreen else TextSub,
            fontSize = 12.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
        )
    }
}

@Composable
private fun SettingSwitchRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(CardWhite.copy(alpha = 0.68f))
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            title,
            color = TextDark,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(1f)
        )
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun FormTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    singleLine: Boolean = true,
    minLines: Int = 1
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        singleLine = singleLine,
        minLines = minLines,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(15.dp),
        colors = formFieldColors()
    )
}

@Composable
private fun formFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = PrimaryGreen,
    unfocusedBorderColor = BorderStrong,
    focusedContainerColor = CardWhite,
    unfocusedContainerColor = CardWhite,
    focusedLabelColor = PrimaryGreen,
    unfocusedLabelColor = TextSub,
    cursorColor = PrimaryGreen
)

@Composable
private fun NumberField(
    label: String,
    value: String,
    onChange: (String) -> Unit,
    decimal: Boolean = false
) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(15.dp),
        colors = formFieldColors(),
        keyboardOptions = KeyboardOptions(
            keyboardType = if (decimal) KeyboardType.Decimal else KeyboardType.Number
        )
    )
}

/** 药品总量输入：文本框两侧带 −/+ 快捷增减按钮（点按步进 1，长按弹出自定义增减量） */
@Composable
private fun QuantityStepperField(label: String, value: String, onChange: (String) -> Unit) {
    var stepPrompt by remember { mutableStateOf<Boolean?>(null) }
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
                shape = RoundedCornerShape(15.dp),
                colors = formFieldColors(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
            )
            Spacer(Modifier.width(8.dp))
            QuantityStepButton("+", onLongPress = { stepPrompt = true }) {
                val current = value.toDoubleOrNull() ?: 0.0
                onChange(formatNum(current + 1.0))
            }
        }
        Spacer(Modifier.height(4.dp))
        Text(
            stringResource(R.string.manage_quantity_step_hint),
            color = TextFaint,
            fontSize = 10.5.sp
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
        shape = RoundedCornerShape(30.dp),
        containerColor = CardWhite,
        title = {
            Text(
                stringResource(
                    if (increasing) R.string.manage_quantity_step_increase
                    else R.string.manage_quantity_step_decrease
                ),
                fontWeight = FontWeight.Bold,
                color = TextDark,
                fontSize = 19.sp
            )
        },
        text = {
            OutlinedTextField(
                value = amount,
                onValueChange = { amount = it },
                label = { Text(stringResource(R.string.manage_quantity_step_amount)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(15.dp),
                colors = formFieldColors(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
            )
        },
        confirmButton = {
            TextButton(
                onClick = { amount.toDoubleOrNull()?.takeIf { it > 0.0 }?.let(onConfirm) },
                enabled = amount.toDoubleOrNull()?.let { it > 0.0 } == true
            ) {
                Text(stringResource(R.string.action_ok), fontWeight = FontWeight.Bold)
            }
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
            .clip(RoundedCornerShape(15.dp))
            .background(CardWhite)
            .border(1.dp, BorderStrong, RoundedCornerShape(15.dp))
            .clickable { open = true }
            .padding(horizontal = 13.dp, vertical = 13.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            GlyphIcon(AppGlyph.Clock, tint = PrimaryGreen, size = 16.dp)
            Spacer(Modifier.width(8.dp))
            Text(label, color = TextSub, fontSize = 12.5.sp)
            Spacer(Modifier.weight(1f))
            Text(value, color = TextDark, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Spacer(Modifier.width(4.dp))
            GlyphIcon(AppGlyph.ChevronDown, tint = TextFaint, size = 15.dp)
        }
    }
    if (open) {
        val (hour, minute) = parseHm(value)
        WheelTimeDialog(
            initialHour = hour,
            initialMinute = minute,
            onConfirm = { onChange(it); open = false },
            onDismiss = { open = false }
        )
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
        shape = RoundedCornerShape(30.dp),
        containerColor = CardWhite,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                TintedGlyph(AppGlyph.Clock, size = 38.dp)
                Spacer(Modifier.width(10.dp))
                Text(
                    stringResource(R.string.manage_dialog_pick_time),
                    fontWeight = FontWeight.Bold,
                    color = TextDark,
                    fontSize = 19.sp
                )
            }
        },
        text = {
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(MintWash)
                    .padding(vertical = 8.dp),
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
            Button(
                onClick = { onConfirm("%02d:%02d".format(hour, minute)) },
                shape = RoundedCornerShape(13.dp)
            ) {
                Text(stringResource(R.string.action_ok), fontWeight = FontWeight.Bold)
            }
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

    Box(modifier.width(76.dp).height(wheelItemHeight * 3)) {
        LazyColumn(state = listState, flingBehavior = fling, modifier = Modifier.fillMaxSize()) {
            item { Spacer(Modifier.height(wheelItemHeight)) }
            items(items.size) { index ->
                val isCenter = listState.firstVisibleItemIndex == index
                val color = if (isCenter) PrimaryGreen else TextFaint
                val size by animateFloatAsState(
                    targetValue = if (isCenter) 20f else 15f,
                    animationSpec = tween(150),
                    label = "wheelItemSize"
                )
                Box(
                    Modifier
                        .height(wheelItemHeight)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "%02d".format(items[index]),
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
                .clip(RoundedCornerShape(10.dp))
                .background(PrimaryGreen.copy(alpha = 0.09f))
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
    return (0 until count).map { index ->
        val total = (start + index * step) % (24 * 60)
        "%02d:%02d".format(total / 60, total % 60)
    }
}

private fun resizeList(list: List<String>, n: Int): List<String> {
    val count = n.coerceAtLeast(1)
    if (list.size >= count) return list.take(count)
    return list + (list.size until count).map { index -> defaultTimeList(count)[index] }
}

private fun setAt(list: List<String>, i: Int, v: String): List<String> =
    list.toMutableList().also {
        if (i < it.size) it[i] = v else it.add(v)
    }
