package com.example.pixeldiet.repository

import android.app.usage.UsageStats
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.pixeldiet.model.AppName
import com.example.pixeldiet.model.AppUsage
import com.example.pixeldiet.model.DailyUsage
import com.example.pixeldiet.model.NotificationSettings
import java.text.SimpleDateFormat
import java.util.*

object UsageRepository {

    private var prefs: NotificationPrefs? = null

    private val _appUsageList = MutableLiveData<List<AppUsage>>()
    val appUsageList: LiveData<List<AppUsage>> = _appUsageList

    private val _dailyUsageList = MutableLiveData<List<DailyUsage>>()
    val dailyUsageList: LiveData<List<DailyUsage>> = _dailyUsageList

    private val _notificationSettings = MutableLiveData<NotificationSettings>()
    val notificationSettings: LiveData<NotificationSettings> = _notificationSettings

    private val currentGoals = mutableMapOf<AppName, Int>()

    init {
        // 초기화 시 빈 리스트로 시작 (나중에 loadRealData에서 채움)
        _appUsageList.postValue(emptyList())
    }

    private fun getPrefs(context: Context): NotificationPrefs {
        if (prefs == null) {
            prefs = NotificationPrefs(context.applicationContext)
            _notificationSettings.postValue(prefs!!.loadNotificationSettings())
        }
        return prefs!!
    }

    // ⭐️ [신규] 선택된 앱 목록 업데이트
    fun updateSelectedApps(context: Context, newApps: List<AppName>) {
        getPrefs(context).saveSelectedApps(newApps)
    }

    fun updateNotificationSettings(settings: NotificationSettings, context: Context) {
        getPrefs(context).saveNotificationSettings(settings)
        _notificationSettings.postValue(settings)
    }

    fun updateGoalTimes(goals: Map<AppName, Int>) {
        currentGoals.clear()
        currentGoals.putAll(goals)
        val currentList = _appUsageList.value ?: return
        val newList = currentList.map {
            it.copy(goalTime = currentGoals[it.appName] ?: 0)
        }
        _appUsageList.postValue(newList)
    }

    suspend fun loadRealData(context: Context) {
        val prefs = getPrefs(context)
        // ⭐️ [수정] 저장된 앱 목록을 불러옵니다.
        val targetApps = prefs.loadSelectedApps()

        val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val packageManager = context.packageManager
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.KOREAN)
        val calendar = Calendar.getInstance()

        // 1. 오늘 사용량
        calendar.set(Calendar.HOUR_OF_DAY, 0); calendar.set(Calendar.MINUTE, 0); calendar.set(Calendar.SECOND, 0)
        val startTime = calendar.timeInMillis
        val endTime = System.currentTimeMillis()
        val todayStats = usageStatsManager.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, startTime, endTime)
        val todayUsageMap = parseUsageStats(todayStats, targetApps)

        // 2. 과거 30일 사용량
        calendar.add(Calendar.DAY_OF_MONTH, -30)
        val thirtyDaysAgo = calendar.timeInMillis
        val dailyStats = usageStatsManager.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, thirtyDaysAgo, endTime)

        // ⭐️ [수정] targetApps에 있는 앱만 필터링
        val dailyUsageMap = mutableMapOf<String, MutableMap<AppName, Int>>()
        val targetPackages = targetApps.map { it.packageName }.toSet()

        for (stat in dailyStats) {
            if (stat.packageName in targetPackages) {
                // 앱 목록에 있는 경우만 처리
                val appName = targetApps.find { it.packageName == stat.packageName }
                if (appName != null) {
                    val date = sdf.format(Date(stat.firstTimeStamp))
                    val usageInMinutes = (stat.totalTimeInForeground / (1000 * 60)).toInt()
                    val dayMap = dailyUsageMap.getOrPut(date) { mutableMapOf() }
                    dayMap[appName] = (dayMap[appName] ?: 0) + usageInMinutes
                }
            }
        }

        val newDailyList = dailyUsageMap.map { (date, usages) ->
            DailyUsage(date = date, appUsages = usages)
        }.sortedBy { it.date }
        _dailyUsageList.postValue(newDailyList)

        // 3. 스트릭 계산
        val streakMap = calculateStreaks(newDailyList, currentGoals, targetApps)

        // 4. 최종 리스트 생성 (targetApps 기준)
        val newAppUsageList = targetApps.map { appName ->
            val appIcon: Drawable? = try {
                packageManager.getApplicationIcon(appName.packageName)
            } catch (e: Exception) { null }

            val todayUsage = todayUsageMap[appName] ?: 0
            val goal = currentGoals[appName] ?: 0
            val pastStreak = streakMap[appName] ?: 0
            var finalStreak = pastStreak

            if (goal > 0 && todayUsage > goal) {
                if (pastStreak < 0) finalStreak = pastStreak - 1
                else finalStreak = -1
            }

            AppUsage(
                appName = appName,
                currentUsage = todayUsage,
                goalTime = goal,
                streak = finalStreak,
                icon = appIcon
            )
        }
        _appUsageList.postValue(newAppUsageList)
    }

    // ⭐️ [수정] targetApps만 파싱
    private fun parseUsageStats(stats: List<UsageStats>, targetApps: List<AppName>): Map<AppName, Int> {
        val usageMap = mutableMapOf<AppName, Int>()
        val targetPackages = targetApps.map { it.packageName }.toSet()

        for (stat in stats) {
            if (stat.packageName in targetPackages) {
                val appName = targetApps.find { it.packageName == stat.packageName }
                if (appName != null) {
                    val usageInMinutes = (stat.totalTimeInForeground / (1000 * 60)).toInt()
                    usageMap[appName] = (usageMap[appName] ?: 0) + usageInMinutes
                }
            }
        }
        return usageMap
    }

    // ⭐️ [수정] targetApps만 계산
    private fun calculateStreaks(
        dailyList: List<DailyUsage>,
        goals: Map<AppName, Int>,
        targetApps: List<AppName>
    ): Map<AppName, Int> {
        val streakMap = mutableMapOf<AppName, Int>()
        val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.KOREAN).format(Date())
        val pastDays = dailyList.filter { it.date != todayStr }.sortedByDescending { it.date }

        if (pastDays.isEmpty()) {
            targetApps.forEach { streakMap[it] = 0 }
            return streakMap
        }

        for (appName in targetApps) {
            val goal = goals[appName] ?: 0
            if (goal == 0) {
                streakMap[appName] = 0
                continue
            }
            val firstDayUsage = pastDays.first().appUsages[appName] ?: 0
            val wasSuccess = firstDayUsage <= goal
            var streak = 0
            for (day in pastDays) {
                val usage = day.appUsages[appName] ?: 0
                if ((usage <= goal) == wasSuccess) streak++
                else break
            }
            streakMap[appName] = if (wasSuccess) streak else -streak
        }
        return streakMap
    }
}