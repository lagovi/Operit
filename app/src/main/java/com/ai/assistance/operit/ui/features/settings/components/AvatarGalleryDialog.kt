package com.ai.assistance.operit.ui.features.settings.components

import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.ai.assistance.operit.data.avatar.AvatarAssetStore
import com.ai.assistance.operit.data.avatar.AvatarImageGenerationSettings
import com.ai.assistance.operit.data.avatar.AvatarImageGenerator
import com.ai.assistance.operit.data.avatar.AvatarPresetRepository
import com.ai.assistance.operit.data.repository.AvatarRepository
import com.ai.assistance.operit.core.avatar.impl.factory.AvatarModelFactoryImpl
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Avatar gallery used by every portrait entry point: character card, group
 * card, per-card user portrait and the global user portrait.
 *
 * Three tabs:
 *  - Presets: catalog from [AvatarPresetRepository]; choosing one downloads and
 *    verifies the model, installs it into [AvatarRepository], and reports its id.
 *  - Device: the existing system picker plus the already cropped result.
 *  - Create: draw, local composition, or generation through the user's
 *    configured image endpoint.
 *
 * All paths return a `file://` URI through [AvatarAssetStore] so nothing
 * downstream has to know where the picture came from.
 */
@Composable
fun AvatarGalleryDialog(
    title: String,
    currentAvatarUri: String?,
    onDismiss: () -> Unit,
    onPickImage: () -> Unit,
    onImageReady: (Uri) -> Unit,
    onReset: (() -> Unit)?,
    onPresetInstalled: ((modelId: String) -> Unit)? = null
) {
    var selectedTab by remember { mutableStateOf(0) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, fontWeight = FontWeight.Bold) },
        text = {
            Column(modifier = Modifier.fillMaxWidth().heightIn(max = 460.dp)) {
                TabRow(selectedTabIndex = selectedTab) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text("Presets", maxLines = 1, overflow = TextOverflow.Ellipsis) }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text("Device", maxLines = 1, overflow = TextOverflow.Ellipsis) }
                    )
                    Tab(
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 },
                        text = { Text("Create", maxLines = 1, overflow = TextOverflow.Ellipsis) }
                    )
                    Tab(
                        selected = selectedTab == 3,
                        onClick = { selectedTab = 3 },
                        text = { Text("Backend", maxLines = 1, overflow = TextOverflow.Ellipsis) }
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                when (selectedTab) {
                    0 -> PresetTab(
                        currentAvatarUri = currentAvatarUri,
                        onPresetInstalled = onPresetInstalled
                    )
                    1 -> DeviceTab(
                        currentAvatarUri = currentAvatarUri,
                        onPickImage = onPickImage,
                        onReset = onReset
                    )
                    2 -> CreateTab(
                        currentAvatarUri = currentAvatarUri,
                        onImageReady = onImageReady
                    )
                    else -> GenerationEndpointSettings(Modifier.weight(1f))
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        }
    )
}

