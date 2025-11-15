package com.example.pixeldiet.model

import android.graphics.Color
import android.graphics.drawable.Drawable
import androidx.compose.ui.graphics.Color as ComposeColor // ⭐️ Compose용 Color import
import com.prolificinteractive.materialcalendarview.CalendarDay

// 앱 이름 Enum (패키지 이름, 표시 이름, 색상 관리)
enum class AppName(
    val packageName: String,
    val displayName: String,
    // val color: Int, // ⭐️ Android View용 (더 이상 사용 안 할 예정이지만 일단 놔둠)
    val composeColor: ComposeColor // ⭐️ Compose용 (이걸 사용합니다)
) {
    NAVER_WEBTOON(
        "com.nhn.android.webtoon", "네이버 웹툰",
        // Color.parseColor("#00D564"), // ⭐️ 삭제 또는 주석 처리 가능
        ComposeColor(0xFF00D564)
    ),
    INSTAGRAM(
        "com.instagram.android", "인스타그램",
        // Color.parseColor("#E1306C"),
        ComposeColor(0xFFE1306C)
    ),
    YOUTUBE(
        "com.google.android.youtube", "유튜브",
        // Color.parseColor("#FF0000"),
        ComposeColor(0xFFFF0000)
    )
}

// ... (AppUsage, DailyUsage 등은 그대로) ...