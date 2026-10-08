package com.baiyue.medicine

import android.Manifest
import android.app.NotificationManager
import android.content.Intent
import androidx.annotation.StringRes
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.GrantPermissionRule
import com.baiyue.medicine.data.entity.DoseRecord
import com.baiyue.medicine.data.entity.Medication
import com.baiyue.medicine.i18n.AppLocale
import com.baiyue.medicine.model.DoseType
import com.baiyue.medicine.model.RecordCategory
import com.baiyue.medicine.model.RecordFlag
import com.baiyue.medicine.prediction.doseSlotDate
import com.baiyue.medicine.reminder.ReminderReceiver
import com.baiyue.medicine.reminder.ReminderScheduler
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assume
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate
import java.time.LocalTime

/**
 * 打卡端到端：点击打卡后应写入记录（含计划时段）并按一次剂量扣减总量。
 * 打卡按钮 testTag = checkInButton。
 */
@RunWith(AndroidJUnit4::class)
class CheckInFlowTest {

    @get:Rule(order = 0)
    val permissionRule: GrantPermissionRule = GrantPermissionRule.grant(Manifest.permission.POST_NOTIFICATIONS)

    @get:Rule(order = 1)
    val composeRule = createEmptyComposeRule()

    private val app: MedicineApp = ApplicationProvider.getApplicationContext()
    private val testName = "测试用药超长名称"
    private val backfillName = "测试补签用药"

    /** 取当前时刻作为计划时段：恒落在打卡窗口内，不会被判定为「已错过」 */
    private val testSlot: String = LocalTime.now().let { "%02d:%02d".format(it.hour, it.minute) }

    /** 测试期间被临时停用的其它药品，用于结束后恢复 */
    private var otherEnabledMeds: List<Medication> = emptyList()

    @After
    fun tearDown() {
        restoreOtherMedications()
        runBlocking {
            val testMeds = app.repository.observeAllMedications().first()
                .filter { it.name == testName || it.name == backfillName }
            val nm = notificationManager()
            testMeds.forEach { nm.cancel(missedNotificationId(it.id)) }
            app.database.openHelper.writableDatabase.execSQL(
                "DELETE FROM dose_records WHERE medicationId IN (SELECT id FROM medications WHERE name IN (?, ?))",
                arrayOf(testName, backfillName)
            )
            testMeds.forEach { app.repository.deleteMedication(it) }
        }
    }

    @Test
    fun 打卡写入记录并按剂量扣减总量() {
        val id = runBlocking {
            app.repository.upsertMedication(
                Medication(
                    name = testName,
                    doseType = DoseType.MORNING_NOON_NIGHT,
                    doseTimes = testSlot,
                    dosePerIntake = 2.0,
                    quantity = 10.0,
                    note = "测试备注",
                    createdAt = System.currentTimeMillis()
                )
            )
        }

        ActivityScenario.launch(MainActivity::class.java).use {
            dismissWarningDialogIfShown()

            // 长名称默认截断显示，剂量与备注正常展示
            composeRule.awaitText("测试用药…")
            composeRule.awaitText(str(R.string.label_intake, "2"))
            composeRule.awaitText("测试备注")

            composeRule.onNodeWithTag("checkInButton").performClick()

            val record = awaitRecord(id)
            assertNotNull("打卡后应写入当天记录", record)
            assertEquals(RecordCategory.NORMAL, record!!.category)
            assertEquals(testSlot, record.scheduledTime)

            // 写记录与扣库存是两次写库，需轮询等待扣减生效
            assertEquals("总量应按一次剂量 2 扣减", 8.0, awaitQuantity(id, 8.0), 0.001)
        }
    }

    @Test
    fun 打卡页与底部导航渲染正常() {
        ActivityScenario.launch(MainActivity::class.java).use {
            dismissWarningDialogIfShown()
            composeRule.awaitText(str(R.string.checkin_title))
            listOf(R.string.tab_calendar, R.string.tab_manage, R.string.tab_card).forEach { res ->
                composeRule.awaitText(str(res))
            }
        }
    }

