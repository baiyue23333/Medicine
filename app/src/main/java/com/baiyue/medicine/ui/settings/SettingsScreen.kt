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
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.baiyue.medicine.R
import com.baiyue.medicine.i18n.AppLocale
import com.baiyue.medicine.i18n.findActivity
import com.baiyue.medicine.ui.theme.PrimaryGreen
import com.baiyue.medicine.ui.theme.TextDark
import com.baiyue.medicine.ui.theme.TextSub
import kotlinx.coroutines.launch
import java.time.LocalDate

@Composable
fun SettingsScreen(vm: SettingsViewModel = viewModel()) {
    val settings by vm.settings.collectAsState()
    val language by AppLocale.language.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val notifEnabled = remember { mutableStateOf(NotificationManagerCompat.from(context).areNotificationsEnabled()) }

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

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text(
            stringResource(R.string.settings_title),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = TextDark
        )
        Spacer(Modifier.height(12.dp))

        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                SettingRow(
                    stringResource(R.string.settings_dose_reminder),
                    stringResource(R.string.settings_dose_reminder_desc)
                ) {
                    Switch(checked = settings.reminderEnabled, onCheckedChange = vm::setReminderEnabled)
                }
                SettingRow(
                    stringResource(R.string.settings_missed_reminder),
                    stringResource(R.string.settings_missed_reminder_desc)
                ) {
                    Switch(
                        checked = settings.missedReminderEnabled,
                        onCheckedChange = vm::setMissedReminderEnabled
                    )
                }
                if (!notifEnabled.value) {
                    SettingRow(
                        stringResource(R.string.settings_notif_permission),
                        stringResource(R.string.settings_notif_permission_desc)
                    ) {
                        TextButton(onClick = {
                            runCatching {
                                context.startActivity(
                                    Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                                        .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                                )
                            }
                        }) { Text(stringResource(R.string.settings_go_enable), color = PrimaryGreen) }
                    }
                }
                SettingRow(
                    stringResource(R.string.settings_keep_alive),
                    stringResource(R.string.settings_keep_alive_desc)
                ) {
                    Switch(checked = settings.keepAlive, onCheckedChange = vm::setKeepAlive)
                }

                Column {
                    Text(
                        stringResource(R.string.settings_language),
                        color = TextDark,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        stringResource(R.string.settings_language_desc),
                        color = TextSub,
                        fontSize = 12.sp
                    )
                    Spacer(Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(
                            AppLocale.SYSTEM to R.string.settings_language_system,
                            AppLocale.CHINESE to R.string.settings_language_zh,
                            AppLocale.ENGLISH to R.string.settings_language_en
                        ).forEach { (tag, labelRes) ->
                            FilterChip(
                                selected = language == tag,
                                onClick = {
                                    AppLocale.set(context, tag)
                                    context.findActivity()?.recreate()
                                },
                                label = { Text(stringResource(labelRes)) }
                            )
                        }
                    }
                }

                Column {
                    Text(
                        stringResource(R.string.settings_warning_days),
                        color = TextDark,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(1, 3, 5, 7).forEach { d ->
                            FilterChip(
                                selected = settings.defaultWarningDays == d,
                                onClick = { vm.setWarningDays(d) },
                                label = { Text(stringResource(R.string.settings_days, d)) }
                            )
                        }
                        FilterChip(
                            selected = settings.defaultWarningDays !in listOf(1, 3, 5, 7),
                            onClick = { vm.setWarningDays(settings.defaultWarningDays.coerceAtLeast(8)) },
                            label = { Text(stringResource(R.string.settings_custom)) }
                        )
                    }
                    AnimatedVisibility(
                        visible = settings.defaultWarningDays !in listOf(1, 3, 5, 7),
                        enter = expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut()
                    ) {
                        Column {
                            Spacer(Modifier.height(8.dp))
                            OutlinedTextField(
                                value = settings.defaultWarningDays.toString(),
                                onValueChange = { v ->
                                    v.toIntOrNull()?.takeIf { it >= 8 }?.let(vm::setWarningDays)
                                },
                                label = { Text(stringResource(R.string.settings_custom_days_label)) },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                            )
                        }
                    }
                }

                Column {
                    Text(
                        stringResource(R.string.settings_advance),
                        color = TextDark,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(0, 5, 10, 30).forEach { m ->
                            FilterChip(
                                selected = settings.reminderAdvanceMinutes == m,
                                onClick = { vm.setReminderAdvanceMinutes(m) },
                                label = {
                                    Text(
                                        if (m == 0) stringResource(R.string.settings_on_time)
                                        else stringResource(R.string.settings_advance_minutes, m)
                                    )
                                }
                            )
                        }
                        FilterChip(
                            selected = settings.reminderAdvanceMinutes !in listOf(0, 5, 10, 30),
                            onClick = { vm.setReminderAdvanceMinutes(settings.reminderAdvanceMinutes.coerceAtLeast(60)) },
                            label = { Text(stringResource(R.string.settings_custom)) }
                        )
                    }
                    AnimatedVisibility(
                        visible = settings.reminderAdvanceMinutes !in listOf(0, 5, 10, 30),
                        enter = expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut()
                    ) {
                        Column {
                            Spacer(Modifier.height(8.dp))
                            OutlinedTextField(
                                value = settings.reminderAdvanceMinutes.toString(),
                                onValueChange = { v ->
                                    v.toIntOrNull()?.takeIf { it >= 0 }?.let(vm::setReminderAdvanceMinutes)
                                },
                                label = { Text(stringResource(R.string.settings_advance_minutes_label)) },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = settings.reminderMessage,
                    onValueChange = vm::setReminderMessage,
                    label = { Text(stringResource(R.string.settings_reminder_message_label)) },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedButton(
                    onClick = {
                        runCatching {
                            context.startActivity(
                                Intent(
                                    Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                                    Uri.parse("package:${context.packageName}")
                                )
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.settings_battery_whitelist))
                }

                OutlinedButton(
                    onClick = { showExportNotice = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.settings_export_csv))
                }
                OutlinedButton(
                    onClick = { importLauncher.launch(arrayOf("text/csv", "text/comma-separated-values", "text/plain")) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.settings_import_csv))
                }
                Text(
                    stringResource(R.string.settings_export_warning),
                    color = TextSub,
                    fontSize = 12.sp
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFFEAF4EE)),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    stringResource(R.string.settings_declaration_title),
                    color = TextDark,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    stringResource(R.string.settings_declaration_open_source),
                    color = TextSub,
                    fontSize = 13.sp
                )
                Text(
                    stringResource(R.string.settings_declaration_privacy),
                    color = TextSub,
                    fontSize = 13.sp
                )
                Text(
                    stringResource(R.string.settings_declaration_author),
                    color = TextSub,
                    fontSize = 13.sp
                )
            }
        }
    }

    if (showExportNotice) {
        AlertDialog(
            onDismissRequest = { showExportNotice = false },
            shape = RoundedCornerShape(28.dp),
            title = { Text(stringResource(R.string.settings_export_privacy_title), fontWeight = FontWeight.Bold, color = TextDark) },
            text = { Text(stringResource(R.string.settings_export_privacy_message)) },
            confirmButton = {
                TextButton(onClick = {
                    showExportNotice = false
                    exportLauncher.launch(
                        context.getString(R.string.settings_export_file_name, LocalDate.now().toString())
                    )
                }) { Text(stringResource(R.string.action_ok), color = PrimaryGreen) }
            },
            dismissButton = {
                TextButton(onClick = { showExportNotice = false }) { Text(stringResource(R.string.action_cancel)) }
            }
        )
    }
}

@Composable
private fun SettingRow(title: String, subtitle: String, trailing: @Composable () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, color = TextDark, fontWeight = FontWeight.Medium)
            Text(subtitle, color = TextSub, fontSize = 12.sp)
        }
        trailing()
    }
}
