package com.baiyue.medicine.ui.settings

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.baiyue.medicine.R
import com.baiyue.medicine.i18n.AppLocale
import com.baiyue.medicine.i18n.findActivity
import com.baiyue.medicine.ui.components.AppBackdrop
import com.baiyue.medicine.ui.components.AppCard
import com.baiyue.medicine.ui.components.AppGlyph
import com.baiyue.medicine.ui.components.GlyphIcon
import com.baiyue.medicine.ui.components.ScreenHeader
import com.baiyue.medicine.ui.components.SectionHeader
import com.baiyue.medicine.ui.components.TintedGlyph
import com.baiyue.medicine.ui.theme.BorderSoft
import com.baiyue.medicine.ui.theme.BorderStrong
import com.baiyue.medicine.ui.theme.CardWhite
import com.baiyue.medicine.ui.theme.MintTint
import com.baiyue.medicine.ui.theme.MintWash
import com.baiyue.medicine.ui.theme.PrimaryGreen
import com.baiyue.medicine.ui.theme.TextDark
import com.baiyue.medicine.ui.theme.TextFaint
import com.baiyue.medicine.ui.theme.TextSub
import com.baiyue.medicine.ui.theme.WarningRed
import com.baiyue.medicine.ui.theme.WarningSoft
import kotlinx.coroutines.launch
import java.time.LocalDate

