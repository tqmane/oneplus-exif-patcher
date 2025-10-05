package com.oneplus.exifpatcher

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.oneplus.exifpatcher.data.ImageRepository
import com.oneplus.exifpatcher.data.PresetRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * UI State for the main screen
 */
data class MainUiState(
    val selectedImages: List<Uri> = emptyList(),
    val destinationUri: Uri? = null,
    val customModelName: String = "",
    val modelPresets: List<String> = emptyList(),
    val isProcessing: Boolean = false,
    val processingProgress: Pair<Int, Int>? = null,
    val successMessage: String? = null,
    val errorMessage: String? = null
)

/**
 * ViewModel for managing the main screen state and business logic
 */
class MainViewModel(application: Application) : AndroidViewModel(application) {
    
    private val repository = ImageRepository(application)
    private val presetRepository = PresetRepository(application)
    private val context = application.applicationContext
    
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
}
