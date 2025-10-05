package com.oneplus.exifpatcher

import android.app.Application
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Shader
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.oneplus.exifpatcher.data.ImageRepository
import com.oneplus.exifpatcher.data.PresetRepository
import com.oneplus.exifpatcher.watermark.HasselbladWatermarkManager
import com.oneplus.exifpatcher.watermark.model.CameraInfo
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch

/**
 * UI State for the main screen
 */
data class MainUiState(
    val selectedImages: List<Uri> = emptyList(),
    val destinationUri: Uri? = null,
    val customModelName: String = "",
    val modelPresets: List<String> = emptyList(),
    val selectedWatermarkStyle: String? = null,  // 選択された透かしスタイルID
    val watermarkCategory: String = "All",  // 選択されたカテゴリ
    val isProcessing: Boolean = false,
    val processingProgress: Pair<Int, Int>? = null,
    val successMessage: String? = null,
    val errorMessage: String? = null,
    val previewBitmap: Bitmap? = null,
    val isPreviewLoading: Boolean = false,
    val previewErrorMessage: String? = null
)

/**
 * ViewModel for managing the main screen state and business logic
 */
class MainViewModel(application: Application) : AndroidViewModel(application) {
    
    private val repository = ImageRepository(application)
    private val presetRepository = PresetRepository(application)
    private val context = application.applicationContext
    private val watermarkManager = HasselbladWatermarkManager(application)
    private val basePreviewCameraInfo = CameraInfo(
        aperture = "f/1.8",
        shutterSpeed = "1/125",
        iso = "ISO200",
        focalLength = "24mm",
        deviceName = "OnePlus 12"
    )
    private var previewJob: Job? = null
    
    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()
    
    init {
        // Load presets on initialization
        viewModelScope.launch {
            presetRepository.presets.collect { presets ->
                _uiState.update { it.copy(modelPresets = presets) }
            }
        }
        
        // Load last used destination URI
        loadLastDestinationUri()

        // 初期プレビューを生成
        refreshPreview()
    }
    
    /**
     * Load last used destination URI from preferences
     */
    private fun loadLastDestinationUri() {
        val lastUriString = presetRepository.getLastDestinationUri()
        if (lastUriString != null) {
            try {
                val uri = Uri.parse(lastUriString)
                // Try to take persistable permission if not already taken
                try {
                    context.contentResolver.takePersistableUriPermission(
                        uri,
                        android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION or 
                        android.content.Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                    )
                } catch (e: SecurityException) {
                    // Permission already exists or not available, continue anyway
                }
                _uiState.update { it.copy(destinationUri = uri) }
            } catch (e: Exception) {
                // Invalid URI, ignore
                e.printStackTrace()
            }
        }
    }
    
    /**
     * Update selected images
     */
    fun setSelectedImages(uris: List<Uri>) {
        _uiState.update { it.copy(selectedImages = uris, errorMessage = null) }
    }
    
    /**
     * Update destination URI
     */
    fun setDestinationUri(uri: Uri?) {
        _uiState.update { it.copy(destinationUri = uri, errorMessage = null) }
        // Save to preferences
        uri?.let {
            presetRepository.saveLastDestinationUri(it.toString())
        }
    }
    
    /**
     * Update custom model name
     */
    fun setCustomModelName(name: String) {
        _uiState.update { it.copy(customModelName = name, errorMessage = null) }
        refreshPreview()
    }
    
    /**
     * Process the selected images
     */
    fun processImages() {
        val currentState = _uiState.value
        
        // Validation
        if (currentState.selectedImages.isEmpty()) {
            _uiState.update { it.copy(errorMessage = "画像が選択されていません") }
            return
        }
        
        if (currentState.destinationUri == null) {
            _uiState.update { it.copy(errorMessage = "保存先が選択されていません") }
            return
        }
        
        // Start processing
        viewModelScope.launch {
            _uiState.update { 
                it.copy(
                    isProcessing = true, 
                    errorMessage = null,
                    successMessage = null,
                    processingProgress = Pair(0, currentState.selectedImages.size)
                )
            }
            
            val result = repository.processImages(
                imageUris = currentState.selectedImages,
                destinationUri = currentState.destinationUri,
                customModelName = currentState.customModelName.ifEmpty { null },
                watermarkStyleId = currentState.selectedWatermarkStyle,
                onProgress = { current, total ->
                    _uiState.update { it.copy(processingProgress = Pair(current, total)) }
                }
            )
            
            result.fold(
                onSuccess = { successCount ->
                    _uiState.update { 
                        it.copy(
                            isProcessing = false,
                            successMessage = "すべての画像を処理しました！ ($successCount 枚)",
                            processingProgress = null
                        )
                    }
                },
                onFailure = { error ->
                    _uiState.update { 
                        it.copy(
                            isProcessing = false,
                            errorMessage = "処理エラー: ${error.message}",
                            processingProgress = null
                        )
                    }
                }
            )
        }
    }
    
