package com.baiyue.medicine.ui.checkin

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.baiyue.medicine.R
import com.baiyue.medicine.data.entity.Medication
import com.baiyue.medicine.ui.components.AppCard
import com.baiyue.medicine.ui.components.AppGlyph
import com.baiyue.medicine.ui.components.AppPill
import com.baiyue.medicine.ui.components.EmptyState
import com.baiyue.medicine.ui.components.GlyphIcon
import com.baiyue.medicine.ui.components.HeroDecorations
import com.baiyue.medicine.ui.components.MedicationAvatar
import com.baiyue.medicine.ui.components.ScreenHeader
import com.baiyue.medicine.ui.components.SectionHeader
import com.baiyue.medicine.ui.components.TintedGlyph
import com.baiyue.medicine.ui.intakeLabel
import com.baiyue.medicine.ui.localizedDate
import com.baiyue.medicine.ui.usageLabel
import com.baiyue.medicine.ui.theme.BackgroundMint
import com.baiyue.medicine.ui.theme.BorderStrong
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
import com.baiyue.medicine.ui.theme.WarningRed
import com.baiyue.medicine.ui.theme.WarningSoft
import com.baiyue.medicine.ui.theme.WarmTint
import kotlinx.coroutines.delay
import java.time.LocalDate

private class Feedback(val color: Color, val message: String)

private val pastelColors = listOf(
    Color(0xFFCDECDD), Color(0xFFCDE8F4), Color(0xFFE1EBC8), Color(0xFFF8E7B8),
    Color(0xFFF5D8C8), Color(0xFFDED8F1), Color(0xFFF6D9E4), Color(0xFFD8ECD2)
)

