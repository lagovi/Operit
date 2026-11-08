package com.ai.assistance.operit.core.avatar.impl.gltf.view

import android.content.Context
import android.net.Uri
import android.graphics.PixelFormat
import android.util.Base64
import android.util.AttributeSet
import android.view.Choreographer
import android.view.SurfaceView
import com.ai.assistance.operit.util.AppLogger
import com.google.android.filament.EntityManager
import com.google.android.filament.IndirectLight
import com.google.android.filament.LightManager
import com.google.android.filament.Renderer
import com.google.android.filament.View
import com.google.android.filament.android.UiHelper
import com.google.android.filament.gltfio.FilamentAsset
import com.google.android.filament.utils.Manipulator
import com.google.android.filament.utils.ModelViewer
import com.google.android.filament.utils.Utils
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.FileChannel
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

class GltfSurfaceView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : SurfaceView(context, attrs), Choreographer.FrameCallback {

    private data class PreparedGltfModel(
        val modelFile: File,
        val mimeHints: Map<String, String>
    )

    private data class TextureDiagnostics(
        val imageCount: Int,
        val textureCount: Int,
        val materialCount: Int,
        val materialsWithTextureRefs: Int,
        val externalImageFileCount: Int,
        val sampleExternalImageFiles: List<String>,
        val unsupportedExternalImageFileCount: Int,
        val sampleUnsupportedExternalImageFiles: List<String>
    )

    companion object {
        private const val TAG = "GltfSurfaceView"
        private const val DEFAULT_CAMERA_PITCH = 8.0f
        private const val DEFAULT_CAMERA_YAW = 0.0f
        // 1.0 means "exactly the distance at which the model fits"; see updateFittedCameraDistance.
        private const val DEFAULT_CAMERA_DISTANCE_SCALE = 1.0f
        private const val MIN_CAMERA_DISTANCE_SCALE = 0.0f
        private const val MAX_CAMERA_DISTANCE_SCALE = 10.0f
        private const val MIN_CAMERA_TARGET_HEIGHT = -2.0f
        private const val MAX_CAMERA_TARGET_HEIGHT = 2.0f
        private const val BASE_CAMERA_DISTANCE = 0.5
        private const val CAMERA_DISTANCE_EPSILON = 1e-6
        // Vertical field of view used for the auto-fit calculation. Filament's default camera
        // is 45 degrees; keep the two in sync if the projection is ever changed explicitly.
        private const val CAMERA_VERTICAL_FOV_DEGREES = 45.0
        // Leave a small margin so the silhouette does not touch the frame edges.
        private const val CAMERA_FIT_MARGIN = 1.12
        // Share of the preview height the drawn figure should occupy after silhouette framing.
        // Measured by eye on Xbot and RobotExpressive: at 0.80 the figure nearly touches the frame,
        // 0.70 leaves a visible band above and below, which is what the user asked for.
        private const val SILHOUETTE_TARGET_FILL = 0.70
        // Alpha above which a pixel counts as part of the model. The surface is TRANSLUCENT, so the
        // empty background is fully transparent and only antialiased edges sit in between.
        private const val SILHOUETTE_ALPHA_THRESHOLD = 16
        // A row must carry this many opaque pixels to count, which rejects stray specks from
        // bloom or from a shadow that leaked past the figure.
        private const val SILHOUETTE_MIN_PIXELS_PER_ROW = 2
        // Frames to wait for a model to become visible before giving up on measuring it.
        private const val SILHOUETTE_MAX_ATTEMPTS = 30
        // How many measure-and-correct steps the framing may take once the model is visible. Models
        // whose authored bounds are far off need a handful of steps; the budget stops a model that
        // cannot reach the target from looping forever.
        private const val SILHOUETTE_SETTLE_ITERATIONS = 10
        // The fill is considered reached when it is this close (absolute share of the viewport).
        private const val SILHOUETTE_FILL_TOLERANCE = 0.02
        // The figure is considered centred when its centre is this close to the viewport centre.
        private const val SILHOUETTE_CENTRE_TOLERANCE_PIXELS = 4.0
        // One framing step may scale the distance by at most this much in either direction, so a
        // single bad measurement cannot jump the camera into or out of the model in one go.
        // Lower bound of the distance-scale bisection bracket. The distance scale may go to zero in
        // principle, but the bracket is kept strictly positive so the geometric midpoint stays
        // meaningful; RobotExpressive needed a scale of about 0.02 to reach the target fill.
        private const val SILHOUETTE_MIN_BISECTION_SCALE = 1e-4f
        private const val CAMERA_TARGET_X = 0.0
        private const val CAMERA_TARGET_Y = 0.0
        private const val CAMERA_TARGET_Z = 0.0
        private const val MODEL_ORBIT_PIVOT_Y = 0.0f
        private val SUPPORTED_TEXTURE_MIME_TYPES = setOf(
            "image/png",
            "image/jpeg",
            "image/webp",
            "image/ktx2"
        )
        private val UNSUPPORTED_TEXTURE_EXTENSIONS = setOf(
            "tga",
            "bmp",
            "gif",
            "dds",
            "tif",
            "tiff",
            "hdr",
            "exr"
        )

        init {
            Utils.init()
        }
    }

    private val choreographer = Choreographer.getInstance()

    private val modelViewer = ModelViewer(this)

    private var currentModelPath: String? = null
    private var requestedAnimationName: String? = null
    private var requestedLooping: Boolean = false
    private var requestedPlaybackNonce: Long = -1L

    private var cameraPitchDegrees: Float = DEFAULT_CAMERA_PITCH
    private var cameraYawDegrees: Float = DEFAULT_CAMERA_YAW
    private var cameraDistanceScale: Float = DEFAULT_CAMERA_DISTANCE_SCALE
    private var cameraTargetHeightOffset: Float = 0.0f
    private var cameraTargetX: Double = CAMERA_TARGET_X
    private var cameraTargetY: Double = CAMERA_TARGET_Y
    private var cameraTargetZ: Double = CAMERA_TARGET_Z

    // Distance, in scene units, at which the loaded model fills the view. Computed from the
    // model bounds after loading, then multiplied by the user's distance scale so that the
    // slider keeps meaning "times the fitted distance" for every model.
    private var fittedCameraDistance: Double = 0.0

    // Distance-scale bracket for the silhouette framing. The drawn fill decreases monotonically as
    // the distance grows, so the scale that reaches the target fill can be found by bisecting:
    // `framingLowScale` always yields too small a figure, `framingHighScale` too large a one. The
    // bracket is kept across frames because each bisection step needs a rendered frame to measure.
    // The relation between distance and fill is only reciprocal while the whole figure is in frame,
    // and the authored bounds cannot be trusted (RobotExpressive draws as if it were ~48 times
    // smaller than its bounds claim), so bisection is safer than extrapolating the ratio.
    private var framingLowScale: Float = Float.NaN
    private var framingHighScale: Float = Float.NaN

    private var activeAnimationIndex: Int = -1
    private var animationStartNanos: Long = System.nanoTime()

    private var sunLightEntity: Int = 0
    private var fillLightEntity: Int = 0
    private var rimLightEntity: Int = 0
    private var indirectLight: IndirectLight? = null

    private var baseRootTransform: FloatArray? = null
    private var baseEntityTransforms: Map<Int, FloatArray> = emptyMap()

    private var hasLoggedManipulatorUnavailable: Boolean = false
    private var hasLoggedManipulatorApplyFailure: Boolean = false

    private val cameraManipulatorField by lazy {
        runCatching {
            ModelViewer::class.java.declaredFields
                .firstOrNull { field -> field.type == Manipulator::class.java }
                ?.apply { isAccessible = true }
        }.getOrNull()
    }

    private val uiHelperField by lazy {
        runCatching {
            ModelViewer::class.java.declaredFields
                .firstOrNull { field -> field.type == UiHelper::class.java }
                ?.apply { isAccessible = true }
        }.getOrNull()
    }

    private var animationNames: List<String> = emptyList()
    private var animationNameToIndex: Map<String, Int> = emptyMap()
    private var animationDurations: Map<Int, Float> = emptyMap()
    private var retainedModelSourceBuffer: ByteBuffer? = null
    private var retainedResourceBuffers: List<ByteBuffer> = emptyList()
    private var gltfResourceMimeHints: Map<String, String> = emptyMap()
    private var pendingModelPath: String? = null
    private var hasValidSurfaceSize: Boolean = false
    private var isModelLoadScheduled: Boolean = false
    private var modelLoadGeneration: Int = 0

    @Volatile
    private var isRendering: Boolean = false

    @Volatile
    private var onRenderErrorListener: ((String) -> Unit)? = null
    private var onCameraDistanceScaleChanged: ((Float) -> Unit)? = null

    @Volatile
    private var onAnimationsDiscoveredListener: ((List<String>, Map<String, Long>) -> Unit)? = null

    init {
        setupTransparentSurface()
        setupCameraDefaults()
        setupSceneLighting()
        // A tap on the preview re-reads the tuner file and re-applies the camera, so a value edited
        // through adb is visible without restarting the app.
        setOnClickListener {
            GltfTuner.invalidate()
            GltfTuner.settings(context)
            updateFittedCameraDistance()
            applyCameraPose()
            AppLogger.i(TAG, "Re-applied glTF tuner after tap: ${GltfTuner.settings(context)}")
        }
    }

    fun setOnRenderErrorListener(listener: ((String) -> Unit)?) {
        onRenderErrorListener = listener
    }

    fun setOnAnimationsDiscoveredListener(listener: ((List<String>, Map<String, Long>) -> Unit)?) {
        onAnimationsDiscoveredListener = listener
    }

    /**
     * Receives the distance scale chosen by [scheduleSilhouetteFraming] so the settings layer can
     * persist it. Without this the correction would survive only until the next UI emission.
     */
    fun setOnCameraDistanceScaleChangedListener(listener: ((Float) -> Unit)?) {
        onCameraDistanceScaleChanged = listener
    }

    fun setModelPath(path: String) {
        val normalizedPath = path.trim()
        if (normalizedPath.isEmpty() || normalizedPath == currentModelPath || normalizedPath == pendingModelPath) {
            return
        }

        pendingModelPath = normalizedPath
        schedulePendingModelLoad()
    }

    fun setAnimationState(animationName: String?, isLooping: Boolean, playbackNonce: Long) {
        val normalizedName = animationName?.trim()?.takeIf { it.isNotEmpty() }
        if (
            requestedAnimationName == normalizedName &&
            requestedLooping == isLooping &&
            requestedPlaybackNonce == playbackNonce
        ) {
            return
        }

        requestedAnimationName = normalizedName
        requestedLooping = isLooping
        requestedPlaybackNonce = playbackNonce
        restartAnimationClock()
        applyRequestedAnimationSelection()
    }

