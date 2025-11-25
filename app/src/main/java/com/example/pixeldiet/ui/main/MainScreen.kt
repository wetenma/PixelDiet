package com.example.pixeldiet.ui.main

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import com.example.pixeldiet.model.AppCategory // ⭐️ import
import com.example.pixeldiet.model.AppName
import com.example.pixeldiet.model.AppUsage
import com.example.pixeldiet.viewmodel.SharedViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun MainScreen(viewModel: SharedViewModel = viewModel()) {

    val appList by viewModel.appUsageList.observeAsState(emptyList())
    val totalUsage by viewModel.totalUsageData.observeAsState(Pair(0, 0))
    // ⭐️ [신규] 선택된 앱 목록
    val selectedApps by viewModel.selectedApps.observeAsState(emptyList())

    var showGoalDialog by remember { mutableStateOf(false) }
    var showAppSelectDialog by remember { mutableStateOf(false) } // ⭐️ [신규]

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            val dateFormat = SimpleDateFormat("yyyy년 M월 d일", Locale.KOREAN)
            Text(text = dateFormat.format(Date()), fontSize = 16.sp, color = Color.Gray)
        }

        // ⭐️ [수정] 버튼 2개 가로 배치
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { showGoalDialog = true },
                    modifier = Modifier.weight(1f)
                ) { Text("목표 설정") }

                Button(
                    onClick = { showAppSelectDialog = true },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                ) { Text("앱 선택") }
            }
        }

        item { VisualNotification(appList) }
        item { TotalProgress(totalUsage.first, totalUsage.second) }

        // ⭐️ [수정] icon이 null이 아닌 앱만 표시하도록 필터링
        items(appList.filter { it.icon != null }, key = { it.appName.name }) { app ->
            AppUsageCard(app)
        }
    }

    // ⭐️ [신규] 앱 선택 다이얼로그
    if (showAppSelectDialog) {
        AppSelectionDialog(
            currentSelected = selectedApps,
            onDismiss = { showAppSelectDialog = false },
            onSave = { newApps ->
                viewModel.updateSelectedApps(newApps)
                showAppSelectDialog = false
            }
        )
    }

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

// ⭐️ [신규] 앱 선택 다이얼로그 (카테고리 탭)
@Composable
fun AppSelectionDialog(
    currentSelected: List<AppName>,
    onDismiss: () -> Unit,
    onSave: (List<AppName>) -> Unit
) {
    val selectedState = remember { mutableStateListOf(*currentSelected.toTypedArray()) }
    var currentTab by remember { mutableStateOf(AppCategory.SNS) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("관리할 앱 선택 (최대 3개)") },
        text = {
            Column {
                TabRow(selectedTabIndex = currentTab.ordinal) {
                    AppCategory.values().forEach { category ->
                        Tab(
                            selected = currentTab == category,
                            onClick = { currentTab = category },
                            text = { Text(category.title, fontSize = 12.sp) }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                LazyColumn(modifier = Modifier.height(300.dp)) {
                    val appsInTab = AppName.values().filter { it.category == currentTab }
                    items(appsInTab) { app ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    if (selectedState.contains(app)) {
                                        selectedState.remove(app)
                                    } else {
                                        if (selectedState.size < 3) {
                                            selectedState.add(app)
                                        }
                                    }
                                }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = selectedState.contains(app),
                                onCheckedChange = { isChecked ->
                                    if (isChecked) {
                                        if (selectedState.size < 3) selectedState.add(app)
                                    } else {
                                        selectedState.remove(app)
                                    }
                                }
                            )
                            Text(app.displayName)
                        }
                    }
                }
                Text(
                    text = "선택됨: ${selectedState.size} / 3",
                    color = if (selectedState.size == 3) Color.Red else Color.Gray,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(selectedState.toList()) },
                enabled = selectedState.isNotEmpty()
            ) { Text("저장") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("취소") } }
    )
}

// ⭐️ [수정] GoalSettingDialog (appList만 사용하도록 확인)
@Composable
fun GoalSettingDialog(
    appList: List<AppUsage>,
    onDismiss: () -> Unit,
    onSave: (Map<AppName, Int>) -> Unit
) {
    val goalStates = remember {
        mutableStateMapOf<AppName, Pair<String, String>>().apply {
            appList.forEach { appUsage ->
                val currentMinutes = appUsage.goalTime
                val hours = (currentMinutes / 60).toString()
                val minutes = (currentMinutes % 60).toString()
                put(appUsage.appName, Pair(hours, minutes))
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("목표 시간 설정") },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(appList) { appUsage ->
                    Text(appUsage.appName.displayName, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        val (hours, minutes) = goalStates[appUsage.appName] ?: Pair("0", "0")
                        OutlinedTextField(
                            value = hours,
                            onValueChange = { goalStates[appUsage.appName] = Pair(it.filter { c -> c.isDigit() }, minutes) },
                            label = { Text("시간") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = minutes,
                            onValueChange = { goalStates[appUsage.appName] = Pair(hours, it.filter { c -> c.isDigit() }) },
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

// (나머지 함수들은 기존과 동일)
@Composable
fun VisualNotification(appList: List<AppUsage>) {
    val appsWithUsage = appList.filter { it.currentUsage > 0 }
    val maxUsage = appsWithUsage.maxOfOrNull { it.currentUsage }?.toFloat() ?: 1f
    Card(elevation = CardDefaults.cardElevation(2.dp)) {
        Row(modifier = Modifier.fillMaxWidth().height(200.dp).horizontalScroll(rememberScrollState()).padding(24.dp), verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            appsWithUsage.forEach { app ->
                val size = (40 + (app.currentUsage / maxUsage) * 100).dp
                Box(modifier = Modifier.size(size).background(app.appName.composeColor))
            }
        }
    }
}
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
private fun formatTime(minutes: Int): String {
    val hours = minutes / 60
    val mins = minutes % 60
    return String.format("%d시간 %02d분", hours, mins)
}