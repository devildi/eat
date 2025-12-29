package com.example.eat.ui

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDp
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import kotlin.math.roundToInt
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.collectAsState
import androidx.lifecycle.viewmodel.compose.viewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.core.content.ContextCompat
import android.os.Environment
import java.io.File

import android.graphics.BitmapFactory
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.foundation.Image
import androidx.compose.ui.layout.ContentScale
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import coil.compose.AsyncImage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(viewModel: MainViewModel = viewModel()) {
    var selectedItem by remember { mutableIntStateOf(2) } // Default to Camera (index 2)
    val items = listOf("阅读", "听力", "相机", "我的")
    val icons = listOf(
        Icons.Filled.Home,
        Icons.Filled.Headphones, // Replaced PhotoCamera -> Headphones for Listening could use Headset or PlayArrow if Headphones not available
        Icons.Filled.PhotoCamera,
        Icons.Filled.Person
    )
    
    // Date Picker State
    var showDatePicker by remember { mutableStateOf(false) }
    var selectedDate by remember { mutableStateOf(System.currentTimeMillis()) }

    
    // Navigation State
    var showWeightChart by remember { mutableStateOf(false) }
    var showBloodPressureChart by remember { mutableStateOf(false) }
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    
    // Article Dialog State
    var showAddArticleDialog by remember { mutableStateOf(false) }
    var articleUrl by remember { mutableStateOf("") }
    
    // Detail View State
    var selectedArticle by remember { mutableStateOf<com.example.eat.data.ArticleEntity?>(null) }
    
    // Deletion State
    var articleToDelete by remember { mutableStateOf<com.example.eat.data.ArticleEntity?>(null) }
    
    // Parsing & Editing State
    var isParsing by remember { mutableStateOf(false) }
    var isEditing by remember { mutableStateOf(false) }
    var editTitle by remember { mutableStateOf("") }
    var editContent by remember { mutableStateOf("") }

    var context = androidx.compose.ui.platform.LocalContext.current
    val playbackPosition by viewModel.currentPlaybackPosition.collectAsState()
    val isCalibrated by viewModel.isCalibrated.collectAsState()
    val todayArticleCount by viewModel.todayArticleCount.collectAsState()

    // Config Dialog State
    var showConfigDialog by remember { mutableStateOf(false) }
    var showLanSync by remember { mutableStateOf(false) }

    // Loading Indicator
    if (isParsing) {
        androidx.compose.ui.window.Dialog(onDismissRequest = {}) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(100.dp)
                    .background(Color.White, shape = RoundedCornerShape(8.dp))
            ) {
                androidx.compose.material3.CircularProgressIndicator()
            }
        }
    }

    // Editing Screen
    if (isEditing) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White)
                .padding(16.dp)
        ) {
            Text(
                text = "编辑文章",
                fontSize = 20.sp,
                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                color = Color.Black,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            androidx.compose.material3.OutlinedTextField(
                value = editTitle,
                onValueChange = { editTitle = it },
                label = { Text("标题") },
                modifier = Modifier.fillMaxWidth()
            )

            androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(16.dp))

            androidx.compose.material3.OutlinedTextField(
                value = editContent,
                onValueChange = { editContent = it },
                label = { Text("正文") },
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f), // Takes up remaining space
                minLines = 5
            )

            androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    scope.launch {
                        if (viewModel.isArticleExists(editTitle)) {
                            android.widget.Toast.makeText(context, "文章已存在", android.widget.Toast.LENGTH_SHORT).show()
                        } else {
                            viewModel.saveArticle(editTitle, editContent, articleUrl)
                            isEditing = false
                            articleUrl = ""
                            editTitle = ""
                            editContent = ""
                            android.widget.Toast.makeText(context, "已保存", android.widget.Toast.LENGTH_SHORT).show()
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("保存")
            }
            
            androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(8.dp))
             
            TextButton(
                onClick = { isEditing = false },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("取消")
            }
        }
        return
    }

    if (showAddArticleDialog) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showAddArticleDialog = false },
            title = { Text("添加文章") },
            text = {
                androidx.compose.material3.TextField(
                    value = articleUrl,
                    onValueChange = { articleUrl = it },
                    label = { Text("文章链接") },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    if (articleUrl.isNotEmpty()) {
                        showAddArticleDialog = false
                        isParsing = true
                        viewModel.parseArticle(
                            url = articleUrl,
                            onSuccess = { title, content ->
                                isParsing = false
                                isEditing = true
                                editTitle = title
                                editContent = content
                            },
                            onError = { error ->
                                isParsing = false
                                android.widget.Toast.makeText(context, "Error: $error", android.widget.Toast.LENGTH_SHORT).show()
                            }
                        )
                    }
                }) {
                    Text("添加")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddArticleDialog = false }) {
                    Text("取消")
                }
            }
        )
    }

    if (articleToDelete != null) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { articleToDelete = null },
            title = { Text("删除文章") },
            text = { Text("确定要删除吗？") },
            confirmButton = {
                TextButton(onClick = {
                    articleToDelete?.let { article ->
                        viewModel.deleteArticle(article)
                    }
                    articleToDelete = null
                }) {
                    Text("删除", color = androidx.compose.material3.MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { articleToDelete = null }) {
                    Text("取消")
                }
            }
        )
    }

    if (showConfigDialog) {
        val currentIsSyncEnabled by viewModel.isSyncEnabled.collectAsState()
        val currentSyncTarget by viewModel.syncTarget.collectAsState()

        var tempIsSyncEnabled by remember { mutableStateOf(currentIsSyncEnabled) }
        var tempSyncTarget by remember { mutableStateOf(currentSyncTarget) }

        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showConfigDialog = false },
            title = { Text("配置选项") },
            text = {
                Column {
                    // Sync Switch
                    androidx.compose.foundation.layout.Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("数据同步")
                        androidx.compose.material3.Switch(
                            checked = tempIsSyncEnabled,
                            onCheckedChange = { tempIsSyncEnabled = it }
                        )
                    }

                    // Sync Target Toggle
                    if (tempIsSyncEnabled) {
                        androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(16.dp))
                        androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(16.dp))
                        
                        // Firebase Switch
                        androidx.compose.foundation.layout.Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Firebase")
                            androidx.compose.material3.Switch(
                                checked = tempSyncTarget == "FIREBASE",
                                onCheckedChange = { if (it) tempSyncTarget = "FIREBASE" }
                            )
                        }

                        // Backend Switch
                        androidx.compose.foundation.layout.Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("后台服务")
                            androidx.compose.material3.Switch(
                                checked = tempSyncTarget == "BACKEND",
                                onCheckedChange = { if (it) tempSyncTarget = "BACKEND" }
                            )
                        }

                        // LAN Sync Switch
                        androidx.compose.foundation.layout.Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("局域网同步")
                            androidx.compose.material3.Switch(
                                checked = tempSyncTarget == "LAN",
                                onCheckedChange = { if (it) tempSyncTarget = "LAN" }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { 
                    viewModel.setSyncEnabled(tempIsSyncEnabled)
                    viewModel.setSyncTarget(tempSyncTarget)
                    showConfigDialog = false 
                    if (tempSyncTarget == "LAN") {
                        showLanSync = true
                    }
                }) {
                    Text("确认")
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfigDialog = false }) {
                    Text("取消")
                }
            }
        )
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = null,
            initialDisplayedMonthMillis = selectedDate,
            selectableDates = object : androidx.compose.material3.SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                    // Allow dates up to the current moment (effectively allowing "Today")
                    return utcTimeMillis <= System.currentTimeMillis()
                }
            }
        )

        // Observe state changes. When a date is selected (becomes non-null), update and dismiss.
        androidx.compose.runtime.LaunchedEffect(datePickerState) {
            androidx.compose.runtime.snapshotFlow { datePickerState.selectedDateMillis }
                .collect { selectedDateMillis ->
                    if (selectedDateMillis != null) {
                        selectedDate = selectedDateMillis
                        showDatePicker = false
                    }
                }
        }

        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancel")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
    
    // Show WeightChartScreen if navigated
    if (showWeightChart) {
        WeightChartScreen(onBack = { showWeightChart = false })
        return
    }

    // Show Detail Screen if selected
    if (selectedArticle != null) {
        ArticleDetailScreen(
            article = selectedArticle!!,
            onBack = { selectedArticle = null }
        )
        return
    }
    
    // Show BloodPressureChartScreen if navigated
    if (showBloodPressureChart) {
        BloodPressureChartScreen(onBack = { showBloodPressureChart = false })
        return
    }

    // Show LanSyncScreen if navigated
    if (showLanSync) {
        LanSyncScreen(onBack = { showLanSync = false }, viewModel = viewModel)
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    if (selectedItem == 3) {
                        androidx.compose.material3.IconButton(onClick = { showDatePicker = true }) {
                            Icon(
                                imageVector = Icons.Filled.DateRange,
                                contentDescription = "Select Date",
                                tint = Color.Black
                            )
                        }
                    }
                },
                title = {
                    val titleText = when (selectedItem) {
                        0 -> "每日阅读"
                        1 -> "每日听力"
                        2 -> "吃了什么"
                        3 -> SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(selectedDate))
                        else -> "吃了什么"
                    }
                    Text(
                        text = titleText,
                        modifier = if (selectedItem == 3) {
                            Modifier.pointerInput(Unit) {
                                detectTapGestures(onTap = { showDatePicker = true })
                            }
                        } else {
                            Modifier
                        }
                    )
                },
                actions = {
                    if (selectedItem == 0) {
                        Text(
                            text = "今日已读${todayArticleCount}篇",
                            color = Color.Black,
                            modifier = Modifier.padding(end = 16.dp)
                        )
                    }


                    if (selectedItem == 1) {
                         androidx.compose.material3.TextButton(
                            onClick = {
                                viewModel.syncTranscript(playbackPosition)
                                android.widget.Toast.makeText(context, "已校准起始点", android.widget.Toast.LENGTH_SHORT).show()
                            },
                            colors = androidx.compose.material3.ButtonDefaults.textButtonColors(
                                contentColor = Color.Black
                            )
                        ) {
                            Text(if (isCalibrated) "已校准" else "校准")
                        }
                    }

                    if (selectedItem == 3) {
                        // Weight Chart Icon
                        androidx.compose.material3.IconButton(onClick = { showWeightChart = true }) {
                            Icon(
                                imageVector = Icons.Filled.ShowChart,
                                contentDescription = "Weight Chart",
                                tint = Color.Black
                            )
                        }
                        // Blood Pressure Chart Icon
                        androidx.compose.material3.IconButton(onClick = { showBloodPressureChart = true }) {
                            Icon(
                                imageVector = Icons.Filled.Favorite,
                                contentDescription = "Blood Pressure Chart",
                                tint = Color.Black
                            )
                        }
                        // Settings Icon for Config
                        androidx.compose.material3.IconButton(onClick = { showConfigDialog = true }) {
                            Icon(
                                imageVector = Icons.Filled.Settings,
                                contentDescription = "Configuration",
                                tint = Color.Black
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White,
                    titleContentColor = Color.Black
                )
            )
        },
        floatingActionButton = {
            if (selectedItem == 0) {
                androidx.compose.material3.FloatingActionButton(
                    onClick = { showAddArticleDialog = true },
                    containerColor = Color.Black,
                    contentColor = Color.White
                ) {
                    Icon(Icons.Filled.Add, contentDescription = "Add")
                }
            }
        },
        bottomBar = {
            NavigationBar(
                containerColor = Color.White
            ) {
                items.forEachIndexed { index, item ->
                    NavigationBarItem(
                        icon = { Icon(icons[index], contentDescription = item) },
                        label = { Text(item) },
                        selected = selectedItem == index,
                        onClick = { selectedItem = index },
                        colors = NavigationBarItemDefaults.colors(
                            selectedTextColor = Color.Black,
                            unselectedTextColor = Color.LightGray,
                            selectedIconColor = Color.Black,
                            unselectedIconColor = Color.LightGray,
                            indicatorColor = Color.White
                        )
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (selectedItem) {
                0 -> HomeContent(
                    viewModel, 
                    onArticleClick = { selectedArticle = it },
                    onArticleLongClick = { articleToDelete = it }
                )
                1 -> ListeningContent(viewModel)
                2 -> CameraContent(viewModel)
                3 -> ProfileContent(
                    viewModel, 
                    selectedDate,
                    onDateChange = { newDate ->
                        if (newDate <= System.currentTimeMillis()) {
                            selectedDate = newDate
                        } else {
                            android.widget.Toast.makeText(context, "无法查看未来日期", android.widget.Toast.LENGTH_SHORT).show()
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun ListeningContent(viewModel: MainViewModel) {
    val currentTedTalk by viewModel.currentTedTalk.collectAsState()
    val hasTedFetchCompleted by viewModel.hasTedFetchCompleted.collectAsState()
    val isTedLoading by viewModel.isTedLoading.collectAsState()
    val isCalibrated by viewModel.isCalibrated.collectAsState()
    
    // Initial fetch if null
    androidx.compose.runtime.LaunchedEffect(Unit) {
        if (currentTedTalk == null && !isTedLoading) {
            viewModel.fetchRandomTedTalk()
        }
    }

    var isPlaying by remember { mutableStateOf(false) }
    var isPrepared by remember { mutableStateOf(false) } // Track preparation state
    var currentPlaybackPosition by remember { androidx.compose.runtime.mutableLongStateOf(0L) }
    val listState = androidx.compose.foundation.lazy.rememberLazyListState()

    // MediaPlayer State
    val context = androidx.compose.ui.platform.LocalContext.current
    val mediaPlayer = remember { android.media.MediaPlayer() }
    
    // Cleanup MediaPlayer
    androidx.compose.runtime.DisposableEffect(Unit) {
        onDispose {
            try {
                if (mediaPlayer.isPlaying) {
                    mediaPlayer.stop()
                }
                mediaPlayer.release()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // Load Audio when talk changes
    androidx.compose.runtime.LaunchedEffect(currentTedTalk) {
        currentTedTalk?.let { talk ->
            try {
                isPrepared = false // Reset prepared state
                mediaPlayer.reset()
                mediaPlayer.setAudioAttributes(
                    android.media.AudioAttributes.Builder()
                        .setContentType(android.media.AudioAttributes.CONTENT_TYPE_MUSIC)
                        .setUsage(android.media.AudioAttributes.USAGE_MEDIA)
                        .build()
                )
                mediaPlayer.setDataSource(talk.audioUrl)
                mediaPlayer.prepareAsync()
                mediaPlayer.setOnPreparedListener { 
                    // Ready to play
                    isPrepared = true
                    if (isPlaying) {
                        it.start()
                    }
                }
                mediaPlayer.setOnCompletionListener { 
                    isPlaying = false
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
    
    // Handle Play/Pause
    androidx.compose.runtime.LaunchedEffect(isPlaying) {
        try {
            if (isPlaying) {
                if (isPrepared && !mediaPlayer.isPlaying) {
                    mediaPlayer.start()
                }
            } else {
                if (mediaPlayer.isPlaying) {
                    mediaPlayer.pause()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    // Rotation Animation
    val currentRotation = remember { androidx.compose.animation.core.Animatable(0f) }
    androidx.compose.runtime.LaunchedEffect(isPlaying) {
        if (isPlaying) {
            // Launch rotation animation
            launch {
                while (true) {
                    currentRotation.animateTo(
                        targetValue = currentRotation.value + 360f,
                        animationSpec = tween(3000, easing = androidx.compose.animation.core.LinearEasing)
                    )
                }
            }
            // Launch position polling
            while (true) {
                if (mediaPlayer.isPlaying) {
                    val pos = mediaPlayer.currentPosition.toLong()
                    currentPlaybackPosition = pos
                    viewModel.updatePlaybackPosition(pos)
                }
                kotlinx.coroutines.delay(100)
            }
        }
    }

    // Auto-scroll effect
    // Calculate active line index
    val activeIndex = remember(currentPlaybackPosition, currentTedTalk, isCalibrated) {
        if (!isCalibrated) -1
        else currentTedTalk?.transcriptLines?.indexOfLast { it.startTime <= currentPlaybackPosition } ?: -1
    }

    androidx.compose.runtime.LaunchedEffect(activeIndex) {
        if (activeIndex >= 0) {
            // Scroll to the item + 1 (because index 0 is the title/header)
            // Or better, assume list contains [Header, ...Lines]
            // Header is index 0. Lines start at index 1.
            // So scrolling to activeIndex + 1.
            try {
                listState.animateScrollToItem(activeIndex + 1)
            } catch (e: Exception) {
                // Ignore scroll errors
            }
        }
    }

    // Show Loading if fetch hasn't completed yet
    if (!hasTedFetchCompleted) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            androidx.compose.material3.CircularProgressIndicator()
        }
        return
    }

    // Show Empty/Error if fetch completed but no talk found
    if (currentTedTalk == null) {
         Box(
            modifier = Modifier
                .fillMaxSize()
                .clickable { viewModel.fetchRandomTedTalk() },
            contentAlignment = Alignment.Center
         ) {
            Text("暂无内容 (点击刷新)", color = Color.Gray)
        }
        return
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Top Half: Record Player
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(Color(0xFFF5F5F5)), 
            contentAlignment = Alignment.Center
        ) {
            // Vinyl Record Container (Static Size)
            Box(
                modifier = Modifier.size(240.dp),
                contentAlignment = Alignment.Center
            ) {
                // 1. Visual Layer (Rotating)
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .rotate(currentRotation.value)
                        .clip(CircleShape)
                        .background(Color.Black)
                        .border(2.dp, Color.Gray, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    // Cover Image
                    if (currentTedTalk != null) {
                        coil.compose.AsyncImage(
                            model = currentTedTalk?.imageUrl,
                            contentDescription = "Cover",
                            modifier = Modifier
                                .size(130.dp)
                                .clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .size(130.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFE53935)) 
                        )
                    }
                    
                    // Vinyl grooves
                    Box(modifier = Modifier.size(200.dp).border(1.dp, Color(0xFF333333), CircleShape))
                    Box(modifier = Modifier.size(160.dp).border(1.dp, Color(0xFF333333), CircleShape))
                }

                // 2. Interaction Layer (Static & Transparent)
                // We handle gestures here so coordinates don't rotate with the vinyl
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape)
                        .pointerInput(Unit) {
                            detectDragGestures(
                                onDragStart = { },
                                onDragEnd = { },
                                onDrag = { change, dragAmount ->
                                    val center = Offset(size.width / 2f, size.height / 2f)
                                    val currentPos = change.position
                                    val prevPos = change.position - dragAmount
                                    
                                    // Calculate angles
                                    val currentAngle = kotlin.math.atan2(currentPos.y - center.y, currentPos.x - center.x)
                                    val prevAngle = kotlin.math.atan2(prevPos.y - center.y, prevPos.x - center.x)
                                    
                                    var angleDiff = (currentAngle - prevAngle).toDouble()
                                    
                                    // Handle wrap-around (e.g. PI to -PI)
                                    if (angleDiff > Math.PI) angleDiff -= 2 * Math.PI
                                    if (angleDiff < -Math.PI) angleDiff += 2 * Math.PI
                                    
                                    // Sensitivity: 1 full rotation (2PI) = 60 seconds (60000ms)
                                    // So ms = (diff / 2PI) * 60000
                                    val seekMs = (angleDiff / (2 * Math.PI)) * 60000
                                    
                                    val newPos = (mediaPlayer.currentPosition + seekMs).toInt()
                                    val duration = mediaPlayer.duration
                                    
                                    if (duration > 0) {
                                        val clampedPos = newPos.coerceIn(0, duration)
                                        mediaPlayer.seekTo(clampedPos)
                                    }
                                }
                            )
                        }
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onTap = { isPlaying = !isPlaying }
                            )
                        }
                )

                // Play Button Overlay (Static, on top)
                if (!isPlaying) {
                    Icon(
                        imageVector = Icons.Filled.PlayArrow,
                        contentDescription = "Play",
                        tint = Color.White,
                        modifier = Modifier
                            .size(64.dp)
                            .background(Color.Black.copy(alpha = 0.3f), CircleShape)
                            .padding(8.dp)
                    )
                }
            }
        }

        // Bottom Half: Text
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(Color.White)
                .padding(16.dp)
        ) {
             androidx.compose.foundation.lazy.LazyColumn(
                modifier = Modifier.fillMaxSize(),
                state = listState
            ) {
                item {
                    Text(
                        text = currentTedTalk?.title ?: "Loading...",
                        fontSize = 20.sp,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                }
                if (currentTedTalk?.transcriptLines.isNullOrEmpty()) {
                    item {
                         Text(
                            text = currentTedTalk?.transcript ?: "...",
                            fontSize = 16.sp,
                            lineHeight = 24.sp,
                            color = Color.DarkGray
                        )
                    }
                } else {
                    itemsIndexed(currentTedTalk!!.transcriptLines) { index, line ->
                         val isActive = index == activeIndex
                         Text(
                            text = line.text,
                            fontSize = 16.sp,
                            lineHeight = 24.sp,
                            color = if (isActive) Color.Red else Color.DarkGray,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clickable {
                                    mediaPlayer?.seekTo(line.startTime.toInt())
                                    isPlaying = true
                                }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CameraContent(viewModel: MainViewModel) {
    var isPhotoCaptured by remember { mutableStateOf(false) }
    var isTakingPhoto by remember { mutableStateOf(false) }

    var capturedPhotoPath by remember { mutableStateOf<String?>(null) }
    val imageCapture = remember { ImageCapture.Builder().build() }
    val density = androidx.compose.ui.platform.LocalDensity.current
    val context = androidx.compose.ui.platform.LocalContext.current
    
    // Cleanup: Delete photo when component is disposed (user navigates away)
    androidx.compose.runtime.DisposableEffect(Unit) {
        onDispose {
            // If there's a captured photo that hasn't been saved, delete it
            if (isPhotoCaptured && capturedPhotoPath != null) {
                try {
                    val file = File(capturedPhotoPath!!)
                    if (file.exists()) {
                        file.delete()
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val maxWidth = maxWidth
        val maxHeight = maxHeight
        // Calculate initial preview height based on 4:3 ratio
        val initialPreviewHeight = maxWidth * 4 / 3

        // Animation Transition
        val transition = updateTransition(targetState = isPhotoCaptured, label = "CaptureTransition")

        val previewWidth by transition.animateDp(
            transitionSpec = { tween(durationMillis = 500, easing = FastOutSlowInEasing) },
            label = "Width"
        ) { captured ->
            if (captured) 80.dp else maxWidth
        }

        val previewHeight by transition.animateDp(
            transitionSpec = { tween(durationMillis = 500, easing = FastOutSlowInEasing) },
            label = "Height"
        ) { captured ->
            if (captured) 80.dp else initialPreviewHeight
        }

        val previewX by transition.animateDp(
            transitionSpec = { tween(durationMillis = 500, easing = FastOutSlowInEasing) },
            label = "X"
        ) { captured ->
            if (captured) (maxWidth - 80.dp) / 2 else 0.dp
        }

        val previewY by transition.animateDp(
            transitionSpec = { tween(durationMillis = 500, easing = FastOutSlowInEasing) },
            label = "Y"
        ) { captured ->
            if (captured) (maxHeight - 80.dp) / 2 else 0.dp
        }

        val previewCornerRadius by transition.animateDp(
            transitionSpec = { tween(durationMillis = 500, easing = FastOutSlowInEasing) },
            label = "CornerRadius"
        ) { captured ->
            if (captured) 40.dp else 0.dp
        }

        // Camera Preview (Animated)
        CameraPreview(
            modifier = Modifier
                .offset(x = previewX, y = previewY)
                .size(width = previewWidth, height = previewHeight)
                .clip(androidx.compose.foundation.shape.RoundedCornerShape(previewCornerRadius))
                .background(Color.Black),
            imageCapture = imageCapture
        )

        // Bottom Controls (Visible when NOT captured)
        androidx.compose.animation.AnimatedVisibility(
            visible = !isPhotoCaptured,
            exit = androidx.compose.animation.fadeOut(animationSpec = tween(200)),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(maxHeight - initialPreviewHeight)
            ) {


                if (isTakingPhoto) {
                    val buttonSize = 80.dp
                    val bottomBarHeight = maxHeight - initialPreviewHeight
                    val gapHeight = (bottomBarHeight - buttonSize) / 2
                    
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(gapHeight)
                            .align(Alignment.TopCenter),
                        contentAlignment = Alignment.Center
                    ) {
                        androidx.compose.material3.Surface(
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(50),
                            color = Color.DarkGray.copy(alpha = 0.9f),
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = "正在拍照中",
                                color = Color.White,
                                fontSize = 14.sp,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                            )
                        }
                    }
                }
                Button(
                    onClick = {
                        if (!isTakingPhoto) {
                            isTakingPhoto = true
                            val photoFile = File(
                                context.getExternalFilesDir(Environment.DIRECTORY_PICTURES),
                                SimpleDateFormat("yyyy-MM-dd-HH-mm-ss-SSS", Locale.US)
                                    .format(System.currentTimeMillis()) + ".jpg"
                            )

                            val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

                            imageCapture.takePicture(
                                outputOptions,
                                ContextCompat.getMainExecutor(context),
                                object : ImageCapture.OnImageSavedCallback {
                                    override fun onError(exc: ImageCaptureException) {
                                        android.util.Log.e("CameraContent", "Photo capture failed: ${exc.message}", exc)
                                        isTakingPhoto = false
                                    }

                                    override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                                        capturedPhotoPath = photoFile.absolutePath
                                        isPhotoCaptured = true
                                        isTakingPhoto = false
                                    }
                                }
                            )
                        }
                    },
                    modifier = Modifier
                        .size(80.dp)
                        .align(Alignment.Center)
                        .border(2.dp, Color.Black, CircleShape),
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White,
                        contentColor = Color.Black
                    ),
                    border = BorderStroke(2.dp, Color.Black)
                ) {
                    // Empty content
                }
            }
        }

        // Center Button (Visible when captured)
        androidx.compose.animation.AnimatedVisibility(
            visible = isPhotoCaptured,
            enter = fadeIn(animationSpec = tween(500, delayMillis = 200)) + scaleIn(
                initialScale = 0.8f,
                animationSpec = tween(500, delayMillis = 200, easing = FastOutSlowInEasing)
            ),
            modifier = Modifier.align(Alignment.Center)
        ) {
            val offsetX = remember { androidx.compose.animation.core.Animatable(0f) }
            val offsetY = remember { androidx.compose.animation.core.Animatable(0f) }
            // "NONE", "HORIZONTAL", "VERTICAL"
            var dragAxis by remember { mutableStateOf("NONE") }
            val scope = androidx.compose.runtime.rememberCoroutineScope()

            Box(modifier = Modifier.fillMaxSize()) {
                // Labels
                Text(
                    text = "正餐",
                    color = Color.Black,
                    fontSize = 20.sp,
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .padding(start = 32.dp)
                )

                Text(
                    text = "零食",
                    color = Color.Black,
                    fontSize = 20.sp,
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .padding(end = 32.dp)
                )

                // Draggable Button
                val currentTime = remember { mutableStateOf("") }
                
                // Feedback Text
                if (offsetX.value < -200f || offsetX.value > 200f) {
                    val isMainMeal = offsetX.value < -200f
                    val text = "在${currentTime.value}即将添加${if (isMainMeal) "正餐" else "零食"}"
                    
                    Text(
                        text = text,
                        color = Color.Black,
                        fontSize = 14.sp,
                        modifier = Modifier
                            .align(Alignment.Center)
                            .offset(y = (-100).dp) // Fixed position above button
                    )
                }

                Button(
                    onClick = { /* TODO: Next Step */ },
                    modifier = Modifier
                        .align(Alignment.Center)
                        .offset { androidx.compose.ui.unit.IntOffset(offsetX.value.roundToInt(), offsetY.value.roundToInt()) }
                        .pointerInput(maxWidth) {
                            val maxOffsetPx = with(density) { ((maxWidth - 80.dp) / 2).toPx() }
                            val maxOffsetYPx = with(density) { ((maxHeight - 80.dp) / 2).toPx() }
                            val resetThresholdPx = with(density) { 150.dp.toPx() }
                            
                            detectDragGestures(
                                onDragStart = {
                                    currentTime.value = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
                                    dragAxis = "NONE"
                                },
                                onDrag = { change, dragAmount ->
                                    change.consume()
                                    scope.launch {
                                        if (dragAxis == "NONE") {
                                            if (kotlin.math.abs(dragAmount.x) > kotlin.math.abs(dragAmount.y)) {
                                                dragAxis = "HORIZONTAL"
                                            } else {
                                                dragAxis = "VERTICAL"
                                            }
                                        }

                                        if (dragAxis == "HORIZONTAL") {
                                            val newOffsetX = (offsetX.value + dragAmount.x).coerceIn(-maxOffsetPx, maxOffsetPx)
                                            offsetX.snapTo(newOffsetX)
                                            // Ensure Y stays 0
                                            offsetY.snapTo(0f)
                                        } else if (dragAxis == "VERTICAL") {
                                            val newOffsetY = (offsetY.value + dragAmount.y).coerceIn(-maxOffsetYPx, maxOffsetYPx)
                                            offsetY.snapTo(newOffsetY)
                                            // Ensure X stays 0
                                            offsetX.snapTo(0f)
                                        }

                                        // Update time continuously if needed, but onDragStart is probably enough for "current time" of action
                                        // If we want exact time of drop, we can update it here or just use the one from start. 
                                        // Requirement says "at {timestamp} will add...", usually implies current time.
                                        currentTime.value = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
                                    }
                                },
                                onDragEnd = {
                                    scope.launch {
                                        // Check for vertical drag to reset
                                        if (kotlin.math.abs(offsetY.value) > resetThresholdPx) {
                                            // Delete the captured photo if it exists
                                            capturedPhotoPath?.let { path ->
                                                try {
                                                    val file = File(path)
                                                    if (file.exists()) {
                                                        file.delete()
                                                    }
                                                } catch (e: Exception) {
                                                    e.printStackTrace()
                                                }
                                            }
                                            isPhotoCaptured = false
                                            capturedPhotoPath = null
                                            offsetX.snapTo(0f)
                                            offsetY.snapTo(0f)
                                        } else {
                                            // Check if dragged far enough to the right (Snack)
                                            if (offsetX.value > 200f) {
                                                viewModel.addEvent("Snack", capturedPhotoPath)
                                                android.widget.Toast.makeText(context, "已记录零食", android.widget.Toast.LENGTH_SHORT).show()
                                                isPhotoCaptured = false 
                                                capturedPhotoPath = null
                                                offsetX.snapTo(0f)
                                                offsetY.snapTo(0f)
                                            }
                                            // Check if dragged far enough to the left (Main Meal)
                                            else if (offsetX.value < -200f) {
                                                viewModel.addEvent("Main Meal", capturedPhotoPath)
                                                android.widget.Toast.makeText(context, "已记录正餐", android.widget.Toast.LENGTH_SHORT).show()
                                                isPhotoCaptured = false
                                                capturedPhotoPath = null
                                                offsetX.snapTo(0f)
                                                offsetY.snapTo(0f)
                                            } else {
                                                // Snap back if no action taken
                                                launch { offsetX.animateTo(0f) }
                                                launch { offsetY.animateTo(0f) }
                                            }
                                        }
                                    }
                                },
                                onDragCancel = {
                                    scope.launch {
                                        launch { offsetX.animateTo(0f) }
                                        launch { offsetY.animateTo(0f) }
                                    }
                                }
                            )
                        }
                        .rotate(offsetX.value / 2f)
                        .size(80.dp)
                        .border(2.dp, Color.Black, CircleShape),
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White,
                        contentColor = Color.Black
                    ),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp),
                    border = BorderStroke(2.dp, Color.Black)
                ) {
                    if (capturedPhotoPath != null) {
                        val bitmap = remember(capturedPhotoPath) {
                            com.example.eat.utils.ImageUtils.loadRotatedBitmap(capturedPhotoPath!!)
                        }
                        if (bitmap != null) {
                            Image(
                                bitmap = bitmap.asImageBitmap(),
                                contentDescription = "Captured Photo",
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(CircleShape),
                                contentScale = ContentScale.Crop
                            )
                        }
                    }
                }
            }
        }
    }
}
