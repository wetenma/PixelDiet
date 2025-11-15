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
                    // ⭐️ [수정] 배경색 Box를 아이콘 뒤에 두어, 아이콘이 투명한 부분에 나타날 수 있도록 변경
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(appUsage.appName.composeColor, RoundedCornerShape(8.dp)) // 아이콘이 없는 경우를 위한 배경
                    ) {
                        AsyncImage(
                            model = appUsage.icon,
                            contentDescription = appUsage.appName.displayName,
                            modifier = Modifier
                                .fillMaxSize() // Box 크기에 맞게 채우기
                                .padding(2.dp) // 약간의 패딩으로 둥근 테두리 살리기
                        )
                    }
                } else {
                    // 아이콘이 없을 경우 (에러 또는 앱 미설치)
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(appUsage.appName.composeColor, RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("?", color = Color.White, fontSize = 24.sp) // 임시로 "?" 표시
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
                Text(text = "${appUsage.currentUsage}분", fontSize = 14.sp)
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
                Text(text = "${appUsage.goalTime}분", fontSize = 14.sp)
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun AppUsageCardPreview() {
    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        AppUsageCard(AppUsage(AppName.NAVER_WEBTOON, 120, 180, 5, null))
        AppUsageCard(AppUsage(AppName.INSTAGRAM, 90, 60, -3, null))
        AppUsageCard(AppUsage(AppName.YOUTUBE, 30, 0, 0, null))
    }
}