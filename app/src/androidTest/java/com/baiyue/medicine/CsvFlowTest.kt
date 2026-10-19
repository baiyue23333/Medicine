package com.baiyue.medicine

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.baiyue.medicine.data.entity.Medication
import com.baiyue.medicine.model.DoseType
import com.baiyue.medicine.model.RecordCategory
import com.baiyue.medicine.model.RecordFlag
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate

/**
 * CSV 导出/导入往返测试：验证提前/补签标注列、偶尔服药去重修复与数值精确导出。
 */
@RunWith(AndroidJUnit4::class)
class CsvFlowTest {

    private val app: MedicineApp = ApplicationProvider.getApplicationContext()

    @Before
    fun clearDb() = runBlocking {
        app.database.openHelper.writableDatabase.execSQL("DELETE FROM dose_records")
        app.database.openHelper.writableDatabase.execSQL("DELETE FROM medications")
    }

    @After
    fun tearDown() = clearDb()

    @Test
    fun 导出多行备注后可完整导入() = runBlocking {
        val note = "第一行, \"带引号\"\n第二行"
        app.repository.upsertMedication(
            Medication(
                name = "多行备注药",
                doseType = DoseType.MORNING_NOON_NIGHT,
                doseTimes = "08:00",
                note = note,
                quantity = 5.0,
                createdAt = System.currentTimeMillis()
            )
        )
        val csv = app.repository.exportCsv(app)

        app.database.openHelper.writableDatabase.execSQL("DELETE FROM dose_records")
        app.database.openHelper.writableDatabase.execSQL("DELETE FROM medications")

        app.repository.importCsv(csv)
        val imported = app.repository.observeAllMedications().first().first { it.name == "多行备注药" }
        assertEquals(note, imported.note)
    }

    @Test
    fun 删除药品后记录与导出仍保留药名() = runBlocking {
        val name = "历史药名测试药"
        val id = app.repository.upsertMedication(
            Medication(
                name = name,
                doseType = DoseType.MORNING_NOON_NIGHT,
                doseTimes = "08:00",
                quantity = 10.0,
                createdAt = System.currentTimeMillis()
            )
        )
        app.repository.addRecord(id, LocalDate.now().toString(), "08:05", RecordCategory.NORMAL, scheduledTime = "08:00")
        val med = app.repository.getMedication(id)!!
        app.repository.deleteMedication(med)

        val csv = app.repository.exportCsv(app)
        assertTrue("删除药品后导出仍应保留历史药名", csv.contains(name))
    }

    @Test
    fun 导出包含标注列与计划日期列() = runBlocking {
        val medId = app.repository.upsertMedication(
            Medication(
                name = "csv测试药",
                doseType = DoseType.MORNING_NOON_NIGHT,
                doseTimes = "08:00",
                dosePerIntake = 2.0,
                quantity = 10.0,
                createdAt = System.currentTimeMillis()
            )
        )
        val today = LocalDate.now().toString()
        val yesterday = LocalDate.now().minusDays(1).toString()

        app.repository.addRecord(
            medId, today, "23:30", RecordCategory.NORMAL,
            scheduledTime = "22:00", flag = RecordFlag.BACKFILL, plannedDate = yesterday
        )
        app.repository.addRecord(
            medId, today, "10:00", RecordCategory.NORMAL,
            scheduledTime = "12:00", flag = RecordFlag.EARLY, plannedDate = today
        )

        val csv = app.repository.exportCsv(app)

        // 记录行含 9 列（含 Flag / Planned date）
        val recordLines = csv.lineSequence()
            .filter { it.contains("BACKFILL") || it.contains("EARLY") }
            .toList()
        assertEquals("应有提前与补签两条记录", 2, recordLines.size)
        recordLines.forEach { line ->
            assertEquals("记录行应为 9 列", 9, line.split(',').size)
        }
        assertTrue("导出应包含补签标注", csv.contains("BACKFILL"))
        assertTrue("导出应包含提前标注", csv.contains("EARLY"))
        assertTrue("导出应包含计划日期(昨天)", csv.contains(yesterday))

        // 数值精确：2.0/10.0 应导成整数形式
        assertFalse("每次剂量 2.0 不应带小数点", csv.contains("2.0"))
        assertFalse("总量 10.0 不应带小数点", csv.contains("10.0"))
    }

    @Test
    fun 导出后清库再导入可往返且偶尔服药去重() = runBlocking {
        val regularId = app.repository.upsertMedication(
            Medication(
                name = "csv常规药",
                doseType = DoseType.MORNING_NOON_NIGHT,
                doseTimes = "08:00",
                dosePerIntake = 2.0,
                quantity = 10.0,
                createdAt = System.currentTimeMillis()
            )
        )
        app.repository.upsertMedication(
            Medication(
                name = "csv偶尔药",
                doseType = DoseType.MORNING_NOON_NIGHT,
                doseTimes = "",
                dosePerIntake = 1.0,
                quantity = -1.0,
                isOccasional = true,
                createdAt = System.currentTimeMillis()
            )
        )
        val today = LocalDate.now().toString()
        app.repository.addRecord(regularId, today, "08:00", RecordCategory.NORMAL, scheduledTime = "08:00")
        // 偶尔服药：无计划时段（scheduledTime 为空）
        app.repository.addRecord(
            regularId, today, "15:00", RecordCategory.OCCASIONAL, scheduledTime = ""
        )

        val csv = app.repository.exportCsv(app)

        // 清空后模拟换机迁移
        app.database.openHelper.writableDatabase.execSQL("DELETE FROM dose_records")
        app.database.openHelper.writableDatabase.execSQL("DELETE FROM medications")

        val imported = app.repository.importCsv(csv)
        assertEquals("两条记录都应成功导入", 2, imported)

        // 往返后药品数值无精度损失
        val meds = app.repository.observeAllMedications().first()
        val regular = meds.first { it.name == "csv常规药" }
        assertEquals(2.0, regular.dosePerIntake, 0.0)
        assertEquals(10.0, regular.quantity, 0.0)

        // 记录完整保留：常规 + 偶尔
        val records = app.repository.observeAllRecords().first()
        assertEquals(2, records.size)
        val occasional = records.first { it.category == RecordCategory.OCCASIONAL }
        assertEquals("偶尔服药的计划时段应为空", "", occasional.scheduledTime)

        // 重复导入同一份不应产生新记录（去重生效）
        val reimport = app.repository.importCsv(csv)
        assertEquals("重复导入应去重为 0 条", 0, reimport)
    }
}