    @Test
    fun 错过时段可补签并写入记录() {
        val slot = missedSlotForTest()
        val id = createBackfillMedication(slot)

        ActivityScenario.launch(MainActivity::class.java).use {
            dismissWarningDialogIfShown()

            composeRule.awaitTag("missedBanner")
            composeRule.onNodeWithTag("missedBanner").performClick()

            composeRule.awaitText(str(R.string.checkin_backfill_dialog_title))
            composeRule.onNodeWithTag("backfillConfirm").performClick()

            val record = awaitRecord(id, slot)
            assertNotNull("补签后应写入当天记录", record)
            assertEquals(RecordCategory.NORMAL, record!!.category)
            assertEquals("补签记录应保留原计划时段", slot, record.scheduledTime)

            assertEquals("补签应按一次剂量扣减总量", 4.0, awaitQuantity(id, 4.0), 0.001)
        }
    }

    @Test
    fun 已错过项可删除且不再提示() {
        val slot = missedSlotForTest()
        val id = createBackfillMedication(slot)

        ActivityScenario.launch(MainActivity::class.java).use {
            dismissWarningDialogIfShown()

            composeRule.awaitTag("missedBanner")
            composeRule.onNodeWithTag("missedBanner").performClick()

            composeRule.awaitText(str(R.string.checkin_backfill_dialog_title))
            composeRule.onNodeWithTag("backfillManage").performClick()
            composeRule.awaitText(str(R.string.action_delete))
            composeRule.onNodeWithText(str(R.string.action_delete)).performClick()

            // 二次确认
            composeRule.awaitText(str(R.string.checkin_backfill_delete_title))
            composeRule.onNodeWithTag("backfillDeleteConfirm").performClick()

            // 删除后提示条应消失
            composeRule.waitUntil(TIMEOUT) {
                composeRule.onAllNodesWithTag("missedBanner").fetchSemanticsNodes().isEmpty()
            }

            val record = awaitRecord(id, slot)
            assertNotNull("删除后应写入跳过记录", record)
            assertEquals(RecordCategory.SKIPPED, record!!.category)

            // 留出写库时间，再确认库存未被扣减
            Thread.sleep(POLL_INTERVAL * 3)
            val updated = runBlocking {
                app.repository.observeAllMedications().first().first { it.id == id }
            }
            assertEquals("删除不应扣减库存", 5.0, updated.quantity, 0.001)
        }
    }

    @Test
    fun 可补签昨天的漏服时段() {
        val nowMin = LocalTime.now().let { it.hour * 60 + it.minute }
        // 取「今天尚未到来」的时段：它属于昨天且昨天已错过，可跨天补签
        Assume.assumeTrue("需在 22:00 前运行才能构造该场景", nowMin < 22 * 60)
        disableOtherMedications()

        val slotMin = nowMin + 120
        val slot = "%02d:%02d".format(slotMin / 60, slotMin % 60)
        // 药品是两天前添加的，因此昨天的时段也在补签范围内
        val id = createBackfillMedication(slot, System.currentTimeMillis() - 2 * DAY_MILLIS)

        ActivityScenario.launch(MainActivity::class.java).use {
            dismissWarningDialogIfShown()

            composeRule.awaitTag("missedBanner")
            composeRule.onNodeWithTag("missedBanner").performClick()

            composeRule.awaitText(str(R.string.checkin_backfill_dialog_title))
            // 昨天的项会额外标注「昨天」
            composeRule.awaitText(str(R.string.checkin_yesterday))
            composeRule.onNodeWithTag("backfillConfirm").performClick()

            val yesterday = LocalDate.now().minusDays(1).toString()
            val today = LocalDate.now().toString()
            // 补签跨零点：记录落在实际当天，计划日标记为昨天
            val record = awaitRecord(id, slot, today)
            assertNotNull("昨天的漏服时段应可补签", record)
            assertEquals(RecordFlag.BACKFILL, record!!.flag)
            assertEquals(yesterday, record.plannedDate)
            assertEquals(slot, record.scheduledTime)
        }
    }