    fun setCameraPose(
        pitchDegrees: Float,
        yawDegrees: Float,
        distanceScale: Float,
        targetHeightOffset: Float
    ) {
        val clampedPitch = pitchDegrees.coerceIn(-89f, 89f)
        val normalizedYaw = yawDegrees.coerceIn(-180f, 180f)
        val clampedDistanceScale = distanceScale.coerceIn(MIN_CAMERA_DISTANCE_SCALE, MAX_CAMERA_DISTANCE_SCALE)
        val clampedTargetHeight = targetHeightOffset.coerceIn(MIN_CAMERA_TARGET_HEIGHT, MAX_CAMERA_TARGET_HEIGHT)

        val unchanged =
            cameraPitchDegrees == clampedPitch &&
                cameraYawDegrees == normalizedYaw &&
                cameraDistanceScale == clampedDistanceScale &&
                cameraTargetHeightOffset == clampedTargetHeight
        if (unchanged) {
            return
        }

        cameraPitchDegrees = clampedPitch
        cameraYawDegrees = normalizedYaw
        cameraDistanceScale = clampedDistanceScale
        cameraTargetHeightOffset = clampedTargetHeight
        applyCameraPose()
    }

    fun onResume() {
        if (isRendering) {
            return
        }
        isRendering = true
        restartAnimationClock()
        choreographer.postFrameCallback(this)
    }

    fun onPause() {
        if (!isRendering) {
            return
        }
        isRendering = false
        choreographer.removeFrameCallback(this)
    }

    override fun doFrame(frameTimeNanos: Long) {
        if (!isRendering) {
            return
        }

        choreographer.postFrameCallback(this)
        renderFrame(frameTimeNanos)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        hasValidSurfaceSize = w > 0 && h > 0
        if (hasValidSurfaceSize) {
            schedulePendingModelLoad()
        }
        // The fit depends on the aspect ratio, so recompute it whenever the surface changes size.
        if (currentModelPath != null && fittedCameraDistance > 0.0) {
            val viewportChanged = w != oldw || h != oldh
            if (viewportChanged) {
                updateFittedCameraDistance()
            }
        }
        applyCameraPose()
    }

    override fun onDetachedFromWindow() {
        onPause()
        hasValidSurfaceSize = false
        isModelLoadScheduled = false
        pendingModelPath = null
        super.onDetachedFromWindow()
    }

    private fun renderFrame(frameTimeNanos: Long) {
        try {
            // Re-apply camera each frame because ModelViewer.render() internally updates camera from manipulator.
            applyCameraPose()
            applyAnimation(frameTimeNanos)
            modelViewer.render(frameTimeNanos)
        } catch (e: Exception) {
            dispatchError("Failed to render glTF frame: ${e.message ?: "unknown error"}")
        }
    }

    private fun applyAnimation(frameTimeNanos: Long) {
        val animator = modelViewer.animator ?: return
        val animationCount = animator.animationCount
        if (animationCount <= 0) {
            return
        }

        val animationIndex = activeAnimationIndex
        if (animationIndex !in 0 until animationCount) {
            return
        }

        val elapsedSeconds = max((frameTimeNanos - animationStartNanos).toDouble() / 1_000_000_000.0, 0.0).toFloat()
        val durationSeconds = animationDurations[animationIndex]
            ?: runCatching { animator.getAnimationDuration(animationIndex) }.getOrNull()

        val sampleTime = when {
            durationSeconds != null && durationSeconds > 0f && requestedLooping -> elapsedSeconds % durationSeconds
            durationSeconds != null && durationSeconds > 0f -> min(elapsedSeconds, durationSeconds)
            else -> elapsedSeconds
        }

        animator.applyAnimation(animationIndex, sampleTime)
        animator.updateBoneMatrices()
    }

    private fun loadModel(
        modelPath: String,
        sourceModelFile: File = File(modelPath),
        mimeHints: Map<String, String> = emptyMap()
    ) {
        val modelFile = sourceModelFile
        if (!modelFile.exists() || !modelFile.isFile) {
            dispatchError("glTF model file not found: $modelPath")
            return
        }

        val extension = modelFile.extension.lowercase()
        if (extension != "glb" && extension != "gltf") {
            dispatchError("Unsupported glTF model extension: .$extension (expected .glb or .gltf)")
            return
        }

        try {
            val nextResourceBuffers = ArrayList<ByteBuffer>()
            if (extension == "gltf") {
                gltfResourceMimeHints = mimeHints
            } else {
                gltfResourceMimeHints = emptyMap()
            }
            val modelBuffer = readFileToDirectByteBuffer(modelFile)
            if (extension == "glb") {
                modelViewer.loadModelGlb(modelBuffer)
            } else {
                val baseDirectory = modelFile.parentFile
                modelViewer.loadModelGltf(modelBuffer) { relativeUri ->
                    readResourceToDirectByteBuffer(baseDirectory, relativeUri).also(nextResourceBuffers::add)
                }
            }
            retainedModelSourceBuffer = modelBuffer
            retainedResourceBuffers = nextResourceBuffers

            currentModelPath = modelPath
            applySafeUnitCubeTransform()
            captureBaseRootTransform()
            updateFittedCameraDistance()
            applyCameraPose()
            refreshDiscoveredAnimations()
            restartAnimationClock()
            applyRequestedAnimationSelection()
            // Reframe on the pixels actually drawn; asset bounds do not describe them for a
            // skinned model (see scheduleSilhouetteFraming).
            scheduleSilhouetteFraming()

            AppLogger.i(TAG, "Loaded glTF model: $modelPath")
        } catch (e: Exception) {
            runCatching { modelViewer.destroyModel() }
            currentModelPath = null
            activeAnimationIndex = -1
            baseRootTransform = null
            baseEntityTransforms = emptyMap()
            animationNames = emptyList()
            animationNameToIndex = emptyMap()
            animationDurations = emptyMap()
            retainedModelSourceBuffer = null
            retainedResourceBuffers = emptyList()
            gltfResourceMimeHints = emptyMap()
            cameraTargetX = CAMERA_TARGET_X
            cameraTargetY = CAMERA_TARGET_Y
            cameraTargetZ = CAMERA_TARGET_Z
            onAnimationsDiscoveredListener?.invoke(emptyList(), emptyMap())
            dispatchError("Failed to load glTF model: ${e.message ?: "unknown error"}")
        }
    }

    private fun schedulePendingModelLoad() {
        if (!hasValidSurfaceSize || !isAttachedToWindow || isModelLoadScheduled) {
            return
        }
        val requestedPath = pendingModelPath ?: return
        if (requestedPath == currentModelPath) {
            pendingModelPath = null
            return
        }

        isModelLoadScheduled = true
        post {
            isModelLoadScheduled = false
            if (!hasValidSurfaceSize || !isAttachedToWindow) {
                return@post
            }

            val nextPath = pendingModelPath ?: return@post
            if (nextPath == currentModelPath) {
                pendingModelPath = null
                return@post
            }

            pendingModelPath = null
            beginModelLoad(nextPath)
        }
    }

    private fun beginModelLoad(modelPath: String) {
        val generation = ++modelLoadGeneration
        val sourceFile = resolveSourceModelFile(modelPath)
        val extension = sourceFile.extension.lowercase()
        if (extension != "gltf") {
            loadModel(modelPath, sourceModelFile = sourceFile)
            return
        }

        Thread(
            {
                val prepared = runCatching { prepareGltfModelForLoad(sourceFile) }
                post {
                    if (generation != modelLoadGeneration || !isAttachedToWindow) {
                        return@post
                    }
                    prepared
                        .onSuccess { result ->
                            loadModel(
                                modelPath = modelPath,
                                sourceModelFile = result.modelFile,
                                mimeHints = result.mimeHints
                            )
                        }
                        .onFailure { error ->
                            dispatchError("Failed to prepare glTF model: ${error.message ?: "unknown error"}")
                        }
                }
            },
            "gltf-prepare"
        ).start()
    }

    private fun resolveSourceModelFile(modelPath: String): File {
        val candidate = File(modelPath)
        if (!candidate.name.startsWith(".operit_normalized_", ignoreCase = true)) {
            return candidate
        }

        val originalName = candidate.name.removePrefix(".operit_normalized_")
        if (originalName.isBlank()) {
            return candidate
        }

        val siblingOriginal = File(candidate.parentFile, originalName)
        return if (siblingOriginal.exists() && siblingOriginal.isFile) {
            siblingOriginal
        } else {
            candidate
        }
    }

