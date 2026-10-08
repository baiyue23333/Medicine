package com.baiyue.medicine.i18n

import android.app.Activity
import android.app.LocaleManager
import android.content.Context
import android.content.ContextWrapper
import android.content.res.Configuration
import android.content.res.Resources
import android.os.Build
import android.os.LocaleList
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.Locale

/**
 * 应用内语言：跟随系统 / 简体中文 / English。
 *
 * API 33 及以上同时写入系统的「应用语言」，会在系统设置中体现；
 * 更低版本通过包装 Context 生效。两条路径读取的是同一份偏好，表现一致。
 */
object AppLocale {

    const val SYSTEM = ""
    const val CHINESE = "zh"
    const val ENGLISH = "en"

    private const val PREFS = "app_locale"
    private const val KEY = "language"

    private val _language = MutableStateFlow(SYSTEM)

    /** 当前语言设置，供设置页展示 */
    val language: StateFlow<String> get() = _language

    fun init(context: Context) {
        val tag = read(context)
        _language.value = tag
        apply(context, tag)
    }

    fun set(context: Context, tag: String) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putString(KEY, tag).apply()
        _language.value = tag
        apply(context, tag)
    }

    /** 取已按设置本地化的 Context，供通知、导出等非 Compose 场景使用 */
    fun localized(context: Context): Context {
        val tag = _language.value
        if (tag.isEmpty()) return context
        val config = Configuration(context.resources.configuration).apply {
            setLocale(Locale.forLanguageTag(tag))
        }
        return context.createConfigurationContext(config)
    }

    private fun read(context: Context): String =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY, SYSTEM) ?: SYSTEM

    private fun apply(context: Context, tag: String) {
        val locale = if (tag.isEmpty()) null else Locale.forLanguageTag(tag)
        Locale.setDefault(locale ?: systemLocale())
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val lm = context.getSystemService(LocaleManager::class.java) ?: return
            lm.applicationLocales =
                if (locale == null) LocaleList.getEmptyLocaleList() else LocaleList(locale)
        }
    }

    private fun systemLocale(): Locale = Resources.getSystem().configuration.locales[0]
}

/** 从 Context 包装链中取出 Activity，用于切换语言后重建界面 */
fun Context.findActivity(): Activity? {
    var current: Context? = this
    while (current is ContextWrapper) {
        if (current is Activity) return current
        current = current.baseContext
    }
    return null
}