@Composable
fun CheckInScreen(
    onOpenSettings: () -> Unit = {},
    isActive: Boolean = true,
    onOpenManage: () -> Unit = {},
    vm: CheckInViewModel = viewModel()
) {
    val dueItems by vm.dueItems.collectAsState()
    val missedItems by vm.missedItems.collectAsState()
    val occasional by vm.occasionalMeds.collectAsState()
    val nextDose by vm.nextDose.collectAsState()
    val warningMeds by vm.warningMeds.collectAsState()
    val closeDoseWarnings by vm.closeDoseWarnings.collectAsState()
    val canUndo by vm.canUndo.collectAsState()
    val showImage by vm.showImage.collectAsState()
    val hasMedications by vm.hasMedications.collectAsState()

    val praiseMessages = stringArrayResource(R.array.checkin_praise_messages)
    val context = LocalContext.current

    var showOccasionalDialog by remember { mutableStateOf(false) }
    var showBackfillDialog by remember { mutableStateOf(false) }
    var selectedOccasional by remember { mutableStateOf<List<Medication>>(emptyList()) }
    var feedback by remember { mutableStateOf<Feedback?>(null) }
    var warningDismissed by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(feedback) {
        if (feedback != null) {
            delay(1100)
            feedback = null
        }
    }

    val today = LocalDate.now()
    val pending = dueItems.size + selectedOccasional.size

    Box(Modifier.fillMaxSize()) {
        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 14.dp, bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(13.dp)
            ) {
                item(key = "header") {
                    ScreenHeader(
                        eyebrow = localizedDate(today, R.string.common_date_header),
                        title = stringResource(R.string.checkin_title),
                        subtitle = stringResource(R.string.ui_checkin_subtitle),
                        onOpenSettings = onOpenSettings
                    )
                }

                if (missedItems.isNotEmpty()) {
                    item(key = "missed") {
                        MissedBanner(count = missedItems.size, onClick = { showBackfillDialog = true })
                    }
                }

                closeDoseWarnings.firstOrNull()?.let { warning ->
                    item(key = "closeDose") {
                        CloseDoseBanner(warning)
                    }
                }

                if (dueItems.isEmpty() && selectedOccasional.isEmpty()) {
                    val nd = nextDose
                    if (!hasMedications) {
                        item(key = "noMeds") {
                            EmptyState(
                                glyph = AppGlyph.Pill,
                                title = stringResource(R.string.ui_no_meds_title),
                                body = stringResource(R.string.ui_no_meds_body),
                                action = {
                                    Button(
                                        onClick = onOpenManage,
                                        shape = RoundedCornerShape(15.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen)
                                    ) {
                                        GlyphIcon(AppGlyph.Plus, tint = Color.White, size = 16.dp)
                                        Spacer(Modifier.width(6.dp))
                                        Text(
                                            stringResource(R.string.ui_add_first_medicine),
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            )
                        }
                    } else if (nd != null) {
                        item(key = "next") {
                            NextDoseHero(
                                nd = nd,
                                showImage = showImage,
                                onEarly = if (!nd.missed && !nd.tomorrow) {
                                    {
                                        vm.checkInEarly(nd.time, nd.items)
                                        feedback = Feedback(pastelColors.random(), context.getString(R.string.checkin_early_done))
                                    }
                                } else null
                            )
                        }
                    } else {
                        item(key = "empty") {
                            EmptyState(
                                glyph = AppGlyph.Check,
                                title = stringResource(R.string.checkin_no_meds_now),
                                body = stringResource(R.string.ui_all_done_subtitle)
                            )
                        }
                    }

                    if (hasMedications) {
                        item(key = "occasionalShortcut") {
                            OccasionalShortcut(
                                hasMeds = occasional.isNotEmpty(),
                                onClick = { showOccasionalDialog = true }
                            )
                        }
                    }
                } else {
                    item(key = "dueHeader") {
                        SectionHeader(
                            title = stringResource(R.string.ui_today_plan),
                            trailing = {
                                AppPill(
                                    stringResource(R.string.ui_progress_count, pending),
                                    color = PrimaryGreen,
                                    containerColor = MintTint,
                                    glyph = AppGlyph.Clock
                                )
                            }
                        )
                    }

                    items(dueItems, key = { "due-${it.date}-${it.medication.id}-${it.time}" }) { item ->
                        DueMedicationCard(item = item, showImage = showImage)
                    }

                    items(selectedOccasional, key = { "occ-${it.id}" }) { med ->
                        OccasionalSelectedCard(med = med, showImage = showImage)
                    }

                    item(key = "occasionalHint") {
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .clickable { showOccasionalDialog = true }
                                .padding(horizontal = 4.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            GlyphIcon(AppGlyph.Plus, tint = PrimaryGreen, size = 14.dp)
                            Spacer(Modifier.width(6.dp))
                            Text(
                                stringResource(R.string.checkin_occasional),
                                color = PrimaryGreen,
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }

            ActionDock(
                canUndo = canUndo,
                pending = pending,
                recordingOccasional = selectedOccasional.isNotEmpty(),
                occasionalEnabled = hasMedications,
                onUndo = { vm.undoLastCheckIn() },
                onOccasional = { showOccasionalDialog = true },
                onCheckIn = {
                    feedback = Feedback(pastelColors.random(), praiseMessages.random())
                    if (selectedOccasional.isNotEmpty()) {
                        vm.checkInOccasional(selectedOccasional)
                        selectedOccasional = emptyList()
                    } else {
                        vm.checkInNormal(dueItems)
                    }
                }
            )
        }

        feedback?.let { CelebrationOverlay(it) }
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

    if (warningMeds.isNotEmpty() && !warningDismissed && isActive) {
        AlertDialog(
            onDismissRequest = { warningDismissed = true },
            shape = RoundedCornerShape(30.dp),
            containerColor = CardWhite,
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TintedGlyph(AppGlyph.Bell, tint = WarningRed, container = WarningSoft, size = 38.dp)
                    Spacer(Modifier.width(10.dp))
                    Text(
                        stringResource(R.string.checkin_refill_alert_title),
                        fontWeight = FontWeight.Bold,
                        color = TextDark
                    )
                }
            },
            text = {
                Column {
                    Text(stringResource(R.string.checkin_refill_alert_message), color = TextSub)
                    Spacer(Modifier.height(10.dp))
                    warningMeds.forEach { med ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(Modifier.size(6.dp).clip(CircleShape).background(WarningRed))
                            Spacer(Modifier.width(9.dp))
                            MedNameToggle(med, fontSize = 14.sp)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { warningDismissed = true }) {
                    Text(stringResource(R.string.checkin_got_it), fontWeight = FontWeight.Bold)
                }
            }
        )
    }
}

@Composable
private fun ActionDock(
    canUndo: Boolean,
    pending: Int,
    recordingOccasional: Boolean,
    occasionalEnabled: Boolean,
    onUndo: () -> Unit,
    onOccasional: () -> Unit,
    onCheckIn: () -> Unit
) {
    Column(
        Modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    listOf(BackgroundMint.copy(alpha = 0f), BackgroundMint.copy(alpha = 0.94f), BackgroundMint)
                )
            )
            .padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 10.dp)
    ) {
        AnimatedVisibility(visible = canUndo) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(bottom = 4.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                TextButton(onClick = onUndo, contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp)) {
                    GlyphIcon(AppGlyph.Undo, tint = TextSub, size = 14.dp)
                    Spacer(Modifier.width(5.dp))
                    Text(stringResource(R.string.checkin_undo), color = TextSub, fontSize = 12.sp)
                }
            }
        }

        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedButton(
                onClick = onOccasional,
                enabled = occasionalEnabled,
                modifier = Modifier
                    .weight(0.9f)
                    .height(56.dp),
                shape = RoundedCornerShape(18.dp),
                border = BorderStroke(1.dp, BorderStrong),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = PrimaryGreen,
                    disabledContentColor = TextFaint
                ),
                contentPadding = PaddingValues(horizontal = 8.dp)
            ) {
                GlyphIcon(
                    AppGlyph.Pill,
                    tint = if (occasionalEnabled) PrimaryGreen else TextFaint,
                    size = 17.dp
                )
                Spacer(Modifier.width(5.dp))
                Text(
                    stringResource(R.string.checkin_occasional),
                    color = if (occasionalEnabled) PrimaryGreen else TextFaint,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1
                )
            }

            val interactionSource = remember { MutableInteractionSource() }
            val pressed by interactionSource.collectIsPressedAsState()
            val scale by animateFloatAsState(
                targetValue = if (pressed) 0.97f else 1f,
                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                label = "checkInScale"
            )

            Button(
                onClick = onCheckIn,
                interactionSource = interactionSource,
                enabled = pending > 0,
                modifier = Modifier
                    .weight(1.25f)
                    .height(56.dp)
                    .testTag("checkInButton")
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                    },
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = PrimaryGreen,
                    contentColor = Color.White,
                    disabledContainerColor = PrimaryGreen.copy(alpha = 0.32f),
                    disabledContentColor = Color.White.copy(alpha = 0.85f)
                ),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 3.dp, pressedElevation = 0.dp)
            ) {
                GlyphIcon(AppGlyph.Check, tint = Color.White, size = 18.dp)
                Spacer(Modifier.width(6.dp))
                Text(
                    if (recordingOccasional) stringResource(R.string.checkin_record_occasional)
                    else stringResource(R.string.checkin_action_check_in),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    fontSize = 13.sp
                )
            }
        }
    }
}

