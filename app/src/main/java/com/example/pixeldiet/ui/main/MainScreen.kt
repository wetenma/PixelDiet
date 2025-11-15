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
        // 3. 시각적 알림
        item { VisualNotification(appList) }
        // 4. 전체 프로그레스바
        item { TotalProgress(totalUsage.first, totalUsage.second) }
        // 5. 개별 앱 리스트
        items(appList, key = { it.appName.name }) { app ->
            AppProgressItem(app)
        }
    }

    // 6. 목표 설정 다이얼로그
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

// 3. 시각적 알림
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

// 5. 개별 앱 아이템
@Composable
fun AppProgressItem(app: AppUsage) {
    Card(modifier = Modifier.fillMaxWidth(), elevation = CardDefaults.cardElevation(2.dp)) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.width(60.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Box(Modifier.size(40.dp).background(app.appName.composeColor))
                Text(formatTime(app.goalTime), fontSize = 12.sp, color = Color.Gray, modifier = Modifier.padding(top = 4.dp))
            }
            Spacer(Modifier.width(12.dp))
            val progress = if (app.goalTime > 0) (app.currentUsage.toFloat() / app.goalTime).coerceAtMost(1f) else 0f
            LinearProgressIndicator(progress = { progress }, modifier = Modifier.weight(1f).height(12.dp))
            Spacer(Modifier.width(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                val icon = if (app.streak >= 0) "🔥" else "💀"
                Text(icon, fontSize = 20.sp, modifier = Modifier.padding(end = 4.dp))
                Text(Math.abs(app.streak).toString(), fontSize = 16.sp)
            }
        }
    }
}

// 6. 목표 설정 다이얼로그
@Composable
fun GoalSettingDialog(
    appList: List<AppUsage>,
    onDismiss: () -> Unit,
    onSave: (Map<AppName, Int>) -> Unit
) {
    val goalStates = remember {
        mutableStateMapOf<AppName, String>().apply {
            AppName.values().forEach { appName ->
                val hours = (appList.find { it.appName == appName }?.goalTime ?: 0) / 60
                put(appName, hours.toString())
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("목표 시간 설정") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                AppName.values().forEach { appName ->
                    Text(appName.displayName, fontWeight = FontWeight.Bold)
                    OutlinedTextField(
                        value = goalStates[appName] ?: "0",
                        onValueChange = { goalStates[appName] = it.filter { char -> char.isDigit() } },
                        label = { Text("시간 단위로 입력") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                val newGoals = goalStates.mapValues { (it.value.toIntOrNull() ?: 0) * 60 }
                onSave(newGoals)
            }) { Text("저장") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("취소") } }
    )
}

// 시간 포맷 유틸
private fun formatTime(minutes: Int): String {
    val hours = minutes / 60
    val mins = minutes % 60
    return String.format("%d:%02d:00", hours, mins)
}