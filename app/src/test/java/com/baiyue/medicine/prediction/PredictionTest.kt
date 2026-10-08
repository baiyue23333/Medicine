package com.baiyue.medicine.prediction

import com.baiyue.medicine.data.entity.Medication
import com.baiyue.medicine.model.DoseType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

class PredictionTest {

    private fun med(
        doseType: DoseType = DoseType.MORNING_NOON_NIGHT,
        doseTimes: String = "08:00,12:00,18:00",
        intervalHours: Int = 0,
        timesPerDay: Int = 1,
        isScheduled: Boolean = true,
        isOccasional: Boolean = false,
        quantity: Double = 10.0,
        minKeepQuantity: Double = 0.0,
        warningDays: Int = 3
    ) = Medication(
        name = "测试药",
        doseType = doseType,
        doseTimes = doseTimes,
        intervalHours = intervalHours,
        timesPerDay = timesPerDay,
        isScheduled = isScheduled,
        isOccasional = isOccasional,
        quantity = quantity,
        minKeepQuantity = minKeepQuantity,
        warningDays = warningDays,
        enabled = true
    )

    @Test
    fun `dailyDose returns count of morning noon night times`() {
        assertEquals(3.0, dailyDose(med()), 0.001)
    }

    @Test
    fun `dailyDose every hours computes 24 by interval`() {
        assertEquals(4.0, dailyDose(med(doseType = DoseType.EVERY_HOURS, intervalHours = 6)), 0.001)
    }

    @Test
    fun `dailyDose custom prefers parsed doseTimes`() {
        assertEquals(
            3.0,
            dailyDose(med(doseType = DoseType.CUSTOM, doseTimes = "07:30,13:00,21:00", timesPerDay = 5)),
            0.001
        )
    }

    @Test
    fun `dailyDose custom falls back to timesPerDay`() {
        assertEquals(4.0, dailyDose(med(doseType = DoseType.CUSTOM, doseTimes = "", timesPerDay = 4)), 0.001)
    }

    @Test
    fun `dailyDose is zero for non-scheduled medication`() {
        assertEquals(0.0, dailyDose(med(isScheduled = false)), 0.001)
    }

    @Test
    fun `predict returns null when daily dose is zero`() {
        assertNull(predict(med(isScheduled = false), LocalDate.of(2026, 1, 10)))
    }

    @Test
    fun `predict computes remaining days and run out date`() {
        // 数量 10，每天 3 次 -> 10/3 ≈ 3.33 天
        val p = predict(med(quantity = 10.0), LocalDate.of(2026, 1, 10))!!
        assertEquals(3.33, p.daysRemaining, 0.01)
        assertEquals(LocalDate.of(2026, 1, 14), p.runOutDate)
    }

    @Test
    fun `predict triggers visit when quantity below min keep`() {
        val p = predict(
            med(quantity = 5.0, minKeepQuantity = 6.0, warningDays = 3),
            LocalDate.of(2026, 1, 10)
        )!!
        assertTrue(p.needVisit)
    }

    @Test
    fun `predict triggers visit when days remaining within warning window`() {
        // 数量 6，每天 3 次 -> 2 天 <= 3 天预警
        val p = predict(med(quantity = 6.0, warningDays = 3), LocalDate.of(2026, 1, 10))!!
        assertTrue(p.needVisit)
    }

    @Test
    fun `predict does not trigger visit when enough stock`() {
        val p = predict(med(quantity = 30.0, warningDays = 3), LocalDate.of(2026, 1, 10))!!
        assertFalse(p.needVisit)
    }

    @Test
    fun `visitWindow spans from warning days before run out`() {
        val m = med(quantity = 6.0, warningDays = 3)
        val today = LocalDate.of(2026, 1, 10)
        val p = predict(m, today)!!
        // runOutDate = 1/12，窗口 = 1/9 ~ 1/12（4 天）
        val window = visitWindow(p, LocalDate.of(2026, 1, 9))
        assertEquals(LocalDate.of(2026, 1, 9), window.first())
        assertEquals(LocalDate.of(2026, 1, 12), window.last())
        assertEquals(4, window.size)
    }

    @Test
    fun `visitWindow 不标注今天之前的日期`() {
        val m = med(quantity = 6.0, warningDays = 3)
        val p = predict(m, LocalDate.of(2026, 1, 10))!!
        // 起点 1/9 早于今天，应裁剪到今天；终点仍为用完日
        val window = visitWindow(p, LocalDate.of(2026, 1, 11))
        assertEquals(LocalDate.of(2026, 1, 11), window.first())
        assertEquals(LocalDate.of(2026, 1, 12), window.last())
        assertEquals(2, window.size)
    }

    @Test
    fun `未设置总量时不做预测`() {
        assertNull(predict(med(quantity = Medication.UNKNOWN_QUANTITY)))
    }

    @Test
    fun `时段日期按当前时刻归属当天或前一天`() {
        val today = LocalDate.of(2026, 9, 21)
        // 提醒在时段结束后触发：16:30 触发时，16:00 的时段属于当天
        assertEquals(
            today,
            doseSlotDate(16, 0, LocalTime.of(16, 30), today)
        )
        // 跨零点：次日 00:30 触发时，23:30 的时段属于前一天
        assertEquals(
            today.minusDays(1),
            doseSlotDate(23, 30, LocalTime.of(0, 30), today)
        )
    }

    @Test
    fun `时段日期在时刻相等时归属当天`() {
        val today = LocalDate.of(2026, 9, 21)
        assertEquals(today, doseSlotDate(8, 0, LocalTime.of(8, 0), today))
    }

    @Test
    fun `药品只在添加当天及之后视为存在`() {
        val today = LocalDate.of(2026, 9, 21)
        val createdAtToday = today.atTime(12, 0).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

        // createdAt 为 0 表示升级前已存在，视为一直存在
        assertTrue(medicationExistsOn(Medication(name = "旧药"), today.minusDays(1)))
        // 今天新加的药品不应追溯昨天
        val addedToday = Medication(name = "新药", createdAt = createdAtToday)
        assertFalse(medicationExistsOn(addedToday, today.minusDays(1)))
        assertTrue(medicationExistsOn(addedToday, today))
    }
}