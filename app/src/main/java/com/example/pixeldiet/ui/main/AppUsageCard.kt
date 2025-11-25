package com.example.pixeldiet.ui.main

import android.graphics.drawable.Drawable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BrokenImage
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.pixeldiet.model.AppUsage
import com.example.pixeldiet.model.AppName

@Composable
fun AppUsageCard(appUsage: AppUsage) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 아이콘 표시 (AsyncImage)
                if (appUsage.icon != null) {
                    AsyncImage(
                        model = appUsage.icon,
                        contentDescription = appUsage.appName.displayName,
                        modifier = Modifier.size(40.dp)
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(appUsage.appName.composeColor, RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("?", color = Color.White, fontSize = 24.sp)
                    }
                }
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = appUsage.appName.displayName,
                    fontSize = 18.sp,
                    modifier = Modifier.weight(1f)
                )

                // 스트릭 (불꽃 아이콘)
                if (appUsage.streak != 0) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (appUsage.streak > 0) Icons.Filled.LocalFireDepartment else Icons.Filled.BrokenImage,
                            contentDescription = "Streak",
                            tint = if (appUsage.streak > 0) Color(0xFFFF4500) else Color.Gray,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(text = "${Math.abs(appUsage.streak)}", fontSize = 16.sp)
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))

            // 사용 시간 / 목표 시간 (ProgressBar)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = formatTime(appUsage.currentUsage), fontSize = 14.sp)
                Spacer(modifier = Modifier.width(8.dp))
                LinearProgressIndicator(
                    progress = { if (appUsage.goalTime > 0) appUsage.currentUsage.toFloat() / appUsage.goalTime.toFloat() else 0f },
                    modifier = Modifier
                        .weight(1f)
                        .height(8.dp),
                    color = appUsage.appName.composeColor,
                    trackColor = Color.LightGray
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = formatTime(appUsage.goalTime), fontSize = 14.sp)
            }
        }
    }
}

// 시간 포맷 유틸
private fun formatTime(minutes: Int): String {
    val hours = minutes / 60
    val mins = minutes % 60
    return String.format("%d시간 %02d분", hours, mins)
}

// Preview용 더미 데이터
@Preview(showBackground = true)
@Composable
fun AppUsageCardPreview() {
    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        AppUsageCard(AppUsage(AppName.NAVER_WEBTOON, 120, 180, 5, null))
        AppUsageCard(AppUsage(AppName.INSTAGRAM, 90, 60, -3, null))
        // ⭐️ [수정] YOUTUBE로 복구
        AppUsageCard(AppUsage(AppName.YOUTUBE, 30, 0, 0, null))
    }
}