package com.example.pixeldiet.model

import com.prolificinteractive.materialcalendarview.CalendarDay

/**
 * 캘린더에 점(dot)을 찍기 위한 데이터
 *
 * @param date   캘린더에서 표시할 날짜
 * @param status 이 날짜의 상태 (성공/경고/실패)
 */
data class CalendarDecoratorData(
    val date: CalendarDay,
    val status: DayStatus
)
