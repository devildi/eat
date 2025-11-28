package com.example.eat.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ProfileContent(viewModel: MainViewModel) {
    val scrollState = rememberScrollState()
    val textMeasurer = rememberTextMeasurer()
    val hourHeight = 80.dp
    val totalHeight = hourHeight * 24 + 120.dp // 24小时 + 上下边距
    
    // 获取当前日期
    val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    val currentDate = dateFormat.format(Date())
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(totalHeight)
                .padding(horizontal = 16.dp)
        ) {
            val centerX = size.width / 2
            val startY = 50f
            val hourHeightPx = hourHeight.toPx()
            
            
            // 绘制主时间轴线
            drawLine(
                color = Color.Black,
                start = Offset(centerX, startY),
                end = Offset(centerX, startY + hourHeightPx * 24),
                strokeWidth = 4f
            )
            
            // 绘制每个小时的标记
            for (hour in 0..24) {
                val y = startY + hour * hourHeightPx
                
                // 绘制小时刻度线
                drawLine(
                    color = Color.Black,
                    start = Offset(centerX - 30f, y),
                    end = Offset(centerX + 30f, y),
                    strokeWidth = 3f
                )
                
                // 绘制小圆点
                drawCircle(
                    color = Color.Black,
                    radius = 8f,
                    center = Offset(centerX, y)
                )
                
                // 绘制时间文本
                val timeText = String.format("%02d:00", hour)
                val textLayoutResult = textMeasurer.measure(
                    text = timeText,
                    style = TextStyle(
                        fontSize = 16.sp,
                        color = Color.Black
                    )
                )
                
                drawText(
                    textLayoutResult = textLayoutResult,
                    topLeft = Offset(
                        centerX + 50f,
                        y - textLayoutResult.size.height / 2
                    )
                )
            }


            // Draw Events
            val events = viewModel.events
            for (event in events) {
                val date = Date(event.timestamp)
                val hour = SimpleDateFormat("H", Locale.getDefault()).format(date).toInt()
                val minute = SimpleDateFormat("m", Locale.getDefault()).format(date).toInt()
                
                // Calculate Y position: startY + (hour + minute/60) * hourHeight
                val eventY = startY + (hour + minute / 60f) * hourHeightPx
                
                // Determine X position based on event type
                val isMainMeal = event.type == "Main Meal"
                val markerX = if (isMainMeal) centerX - 100f else centerX + 100f
                // Adjusted textX to avoid overlap with 30f radius circle (100 +/- 30 = 70/130 range)
                // Using 160f gives 30f padding from the circle edge (130 -> 160)
                val textX = if (isMainMeal) centerX - 160f else centerX + 160f
                
                // Draw connecting line
                drawLine(
                    color = Color.Gray,
                    start = Offset(markerX, eventY),
                    end = Offset(centerX, eventY),
                    strokeWidth = 2f
                )

                // Draw marker
                drawCircle(
                    color = Color.Red,
                    radius = 30f,
                    center = Offset(markerX, eventY)
                )
                
                // Draw text
                val eventTextLayoutResult = textMeasurer.measure(
                    text = "${if (isMainMeal) "正餐" else "零食"} ${SimpleDateFormat("HH:mm", Locale.getDefault()).format(date)}",
                    style = TextStyle(
                        fontSize = 14.sp,
                        color = Color.Gray
                    )
                )
                
                drawText(
                    textLayoutResult = eventTextLayoutResult,
                    topLeft = Offset(
                        if (isMainMeal) textX - eventTextLayoutResult.size.width else textX,
                        eventY - eventTextLayoutResult.size.height / 2
                    )
                )
            }
        }
    }
}
