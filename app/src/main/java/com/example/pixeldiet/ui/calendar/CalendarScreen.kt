package com.example.pixeldiet.ui.calendar

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.pixeldiet.model.AppName
import com.example.pixeldiet.ui.common.WrappedBarChart
import com.example.pixeldiet.ui.common.WrappedMaterialCalendar
import com.example.pixeldiet.viewmodel.SharedViewModel

@Composable
fun CalendarScreen(viewModel: SharedViewModel = viewModel()) {

    val decoratorData by viewModel.calendarDecoratorData.observeAsState(emptyList())
    val statsText by viewModel.calendarStatsText.observeAsState("")
    val streakText by viewModel.streakText.observeAsState("")
    val chartData by viewModel.chartData.observeAsState(emptyList())
    val goalTime by viewModel.filteredGoalTime.observeAsState(0)

    // ⭐️ [신규] 선택된 앱 목록 가져오기
    val selectedApps by viewModel.selectedApps.observeAsState(emptyList())

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. 스피너 (선택된 앱 목록 전달)
        item {
            FilterSpinner(
                selectedApps = selectedApps, // ⭐️ 전달
                onFilterSelected = { viewModel.setCalendarFilter(it) }
            )
        }
        // ... (나머지 UI 동일) ...
        item {
            Card(elevation = CardDefaults.cardElevation(2.dp)) {
                WrappedMaterialCalendar(Modifier.fillMaxWidth(), decoratorData)
            }
        }
        item {
            Text(statsText, Modifier.fillMaxWidth(), textAlign = TextAlign.Center, fontSize = 16.sp, color = MaterialTheme.colorScheme.secondary)
            Text(streakText, Modifier.fillMaxWidth(), textAlign = TextAlign.Center, fontSize = 16.sp, color = MaterialTheme.colorScheme.primary)
        }
        item {
            Card(Modifier.fillMaxWidth().height(300.dp), elevation = CardDefaults.cardElevation(2.dp)) {
                Column(Modifier.padding(16.dp)) {
                    Text("이번 달 사용 시간", Modifier.fillMaxWidth(), textAlign = TextAlign.Center, fontSize = 16.sp)
                    Spacer(Modifier.height(16.dp))
                    WrappedBarChart(Modifier.fillMaxSize(), chartData, goalTime)
                }
            }
        }
    }
}

// ⭐️ [수정] 동적 옵션 생성
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilterSpinner(
    selectedApps: List<AppName>, // ⭐️ 파라미터 추가
    onFilterSelected: (String) -> Unit
) {
    // ⭐️ "전체" + 선택된 앱들의 표시 이름(displayName)
    val options = listOf("전체") + selectedApps.map { it.displayName }

    var expanded by remember { mutableStateOf(false) }
    var selectedText by remember { mutableStateOf(options[0]) }

    // 옵션이 바뀌면 선택된 텍스트가 유효한지 확인 (앱 변경 시 대응)
    LaunchedEffect(options) {
        if (selectedText !in options) {
            selectedText = options[0]
            onFilterSelected(selectedText)
        }
    }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded }
    ) {
        OutlinedTextField(
            value = selectedText,
            onValueChange = {},
            readOnly = true,
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.fillMaxWidth().menuAnchor()
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option) },
                    onClick = {
                        selectedText = option
                        expanded = false
                        onFilterSelected(option)
                    }
                )
            }
        }
    }
}