package com.example.pixeldiet.ui.main

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.pixeldiet.model.AppName
import com.example.pixeldiet.model.AppUsage
import com.example.pixeldiet.viewmodel.SharedViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun MainScreen(viewModel: SharedViewModel = viewModel()) {

    val appList by viewModel.appUsageList.observeAsState(emptyList())
    val totalUsage by viewModel.totalUsageData.observeAsState(Pair(0, 0))
    var showGoalDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. 날짜
        item {
            val dateFormat = SimpleDateFormat("yyyy년 M월 d일", Locale.KOREAN)
            Text(text = dateFormat.format(Date()), fontSize = 16.sp, color = Color.Gray)
        }
        // 2. 목표 설정 버튼
        item {
            Button(
                onClick = { showGoalDialog = true },
                modifier = Modifier.fillMaxWidth()
            ) { Text("목표 시간 설정") }
        }
        // 3. 시각적 알림 (이 부분은 동일)
        item { VisualNotification(appList) }
        // 4. 전체 프로그레스바 (이 부분은 동일)
        item { TotalProgress(totalUsage.first, totalUsage.second) }
        // 5. 개별 앱 리스트
        items(appList, key = { it.appName.name }) { app ->
            // ⭐️ AppProgressItem -> AppUsageCard로 변경 (이전 수정 사항 반영)
            AppUsageCard(app)
        }
    }

    // 6. 목표 설정 다이얼로그 (showGoalDialog가 true일 때만 보임)
    if (showGoalDialog) {
        GoalSettingDialog(
            appList = appList,
            onDismiss = { showGoalDialog = false },
            onSave = { newGoals ->
                viewModel.setGoalTimes(newGoals)
                showGoalDialog = false
            }
        )
    }
}

// 3. 시각적 알림 (동일)
@Composable
fun VisualNotification(appList: List<AppUsage>) {
    val appsWithUsage = appList.filter { it.currentUsage > 0 }
    val maxUsage = appsWithUsage.maxOfOrNull { it.currentUsage }?.toFloat() ?: 1f

    Card(elevation = CardDefaults.cardElevation(2.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .horizontalScroll(rememberScrollState())
                .padding(24.dp),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            appsWithUsage.forEach { app ->
                val size = (40 + (app.currentUsage / maxUsage) * 100).dp
                Box(modifier = Modifier.size(size).background(app.appName.composeColor))
            }
        }
    }
}

// 4. 전체 프로그레스바
@Composable
fun TotalProgress(totalUsage: Int, totalGoal: Int) {
    Card(modifier = Modifier.fillMaxWidth(), elevation = CardDefaults.cardElevation(2.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("총 사용시간", fontSize = 14.sp, color = Color.Gray)
                Row {
                    // ⭐️ [수정] formatTime 함수 사용
                    Text(formatTime(totalUsage), fontSize = 14.sp, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(end = 16.dp))
                    Text("목표 ${formatTime(totalGoal)}", fontSize = 14.sp, color = MaterialTheme.colorScheme.error)
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            val progress = if (totalGoal > 0) (totalUsage.toFloat() / totalGoal).coerceAtMost(1f) else 0f
            LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth().height(12.dp))
        }
    }
}


// ⭐️ 6. [수정됨] 목표 설정 다이얼로그 Composable
@Composable
fun GoalSettingDialog(
    appList: List<AppUsage>,
    onDismiss: () -> Unit,
    onSave: (Map<AppName, Int>) -> Unit
) {
    // ⭐️ [수정] (시, 분) Pair를 String으로 저장
    val goalStates = remember {
        mutableStateMapOf<AppName, Pair<String, String>>().apply {
            AppName.values().forEach { appName ->
                val currentMinutes = appList.find { it.appName == appName }?.goalTime ?: 0
                val hours = (currentMinutes / 60).toString()
                val minutes = (currentMinutes % 60).toString()
                put(appName, Pair(hours, minutes))
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("목표 시간 설정") },
        text = {
            // ⭐️ [수정] 시/분 입력을 위해 LazyColumn 사용
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(AppName.values()) { appName ->
                    Text(appName.displayName, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp))

                    // ⭐️ [신규] "시"와 "분"을 가로로 배치
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        val (hours, minutes) = goalStates[appName] ?: Pair("0", "0")

                        // "시" 입력창
                        OutlinedTextField(
                            value = hours,
                            onValueChange = {
                                // 숫자만 입력받도록 필터링
                                goalStates[appName] = Pair(it.filter { char -> char.isDigit() }, minutes)
                            },
                            label = { Text("시간") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )
                        // "분" 입력창
                        OutlinedTextField(
                            value = minutes,
                            onValueChange = {
                                // 숫자만 입력받도록 필터링
                                goalStates[appName] = Pair(hours, it.filter { char -> char.isDigit() })
                            },
                            label = { Text("분") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                // ⭐️ [수정] (시 * 60) + 분 = 총 분으로 변환
                val newGoals = goalStates.mapValues {
                    val hours = it.value.first.toIntOrNull() ?: 0
                    val minutes = it.value.second.toIntOrNull() ?: 0
                    (hours * 60) + minutes
                }
                onSave(newGoals)
            }) { Text("저장") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("취소") } }
    )
}

// ⭐️ [수정됨] 시간 포맷 유틸 (H:MM:SS -> H시간 M분)
private fun formatTime(minutes: Int): String {
    val hours = minutes / 60
    val mins = minutes % 60
    return String.format("%d시간 %02d분", hours, mins)
}