    private fun prepareGltfModelForLoad(modelFile: File): PreparedGltfModel {
        val root = JSONObject(modelFile.readText())
        val workspaceDir = prepareWorkspaceDirForModel(modelFile)
        val inlineDir = File(workspaceDir, "inline")
        val sourceBaseDir = modelFile.parentFile
        val bufferBytesCache = HashMap<Int, ByteArray>()
        var changed = false

        val buffers = root.optJSONArray("buffers")
        if (buffers != null) {
            for (index in 0 until buffers.length()) {
                val bufferObj = buffers.optJSONObject(index) ?: continue
                val uri = bufferObj.optString("uri").trim()
                if (!uri.startsWith("data:", ignoreCase = true)) {
                    continue
                }

                val bytes = decodeDataUriToByteArray(uri)
                inlineDir.mkdirs()
                val fileName = "buffer_$index.bin"
                val outputFile = File(inlineDir, fileName)
                if (!outputFile.exists() || outputFile.length() != bytes.size.toLong()) {
                    outputFile.writeBytes(bytes)
                }
                bufferObj.put("uri", "inline/$fileName")
                bufferBytesCache[index] = bytes
                changed = true
            }
        }

        val images = root.optJSONArray("images")
        if (images != null) {
            for (index in 0 until images.length()) {
                val imageObj = images.optJSONObject(index) ?: continue
                val uri = imageObj.optString("uri").trim()
                val declaredMimeType = canonicalizeImageMimeType(imageObj.optString("mimeType").trim())
                if (declaredMimeType.isNotEmpty() && declaredMimeType != imageObj.optString("mimeType").trim().lowercase()) {
                    imageObj.put("mimeType", declaredMimeType)
                    changed = true
                }
                if (!uri.startsWith("data:", ignoreCase = true)) {
                    continue
                }

                val bytes = decodeDataUriToByteArray(uri)
                val dataUriMimeType = inferDataUriMimeType(uri)
                val sniffedMimeType = inferMimeTypeFromBytes(bytes)
                val effectiveMimeType = declaredMimeType
                    .ifBlank { dataUriMimeType }
                    .ifBlank { sniffedMimeType }
                if (declaredMimeType.isBlank() && effectiveMimeType.isNotBlank()) {
                    imageObj.put("mimeType", effectiveMimeType)
                }
                val imageExt = imageFileExtensionForMime(effectiveMimeType)

                inlineDir.mkdirs()
                inlineDir.listFiles { file ->
                    file.isFile && file.name.startsWith("image_${index}.") && file.extension.lowercase() != imageExt.lowercase()
                }?.forEach { stale -> runCatching { stale.delete() } }
                val fileName = "image_$index.$imageExt"
                val outputFile = File(inlineDir, fileName)
                if (!outputFile.exists() || outputFile.length() != bytes.size.toLong()) {
                    outputFile.writeBytes(bytes)
                }
                imageObj.put("uri", "inline/$fileName")
                changed = true
            }
        }

        val bufferViews = root.optJSONArray("bufferViews")
        if (images != null && bufferViews != null && buffers != null) {
            for (index in 0 until images.length()) {
                val imageObj = images.optJSONObject(index) ?: continue
                val uri = imageObj.optString("uri").trim()
                if (uri.isNotEmpty()) {
                    continue
                }

                val declaredMimeType = canonicalizeImageMimeType(imageObj.optString("mimeType").trim())
                if (declaredMimeType.isNotEmpty()) {
                    if (declaredMimeType != imageObj.optString("mimeType").trim().lowercase()) {
                        imageObj.put("mimeType", declaredMimeType)
                        changed = true
                    }
                    continue
                }

                val bufferViewIndex = imageObj.optInt("bufferView", -1)
                if (bufferViewIndex < 0) {
                    continue
                }
                val bufferViewObj = bufferViews.optJSONObject(bufferViewIndex) ?: continue
                val bufferIndex = bufferViewObj.optInt("buffer", -1)
                val byteOffset = bufferViewObj.optInt("byteOffset", 0)
                val byteLength = bufferViewObj.optInt("byteLength", -1)
                if (bufferIndex < 0 || byteLength <= 0 || byteOffset < 0) {
                    continue
                }

                val bufferBytes = bufferBytesCache.getOrPut(bufferIndex) {
                    val bufferObj = buffers.optJSONObject(bufferIndex)
                    val bufferUri = bufferObj?.optString("uri").orEmpty().trim()
                    when {
                        bufferUri.isEmpty() -> ByteArray(0)
                        bufferUri.startsWith("data:", ignoreCase = true) -> decodeDataUriToByteArray(bufferUri)
                        else -> resolveResourceFile(sourceBaseDir, bufferUri).readBytes()
                    }
                }
                if (bufferBytes.isEmpty()) {
                    continue
                }
                val endExclusive = byteOffset + byteLength
                if (endExclusive > bufferBytes.size) {
                    continue
                }

                val sampleSize = min(64, byteLength)
                val sample = bufferBytes.copyOfRange(byteOffset, byteOffset + sampleSize)
                val sniffedMimeType = inferMimeTypeFromBytes(sample)
                if (sniffedMimeType.isNotEmpty()) {
                    imageObj.put("mimeType", sniffedMimeType)
                    changed = true
                }
            }
        }

        val autoLinkedTextureCount = autoLinkMissingBaseColorTextures(
            root = root,
            sourceBaseDir = sourceBaseDir,
            inlineDir = inlineDir
        )
        if (autoLinkedTextureCount > 0) {
            changed = true
        }

        val textureDiagnostics = collectTextureDiagnostics(root, sourceBaseDir)
        if (textureDiagnostics.materialsWithTextureRefs == 0 && textureDiagnostics.externalImageFileCount > 0) {
            val samples = textureDiagnostics.sampleExternalImageFiles.joinToString()
            val unsupportedSamples = textureDiagnostics.sampleUnsupportedExternalImageFiles.joinToString()
            AppLogger.w(
                TAG,
                "glTF has no texture references but directory has image files. " +
                    "source=${modelFile.name}, images=${textureDiagnostics.imageCount}, textures=${textureDiagnostics.textureCount}, " +
                    "materials=${textureDiagnostics.materialCount}, externalImages=${textureDiagnostics.externalImageFileCount}, sample=[$samples], " +
                    "unsupportedExternalImages=${textureDiagnostics.unsupportedExternalImageFileCount}, unsupportedSample=[$unsupportedSamples]"
            )
        }

        val preparedFile = if (changed) {
            val normalizedFile = File(workspaceDir, "normalized.gltf")
            normalizedFile.writeText(toNormalizedGltfJson(root))
            normalizedFile
        } else {
            modelFile
        }

        AppLogger.i(
            TAG,
            "Prepared glTF model for load: source=${modelFile.name}, prepared=${preparedFile.name}, changed=$changed, " +
                "images=${textureDiagnostics.imageCount}, textures=${textureDiagnostics.textureCount}, " +
                "materialsWithTextureRefs=${textureDiagnostics.materialsWithTextureRefs}/${textureDiagnostics.materialCount}, " +
                "autoLinkedTextures=$autoLinkedTextureCount"
        )

        return PreparedGltfModel(
            modelFile = preparedFile,
            mimeHints = buildGltfResourceMimeHints(root)
        )
    }

    private fun prepareWorkspaceDirForModel(modelFile: File): File {
        val sourcePath = runCatching { modelFile.canonicalPath }.getOrElse { modelFile.absolutePath }
        val sourceHash = sourcePath.hashCode().toUInt().toString(16)
        val workspaceDir = File(context.cacheDir, "gltf_prepared/$sourceHash")
        workspaceDir.mkdirs()
        return workspaceDir
    }

    private fun toNormalizedGltfJson(root: JSONObject): String {
        // Filament's glTF URI / mime parsing can mis-handle escaped slashes like `\/`.
        // Serialize JSON without slash escaping so values stay as `inline/...` and `image/png`.
        return root.toString().replace("\\/", "/")
    }

    private fun autoLinkMissingBaseColorTextures(
        root: JSONObject,
        sourceBaseDir: File?,
        inlineDir: File
    ): Int {
        val materials = root.optJSONArray("materials") ?: return 0
        if (materials.length() == 0) {
            return 0
        }
        val currentImages = root.optJSONArray("images")
        val currentTextures = root.optJSONArray("textures")
        if ((currentImages?.length() ?: 0) > 0 || (currentTextures?.length() ?: 0) > 0) {
            return 0
        }

        val candidateFiles = listSupportedImageFiles(sourceBaseDir)
            .filter { file ->
                val normalizedName = normalizeNameForMatch(file.nameWithoutExtension)
                normalizedName.contains("diffuse") ||
                    normalizedName.contains("basecolor") ||
                    normalizedName.contains("albedo")
            }
        if (candidateFiles.isEmpty()) {
            return 0
        }

        val images = currentImages ?: JSONArray().also { root.put("images", it) }
        val textures = currentTextures ?: JSONArray().also { root.put("textures", it) }
        val textureIndexBySourceName = LinkedHashMap<String, Int>()
        val linkedDetails = ArrayList<String>()
        val unmatchedMaterials = ArrayList<String>()
        var pbrNormalizedCount = 0
        var unlitNormalizedCount = 0
        var liftedBaseColorCount = 0
        var linkedCount = 0

        for (index in 0 until materials.length()) {
            val material = materials.optJSONObject(index) ?: continue
            if (materialHasTextureReference(material)) {
                continue
            }

            val materialName = material.optString("name").trim()
            val matchedFile = findBestTextureFileForMaterial(materialName, candidateFiles) ?: continue
            val textureIndex = textureIndexBySourceName.getOrPut(matchedFile.name) {
                val imageIndex = images.length()
                val copiedName = "auto_image_${imageIndex}.${matchedFile.extension.lowercase()}"
                inlineDir.mkdirs()
                val copiedFile = File(inlineDir, copiedName)
                if (!copiedFile.exists() || copiedFile.length() != matchedFile.length()) {
                    copiedFile.writeBytes(matchedFile.readBytes())
                }

                val imageObject = JSONObject().apply {
                    put("uri", "inline/$copiedName")
                    val mimeType = mimeTypeForSupportedImageExtension(matchedFile.extension)
                    if (mimeType.isNotEmpty()) {
                        put("mimeType", mimeType)
                    }
                }
                images.put(imageObject)

                val textureObject = JSONObject().apply {
                    put("source", imageIndex)
                }
                textures.put(textureObject)
                textures.length() - 1
            }

            val pbr = material.optJSONObject("pbrMetallicRoughness") ?: JSONObject().also {
                material.put("pbrMetallicRoughness", it)
            }
            pbr.put("baseColorTexture", JSONObject().put("index", textureIndex))
            applyMmdStyleBaseColorLift(pbr)
            liftedBaseColorCount += 1
            // This model family often ships without proper glTF texture bindings and with high metallic
            // factors, which causes near-black shading when environment lighting is reduced.
            // Normalize to a non-metal workflow when we auto-link external baseColor textures.
            pbr.put("metallicFactor", 0.0)
            val roughness = pbr.optDouble("roughnessFactor", 1.0)
            if (!roughness.isNaN() && roughness < 0.82) {
                pbr.put("roughnessFactor", 0.82)
            }

            val materialExtensions = material.optJSONObject("extensions") ?: JSONObject().also {
                material.put("extensions", it)
            }
            if (!materialExtensions.has("KHR_materials_unlit")) {
                materialExtensions.put("KHR_materials_unlit", JSONObject())
                unlitNormalizedCount += 1
            }
            pbrNormalizedCount += 1
            linkedCount += 1
            linkedDetails.add("${if (materialName.isBlank()) "<unnamed:$index>" else materialName}->${matchedFile.name}")
        }

        if (unlitNormalizedCount > 0) {
            ensureTopLevelGltfExtensionUsed(root, "KHR_materials_unlit")
        }

        for (index in 0 until materials.length()) {
            val material = materials.optJSONObject(index) ?: continue
            if (materialHasTextureReference(material)) {
                continue
            }
            val materialName = material.optString("name").trim()
            unmatchedMaterials.add(if (materialName.isBlank()) "<unnamed:$index>" else materialName)
        }

        if (linkedCount > 0) {
            AppLogger.w(
                TAG,
                "Auto-linked missing glTF baseColor textures by file name: linked=$linkedCount, candidates=${candidateFiles.size}, normalizedPbr=$pbrNormalizedCount, normalizedUnlit=$unlitNormalizedCount, liftedBaseColor=$liftedBaseColorCount"
            )
            AppLogger.i(TAG, "Applied MMD-style base color lift for auto-linked materials: multiplier=1.22, maxRgb=1.6")
            AppLogger.i(TAG, "Auto-linked material textures: ${linkedDetails.joinToString()}")
        }
        if (unmatchedMaterials.isNotEmpty()) {
            AppLogger.w(TAG, "Materials still without baseColorTexture after auto-link: ${unmatchedMaterials.joinToString()}")
        }
        return linkedCount
    }

