package com.example.pixeldiet.model

import android.graphics.drawable.Drawable

data class AppUsage(
    val appName: AppName,        // 어떤 앱인지 (INSTAGRAM, YOUTUBE 등)
    val currentUsage: Int,       // 오늘 사용 시간(분)
    val goalTime: Int,           // 목표 시간(분)
    val streak: Int,             // 연속 성공/실패 일수 (성공: 양수, 실패: 음수)
    val icon: Drawable? = null   // 앱 아이콘 (UI에서 안 써도 있어도 됨)
)
