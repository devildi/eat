package com.example.eat.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ProfileContent(
    viewModel: MainViewModel, 
    selectedDate: Long,
    onDateChange: (Long) -> Unit
) {
    val scrollState = rememberScrollState()
    // Collect data from ViewModel
    val events by viewModel.getEventsByDate(selectedDate).collectAsState(initial = null)
    val weightData by viewModel.weightData.collectAsState()
    val bloodPressureData by viewModel.bloodPressureData.collectAsState()
    
    // Find logs specifically for the selected date
    val weightForDate = remember(weightData, selectedDate) {
        weightData.filter { entity ->
            val calendar = Calendar.getInstance()
            calendar.timeInMillis = entity.timestamp
            val dateCal = Calendar.getInstance().apply { timeInMillis = selectedDate }
            calendar.get(Calendar.YEAR) == dateCal.get(Calendar.YEAR) &&
            calendar.get(Calendar.DAY_OF_YEAR) == dateCal.get(Calendar.DAY_OF_YEAR)
        }.maxByOrNull { it.timestamp }
    }
    
    val bpForDate = remember(bloodPressureData, selectedDate) {
        bloodPressureData.filter { entity ->
            val calendar = Calendar.getInstance()
            calendar.timeInMillis = entity.timestamp
            val dateCal = Calendar.getInstance().apply { timeInMillis = selectedDate }
            calendar.get(Calendar.YEAR) == dateCal.get(Calendar.YEAR) &&
            calendar.get(Calendar.DAY_OF_YEAR) == dateCal.get(Calendar.DAY_OF_YEAR)
        }.maxByOrNull { it.timestamp }
    }

    // Health Data Dialog States
    var showHealthDialog by remember { mutableStateOf(false) }
    var highPressure by remember { mutableStateOf("") }
    var lowPressure by remember { mutableStateOf("") }
    var weight by remember { mutableStateOf("") }
    
    var showDeleteDialog by remember { mutableStateOf(false) }
    var imagePathToDelete by remember { mutableStateOf<String?>(null) }
    var showClearAllDialog by remember { mutableStateOf(false) }

    // Detail View Dialog States
    var selectedEvent by remember { mutableStateOf<Event?>(null) }
    var showEventDetail by remember { mutableStateOf(false) }

    val currentEvents = events

    Scaffold { paddingValues ->
            var dragOffset by remember { mutableStateOf(0f) }
            
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .pointerInput(selectedDate) {
                        detectHorizontalDragGestures(
                            onDragEnd = {
                                if (kotlin.math.abs(dragOffset) > 100f) {
                                    val calendar = Calendar.getInstance()
                                    calendar.timeInMillis = selectedDate
                                    
                                    if (dragOffset > 0) {
                                        // Swipe Right -> Previous Day
                                        calendar.add(Calendar.DAY_OF_YEAR, -1)
                                        onDateChange(calendar.timeInMillis)
                                    } else {
                                        // Swipe Left -> Next Day
                                        calendar.add(Calendar.DAY_OF_YEAR, 1)
                                        onDateChange(calendar.timeInMillis)
                                    }
                                }
                                dragOffset = 0f
                            },
                            onHorizontalDrag = { _, dragAmount ->
                                dragOffset += dragAmount
                            }
                        )
                    }
            ) {
                if (currentEvents == null) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = Color.Black)
                    }
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(0xFFF7F9FA))
                            .verticalScroll(scrollState)
                    ) {
                        // 1. Dashboard Header Card
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                                .clip(RoundedCornerShape(24.dp))
                                .combinedClickable(
                                    onClick = {},
                                    onLongClick = { showClearAllDialog = true }
                                ),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                            shape = RoundedCornerShape(24.dp)
                        ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(56.dp)
                                    .background(Color(0xFFE0F2F1), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "ME",
                                    style = TextStyle(
                                        fontSize = 20.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF00796B)
                                    )
                                )
                            }
                            Column {
                                Text(
                                    text = "我的健康档案",
                                    style = TextStyle(
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.Black
                                    )
                                )
                                Text(
                                    text = "记录今日，管理健康",
                                    style = TextStyle(
                                        fontSize = 13.sp,
                                        color = Color.Gray
                                    )
                                )
                            }
                        }
                    }

                    // 2. Health Stats Summary Cards (Weight, BP, Meals)
                    val mealsCount = remember(currentEvents) { currentEvents.count { it.type == "Main Meal" } }
                    val snacksCount = remember(currentEvents) { currentEvents.count { it.type == "Snack" } }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Card(
                            modifier = Modifier
                                .weight(1f)
                                .clickable { showHealthDialog = true },
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text(text = "体重", style = TextStyle(fontSize = 12.sp, color = Color.Gray))
                                Spacer(modifier = Modifier.height(6.dp))
                                if (weightForDate != null) {
                                    Text(
                                        text = "${String.format("%.1f", weightForDate.value1 * 2f)} 斤",
                                        style = TextStyle(fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                                    )
                                    Text(
                                        text = "${String.format("%.1f", weightForDate.value1)} kg",
                                        style = TextStyle(fontSize = 11.sp, color = Color.Gray)
                                    )
                                } else {
                                    Text(
                                        text = "去记录",
                                        style = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF00796B))
                                    )
                                    Spacer(modifier = Modifier.height(18.dp))
                                }
                            }
                        }

                        Card(
                            modifier = Modifier
                                .weight(1f)
                                .clickable { showHealthDialog = true },
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text(text = "血压", style = TextStyle(fontSize = 12.sp, color = Color.Gray))
                                Spacer(modifier = Modifier.height(6.dp))
                                if (bpForDate != null) {
                                    Text(
                                        text = "${bpForDate.value1.toInt()}/${bpForDate.value2?.toInt() ?: 0}",
                                        style = TextStyle(fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                                    )
                                    Text(
                                        text = "mmHg",
                                        style = TextStyle(fontSize = 11.sp, color = Color.Gray)
                                    )
                                } else {
                                    Text(
                                        text = "去记录",
                                        style = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF00796B))
                                    )
                                    Spacer(modifier = Modifier.height(18.dp))
                                }
                            }
                        }

                        Card(
                            modifier = Modifier.weight(1f),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text(text = "饮食统计", style = TextStyle(fontSize = 12.sp, color = Color.Gray))
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "$mealsCount 正餐",
                                    style = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFFD84315))
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "$snacksCount 零食",
                                    style = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFF8F00))
                                )
                            }
                        }
                    }

                    // 3. Weekly Horizontal Calendar Strip
                    val calendarStripDays = remember(selectedDate) {
                        val list = mutableListOf<Date>()
                        val cal = Calendar.getInstance()
                        cal.timeInMillis = selectedDate
                        val dayOfWeek = cal.get(Calendar.DAY_OF_WEEK)
                        val offset = if (dayOfWeek == Calendar.SUNDAY) -6 else 2 - dayOfWeek
                        cal.add(Calendar.DAY_OF_YEAR, offset)
                        for (i in 0..6) {
                            list.add(cal.time)
                            cal.add(Calendar.DAY_OF_YEAR, 1)
                        }
                        list
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        calendarStripDays.forEach { date ->
                            val cal = Calendar.getInstance().apply { time = date }
                            val dayNum = cal.get(Calendar.DAY_OF_MONTH)
                            val dayOfWeek = cal.get(Calendar.DAY_OF_WEEK)
                            val dayOfWeekStr = when (dayOfWeek) {
                                Calendar.MONDAY -> "一"
                                Calendar.TUESDAY -> "二"
                                Calendar.WEDNESDAY -> "三"
                                Calendar.THURSDAY -> "四"
                                Calendar.FRIDAY -> "五"
                                Calendar.SATURDAY -> "六"
                                Calendar.SUNDAY -> "日"
                                else -> ""
                            }
                            
                            val isSelected = remember(selectedDate, date) {
                                val sCal = Calendar.getInstance().apply { timeInMillis = selectedDate }
                                sCal.get(Calendar.YEAR) == cal.get(Calendar.YEAR) &&
                                sCal.get(Calendar.DAY_OF_YEAR) == cal.get(Calendar.DAY_OF_YEAR)
                            }
                            
                            val isFuture = date.time > System.currentTimeMillis() && !isToday(date)
                            
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (isSelected) Color.Black else Color.Transparent)
                                    .clickable(enabled = !isFuture) {
                                        onDateChange(date.time)
                                    }
                                    .padding(vertical = 8.dp, horizontal = 10.dp)
                            ) {
                                Text(
                                    text = dayOfWeekStr,
                                    style = TextStyle(
                                        fontSize = 11.sp,
                                        color = if (isSelected) Color.White.copy(alpha = 0.8f) else if (isFuture) Color.LightGray else Color.Gray,
                                        fontWeight = FontWeight.Medium
                                    )
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = dayNum.toString(),
                                    style = TextStyle(
                                        fontSize = 15.sp,
                                        color = if (isSelected) Color.White else if (isFuture) Color.LightGray else Color.Black,
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                            }
                        }
                    }

                    // Divider title
                    Text(
                        text = "今日时间线",
                        style = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.Gray),
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )

                    // 4. Daily Timeline Section
                    if (currentEvents.isEmpty()) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(32.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "今天还没有任何饮食记录哦",
                                    style = TextStyle(fontSize = 15.sp, color = Color.Gray)
                                )
                            }
                        }
                    } else {
                        val sortedEvents = remember(currentEvents) { currentEvents.sortedBy { it.latestTimestamp } }
                        
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
                        ) {
                            sortedEvents.forEachIndexed { index, event ->
                                val isFirst = index == 0
                                val isLast = index == sortedEvents.size - 1
                                val date = Date(event.latestTimestamp)
                                
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    // Left side: Timeline Line and Node
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier.width(24.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .width(2.dp)
                                                .height(16.dp)
                                                .background(if (isFirst) Color.Transparent else Color(0xFFB0BEC5))
                                        )
                                        Box(
                                            modifier = Modifier
                                                .size(12.dp)
                                                .background(if (event.type == "Main Meal") Color(0xFFD84315) else Color(0xFFFF8F00), CircleShape)
                                                .border(2.dp, Color.White, CircleShape)
                                        )
                                        Box(
                                            modifier = Modifier
                                                .width(2.dp)
                                                .height(if (isLast) 32.dp else 100.dp) // Provide spacing
                                                .background(if (isLast) Color.Transparent else Color(0xFFB0BEC5))
                                        )
                                    }
                                    
                                    // Right side: Diet Card
                                    Card(
                                        modifier = Modifier
                                            .weight(1f)
                                            .padding(bottom = 16.dp)
                                            .clip(RoundedCornerShape(16.dp))
                                            .clickable { 
                                                selectedEvent = event
                                                showEventDetail = true 
                                            },
                                        colors = CardDefaults.cardColors(containerColor = Color.White),
                                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                                    ) {
                                        Column(modifier = Modifier.padding(16.dp)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                                ) {
                                                    Box(
                                                        modifier = Modifier
                                                            .clip(RoundedCornerShape(6.dp))
                                                            .background(if (event.type == "Main Meal") Color(0xFFFBE9E7) else Color(0xFFFFF8E1))
                                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                                    ) {
                                                        Text(
                                                            text = if (event.type == "Main Meal") "正餐" else "零食",
                                                            color = if (event.type == "Main Meal") Color(0xFFD84315) else Color(0xFFFF8F00),
                                                            style = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                                        )
                                                    }
                                                    Text(
                                                        text = SimpleDateFormat("HH:mm", Locale.getDefault()).format(date),
                                                        style = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Medium, color = Color.Gray)
                                                    )
                                                }
                                            }
                                            
                                            Spacer(modifier = Modifier.height(12.dp))
                                            
                                            if (event.imagePaths.isNotEmpty()) {
                                                Row(
                                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                    modifier = Modifier.fillMaxWidth()
                                                ) {
                                                    event.imagePaths.forEach { path ->
                                                        val bitmap = remember(path) { com.example.eat.utils.ImageUtils.loadRotatedBitmap(path, 200) }
                                                        if (bitmap != null) {
                                                            Image(
                                                                bitmap = bitmap.asImageBitmap(),
                                                                contentDescription = "Food Photo",
                                                                contentScale = ContentScale.Crop,
                                                                modifier = Modifier
                                                                    .size(64.dp)
                                                                    .clip(RoundedCornerShape(12.dp))
                                                                    .border(1.dp, Color(0xFFECEFF1), RoundedCornerShape(12.dp))
                                                            )
                                                        }
                                                    }
                                                }
                                            } else {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                ) {
                                                    event.colorIndices.forEach { colorIndex ->
                                                        val markerColor = when (colorIndex) {
                                                            0 -> Color.Red
                                                            1 -> Color.Yellow
                                                            2 -> Color.Blue
                                                            else -> Color.Red
                                                        }
                                                        Box(
                                                            modifier = Modifier
                                                                .size(10.dp)
                                                                .background(markerColor, CircleShape)
                                                                .border(1.dp, Color.White, CircleShape)
                                                        )
                                                    }
                                                    Text(
                                                        text = "无图片记录",
                                                        style = TextStyle(fontSize = 12.sp, color = Color.LightGray)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(40.dp))
                    }
                }
            }
        }
    }

    // A. Add Health Data Dialog
    if (showHealthDialog) {
        AlertDialog(
            onDismissRequest = { showHealthDialog = false },
            title = { 
                Text(
                    text = "添加健康数据", 
                    style = TextStyle(fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                ) 
            },
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
                Button(
                    onClick = {
                        if (highPressure.isNotEmpty() && lowPressure.isNotEmpty()) {
                            viewModel.saveBloodPressure(highPressure.toFloatOrNull() ?: 0f, lowPressure.toFloatOrNull() ?: 0f)
                        }
                        if (weight.isNotEmpty()) {
                            val weightInJin = weight.toFloatOrNull() ?: 0f
                            val weightInKg = weightInJin * 0.5f
                            viewModel.saveWeight(weightInKg)
                        }
                        highPressure = ""
                        lowPressure = ""
                        weight = ""
                        showHealthDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Black)
                ) {
                    Text("保存")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showHealthDialog = false },
                    colors = ButtonDefaults.textButtonColors(contentColor = Color.Gray)
                ) {
                    Text("取消")
                }
            },
            shape = RoundedCornerShape(24.dp)
        )
    }

    // B. Clear All Data Confirmation Dialog
    if (showClearAllDialog) {
        AlertDialog(
            onDismissRequest = { showClearAllDialog = false },
            title = { Text("删除所有数据", style = TextStyle(fontSize = 18.sp, fontWeight = FontWeight.Bold)) },
            text = { Text("确定要删除所有饮食事件和健康数据吗？此操作无法撤销。") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteAllData()
                        showClearAllDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC62828))
                ) {
                    Text("全部删除")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showClearAllDialog = false },
                    colors = ButtonDefaults.textButtonColors(contentColor = Color.Gray)
                ) {
                    Text("取消")
                }
            },
            shape = RoundedCornerShape(24.dp)
        )
    }

    // C. Event Details Full screen Dialog
    if (showEventDetail && selectedEvent != null) {
        AlertDialog(
            onDismissRequest = { showEventDetail = false },
            title = null,
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = if (selectedEvent!!.type == "Main Meal") "正餐记录" else "零食记录",
                                style = TextStyle(fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                            )
                            Text(
                                text = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(selectedEvent!!.latestTimestamp)),
                                style = TextStyle(fontSize = 12.sp, color = Color.Gray)
                            )
                        }
                        IconButton(onClick = { showEventDetail = false }) {
                            Icon(Icons.Default.Close, contentDescription = "Close")
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    if (selectedEvent!!.imagePaths.isNotEmpty()) {
                        val pagerState = rememberPagerState(pageCount = { selectedEvent!!.imagePaths.size })
                        
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            HorizontalPager(state = pagerState, modifier = Modifier.fillMaxWidth()) { page ->
                                val path = selectedEvent!!.imagePaths[page]
                                val bitmap by produceState<android.graphics.Bitmap?>(initialValue = null, key1 = path) {
                                    value = withContext(Dispatchers.IO) {
                                        com.example.eat.utils.ImageUtils.loadRotatedBitmap(path)
                                    }
                                }
                                
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .aspectRatio(3f / 4f)
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(Color(0xFFF5F5F5))
                                ) {
                                    androidx.compose.animation.AnimatedVisibility(
                                        visible = bitmap != null,
                                        enter = fadeIn(),
                                        exit = fadeOut()
                                    ) {
                                        Image(
                                            bitmap = bitmap!!.asImageBitmap(),
                                            contentDescription = null,
                                            contentScale = ContentScale.Fit,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    }
                                    
                                    IconButton(
                                        onClick = {
                                            imagePathToDelete = path
                                            showDeleteDialog = true
                                        },
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .padding(12.dp)
                                            .size(36.dp)
                                            .background(Color.White.copy(alpha = 0.8f), CircleShape)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Delete Photo",
                                            tint = Color.Black,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }
                            
                            if (selectedEvent!!.imagePaths.size > 1) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(horizontalArrangement = Arrangement.Center) {
                                    repeat(selectedEvent!!.imagePaths.size) { iteration ->
                                        val color = if (pagerState.currentPage == iteration) Color.Black else Color.LightGray
                                        Box(
                                            modifier = Modifier
                                                .padding(3.dp)
                                                .size(6.dp)
                                                .background(color, CircleShape)
                                        )
                                    }
                                }
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(20.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        val isMain = selectedEvent!!.type == "Main Meal"
                        Button(
                            onClick = {
                                viewModel.updateEventCategory(selectedEvent!!.timestamps, if (isMain) "Snack" else "Main Meal")
                                showEventDetail = false
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEEEEEE), contentColor = Color.Black),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(if (isMain) "转为零食" else "转为正餐")
                        }
                        
                        Button(
                            onClick = {
                                showDeleteDialog = true
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFEBEE), contentColor = Color(0xFFC62828)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("删除记录")
                        }
                    }
                }
            },
            confirmButton = {},
            shape = RoundedCornerShape(28.dp),
            properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false),
            modifier = Modifier.padding(16.dp)
        )
    }

    // D. Delete Confirmation Dialog
    if (showDeleteDialog && selectedEvent != null) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("确认删除", style = TextStyle(fontSize = 18.sp, fontWeight = FontWeight.Bold)) },
            text = { Text("确定要删除这条记录吗？") },
            confirmButton = {
                Button(
                    onClick = {
                        if (imagePathToDelete != null) {
                            viewModel.deleteEventByImagePath(imagePathToDelete!!)
                        } else {
                            viewModel.deleteEvent(selectedEvent!!)
                        }
                        imagePathToDelete = null
                        showDeleteDialog = false
                        showEventDetail = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC62828))
                ) {
                    Text("删除")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showDeleteDialog = false },
                    colors = ButtonDefaults.textButtonColors(contentColor = Color.Gray)
                ) {
                    Text("取消")
                }
            },
            shape = RoundedCornerShape(24.dp)
        )
    }
}

// Helper function to check if Date is today
private fun isToday(date: Date): Boolean {
    val cal1 = Calendar.getInstance()
    val cal2 = Calendar.getInstance().apply { time = date }
    return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
           cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
}
