package com.example.pixeldiet.ui.common

import android.graphics.Color
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.example.pixeldiet.model.CalendarDecoratorData
import com.example.pixeldiet.model.DayStatus
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.components.LimitLine // ⭐️ import
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.formatter.ValueFormatter // ⭐️ import
import com.prolificinteractive.materialcalendarview.CalendarDay
import com.prolificinteractive.materialcalendarview.DayViewDecorator
import com.prolificinteractive.materialcalendarview.DayViewFacade
import com.prolificinteractive.materialcalendarview.MaterialCalendarView
import com.prolificinteractive.materialcalendarview.spans.DotSpan

// ----------------------
// MaterialCalendarView 래퍼
// ----------------------
@Composable
fun WrappedMaterialCalendar(
    modifier: Modifier = Modifier,
    decoratorData: List<CalendarDecoratorData>
) {
    AndroidView(
        modifier = modifier,
        factory = { context ->
            MaterialCalendarView(context).apply {
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
                topbarVisible = true
                selectionMode = MaterialCalendarView.SELECTION_MODE_NONE
                setCurrentDate(CalendarDay.today())
            }
        },
        update = { view ->
            view.removeDecorators()
            val successDays = decoratorData.filter { it.status == DayStatus.SUCCESS }.map { it.date }.toSet()
            val warningDays = decoratorData.filter { it.status == DayStatus.WARNING }.map { it.date }.toSet()
            val failDays = decoratorData.filter { it.status == DayStatus.FAIL }.map { it.date }.toSet()

            if (successDays.isNotEmpty()) view.addDecorator(StatusDecorator(successDays, Color.BLUE))
            if (warningDays.isNotEmpty()) view.addDecorator(StatusDecorator(warningDays, Color.parseColor("#FFC107")))
            if (failDays.isNotEmpty()) view.addDecorator(StatusDecorator(failDays, Color.RED))
        }
    )
}

// ----------------------
// BarChart 래퍼
// ----------------------
@Composable
fun WrappedBarChart(
    modifier: Modifier = Modifier,
    chartData: List<Entry>,
    goalTime: Int // ⭐️ [신규] 목표 시간을 파라미터로 받음
) {
    AndroidView(
        modifier = modifier,
        factory = { context ->
            BarChart(context).apply {
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                xAxis.apply {
                    position = XAxis.XAxisPosition.BOTTOM
                    setDrawGridLines(false) // 세로선 제거 (깔끔하게)
                    granularity = 1f
                    valueFormatter = object : ValueFormatter() {
                        override fun getFormattedValue(value: Float): String {
                            return value.toInt().toString() // 날짜는 정수로
                        }
                    }
                }

                // ⭐️ Y축 (왼쪽) 설정: 시간 단위(h)로 변경
                axisLeft.apply {
                    setDrawGridLines(true)
                    axisMinimum = 0f
                    valueFormatter = object : ValueFormatter() {
                        override fun getFormattedValue(value: Float): String {
                            val hours = value / 60f
                            return String.format("%.1fh", hours) // 예: 1.5h
                        }
                    }
                }

                axisRight.isEnabled = false
                description.isEnabled = false
                setDrawValueAboveBar(false)
                legend.isEnabled = false // 범례 숨김
            }
        },
        update = { view ->
            // ⭐️ 1. 목표선 (Red Line) 그리기
            val leftAxis = view.axisLeft
            leftAxis.removeAllLimitLines()

            if (goalTime > 0) {
                val limitLine = LimitLine(goalTime.toFloat(), "목표").apply {
                    lineWidth = 2f
                    lineColor = Color.RED
                    textColor = Color.RED
                    textSize = 12f
                    enableDashedLine(10f, 10f, 0f) // 점선 효과
                    labelPosition = LimitLine.LimitLabelPosition.RIGHT_TOP
                }
                leftAxis.addLimitLine(limitLine)
                // 목표선이 그래프 밖으로 나가지 않게 최대값 조정
                if (leftAxis.axisMaximum < goalTime) {
                    leftAxis.axisMaximum = goalTime * 1.2f
                }
            }

            // ⭐️ 2. 데이터 업데이트
            if (chartData.isEmpty()) {
                view.clear()
                view.invalidate()
                return@AndroidView
            }

            val barEntries = chartData.map { BarEntry(it.x, it.y) }
            val dataSet = BarDataSet(barEntries, "사용시간").apply {
                color = Color.parseColor("#2196F3") // 파란색
                setDrawValues(false)
            }
            view.data = BarData(dataSet)
            view.invalidate()
        }
    )
}

// ----------------------
// 캘린더 데코레이터
// ----------------------
private class StatusDecorator(
    private val dates: Set<CalendarDay>,
    private val color: Int
) : DayViewDecorator {
    override fun shouldDecorate(day: CalendarDay): Boolean = dates.contains(day)
    override fun decorate(view: DayViewFacade) {
        view.addSpan(DotSpan(10f, color))
    }
}