    private fun applyMmdStyleBaseColorLift(pbr: JSONObject) {
        val source = pbr.optJSONArray("baseColorFactor")
        val rgba = FloatArray(4)
        rgba[0] = source?.optDouble(0, 1.0)?.toFloat() ?: 1f
        rgba[1] = source?.optDouble(1, 1.0)?.toFloat() ?: 1f
        rgba[2] = source?.optDouble(2, 1.0)?.toFloat() ?: 1f
        rgba[3] = source?.optDouble(3, 1.0)?.toFloat() ?: 1f

        // Match MMD preview's brighter look by lifting base color before rendering.
        val lift = 1.22f
        val maxRgb = 1.6f
        val lifted = JSONArray()
        lifted.put((rgba[0] * lift).coerceIn(0f, maxRgb).toDouble())
        lifted.put((rgba[1] * lift).coerceIn(0f, maxRgb).toDouble())
        lifted.put((rgba[2] * lift).coerceIn(0f, maxRgb).toDouble())
        lifted.put(rgba[3].coerceIn(0f, 1f).toDouble())
        pbr.put("baseColorFactor", lifted)
    }

    private fun ensureTopLevelGltfExtensionUsed(root: JSONObject, extensionName: String) {
        if (extensionName.isBlank()) {
            return
        }

        val extensionsUsed = root.optJSONArray("extensionsUsed") ?: JSONArray().also {
            root.put("extensionsUsed", it)
        }
        for (index in 0 until extensionsUsed.length()) {
            if (extensionsUsed.optString(index) == extensionName) {
                return
            }
        }
        extensionsUsed.put(extensionName)
    }

    private fun listSupportedImageFiles(baseDirectory: File?): List<File> {
        if (baseDirectory == null || !baseDirectory.exists() || !baseDirectory.isDirectory) {
            return emptyList()
        }

        return baseDirectory.listFiles { file ->
            if (!file.isFile) {
                return@listFiles false
            }
            when (file.extension.lowercase()) {
                "png", "jpg", "jpeg", "webp", "ktx2" -> true
                else -> false
            }
        }?.sortedBy { it.name.lowercase() }.orEmpty()
    }

    private fun listImageFilesForDiagnostics(baseDirectory: File?): List<File> {
        if (baseDirectory == null || !baseDirectory.exists() || !baseDirectory.isDirectory) {
            return emptyList()
        }

        return baseDirectory.listFiles { file ->
            if (!file.isFile) {
                return@listFiles false
            }
            when (file.extension.lowercase()) {
                "png", "jpg", "jpeg", "webp", "ktx2", "exr", "hdr", "bmp", "tga", "tif", "tiff", "dds", "gif" -> true
                else -> false
            }
        }?.sortedBy { it.name.lowercase() }.orEmpty()
    }

    private fun findBestTextureFileForMaterial(materialName: String, candidates: List<File>): File? {
        if (materialName.isBlank() || candidates.isEmpty()) {
            return null
        }

        val aliases = buildMaterialAliases(materialName)
        if (aliases.isEmpty()) {
            return null
        }

        val materialDigits = materialName.filter { it.isDigit() }.trimStart('0')
        var bestScore = Int.MIN_VALUE
        var bestFile: File? = null
        for (candidate in candidates) {
            val normalizedCandidate = normalizeNameForMatch(candidate.nameWithoutExtension)
            val aliasScore = aliases.maxOfOrNull { alias ->
                if (normalizedCandidate.contains(alias)) alias.length * 20 else Int.MIN_VALUE
            } ?: Int.MIN_VALUE
            if (aliasScore == Int.MIN_VALUE) {
                continue
            }

            var score = aliasScore
            if (normalizedCandidate.contains("diffuse")) {
                score += 8
            }
            if (normalizedCandidate.contains("basecolor") || normalizedCandidate.contains("albedo")) {
                score += 6
            }

            if (materialDigits.isNotEmpty()) {
                when {
                    normalizedCandidate.contains(materialDigits) -> score += 12
                    normalizedCandidate.any { it.isDigit() } -> score -= 6
                }
            }

            if (score > bestScore) {
                bestScore = score
                bestFile = candidate
            }
        }

        return bestFile
    }

    private fun buildMaterialAliases(materialName: String): List<String> {
        val lowerName = materialName.lowercase()
        val segments = lowerName
            .split(Regex("[^a-z0-9]+"))
            .filter { it.isNotBlank() }
            .toMutableList()
        while (segments.isNotEmpty() && segments.first() in setOf("ani", "mat", "material", "grokani")) {
            segments.removeAt(0)
        }

        val aliases = LinkedHashSet<String>()
        val genericSegments = setOf("ani", "mat", "material", "grokani", "sticker", "mesh", "model", "part")
        val joined = normalizeNameForMatch(segments.joinToString(""))
        if (joined.isNotBlank()) {
            aliases.add(joined)
        }
        segments.forEach { segment ->
            if (segment in genericSegments) {
                return@forEach
            }
            val normalized = normalizeNameForMatch(segment)
            if (normalized.isNotBlank()) {
                aliases.add(normalized)
            }
        }

        if (aliases.any { it.contains("outfit") }) {
            aliases.add("dress")
        }
        if (aliases.any { it.contains("body") }) {
            aliases.add("main")
            aliases.add("dress")
            aliases.add("face")
        }
        if (aliases.any { it.contains("eyes") || it == "eye" }) {
            aliases.add("face")
            aliases.add("eye")
        }
        if (aliases.any { it.contains("lashes") || it.contains("lash") }) {
            aliases.add("face")
            aliases.add("eye")
        }
        if (aliases.any { it.contains("blush") }) {
            aliases.add("faceblush")
        }
        if (aliases.any { it.contains("spark") }) {
            aliases.add("eyespark")
        }

        return aliases.toList()
    }

    private fun normalizeNameForMatch(value: String): String {
        return value
            .lowercase()
            .replace(Regex("[^a-z0-9]+"), "")
    }

    private fun mimeTypeForSupportedImageExtension(extension: String): String {
        return when (extension.lowercase()) {
            "png" -> "image/png"
            "jpg", "jpeg" -> "image/jpeg"
            "webp" -> "image/webp"
            "ktx2" -> "image/ktx2"
            else -> ""
        }
    }

    private fun materialHasTextureReference(material: JSONObject): Boolean {
        val pbr = material.optJSONObject("pbrMetallicRoughness")
        if (pbr?.optJSONObject("baseColorTexture") != null) return true
        if (pbr?.optJSONObject("metallicRoughnessTexture") != null) return true
        if (material.optJSONObject("normalTexture") != null) return true
        if (material.optJSONObject("occlusionTexture") != null) return true
        if (material.optJSONObject("emissiveTexture") != null) return true

        val extensions = material.optJSONObject("extensions") ?: return false
        val extensionNames = extensions.keys()
        while (extensionNames.hasNext()) {
            val extensionName = extensionNames.next()
            val extensionValue = extensions.optJSONObject(extensionName) ?: continue
            val extensionKeys = extensionValue.keys()
            while (extensionKeys.hasNext()) {
                val key = extensionKeys.next()
                if (key.endsWith("Texture", ignoreCase = true)) {
                    val textureObject = extensionValue.optJSONObject(key)
                    if (textureObject?.has("index") == true) {
                        return true
                    }
                }
            }
        }

        return false
    }

    private fun collectTextureDiagnostics(root: JSONObject, sourceBaseDir: File?): TextureDiagnostics {
        val images = root.optJSONArray("images")
        val textures = root.optJSONArray("textures")
        val materials = root.optJSONArray("materials")
        val materialCount = materials?.length() ?: 0
        var materialsWithTextureRefs = 0
        if (materials != null) {
            for (index in 0 until materials.length()) {
                val material = materials.optJSONObject(index) ?: continue
                if (materialHasTextureReference(material)) {
                    materialsWithTextureRefs += 1
                }
            }
        }

        val externalImages = listImageFilesForDiagnostics(sourceBaseDir)
        val unsupportedExternalImages = externalImages.filter { file ->
            file.extension.lowercase() in UNSUPPORTED_TEXTURE_EXTENSIONS
        }
        return TextureDiagnostics(
            imageCount = images?.length() ?: 0,
            textureCount = textures?.length() ?: 0,
            materialCount = materialCount,
            materialsWithTextureRefs = materialsWithTextureRefs,
            externalImageFileCount = externalImages.size,
            sampleExternalImageFiles = externalImages.take(6).map { it.name },
            unsupportedExternalImageFileCount = unsupportedExternalImages.size,
            sampleUnsupportedExternalImageFiles = unsupportedExternalImages.take(4).map { it.name }
        )
    }

    private fun buildGltfResourceMimeHints(root: JSONObject): Map<String, String> {
        val images = root.optJSONArray("images") ?: return emptyMap()
        val hints = LinkedHashMap<String, String>()
        for (index in 0 until images.length()) {
            val image = images.optJSONObject(index) ?: continue
            val uri = image.optString("uri").trim()
            val mimeType = canonicalizeImageMimeType(image.optString("mimeType").trim())
            if (uri.isNotEmpty() && mimeType.isNotEmpty()) {
                hints[uri] = mimeType
            }
        }
        return hints
    }

    private fun inferDataUriMimeType(dataUri: String): String {
        val metadata = dataUri.substringBefore(',')
        return canonicalizeImageMimeType(
            metadata
            .removePrefix("data:")
            .substringBefore(';')
            .trim()
        )
    }

    private fun imageFileExtensionForMime(mimeType: String): String {
        return when (canonicalizeImageMimeType(mimeType)) {
            "image/png" -> "png"
            "image/jpeg" -> "jpg"
            "image/webp" -> "webp"
            "image/ktx2" -> "ktx2"
            else -> "bin"
        }
    }

