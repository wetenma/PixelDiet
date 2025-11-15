package com.example.pixeldiet.model

/**
 * 날짜별 각 앱 사용시간(분)을 모아둔 데이터
 */
data class DailyUsage(
    val date: String,                    // "yyyy-MM-dd" 형식의 날짜 문자열
    val appUsages: Map<AppName, Int>     // 각 AppName 별 사용 시간(분)
)
