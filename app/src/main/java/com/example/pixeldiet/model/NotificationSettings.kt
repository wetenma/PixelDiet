package com.example.pixeldiet.model

data class NotificationSettings(
    // 개별 앱 알림
    var individualApp50: Boolean = false,
    var individualApp70: Boolean = false,
    var individualApp100: Boolean = false,

    // 전체 시간 알림
    var total50: Boolean = false,
    var total70: Boolean = false,
    var total100: Boolean = false
)
