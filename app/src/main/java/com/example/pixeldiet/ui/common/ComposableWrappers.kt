package com.example.pixeldiet.ui.common

import android.graphics.Color
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.example.pixeldiet.model.CalendarDecoratorData
import com.example.pixeldiet.model.DayStatus
// ⭐️ 1. import 변경: LineChart -> BarChart
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.components.XAxis
// ⭐️ 2. import 변경: LineData/Set -> BarData/Set/Entry
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.data.Entry
import com.prolificinteractive.materialcalendarview.CalendarDay
import com.prolificinteractive.materialcalendarview.DayViewDecorator
import com.prolificinteractive.materialcalendarview.DayViewFacade
import com.prolificinteractive.materialcalendarview.MaterialCalendarView
import com.prolificinteractive.materialcalendarview.spans.DotSpan

// ----------------------
// MaterialCalendarView 래퍼 (이 부분은 동일)
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
                currentDate = CalendarDay.today()
            }
        },
        update = { view ->
            view.removeDecorators()
            val successDays = decoratorData.filter { it.status == DayStatus.SUCCESS }.map { it.date }.toSet()
            val warningDays = decoratorData.filter { it.status == DayStatus.WARNING }.map { it.date }.toSet()
            val failDays = decoratorData.filter { it.status == DayStatus.FAIL }.map { it.date }.toSet()

            if (successDays.isNotEmpty()) {
                view.addDecorator(StatusDecorator(dates = successDays, color = Color.BLUE))
            }
            if (warningDays.isNotEmpty()) {
                view.addDecorator(StatusDecorator(dates = warningDays, color = Color.parseColor("#FFC107")))
            }
            if (failDays.isNotEmpty()) {
                view.addDecorator(StatusDecorator(dates = failDays, color = Color.RED))
            }
        }
    )
}

// ----------------------
// ⭐️ 3. [수정됨] MPAndroidChart 래퍼 (BarChart)
// ----------------------
@Composable
fun WrappedBarChart( // ⭐️ 4. 이름 변경
    modifier: Modifier = Modifier,
    chartData: List<Entry> // ⭐️ 5. BarEntry는 Entry의 자식이므로 이 부분은 동일
) {
    AndroidView(
        modifier = modifier,
        factory = { context ->
            // ⭐️ 6. LineChart -> BarChart
            BarChart(context).apply {
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                xAxis.apply {
                    position = XAxis.XAxisPosition.BOTTOM
                    setDrawGridLines(true)
                    granularity = 1f
                }
                axisRight.isEnabled = false
                description.isEnabled = false
                setDrawValueAboveBar(false) // 막대 위에 값 표시 안 함
            }
        },
        update = { view -> // ⭐️ view는 이제 BarChart 타입
            if (chartData.isEmpty()) {
                view.clear()
                view.invalidate()
                return@AndroidView
            }

            // ⭐️ 7. BarEntry로 변환 (Entry 리스트를 BarEntry 리스트로)
            val barEntries = chartData.map { BarEntry(it.x, it.y) }

            // ⭐️ 8. LineDataSet -> BarDataSet
            val dataSet = BarDataSet(barEntries, "사용시간(분)").apply {
                color = Color.BLUE // 막대 색상
                setDrawValues(false) // 막대 위에 값 표시 안 함
            }

            // ⭐️ 9. LineData -> BarData
            view.data = BarData(dataSet)
            view.invalidate()
        }
    )
}

// ----------------------
// 캘린더 데코레이터 (이 부분은 동일)
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