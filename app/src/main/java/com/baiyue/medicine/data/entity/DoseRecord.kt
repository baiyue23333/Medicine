package com.baiyue.medicine.data.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.baiyue.medicine.model.RecordCategory
import com.baiyue.medicine.model.RecordFlag

@Entity(
    tableName = "dose_records",
    indices = [Index("date"), Index("plannedDate")]
)
data class DoseRecord(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val medicationId: Long,
    /** 日期 yyyy-MM-dd（实际发生/展示日期） */
    val date: String,
    /** 实际打卡时间 HH:mm */
    val time: String,
    val category: RecordCategory = RecordCategory.NORMAL,
    val taken: Boolean = true,
    val note: String = "",
    /** 计划服用时间槽位 HH:mm（用于「该时段是否已打卡」去重，偶尔服药为空） */
    val scheduledTime: String = "",
    /** 服药标注：正常 / 提前 / 补签 */
    val flag: RecordFlag = RecordFlag.NORMAL,
    /** 计划所属日期 yyyy-MM-dd；空表示与 date 相同（补签跨日时 date 为实际当天、plannedDate 为计划日） */
    val plannedDate: String = "",
    /** 记录写入时的药品名称快照；即使药品之后被删除，历史记录仍能显示名称 */
    val medicationName: String = ""
)