@Composable
private fun PresetTab(
    currentAvatarUri: String?,
    onPresetInstalled: ((String) -> Unit)?
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var catalog by remember { mutableStateOf<AvatarPresetRepository.Catalog?>(null) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var downloadingId by remember { mutableStateOf<String?>(null) }
    var progress by remember { mutableStateOf(0f) }

    LaunchedEffect(Unit) {
        loading = true
        AvatarPresetRepository.fetchCatalog()
            .onSuccess { catalog = it; error = null }
            .onFailure { error = it.message ?: "Presets could not be loaded" }
        loading = false
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            "Image presets load from the fork repository. 3D model presets install into the assistant avatar list.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(8.dp))
        if (downloadingId != null) {
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth()
            )
        }
        when {
            loading -> Box(
                modifier = Modifier.fillMaxWidth().height(160.dp),
                contentAlignment = Alignment.Center
            ) { CircularProgressIndicator() }
            error != null -> Column(modifier = Modifier.fillMaxWidth()) {
                Text(error!!, color = MaterialTheme.colorScheme.error)
                TextButton(onClick = {
                    scope.launch {
                        loading = true
                        AvatarPresetRepository.fetchCatalog()
                            .onSuccess { catalog = it; error = null }
                            .onFailure { error = it.message }
                        loading = false
                    }
                }) { Text("Retry") }
            }
            else -> LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(catalog?.presets.orEmpty(), key = { it.id }) { preset ->
                    PresetRow(
                        preset = preset,
                        busy = downloadingId == preset.id,
                        onSelect = {
                            scope.launch {
                                downloadingId = preset.id
                                progress = 0f
                                AvatarPresetRepository.downloadGltfPreset(
                                    context = context,
                                    catalogUrl = AvatarPresetRepository.DEFAULT_CATALOG_URL,
                                    preset = preset,
                                    onProgress = { done, total ->
                                        progress = if (total > 0) done.toFloat() / total else 0f
                                    }
                                ).onSuccess { model ->
                                    val repository = AvatarRepository.getInstance(
                                        context,
                                        AvatarModelFactoryImpl()
                                    )
                                    val modelId = repository.installGltfPreset(
                                        presetId = preset.id,
                                        name = preset.name,
                                        sourceFile = File(model.basePath, model.modelFile),
                                        modelFileName = model.modelFile
                                    )
                                    downloadingId = null
                                    onPresetInstalled?.invoke(modelId ?: preset.id)
                                    if (modelId != null) {
                                        File(model.basePath, model.modelFile)
                                            .parentFile
                                            ?.deleteRecursively()
                                    }
                                }.onFailure { error = it.message }
                                downloadingId = null
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun PresetRow(
    preset: AvatarPresetRepository.Preset,
    busy: Boolean,
    onSelect: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth().clickable(enabled = !busy, onClick = onSelect),
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Category, contentDescription = null)
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(preset.name, fontWeight = FontWeight.SemiBold)
                Text(
                    preset.summary,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    buildString {
                        append(preset.type.uppercase())
                        if (preset.sizeBytes > 0) {
                            append(" · ")
                            append(formatBytes(preset.sizeBytes))
                        }
                        if (preset.animations.isNotEmpty()) {
                            append(" · ")
                            append(preset.animations.joinToString(", "))
                        }
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (preset.license.isNotBlank()) {
                    Text(
                        preset.license,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Spacer(modifier = Modifier.width(8.dp))
            if (busy) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
            } else {
                Icon(Icons.Default.CloudDownload, contentDescription = "Download preset")
            }
        }
    }
}

@Composable
private fun DeviceTab(
    currentAvatarUri: String?,
    onPickImage: () -> Unit,
    onReset: (() -> Unit)?
) {
    Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        AvatarPreview(uri = currentAvatarUri)
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            "Choose a picture from this device. It is cropped to a square and used immediately.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(12.dp))
        Button(onClick = onPickImage) {
            Icon(Icons.Default.PhotoLibrary, contentDescription = null)
            Spacer(modifier = Modifier.width(6.dp))
            Text("Choose image")
        }
        if (onReset != null) {
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedButton(onClick = onReset) {
                Icon(Icons.Default.Refresh, contentDescription = null)
                Spacer(modifier = Modifier.width(6.dp))
                Text("Restore default")
            }
        }
    }
}

@Composable
private fun AvatarPreview(uri: String?) {
    Box(
        modifier = Modifier
            .size(96.dp)
            .border(2.dp, MaterialTheme.colorScheme.primary, CircleShape)
            .padding(4.dp),
        contentAlignment = Alignment.Center
    ) {
        if (!uri.isNullOrBlank()) {
            AsyncImage(
                model = Uri.parse(uri),
                contentDescription = "Avatar preview",
                modifier = Modifier.fillMaxSize().background(Color.Gray.copy(alpha = 0.2f), CircleShape),
                contentScale = ContentScale.Crop
            )
        } else {
            Icon(
                Icons.Default.PhotoLibrary,
                contentDescription = null,
                modifier = Modifier.size(32.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private enum class CreateMode {
    DRAW,
    COMPOSE,
    GENERATE
}

@Composable
private fun CreateTab(
    currentAvatarUri: String?,
    onImageReady: (Uri) -> Unit
) {
    var mode by remember { mutableStateOf(CreateMode.COMPOSE) }

    Column(modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState())) {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            ModeChip("Compose", Icons.Default.Palette, mode == CreateMode.COMPOSE) { mode = CreateMode.COMPOSE }
            ModeChip("Draw", Icons.Default.Brush, mode == CreateMode.DRAW) { mode = CreateMode.DRAW }
            ModeChip("Generate", Icons.Default.AutoAwesome, mode == CreateMode.GENERATE) { mode = CreateMode.GENERATE }
        }
        Spacer(modifier = Modifier.height(12.dp))
        when (mode) {
            CreateMode.DRAW -> DrawPad(onImageReady = onImageReady, onDone = {})
            CreateMode.COMPOSE -> ComposePad(onImageReady = onImageReady, onDone = {})
            CreateMode.GENERATE -> GeneratePanel(onImageReady = onImageReady)
        }
        Spacer(modifier = Modifier.height(8.dp))
        AvatarPreview(uri = currentAvatarUri)
    }
}

@Composable
private fun ModeChip(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    selected: Boolean,
    onClick: () -> Unit
) {
    val color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp), tint = color)
            Spacer(modifier = Modifier.width(4.dp))
            Text(label, style = MaterialTheme.typography.labelMedium, color = color)
        }
    }
}

@Composable
private fun DrawPad(
    onImageReady: (Uri) -> Unit,
    onDone: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val strokes = remember { mutableStateListOf<DrawStroke>() }
    var activeIndex by remember { mutableStateOf(-1) }
    var color by remember { mutableStateOf(Color(0xFF4C6FFF)) }
    val palette = listOf(
        Color(0xFF4C6FFF), Color(0xFFE5484D), Color(0xFF30A46C),
        Color(0xFFF5A623), Color(0xFF8E4EC6), Color(0xFF111111), Color.White
    )

    Column {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .heightIn(max = 240.dp)
                .background(Color.White, RoundedCornerShape(12.dp))
                .pointerInput(color) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            strokes.add(DrawStroke(color, mutableListOf(offset)))
                            activeIndex = strokes.lastIndex
                        },
                        onDrag = { change, _ ->
                            change.consume()
                            if (activeIndex in strokes.indices) {
                                val stroke = strokes[activeIndex]
                                strokes[activeIndex] =
                                    stroke.copy(points = stroke.points + change.position)
                            }
                        },
                        onDragEnd = { activeIndex = -1 },
                        onDragCancel = { activeIndex = -1 }
                    )
                }
                .pointerInput(color) {
                    detectTapGestures { offset ->
                        strokes.add(DrawStroke(color, mutableListOf(offset, offset)))
                    }
                }
        ) {
            strokes.forEach { stroke ->
                if (stroke.points.size >= 2) {
                    for (i in 1 until stroke.points.size) {
                        drawLine(
                            color = stroke.color,
                            start = stroke.points[i - 1],
                            end = stroke.points[i],
                            strokeWidth = 8.dp.toPx(),
                            cap = androidx.compose.ui.graphics.StrokeCap.Round
                        )
                    }
                } else if (stroke.points.size == 1) {
                    drawCircle(stroke.color, 4.dp.toPx(), stroke.points[0])
                }
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            palette.forEach { swatch ->
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .background(swatch, CircleShape)
                        .border(
                            if (swatch == color) 2.dp else 1.dp,
                            MaterialTheme.colorScheme.outline,
                            CircleShape
                        )
                        .clickable { color = swatch }
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            OutlinedButton(onClick = { if (strokes.isNotEmpty()) strokes.removeAt(strokes.lastIndex) }) {
                Icon(Icons.Default.Undo, contentDescription = null)
                Spacer(modifier = Modifier.width(4.dp))
                Text("Undo")
            }
            OutlinedButton(onClick = { strokes.clear() }) {
                Icon(Icons.Default.Delete, contentDescription = null)
                Spacer(modifier = Modifier.width(4.dp))
                Text("Clear")
            }
            Button(
                enabled = strokes.isNotEmpty(),
                onClick = {
                    scope.launch {
                        val bitmap = renderDrawPad(strokes, 512)
                        AvatarAssetStore.saveBitmap(context, bitmap, "drawn_avatar")?.let { uri ->
                            onImageReady(uri)
                            onDone()
                        }
                    }
                }
            ) { Text("Use drawing") }
        }
    }
}

private data class DrawStroke(val color: Color, val points: List<Offset>)

private suspend fun renderDrawPad(strokes: List<DrawStroke>, size: Int): Bitmap =
    withContext(Dispatchers.Default) {
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(bitmap)
        canvas.drawColor(android.graphics.Color.WHITE)
        val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
            style = android.graphics.Paint.Style.STROKE
            strokeCap = android.graphics.Paint.Cap.ROUND
            strokeJoin = android.graphics.Paint.Join.ROUND
            strokeWidth = size * 8f / 512f
        }
        strokes.forEach { stroke ->
            paint.color = stroke.color.toArgbInt()
            if (stroke.points.size >= 2) {
                val path = android.graphics.Path()
                path.moveTo(stroke.points[0].x, stroke.points[0].y)
                for (i in 1 until stroke.points.size) {
                    path.lineTo(stroke.points[i].x, stroke.points[i].y)
                }
                canvas.drawPath(path, paint)
            } else if (stroke.points.size == 1) {
                paint.style = android.graphics.Paint.Style.FILL
                canvas.drawCircle(stroke.points[0].x, stroke.points[0].y, paint.strokeWidth / 2, paint)
                paint.style = android.graphics.Paint.Style.STROKE
            }
        }
        bitmap
    }

private fun Color.toArgbInt(): Int = (alpha * 255).toInt() shl 24 or
    (red * 255).toInt() shl 16 or (green * 255).toInt() shl 8 or (blue * 255).toInt()

@Composable
private fun ComposePad(
    onImageReady: (Uri) -> Unit,
    onDone: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val backgrounds = listOf(
        Color(0xFF2B2D42), Color(0xFF1B4332), Color(0xFF5F0F40),
        Color(0xFF23395B), Color(0xFF3D348B), Color(0xFF6B4226)
    )
    val accents = listOf(
        Color(0xFFFFB703), Color(0xFF8ECAE6), Color(0xFF90BE6D),
        Color(0xFFF28482), Color(0xFFC77DFF), Color(0xFFFFFFFF)
    )
    var background by remember { mutableStateOf(backgrounds.first()) }
    var accent by remember { mutableStateOf(accents.first()) }
    var shape by remember { mutableStateOf(ComposeShape.FIGURE) }

    Column {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .background(background, RoundedCornerShape(12.dp))
        ) {
            drawComposeShape(shape, accent, size.width, size.height)
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text("Shape", style = MaterialTheme.typography.labelMedium)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            ComposeShape.values().forEach { option ->
                ModeChip(option.label, Icons.Default.Category, shape == option) { shape = option }
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text("Background", style = MaterialTheme.typography.labelMedium)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            backgrounds.forEach { option ->
                Swatch(option, background == option) { background = option }
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text("Accent", style = MaterialTheme.typography.labelMedium)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            accents.forEach { option ->
                Swatch(option, accent == option) { accent = option }
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
        Button(onClick = {
            scope.launch {
                val bitmap = renderCompose(shape, background, accent, 512)
                AvatarAssetStore.saveBitmap(context, bitmap, "composed_avatar")?.let { uri ->
                    onImageReady(uri)
                    onDone()
                }
            }
        }) { Text("Use composition") }
    }
}

private enum class ComposeShape(val label: String) {
    FIGURE("Figure"),
    BUST("Bust"),
    ORB("Orb"),
    BLOB("Blob")
}

@Composable
private fun Swatch(color: Color, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(24.dp)
            .background(color, CircleShape)
            .border(
                if (selected) 2.dp else 1.dp,
                if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                CircleShape
            )
            .clickable(onClick = onClick)
    )
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawComposeShape(
    shape: ComposeShape,
    accent: Color,
    width: Float,
    height: Float
) {
    val cx = width / 2f
    val cy = height / 2f
    val r = minOf(width, height) * 0.22f
    when (shape) {
        ComposeShape.ORB -> {
            drawCircle(accent, r, Offset(cx, cy))
            drawCircle(accent.copy(alpha = 0.35f), r * 1.35f, Offset(cx, cy), style = Stroke(r * 0.06f))
        }
        ComposeShape.FIGURE -> {
            drawCircle(accent, r * 0.7f, Offset(cx, cy - r * 1.1f))
            drawRoundRect(
                color = accent,
                topLeft = Offset(cx - r * 0.85f, cy - r * 0.2f),
                size = androidx.compose.ui.geometry.Size(r * 1.7f, r * 1.9f),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(r * 0.5f)
            )
        }
        ComposeShape.BUST -> {
            drawCircle(accent, r * 0.85f, Offset(cx, cy - r * 0.5f))
            drawArc(
                color = accent,
                startAngle = 180f,
                sweepAngle = 180f,
                useCenter = true,
                topLeft = Offset(cx - r * 1.2f, cy + r * 0.4f),
                size = androidx.compose.ui.geometry.Size(r * 2.4f, r * 2.4f)
            )
        }
        ComposeShape.BLOB -> {
            drawCircle(accent, r * 0.9f, Offset(cx - r * 0.4f, cy - r * 0.2f))
            drawCircle(accent, r * 0.7f, Offset(cx + r * 0.55f, cy + r * 0.35f))
            drawCircle(accent.copy(alpha = 0.75f), r * 0.55f, Offset(cx + r * 0.1f, cy + r * 0.7f))
        }
    }
}

private suspend fun renderCompose(
    shape: ComposeShape,
    background: Color,
    accent: Color,
    size: Int
): Bitmap = withContext(Dispatchers.Default) {
    val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = android.graphics.Canvas(bitmap)
    canvas.drawColor(background.toArgbInt())
    val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
        color = accent.toArgbInt()
    }
    val cx = size / 2f
    val cy = size / 2f
    val r = size * 0.22f
    when (shape) {
        ComposeShape.ORB -> {
            canvas.drawCircle(cx, cy, r, paint)
            paint.style = android.graphics.Paint.Style.STROKE
            paint.strokeWidth = r * 0.06f
            paint.color = (accent.copy(alpha = 0.35f)).toArgbInt()
            canvas.drawCircle(cx, cy, r * 1.35f, paint)
        }
        ComposeShape.FIGURE -> {
            canvas.drawCircle(cx, cy - r * 1.1f, r * 0.7f, paint)
            canvas.drawRoundRect(
                cx - r * 0.85f, cy - r * 0.2f, cx + r * 0.85f, cy + r * 1.7f,
                r * 0.5f, r * 0.5f, paint
            )
        }
        ComposeShape.BUST -> {
            canvas.drawCircle(cx, cy - r * 0.5f, r * 0.85f, paint)
            canvas.drawArc(
                cx - r * 1.2f, cy + r * 0.4f, cx + r * 1.2f, cy + r * 2.8f,
                180f, 180f, true, paint
            )
        }
        ComposeShape.BLOB -> {
            canvas.drawCircle(cx - r * 0.4f, cy - r * 0.2f, r * 0.9f, paint)
            canvas.drawCircle(cx + r * 0.55f, cy + r * 0.35f, r * 0.7f, paint)
            paint.color = accent.copy(alpha = 0.75f).toArgbInt()
            canvas.drawCircle(cx + r * 0.1f, cy + r * 0.7f, r * 0.55f, paint)
        }
    }
    bitmap
}

@Composable
private fun GeneratePanel(onImageReady: (Uri) -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var prompt by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var styles by remember { mutableStateOf(setOf("portrait")) }

    Column {
        Text(
            "Describe the avatar. Generation uses the image backend configured on the Backend tab.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = prompt,
            onValueChange = { prompt = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Description") },
            minLines = 2,
            maxLines = 4
        )
        Spacer(modifier = Modifier.height(8.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf("portrait", "anime", "realistic", "ink", "pixel", "flat").forEach { style ->
                ModeChip(style, Icons.Default.AutoAwesome, styles.contains(style)) {
                    styles = if (styles.contains(style)) styles - style else styles + style
                }
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
        Button(
            enabled = prompt.isNotBlank() && !busy,
            onClick = {
                scope.launch {
                    busy = true
                    error = null
                    val result = AvatarImageGenerator.generate(
                        context = context,
                        prompt = buildString {
                            append(prompt.trim())
                            if (styles.isNotEmpty()) {
                                append(". Style: ")
                                append(styles.joinToString(", "))
                            }
                            append(". Square avatar, centered composition, clean background.")
                        }
                    )
                    result.onSuccess { uri -> onImageReady(uri) }
                        .onFailure { error = it.message ?: "Generation failed" }
                    busy = false
                }
            }
        ) {
            if (busy) {
                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
            } else {
                Icon(Icons.Default.AutoAwesome, contentDescription = null)
            }
            Spacer(modifier = Modifier.width(6.dp))
            Text(if (busy) "Generating…" else "Generate avatar")
        }
        error?.let {
            Spacer(modifier = Modifier.height(8.dp))
            Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }
    }
}

private fun formatBytes(bytes: Long): String {
    return when {
        bytes >= 1024 * 1024 -> "%.1f MB".format(bytes / (1024.0 * 1024.0))
        bytes >= 1024 -> "%.0f KB".format(bytes / 1024.0)
        else -> "$bytes B"
    }
}

@Composable
private fun GenerationEndpointSettings(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var endpoint by remember { mutableStateOf("") }
    var apiKey by remember { mutableStateOf("") }
    var model by remember { mutableStateOf("") }
    var loaded by remember { mutableStateOf(false) }
    var saved by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        val snapshot = AvatarImageGenerationSettings.current(context)
        endpoint = snapshot.endpoint
        apiKey = snapshot.apiKey
        model = snapshot.model
        loaded = true
    }

    if (!loaded) return

    Column(modifier = modifier.fillMaxWidth().verticalScroll(rememberScrollState())) {
        Text(
            "Image backend for Create → Generate. Any OpenAI-compatible images endpoint works, including a Colab notebook or an instance of Bonsai Image 4B.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = endpoint,
            onValueChange = { endpoint = it; saved = false },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Endpoint") },
            placeholder = { Text("https://host/v1/images/generations") },
            singleLine = true
        )
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = apiKey,
            onValueChange = { apiKey = it; saved = false },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("API key (optional)") },
            singleLine = true
        )
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = model,
            onValueChange = { model = it; saved = false },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Model name (optional)") },
            singleLine = true
        )
        Spacer(modifier = Modifier.height(12.dp))
        Button(onClick = {
            scope.launch {
                AvatarImageGenerationSettings.save(context, endpoint, apiKey, model)
                saved = true
            }
        }) { Text("Save backend") }
        if (saved) {
            Spacer(modifier = Modifier.height(6.dp))
            Text("Saved", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodySmall)
        }
    }
}

/**
 * Preset-only gallery for the Assistant avatar list. It installs a verified
 * model through [AvatarRepository.installGltfPreset] and reports the resulting
 * avatar config id so the caller can switch to it.
 */
@Composable
fun AvatarPresetGalleryDialog(
    onDismiss: () -> Unit,
    onInstalled: (modelId: String) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Avatar presets", fontWeight = FontWeight.Bold) },
        text = {
            Column(modifier = Modifier.fillMaxWidth().heightIn(max = 520.dp)) {
                PresetTab(
                    currentAvatarUri = null,
                    onPresetInstalled = { modelId -> onInstalled(modelId) }
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        }
    )
}
