package com.example.eat.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.runtime.collectAsState
import com.example.eat.ui.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeightChartScreen(
    onBack: () -> Unit,
    viewModel: MainViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
) {
    var currentDate by remember { mutableStateOf(Calendar.getInstance()) }
    val currentMonthStr = SimpleDateFormat("M", Locale.getDefault()).format(currentDate.time)
    val textMeasurer = rememberTextMeasurer()
    val context = androidx.compose.ui.platform.LocalContext.current
    
    val weightData by viewModel.weightData.collectAsState()
    
    // Filter data for current month and map to (day, weight)
    // Use the latest entry for each day if multiple exist
    val sampleData = remember(weightData, currentDate) {
        val currentYear = currentDate.get(Calendar.YEAR)
        val currentMonth = currentDate.get(Calendar.MONTH)
        
        weightData.filter { entity ->
            val calendar = Calendar.getInstance()
            calendar.timeInMillis = entity.timestamp
            calendar.get(Calendar.YEAR) == currentYear && calendar.get(Calendar.MONTH) == currentMonth
        }.groupBy { entity ->
            val calendar = Calendar.getInstance()
            calendar.timeInMillis = entity.timestamp
            calendar.get(Calendar.DAY_OF_MONTH)
        }.map { (day, entities) ->
            // Take the latest entry for the day
            val latestEntity = entities.maxByOrNull { it.timestamp }!!
            // Convert kg to jin (1 kg = 2 jin)
            day to (latestEntity.value1 * 2f)
        }.sortedBy { it.first }
    }
    
    var selectedPointIndex by remember { mutableStateOf<Int?>(null) }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("${currentMonthStr}月体重走势") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },

                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White,
                    titleContentColor = Color.Black
                )
            )
        }
    ) { paddingValues ->
        var dragOffset by remember { mutableStateOf(0f) }
        var isProcessingSwipe by remember { mutableStateOf(false) }
        
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = {
                            dragOffset = 0f
                        },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            dragOffset += dragAmount.y
                        },
                        onDragEnd = {
                            if (!isProcessingSwipe && kotlin.math.abs(dragOffset) > 100f) {
                                isProcessingSwipe = true
                                
                                if (dragOffset < 0) {
                                    // Swipe up - next month
                                    val nextMonth = currentDate.clone() as Calendar
                                    nextMonth.add(Calendar.MONTH, 1)
                                    
                                    // Check if next month is in the future
                                    val now = Calendar.getInstance()
                                    if (nextMonth.get(Calendar.YEAR) < now.get(Calendar.YEAR) ||
                                        (nextMonth.get(Calendar.YEAR) == now.get(Calendar.YEAR) &&
                                         nextMonth.get(Calendar.MONTH) <= now.get(Calendar.MONTH))) {
                                        currentDate = nextMonth
                                    } else {
                                        android.widget.Toast.makeText(context, "不能查看未来月份", android.widget.Toast.LENGTH_SHORT).show()
                                    }
                                } else {
                                    // Swipe down - previous month
                                    val prevMonth = currentDate.clone() as Calendar
                                    prevMonth.add(Calendar.MONTH, -1)
                                    currentDate = prevMonth
                                }
                                
                                isProcessingSwipe = false
                            }
                            dragOffset = 0f
                        }
                    )
                }
        ) {
            if (sampleData.isEmpty()) {
                androidx.compose.foundation.layout.Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = androidx.compose.ui.Alignment.Center
                ) {
                    Text(
                        text = "无数据",
                        style = TextStyle(fontSize = 20.sp, color = Color.Gray)
                    )
                }
            } else {
                var totalDragY = 0f
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f) // Fill available height
                        .padding(16.dp)
                        .pointerInput(Unit) {
                            detectTapGestures { tapOffset ->
                                val chartWidth = size.width
                                val chartHeight = size.height
                                
                                // Chart parameters (Must match drawing logic, weight in jin, range 140-150)
                                val minWeight = 140f
                                val maxWeight = 150f
                                val weightRange = maxWeight - minWeight
                                
                                val maxDay = currentDate.getActualMaximum(Calendar.DAY_OF_MONTH)
                                
                                val leftMargin = 80f
                                val bottomMargin = 60f
                                val topMargin = 40f
                                val rightMargin = 40f
                                
                                val plotWidth = chartWidth - leftMargin - rightMargin
                                val plotHeight = chartHeight - topMargin - bottomMargin
                                
                                // Check each point
                                var foundIndex: Int? = null
                                sampleData.forEachIndexed { index, (day, weight) ->
                                    val clampedWeight = weight.coerceIn(minWeight, maxWeight)
                                    val x = leftMargin + ((clampedWeight - minWeight) / weightRange) * plotWidth
                                    val y = topMargin + ((day - 1).toFloat() / (maxDay - 1)) * plotHeight
                                    
                                    val distance = Math.sqrt(
                                        Math.pow((tapOffset.x - x).toDouble(), 2.0) + 
                                        Math.pow((tapOffset.y - y).toDouble(), 2.0)
                                    )
                                    
                                    if (distance < 50) { // Hit radius
                                        foundIndex = index
                                    }
                                }
                                selectedPointIndex = foundIndex
                            }
                        }

                ) {
                    val chartWidth = size.width
                    val chartHeight = size.height
                    
                    // Chart parameters (weight in jin, range 140-150)
                    val minWeight = 140f
                    val maxWeight = 150f
                    val weightRange = maxWeight - minWeight
                    
                    // Determine days in month
                    val maxDay = currentDate.getActualMaximum(Calendar.DAY_OF_MONTH)
                    
                    val leftMargin = 80f
                    val bottomMargin = 60f
                    val topMargin = 40f
                    val rightMargin = 40f
                    
                    val plotWidth = chartWidth - leftMargin - rightMargin
                    val plotHeight = chartHeight - topMargin - bottomMargin
                    
                    // Draw Y-axis (dates/days) - Top to Bottom
                    drawLine(
                        color = Color.Black,
                        start = Offset(leftMargin, topMargin),
                        end = Offset(leftMargin, chartHeight - bottomMargin),
                        strokeWidth = 2f
                    )
                    
                    // Draw X-axis (weight) - Bottom
                    drawLine(
                        color = Color.Black,
                        start = Offset(leftMargin, chartHeight - bottomMargin),
                        end = Offset(chartWidth - rightMargin, chartHeight - bottomMargin),
                        strokeWidth = 2f
                    )
                    
                    // Draw Y-axis labels (days) - 1 to maxDay, Top to Bottom
                    for (day in 1..maxDay) {
                        // Calculate Y position: Top is day 1, Bottom is maxDay
                        val normalizedPos = (day - 1).toFloat() / (maxDay - 1)
                        val y = topMargin + normalizedPos * plotHeight
                        
                        // Draw label for every 5 days or 1st/last
                        if (day == 1 || day == maxDay || day % 5 == 0) {
                            val labelText = textMeasurer.measure(
                                text = "${day}日",
                                style = TextStyle(fontSize = 12.sp, color = Color.Black)
                            )
                            drawText(
                                textLayoutResult = labelText,
                                topLeft = Offset(
                                    leftMargin - labelText.size.width - 16f,
                                    y - labelText.size.height / 2
                                )
                            )
                            
                            // Grid line
                            drawLine(
                                color = Color.LightGray.copy(alpha = 0.5f),
                                start = Offset(leftMargin, y),
                                end = Offset(chartWidth - rightMargin, y),
                                strokeWidth = 1f
                            )
                        }
                    }
                    
                    // Draw X-axis labels (weight in jin) - 140 to 150
                    for (weight in 140..150) {
                        val normalizedPos = (weight - minWeight) / weightRange
                        val x = leftMargin + normalizedPos * plotWidth
                        
                        // Draw label for every 1 unit
                        val labelText = textMeasurer.measure(
                            text = "${weight}",
                            style = TextStyle(fontSize = 12.sp, color = Color.Black)
                        )
                        drawText(
                            textLayoutResult = labelText,
                            topLeft = Offset(
                                x - labelText.size.width / 2,
                                chartHeight - bottomMargin + 16f
                            )
                        )
                        
                        // Grid line (Vertical)
                        drawLine(
                            color = Color.LightGray.copy(alpha = 0.5f),
                            start = Offset(x, topMargin),
                            end = Offset(x, chartHeight - bottomMargin),
                            strokeWidth = 1f
                        )
                    }

                    // Draw data line
                    if (sampleData.isNotEmpty()) {
                        val path = Path()
                        sampleData.forEachIndexed { index, (day, weight) ->
                            // Clamp weight to range
                            val clampedWeight = weight.coerceIn(minWeight, maxWeight)
                            
                            val x = leftMargin + ((clampedWeight - minWeight) / weightRange) * plotWidth
                            val y = topMargin + ((day - 1).toFloat() / (maxDay - 1)) * plotHeight
                            
                            if (index == 0) {
                                path.moveTo(x, y)
                            } else {
                                path.lineTo(x, y)
                            }
                            
                            // Draw data point
                            val isSelected = selectedPointIndex == index
                            val radius = if (isSelected) 10f else 6f
                            val color = if (isSelected) Color.Red else Color.Black
                            
                            drawCircle(
                                color = color,
                                radius = radius,
                                center = Offset(x, y)
                            )
                            
                            // Draw value if selected
                            if (isSelected) {
                                val valueText = textMeasurer.measure(
                                    text = String.format("%.1f", weight),
                                    style = TextStyle(
                                        fontSize = 14.sp, 
                                        color = Color.Red,
                                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                                    )
                                )
                                drawText(
                                    textLayoutResult = valueText,
                                    topLeft = Offset(
                                        x + 15f,
                                        y - valueText.size.height / 2
                                    )
                                )
                            }
                        }
                        
                        drawPath(
                            path = path,
                            color = Color.Black,
                            style = Stroke(width = 3f)
                        )
                    }
                }
            }
        }
    }
}