    private fun inferMimeTypeFromBytes(bytes: ByteArray): String {
        if (bytes.size >= 8 &&
            bytes[0] == 0x89.toByte() &&
            bytes[1] == 0x50.toByte() &&
            bytes[2] == 0x4E.toByte() &&
            bytes[3] == 0x47.toByte() &&
            bytes[4] == 0x0D.toByte() &&
            bytes[5] == 0x0A.toByte() &&
            bytes[6] == 0x1A.toByte() &&
            bytes[7] == 0x0A.toByte()
        ) {
            return "image/png"
        }

        if (bytes.size >= 3 &&
            bytes[0] == 0xFF.toByte() &&
            bytes[1] == 0xD8.toByte() &&
            bytes[2] == 0xFF.toByte()
        ) {
            return "image/jpeg"
        }

        if (bytes.size >= 12 &&
            bytes[0] == 0x52.toByte() &&
            bytes[1] == 0x49.toByte() &&
            bytes[2] == 0x46.toByte() &&
            bytes[3] == 0x46.toByte() &&
            bytes[8] == 0x57.toByte() &&
            bytes[9] == 0x45.toByte() &&
            bytes[10] == 0x42.toByte() &&
            bytes[11] == 0x50.toByte()
        ) {
            return "image/webp"
        }

        if (bytes.size >= 12 &&
            bytes[0] == 0xAB.toByte() &&
            bytes[1] == 0x4B.toByte() &&
            bytes[2] == 0x54.toByte() &&
            bytes[3] == 0x58.toByte() &&
            bytes[4] == 0x20.toByte() &&
            bytes[5] == 0x32.toByte() &&
            bytes[6] == 0x30.toByte() &&
            bytes[7] == 0xBB.toByte() &&
            bytes[8] == 0x0D.toByte() &&
            bytes[9] == 0x0A.toByte() &&
            bytes[10] == 0x1A.toByte() &&
            bytes[11] == 0x0A.toByte()
        ) {
            return "image/ktx2"
        }

        return ""
    }

    private fun refreshDiscoveredAnimations() {
        val animator = modelViewer.animator
        val animationCount = animator?.animationCount ?: 0
        if (animator == null || animationCount <= 0) {
            animationNames = emptyList()
            animationNameToIndex = emptyMap()
            animationDurations = emptyMap()
            onAnimationsDiscoveredListener?.invoke(emptyList(), emptyMap())
            return
        }

        val names = ArrayList<String>(animationCount)
        val nameIndexMap = LinkedHashMap<String, Int>(animationCount)
        val durationMap = LinkedHashMap<Int, Float>(animationCount)

        for (index in 0 until animationCount) {
            val rawName = runCatching { animator.getAnimationName(index) }.getOrNull().orEmpty().trim()
            val safeName = if (rawName.isBlank()) "Animation $index" else rawName
            names.add(safeName)
            nameIndexMap[safeName] = index

            val duration = runCatching { animator.getAnimationDuration(index) }.getOrNull()
            if (duration != null && duration > 0f) {
                durationMap[index] = duration
            }
        }

        animationNames = names
        animationNameToIndex = nameIndexMap
        animationDurations = durationMap
        val durationMillisByName =
            durationMap.entries.associate { (index, durationSeconds) ->
                names[index] to (durationSeconds * 1000f).toLong().coerceAtLeast(1L)
            }
        onAnimationsDiscoveredListener?.invoke(names, durationMillisByName)
    }

    private fun applyRequestedAnimationSelection() {
        val animator = modelViewer.animator
        val animationCount = animator?.animationCount ?: 0
        if (animator == null || animationCount <= 0) {
            activeAnimationIndex = -1
            return
        }

        activeAnimationIndex = resolveAnimationIndex(requestedAnimationName, animationCount)
    }

    private fun resolveAnimationIndex(animationName: String?, animationCount: Int): Int {
        if (animationCount <= 0) {
            return -1
        }

        if (animationName.isNullOrBlank()) {
            return -1
        }

        animationNameToIndex[animationName]?.let { index ->
            return index
        }

        val parsedIndex = animationName.toIntOrNull()
        if (parsedIndex != null && parsedIndex in 0 until animationCount) {
            return parsedIndex
        }

        val normalized = animationName.lowercase()
        for (index in animationNames.indices) {
            if (animationNames[index].lowercase() == normalized) {
                return index
            }
        }

        return -1
    }

    private fun restartAnimationClock() {
        animationStartNanos = System.nanoTime()
    }

    private fun setupTransparentSurface() {
        // Keep transparent composition but avoid onTop mode, which is more crash-prone on some drivers.
        holder.setFormat(PixelFormat.TRANSLUCENT)
        setZOrderOnTop(true)

        val clearOptions = Renderer.ClearOptions().apply {
            clear = true
            discard = true
            clearColor = floatArrayOf(0f, 0f, 0f, 0f)
        }
        modelViewer.renderer.clearOptions = clearOptions
        modelViewer.view.blendMode = View.BlendMode.TRANSLUCENT
        modelViewer.view.setPostProcessingEnabled(false)

        val uiHelper = runCatching { uiHelperField?.get(modelViewer) as? UiHelper }.getOrNull()
        uiHelper?.setOpaque(false)
    }

    private fun setupCameraDefaults() {
        modelViewer.cameraNear = 0.0005f
        modelViewer.cameraFar = 500.0f
    }

    private fun setupSceneLighting() {
        if (sunLightEntity != 0) {
            return
        }

        val engine = modelViewer.engine
        val defaultLight = modelViewer.light
        if (defaultLight != 0) {
            modelViewer.scene.removeEntity(defaultLight)
        }

        sunLightEntity = createDirectionalLight(
            engine = engine,
            colorR = 1.0f,
            colorG = 0.98f,
            colorB = 0.95f,
            intensity = 175_000.0f,
            directionX = 0.0f,
            directionY = -0.22f,
            directionZ = -0.98f
        )
        fillLightEntity = createDirectionalLight(
            engine = engine,
            colorR = 0.94f,
            colorG = 0.96f,
            colorB = 1.0f,
            intensity = 52_000.0f,
            directionX = 0.55f,
            directionY = -0.12f,
            directionZ = -0.82f
        )
        rimLightEntity = createDirectionalLight(
            engine = engine,
            colorR = 0.92f,
            colorG = 0.94f,
            colorB = 1.0f,
            intensity = 16_000.0f,
            directionX = -0.70f,
            directionY = -0.10f,
            directionZ = -0.70f
        )

        if (indirectLight == null) {
            indirectLight = IndirectLight.Builder()
                .irradiance(1, floatArrayOf(0.46f, 0.46f, 0.46f))
                .intensity(16_000.0f)
                .build(engine)
        }
        modelViewer.scene.indirectLight = indirectLight
        modelViewer.scene.skybox = null
        AppLogger.i(
            TAG,
            "Applied fixed front lighting: " +
                "key=(0.00,-0.22,-0.98,175000), " +
                "fill=(0.55,-0.12,-0.82,52000), " +
                "rim=(-0.70,-0.10,-0.70,16000), " +
                "ibl=16000"
        )
    }

    private fun createDirectionalLight(
        engine: com.google.android.filament.Engine,
        colorR: Float,
        colorG: Float,
        colorB: Float,
        intensity: Float,
        directionX: Float,
        directionY: Float,
        directionZ: Float
    ): Int {
        val entity = EntityManager.get().create()
        LightManager.Builder(LightManager.Type.DIRECTIONAL)
            .color(colorR, colorG, colorB)
            .intensity(intensity)
            .direction(directionX, directionY, directionZ)
            .castShadows(false)
            .build(engine, entity)
        modelViewer.scene.addEntity(entity)
        return entity
    }

    private data class PosedBounds(
        val centerX: Double,
        val centerY: Double,
        val centerZ: Double,
        val halfExtentX: Double,
        val halfExtentY: Double,
        val halfExtentZ: Double
    )

    private enum class BoundsSource { MESH, BONES }

    /**
     * Bounds source for the camera fit. Always the mesh box.
     *
     * This used to compare the mesh box against the joint spread and prefer the joints when they
     * were much larger, on the theory that a tiny mesh box meant unskinned authoring-space
     * vertices. That theory does not hold once [applySafeUnitCubeTransform] has run: the unit-cube
     * transform scales the mesh but not the joints, so afterwards the two describe the same asset
     * in different units and are not comparable at all. The ratio is therefore always extreme -
     * Xbot reports 0.009 against 74 - and the heuristic always chose the joints, which put the
     * camera at distance 232 where the figure is entirely outside the frame.
     *
     * What is drawn is the mesh, and after the unit-cube transform its box is the one in the same
     * space as the rendered geometry. Verified on the phone: forcing MESH shows Xbot centred at
     * about 65% of the preview height, while BONES leaves an empty preview.
     */
    private fun resolveAutoBoundsSource(asset: com.google.android.filament.gltfio.FilamentAsset): BoundsSource {
        return BoundsSource.MESH
    }

    /** `asset.boundingBox`: the range Filament reports for the mesh it draws. */
    private fun resolveMeshBounds(asset: com.google.android.filament.gltfio.FilamentAsset): PosedBounds? {
        val halfExtent = asset.boundingBox.halfExtent
        val center = asset.boundingBox.center
        if (halfExtent.size < 3 || !halfExtent.all(::isFiniteFloat)) {
            return null
        }
        if (center.size < 3 || !center.all(::isFiniteFloat)) {
            return null
        }
        if (halfExtent.maxOrNull() ?: 0f <= 1e-6f) {
            return null
        }
        return PosedBounds(
            centerX = center[0].toDouble(),
            centerY = center[1].toDouble(),
            centerZ = center[2].toDouble(),
            halfExtentX = halfExtent[0].toDouble(),
            halfExtentY = halfExtent[1].toDouble(),
            halfExtentZ = halfExtent[2].toDouble()
        )
    }

    /**
     * Returns the world bounds of the loaded model.
     *
     * Bone origins are used rather than `asset.boundingBox`: for a skinned mesh the latter reports
     * the unskinned vertex range, which for a humanoid is a few millimetres and frames the camera
     * inside the body. Joint world positions follow the pose and span the visible figure.
     *
     * This is only meaningful because the model is no longer forced into a unit cube; see
     * [applySafeUnitCubeTransform]. Keeping both the skeleton and the mesh in their authoring space
     * is what makes these numbers comparable to what is drawn.
     */
    private fun resolvePosedBounds(asset: com.google.android.filament.gltfio.FilamentAsset): PosedBounds? {
        val transformManager = modelViewer.engine.transformManager
        var minX = Double.MAX_VALUE
        var minY = Double.MAX_VALUE
        var minZ = Double.MAX_VALUE
        var maxX = -Double.MAX_VALUE
        var maxY = -Double.MAX_VALUE
        var maxZ = -Double.MAX_VALUE
        var counted = 0

        asset.entities.forEach { entity ->
            val instance = transformManager.getInstance(entity)
            if (instance == 0) {
                return@forEach
            }
            val transform = transformManager.getTransform(instance, FloatArray(16))
            if (!transform.all(::isFiniteFloat)) {
                return@forEach
            }
            val x = transform[12].toDouble()
            val y = transform[13].toDouble()
            val z = transform[14].toDouble()
            if (x < minX) minX = x
            if (y < minY) minY = y
            if (z < minZ) minZ = z
            if (x > maxX) maxX = x
            if (y > maxY) maxY = y
            if (z > maxZ) maxZ = z
            counted += 1
        }

        if (counted < 2 || minX > maxX) {
            return null
        }

        return PosedBounds(
            centerX = (minX + maxX) / 2.0,
            centerY = (minY + maxY) / 2.0,
            centerZ = (minZ + maxZ) / 2.0,
            halfExtentX = (maxX - minX) / 2.0,
            halfExtentY = (maxY - minY) / 2.0,
            halfExtentZ = (maxZ - minZ) / 2.0
        )
    }

