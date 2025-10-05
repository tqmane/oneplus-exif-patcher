package com.oneplus.exifpatcher.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Repository for managing model name presets
 */
class PresetRepository(context: Context) {
    
    private val prefs: SharedPreferences = context.getSharedPreferences(
        "model_presets",
        Context.MODE_PRIVATE
    )
    
    private val _presets = MutableStateFlow(loadPresets())
    val presets: StateFlow<List<String>> = _presets.asStateFlow()
    
    /**
     * Load presets from SharedPreferences
     */
    private fun loadPresets(): List<String> {
        val presetsString = prefs.getString(PRESETS_KEY, null) ?: return emptyList()
        return if (presetsString.isEmpty()) {
            emptyList()
        } else {
            presetsString.split(DELIMITER)
        }
    }
    
    /**
     * Add a new preset
     */
    fun addPreset(modelName: String) {
        if (modelName.isBlank()) return
        
        val currentPresets = _presets.value.toMutableList()
        if (!currentPresets.contains(modelName)) {
            currentPresets.add(modelName)
            savePresets(currentPresets)
            _presets.value = currentPresets
        }
    }
    
    /**
     * Remove a preset
     */
    fun removePreset(modelName: String) {
        val currentPresets = _presets.value.toMutableList()
        if (currentPresets.remove(modelName)) {
            savePresets(currentPresets)
            _presets.value = currentPresets
        }
    }
    
    /**
     * Save presets to SharedPreferences
     */
    private fun savePresets(presets: List<String>) {
        prefs.edit()
            .putString(PRESETS_KEY, presets.joinToString(DELIMITER))
            .apply()
    }
    
    /**
     * Save last used destination URI
     */
    fun saveLastDestinationUri(uriString: String) {
        prefs.edit()
            .putString(LAST_DESTINATION_KEY, uriString)
            .apply()
    }
    
    /**
     * Get last used destination URI
     */
    fun getLastDestinationUri(): String? {
        return prefs.getString(LAST_DESTINATION_KEY, null)
    }
    
    companion object {
        private const val PRESETS_KEY = "model_presets"
        private const val LAST_DESTINATION_KEY = "last_destination_uri"
        private const val DELIMITER = "|||"
    }
}
