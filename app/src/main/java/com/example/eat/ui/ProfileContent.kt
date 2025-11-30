package com.example.eat.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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

import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.material3.Scaffold
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileContent(viewModel: MainViewModel, selectedDate: Long) {
    val scrollState = rememberScrollState()
    val textMeasurer = rememberTextMeasurer()
    
    // Health Data Dialog State
    var showHealthDialog by remember { mutableStateOf(false) }
    var highPressure by remember { mutableStateOf("") }
    var lowPressure by remember { mutableStateOf("") }
    var weight by remember { mutableStateOf("") }
    
    // Hero Animation State
    var selectedEvent by remember { mutableStateOf<Event?>(null) }
    var showEventDetail by remember { mutableStateOf(false) }
    
    // 1. Determine Key Hours and Layout Logic
    val hasData = viewModel.events.isNotEmpty()
    val keyHours = sortedSetOf<Int>()
    val activeHours = mutableSetOf<Int>()
    val hourYPositions = mutableMapOf<Int, Float>()
    
    // We need density to convert dp to px for calculation
    val density = androidx.compose.ui.platform.LocalDensity.current
    val activeHeightPx = with(density) { 90.dp.toPx() }
    
    var currentY = 50f // startY
    var totalHeightPx = currentY

    if (!hasData) {
        // Empty State: 3-hour intervals, 80dp spacing
        val step = 3
        val spacingPx = with(density) { 80.dp.toPx() }
        
        for (h in 0..24 step step) {
            keyHours.add(h)
            hourYPositions[h] = currentY
            if (h < 24) {
                currentY += spacingPx
            }
        }
    } else {
        // Dynamic State: Based on events
        keyHours.add(0)
        keyHours.add(24)
        
        viewModel.events.forEach { event ->
            val date = Date(event.latestTimestamp)
            val hour = SimpleDateFormat("H", Locale.getDefault()).format(date).toInt()
            keyHours.add(hour)
            keyHours.add(hour + 1)
            activeHours.add(hour)
        }
        
        val sortedKeyHours = keyHours.toList()
        val inactiveHeightPx = with(density) { 50.dp.toPx() }
        
        sortedKeyHours.forEachIndexed { index, hour ->
            hourYPositions[hour] = currentY
            
            if (index < sortedKeyHours.size - 1) {
                val nextHour = sortedKeyHours[index + 1]
                // Check if this segment is active
                val isActive = (nextHour == hour + 1) && activeHours.contains(hour)
                val segmentHeight = if (isActive) activeHeightPx else inactiveHeightPx
                currentY += segmentHeight
            }
        }
    }
    
    totalHeightPx = currentY + with(density) { 50.dp.toPx() } // Add bottom padding
    val totalHeightDp = with(density) { totalHeightPx.toDp() }
    val sortedKeyHours = keyHours.toList()

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showHealthDialog = true },
                containerColor = Color.Black,
                contentColor = Color.White
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Health Data")
            }
        }
    ) { paddingValues ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(scrollState)
        ) {
            val containerWidth = maxWidth
            val containerWidthPx = with(density) { containerWidth.toPx() }
            
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(totalHeightDp)
                    .padding(horizontal = 16.dp)
            ) {
                val centerX = size.width / 2
                
                // 3. Render Timeline Segments
                sortedKeyHours.forEachIndexed { index, hour ->
                    val startY = hourYPositions[hour]!!
                    
                    // Draw Marker (Circle + Text)
                    drawCircle(
                        color = Color.Black,
                        radius = 8f,
                        center = Offset(centerX, startY)
                    )
                    
                    // Draw Hour Text
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
                            startY - textLayoutResult.size.height / 2
                        )
                    )
                    
                    // Draw Line to Next Marker
                    if (index < sortedKeyHours.size - 1) {
                        val nextHour = sortedKeyHours[index + 1]
                        val endY = hourYPositions[nextHour]!!
                        
                        // Determine line style
                        val isSolid = if (!hasData) {
                            true // Empty state always solid
                        } else {
                            // Dynamic state: solid if active
                            (nextHour == hour + 1) && activeHours.contains(hour)
                        }
                        
                        if (isSolid) {
                            // Solid Line
                            drawLine(
                                color = Color.Black,
                                start = Offset(centerX, startY),
                                end = Offset(centerX, endY),
                                strokeWidth = 4f
                            )
                        } else {
                            // Dashed Line
                            val pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(20f, 20f), 0f)
                            drawLine(
                                color = Color.Black,
                                start = Offset(centerX, startY),
                                end = Offset(centerX, endY),
                                strokeWidth = 4f,
                                pathEffect = pathEffect
                            )
                        }
                    }
                }

                // 4. Render Events
                val events = viewModel.events
                for (event in events) {
                    val date = Date(event.latestTimestamp)
                    val hour = SimpleDateFormat("H", Locale.getDefault()).format(date).toInt()
                    val minute = SimpleDateFormat("m", Locale.getDefault()).format(date).toInt()
                    
                    // Calculate Y position
                    // Base Y is the position of the start hour
                    val baseY = hourYPositions[hour]!!
                    // Offset is proportional to the minute within the 90dp active height
                    val offset = (minute / 60f) * activeHeightPx
                    val eventY = baseY + offset
                    
                    // Determine X position based on event type
                    val isMainMeal = event.type == "Main Meal"
                    val markerX = if (isMainMeal) centerX - 100f else centerX + 100f
                    val numCircles = event.colorIndices.size
                    val rightmostCircleX = markerX + (numCircles - 1) * 30f
                    
                    // Adjusted textX to account for overlapping circles
                    val textX = if (isMainMeal) {
                        rightmostCircleX - 30f - 30f 
                    } else {
                        rightmostCircleX + 30f + 30f 
                    }
                    
                    // Calculate text dimensions first
                    val eventTextLayoutResult = textMeasurer.measure(
                        text = "${if (isMainMeal) "正餐" else "零食"} ${SimpleDateFormat("HH:mm", Locale.getDefault()).format(date)}",
                        style = TextStyle(
                            fontSize = 14.sp,
                            color = Color.Gray
                        )
                    )
                    
                    // Calculate background bounds
                    val padding = 8f
                    val leftBound = if (isMainMeal) {
                        textX - eventTextLayoutResult.size.width - padding
                    } else {
                        markerX - 30f - padding
                    }
                    val rightBound = if (isMainMeal) {
                        rightmostCircleX + 30f + padding
                    } else {
                        textX + eventTextLayoutResult.size.width + padding
                    }
                    val topBound = eventY - 30f - padding
                    val bottomBound = eventY + 30f + padding
                    
                    // Draw white background rectangle
                    drawRect(
                        color = Color.White,
                        topLeft = Offset(leftBound, topBound),
                        size = androidx.compose.ui.geometry.Size(
                            width = rightBound - leftBound,
                            height = bottomBound - topBound
                        )
                    )
                    
                    // Draw connecting line
                    drawLine(
                        color = Color.Gray,
                        start = Offset(rightmostCircleX, eventY),
                        end = Offset(centerX, eventY),
                        strokeWidth = 2f
                    )

                    // Draw overlapping circles
                    event.colorIndices.forEachIndexed { index, colorIndex ->
                        val circleX = markerX + index * 30f 
                        val markerColor = when (colorIndex) {
                            0 -> Color.Red
                            1 -> Color.Yellow
                            2 -> Color.Blue
                            else -> Color.Red
                        }
                        drawCircle(
                            color = markerColor,
                            radius = 30f,
                            center = Offset(circleX, eventY)
                        )
                    }
                    
                    // Draw text
                    drawText(
                        textLayoutResult = eventTextLayoutResult,
                        topLeft = Offset(
                            if (isMainMeal) textX - eventTextLayoutResult.size.width else textX,
                            eventY - eventTextLayoutResult.size.height / 2
                        )
                    )
                }
            }
            
            // Clickable overlay for events
            viewModel.events.forEach { event ->
                val date = Date(event.latestTimestamp)
                val hour = SimpleDateFormat("H", Locale.getDefault()).format(date).toInt()
                val minute = SimpleDateFormat("m", Locale.getDefault()).format(date).toInt()
                
                val baseY = hourYPositions[hour]!!
                val offset = (minute / 60f) * activeHeightPx
                val eventY = baseY + offset
                
                val isMainMeal = event.type == "Main Meal"
                val centerX = containerWidthPx / 2
                val markerX = if (isMainMeal) centerX - 100f else centerX + 100f
                val numCircles = event.colorIndices.size
                val rightmostCircleX = markerX + (numCircles - 1) * 30f
                
                // Create clickable box at event position
                Box(
                    modifier = Modifier
                        .offset(
                            x = with(density) { (markerX - 30f).toDp() },
                            y = with(density) { (eventY - 30f).toDp() }
                        )
                        .size(with(density) { ((rightmostCircleX - markerX + 60f).toDp()) }, 60.dp)
                        .clickable {
                            selectedEvent = event
                            showEventDetail = true
                        }
                )
            }
        }
    }
    
    // Hero Animation Detail View
    if (showEventDetail && selectedEvent != null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.5f))
                .clickable { showEventDetail = false },
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.clickable(enabled = false) { }
            ) {
                // Draw circles
                Row(
                    horizontalArrangement = androidx.compose.foundation.layout.Arrangement.Center
                ) {
                    selectedEvent!!.colorIndices.forEach { colorIndex ->
                        val markerColor = when (colorIndex) {
                            0 -> Color.Red
                            1 -> Color.Yellow
                            2 -> Color.Blue
                            else -> Color.Red
                        }
                        Box(
                            modifier = Modifier
                                .size(80.dp)
                                .background(markerColor, CircleShape)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Text below circles
                val date = Date(selectedEvent!!.latestTimestamp)
                Text(
                    text = "${if (selectedEvent!!.type == "Main Meal") "正餐" else "零食"} ${SimpleDateFormat("HH:mm", Locale.getDefault()).format(date)}",
                    style = TextStyle(
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                )
            }
        }
    }

    if (showHealthDialog) {
        AlertDialog(
            onDismissRequest = { showHealthDialog = false },
            title = { Text("Record Health Data") },
            text = {
                Column {
                    OutlinedTextField(
                        value = highPressure,
                        onValueChange = { highPressure = it },
                        label = { Text("High Pressure (mmHg)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = lowPressure,
                        onValueChange = { lowPressure = it },
                        label = { Text("Low Pressure (mmHg)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = weight,
                        onValueChange = { weight = it },
                        label = { Text("Weight (kg)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    // TODO: Save data
                    showHealthDialog = false
                    highPressure = ""
                    lowPressure = ""
                    weight = ""
                }) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showHealthDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