    /**
     * Computes the camera distance that makes the whole model fit the viewport.
     *
     * Both the vertical and the horizontal half-angle are considered, because a portrait-shaped
     * preview crops a wide model sideways: the horizontal angle is the vertical one scaled by the
     * aspect ratio. The larger of the two required distances wins, so nothing is clipped on any
     * side. The result is stored in scene units and later scaled by the user's distance slider.
     */
    private fun updateFittedCameraDistance() {
        val asset = modelViewer.asset
        if (asset == null) {
            fittedCameraDistance = 0.0
            return
        }

        val tuner = GltfTuner.settings(context)
        val resolvedSource = when (tuner.boundsSource) {
            GltfTuner.BoundsSource.MESH -> BoundsSource.MESH
            GltfTuner.BoundsSource.BONES -> BoundsSource.BONES
            GltfTuner.BoundsSource.AUTO -> resolveAutoBoundsSource(asset)
        }

        val bounds = when (resolvedSource) {
            BoundsSource.BONES -> resolvePosedBounds(asset)
            BoundsSource.MESH -> resolveMeshBounds(asset)
        }
        if (bounds == null) {
            AppLogger.w(TAG, "Skip camera auto-fit: no usable bounds ($resolvedSource) for the loaded asset")
            fittedCameraDistance = 0.0
            return
        }

        cameraTargetX = tuner.targetX?.toDouble() ?: bounds.centerX
        cameraTargetY = tuner.targetY?.toDouble() ?: bounds.centerY
        cameraTargetZ = tuner.targetZ?.toDouble() ?: bounds.centerZ

        // The model can be yawed, so the largest horizontal extent is what must fit horizontally.
        val halfWidth = max(max(bounds.halfExtentX, bounds.halfExtentZ), 1e-4)
        val halfHeight = max(bounds.halfExtentY, 1e-4)
        val halfDepth = max(max(bounds.halfExtentZ, bounds.halfExtentX), 1e-4)

        val viewportWidth = max(width, 1).toDouble()
        val viewportHeight = max(height, 1).toDouble()
        val aspect = viewportWidth / viewportHeight

        val fovDegrees: Double = tuner.verticalFovDegrees?.toDouble() ?: CAMERA_VERTICAL_FOV_DEGREES
        val verticalHalfFov = Math.toRadians(fovDegrees / 2.0)
        val horizontalHalfFov = Math.atan(Math.tan(verticalHalfFov) * aspect)

        // The model is also rotated by pitch and yaw, so reserve for its depth as well: orbit
        // moves the silhouette towards the camera by up to halfDepth.
        val distanceForHeight = halfHeight / Math.tan(verticalHalfFov)
        val distanceForWidth = halfWidth / Math.tan(horizontalHalfFov)
        val fitTarget = max(distanceForHeight, distanceForWidth)

        val margin: Double = tuner.fitMargin?.toDouble() ?: CAMERA_FIT_MARGIN
        val distanceMultiplier: Double = tuner.distanceMultiplier?.toDouble() ?: 1.0
        val fitted = (fitTarget + halfDepth) * margin * distanceMultiplier
        fittedCameraDistance = if (!isFiniteFloat(fitted.toFloat()) || fitted <= 0.0) {
            BASE_CAMERA_DISTANCE.toDouble()
        } else {
            fitted
        }

        AppLogger.i(
            TAG,
            "Camera auto-fit[$resolvedSource]: halfExtent=${"%.4f".format(halfWidth)}, ${"%.4f".format(halfHeight)}, ${"%.4f".format(halfDepth)}, " +
                "centre=(${"%.4f".format(cameraTargetX)}, ${"%.4f".format(cameraTargetY)}, ${"%.4f".format(cameraTargetZ)}), " +
                "aspect=${"%.3f".format(aspect)}, distance=${"%.4f".format(fittedCameraDistance)}"
        )
    }

    /**
     * Measures the drawn silhouette once and reframes the camera on it.
     *
     * The asset bounds cannot be trusted for a skinned glTF: after
     * [applySafeUnitCubeTransform] the mesh lives in a unit cube while the joints stay in
     * authoring space, so `asset.boundingBox` and [resolvePosedBounds] describe two different
     * spaces, neither of which matches the pixels on screen. Measured proof is in TODO-AUDIT.md
     * under the slider reference: the logged bounds predict a figure at 57% of the preview height
     * while the phone shows about 9%.
     *
     * So the only dependable source is the rendered image itself. Filament renders one frame into
     * a Bitmap for us through ModelViewer, the background is transparent, and everything opaque is
     * the model. The silhouette then gives both the size correction and the vertical offset in one
     * pass, which is why this replaces guessing from geometry.
     *
     * Called after the first frame of a newly loaded model. It runs at most once per model, because
     * each call costs a full read-back and allocates a Bitmap.
     */
    private fun scheduleSilhouetteFraming() {
        framingLowScale = Float.NaN
        framingHighScale = Float.NaN
        scheduleSilhouetteFramingAttempt(SILHOUETTE_MAX_ATTEMPTS, SILHOUETTE_SETTLE_ITERATIONS)
    }

    /**
     * Takes one read-back, retries while the frame is still empty, then converges the framing.
     *
     * A model is not drawn on the very next frame after `loadModelGlb`: the resource loader
     * finishes textures asynchronously and `populateScene` only adds renderables once they are
     * ready. Measuring the first frame therefore produced "nothing opaque was drawn" on a real
     * device while the model appeared a moment later. Each retry waits for one more frame, and the
     * attempt count is small because by then the model is either ready or genuinely invisible.
     *
     * Once something is drawn, the frame is measured and the camera corrected, then another frame
     * is requested to measure the result. A single correction is not enough for models whose
     * authored bounds lie about their size: the relationship between distance and fill is only
     * linear while the whole figure stays inside the frame, and a model that is far too small on
     * the first frame needs several steps to settle. Each step re-registers itself from inside the
     * callback, the same way the empty-frame retry does, because that is the only registration
     * point that Filament services after `loadModelGlb`.
     */
    private fun scheduleSilhouetteFramingAttempt(attemptsLeft: Int, settleLeft: Int) {
        if (!hasValidSurfaceSize || width <= 0 || height <= 0 || attemptsLeft <= 0) {
            if (attemptsLeft <= 0) {
                AppLogger.w(TAG, "Silhouette framing gave up after ${SILHOUETTE_MAX_ATTEMPTS} frames")
            }
            return
        }
        modelViewer.debugGetNextFrameCallback { bitmap ->
            try {
                if (measureSilhouette(bitmap) == null) {
                    scheduleSilhouetteFramingAttempt(attemptsLeft - 1, settleLeft)
                    return@debugGetNextFrameCallback
                }
                val settled = applySilhouetteFraming(bitmap, settleLeft)
                if (!settled) {
                    scheduleSilhouetteFramingAttempt(attemptsLeft - 1, settleLeft - 1)
                }
            } catch (e: Exception) {
                AppLogger.w(TAG, "Silhouette framing failed: ${e.message}")
            }
        }
    }

