package com.baiyue.medicine

import android.app.Application
import com.baiyue.medicine.data.MedicineDatabase
import com.baiyue.medicine.data.MedicineRepository
import com.baiyue.medicine.data.SettingsRepository
import com.baiyue.medicine.i18n.AppLocale
import com.baiyue.medicine.reminder.NotificationHelper

class MedicineApp : Application() {

    val database: MedicineDatabase by lazy { MedicineDatabase.getInstance(this) }
    val repository: MedicineRepository by lazy { MedicineRepository(database) }
    val settingsRepository: SettingsRepository by lazy { SettingsRepository(this) }

    override fun onCreate() {
        super.onCreate()
        instance = this
        // 必须在任何 Activity 之前完成，界面才能拿到正确的语言
        AppLocale.init(this)
        NotificationHelper.createChannels(this)
    }

    companion object {
        lateinit var instance: MedicineApp
    }
}