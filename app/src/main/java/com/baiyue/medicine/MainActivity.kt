package com.baiyue.medicine

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.lifecycleScope
import com.baiyue.medicine.i18n.AppLocale
import com.baiyue.medicine.reminder.KeepAliveService
import com.baiyue.medicine.reminder.ReminderScheduler
import com.baiyue.medicine.ui.calendar.CalendarScreen
import com.baiyue.medicine.ui.card.CardScreen
import com.baiyue.medicine.ui.checkin.CheckInScreen
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

private data class Tab(@StringRes val labelRes: Int, val icon: ImageVector, val pageIndex: Int)

private val tabs = listOf(
    Tab(R.string.tab_check_in, Icons.Filled.CheckCircle, 1),
    Tab(R.string.tab_calendar, Icons.Filled.DateRange, 2),
    Tab(R.string.tab_manage, Icons.AutoMirrored.Filled.List, 3),
    Tab(R.string.tab_card, Icons.Filled.Star, 4)
)

@Composable
fun MedicineAppRoot() {
    val pagerState = rememberPagerState(initialPage = 1, pageCount = { tabs.size + 1 })
    val scope = rememberCoroutineScope()

    Scaffold(
        bottomBar = {
            NavigationBar {
                tabs.forEach { tab ->
                    val label = stringResource(tab.labelRes)
                    NavigationBarItem(
                        selected = pagerState.currentPage == tab.pageIndex,
                        onClick = { scope.launch { pagerState.animateScrollToPage(tab.pageIndex) } },
                        icon = { Icon(tab.icon, contentDescription = label) },
                        label = { Text(label) },
                        alwaysShowLabel = true
                    )
                }
            }
        }
    ) { padding ->
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            beyondViewportPageCount = 1
        ) { page ->
            when (page) {
                0 -> SettingsScreen()
                1 -> CheckInScreen()
                2 -> CalendarScreen()
                3 -> ManageScreen()
                4 -> CardScreen()
            }
        }
    }
}