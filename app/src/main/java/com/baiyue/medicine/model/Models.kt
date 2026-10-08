package com.baiyue.medicine.model

/** 服用次数类型 */
enum class DoseType { MORNING_NOON_NIGHT, EVERY_HOURS, TIMES_PER_DAY, CUSTOM }

/** 使用方式（默认枚举 + 自定义文案） */
enum class UsageType { BEFORE_MEAL, WITH_MEAL, AFTER_MEAL, PATCH, ORAL, INJECTION, INHALATION, EXTERNAL, CUSTOM }

/** 打卡记录类别；SKIPPED 表示该时段确认未服用（不扣库存、不计入服药统计） */
enum class RecordCategory { NORMAL, OCCASIONAL, SKIPPED }

/** 服药标注：NORMAL 正常 / EARLY 提前服用 / BACKFILL 补签（错过时段后补记） */
enum class RecordFlag { NORMAL, EARLY, BACKFILL }