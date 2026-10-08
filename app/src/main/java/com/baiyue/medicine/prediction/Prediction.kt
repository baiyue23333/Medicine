package com.baiyue.medicine.prediction

import com.baiyue.medicine.data.entity.Medication
import com.baiyue.medicine.model.DoseType
import java.time.LocalDate

/** 单个药品的预测结果 */
data class Prediction(
    val medication: Medication,
    val dailyDose: Double,
    /** 预估还能维持的天数（就医天数） */
    val daysRemaining: Double,
    val runOutDate: LocalDate,
    /** 是否触发就医提醒 */
    val needVisit: Boolean
)

/** 每日用量（次数 × 每次剂量） */
fun dailyDose(med: Medication): Double {
    if (!med.isScheduled || !med.enabled) return 0.0
    val perIntake = med.dosePerIntake.coerceAtLeast(0.0)
    if (perIntake <= 0.0) return 0.0
    val timesPerDay = when (med.doseType) {
        DoseType.MORNING_NOON_NIGHT -> parseTimes(med.doseTimes).size.toDouble()
        DoseType.EVERY_HOURS -> if (med.intervalHours > 0) kotlin.math.ceil(24.0 / med.intervalHours) else 0.0
        DoseType.TIMES_PER_DAY -> med.timesPerDay.coerceAtLeast(1).toDouble()
        DoseType.CUSTOM -> {
            val count = parseTimes(med.doseTimes).size
            (if (count > 0) count else med.timesPerDay.coerceAtLeast(1)).toDouble()
        }
    }
    return timesPerDay * perIntake
}

/**
 * 预测计算（两者结合）：
 * - daysRemaining = 数量 / 每日用量
 * - 触发就医提醒：daysRemaining <= 提前预警天数 或 数量 <= 最少持续数量阈值
 */
fun predict(med: Medication, today: LocalDate = LocalDate.now()): Prediction? {
    val dose = dailyDose(med)
    if (dose <= 0.0) return null
    // 未设置总量时不做预测，避免误触发就医提醒
    if (med.quantity < 0.0) return null
    val days = med.quantity / dose
    val runOut = today.plusDays(kotlin.math.ceil(days).toLong())
    val needVisit = days <= med.warningDays || med.quantity <= med.minKeepQuantity
    return Prediction(med, dose, days, runOut, needVisit)
}

/**
 * 日历上标红的「就医时间段」日期列表
 * 只标今天及之后的日期，避免整月/过去日期大片标红
 */
fun visitWindow(p: Prediction, today: LocalDate = LocalDate.now()): List<LocalDate> {
    if (!p.needVisit) return emptyList()
    val start = maxOf(p.runOutDate.minusDays(p.medication.warningDays.toLong()), today)
    if (start.isAfter(p.runOutDate)) return emptyList()
    return generateSequence(start) { it.plusDays(1) }
        .takeWhile { !it.isAfter(p.runOutDate) }
        .toList()
}