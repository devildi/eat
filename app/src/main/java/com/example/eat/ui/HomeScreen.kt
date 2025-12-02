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
import androidx.compose.foundation.gestures.detectDragGestures
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material3.Button
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
import androidx.compose.ui.input.pointer.pointerInput
import kotlin.math.roundToInt
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(viewModel: MainViewModel = viewModel()) {
    var selectedItem by remember { mutableIntStateOf(1) } // Default to Camera (index 1)
    val items = listOf("首页", "相机", "我的")
    val icons = listOf(
        Icons.Filled.Home,
        Icons.Filled.PhotoCamera,
        Icons.Filled.Person
    )
    
    // Date Picker State
    var showDatePicker by remember { mutableStateOf(false) }
    var selectedDate by remember { mutableStateOf(System.currentTimeMillis()) }
    val datePickerState = rememberDatePickerState(initialSelectedDateMillis = selectedDate)
    
    // Navigation State
    var showWeightChart by remember { mutableStateOf(false) }
    var showBloodPressureChart by remember { mutableStateOf(false) }

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let {
                        selectedDate = it
                    }
                    showDatePicker = false
                }) {
                    Text("OK")
                }
            },
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
    
    // Show BloodPressureChartScreen if navigated
    if (showBloodPressureChart) {
        BloodPressureChartScreen(onBack = { showBloodPressureChart = false })
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    val titleText = when (selectedItem) {
                        0 -> "每日听力"
                        1 -> "吃了什么"
                        2 -> SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(selectedDate))
                        else -> "吃了什么"
                    }
                    Text(
                        text = titleText,
                        modifier = if (selectedItem == 2) {
                            Modifier.pointerInput(Unit) {
                                detectTapGestures(onTap = { showDatePicker = true })
                            }
                        } else {
                            Modifier
                        }
                    )
                },
                actions = {
                    if (selectedItem == 2) {
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
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White,
                    titleContentColor = Color.Black
                )
            )
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
                0 -> HomeContent()
                1 -> CameraContent(viewModel)
                2 -> ProfileContent(viewModel, selectedDate)
            }
        }
    }
}

@Composable
fun CameraContent(viewModel: MainViewModel) {
    var isPhotoCaptured by remember { mutableStateOf(false) }
    var capturedPhotoPath by remember { mutableStateOf<String?>(null) }
    val imageCapture = remember { ImageCapture.Builder().build() }
    val density = androidx.compose.ui.platform.LocalDensity.current
    val context = androidx.compose.ui.platform.LocalContext.current

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
                Button(
                    onClick = {
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
                                }

                                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                                    capturedPhotoPath = photoFile.absolutePath
                                    isPhotoCaptured = true
                                }
                            }
                        )
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
                                            isPhotoCaptured = false
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
