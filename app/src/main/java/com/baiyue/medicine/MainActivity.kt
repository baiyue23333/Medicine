package com.baiyue.medicine

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.lifecycleScope
import com.baiyue.medicine.i18n.AppLocale
import com.baiyue.medicine.reminder.KeepAliveService
import com.baiyue.medicine.reminder.ReminderScheduler
import com.baiyue.medicine.ui.calendar.CalendarScreen
import com.baiyue.medicine.ui.card.CardScreen
import com.baiyue.medicine.ui.checkin.CheckInScreen
import com.baiyue.medicine.ui.components.AppBackdrop
import com.baiyue.medicine.ui.components.AppBottomBar
import com.baiyue.medicine.ui.components.AppGlyph
import com.baiyue.medicine.ui.manage.ManageScreen
import com.baiyue.medicine.ui.settings.SettingsScreen
import com.baiyue.medicine.ui.theme.MedicineTheme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    /** 在界面创建前套用语言设置 */
    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(AppLocale.localized(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        requestNotificationPermissionIfNeeded()
        initReminders()
        setContent {
            MedicineTheme {
                MedicineAppRoot()
            }
        }
    }

    /** Android 13+ 通知需要运行时授权，否则提醒通知会被系统丢弃 */
    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), REQUEST_NOTIFICATION)
        }
    }

    /** 启动时按设置排程提醒，并按设置启动强保活前台服务 */
    private fun initReminders() {
        val app = application as MedicineApp
        lifecycleScope.launch {
            runCatching {
                val meds = app.repository.observeAllMedications().first()
                val s = app.settingsRepository.settings.first()
                ReminderScheduler.sync(
                    this@MainActivity,
                    meds,
                    s.reminderAdvanceMinutes,
                    s.reminderEnabled,
                    s.missedReminderEnabled
                )
                if (s.keepAlive) KeepAliveService.start(this@MainActivity)
            }
        }
    }

    private companion object {
        const val REQUEST_NOTIFICATION = 1001
    }
}

private data class Tab(@StringRes val labelRes: Int, val glyph: AppGlyph)

private val tabs = listOf(
    Tab(R.string.tab_check_in, AppGlyph.Sun),
    Tab(R.string.tab_calendar, AppGlyph.Calendar),
    Tab(R.string.tab_manage, AppGlyph.Pill),
    Tab(R.string.tab_card, AppGlyph.Heart)
)

@Composable
fun MedicineAppRoot() {
    val pagerState = rememberPagerState(initialPage = 0, pageCount = { tabs.size })
    val scope = rememberCoroutineScope()
    var settingsOpen by rememberSaveable { mutableStateOf(false) }

    BackHandler(enabled = settingsOpen) { settingsOpen = false }

    AppBackdrop {
        Scaffold(
            containerColor = Color.Transparent,
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            bottomBar = {
                AnimatedVisibility(
                    visible = !settingsOpen,
                    enter = fadeIn(tween(200)) + slideInVertically(),
                    exit = fadeOut(tween(140)) + slideOutVertically()
                ) {
                    AppBottomBar(
                        labels = tabs.map { stringResource(it.labelRes) },
                        glyphs = tabs.map { it.glyph },
                        selectedIndex = pagerState.currentPage,
                        onSelect = { page ->
                            scope.launch { pagerState.animateScrollToPage(page) }
                        }
                    )
                }
            }
        ) { padding ->
            Box(
                Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize(),
                    beyondViewportPageCount = 1,
                    userScrollEnabled = !settingsOpen
                ) { page ->
                    when (page) {
                        0 -> CheckInScreen(
                            onOpenSettings = { settingsOpen = true },
                            isActive = pagerState.currentPage == 0,
                            onOpenManage = {
                                scope.launch { pagerState.animateScrollToPage(2) }
                            }
                        )
                        1 -> CalendarScreen(onOpenSettings = { settingsOpen = true })
                        2 -> ManageScreen(onOpenSettings = { settingsOpen = true })
                        3 -> CardScreen(onOpenSettings = { settingsOpen = true })
                    }
                }

                AnimatedVisibility(
                    visible = settingsOpen,
                    enter = slideInHorizontally(tween(280)) { it / 3 } + fadeIn(tween(180)),
                    exit = slideOutHorizontally(tween(220)) { it / 3 } + fadeOut(tween(160))
                ) {
                    Box(Modifier.fillMaxSize()) {
                        SettingsScreen(onBack = { settingsOpen = false })
                    }
                }
            }
        }
    }
}

private fun slideInVertically() = androidx.compose.animation.slideInVertically { it / 2 }

private fun slideOutVertically() = androidx.compose.animation.slideOutVertically { it / 2 }