    /**
     * Reframes the camera so the figure fills [SILHOUETTE_TARGET_FILL] of the viewport height and
     * sits in the middle, and reports whether the goal is reached.
     *
     * The measurement is the only trustworthy source of the model's on-screen size, because
     * `asset.boundingBox` and the posed joints both describe authoring spaces that do not match the
     * pixels for a skinned model (RobotExpressive reports a 2.4 half height while it draws at 11% of
     * the preview, and the fox draws nothing at all). A single correction is not enough: the
     * distance-to-fill relation is only its reciprocal while the whole figure stays inside the
     * frame, so a model that starts far too small needs several steps. Each step therefore returns
     * "not settled" and the caller measures again.
     *
     * The distance is scaled by the ratio between the current and target fill, bounded below so the
     * eye never enters the geometry (below that the near faces are clipped and the silhouette
     * shrinks instead of growing). The target is shifted by the pixel distance between the
     * silhouette centre and the viewport centre at the current distance, so the figure ends centred
     * even when its authored bounds centre lies somewhere else.
     *
     * @return true when the fill and the centre are within tolerance, false when another step is
     *   needed.
     */
    private fun applySilhouetteFraming(bitmap: android.graphics.Bitmap, settleLeft: Int): Boolean {
        val box = measureSilhouette(bitmap) ?: run {
            AppLogger.w(TAG, "Silhouette framing skipped: nothing opaque was drawn")
            return true
        }

        val viewportHeight = bitmap.height.toDouble()
        if (viewportHeight <= 0.0) {
            return true
        }

        val currentFill = box.heightPixels / viewportHeight
        if (currentFill <= 1e-4) {
            return true
        }

        val centreOffsetPixels = box.centerY - viewportHeight / 2.0
        val fillError = currentFill - SILHOUETTE_TARGET_FILL
        val fillSettled = abs(fillError) <= SILHOUETTE_FILL_TOLERANCE
        val centreSettled = abs(centreOffsetPixels) <= SILHOUETTE_CENTRE_TOLERANCE_PIXELS

        if ((fillSettled && centreSettled) || settleLeft <= 0) {
            AppLogger.i(
                TAG,
                "Silhouette framing settled: fill=${"%.3f".format(currentFill)} (target ${"%.3f".format(SILHOUETTE_TARGET_FILL)}), " +
                    "distanceScale=${"%.4f".format(cameraDistanceScale)}, " +
                    "centreOffsetPx=${"%.1f".format(centreOffsetPixels)}, " +
                    "silhouettePx=${box.heightPixels.toInt()}/${viewportHeight.toInt()}, stepsLeft=$settleLeft"
            )
            return true
        }

        // Only one quantity is corrected per step. Moving the distance changes both the fill and the
        // apparent centre (the pivot sits on the authored bounds centre, which is not the drawn
        // centre), so interleaving the two corrections made neither converge. The centre is fixed
        // first: a figure that hangs below the frame is clipped at the bottom, its measured height
        // saturates and the fill loop keeps shrinking the distance without ever reaching the target.
        // Once the figure is centred and fully in frame, the fill is reciprocal to the distance and
        // converges in a couple of steps.
        if (!centreSettled) {
            // One pixel at the current distance maps to 2 * distance * tan(vFov/2) / viewportHeight
            // scene units, independent of the authored bounds.
            val centreVisibleHalfHeight =
                kotlin.math.tan(Math.toRadians(CAMERA_VERTICAL_FOV_DEGREES / 2.0)) *
                    fittedCameraDistance * cameraDistanceScale.toDouble()
            val centreSceneUnitsPerPixel = (2.0 * centreVisibleHalfHeight) / viewportHeight
            cameraTargetY -= centreOffsetPixels * centreSceneUnitsPerPixel
            applyCameraPose()
            AppLogger.i(
                TAG,
                "Silhouette framing step(centre): centreOffsetPx=${"%.1f".format(centreOffsetPixels)}, " +
                    "fill=${"%.3f".format(currentFill)}, distanceScale=${"%.4f".format(cameraDistanceScale)}, " +
                    "targetY=${"%.4f".format(cameraTargetY)}, stepsLeft=$settleLeft"
            )
            return false
        }

        // The figure is centred; correct the fill. Update the bisection bracket from this
        // measurement: a fill above the target means the distance must grow, below means it must
        // shrink. The bracket is kept between frames because each step needs a rendered frame.
        if (currentFill > SILHOUETTE_TARGET_FILL) {
            // Too large: the scale that produced it is a lower bound (smaller scale = closer).
            if (framingLowScale.isNaN() || cameraDistanceScale > framingLowScale) {
                framingLowScale = cameraDistanceScale
            }
        } else {
            // Too small: this scale is an upper bound (larger scale = farther).
            if (framingHighScale.isNaN() || cameraDistanceScale < framingHighScale) {
                framingHighScale = cameraDistanceScale
            }
        }
        if (framingHighScale.isNaN()) {
            framingHighScale = MAX_CAMERA_DISTANCE_SCALE
        }
        if (framingLowScale.isNaN()) {
            framingLowScale = SILHOUETTE_MIN_BISECTION_SCALE
        }

        // Geometric midpoint, because the fill responds multiplicatively to the distance.
        val nextDistanceScale =
            kotlin.math.sqrt(framingLowScale.toDouble() * framingHighScale.toDouble()).toFloat()
        cameraDistanceScale = nextDistanceScale.coerceIn(MIN_CAMERA_DISTANCE_SCALE, MAX_CAMERA_DISTANCE_SCALE)
        onCameraDistanceScaleChanged?.invoke(cameraDistanceScale)
        applyCameraPose()
        AppLogger.i(
            TAG,
            "Silhouette framing step(fill): fill=${"%.3f".format(currentFill)} -> ${"%.3f".format(SILHOUETTE_TARGET_FILL)}, " +
                "bracket=[${"%.4f".format(framingLowScale)}, ${"%.4f".format(framingHighScale)}], " +
                "nextDistanceScale=${"%.4f".format(cameraDistanceScale)}, " +
                "silhouettePx=${box.heightPixels.toInt()}/${viewportHeight.toInt()}, stepsLeft=$settleLeft"
        )
        return false
    }
    private data class SilhouetteBox(val top: Double, val bottom: Double, val heightPixels: Double) {
        val centerY: Double get() = (top + bottom) / 2.0
    }

    /**
     * Returns the vertical extent of opaque pixels, or null when the frame holds nothing drawn.
     *
     * A pixel counts as model when its alpha is above a threshold. The surface is TRANSLUCENT, so
     * the background arrives fully transparent and the antialiased edges carry alpha in between;
     * the threshold keeps those edges out of the measurement without cutting into the body.
     */
    private fun measureSilhouette(bitmap: android.graphics.Bitmap): SilhouetteBox? {
        val width = bitmap.width
        val height = bitmap.height
        if (width <= 0 || height <= 0) {
            return null
        }
        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

        var top = -1
        var bottom = -1
        for (y in 0 until height) {
            val rowStart = y * width
            var opaqueCount = 0
            for (x in 0 until width) {
                val alpha = (pixels[rowStart + x] ushr 24) and 0xFF
                if (alpha >= SILHOUETTE_ALPHA_THRESHOLD) {
                    opaqueCount++
                }
            }
            // A handful of stray pixels is not the model; require a small run on the row.
            if (opaqueCount >= SILHOUETTE_MIN_PIXELS_PER_ROW) {
                if (top < 0) {
                    top = y
                }
                bottom = y
            }
        }

        if (top < 0 || bottom < 0) {
            return null
        }
        return SilhouetteBox(top = top.toDouble(), bottom = bottom.toDouble(), heightPixels = (bottom - top + 1).toDouble())
    }

    private fun applyCameraPose() {
        runCatching {
            val pitchRadians = Math.toRadians(cameraPitchDegrees.toDouble())
            val yawRadians = Math.toRadians(cameraYawDegrees.toDouble())
            val baseDistance = if (fittedCameraDistance > 0.0) {
                fittedCameraDistance
            } else {
                BASE_CAMERA_DISTANCE.toDouble()
            }
            val rawDistance = baseDistance * cameraDistanceScale.toDouble()
            val distance = if (abs(rawDistance) < CAMERA_DISTANCE_EPSILON) {
                CAMERA_DISTANCE_EPSILON
            } else {
                rawDistance
            }

            val targetY = cameraTargetY + cameraTargetHeightOffset.toDouble()

            val horizontalFactor = cos(pitchRadians)
            val eyeX = cameraTargetX + distance * horizontalFactor * sin(yawRadians)
            val eyeY = targetY + distance * sin(pitchRadians)
            val eyeZ = cameraTargetZ + distance * horizontalFactor * cos(yawRadians)

            val manipulatorApplied = applyManipulatorPose(
                eyeX = eyeX.toFloat(),
                eyeY = eyeY.toFloat(),
                eyeZ = eyeZ.toFloat(),
                targetX = cameraTargetX.toFloat(),
                targetY = targetY.toFloat(),
                targetZ = cameraTargetZ.toFloat()
            )

            modelViewer.camera.lookAt(
                eyeX,
                eyeY,
                eyeZ,
                cameraTargetX,
                targetY,
                cameraTargetZ,
                0.0,
                1.0,
                0.0
            )

            // The camera position above already orbits the target, so rotating the model as well
            // would apply pitch and yaw twice and throw it out of frame. The model-space rotation
            // exists only for devices where the manipulator cannot be installed.
            val disableModelOrbit = GltfTuner.settings(context).disableModelOrbit == true
            if (!manipulatorApplied && !disableModelOrbit) {
                applyModelOrbitFallback(pitchDegrees = cameraPitchDegrees, yawDegrees = cameraYawDegrees)
            }
        }.onFailure { error ->
            dispatchError("Failed to apply glTF camera: ${error.message ?: "unknown error"}")
        }
    }

    private fun applyManipulatorPose(
        eyeX: Float,
        eyeY: Float,
        eyeZ: Float,
        targetX: Float,
        targetY: Float,
        targetZ: Float
    ): Boolean {
        val field = cameraManipulatorField
        if (field == null) {
            if (!hasLoggedManipulatorUnavailable) {
                AppLogger.w(TAG, "Manipulator field unavailable, using root-transform fallback.")
                hasLoggedManipulatorUnavailable = true
            }
            return false
        }
        return runCatching {
            val viewportWidth = max(width, 1)
            val viewportHeight = max(height, 1)
            val manipulator = Manipulator.Builder()
                .viewport(viewportWidth, viewportHeight)
                .targetPosition(targetX, targetY, targetZ)
                .upVector(0f, 1f, 0f)
                .orbitHomePosition(eyeX, eyeY, eyeZ)
                .panning(false)
                .build(Manipulator.Mode.ORBIT)

            manipulator.jumpToBookmark(manipulator.homeBookmark)
            field.set(modelViewer, manipulator)

            val currentFieldValue = field.get(modelViewer)
            currentFieldValue === manipulator
        }.onFailure { error ->
            if (!hasLoggedManipulatorApplyFailure) {
                AppLogger.w(TAG, "Failed to apply manipulator pose: ${error.message}")
                hasLoggedManipulatorApplyFailure = true
            }
        }.getOrDefault(false)
    }

    private fun captureBaseRootTransform() {
        val asset = modelViewer.asset ?: run {
            baseRootTransform = null
            baseEntityTransforms = emptyMap()
            cameraTargetX = CAMERA_TARGET_X
            cameraTargetY = CAMERA_TARGET_Y
            cameraTargetZ = CAMERA_TARGET_Z
            return
        }

        val transformManager = modelViewer.engine.transformManager

        val rootEntity = asset.root
        val rootInstance = transformManager.getInstance(rootEntity)
        if (rootInstance != 0) {
            val rootTransform = transformManager.getTransform(rootInstance, FloatArray(16))
            val transformIsFinite = rootTransform.all(::isFiniteFloat)
            val tx = rootTransform[12]
            val tz = rootTransform[14]
            if (transformIsFinite && isFiniteFloat(tx) && isFiniteFloat(tz)) {
                baseRootTransform = rootTransform
                cameraTargetX = tx.toDouble()
                cameraTargetY = CAMERA_TARGET_Y
                cameraTargetZ = tz.toDouble()
            } else {
                baseRootTransform = null
                cameraTargetX = CAMERA_TARGET_X
                cameraTargetY = CAMERA_TARGET_Y
                cameraTargetZ = CAMERA_TARGET_Z
                AppLogger.w(TAG, "Skip invalid root transform after glTF load (non-finite values).")
            }
        } else {
            baseRootTransform = null
            cameraTargetX = CAMERA_TARGET_X
            cameraTargetY = CAMERA_TARGET_Y
            cameraTargetZ = CAMERA_TARGET_Z
        }

        val captured = LinkedHashMap<Int, FloatArray>()
        asset.entities.forEach { entity ->
            val instance = transformManager.getInstance(entity)
            if (instance != 0) {
                val transform = transformManager.getTransform(instance, FloatArray(16))
                if (transform.all(::isFiniteFloat)) {
                    captured[entity] = transform
                }
            }
        }
        baseEntityTransforms = captured
    }

