package com.example.pixeldiet.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.map
import androidx.lifecycle.viewModelScope
import com.example.pixeldiet.model.*
import com.example.pixeldiet.repository.UsageRepository
import com.github.mikephil.charting.data.Entry
import com.prolificinteractive.materialcalendarview.CalendarDay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class SharedViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = UsageRepository

    val appUsageList: LiveData<List<AppUsage>> = repository.appUsageList
    private val dailyUsageList: LiveData<List<DailyUsage>> = repository.dailyUsageList
    val notificationSettings: LiveData<NotificationSettings> = repository.notificationSettings

    // ⭐️ [신규] 현재 선택된 앱 목록 (UI용)
    val selectedApps: LiveData<List<AppName>> = appUsageList.map { list ->
        list.map { it.appName }
    }

    private val _selectedFilter = MutableLiveData<AppName?>(null) // null = 전체

    val totalUsageData: LiveData<Pair<Int, Int>> = appUsageList.map { list ->
        val totalUsage = list.sumOf { it.currentUsage }
        val totalGoal = list.sumOf { it.goalTime }
        Pair(totalUsage, totalGoal)
    }

    val filteredGoalTime: LiveData<Int> = MediatorLiveData<Int>().apply {
        addSource(appUsageList) { goals ->
            val filter = _selectedFilter.value
            value = if (filter == null) goals.sumOf { it.goalTime }
            else goals.find { it.appName == filter }?.goalTime ?: 0
        }
        addSource(_selectedFilter) { filter ->
            val goals = appUsageList.value ?: return@addSource
            value = if (filter == null) goals.sumOf { it.goalTime }
            else goals.find { it.appName == filter }?.goalTime ?: 0
        }
    }

    // ... (calendarDecoratorData, calendarStatsText, streakText, chartData 등 로직은 기존과 동일) ...
    // (너무 길어서 생략하지만, 기존 코드 그대로 두셔도 됩니다. 로직이 Generic해서 자동으로 작동합니다.)
    // 단, setCalendarFilter만 아래처럼 수정해주세요.

    val calendarDecoratorData: LiveData<List<CalendarDecoratorData>> = MediatorLiveData<List<CalendarDecoratorData>>().apply {
        fun updateDecorators() {
            val goals = appUsageList.value ?: return
            val dailies = dailyUsageList.value ?: return
            val filter = _selectedFilter.value
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.KOREAN)
            val decorators = mutableListOf<CalendarDecoratorData>()
            for (daily in dailies) {
                val date = sdf.parse(daily.date) ?: continue
                val cal = Calendar.getInstance(); cal.time = date
                val calDay = CalendarDay.from(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1, cal.get(Calendar.DAY_OF_MONTH))
                val (usage, goal) = if (filter == null) {
                    Pair(daily.appUsages.values.sum(), goals.sumOf { it.goalTime })
                } else {
                    Pair(daily.appUsages[filter] ?: 0, goals.find { it.appName == filter }?.goalTime ?: 0)
                }
                if (goal == 0) continue
                val status = when {
                    usage > goal -> DayStatus.FAIL
                    usage > goal * 0.7 -> DayStatus.WARNING
                    else -> DayStatus.SUCCESS
                }
                decorators.add(CalendarDecoratorData(calDay, status))
            }
            value = decorators
        }
        addSource(dailyUsageList) { updateDecorators() }
        addSource(filteredGoalTime) { updateDecorators() }
        addSource(_selectedFilter) { updateDecorators() }
    }

    val calendarStatsText: LiveData<String> = MediatorLiveData<String>().apply {
        addSource(calendarDecoratorData) { decorators ->
            val currentMonth = Calendar.getInstance().get(Calendar.MONTH) + 1
            val successDays = decorators.count { it.date.month == currentMonth && (it.status == DayStatus.SUCCESS || it.status == DayStatus.WARNING) }
            val filterName = _selectedFilter.value?.displayName ?: "전체"
            value = "이번달 $filterName 목표 성공일: 총 ${successDays}일!"
        }
    }

    val streakText: LiveData<String> = MediatorLiveData<String>().apply {
        fun updateStreak() {
            val filter = _selectedFilter.value
            val appList = appUsageList.value ?: return
            val streak = if (filter == null) (appList.firstOrNull()?.streak ?: 0) else (appList.find { it.appName == filter }?.streak ?: 0)
            val days = Math.abs(streak)
            val status = if (streak >= 0) "달성" else "실패"
            value = "${days}일 연속 목표 $status 중!"
        }
        addSource(appUsageList) { updateStreak() }
        addSource(_selectedFilter) { updateStreak() }
    }

    val chartData: LiveData<List<Entry>> = MediatorLiveData<List<Entry>>().apply {
        fun updateChart() {
            val dailies = dailyUsageList.value ?: emptyList()
            val filter = _selectedFilter.value
            val currentMonth = Calendar.getInstance().get(Calendar.MONTH) + 1
            val entries = mutableListOf<Entry>()
            dailies.filter { it.date.substring(5, 7).toInt() == currentMonth }.forEach { daily ->
                val dayOfMonth = daily.date.substring(8, 10).toFloat()
                val usage = if (filter == null) daily.appUsages.values.sum() else daily.appUsages[filter] ?: 0
                entries.add(Entry(dayOfMonth, usage.toFloat()))
            }
            value = entries
        }
        addSource(dailyUsageList) { updateChart() }
        addSource(_selectedFilter) { updateChart() }
    }

    init {
        refreshData()
    }

    fun refreshData() {
        viewModelScope.launch(Dispatchers.IO) {
            repository.loadRealData(getApplication())
        }
    }

    // ⭐️ [신규] 앱 선택 업데이트 함수
    fun updateSelectedApps(newApps: List<AppName>) = viewModelScope.launch(Dispatchers.IO) {
        repository.updateSelectedApps(getApplication(), newApps)
        repository.loadRealData(getApplication()) // 데이터 새로고침
    }

    fun setGoalTimes(goals: Map<AppName, Int>) = viewModelScope.launch(Dispatchers.IO) {
        repository.updateGoalTimes(goals)
    }

    // ⭐️ [수정] 캘린더 필터 로직 (이름으로 찾기)
    fun setCalendarFilter(filterName: String) {
        if (filterName == "전체") {
            _selectedFilter.value = null
        } else {
            // 전체 앱 목록에서 이름 일치하는 것 찾기
            val foundApp = AppName.values().find { it.displayName == filterName }
            if (foundApp != null) {
                _selectedFilter.value = foundApp
            }
        }
    }

    fun saveNotificationSettings(settings: NotificationSettings) = viewModelScope.launch {
        repository.updateNotificationSettings(settings, getApplication())
    }
}