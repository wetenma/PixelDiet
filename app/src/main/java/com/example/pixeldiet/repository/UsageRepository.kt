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
import kotlin.random.Random

object UsageRepository {

    private val _appUsageList = MutableLiveData<List<AppUsage>>()
    val appUsageList: LiveData<List<AppUsage>> = _appUsageList

    private val _dailyUsageList = MutableLiveData<List<DailyUsage>>()
    val dailyUsageList: LiveData<List<DailyUsage>> = _dailyUsageList

    private val _notificationSettings = MutableLiveData<NotificationSettings>()
    val notificationSettings: LiveData<NotificationSettings> = _notificationSettings

    private val currentGoals = mutableMapOf<AppName, Int>()

    init {
        _notificationSettings.value = NotificationSettings()

        // 앱이 시작될 때 0값으로 초기화
        val initialList = AppName.values().map { appName ->
            AppUsage(
                appName = appName,
                currentUsage = 0,
                goalTime = 0,
                streak = 0,
                icon = null
            )
        }
        _appUsageList.postValue(initialList)
    }

    fun updateGoalTimes(goals: Map<AppName, Int>) {
        currentGoals.clear()
        currentGoals.putAll(goals)

        val currentList = _appUsageList.value!!
        val newList = currentList.map {
            it.copy(goalTime = currentGoals[it.appName] ?: 0)
        }
        _appUsageList.postValue(newList)
    }

    fun updateNotificationSettings(newSettings: NotificationSettings) {
        _notificationSettings.postValue(newSettings)
    }

    suspend fun loadRealData(context: Context) {
        val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val packageManager = context.packageManager
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.KOREAN)
        val calendar = Calendar.getInstance()

        // --- 1. 오늘 사용량 계산 ---
        calendar.set(Calendar.HOUR_OF_DAY, 0); calendar.set(Calendar.MINUTE, 0); calendar.set(Calendar.SECOND, 0)
        val startTime = calendar.timeInMillis
        val endTime = System.currentTimeMillis()
        val todayStats = usageStatsManager.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, startTime, endTime)
        val todayUsageMap = parseUsageStats(todayStats)

        // --- 2. 지난 30일 사용량 계산 ---
        calendar.add(Calendar.DAY_OF_MONTH, -30)
        val thirtyDaysAgo = calendar.timeInMillis
        val dailyStats = usageStatsManager.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, thirtyDaysAgo, endTime)
        val dailyUsageMap = mutableMapOf<String, MutableMap<AppName, Int>>()
        val appPackages = AppName.values().map { it.packageName }.toSet()

        for (stat in dailyStats) {
            if (stat.packageName in appPackages) {
                val appName = AppName.values().find { it.packageName == stat.packageName }!!
                val date = sdf.format(Date(stat.firstTimeStamp))
                val usageInMinutes = (stat.totalTimeInForeground / (1000 * 60)).toInt()
                val dayMap = dailyUsageMap.getOrPut(date) { mutableMapOf() }
                dayMap[appName] = (dayMap[appName] ?: 0) + usageInMinutes
            }
        }
        val newDailyList = dailyUsageMap.map { (date, usages) ->
            DailyUsage(
                date = date,
                appUsages = mapOf(
                    AppName.NAVER_WEBTOON to (usages[AppName.NAVER_WEBTOON] ?: 0),
                    AppName.INSTAGRAM to (usages[AppName.INSTAGRAM] ?: 0),
                    AppName.YOUTUBE to (usages[AppName.YOUTUBE] ?: 0)
                )
            )
        }.sortedBy { it.date }
        _dailyUsageList.postValue(newDailyList)

        // --- 3. 어제까지의 스트릭 계산 ---
        val streakMap = calculateStreaks(newDailyList, currentGoals)

        // --- 4. 최종 _appUsageList 생성 및 post (아이콘 포함) ---
        val newAppUsageList = AppName.values().map { appName ->
            val appIcon: Drawable? = try {
                packageManager.getApplicationIcon(appName.packageName)
            } catch (e: PackageManager.NameNotFoundException) { null }
            catch (e: Exception) { null }

            // ⭐️ --- [버그 수정 로직] --- ⭐️
            val todayUsage = todayUsageMap[appName] ?: 0
            val goal = currentGoals[appName] ?: 0
            val pastStreak = streakMap[appName] ?: 0 // 어제까지의 스트릭 (예: +1)

            var finalStreak = pastStreak // 일단 어제 스트릭을 기본값으로

            if (goal > 0 && todayUsage > goal) {
                // ⭐️ 오늘 목표가 있고, 오늘 사용량이 이미 목표를 넘었다면 (실패!)
                if (pastStreak < 0) {
                    // 어제도 실패했다면 (예: -1), 오늘 실패를 더해서 -2
                    finalStreak = pastStreak - 1
                } else {
                    // 어제는 성공했다면 (예: +1), 오늘 실패했으니 -1로 리셋
                    finalStreak = -1
                }
            }
            // ⭐️ --- [버그 수정 완료] --- ⭐️

            AppUsage(
                appName = appName,
                currentUsage = todayUsage,
                goalTime = goal,
                streak = finalStreak, // ⭐️ 수정된 finalStreak 사용
                icon = appIcon
            )
        }

        _appUsageList.postValue(newAppUsageList)
    }

    private fun parseUsageStats(stats: List<UsageStats>): Map<AppName, Int> {
        val usageMap = mutableMapOf<AppName, Int>()
        val appPackages = AppName.values().map { it.packageName }.toSet()

        for (stat in stats) {
            if (stat.packageName in appPackages) {
                val appName = AppName.values().find { it.packageName == stat.packageName }!!
                val usageInMinutes = (stat.totalTimeInForeground / (1000 * 60)).toInt()
                usageMap[appName] = (usageMap[appName] ?: 0) + usageInMinutes
            }
        }
        return usageMap
    }

    private fun calculateStreaks(
        dailyList: List<DailyUsage>,
        goals: Map<AppName, Int>
    ): Map<AppName, Int> {

        val streakMap = mutableMapOf<AppName, Int>()
        val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.KOREAN).format(Date())

        // ⭐️ [수정] 오늘을 제외한 과거 기록
        val pastDays = dailyList.filter { it.date != todayStr }.sortedByDescending { it.date }

        // ⭐️ [수정] 과거 기록이 없으면 0으로 반환
        if (pastDays.isEmpty()) {
            AppName.values().forEach { streakMap[it] = 0 }
            return streakMap
        }

        for (appName in AppName.values()) {
            val goal = goals[appName] ?: 0
            if (goal == 0) {
                streakMap[appName] = 0
                continue
            }

            // ⭐️ 어제(pastDays.first())의 성공/실패 여부
            val firstDayUsage = pastDays.first().appUsages[appName] ?: 0
            val wasSuccess = firstDayUsage <= goal
            var streak = 0

            // 어제부터 과거로 가면서 연속 기록 확인
            for (day in pastDays) {
                val usage = day.appUsages[appName] ?: 0
                if ((usage <= goal) == wasSuccess) {
                    streak++
                } else {
                    break // 연속 기록 깨짐
                }
            }

            streakMap[appName] = if (wasSuccess) streak else -streak
        }
        return streakMap
    }
}