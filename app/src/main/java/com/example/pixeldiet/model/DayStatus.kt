package com.example.pixeldiet.model

/**
 * 하루 사용량 상태
 * SUCCESS : 목표 이내 사용
 * WARNING : 목표를 어느 정도 넘김
 * FAIL    : 크게 초과
 */
enum class DayStatus {
    SUCCESS,
    WARNING,
    FAIL
}
