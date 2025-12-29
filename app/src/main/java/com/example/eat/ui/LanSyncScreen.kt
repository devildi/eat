package com.example.eat.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester

enum class SyncMode {
    NONE, SENDER, RECEIVER
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LanSyncScreen(
    onBack: () -> Unit,
    viewModel: MainViewModel
) {
    var mode by remember { mutableStateOf(SyncMode.NONE) }
    val serverIp by viewModel.serverIp.collectAsState()
    val syncStatus by viewModel.syncStatus.collectAsState()
    val isSyncing by viewModel.isSyncing.collectAsState()
    val backupSummary by viewModel.backupSummary.collectAsState()
    var targetIp by remember { mutableStateOf("") }
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(mode) {
        if (mode == SyncMode.RECEIVER) {
            // Request focus when entering Receiver mode
             kotlinx.coroutines.delay(300) // Small delay to allow UI to build
             focusRequester.requestFocus()
             keyboardController?.show()
        }
    }

    // Handle back press to reset mode first
    fun handleBack() {
        if (mode != SyncMode.NONE) {
            mode = SyncMode.NONE
            viewModel.stopLanServer()
        } else {
            onBack()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(
                    when(mode) {
                        SyncMode.NONE -> "局域网同步"
                        SyncMode.SENDER -> "发送方"
                        SyncMode.RECEIVER -> "接收方"
                    }
                ) },
                navigationIcon = {
                    IconButton(onClick = { handleBack() }) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            when (mode) {
                SyncMode.NONE -> {
                    Button(
                        onClick = { 
                            mode = SyncMode.SENDER 
                            viewModel.startLanServer()
                        },
                        modifier = Modifier.fillMaxWidth().height(60.dp)
                    ) {
                        Text("我是发送方")
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(
                        onClick = { mode = SyncMode.RECEIVER },
                        modifier = Modifier.fillMaxWidth().height(60.dp)
                    ) {
                        Text("我是接收方")
                    }
                }
                SyncMode.SENDER -> {
                    
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("打包文件明细:", style = MaterialTheme.typography.titleMedium)
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            val summary = backupSummary
                            if (summary != null) {
                                val piData = mapOf(
                                    "文章" to summary.articleCount,
                                    "图片" to summary.imageCount,
                                    "健康数据" to summary.healthDataCount,
                                    "记录" to summary.eventCount
                                ).filter { it.value > 0 }

                                if (piData.isNotEmpty()) {
                                    PieChart(
                                        data = piData,
                                        colors = listOf(
                                            MaterialTheme.colorScheme.primary,
                                            MaterialTheme.colorScheme.secondary,
                                            MaterialTheme.colorScheme.tertiary,
                                            MaterialTheme.colorScheme.error,
                                            MaterialTheme.colorScheme.primaryContainer
                                        )
                                    )
                                } else {
                                    Text("无数据", style = MaterialTheme.typography.bodyMedium)
                                }
                            } else {
                                // Loading State
                                Box(
                                    modifier = Modifier.fillMaxWidth().height(160.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("正在打包数据...", style = MaterialTheme.typography.bodyLarge)
                                        Spacer(modifier = Modifier.width(16.dp))
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(20.dp),
                                            strokeWidth = 2.dp
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Text("服务状态:", style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = syncStatus,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.primary,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(32.dp))
                    
                    if (serverIp.isNotEmpty()) {
                        Text("请在接收方输入此 IP:", style = MaterialTheme.typography.labelLarge)
                        Text(serverIp.replace(":8080", ""), style = MaterialTheme.typography.displaySmall)
                    }
                    
                    Spacer(modifier = Modifier.height(32.dp))
                    Text("保持此页面打开，直到接收方完成同步。", style = MaterialTheme.typography.bodySmall)
                }
                SyncMode.RECEIVER -> {
                    Text("请输入发送方的 IP 地址:", style = MaterialTheme.typography.titleMedium)
                    OutlinedTextField(
                        value = targetIp,
                        onValueChange = { targetIp = it },
                        label = { Text("例如 192.168.1.5") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(focusRequester)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = { 
                            keyboardController?.hide()
                            focusManager.clearFocus()
                            viewModel.importLanData(targetIp) 
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = targetIp.isNotBlank() && !isSyncing
                    ) {
                        if (isSyncing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text("连接并同步")
                        }
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                    Text("状态日志:", style = MaterialTheme.typography.titleSmall)
                    Text(syncStatus)
                }
            }
        }
    }
}



@Composable
fun PieChart(
    data: Map<String, Int>,
    colors: List<Color>,
    modifier: Modifier = Modifier
) {
    val total = data.values.sum()
    val animatedProgress = remember { Animatable(0f) }
    
    LaunchedEffect(data) {
        animatedProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 800)
        )
    }

    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        // Chart
        Canvas(
            modifier = Modifier.size(160.dp)
        ) {
            var startAngle = -90f
            val keys = data.keys.toList()
            
            keys.forEachIndexed { index, key ->
                val value = data[key] ?: 0
                val sweepAngle = (value.toFloat() / total) * 360f * animatedProgress.value
                val color = colors.getOrElse(index) { Color.Gray }
                
                drawArc(
                    color = color,
                    startAngle = startAngle,
                    sweepAngle = sweepAngle,
                    useCenter = true
                )
                startAngle += sweepAngle
            }
        }
        
        // Legend
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            data.keys.forEachIndexed { index, key ->
                val count = data[key] ?: 0
                val color = colors.getOrElse(index) { Color.Gray }
                val percentage = (count.toFloat() / total * 100).toInt()
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .background(color, CircleShape)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "$key: $count ($percentage%)",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}