    /**
     * Clear messages
     */
    fun clearMessages() {
        _uiState.update { it.copy(successMessage = null, errorMessage = null) }
    }
    
    /**
     * Add a new model preset
     */
    fun addPreset(modelName: String) {
        if (modelName.isNotBlank()) {
            presetRepository.addPreset(modelName.trim())
        }
    }
    
    /**
     * Remove a model preset
     */
    fun removePreset(modelName: String) {
        presetRepository.removePreset(modelName)
    }
    
    /**
     * Set selected watermark style
     */
    fun setWatermarkStyle(styleId: String?) {
        _uiState.update { it.copy(selectedWatermarkStyle = styleId, errorMessage = null) }
        refreshPreview()
    }
    
    /**
     * Set watermark category filter
     */
    fun setWatermarkCategory(category: String) {
        _uiState.update { it.copy(watermarkCategory = category) }
    }

    private fun refreshPreview() {
        val currentState = _uiState.value
        val deviceName = currentState.customModelName.takeIf { it.isNotBlank() }
        val styleId = currentState.selectedWatermarkStyle

        previewJob?.cancel()
        _uiState.update { it.copy(isPreviewLoading = true, previewErrorMessage = null) }

        previewJob = viewModelScope.launch(Dispatchers.Default) {
            var generatedBitmap: Bitmap? = null
            try {
                generatedBitmap = generatePreviewBitmap(styleId, deviceName)
                ensureActive()
                val previewBitmap = generatedBitmap
                _uiState.update {
                    it.copy(
                        isPreviewLoading = false,
                        previewBitmap = previewBitmap,
                        previewErrorMessage = null
                    )
                }
                generatedBitmap = null
            } catch (c: CancellationException) {
                generatedBitmap?.takeIf { !it.isRecycled }?.recycle()
                generatedBitmap = null
                throw c
            } catch (t: Throwable) {
                generatedBitmap?.takeIf { !it.isRecycled }?.recycle()
                _uiState.update {
                    it.copy(
                        isPreviewLoading = false,
                        previewBitmap = null,
                        previewErrorMessage = t.localizedMessage ?: "透かしプレビューを生成できませんでした"
                    )
                }
            }
        }
    }

    private fun generatePreviewBitmap(styleId: String?, deviceName: String?): Bitmap {
        val baseBitmap = createPreviewBackgroundBitmap()
        val cameraInfo = basePreviewCameraInfo.copy(
            deviceName = deviceName ?: basePreviewCameraInfo.deviceName
        )

        if (styleId.isNullOrBlank()) {
            return baseBitmap
        }

        val rendered = watermarkManager.addWatermark(baseBitmap, cameraInfo, styleId)
        if (!baseBitmap.isRecycled) {
            baseBitmap.recycle()
        }
        return rendered
    }
}

private fun createPreviewBackgroundBitmap(
    width: Int = 1600,
    height: Int = 1200
): Bitmap {
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    val gradientPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        shader = LinearGradient(
            0f,
            0f,
            0f,
            height.toFloat(),
            Color.parseColor("#0F172A"),
            Color.parseColor("#020617"),
            Shader.TileMode.CLAMP
        )
    }
    canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), gradientPaint)

    val accentPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#2563EB")
        alpha = 96
    }
    val ridgePath = Path().apply {
        moveTo(0f, height * 0.72f)
        lineTo(width * 0.35f, height * 0.58f)
        lineTo(width * 0.68f, height * 0.72f)
        lineTo(width.toFloat(), height * 0.62f)
        lineTo(width.toFloat(), height.toFloat())
        lineTo(0f, height.toFloat())
        close()
    }
    canvas.drawPath(ridgePath, accentPaint)

    accentPaint.color = Color.parseColor("#34D399")
    accentPaint.alpha = 70
    val foregroundPath = Path().apply {
        moveTo(0f, height * 0.86f)
        lineTo(width * 0.25f, height * 0.74f)
        lineTo(width * 0.52f, height * 0.88f)
        lineTo(width * 0.85f, height * 0.78f)
        lineTo(width.toFloat(), height * 0.9f)
        lineTo(width.toFloat(), height.toFloat())
        lineTo(0f, height.toFloat())
        close()
    }
    canvas.drawPath(foregroundPath, accentPaint)

    val sunPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#FACC15")
        alpha = 160
    }
    canvas.drawCircle(width * 0.78f, height * 0.28f, height * 0.12f, sunPaint)

    val hazePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        alpha = 36
    }
    canvas.drawCircle(width * 0.28f, height * 0.32f, height * 0.2f, hazePaint)
    hazePaint.alpha = 24
    canvas.drawRect(0f, height * 0.9f, width.toFloat(), height.toFloat(), hazePaint)

    return bitmap
}