    @Test
    fun 错过未打卡会发送补签提醒() {
        val slot = pastSlotForTest()
        val id = createBackfillMedication(slot)
        val notifId = missedNotificationId(id)

        sendMissedBroadcast(id, slot)
        assertTrue("未打卡的错过时段应发送补签提醒", awaitNotification(notifId))

        notificationManager().cancel(notifId)
    }

    @Test
    fun 已打卡的时段不再发送补签提醒() {
        val slot = pastSlotForTest()
        val id = createBackfillMedication(slot)
        val notifId = missedNotificationId(id)
        val (hour, minute) = slot.split(":").map { it.toInt() }

        runBlocking {
            app.repository.addRecord(
                id,
                doseSlotDate(hour, minute).toString(),
                slot,
                RecordCategory.NORMAL,
                scheduledTime = slot
            )
        }

        sendMissedBroadcast(id, slot)
        Thread.sleep(NOTIFICATION_SETTLE_MS)
        assertFalse(
            "已打卡的时段不应再提醒补签",
            notificationManager().activeNotifications.any { it.id == notifId }
        )
    }

    @Test
    fun 切换语言为英文后界面显示英文() {
        AppLocale.set(app, AppLocale.ENGLISH)
        try {
            ActivityScenario.launch(MainActivity::class.java).use {
                composeRule.awaitText(str(R.string.checkin_title))
                composeRule.awaitText(str(R.string.tab_calendar))
            }
        } finally {
            AppLocale.set(app, AppLocale.SYSTEM)
        }
    }

    /** 取一个已经过去的时段（允许跨零点）；通知类用例不依赖运行时刻 */
    private fun pastSlotForTest(): String {
        val nowMin = LocalTime.now().let { it.hour * 60 + it.minute }
        val m = ((nowMin - 120) % 1440 + 1440) % 1440
        return "%02d:%02d".format(m / 60, m % 60)
    }

    /** 构造一个「已错过」的当日时段；凌晨 0:00-1:59 不存在这样的当日时段 */
    private fun missedSlotForTest(): String {
        val nowMin = LocalTime.now().let { it.hour * 60 + it.minute }
        Assume.assumeTrue("凌晨时段无法构造已错过的当日时段", nowMin >= 120)
        disableOtherMedications()
        val missedMin = nowMin - 120
        return "%02d:%02d".format(missedMin / 60, missedMin % 60)
    }

    /**
     * 补签弹窗默认全选，若设备上已有真实药品的漏服项会被一并写入（且扣减其库存）。
     * 测试期间临时停用其它药品，结束后在 tearDown 中恢复，避免污染既有数据。
     */
    private fun disableOtherMedications() {
        otherEnabledMeds = runBlocking {
            app.repository.observeAllMedications().first()
                .filter { it.enabled && it.isScheduled && it.name != backfillName && it.name != testName }
        }
        runBlocking {
            otherEnabledMeds.forEach { app.repository.upsertMedication(it.copy(enabled = false)) }
        }
    }

    private fun restoreOtherMedications() {
        runBlocking { otherEnabledMeds.forEach { app.repository.upsertMedication(it) } }
        otherEnabledMeds = emptyList()
    }

    /** 创建补签用药品；[createdAt] 默认「刚添加」，需要覆盖昨天时传入更早的时间 */
    private fun createBackfillMedication(
        slot: String,
        createdAt: Long = System.currentTimeMillis()
    ): Long = runBlocking {
        app.repository.upsertMedication(
            Medication(
                name = backfillName,
                doseType = DoseType.MORNING_NOON_NIGHT,
                doseTimes = slot,
                dosePerIntake = 1.0,
                quantity = 5.0,
                createdAt = createdAt
            )
        )
    }

