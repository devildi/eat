package com.example.eat.ui

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateValue
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.animation.core.VectorConverter
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.ImageBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

// Data class to hold cached layout calculations
private data class LayoutData(
    val hasData: Boolean,
    val keyHours: Set<String>, // Changed to String to support "9.0", "9.5" format
    val activeHours: Set<Int>,
    val hourYPositions: Map<String, Float>, // Changed to String keys
    val activeHeightPx: Float,
    val totalHeightDp: Dp,
    val sortedKeyHours: List<String> // Changed to String
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ProfileContent(viewModel: MainViewModel, selectedDate: Long) {
    val scrollState = rememberScrollState()
    val textMeasurer = rememberTextMeasurer()

    // Collect events from ViewModel
    val events by viewModel.events.collectAsState()

    // Health Data Dialog State
    var showHealthDialog by remember { mutableStateOf(false) }
    var highPressure by remember { mutableStateOf("") }
    var lowPressure by remember { mutableStateOf("") }
    var weight by remember { mutableStateOf("") }
    var showDeleteDialog by remember { mutableStateOf(false) }

    // Hero Animation State
    var selectedEvent by remember { mutableStateOf<Event?>(null) }
    var showEventDetail by remember { mutableStateOf(false) }

    // 1. Determine Key Hours and Layout Logic
    val currentEvents = events
    if (currentEvents == null) {
        // Loading state: Show loading indicator
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            androidx.compose.material3.CircularProgressIndicator()
        }
        return
    }


    val hasData = currentEvents.isNotEmpty()

    // We need density to convert dp to px for calculation
    val density = androidx.compose.ui.platform.LocalDensity.current

    // Optimize: Cache expensive layout calculations
    val layoutData = remember(currentEvents, density) {
        val keyHours = mutableSetOf<String>()
        val activeHours = mutableSetOf<Int>()
        val hourYPositions = mutableMapOf<String, Float>()
        val activeHeightPx = with(density) { 90.dp.toPx() }

        var currentY = 50f // startY

        if (currentEvents.isEmpty()) {
            // Empty State: 3-hour intervals, 80dp spacing
            val step = 3
            val spacingPx = with(density) { 80.dp.toPx() }

            for (h in 0..24 step step) {
                val key = "$h:00"
                keyHours.add(key)
                hourYPositions[key] = currentY
                if (h < 24) {
                    currentY += spacingPx
                }
            }
        } else {
            // Dynamic State: Based on events with 30-minute intervals
            val activeHalfHours = mutableSetOf<String>()

            // Reuse SimpleDateFormat instance
            val hourFormat = SimpleDateFormat("H", Locale.getDefault())
            val minuteFormat = SimpleDateFormat("m", Locale.getDefault())

            currentEvents.forEach { event ->
                val date = Date(event.latestTimestamp)
                val hour = hourFormat.format(date).toInt()
                val minute = minuteFormat.format(date).toInt()

                // Determine which 30-minute segment this event falls into
                val halfHourKey = if (minute < 30) {
                    "$hour:00"
                } else {
                    "$hour:30"
                }
                activeHalfHours.add(halfHourKey)
                activeHours.add(hour)
            }

            // Generate 30-minute interval marks around active segments
            activeHalfHours.forEach { halfHourKey ->
                val parts = halfHourKey.split(":")
                val hour = parts[0].toInt()
                val isOnHour = parts[1] == "00"

                if (isOnHour) {
                    // Event in :00-:29, add marks at hour:00 and hour:30
                    keyHours.add("$hour:00")
                    keyHours.add("$hour:30")
                } else {
                    // Event in :30-:59, add marks at hour:30 and (hour+1):00
                    keyHours.add("$hour:30")
                    keyHours.add("${hour + 1}:00")
                }
            }

            // Also add 0:00 and 24:00
            keyHours.add("0:00")
            keyHours.add("24:00")

            // Sort keys properly
            val sortedKeyHours = keyHours.toList().sortedWith(compareBy(
                { it.split(":")[0].toInt() },
                { it.split(":")[1].toInt() }
            ))

            val activeSegmentPx = with(density) { 200.dp.toPx() } // Active segment spacing (changed to 200dp)
            val inactiveSegmentPx = with(density) { 50.dp.toPx() }

            sortedKeyHours.forEachIndexed { index, hourMark ->
                hourYPositions[hourMark] = currentY

                if (index < sortedKeyHours.size - 1) {
                    // Check if this segment is active
                    val isActive = activeHalfHours.contains(hourMark)
                    val segmentHeight = if (isActive) activeSegmentPx else inactiveSegmentPx
                    currentY += segmentHeight
                }
            }
        }

        val totalHeightPx = currentY + with(density) { 50.dp.toPx() } // Add bottom padding
        val totalHeightDp = with(density) { totalHeightPx.toDp() }
        val sortedKeyHours = keyHours.toList().sortedWith(compareBy(
            { it.split(":")[0].toInt() },
            { it.split(":")[1].toInt() }
        ))

        LayoutData(
            hasData = currentEvents.isNotEmpty(),
            keyHours = keyHours,
            activeHours = activeHours,
            hourYPositions = hourYPositions,
            activeHeightPx = activeHeightPx,
            totalHeightDp = totalHeightDp,
            sortedKeyHours = sortedKeyHours
        )
    }


    Scaffold(
        floatingActionButton = {
            androidx.compose.material3.Surface(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .combinedClickable(
                        onClick = { showHealthDialog = true },
                        onLongClick = { showDeleteDialog = true }
                    ),
                color = Color.Black,
                contentColor = Color.White,
                shape = CircleShape,
                shadowElevation = 6.dp
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Add, contentDescription = "Add Health Data")
                }
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
                    .height(layoutData.totalHeightDp)
                    .padding(horizontal = 16.dp)
            ) {
                val centerX = size.width / 2

                // 3. Render Timeline Segments
                layoutData.sortedKeyHours.forEachIndexed { index, hour ->
                    val startY = layoutData.hourYPositions[hour]!!

                    // Draw Marker (Circle + Text)
                    drawCircle(
                        color = Color.Black,
                        radius = 8f,
                        center = Offset(centerX, startY)
                    )

                    // Draw Hour Text
                    val timeText = hour
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
                    if (index < layoutData.sortedKeyHours.size - 1) {
                        val nextHour = layoutData.sortedKeyHours[index + 1]
                        val endY = layoutData.hourYPositions[nextHour]!!

                        // Determine line style
                        val isSolid = if (!layoutData.hasData) {
                            true // Empty state always solid
                        } else {
                            // Dynamic state: solid if this is an active segment
                            // Check if current hour mark is in the active segments
                            val hourParts = hour.split(":")
                            val nextHourParts = nextHour.split(":")
                            val currentHourInt = hourParts[0].toInt()
                            val currentMinute = hourParts[1].toInt()
                            val nextHourInt = nextHourParts[0].toInt()
                            val nextMinute = nextHourParts[1].toInt()

                            // Check if they are consecutive 30-minute marks
                            val isConsecutive = (currentHourInt == nextHourInt && currentMinute == 0 && nextMinute == 30) ||
                                               (currentHourInt + 1 == nextHourInt && currentMinute == 30 && nextMinute == 0)

                            isConsecutive && layoutData.activeHours.contains(currentHourInt)
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
                for (event in currentEvents) {
                    val date = Date(event.latestTimestamp)
                    val hour = SimpleDateFormat("H", Locale.getDefault()).format(date).toInt()
                    val minute = SimpleDateFormat("m", Locale.getDefault()).format(date).toInt()

                    // Calculate Y position
                    val hourKey = if (minute < 30) {
                        "$hour:00"
                    } else {
                        "$hour:30"
                    }
                    val baseY = layoutData.hourYPositions[hourKey]!!
                    val minuteInSegment = if (minute < 30) minute else minute - 30
                    val offset = (minuteInSegment / 30f) * layoutData.activeHeightPx
                    val eventY = baseY + offset

                    val isMainMeal = event.type == "Main Meal"
                    
                    // Calculate dynamic gap: 40dp base, minus 10dp for each overlapping item
                    val numItems = if (event.imagePaths.isNotEmpty()) event.imagePaths.size else event.colorIndices.size
                    val overlapCount = (numItems - 1).coerceAtLeast(0)
                    val symmetricGapDp = (40 - (overlapCount * 10)).coerceAtLeast(0).dp
                    val symmetricGapPx = with(density) { symmetricGapDp.toPx() }

                    // The x-coordinate where the connecting line meets the timeline side of the data container.
                    val lineConnectX = if (isMainMeal) centerX - symmetricGapPx else centerX + symmetricGapPx

                    // Draw connecting line
                    drawLine(
                        color = Color.Gray,
                        start = Offset(lineConnectX, eventY),
                        end = Offset(centerX, eventY),
                        strokeWidth = 2f
                    )
                }

                }

            // Render Events (Composables)
            currentEvents.forEach { event ->
                val date = Date(event.latestTimestamp)
                val hour = SimpleDateFormat("H", Locale.getDefault()).format(date).toInt()
                val minute = SimpleDateFormat("m", Locale.getDefault()).format(date).toInt()
                val hourKey = if (minute < 30) "$hour:00" else "$hour:30"
                val baseY = layoutData.hourYPositions[hourKey]!!
                val minuteInSegment = if (minute < 30) minute else minute - 30
                val offset = (minuteInSegment / 30f) * layoutData.activeHeightPx
                val eventY = baseY + offset
                val numItems = if (event.imagePaths.isNotEmpty()) event.imagePaths.size else event.colorIndices.size
                val isMainMeal = event.type == "Main Meal"
                
                // Calculate dynamic gap: 40dp base, minus 10dp for each overlapping item
                // Overlap count is numItems - 1
                val overlapCount = (numItems - 1).coerceAtLeast(0)
                val symmetricGapDp = (40 - (overlapCount * 10)).coerceAtLeast(0).dp
                val symmetricGapPx = with(density) { symmetricGapDp.toPx() }
                
                val availableWidthPx = (containerWidthPx / 2f) - symmetricGapPx
                val availableWidthDp = with(density) { availableWidthPx.toDp() }
                
                val circleDiameterDp = 30.dp
                val overlapOffsetDp = 15.dp
                val circlesWidthDp = ((numItems - 1) * 15 + 30).dp

                Box(
                    modifier = Modifier
                        .offset(
                            x = with(density) {
                                if (isMainMeal) {
                                    val boxRightEdgePx = containerWidthPx / 2f - symmetricGapPx
                                    (boxRightEdgePx - availableWidthPx).toDp()
                                } else {
                                    (containerWidthPx / 2f + symmetricGapPx).toDp()
                                }
                            },
                            y = with(density) { (eventY - (circleDiameterDp / 2).toPx()).toDp() }
                        )
                        .width(availableWidthDp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = if (isMainMeal) Arrangement.End else Arrangement.Start
                    ) {
                        Box(
                             modifier = Modifier
                                .clip(androidx.compose.foundation.shape.RoundedCornerShape(8.dp))
                                .clickable {
                                    selectedEvent = event
                                    showEventDetail = true
                                }
                                .background(Color(0xFFE0F7FA)) // Light Blue
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (isMainMeal) {
                                    Text(
                                        text = "正餐 ${SimpleDateFormat("HH:mm", Locale.getDefault()).format(date)}",
                                        style = TextStyle(fontSize = 14.sp, color = Color.Gray),
                                        modifier = Modifier.padding(end = 8.dp)
                                    )
                                }
                                Box(modifier = Modifier.size(width = circlesWidthDp, height = circleDiameterDp)) {
                                    if (event.imagePaths.isNotEmpty()) {
                                        event.imagePaths.forEachIndexed { index, path ->
                                            // Load thumbnail (30dp circle = ~90px on most devices)
                                            val bitmap = remember(path) { com.example.eat.utils.ImageUtils.loadRotatedBitmap(path, 200) }
                                            if (bitmap != null) {
                                                Image(
                                                    bitmap = bitmap.asImageBitmap(),
                                                    contentDescription = "Event Photo",
                                                    contentScale = ContentScale.Crop,
                                                    modifier = Modifier
                                                        .size(circleDiameterDp)
                                                        .offset(x = overlapOffsetDp * index)
                                                        .clip(CircleShape)
                                                        .border(1.dp, Color.White, CircleShape)
                                                )
                                            }
                                        }
                                    } else {
                                        event.colorIndices.forEachIndexed { index, colorIndex ->
                                            val markerColor = when (colorIndex) {
                                                0 -> Color.Red
                                                1 -> Color.Yellow
                                                2 -> Color.Blue
                                                else -> Color.Red
                                            }
                                            Box(
                                                modifier = Modifier
                                                    .size(circleDiameterDp)
                                                    .offset(x = overlapOffsetDp * index)
                                                    .background(markerColor, CircleShape)
                                                    .border(1.dp, Color.White, CircleShape)
                                            )
                                        }
                                    }
                                }
                                if (!isMainMeal) {
                                    Text(
                                        text = "零食 ${SimpleDateFormat("HH:mm", Locale.getDefault()).format(date)}",
                                        style = TextStyle(fontSize = 14.sp, color = Color.Gray),
                                        modifier = Modifier.padding(start = 8.dp)
                                    )
                                }
                            }
                        }
                    }
                }

            }
        }
    }

    if (showHealthDialog) {
        AlertDialog(
            onDismissRequest = { showHealthDialog = false },
            title = { Text("添加健康数据") },
            text = {
                Column {
                    OutlinedTextField(
                        value = highPressure,
                        onValueChange = { highPressure = it },
                        label = { Text("高压 (mmHg)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = lowPressure,
                        onValueChange = { lowPressure = it },
                        label = { Text("低压 (mmHg)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = weight,
                        onValueChange = { weight = it },
                        label = { Text("体重 (斤)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (highPressure.isNotEmpty() && lowPressure.isNotEmpty()) {
                            viewModel.saveBloodPressure(highPressure.toFloatOrNull() ?: 0f, lowPressure.toFloatOrNull() ?: 0f)
                        }
                        if (weight.isNotEmpty()) {
                            // Convert jin to kg for storage (1 jin = 0.5 kg)
                            val weightInJin = weight.toFloatOrNull() ?: 0f
                            val weightInKg = weightInJin * 0.5f
                            viewModel.saveWeight(weightInKg)
                        }
                        highPressure = ""
                        lowPressure = ""
                        weight = ""
                        showHealthDialog = false
                    }
                ) {
                    Text("保存")
                }
            },
            dismissButton = {
                TextButton(onClick = { showHealthDialog = false }) {
                    Text("取消")
                }
            }
        )
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("删除所有数据") },
            text = { Text("确定要删除所有事件和健康数据吗？此操作无法撤销。") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteAllData()
                        showDeleteDialog = false
                    }
                ) {
                    Text("删除", color = Color.Red)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("取消")
                }
            }
        )
    }


    if (showEventDetail && selectedEvent != null) {
        AlertDialog(
            onDismissRequest = { showEventDetail = false },
            title = { 
                val titleText = if (selectedEvent!!.type == "Main Meal") "正餐" else "零食"
                Text(titleText) 
            },
            text = {
                Column {
                    if (selectedEvent!!.imagePaths.isNotEmpty()) {
                        val pagerState = rememberPagerState(pageCount = { selectedEvent!!.imagePaths.size })
                        
                        // Display timestamp for current page
                        val currentTimestamp = selectedEvent!!.timestamps.getOrNull(pagerState.currentPage) ?: selectedEvent!!.latestTimestamp
                        Text("时间: ${SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(currentTimestamp))}")
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        HorizontalPager(state = pagerState, modifier = Modifier.fillMaxWidth()) { page ->
                            val path = selectedEvent!!.imagePaths[page]
                            
                            // Load image asynchronously to avoid blocking the main thread
                            val bitmap by produceState<android.graphics.Bitmap?>(initialValue = null, key1 = path) {
                                value = withContext(Dispatchers.IO) {
                                    com.example.eat.utils.ImageUtils.loadRotatedBitmap(path)
                                }
                            }

                            // Fixed-size placeholder box with 3:4 aspect ratio
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .aspectRatio(3f / 4f) // Width:Height = 3:4
                                    .background(Color(0xFFF5F5F5)) // Light gray placeholder
                            ) {
                                // Display image with fade-in animation when loaded
                                androidx.compose.animation.AnimatedVisibility(
                                    visible = bitmap != null,
                                    enter = androidx.compose.animation.fadeIn(
                                        animationSpec = tween(durationMillis = 300)
                                    ),
                                    exit = androidx.compose.animation.fadeOut(
                                        animationSpec = tween(durationMillis = 300)
                                    )
                                ) {
                                    Image(
                                        bitmap = bitmap!!.asImageBitmap(),
                                        contentDescription = null,
                                        contentScale = ContentScale.Fit,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                                
                                // Delete button (always visible)
                                IconButton(
                                    onClick = {
                                        viewModel.deleteEvent(selectedEvent!!)
                                        showEventDetail = false
                                    },
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(20.dp)
                                        .size(24.dp)
                                        .clip(CircleShape)
                                        .background(Color.White.copy(alpha = 0.7f))
                                        .border(1.dp, Color.Black, CircleShape)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Delete",
                                        tint = Color.Black,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                        }
                        if (selectedEvent!!.imagePaths.size > 1) {
                            Row(
                                Modifier.fillMaxWidth().padding(top = 8.dp),
                                horizontalArrangement = Arrangement.Center
                            ) {
                                repeat(selectedEvent!!.imagePaths.size) { iteration ->
                                    val color = if (pagerState.currentPage == iteration) Color.DarkGray else Color.LightGray
                                    Box(
                                        modifier = Modifier
                                            .padding(2.dp)
                                            .clip(CircleShape)
                                            .background(color)
                                            .size(8.dp)
                                    )
                                }
                            }
                        }
                    } else {
                        // For events without images, just show the latest timestamp
                        Text("时间: ${SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(selectedEvent!!.latestTimestamp))}")
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showEventDetail = false }) {
                    Text("关闭")
                }
            }
        )
    }
}
