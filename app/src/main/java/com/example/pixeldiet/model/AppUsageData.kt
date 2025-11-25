package com.example.pixeldiet.model

import android.graphics.Color
import android.graphics.drawable.Drawable
import androidx.compose.ui.graphics.Color as ComposeColor
import com.prolificinteractive.materialcalendarview.CalendarDay

// 앱 카테고리
enum class AppCategory(val title: String) {
    SNS("SNS"),
    GAME("게임"),
    WEBTOON("웹툰")
}

// ⭐️ [수정] YOUTUBE를 SNS 목록에 재추가했습니다.
enum class AppName(
    val packageName: String,
    val displayName: String,
    val category: AppCategory,
    val composeColor: ComposeColor
) {
    // --- SNS ---
    INSTAGRAM("com.instagram.android", "Instagram", AppCategory.SNS, ComposeColor(0xFFE1306C)),
    YOUTUBE("com.google.android.youtube", "유튜브", AppCategory.SNS, ComposeColor(0xFFFF0000)),
    TIKTOK("com.zhiliaoapp.musically", "TikTok", AppCategory.SNS, ComposeColor(0xFF000000)),
    SNAPCHAT("com.snapchat.android", "Snapchat", AppCategory.SNS, ComposeColor(0xFFFFFC00)),
    TWITTER("com.twitter.android", "Twitter(X)", AppCategory.SNS, ComposeColor(0xFF1DA1F2)),
    DISCORD("com.discord", "Discord", AppCategory.SNS, ComposeColor(0xFF5865F2)),
    THREADS("com.instagram.barcelona", "Threads", AppCategory.SNS, ComposeColor(0xFF000000)),
    BEREAL("com.bereal.ft", "BeReal", AppCategory.SNS, ComposeColor(0xFF000000)),
    PINTEREST("com.pinterest", "Pinterest", AppCategory.SNS, ComposeColor(0xFFBD081C)),

    // --- GAME ---
    BRAWL_STARS("com.supercell.brawlstars", "브롤스타즈", AppCategory.GAME, ComposeColor(0xFFFFC107)),
    ROBLOX("com.roblox.client", "로블록스", AppCategory.GAME, ComposeColor(0xFFDE350C)),
    WILD_RIFT("com.riotgames.league.wildrift", "와일드 리프트", AppCategory.GAME, ComposeColor(0xFF0091EA)),
    PUBG_MOBILE("com.pubg.krmobile", "배그 모바일", AppCategory.GAME, ComposeColor(0xFFFF9800)),
    COOKIE_RUN("com.devsisters.ck", "쿠키런: 킹덤", AppCategory.GAME, ComposeColor(0xFFE88A52)),
    GENSHIN("com.miHoYo.GenshinImpact", "원신", AppCategory.GAME, ComposeColor(0xFF5C6BC0)),
    SUDDEN_ATTACK("com.nexon.sudden", "서든어택M", AppCategory.GAME, ComposeColor(0xFF4CAF50)),
    KARTRIDER("com.nexon.kartdrift", "카트라이더", AppCategory.GAME, ComposeColor(0xFFF44336)),

    // --- WEBTOON ---
    NAVER_WEBTOON("com.nhn.android.webtoon", "네이버 웹툰", AppCategory.WEBTOON, ComposeColor(0xFF00D564)),
    KAKAO_WEBTOON("com.kakao.page", "카카오웹툰", AppCategory.WEBTOON, ComposeColor(0xFFFFEB3B)),
    KAKAO_PAGE("com.kakao.page", "카카오페이지", AppCategory.WEBTOON, ComposeColor(0xFFFFC107)),
    LEZHIN("com.lezhin.comics", "레진코믹스", AppCategory.WEBTOON, ComposeColor(0xFFEC1C24)),
    RIDI("com.initialcoms.ridi", "리디북스", AppCategory.WEBTOON, ComposeColor(0xFF1E88E5)),
    BOMTOON("com.bomcomics.bomtoon", "봄툰", AppCategory.WEBTOON, ComposeColor(0xFFE91E63)),
    TOOMICS("com.toomics.global", "투믹스", AppCategory.WEBTOON, ComposeColor(0xFFF44336)),
    TOPTOON("com.toptoon.toptoon", "탑툰", AppCategory.WEBTOON, ComposeColor(0xFFD32F2F));
}

data class AppUsage(
    val appName: AppName,
    var currentUsage: Int = 0,
    var goalTime: Int = 0,
    var streak: Int = 0,
    val icon: Drawable? = null
)

data class DailyUsage(
    val date: String,
    val appUsages: Map<AppName, Int>
)

data class NotificationSettings(
    var individualApp50: Boolean = true,
    var individualApp70: Boolean = true,
    var individualApp100: Boolean = true,
    var total50: Boolean = true,
    var total70: Boolean = true,
    var total100: Boolean = true,
    var repeatIntervalMinutes: Int = 5
)

data class CalendarDecoratorData(
    val date: CalendarDay,
    val status: DayStatus
)

enum class DayStatus {
    SUCCESS, WARNING, FAIL
}