    private fun applySafeUnitCubeTransform() {
        val asset = modelViewer.asset ?: return

        // A skinned model is left in its authoring space on purpose. transformToUnitCube scales the
        // mesh but not the joints, so afterwards the drawn geometry and the bone positions describe
        // two different spaces: the camera was framed from one and the mesh rendered in the other,
        // which put the camera inside the model. The camera fit reads bone positions, so the mesh
        // has to stay in that same space.
        // transformToUnitCube stays on for skinned models as well. Suppressing it to keep the mesh
        // in the same space as its joints sounded right, but it made the model vanish: the renderer
        // draws nothing usable when a skinned mesh is left in authoring scale. The unit-cube
        // transform is what puts the geometry where the camera can see it.
        val tuner = GltfTuner.settings(context)
        if (tuner.skipUnitCube == true) {
            AppLogger.i(TAG, "Skip transformToUnitCube: requested by tuner")
            return
        }

        val halfExtent = asset.boundingBox.halfExtent
        val maxHalfExtent = halfExtent.maxOrNull() ?: 0f
        val maxExtent = maxHalfExtent * 2f
        if (!isFiniteFloat(maxExtent) || maxExtent <= 1e-6f) {
            AppLogger.w(
                TAG,
                "Skip transformToUnitCube due to invalid bounds: halfExtent=${halfExtent.joinToString()}"
            )
            runCatching { modelViewer.clearRootTransform() }
            return
        }
        runCatching { modelViewer.transformToUnitCube() }
            .onFailure { error ->
                AppLogger.w(TAG, "transformToUnitCube failed: ${error.message}")
                runCatching { modelViewer.clearRootTransform() }
            }
    }

    private fun applyModelOrbitFallback(pitchDegrees: Float, yawDegrees: Float) {
        val asset = modelViewer.asset ?: return
        val transformManager = modelViewer.engine.transformManager

        val rotationY = createRotationYMatrix((-yawDegrees).toDouble())
        val rotationX = createRotationXMatrix((-pitchDegrees).toDouble())
        val rotation = multiplyMat4(rotationY, rotationX)
        val pivotedRotation = buildPivotedRotation(rotation)

        val base = baseRootTransform
        if (base != null) {
            val rootInstance = transformManager.getInstance(asset.root)
            if (rootInstance != 0) {
                val composed = multiplyMat4(base, pivotedRotation)
                transformManager.setTransform(rootInstance, composed)
                return
            }
        }

        if (baseEntityTransforms.isNotEmpty()) {
            baseEntityTransforms.forEach { (entity, transform) ->
                val instance = transformManager.getInstance(entity)
                if (instance != 0) {
                    val composed = multiplyMat4(transform, pivotedRotation)
                    transformManager.setTransform(instance, composed)
                }
            }
        }
    }

    private fun buildPivotedRotation(rotation: FloatArray): FloatArray {
        // Orbit around the same point the camera looks at, otherwise a model whose origin is at its
        // feet swings out of frame as soon as the user changes pitch or yaw.
        val pivotY = cameraTargetY.toFloat()
        val pivotZ = cameraTargetZ.toFloat()
        val translateToPivot = createTranslationMatrix(0f, pivotY, pivotZ)
        val translateBack = createTranslationMatrix(0f, -pivotY, -pivotZ)
        return multiplyMat4(translateToPivot, multiplyMat4(rotation, translateBack))
    }

    private fun createRotationXMatrix(degrees: Double): FloatArray {
        val radians = Math.toRadians(degrees)
        val c = cos(radians).toFloat()
        val s = sin(radians).toFloat()

        return floatArrayOf(
            1f, 0f, 0f, 0f,
            0f, c, s, 0f,
            0f, -s, c, 0f,
            0f, 0f, 0f, 1f
        )
    }

    private fun createRotationYMatrix(degrees: Double): FloatArray {
        val radians = Math.toRadians(degrees)
        val c = cos(radians).toFloat()
        val s = sin(radians).toFloat()

        return floatArrayOf(
            c, 0f, -s, 0f,
            0f, 1f, 0f, 0f,
            s, 0f, c, 0f,
            0f, 0f, 0f, 1f
        )
    }

    private fun createTranslationMatrix(tx: Float, ty: Float, tz: Float): FloatArray {
        return floatArrayOf(
            1f, 0f, 0f, 0f,
            0f, 1f, 0f, 0f,
            0f, 0f, 1f, 0f,
            tx, ty, tz, 1f
        )
    }

    private fun multiplyMat4(left: FloatArray, right: FloatArray): FloatArray {
        val result = FloatArray(16)
        for (column in 0 until 4) {
            for (row in 0 until 4) {
                var sum = 0f
                for (k in 0 until 4) {
                    sum += left[k * 4 + row] * right[column * 4 + k]
                }
                result[column * 4 + row] = sum
            }
        }
        return result
    }

    private fun resolveResourceFile(baseDirectory: File?, relativeUri: String): File {
        val trimmedUri = relativeUri.trim()
        require(trimmedUri.isNotEmpty()) { "Empty glTF resource URI." }

        val normalizedUri = normalizeFileResourceUri(trimmedUri)
        val candidate = if (File(normalizedUri).isAbsolute) {
            File(normalizedUri)
        } else {
            File(baseDirectory, normalizedUri)
        }

        val canonicalFile = candidate.canonicalFile
        require(canonicalFile.exists() && canonicalFile.isFile) {
            "Missing glTF resource: uri=$relativeUri resolved=${canonicalFile.absolutePath}"
        }

        return canonicalFile
    }

    private fun normalizeFileResourceUri(uri: String): String {
        val withoutFragment = uri.substringBefore('#')
        val withoutQuery = withoutFragment.substringBefore('?')
        val normalizedSeparators = withoutQuery
            .replace("\\/", "/")
            .replace('\\', '/')
        val decoded = Uri.decode(normalizedSeparators).trim()
        if (decoded.startsWith("file://", ignoreCase = true)) {
            val parsed = Uri.parse(decoded)
            val parsedPath = parsed.path
            if (!parsedPath.isNullOrBlank()) {
                return parsedPath
            }
            return decoded.removePrefix("file://")
        }
        return decoded
    }

    private fun readResourceToDirectByteBuffer(baseDirectory: File?, relativeUri: String): ByteBuffer {
        val trimmedUri = relativeUri.trim()
        require(trimmedUri.isNotEmpty()) { "Empty glTF resource URI." }

        validateResourceCompatibility(trimmedUri)

        if (trimmedUri.startsWith("data:", ignoreCase = true)) {
            val decoded = decodeDataUriToDirectByteBuffer(trimmedUri)
            AppLogger.i(TAG, "Resolved glTF data URI resource: length=${trimmedUri.length}, bytes=${decoded.remaining()}")
            return decoded
        }

        val resourceFile = resolveResourceFile(baseDirectory, trimmedUri)
        val buffer = readFileToDirectByteBuffer(resourceFile)
        AppLogger.i(
            TAG,
            "Resolved glTF file resource: uri=$trimmedUri path=${resourceFile.absolutePath} bytes=${buffer.remaining()}"
        )
        return buffer
    }

    private fun decodeDataUriToDirectByteBuffer(dataUri: String): ByteBuffer {
        val separatorIndex = dataUri.indexOf(',')
        require(separatorIndex >= 0) { "Malformed glTF data URI: missing payload separator." }

        val metadata = dataUri.substring(0, separatorIndex)
        val payload = dataUri.substring(separatorIndex + 1)
        val bytes = if (metadata.contains(";base64", ignoreCase = true)) {
            Base64.decode(payload, Base64.DEFAULT)
        } else {
            Uri.decode(payload).toByteArray(Charsets.UTF_8)
        }

        require(bytes.isNotEmpty()) { "Malformed glTF data URI: empty payload." }
        return bytes.toDirectByteBuffer()
    }

    private fun readFileToDirectByteBuffer(file: File): ByteBuffer {
        RandomAccessFile(file, "r").use { raf ->
            val channel = raf.channel
            val size = channel.size()
            require(size > 0L) { "Empty glTF resource file: ${file.absolutePath}" }
            require(size <= Int.MAX_VALUE.toLong()) { "glTF resource too large: ${file.absolutePath}" }
            return channel
                .map(FileChannel.MapMode.READ_ONLY, 0, size)
                .order(ByteOrder.nativeOrder())
        }
    }

    private fun ByteArray.toDirectByteBuffer(): ByteBuffer {
        return ByteBuffer.allocateDirect(size)
            .order(ByteOrder.nativeOrder())
            .apply {
                put(this@toDirectByteBuffer)
                flip()
            }
    }

    private fun isFiniteFloat(value: Float): Boolean {
        return !(value.isNaN() || value.isInfinite())
    }

    private fun validateResourceCompatibility(resourceUri: String) {
        val mimeHint = canonicalizeImageMimeType(gltfResourceMimeHints[resourceUri].orEmpty())
        if (mimeHint.isNotEmpty() && mimeHint !in SUPPORTED_TEXTURE_MIME_TYPES) {
            throw IllegalArgumentException(
                "Unsupported glTF image mimeType: $mimeHint (uri=$resourceUri). Supported: png/jpeg/webp/ktx2."
            )
        }

        if (resourceUri.startsWith("data:", ignoreCase = true)) {
            val metadata = resourceUri.substringBefore(',')
            val mimeInDataUri = canonicalizeImageMimeType(
                metadata
                .removePrefix("data:")
                .substringBefore(';')
                .trim()
            )
            if (mimeInDataUri.startsWith("image/") && mimeInDataUri !in SUPPORTED_TEXTURE_MIME_TYPES) {
                throw IllegalArgumentException(
                    "Unsupported glTF image data URI mimeType: $mimeInDataUri. Supported: png/jpeg/webp/ktx2."
                )
            }
            return
        }

        val normalized = normalizeFileResourceUri(resourceUri)
        val ext = File(normalized).extension.lowercase()
        if (ext in UNSUPPORTED_TEXTURE_EXTENSIONS) {
            throw IllegalArgumentException(
                "Unsupported glTF image extension: .$ext (uri=$resourceUri). Convert to png/jpeg/webp/ktx2."
            )
        }
    }

    private fun decodeDataUriToByteArray(dataUri: String): ByteArray {
        val separatorIndex = dataUri.indexOf(',')
        require(separatorIndex >= 0) { "Malformed glTF data URI: missing payload separator." }

        val metadata = dataUri.substring(0, separatorIndex)
        val payload = dataUri.substring(separatorIndex + 1)
        return if (metadata.contains(";base64", ignoreCase = true)) {
            Base64.decode(payload, Base64.DEFAULT)
        } else {
            Uri.decode(payload).toByteArray(Charsets.UTF_8)
        }
    }

    private fun canonicalizeImageMimeType(rawMimeType: String): String {
        val normalized = rawMimeType
            .trim()
            .replace("\\/", "/")
            .lowercase()
        return when (normalized) {
            "image/jpg" -> "image/jpeg"
            else -> normalized
        }
    }

    private fun dispatchError(message: String) {
        if (message.isBlank()) {
            return
        }

        AppLogger.e(TAG, message)
        onRenderErrorListener?.invoke(message)
    }
}