@Composable
private fun OccasionalShortcut(hasMeds: Boolean, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(WarmTint.copy(alpha = 0.75f))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        TintedGlyph(
            AppGlyph.Pill,
            tint = OccasionalOrange,
            container = Color.White.copy(alpha = 0.72f),
            size = 36.dp
        )
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                stringResource(R.string.checkin_occasional),
                color = TextDark,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp
            )
            Text(
                if (hasMeds) stringResource(R.string.checkin_will_record_occasional)
                else stringResource(R.string.checkin_occasional_empty),
                color = TextSub,
                fontSize = 11.5.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        GlyphIcon(AppGlyph.ArrowRight, tint = OccasionalOrange, size = 18.dp)
    }
}

@Composable
private fun NextDoseHero(nd: NextDose, showImage: Boolean, onEarly: (() -> Unit)?) {
    val isMissed = nd.missed
    val gradient = if (isMissed) {
        listOf(Color(0xFFB9574D), Color(0xFFD9876F))
    } else {
        listOf(HeroStart, HeroEnd)
    }

    Box(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(30.dp))
            .background(Brush.linearGradient(gradient))
    ) {
        HeroDecorations(Modifier.matchParentSize(), accent = Color.White)
        Canvas(Modifier.matchParentSize()) {
            drawCircle(
                color = HeroGlow.copy(alpha = 0.18f),
                radius = size.width * 0.28f,
                center = androidx.compose.ui.geometry.Offset(size.width * 0.94f, size.height * 0.12f)
            )
        }
        Column(Modifier.padding(22.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    GlyphIcon(AppGlyph.Sun, tint = Color.White.copy(alpha = 0.82f), size = 14.dp)
                    Spacer(Modifier.width(6.dp))
                    Text(
                        stringResource(if (isMissed) R.string.checkin_missed_next_title else R.string.checkin_next_dose_title),
                        color = Color.White.copy(alpha = 0.9f),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 0.4.sp
                    )
                }
                Spacer(Modifier.weight(1f))
                Box(
                    Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color.White.copy(alpha = 0.16f))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(nd.time, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(Modifier.height(7.dp))
            Text(
                when {
                    isMissed -> stringResource(R.string.checkin_missed_next_subtitle)
                    nd.tomorrow -> stringResource(R.string.checkin_tomorrow)
                    else -> stringResource(R.string.checkin_today)
                },
                color = Color.White.copy(alpha = 0.66f),
                fontSize = 12.sp
            )

            Spacer(Modifier.height(16.dp))

            nd.items.forEachIndexed { index, med ->
                if (index > 0) {
                    Spacer(Modifier.height(8.dp))
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(Color.White.copy(alpha = 0.12f))
                    )
                    Spacer(Modifier.height(8.dp))
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (showImage) {
                        MedicationAvatar(
                            name = med.name,
                            imageUri = med.imageUri,
                            showImage = true,
                            size = 38.dp
                        )
                        Spacer(Modifier.width(11.dp))
                    }
                    Column(Modifier.weight(1f)) {
                        Text(
                            med.name,
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            "${usageLabel(med)} · ${intakeLabel(med)}",
                            color = Color.White.copy(alpha = 0.68f),
                            fontSize = 11.5.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            if (onEarly != null) {
                Spacer(Modifier.height(18.dp))
                Button(
                    onClick = onEarly,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp),
                    shape = RoundedCornerShape(15.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White,
                        contentColor = PrimaryGreen
                    ),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
                ) {
                    Text(stringResource(R.string.checkin_early), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DueMedicationCard(item: DueItem, showImage: Boolean) {
    var expanded by remember { mutableStateOf(false) }
    val med = item.medication
    val longName = med.name.length > 6
    val display = if (expanded || !longName) med.name else med.name.take(4) + "…"
    val today = LocalDate.now().toString()

    AppCard(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(),
        contentPadding = PaddingValues(15.dp)
    ) {
        Row(verticalAlignment = Alignment.Top) {
            Column(
                Modifier
                    .width(54.dp)
                    .clip(RoundedCornerShape(17.dp))
                    .background(MintWash)
                    .padding(vertical = 9.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    item.time,
                    color = PrimaryGreen,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    if (item.date == today) stringResource(R.string.ui_today) else stringResource(R.string.checkin_yesterday),
                    color = TextFaint,
                    fontSize = 9.5.sp
                )
            }
            Spacer(Modifier.width(12.dp))
            if (showImage) {
                MedicationAvatar(
                    name = med.name,
                    imageUri = med.imageUri,
                    showImage = true,
                    size = 45.dp
                )
                Spacer(Modifier.width(11.dp))
            }
            Column(
                Modifier
                    .weight(1f)
                    .clickable { expanded = !expanded }
            ) {
                Text(
                    display,
                    color = TextDark,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.5.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(5.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    AppPill(usageLabel(med), glyph = AppGlyph.Leaf)
                    AppPill(intakeLabel(med), glyph = AppGlyph.Pill)
                }
                if (med.note.isNotBlank()) {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        med.note,
                        color = TextSub,
                        fontSize = 11.5.sp,
                        maxLines = if (expanded) 4 else 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                if (longName) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        if (expanded) stringResource(R.string.checkin_collapse)
                        else stringResource(R.string.checkin_view_full_name),
                        color = PrimaryGreen.copy(alpha = 0.85f),
                        fontSize = 10.sp
                    )
                }
            }
            if (longName || med.note.isNotBlank()) {
                Spacer(Modifier.width(6.dp))
                GlyphIcon(
                    if (expanded) AppGlyph.ChevronUp else AppGlyph.ChevronDown,
                    tint = TextFaint,
                    size = 18.dp
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun OccasionalSelectedCard(med: Medication, showImage: Boolean) {
    var expanded by remember { mutableStateOf(false) }
    val longName = med.name.length > 6
    val display = if (expanded || !longName) med.name else med.name.take(4) + "…"

    AppCard(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(),
        containerColor = WarmTint,
        contentPadding = PaddingValues(15.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(54.dp)
                    .clip(RoundedCornerShape(17.dp))
                    .background(Color.White.copy(alpha = 0.68f)),
                contentAlignment = Alignment.Center
            ) {
                GlyphIcon(AppGlyph.Pill, tint = OccasionalOrange, size = 24.dp)
            }
            Spacer(Modifier.width(12.dp))
            if (showImage) {
                MedicationAvatar(
                    name = med.name,
                    imageUri = med.imageUri,
                    showImage = true,
                    size = 45.dp
                )
                Spacer(Modifier.width(11.dp))
            }
            Column(
                Modifier
                    .weight(1f)
                    .clickable { expanded = !expanded }
            ) {
                Text(
                    display,
                    color = TextDark,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.5.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(5.dp))
                AppPill(
                    stringResource(R.string.checkin_will_record_occasional),
                    color = Color(0xFF9A5A1E),
                    containerColor = Color.White.copy(alpha = 0.65f),
                    glyph = AppGlyph.Clock
                )
            }
            GlyphIcon(
                if (expanded) AppGlyph.ChevronUp else AppGlyph.ChevronDown,
                tint = OccasionalOrange,
                size = 18.dp
            )
        }
    }
}

@Composable
private fun MissedBanner(count: Int, onClick: () -> Unit) {
    AppCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("missedBanner"),
        containerColor = WarningSoft,
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
        onClick = onClick
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            TintedGlyph(
                AppGlyph.Bell,
                tint = WarningRed,
                container = Color.White.copy(alpha = 0.72f),
                size = 36.dp
            )
            Spacer(Modifier.width(11.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    stringResource(R.string.checkin_missed_count, count),
                    color = TextDark,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    stringResource(R.string.ui_missed_hint),
                    color = TextSub,
                    fontSize = 10.5.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(Modifier.width(6.dp))
            Text(
                stringResource(R.string.checkin_backfill),
                color = WarningRed,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.width(2.dp))
            GlyphIcon(AppGlyph.ChevronRight, tint = WarningRed, size = 15.dp)
        }
    }
}

@Composable
private fun CloseDoseBanner(warning: CloseDoseWarning) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(WarningSoft)
            .padding(horizontal = 13.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        GlyphIcon(AppGlyph.Clock, tint = WarningRed, size = 16.dp)
        Spacer(Modifier.width(8.dp))
        Text(
            stringResource(R.string.checkin_close_dose_warning, warning.medName, warning.minutes),
            color = TextDark,
            fontSize = 12.sp
        )
    }
}

@Composable
private fun CelebrationOverlay(fb: Feedback) {
    val scale = remember { Animatable(0.84f) }
    val alpha = remember { Animatable(0f) }
    LaunchedEffect(fb) {
        alpha.animateTo(1f)
        scale.animateTo(
            1f,
            spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow)
        )
    }
    Box(
        Modifier
            .fillMaxSize()
            .background(Color(0xFF0B1F19).copy(alpha = 0.46f))
            .clickable(enabled = false) {},
        contentAlignment = Alignment.Center
    ) {
        Box(
            Modifier
                .padding(36.dp)
                .fillMaxWidth()
                .graphicsLayer {
                    scaleX = scale.value
                    scaleY = scale.value
                    this.alpha = alpha.value
                }
                .clip(RoundedCornerShape(30.dp))
                .background(Brush.linearGradient(listOf(fb.color, Color.White)))
        ) {
            Canvas(Modifier.matchParentSize()) {
                drawCircle(Color.White.copy(alpha = 0.45f), size.width * 0.28f, androidx.compose.ui.geometry.Offset(size.width * 0.92f, size.height * 0.08f))
                drawCircle(Color.White.copy(alpha = 0.3f), size.width * 0.16f, androidx.compose.ui.geometry.Offset(size.width * 0.05f, size.height * 0.9f))
            }
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 26.dp, vertical = 30.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    Modifier
                        .size(62.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.76f)),
                    contentAlignment = Alignment.Center
                ) {
                    GlyphIcon(AppGlyph.Check, tint = PrimaryGreen, size = 32.dp)
                }
                Spacer(Modifier.height(15.dp))
                Text(
                    fb.message,
                    color = TextDark,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
                Spacer(Modifier.height(5.dp))
                Text(
                    stringResource(R.string.checkin_celebration_subtitle),
                    color = TextSub,
                    fontSize = 11.5.sp,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
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
        shape = RoundedCornerShape(30.dp),
        containerColor = CardWhite,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                TintedGlyph(AppGlyph.Pill, tint = OccasionalOrange, container = WarmTint, size = 38.dp)
                Spacer(Modifier.width(10.dp))
                Text(
                    stringResource(R.string.checkin_occasional_dialog_title),
                    fontWeight = FontWeight.Bold,
                    color = TextDark,
                    fontSize = 19.sp
                )
            }
        },
        text = {
            if (occasional.isEmpty()) {
                Text(stringResource(R.string.checkin_occasional_empty), color = TextSub)
            } else {
                Column(
                    Modifier
                        .heightIn(max = 320.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    occasional.forEach { med ->
                        val isChecked = checked.contains(med.id)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(if (isChecked) WarmTint else Color.Transparent)
                                .clickable {
                                    if (isChecked) checked.remove(med.id) else checked.add(med.id)
                                }
                                .padding(end = 10.dp)
                        ) {
                            Checkbox(
                                checked = isChecked,
                                onCheckedChange = {
                                    if (isChecked) checked.remove(med.id) else checked.add(med.id)
                                }
                            )
                            MedicationAvatar(
                                name = med.name,
                                imageUri = med.imageUri,
                                showImage = true,
                                size = 34.dp
                            )
                            Spacer(Modifier.width(9.dp))
                            MedNameToggle(med, fontSize = 14.sp)
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(occasional.filter { checked.contains(it.id) }) },
                shape = RoundedCornerShape(14.dp)
            ) {
                Text(stringResource(R.string.action_ok), fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        }
    )
}

/** 时段标签：昨天的项额外标注「昨天」，避免与今天的同名时段混淆 */
@Composable
private fun MissedTimeLabel(item: DueItem, todayDate: String) {
    if (item.date != todayDate) {
        AppPill(
            stringResource(R.string.checkin_yesterday),
            color = OccasionalOrange,
            containerColor = WarmTint
        )
        Spacer(Modifier.width(6.dp))
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

    LaunchedEffect(missed.isEmpty()) {
        if (missed.isEmpty()) onDismiss()
    }

    pendingDelete?.let { item ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            shape = RoundedCornerShape(30.dp),
            containerColor = CardWhite,
            title = {
                Text(
                    stringResource(R.string.checkin_backfill_delete_title),
                    fontWeight = FontWeight.Bold,
                    color = TextDark
                )
            },
            text = {
                Column {
                    Text(stringResource(R.string.checkin_backfill_delete_message), color = TextSub, fontSize = 13.sp)
                    Spacer(Modifier.height(10.dp))
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
                ) {
                    Text(stringResource(R.string.action_delete), color = WarningRed, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) { Text(stringResource(R.string.action_cancel)) }
            }
        )
        return
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(30.dp),
        containerColor = CardWhite,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                TintedGlyph(AppGlyph.Bell, tint = WarningRed, container = WarningSoft, size = 38.dp)
                Spacer(Modifier.width(10.dp))
                Text(
                    stringResource(R.string.checkin_backfill_dialog_title),
                    fontWeight = FontWeight.Bold,
                    color = TextDark,
                    fontSize = 19.sp
                )
                Spacer(Modifier.weight(1f))
                TextButton(
                    onClick = { manage = !manage },
                    modifier = Modifier.testTag("backfillManage")
                ) {
                    Text(
                        if (manage) stringResource(R.string.checkin_done) else stringResource(R.string.checkin_manage),
                        fontSize = 12.sp
                    )
                }
            }
        },
        text = {
            Column {
                Text(
                    if (manage) stringResource(R.string.checkin_backfill_manage_hint)
                    else stringResource(R.string.checkin_backfill_pick_hint),
                    color = TextSub,
                    fontSize = 12.sp
                )
                Spacer(Modifier.height(10.dp))
                Column(
                    Modifier
                        .heightIn(max = 320.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    missed.forEach { item ->
                        val key = dueKey(item)
                        val isChecked = checked.contains(key)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(13.dp))
                                .background(if (isChecked && !manage) MintWash else Color.Transparent)
                                .clickable(enabled = !manage) {
                                    if (isChecked) checked.remove(key) else checked.add(key)
                                }
                                .padding(end = 6.dp)
                        ) {
                            Checkbox(
                                checked = isChecked,
                                enabled = !manage,
                                onCheckedChange = {
                                    if (isChecked) checked.remove(key) else checked.add(key)
                                }
                            )
                            MissedTimeLabel(item, todayDate)
                            Spacer(Modifier.width(9.dp))
                            Box(Modifier.weight(1f)) {
                                MedNameToggle(item.medication, fontSize = 14.sp)
                            }
                            if (manage) {
                                TextButton(
                                    onClick = { pendingDelete = item },
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                                ) {
                                    Text(
                                        stringResource(R.string.action_delete),
                                        color = WarningRed,
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    if (manage) stringResource(R.string.checkin_backfill_delete_note)
                    else stringResource(R.string.checkin_backfill_confirm_note),
                    color = TextFaint,
                    fontSize = 10.5.sp
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(missed.filter { checked.contains(dueKey(it)) }) },
                enabled = !manage && checked.isNotEmpty(),
                modifier = Modifier.testTag("backfillConfirm"),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text(stringResource(R.string.checkin_backfill), fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        }
    )
}

private fun dueKey(item: DueItem): String = "${item.date}|${item.medication.id}|${item.time}"