    private fun notificationManager(): NotificationManager =
        app.getSystemService(NotificationManager::class.java)

    private fun missedNotificationId(medicationId: Long): Int = (5000 + medicationId).toInt()

    /** 直接投递「错过检查」广播：等价于闹钟触发 */
    private fun sendMissedBroadcast(medicationId: Long, slot: String) {
        val (hour, minute) = slot.split(":").map { it.toInt() }
        app.sendBroadcast(
            Intent(app, ReminderReceiver::class.java).apply {
                action = ReminderScheduler.ACTION_MISSED
                putExtra(ReminderScheduler.EXTRA_MED_ID, medicationId)
                putExtra(ReminderScheduler.EXTRA_SLOT_LABEL, "早")
                putExtra(ReminderScheduler.EXTRA_SLOT_HOUR, hour)
                putExtra(ReminderScheduler.EXTRA_SLOT_MINUTE, minute)
                putExtra(ReminderScheduler.EXTRA_SLOT_INDEX, 0)
            }
        )
    }

    private fun awaitNotification(id: Int): Boolean {
        repeat(POLL_TIMES) {
            if (notificationManager().activeNotifications.any { it.id == id }) return true
            Thread.sleep(POLL_INTERVAL)
        }
        return false
    }

    private fun dismissWarningDialogIfShown() {
        val gotIt = str(R.string.checkin_got_it)
        val checkInTitle = str(R.string.checkin_title)
        composeRule.waitUntil(TIMEOUT) {
            composeRule.onAllNodesWithText(gotIt).fetchSemanticsNodes().isNotEmpty() ||
                composeRule.onAllNodesWithText(checkInTitle).fetchSemanticsNodes().isNotEmpty()
        }
        if (composeRule.onAllNodesWithText(gotIt).fetchSemanticsNodes().isNotEmpty()) {
            composeRule.onNodeWithText(gotIt).performClick()
        }
    }

    /** 按当前界面语言取文案，避免断言写死中文 */
    private fun str(@StringRes resId: Int, vararg args: Any): String =
        AppLocale.localized(app).getString(resId, *args)

    /** 轮询等待库存扣减生效，返回最终值 */
    private fun awaitQuantity(medicationId: Long, expected: Double): Double {
        var quantity = Double.NaN
        repeat(POLL_TIMES) {
            quantity = runBlocking {
                app.repository.observeAllMedications().first()
                    .first { it.id == medicationId }.quantity
            }
            if (kotlin.math.abs(quantity - expected) < 0.001) return quantity
            Thread.sleep(POLL_INTERVAL)
        }
        return quantity
    }

    private fun awaitRecord(
        medicationId: Long,
        slot: String = testSlot,
        date: String = LocalDate.now().toString()
    ): DoseRecord? {
        repeat(POLL_TIMES) {
            val record = runBlocking {
                app.repository.observeRecordsByDate(date).first()
                    .firstOrNull { it.medicationId == medicationId && it.scheduledTime == slot }
            }
            if (record != null) return record
            Thread.sleep(POLL_INTERVAL)
        }
        return null
    }

    private companion object {
        const val TIMEOUT = 10_000L
        const val POLL_TIMES = 50
        const val POLL_INTERVAL = 100L
        const val DAY_MILLIS = 24 * 60 * 60 * 1000L

        /** 等广播接收器异步处理完再断言「没有通知」 */
        const val NOTIFICATION_SETTLE_MS = 1_500L
    }
}

/** 等待指定文本出现在语义树中（该模拟器上窗口尺寸判定不稳定，故只校验存在性） */
private fun ComposeTestRule.awaitText(text: String) {
    waitUntil(10_000L) {
        onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty()
    }
}

/** 等待指定 testTag 的节点出现在语义树中 */
private fun ComposeTestRule.awaitTag(tag: String) {
    waitUntil(10_000L) {
        onAllNodesWithTag(tag).fetchSemanticsNodes().isNotEmpty()
    }
}
