package com.example.pixeldiet.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForwardIos // ⭐️ 1. import 추가
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState // ⭐️ 2. import 추가
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector // ⭐️ 3. import 추가
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.pixeldiet.model.NotificationSettings
import com.example.pixeldiet.viewmodel.SharedViewModel

@Composable
fun SettingsScreen(viewModel: SharedViewModel = viewModel()) {

    // 알림 예시 다이얼로그 상태
    var showIndividualExample by remember { mutableStateOf(false) } // ⭐️ 4. 예시 다이얼로그 상태 추가
    var showTotalExample by remember { mutableStateOf(false) }

    // 설정 다이얼로그 상태
    var showIndividualSettings by remember { mutableStateOf(false) }
    var showTotalSettings by remember { mutableStateOf(false) }

    val settings by viewModel.notificationSettings.observeAsState()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item { Text("알림 설정", fontSize = 20.sp, fontWeight = FontWeight.Bold) }
        item {
            Card(elevation = CardDefaults.cardElevation(2.dp)) {
                Column {
                    SettingsItem(
                        title = "개별 앱 시간 알람",
                        icon = Icons.Default.Notifications,
                        iconTint = Color(0xFFE1306C), // 인스타 색
                        onClick = { showIndividualSettings = true }
                    )
                    // ⭐️ 5. Divider -> HorizontalDivider로 변경
                    HorizontalDivider(Modifier.padding(horizontal = 16.dp))
                    SettingsItem(
                        title = "전체시간 알람",
                        icon = Icons.Default.Notifications,
                        iconTint = Color(0xFFFFC107), // 노랑
                        onClick = { showTotalSettings = true }
                    )
                }
            }
        }
    }

    // --- 다이얼로그 ---

    if (showIndividualSettings) {
        NotificationSettingDialog(
            title = "개별 앱 시간 알람",
            currentSettings = settings ?: NotificationSettings(),
            getCheckedItems = { s -> booleanArrayOf(s.individualApp50, s.individualApp70, s.individualApp100) },
            onDismiss = { showIndividualSettings = false },
            onSave = { newSettings -> viewModel.saveNotificationSettings(newSettings); showIndividualSettings = false },
            onShowExample = { showIndividualExample = true }, // ⭐️ 6. onShowExample 연결
            updateLogic = { current, index, isChecked ->
                when (index) {
                    0 -> current.individualApp50 = isChecked
                    1 -> current.individualApp70 = isChecked
                    2 -> current.individualApp100 = isChecked
                }
            }
        )
    }

    if (showTotalSettings) {
        NotificationSettingDialog(
            title = "전체 시간 알람",
            currentSettings = settings ?: NotificationSettings(),
            getCheckedItems = { s -> booleanArrayOf(s.total50, s.total70, s.total100) },
            onDismiss = { showTotalSettings = false },
            onSave = { newSettings -> viewModel.saveNotificationSettings(newSettings); showTotalSettings = false },
            onShowExample = { showTotalExample = true }, // ⭐️ 6. onShowExample 연결
            updateLogic = { current, index, isChecked ->
                when (index) {
                    0 -> current.total50 = isChecked
                    1 -> current.total70 = isChecked
                    2 -> current.total100 = isChecked
                }
            }
        )
    }

    // ⭐️ 7. 알림 예시 다이얼로그 구현
    if (showIndividualExample) {
        ExampleDialog(
            title = "개별 앱 알림 예시",
            message = """
                • 50%: "인스타그램의 목표사용시간을 50% 사용했어요!"
                • 70%: "인스타그램의 목표사용시간을 70% 사용했어요!"
                • 100% 초과: "인스타그램 멈춰!" (3분마다 반복)
            """.trimIndent(),
            onDismiss = { showIndividualExample = false }
        )
    }

    if (showTotalExample) {
        ExampleDialog(
            title = "전체 시간 알림 예시",
            message = """
                • 50%: "전체 목표사용시간을 50% 사용했어요!"
                • 70%: "전체 목표사용시간을 70% 사용했어요!"
                • 100% 초과: "전체 시간 초과!" (3분마다 반복)
            """.trimIndent(),
            onDismiss = { showTotalExample = false }
        )
    }
}

// 1. 설정 아이템
@Composable
fun SettingsItem(title: String, icon: ImageVector, iconTint: Color, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = title, tint = iconTint)
        Spacer(Modifier.width(16.dp))
        Text(title, fontSize = 16.sp, modifier = Modifier.weight(1f))
        Icon(Icons.Default.ArrowForwardIos, contentDescription = null, tint = Color.Gray)
    }
}

// 2. 알림 설정 다이얼로그
@Composable
fun NotificationSettingDialog(
    title: String,
    currentSettings: NotificationSettings,
    getCheckedItems: (NotificationSettings) -> BooleanArray,
    onDismiss: () -> Unit,
    onSave: (NotificationSettings) -> Unit,
    onShowExample: () -> Unit,
    updateLogic: (NotificationSettings, Int, Boolean) -> Unit
) {
    val items = listOf("50% 도달 알림", "70% 도달 알림", "100% 초과 반복 알림")
    val tempSettings = remember { mutableStateOf(currentSettings.copy()) }
    val checkedStates = remember { mutableStateListOf(*getCheckedItems(tempSettings.value).toTypedArray()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                items.forEachIndexed { index, text ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = checkedStates[index],
                            onCheckedChange = { isChecked ->
                                checkedStates[index] = isChecked
                                val newSettings = tempSettings.value.copy()
                                updateLogic(newSettings, index, isChecked)
                                tempSettings.value = newSettings
                            }
                        )
                        Text(text)
                    }
                }
                // ⭐️ 8. "알림 예시" 버튼을 다이얼로그 안에 추가
                Spacer(modifier = Modifier.height(8.dp))
                TextButton(onClick = onShowExample) {
                    Text("알림 예시 보기")
                }
            }
        },
        confirmButton = { Button(onClick = { onSave(tempSettings.value) }) { Text("저장") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("취소") } }
    )
}

// ⭐️ 9. 알림 예시 다이얼로그 Composable
@Composable
fun ExampleDialog(title: String, message: String, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(message) },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("확인")
            }
        }
    )
}