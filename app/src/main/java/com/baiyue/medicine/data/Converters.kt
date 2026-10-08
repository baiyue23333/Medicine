package com.baiyue.medicine.data

import androidx.room.TypeConverter
import com.baiyue.medicine.model.DoseType
import com.baiyue.medicine.model.RecordCategory
import com.baiyue.medicine.model.RecordFlag
import com.baiyue.medicine.model.UsageType

class Converters {
    @TypeConverter fun doseTypeToString(v: DoseType): String = v.name
    @TypeConverter fun stringToDoseType(s: String): DoseType = DoseType.valueOf(s)

    @TypeConverter fun usageTypeToString(v: UsageType): String = v.name
    @TypeConverter fun stringToUsageType(s: String): UsageType =
        runCatching { UsageType.valueOf(s) }.getOrDefault(UsageType.AFTER_MEAL)

    @TypeConverter fun categoryToString(v: RecordCategory): String = v.name
    @TypeConverter fun stringToCategory(s: String): RecordCategory = RecordCategory.valueOf(s)

    @TypeConverter fun flagToString(v: RecordFlag): String = v.name
    @TypeConverter fun stringToFlag(s: String): RecordFlag = RecordFlag.valueOf(s)
}