@Composable
fun SettingsScreen(
    onBack: () -> Unit = {},
    vm: SettingsViewModel = viewModel()
) {
    val settings by vm.settings.collectAsState()
    val language by AppLocale.language.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val notifEnabled = remember { mutableStateOf(NotificationManagerCompat.from(context).areNotificationsEnabled()) }

    val versionName = remember {
        runCatching {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName
        }.getOrNull().orEmpty()
    }

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        if (uri != null) {
            scope.launch {
                runCatching {
                    val csv = vm.exportCsv()
                    context.contentResolver.openOutputStream(uri)?.use { it.write(csv.toByteArray()) }
                }
            }
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                runCatching {
                    val text = context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                    if (text.isNullOrBlank()) throw IllegalStateException("empty")
                    val count = vm.importCsv(text)
                    Toast.makeText(context, context.getString(R.string.settings_import_done, count), Toast.LENGTH_LONG).show()
                }.onFailure {
                    Toast.makeText(context, context.getString(R.string.settings_import_failed), Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    var showExportNotice by remember { mutableStateOf(false) }

    AppBackdrop {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding(),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 14.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item(key = "header") {
                ScreenHeader(
                    eyebrow = stringResource(R.string.app_name),
                    title = stringResource(R.string.settings_title),
                    subtitle = stringResource(R.string.ui_settings_subtitle),
                    trailing = { BackButton(onBack) }
                )
            }

            item(key = "remindersTitle") {
                SectionHeader(
                    title = stringResource(R.string.ui_section_reminders),
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            item(key = "reminders") {
                SettingsCard {
                    SwitchSettingRow(
                        glyph = AppGlyph.Bell,
                        title = stringResource(R.string.settings_dose_reminder),
                        subtitle = stringResource(R.string.settings_dose_reminder_desc),
                        checked = settings.reminderEnabled,
                        onCheckedChange = vm::setReminderEnabled
                    )
                    RowDivider()
                    SwitchSettingRow(
                        glyph = AppGlyph.Clock,
                        title = stringResource(R.string.settings_missed_reminder),
                        subtitle = stringResource(R.string.settings_missed_reminder_desc),
                        checked = settings.missedReminderEnabled,
                        onCheckedChange = vm::setMissedReminderEnabled
                    )
                    RowDivider()
                    SwitchSettingRow(
                        glyph = AppGlyph.Shield,
                        title = stringResource(R.string.settings_keep_alive),
                        subtitle = stringResource(R.string.settings_keep_alive_desc),
                        checked = settings.keepAlive,
                        onCheckedChange = vm::setKeepAlive
                    )
                    if (!notifEnabled.value) {
                        RowDivider()
                        ActionSettingRow(
                            glyph = AppGlyph.Info,
                            title = stringResource(R.string.settings_notif_permission),
                            subtitle = stringResource(R.string.settings_notif_permission_desc),
                            tint = WarningRed,
                            container = WarningSoft,
                            trailingText = stringResource(R.string.settings_go_enable),
                            onClick = {
                                runCatching {
                                    context.startActivity(
                                        Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                                            .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                                    )
                                }
                            }
                        )
                    }
                }
            }

            item(key = "preferencesTitle") {
                SectionHeader(
                    title = stringResource(R.string.ui_section_preferences),
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            item(key = "preferences") {
                SettingsCard {
                    PreferenceChipBlock(
                        title = stringResource(R.string.settings_language),
                        subtitle = stringResource(R.string.settings_language_desc)
                    ) {
                        listOf(
                            AppLocale.SYSTEM to R.string.settings_language_system,
                            AppLocale.CHINESE to R.string.settings_language_zh,
                            AppLocale.ENGLISH to R.string.settings_language_en
                        ).forEach { (tag, labelRes) ->
                            OptionChip(
                                label = stringResource(labelRes),
                                selected = language == tag,
                                onClick = {
                                    AppLocale.set(context, tag)
                                    context.findActivity()?.recreate()
                                }
                            )
                        }
                    }

                    RowDivider()

                    PreferenceChipBlock(
                        title = stringResource(R.string.settings_warning_days),
                        subtitle = null
                    ) {
                        listOf(1, 3, 5, 7).forEach { days ->
                            OptionChip(
                                label = pluralStringResource(R.plurals.settings_days_plural, days, days),
                                selected = settings.defaultWarningDays == days,
                                onClick = { vm.setWarningDays(days) }
                            )
                        }
                        OptionChip(
                            label = stringResource(R.string.settings_custom),
                            selected = settings.defaultWarningDays !in listOf(1, 3, 5, 7),
                            onClick = { vm.setWarningDays(settings.defaultWarningDays.coerceAtLeast(8)) }
                        )
                    }

                    AnimatedVisibility(
                        visible = settings.defaultWarningDays !in listOf(1, 3, 5, 7),
                        enter = expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut()
                    ) {
                        OutlinedTextField(
                            value = settings.defaultWarningDays.toString(),
                            onValueChange = { value ->
                                value.toIntOrNull()?.takeIf { it >= 8 }?.let(vm::setWarningDays)
                            },
                            label = { Text(stringResource(R.string.settings_custom_days_label)) },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 15.dp),
                            shape = RoundedCornerShape(15.dp),
                            colors = settingsFieldColors(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                        )
                    }

                    RowDivider()

                    PreferenceChipBlock(
                        title = stringResource(R.string.settings_advance),
                        subtitle = null
                    ) {
                        listOf(0, 5, 10, 30).forEach { minutes ->
                            OptionChip(
                                label = if (minutes == 0) stringResource(R.string.settings_on_time)
                                else stringResource(R.string.settings_advance_minutes, minutes),
                                selected = settings.reminderAdvanceMinutes == minutes,
                                onClick = { vm.setReminderAdvanceMinutes(minutes) }
                            )
                        }
                        OptionChip(
                            label = stringResource(R.string.settings_custom),
                            selected = settings.reminderAdvanceMinutes !in listOf(0, 5, 10, 30),
                            onClick = { vm.setReminderAdvanceMinutes(settings.reminderAdvanceMinutes.coerceAtLeast(60)) }
                        )
                    }

                    AnimatedVisibility(
                        visible = settings.reminderAdvanceMinutes !in listOf(0, 5, 10, 30),
                        enter = expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut()
                    ) {
                        OutlinedTextField(
                            value = settings.reminderAdvanceMinutes.toString(),
                            onValueChange = { value ->
                                value.toIntOrNull()?.takeIf { it >= 0 }?.let(vm::setReminderAdvanceMinutes)
                            },
                            label = { Text(stringResource(R.string.settings_advance_minutes_label)) },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 15.dp),
                            shape = RoundedCornerShape(15.dp),
                            colors = settingsFieldColors(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                        )
                    }

                    RowDivider()

                    Column(Modifier.padding(15.dp)) {
                        OutlinedTextField(
                            value = settings.reminderMessage,
                            onValueChange = vm::setReminderMessage,
                            label = { Text(stringResource(R.string.settings_reminder_message_label)) },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 2,
                            shape = RoundedCornerShape(15.dp),
                            colors = settingsFieldColors()
                        )
                    }
                }
            }

            item(key = "dataTitle") {
                SectionHeader(
                    title = stringResource(R.string.ui_section_data),
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            item(key = "data") {
                SettingsCard {
                    ActionSettingRow(
                        glyph = AppGlyph.Download,
                        title = stringResource(R.string.settings_export_csv),
                        subtitle = stringResource(R.string.settings_export_warning),
                        onClick = { showExportNotice = true }
                    )
                    RowDivider()
                    ActionSettingRow(
                        glyph = AppGlyph.Upload,
                        title = stringResource(R.string.settings_import_csv),
                        subtitle = null,
                        onClick = {
                            importLauncher.launch(
                                arrayOf("text/csv", "text/comma-separated-values", "text/plain")
                            )
                        }
                    )
                    RowDivider()
                    ActionSettingRow(
                        glyph = AppGlyph.Battery,
                        title = stringResource(R.string.settings_battery_whitelist),
                        subtitle = null,
                        onClick = {
                            runCatching {
                                context.startActivity(
                                    Intent(
                                        Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                                        Uri.parse("package:${context.packageName}")
                                    )
                                )
                            }
                        }
                    )
                    RowDivider()
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 15.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        GlyphIcon(AppGlyph.Shield, tint = PrimaryGreen, size = 15.dp)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            stringResource(R.string.ui_data_local_only),
                            color = TextSub,
                            fontSize = 11.sp
                        )
                    }
                }
            }

            item(key = "aboutTitle") {
                SectionHeader(
                    title = stringResource(R.string.ui_section_about),
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            item(key = "about") {
                AppCard(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(18.dp),
                    containerColor = MintWash
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            Modifier
                                .size(54.dp)
                                .clip(RoundedCornerShape(19.dp))
                                .background(
                                    androidx.compose.ui.graphics.Brush.linearGradient(
                                        listOf(PrimaryGreen, com.baiyue.medicine.ui.theme.PrimarySoft)
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            GlyphIcon(AppGlyph.Leaf, tint = Color.White, size = 26.dp)
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                stringResource(R.string.app_name),
                                color = TextDark,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(
                                stringResource(R.string.ui_version, versionName),
                                color = TextSub,
                                fontSize = 11.sp
                            )
                        }
                    }

                    Spacer(Modifier.height(14.dp))
                    HorizontalDivider(color = BorderSoft)
                    Spacer(Modifier.height(13.dp))

                    Text(
                        stringResource(R.string.settings_declaration_open_source),
                        color = TextSub,
                        fontSize = 11.5.sp,
                        lineHeight = 18.sp
                    )
                    Spacer(Modifier.height(7.dp))
                    Text(
                        stringResource(R.string.settings_declaration_privacy),
                        color = TextSub,
                        fontSize = 11.5.sp,
                        lineHeight = 18.sp
                    )
                    Spacer(Modifier.height(9.dp))
                    Text(
                        stringResource(R.string.settings_declaration_author),
                        color = TextFaint,
                        fontSize = 11.sp
                    )
                }
            }
        }
    }

    if (showExportNotice) {
        AlertDialog(
            onDismissRequest = { showExportNotice = false },
            shape = RoundedCornerShape(30.dp),
            containerColor = CardWhite,
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TintedGlyph(AppGlyph.Download, size = 38.dp)
                    Spacer(Modifier.width(10.dp))
                    Text(
                        stringResource(R.string.settings_export_privacy_title),
                        fontWeight = FontWeight.Bold,
                        color = TextDark,
                        fontSize = 19.sp
                    )
                }
            },
            text = { Text(stringResource(R.string.settings_export_privacy_message), color = TextSub) },
            confirmButton = {
                Button(
                    onClick = {
                        showExportNotice = false
                        exportLauncher.launch(
                            context.getString(R.string.settings_export_file_name, LocalDate.now().toString())
                        )
                    },
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text(stringResource(R.string.action_ok), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showExportNotice = false }) { Text(stringResource(R.string.action_cancel)) }
            }
        )
    }
}

@Composable
private fun BackButton(onBack: () -> Unit) {
    Box(
        Modifier
            .size(46.dp)
            .clip(CircleShape)
            .background(CardWhite.copy(alpha = 0.9f))
            .border(1.dp, BorderSoft, CircleShape)
            .clickable(onClick = onBack),
        contentAlignment = Alignment.Center
    ) {
        GlyphIcon(
            AppGlyph.Back,
            tint = TextDark,
            size = 20.dp,
            contentDescription = stringResource(R.string.ui_back)
        )
    }
}

@Composable
private fun SettingsCard(content: @Composable ColumnScope.() -> Unit) {
    AppCard(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(0.dp)
    ) {
        Column(content = content)
    }
}

@Composable
private fun RowDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(start = 64.dp, end = 15.dp),
        color = BorderSoft
    )
}

@Composable
private fun SwitchSettingRow(
    glyph: AppGlyph,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 14.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        TintedGlyph(glyph, size = 36.dp)
        Spacer(Modifier.width(11.dp))
        Column(Modifier.weight(1f)) {
            Text(
                title,
                color = TextDark,
                fontSize = 13.5.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(2.dp))
            Text(
                subtitle,
                color = TextSub,
                fontSize = 10.5.sp,
                lineHeight = 15.sp
            )
        }
        Spacer(Modifier.width(6.dp))
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun ActionSettingRow(
    glyph: AppGlyph,
    title: String,
    subtitle: String?,
    onClick: () -> Unit,
    tint: Color = PrimaryGreen,
    container: Color = MintTint,
    trailingText: String? = null
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        TintedGlyph(glyph, tint = tint, container = container, size = 36.dp)
        Spacer(Modifier.width(11.dp))
        Column(Modifier.weight(1f)) {
            Text(
                title,
                color = TextDark,
                fontSize = 13.5.sp,
                fontWeight = FontWeight.SemiBold
            )
            if (!subtitle.isNullOrBlank()) {
                Spacer(Modifier.height(2.dp))
                Text(
                    subtitle,
                    color = TextSub,
                    fontSize = 10.5.sp,
                    lineHeight = 15.sp,
                    maxLines = 2
                )
            }
        }
        if (trailingText != null) {
            Text(
                trailingText,
                color = PrimaryGreen,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        } else {
            GlyphIcon(AppGlyph.ChevronRight, tint = TextFaint, size = 16.dp)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PreferenceChipBlock(
    title: String,
    subtitle: String?,
    content: @Composable () -> Unit
) {
    Column(Modifier.padding(horizontal = 15.dp, vertical = 13.dp)) {
        Text(
            title,
            color = TextDark,
            fontSize = 13.5.sp,
            fontWeight = FontWeight.SemiBold
        )
        if (!subtitle.isNullOrBlank()) {
            Spacer(Modifier.height(2.dp))
            Text(subtitle, color = TextSub, fontSize = 10.5.sp)
        }
        Spacer(Modifier.height(9.dp))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(7.dp),
            verticalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            content()
        }
    }
}

@Composable
private fun OptionChip(label: String, selected: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(12.dp)
    Box(
        Modifier
            .clip(shape)
            .background(if (selected) MintTint else CardWhite)
            .border(1.dp, if (selected) PrimaryGreen.copy(alpha = 0.55f) else BorderStrong, shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 7.dp)
    ) {
        Text(
            label,
            color = if (selected) PrimaryGreen else TextSub,
            fontSize = 11.5.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
        )
    }
}

@Composable
private fun settingsFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = PrimaryGreen,
    unfocusedBorderColor = BorderStrong,
    focusedContainerColor = CardWhite,
    unfocusedContainerColor = CardWhite,
    focusedLabelColor = PrimaryGreen,
    unfocusedLabelColor = TextSub,
    cursorColor = PrimaryGreen
)
