package com.ai.assistance.operit.ui.features.toolbox.screens.speechtotext

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import com.ai.assistance.operit.R
import com.ai.assistance.operit.api.speech.SpeechService
import com.ai.assistance.operit.api.speech.SpeechServiceFactory
import com.ai.assistance.operit.data.speech.GigaAMModelDownload
import com.ai.assistance.operit.data.speech.GigaAMModelFiles
import com.ai.assistance.operit.data.speech.SttModelStorage
import com.ai.assistance.operit.ui.features.settings.sections.formatMegabytes
import kotlinx.coroutines.launch

/** 语音识别演示屏幕 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpeechToTextScreen(navController: NavController) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val scrollState = rememberScrollState()
    
    // 权限状态
    var hasAudioPermission by remember { 
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context, 
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
        ) 
    }
    
    // 权限请求启动器
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasAudioPermission = isGranted
    }
    
    // 请求麦克风权限
    fun requestMicrophonePermission() {
        permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
    }
    
    // 权限未获取时，显示请求界面
    if (!hasAudioPermission) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.Mic,
                contentDescription = null,
                modifier = Modifier.size(72.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = stringResource(R.string.microphone_permission_required),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = stringResource(R.string.microphone_permission_description),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Button(
                onClick = { requestMicrophonePermission() },
                modifier = Modifier.fillMaxWidth(0.8f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Icon(
                    imageVector = Icons.Default.MicNone,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.request_microphone_permission))
            }
        }
        return
    }

    // 状态变量
    var recognizedText by remember { mutableStateOf("") }
    // The old default was zh-CN even for engines that do not offer it, so the
    // field showed a language the list did not contain. Default to the device
    // language when the engine speaks it, English otherwise.
    var selectedLanguage by remember {
        mutableStateOf(if (java.util.Locale.getDefault().language == "ru") "ru" else "en")
    }
    var error by remember { mutableStateOf<String?>(null) }
    var availableLanguages by remember { mutableStateOf<List<String>>(emptyList()) }
    var showModelConsent by remember { mutableStateOf(false) }
    // True when the local engine has no model to initialize with. Start stays
    // enabled in that case because it leads to the consent dialog, not to the
    // microphone.
    var modelMissing by remember { mutableStateOf(false) }
    
    // recognitionMode 是驱动服务实例创建的唯一状态源
    var recognitionMode by remember { mutableStateOf(SpeechServiceFactory.SpeechServiceType.LOCAL_GIGAAM) }

    var speechService by remember {
        mutableStateOf(SpeechServiceFactory.createSpeechService(context, recognitionMode))
    }

    LaunchedEffect(recognitionMode) {
        try {
            speechService.shutdown()
        } catch (_: Exception) {
        }
        speechService = SpeechServiceFactory.createSpeechService(context, recognitionMode)
    }

    DisposableEffect(Unit) {
        onDispose {
            try {
                speechService.shutdown()
            } catch (_: Exception) {
            }
        }
    }

    // 从服务中收集状态
    val isInitialized by speechService.isInitialized.collectAsState()
    val recognitionState by speechService.recognitionStateFlow.collectAsState()
    val isListening = recognitionState == SpeechService.RecognitionState.RECOGNIZING ||
                      recognitionState == SpeechService.RecognitionState.PROCESSING

    // 当服务实例改变时，执行初始化
    LaunchedEffect(speechService) {
        error = null // 清理旧的错误信息
        modelMissing =
            recognitionMode == SpeechServiceFactory.SpeechServiceType.LOCAL_GIGAAM &&
                SttModelStorage(context).isDownloaded() == null
        val success = speechService.initialize()
        if (success) {
            val supported = speechService.getSupportedLanguages()
            availableLanguages = supported
            // The field must never show a language the engine does not offer,
            // e.g. after switching engines. Snap to what is actually there.
            if (supported.isNotEmpty() && selectedLanguage !in supported) {
                selectedLanguage = supported.first()
            }
        } else if (!modelMissing) {
            // A missing local model is not an error banner: the consent dialog
            // covers it when the user presses start.
            error = context.getString(R.string.engine_init_failed, recognitionMode.name)
        }
    }
    
    // 当服务实例改变时，重新开始收集结果和错误
    LaunchedEffect(speechService) {
        launch {
            speechService.recognitionResultFlow.collect { result ->
                recognizedText = result.text
            }
        }
        launch {
            speechService.recognitionErrorFlow.collect { recognitionError ->
                if (recognitionError.message.isNotBlank()) {
                    error = context.getString(R.string.recognition_error, recognitionError.message)
                }
            }
        }
    }

    // 开始语音识别
    fun startRecognition() {
        error = null
        // The model downloads only with explicit consent. Declining stores no
        // flag; settings keeps offering the download until the files exist.
        if (recognitionMode == SpeechServiceFactory.SpeechServiceType.LOCAL_GIGAAM &&
            SttModelStorage(context).isDownloaded() == null
        ) {
            showModelConsent = true
            return
        }
        coroutineScope.launch {
            try {
                // 对于本地引擎，启用连续模式和部分结果
                val continuousMode = recognitionMode == SpeechServiceFactory.SpeechServiceType.LOCAL_GIGAAM
                val partialResults = recognitionMode == SpeechServiceFactory.SpeechServiceType.LOCAL_GIGAAM
                speechService.startRecognition(selectedLanguage, continuousMode, partialResults)
            } catch (e: Exception) {
                error = context.getString(R.string.start_recognition_error, e.message ?: "")
            }
        }
    }

    // 停止语音识别
    fun stopRecognition() {
        coroutineScope.launch {
            try {
                speechService.stopRecognition()
            } catch (e: Exception) {
                error = context.getString(R.string.stop_recognition_error, e.message ?: "")
            }
        }
    }
    
    // 切换识别引擎现在只改变状态，Compose框架会处理后续的重新创建和初始化
    fun switchRecognitionMode() {
        recognitionMode = when (recognitionMode) {
            SpeechServiceFactory.SpeechServiceType.LOCAL_GIGAAM ->
                SpeechServiceFactory.SpeechServiceType.OPENAI_STT
            SpeechServiceFactory.SpeechServiceType.OPENAI_STT ->
                SpeechServiceFactory.SpeechServiceType.DEEPGRAM_STT
            SpeechServiceFactory.SpeechServiceType.DEEPGRAM_STT ->
                SpeechServiceFactory.SpeechServiceType.LOCAL_GIGAAM
        }
    }

    // 获取当前引擎的显示名称
    fun getEngineName(mode: SpeechServiceFactory.SpeechServiceType): String {
        return when (mode) {
            SpeechServiceFactory.SpeechServiceType.LOCAL_GIGAAM -> context.getString(R.string.speech_services_stt_type_gigaam)
            SpeechServiceFactory.SpeechServiceType.OPENAI_STT -> context.getString(R.string.speech_services_stt_type_openai)
            SpeechServiceFactory.SpeechServiceType.DEEPGRAM_STT -> context.getString(R.string.speech_services_stt_type_deepgram)
        }
    }
    
    // 复制文本到剪贴板
    fun copyToClipboard(text: String) {
        if (text.isBlank()) {
            Toast.makeText(context, context.getString(R.string.no_text_to_copy), Toast.LENGTH_SHORT).show()
            return
        }
        
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("recognized_text", text)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(context, context.getString(R.string.copied_to_clipboard), Toast.LENGTH_SHORT).show()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(scrollState)
            .padding(16.dp)
    ) {
        // 标题
        Text(
            text = stringResource(R.string.speech_recognition_demo),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // 识别结果卡片
        Card(
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.recognition_result),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    
                    // 复制按钮
                    IconButton(
                        onClick = { copyToClipboard(recognizedText) },
                        enabled = recognizedText.isNotBlank()
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = stringResource(R.string.copy_text),
                            tint = if (recognizedText.isNotBlank()) 
                                MaterialTheme.colorScheme.primary 
                            else 
                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Surface(
                    modifier = Modifier.fillMaxWidth().heightIn(min = 120.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Box(
                        modifier = Modifier.padding(16.dp),
                        contentAlignment = Alignment.TopStart
                    ) {
                        if (recognizedText.isBlank()) {
                            Text(
                                stringResource(R.string.speech_recognition_result_placeholder),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                        } else {
                            Text(
                                recognizedText,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        // 语言选择卡片
        Card(
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                Text(
                    text = stringResource(R.string.recognition_settings),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(16.dp))

                // 语音识别引擎选择
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = stringResource(R.string.recognition_engine_colon),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.width(4.dp))
                    
                    Text(
                        text = getEngineName(recognitionMode),
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                    
                Spacer(modifier = Modifier.height(8.dp))
                    
                // 切换引擎按钮单独一行
                    Button(
                        onClick = { switchRecognitionMode() },
                        // Switching engines is the escape hatch from a dead one,
                        // so it must never require initialization.
                        enabled = !isListening,
                    modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(stringResource(R.string.switch_engine))
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 语言选择
                Text(
                    text = stringResource(R.string.recognition_language_colon),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(8.dp))
                
                // 语言选择下拉菜单
                var expanded by remember { mutableStateOf(false) }
                
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded }
                ) {
                    OutlinedTextField(
                        value = selectedLanguage,
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                        },
                        modifier = Modifier.menuAnchor().fillMaxWidth(),
                        colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors()
                    )
                    
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        availableLanguages.forEach { language ->
                            DropdownMenuItem(
                                text = { Text(language) },
                                onClick = {
                                    selectedLanguage = language
                                    expanded = false
                                }
                            )
                        }
                    }
                }
            }
        }

        // 操作按钮
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Button(
                onClick = { startRecognition() },
                modifier = Modifier.weight(1f).height(56.dp),
                enabled = (isInitialized || modelMissing) && !isListening,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Mic,
                    contentDescription = stringResource(R.string.start_recording),
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.start_recognition), style = MaterialTheme.typography.titleMedium)
            }

            Button(
                onClick = { stopRecognition() },
                modifier = Modifier.weight(1f).height(56.dp),
                enabled = isListening,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Stop,
                    contentDescription = stringResource(R.string.stop_recording),
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.stop_recognition), style = MaterialTheme.typography.titleMedium)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 状态指示器
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer
            ),
            shape = RoundedCornerShape(8.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = if (isInitialized) Icons.Default.CheckCircle else Icons.Default.Error,
                        contentDescription = null,
                        tint = if (isInitialized) Color(0xFF4CAF50) else MaterialTheme.colorScheme.error
                    )
                    Text(
                        text = if (isInitialized) 
                            stringResource(R.string.speech_engine_initialized) 
                        else 
                            stringResource(R.string.speech_engine_not_initialized),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = if (isListening) Icons.Default.Mic else Icons.Default.MicOff,
                        contentDescription = null,
                        tint = if (isListening) Color(0xFF2196F3) else MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    Text(
                        text = if (isListening) 
                            stringResource(R.string.recognizing) 
                        else 
                            stringResource(R.string.not_recognizing),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }

        // 错误提示
        if (error != null) {
            Spacer(modifier = Modifier.height(16.dp))
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.errorContainer,
                shape = RoundedCornerShape(8.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Error,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        text = error ?: "",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }

        // 使用说明
        Spacer(modifier = Modifier.height(24.dp))
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = stringResource(R.string.usage_instructions),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = stringResource(R.string.speech_usage_steps),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = stringResource(R.string.speech_engine_note),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }

    if (showModelConsent) {
        GigaAMConsentDialog(
            onDismiss = { showModelConsent = false },
            onReady = {
                showModelConsent = false
                modelMissing = false
                startRecognition()
            },
        )
    }
}

/**
 * Consent before the one-time model download, asked where the user pressed
 * start. Accepting downloads with progress; declining dismisses and stores
 * nothing, and settings keeps offering the download until the files exist.
 */
