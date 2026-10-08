package com.baiyue.medicine.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.baiyue.medicine.data.dao.DoseRecordDao
import com.baiyue.medicine.data.dao.MedicationDao
import com.baiyue.medicine.data.entity.DoseRecord
import com.baiyue.medicine.data.entity.Medication

@Database(
    entities = [Medication::class, DoseRecord::class],
    version = 8,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class MedicineDatabase : RoomDatabase() {
    abstract fun medicationDao(): MedicationDao
    abstract fun doseRecordDao(): DoseRecordDao

    companion object {
        @Volatile
        private var INSTANCE: MedicineDatabase? = null

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE medications ADD COLUMN intervalStartTime TEXT NOT NULL DEFAULT '08:00'")
                db.execSQL("ALTER TABLE medications ADD COLUMN dosePerIntake REAL NOT NULL DEFAULT 1.0")
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE dose_records ADD COLUMN scheduledTime TEXT NOT NULL DEFAULT ''")
                // 旧记录 time 即计划槽位时间，回填以便打卡去重继续生效
                db.execSQL("UPDATE dose_records SET scheduledTime = time WHERE scheduledTime = ''")
            }
        }

        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE medications ADD COLUMN note TEXT NOT NULL DEFAULT ''")
            }
        }

        /** createdAt 默认 0：视为「一直存在」，不影响升级前药品的补签范围 */
        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE medications ADD COLUMN createdAt INTEGER NOT NULL DEFAULT 0")
            }
        }

        /** 每药专属提醒文案，空则回落到设置里的全局文案 */
        private val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE medications ADD COLUMN reminderText TEXT NOT NULL DEFAULT ''")
            }
        }

        /** 服药标注（正常/提前/补签）+ 计划日期；旧数据回填 plannedDate = date */
        private val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE dose_records ADD COLUMN flag TEXT NOT NULL DEFAULT 'NORMAL'")
                db.execSQL("ALTER TABLE dose_records ADD COLUMN plannedDate TEXT NOT NULL DEFAULT ''")
                db.execSQL("UPDATE dose_records SET plannedDate = date WHERE plannedDate = ''")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_dose_records_plannedDate ON dose_records (plannedDate)")
            }
        }

        /**
         * 药品可同时「按次 + 偶尔」：新增 isScheduled（按次）列，同时删除 abbreviation 列；
         * 旧数据里「偶尔」「非偶尔」两种分别映射为 isScheduled=0 / 1。
         * minSdk 26（SQLite 3.18）不支持 DROP COLUMN，故采用「重建表」的方式删除列。
         */
        private val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `medications_new` (" +
                        "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`name` TEXT NOT NULL, " +
                        "`imageUri` TEXT NOT NULL, " +
                        "`usageType` TEXT NOT NULL, " +
                        "`customUsage` TEXT NOT NULL, " +
                        "`doseType` TEXT NOT NULL, " +
                        "`doseTimes` TEXT NOT NULL, " +
                        "`intervalHours` INTEGER NOT NULL, " +
                        "`intervalStartTime` TEXT NOT NULL, " +
                        "`timesPerDay` INTEGER NOT NULL, " +
                        "`isScheduled` INTEGER NOT NULL, " +
                        "`isOccasional` INTEGER NOT NULL, " +
                        "`dosePerIntake` REAL NOT NULL, " +
                        "`quantity` REAL NOT NULL, " +
                        "`minKeepQuantity` REAL NOT NULL, " +
                        "`warningDays` INTEGER NOT NULL, " +
                        "`enabled` INTEGER NOT NULL, " +
                        "`note` TEXT NOT NULL, " +
                        "`reminderText` TEXT NOT NULL, " +
                        "`createdAt` INTEGER NOT NULL" +
                        ")"
                )
                db.execSQL(
                    "INSERT INTO `medications_new` (`id`,`name`,`imageUri`,`usageType`,`customUsage`,`doseType`,`doseTimes`," +
                        "`intervalHours`,`intervalStartTime`,`timesPerDay`,`isScheduled`,`isOccasional`,`dosePerIntake`,`quantity`," +
                        "`minKeepQuantity`,`warningDays`,`enabled`,`note`,`reminderText`,`createdAt`) " +
                        "SELECT `id`,`name`,`imageUri`,`usageType`," +
                        "`customUsage`,`doseType`,`doseTimes`,`intervalHours`,`intervalStartTime`,`timesPerDay`," +
                        "CASE WHEN `isOccasional` = 1 THEN 0 ELSE 1 END," +
                        "`isOccasional`,`dosePerIntake`,`quantity`,`minKeepQuantity`,`warningDays`,`enabled`,`note`,`reminderText`,`createdAt` " +
                        "FROM `medications`"
                )
                db.execSQL("DROP TABLE `medications`")
                db.execSQL("ALTER TABLE `medications_new` RENAME TO `medications`")
            }
        }

        fun getInstance(context: Context): MedicineDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    MedicineDatabase::class.java,
                    "medicine.db"
                ).addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8)
                    .build().also { INSTANCE = it }
            }
    }
}