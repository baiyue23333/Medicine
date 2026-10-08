package com.baiyue.medicine.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.baiyue.medicine.model.DoseType
import com.baiyue.medicine.model.UsageType

@Entity(tableName = "medications")
data class Medication(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    /** 自定义图片 URI（系统相册选择器，免权限） */
    val imageUri: String = "",
    val usageType: UsageType = UsageType.AFTER_MEAL,
    /** 自定义使用方式文案 */
    val customUsage: String = "",
    val doseType: DoseType = DoseType.MORNING_NOON_NIGHT,
    /** 具体服用时间点，逗号分隔 "HH:mm"，如 "08:00,12:00,18:00" */
    val doseTimes: String = "08:00,12:00,18:00",
    /** 每几小时服用（EVERY_HOURS 时使用） */
    val intervalHours: Int = 0,
    /** 每几小时服用的起始时间 "HH:mm"（EVERY_HOURS 时使用） */
    val intervalStartTime: String = "08:00",
    /** 每日服用次数（TIMES_PER_DAY 时使用） */
    val timesPerDay: Int = 1,
    /** 是否按次（定时）服用 */
    val isScheduled: Boolean = true,
    /** 是否偶尔服用 */
    val isOccasional: Boolean = false,
    /** 一次服用的剂量/数量（可小数，如 0.5 片） */
    val dosePerIntake: Double = 1.0,
    /** 当前药物数量（可小数，如半片）；-1 表示未设置总量 */
    val quantity: Double = UNKNOWN_QUANTITY,
    /** 最少持续数量阈值 */
    val minKeepQuantity: Double = 0.0,
    /** 提前预警天数 */
    val warningDays: Int = 3,
    val enabled: Boolean = true,
    /** 自定义备注 */
    val note: String = "",
    /** 该药专属的服药提醒文案；为空时使用设置里的全局文案 */
    val reminderText: String = "",
    /** 添加时间（epoch millis）；0 表示未知（升级前就已存在的药品），补签时不追溯其添加之前的日期 */
    val createdAt: Long = 0L
) {
    companion object {
        /** 未设置总量的哨兵值 */
        const val UNKNOWN_QUANTITY = -1.0
    }
}