@Composable
private fun GigaAMConsentDialog(
    onDismiss: () -> Unit,
    onReady: () -> Unit,
) {
    val context = LocalContext.current
    val downloads = remember { GigaAMModelDownload.getInstance(context) }
    val storage = remember { SttModelStorage(context) }
    val state by downloads.state.collectAsState()

    LaunchedEffect(Unit) { downloads.refresh() }
    val ready = state is GigaAMModelDownload.State.Ready
    LaunchedEffect(ready) {
        if (ready) onReady()
    }
    if (ready) return

    val sizeLabel = remember { formatMegabytes(GigaAMModelFiles.TOTAL_BYTES) }
    val locationLabel = remember { storage.describe(storage.currentLocation()) }
    val downloading = state as? GigaAMModelDownload.State.Downloading
    val failed = state as? GigaAMModelDownload.State.Failed

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.gigaam_consent_title)) },
        text = {
            Column {
                if (downloading != null) {
                    val fraction =
                        if (downloading.totalBytes > 0) {
                            downloading.downloadedBytes.toFloat() / downloading.totalBytes
                        } else {
                            0f
                        }
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        stringResource(
                            R.string.stt_model_downloading,
                            (fraction * 100).toInt(),
                        ),
                    )
                } else {
                    Text(
                        stringResource(
                            R.string.gigaam_consent_text,
                            sizeLabel,
                            locationLabel,
                        ),
                    )
                    if (failed != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = stringResource(
                                R.string.gigaam_download_failed_summary,
                                failed.message,
                            ),
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                }
            }
        },
        confirmButton = {
            when {
                downloading != null -> {
                    TextButton(onClick = { downloads.cancel() }) {
                        Text(stringResource(R.string.floating_cancel))
                    }
                }
                failed != null -> {
                    TextButton(onClick = { downloads.download() }) {
                        Text(stringResource(R.string.action_retry))
                    }
                }
                else -> {
                    TextButton(onClick = { downloads.download() }) {
                        Text(stringResource(R.string.update_download))
                    }
                }
            }
        },
        dismissButton = {
            if (downloading == null) {
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.gigaam_not_now))
                }
            }
        },
    )
}