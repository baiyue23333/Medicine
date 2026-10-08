package com.baiyue.medicine.ui

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.res.stringResource
import com.baiyue.medicine.R
import com.baiyue.medicine.data.entity.Medication
import com.baiyue.medicine.model.DoseType
import com.baiyue.medicine.model.UsageType
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun usageLabel(med: Medication): String = when (med.usageType) {
    UsageType.BEFORE_MEAL -> stringResource(R.string.usage_before_meal)
    UsageType.WITH_MEAL -> stringResource(R.string.usage_with_meal)
    UsageType.AFTER_MEAL -> stringResource(R.string.usage_after_meal)
    UsageType.PATCH -> stringResource(R.string.usage_patch)
    UsageType.ORAL -> stringResource(R.string.usage_oral)
    UsageType.INJECTION -> stringResource(R.string.usage_injection)
    UsageType.INHALATION -> stringResource(R.string.usage_inhalation)
    UsageType.EXTERNAL -> stringResource(R.string.usage_external)
    UsageType.CUSTOM ->
        if (med.customUsage.isBlank()) stringResource(R.string.usage_custom) else med.customUsage
}

@Composable
fun doseLabel(med: Medication): String {
    val schedule = if (med.isScheduled) {
        when {
            med.doseType == DoseType.MORNING_NOON_NIGHT ->
                stringResource(R.string.dose_morning_noon_night, med.doseTimes)
            med.doseType == DoseType.EVERY_HOURS ->
                stringResource(R.string.dose_every_hours, med.intervalHours, med.intervalStartTime)
            med.doseType == DoseType.TIMES_PER_DAY ->
                stringResource(R.string.dose_times_per_day, med.timesPerDay, med.doseTimes)
            else -> stringResource(R.string.dose_custom_period, med.doseTimes)
        }
    } else null
    val occasional = if (med.isOccasional) stringResource(R.string.dose_occasional) else null
    return listOfNotNull(schedule, occasional).joinToString(" · ")
}

/** 数值展示：整数不显示小数点（与语言无关） */
fun formatNum(d: Double): String =
    if (d == Math.floor(d) && !d.isInfinite()) d.toInt().toString() else d.toString()

/** 一次服用剂量，如「剂量：2」 */
@Composable
fun intakeLabel(med: Medication): String =
    stringResource(R.string.label_intake, formatNum(med.dosePerIntake))

/** 按当前语言格式化日期；patternRes 取 common_date_* 资源 */
@Composable
fun localizedDate(date: LocalDate, @StringRes patternRes: Int): String {
    val pattern = stringResource(patternRes)
    return remember(pattern) {
        DateTimeFormatter.ofPattern(pattern, Locale.getDefault()).format(date)
    }
}
