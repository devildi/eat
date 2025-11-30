package com.example.eat.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeightChartScreen(onBack: () -> Unit) {
    val currentMonth = SimpleDateFormat("M", Locale.getDefault()).format(Calendar.getInstance().time)
    val textMeasurer = rememberTextMeasurer()
    
    // Sample data: (day, weight)
    val sampleData = listOf(
        1 to 145.2f,
        5 to 144.8f,
        10 to 146.1f,
        15 to 145.5f,
        20 to 144.3f,
        25 to 143.9f,
        30 to 144.5f
    )
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("${currentMonth}月体重走势") },
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
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(400.dp)
                    .padding(32.dp)
            ) {
                val chartWidth = size.width
                val chartHeight = size.height
                
                // Chart parameters
                val minWeight = 140f
                val maxWeight = 150f
                val weightRange = maxWeight - minWeight
                val maxDay = 31
                
                val leftMargin = 60f
                val bottomMargin = 40f
                val topMargin = 20f
                val rightMargin = 20f
                
                val plotWidth = chartWidth - leftMargin - rightMargin
                val plotHeight = chartHeight - topMargin - bottomMargin
                
                // Draw Y-axis (dates/days)
                drawLine(
                    color = Color.Black,
                    start = Offset(leftMargin, topMargin),
                    end = Offset(leftMargin, chartHeight - bottomMargin),
                    strokeWidth = 2f
                )
                
                // Draw X-axis (weight)
                drawLine(
                    color = Color.Black,
                    start = Offset(leftMargin, chartHeight - bottomMargin),
                    end = Offset(chartWidth - rightMargin, chartHeight - bottomMargin),
                    strokeWidth = 2f
                )
                
                // Draw Y-axis labels (days)
                for (day in 0..30 step 5) {
                    val y = chartHeight - bottomMargin - (day / 30f) * plotHeight
                    val labelText = textMeasurer.measure(
                        text = "${day + 1}日",
                        style = TextStyle(fontSize = 12.sp, color = Color.Black)
                    )
                    drawText(
                        textLayoutResult = labelText,
                        topLeft = Offset(
                            leftMargin - labelText.size.width - 8f,
                            y - labelText.size.height / 2
                        )
                    )
                    
                    // Grid line
                    drawLine(
                        color = Color.LightGray,
                        start = Offset(leftMargin, y),
                        end = Offset(chartWidth - rightMargin, y),
                        strokeWidth = 1f
                    )
                }
                
                // Draw X-axis labels (weight)
                for (weight in 140..150 step 2) {
                    val x = leftMargin + ((weight - minWeight) / weightRange) * plotWidth
                    val labelText = textMeasurer.measure(
                        text = "${weight}kg",
                        style = TextStyle(fontSize = 12.sp, color = Color.Black)
                    )
                    drawText(
                        textLayoutResult = labelText,
                        topLeft = Offset(
                            x - labelText.size.width / 2,
                            chartHeight - bottomMargin + 8f
                        )
                    )
                    
                    // Grid line
                    drawLine(
                        color = Color.LightGray,
                        start = Offset(x, topMargin),
                        end = Offset(x, chartHeight - bottomMargin),
                        strokeWidth = 1f
                    )
                }
                
                // Draw data line
                val path = Path()
                sampleData.forEachIndexed { index, (day, weight) ->
                    val x = leftMargin + ((weight - minWeight) / weightRange) * plotWidth
                    val y = chartHeight - bottomMargin - ((day - 1) / 30f) * plotHeight
                    
                    if (index == 0) {
                        path.moveTo(x, y)
                    } else {
                        path.lineTo(x, y)
                    }
                    
                    // Draw data point
                    drawCircle(
                        color = Color.Blue,
                        radius = 6f,
                        center = Offset(x, y)
                    )
                }
                
                drawPath(
                    path = path,
                    color = Color.Blue,
                    style = Stroke(width = 3f)
                )
            }
        }
    }
}
