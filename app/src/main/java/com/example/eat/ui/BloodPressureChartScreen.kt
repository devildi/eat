package com.example.eat.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.Alignment
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlin.math.pow
import kotlin.math.sqrt
import kotlinx.coroutines.launch
import androidx.compose.runtime.collectAsState
import com.example.eat.ui.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BloodPressureChartScreen(
    onBack: () -> Unit,
    viewModel: MainViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
) {
    var currentDate by remember { mutableStateOf(Calendar.getInstance()) }
    val currentMonthStr = SimpleDateFormat("M", Locale.getDefault()).format(currentDate.time)
    val textMeasurer = rememberTextMeasurer()
    
    val bpData by viewModel.bloodPressureData.collectAsState()
    
    // Process data for High (Systolic) and Low (Diastolic) BP
    val (highBpData, lowBpData) = remember(bpData, currentDate) {
        val currentYear = currentDate.get(Calendar.YEAR)
        val currentMonth = currentDate.get(Calendar.MONTH)
        
        val filteredData = bpData.filter { entity ->
            val calendar = Calendar.getInstance()
            calendar.timeInMillis = entity.timestamp
            calendar.get(Calendar.YEAR) == currentYear && calendar.get(Calendar.MONTH) == currentMonth
        }.groupBy { entity ->
            val calendar = Calendar.getInstance()
            calendar.timeInMillis = entity.timestamp
            calendar.get(Calendar.DAY_OF_MONTH)
        }.map { (day, entities) ->
            // Take latest
            val latest = entities.maxByOrNull { it.timestamp }!!
            day to latest
        }.sortedBy { it.first }
        
        val high = filteredData.map { it.first to it.second.value1 }
        val low = filteredData.mapNotNull { 
            if (it.second.value2 != null) it.first to it.second.value2!! else null 
        }
        
        high to low
    }
    
    // State for selected point: Pair(Type, Index) -> Type: 0 for High, 1 for Low
    var selectedPoint by remember { mutableStateOf<Pair<Int, Int>?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("${currentMonthStr}月血压走势") },
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (highBpData.isEmpty() && lowBpData.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "无数据",
                        style = TextStyle(fontSize = 20.sp, color = Color.Gray)
                    )
                }
            } else {
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(16.dp)
                        .pointerInput(Unit) {
                            detectTapGestures { tapOffset ->
                                val chartWidth = size.width
                                val chartHeight = size.height
                                
                                val minVal = 70f
                                val maxVal = 150f
                                val valRange = maxVal - minVal
                                
                                val maxDay = currentDate.getActualMaximum(Calendar.DAY_OF_MONTH)
                                
                                val leftMargin = 80f
                                val bottomMargin = 60f
                                val topMargin = 40f
                                val rightMargin = 40f
                                
                                val plotWidth = chartWidth - leftMargin - rightMargin
                                val plotHeight = chartHeight - topMargin - bottomMargin
                                
                                var found: Pair<Int, Int>? = null
                                
                                // Check High BP Points (Type 0)
                                highBpData.forEachIndexed { index, (day, value) ->
                                    val clampedVal = value.coerceIn(minVal, maxVal)
                                    val x = leftMargin + ((clampedVal - minVal) / valRange) * plotWidth
                                    val y = topMargin + ((day - 1).toFloat() / (maxDay - 1)) * plotHeight
                                    
                                    val dist = sqrt((tapOffset.x - x).pow(2) + (tapOffset.y - y).pow(2))
                                    if (dist < 50) {
                                        found = 0 to index
                                    }
                                }
                                
                                // Check Low BP Points (Type 1) if not found yet
                                if (found == null) {
                                    lowBpData.forEachIndexed { index, (day, value) ->
                                        val clampedVal = value.coerceIn(minVal, maxVal)
                                        val x = leftMargin + ((clampedVal - minVal) / valRange) * plotWidth
                                        val y = topMargin + ((day - 1).toFloat() / (maxDay - 1)) * plotHeight
                                        
                                        val dist = sqrt((tapOffset.x - x).pow(2) + (tapOffset.y - y).pow(2))
                                        if (dist < 50) {
                                            found = 1 to index
                                        }
                                    }
                                }
                                
                                selectedPoint = found
                            }
                        }
                ) {
                    val chartWidth = size.width
                    val chartHeight = size.height
                    
                    val minVal = 70f
                    val maxVal = 150f
                    val valRange = maxVal - minVal
                    
                    val maxDay = currentDate.getActualMaximum(Calendar.DAY_OF_MONTH)
                    
                    val leftMargin = 80f
                    val bottomMargin = 60f
                    val topMargin = 40f
                    val rightMargin = 40f
                    
                    val plotWidth = chartWidth - leftMargin - rightMargin
                    val plotHeight = chartHeight - topMargin - bottomMargin
                    
                    // Draw Y-axis (Dates) - Top to Bottom
                    drawLine(
                        color = Color.Black,
                        start = Offset(leftMargin, topMargin),
                        end = Offset(leftMargin, chartHeight - bottomMargin),
                        strokeWidth = 2f
                    )
                    
                    // Draw X-axis (Values) - Bottom
                    drawLine(
                        color = Color.Black,
                        start = Offset(leftMargin, chartHeight - bottomMargin),
                        end = Offset(chartWidth - rightMargin, chartHeight - bottomMargin),
                        strokeWidth = 2f
                    )
                    
                    // Draw Y-axis Labels
                    for (day in 1..maxDay) {
                        val normalizedPos = (day - 1).toFloat() / (maxDay - 1)
                        val y = topMargin + normalizedPos * plotHeight
                        
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
                            drawLine(
                                color = Color.LightGray.copy(alpha = 0.5f),
                                start = Offset(leftMargin, y),
                                end = Offset(chartWidth - rightMargin, y),
                                strokeWidth = 1f
                            )
                        }
                    }
                    
                    // Draw X-axis Labels
                    for (v in 70..150 step 10) {
                        val normalizedPos = (v - minVal) / valRange
                        val x = leftMargin + normalizedPos * plotWidth
                        
                        val labelText = textMeasurer.measure(
                            text = "$v",
                            style = TextStyle(fontSize = 12.sp, color = Color.Black)
                        )
                        drawText(
                            textLayoutResult = labelText,
                            topLeft = Offset(
                                x - labelText.size.width / 2,
                                chartHeight - bottomMargin + 8f
                            )
                        )
                        drawLine(
                            color = Color.LightGray.copy(alpha = 0.5f),
                            start = Offset(x, topMargin),
                            end = Offset(x, chartHeight - bottomMargin),
                            strokeWidth = 1f
                        )
                    }
                    
                    // Helper to draw series
                    fun drawSeries(data: List<Pair<Int, Float>>, type: Int) {
                        if (data.isEmpty()) return
                        val path = Path()
                        data.forEachIndexed { index, (day, value) ->
                            val clampedVal = value.coerceIn(minVal, maxVal)
                            val x = leftMargin + ((clampedVal - minVal) / valRange) * plotWidth
                            val y = topMargin + ((day - 1).toFloat() / (maxDay - 1)) * plotHeight
                            
                            if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
                            
                            // Draw Point
                            val isSelected = selectedPoint?.first == type && selectedPoint?.second == index
                            val radius = if (isSelected) 8f else 5f
                            val color = if (isSelected) Color.Red else Color.Black
                            
                            drawCircle(color = color, radius = radius, center = Offset(x, y))
                            
                            if (isSelected) {
                                val label = if (type == 0) "高压: ${value.toInt()}" else "低压: ${value.toInt()}"
                                val textResult = textMeasurer.measure(
                                    text = label,
                                    style = TextStyle(fontSize = 14.sp, color = Color.Red, fontWeight = FontWeight.Bold)
                                )
                                drawText(
                                    textLayoutResult = textResult,
                                    topLeft = Offset(x + 12f, y - textResult.size.height / 2)
                                )
                            }
                        }
                        drawPath(path = path, color = Color.Black, style = Stroke(width = 3f))
                    }
                    
                    // Draw High BP (Type 0)
                    drawSeries(highBpData, 0)
                    
                    // Draw Low BP (Type 1)
                    drawSeries(lowBpData, 1)
                }
            }
        }
    }
}
