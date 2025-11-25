package com.example.pixeldiet.repository

import android.content.Context
import android.content.SharedPreferences
import com.example.pixeldiet.model.AppName
import com.example.pixeldiet.model.NotificationSettings
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class NotificationPrefs(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("PixelDietPrefs", Context.MODE_PRIVATE)

    private val todayString: String
        get() = SimpleDateFormat("yyyy-MM-dd", Locale.KOREAN).format(Date())

    // 선택된 앱 목록 저장
    fun saveSelectedApps(selectedApps: List<AppName>) {
        val joinedNames = selectedApps.joinToString(",") { it.name }
        prefs.edit().putString("selected_apps", joinedNames).apply()
    }

    // 선택된 앱 목록 불러오기
    fun loadSelectedApps(): List<AppName> {
        val savedString = prefs.getString("selected_apps", null)
        // ⭐️ [수정] 기본값으로 YOUTUBE를 사용합니다.
        if (savedString.isNullOrEmpty()) {
            return listOf(AppName.NAVER_WEBTOON, AppName.INSTAGRAM, AppName.YOUTUBE)
        }

        return try {
            savedString.split(",").map { AppName.valueOf(it) }
        } catch (e: Exception) {
            // ⭐️ [수정] 예외 발생 시에도 YOUTUBE를 기본값으로 사용합니다.
            listOf(AppName.NAVER_WEBTOON, AppName.INSTAGRAM, AppName.YOUTUBE)
        }
    }

    fun saveNotificationSettings(settings: NotificationSettings) {
        prefs.edit().apply {
            putBoolean("ind_50", settings.individualApp50)
            putBoolean("ind_70", settings.individualApp70)
            putBoolean("ind_100", settings.individualApp100)
            putBoolean("total_50", settings.total50)
            putBoolean("total_70", settings.total70)
            putBoolean("total_100", settings.total100)
            putInt("repeat_interval", settings.repeatIntervalMinutes)
            apply()
        }
    }

    fun loadNotificationSettings(): NotificationSettings {
        return NotificationSettings(
            individualApp50 = prefs.getBoolean("ind_50", true),
            individualApp70 = prefs.getBoolean("ind_70", true),
            individualApp100 = prefs.getBoolean("ind_100", true),
            total50 = prefs.getBoolean("total_50", true),
            total70 = prefs.getBoolean("total_70", true),
            total100 = prefs.getBoolean("total_100", true),
            repeatIntervalMinutes = prefs.getInt("repeat_interval", 5)
        )
    }

    fun hasSentToday(type: String): Boolean {
        return prefs.getString(type, null) == todayString
    }

    fun recordSentToday(type: String) {
        prefs.edit().putString(type, todayString).apply()
    }

    fun getLastRepeatSentTime(type: String): Long {
        return prefs.getLong(type, 0L)
    }

    fun recordRepeatSentTime(type: String) {
        prefs.edit().putLong(type, System.currentTimeMillis()).apply()
    }
}