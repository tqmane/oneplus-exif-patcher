package com.oneplus.exifpatcher

import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.oneplus.exifpatcher.ui.theme.OnePlusExifPatcherTheme
import com.oneplus.exifpatcher.watermark.parser.WatermarkStyleParser

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            OnePlusExifPatcherTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainScreen()
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class, ExperimentalFoundationApi::class)
@Composable
fun MainScreen(viewModel: MainViewModel = viewModel()) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    
    // Photo picker launcher
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        if (uris.isNotEmpty()) {
            viewModel.setSelectedImages(uris)
        }
    }
    
    // Directory picker launcher
    val directoryPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        uri?.let {
            // Take persistable URI permission
            context.contentResolver.takePersistableUriPermission(
                it,
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            )
            viewModel.setDestinationUri(it)
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.app_name)) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // App description
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = stringResource(R.string.app_description),
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Device: 0xcdcc8c3fff\nUserComment: oplus_1048864\nMake: OnePlus",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            // Select images button
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = stringResource(R.string.select_images_description),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = { photoPickerLauncher.launch("image/*") },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !uiState.isProcessing
                    ) {
                        Text(stringResource(R.string.select_images))
                    }
                    if (uiState.selectedImages.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = stringResource(
                                R.string.images_selected,
                                uiState.selectedImages.size
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
            
            // Select destination button
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = stringResource(R.string.destination_description),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = { directoryPickerLauncher.launch(null) },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !uiState.isProcessing
                    ) {
                        Text(stringResource(R.string.select_destination))
                    }
                    uiState.destinationUri?.let { uri ->
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = stringResource(
                                R.string.destination_selected,
                                uri.lastPathSegment ?: uri.toString()
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
            
            // Model name configuration
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "モデル名の設定（オプション）",
                        style = MaterialTheme.typography.titleSmall
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "空欄の場合は元のモデル名を保持します",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    // Custom input field
                    var showAddButton by remember { mutableStateOf(false) }
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = uiState.customModelName,
                            onValueChange = { 
                                viewModel.setCustomModelName(it)
                                showAddButton = it.isNotBlank() && !uiState.modelPresets.contains(it.trim())
                            },
                            label = { Text("モデル名") },
                            placeholder = { Text("例: OnePlus 12") },
                            modifier = Modifier.weight(1f),
                            enabled = !uiState.isProcessing,
                            singleLine = true
                        )
                        
                        // Add preset button
                        if (showAddButton) {
                            FilledTonalButton(
                                onClick = {
                                    viewModel.addPreset(uiState.customModelName)
                                    showAddButton = false
                                },
                                enabled = !uiState.isProcessing,
                                modifier = Modifier.height(56.dp)
                            ) {
                                Text("保存")
                            }
                        }
                    }
                    
                    if (uiState.customModelName.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "設定: ${uiState.customModelName}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    
                    // Preset chips
                    if (uiState.modelPresets.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "保存済みプリセット（長押しで削除）:",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        // Display presets in a flow layout
                        var presetToDelete by remember { mutableStateOf<String?>(null) }
                        
                        androidx.compose.foundation.layout.FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            uiState.modelPresets.forEach { preset ->
                                FilterChip(
                                    selected = uiState.customModelName == preset,
                                    onClick = {
                                        viewModel.setCustomModelName(preset)
                                        showAddButton = false
                                    },
                                    label = { Text(preset) },
                                    enabled = !uiState.isProcessing,
                                    modifier = Modifier.combinedClickable(
                                        onClick = {
                                            viewModel.setCustomModelName(preset)
                                            showAddButton = false
                                        },
                                        onLongClick = {
                                            presetToDelete = preset
                                        }
                                    )
                                )
                            }
                        }
                        
                        // Delete confirmation dialog
                        presetToDelete?.let { preset ->
                            AlertDialog(
                                onDismissRequest = { presetToDelete = null },
                                title = { Text("プリセットを削除") },
                                text = { Text("「$preset」を削除しますか？") },
                                confirmButton = {
                                    TextButton(
                                        onClick = {
                                            viewModel.removePreset(preset)
                                            if (uiState.customModelName == preset) {
                                                viewModel.setCustomModelName("")
                                            }
                                            presetToDelete = null
                                        }
                                    ) {
                                        Text("削除")
                                    }
                                },
                                dismissButton = {
                                    TextButton(onClick = { presetToDelete = null }) {
                                        Text("キャンセル")
                                    }
                                }
                            )
                        }
                    }
                }
            }
            
            // Watermark selection
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "透かしスタイルの選択（オプション）",
                        style = MaterialTheme.typography.titleSmall
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Hasselblad風の透かしを画像に追加できます",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    // Category selection
                    val categories = listOf(
                        "All" to "すべて",
                        "Hasselblad" to "Hasselblad",
                        "Brand" to "ブランド",
                        "Film" to "フィルム",
                        "MasterSign" to "マスターサイン",
                        "RetroCamera" to "レトロカメラ",
                        "TextStyle" to "テキスト",
                        "VideoHasselblad" to "動画",
                        "Realme" to "Realme",
                        "Festival" to "フェスティバル"
                    )
                    
                    var expanded by remember { mutableStateOf(false) }
                    val selectedCategoryLabel = categories.find { it.first == uiState.watermarkCategory }?.second 
                        ?: "すべて"
                    
                    ExposedDropdownMenuBox(
                        expanded = expanded,
                        onExpandedChange = { expanded = !expanded && !uiState.isProcessing }
                    ) {
                        OutlinedTextField(
                            value = selectedCategoryLabel,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("カテゴリー") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(),
                            enabled = !uiState.isProcessing,
                            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors()
                        )
                        
                        ExposedDropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false }
                        ) {
                            categories.forEach { (value, label) ->
                                DropdownMenuItem(
                                    text = { Text(label) },
                                    onClick = {
                                        viewModel.setWatermarkCategory(value)
                                        expanded = false
                                    },
                                    contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                                )
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Style selection
                    val availableStyles = if (uiState.watermarkCategory == "All") {
                        WatermarkStyleParser.PresetStyles.getAllStyleIds()
                    } else {
                        WatermarkStyleParser.PresetStyles.getStylesByCategory(uiState.watermarkCategory)
                    }
                    
                    if (availableStyles.isNotEmpty()) {
                        Text(
                            text = "スタイル (${availableStyles.size}種類):",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        // Style chips in a flow layout
                        androidx.compose.foundation.layout.FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // "なし" option
                            FilterChip(
                                selected = uiState.selectedWatermarkStyle == null,
                                onClick = { viewModel.setWatermarkStyle(null) },
                                label = { Text("なし") },
                                enabled = !uiState.isProcessing,
                                leadingIcon = if (uiState.selectedWatermarkStyle == null) {
                                    { Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(18.dp)) }
                                } else null
                            )
                            
                            // Style options
                            availableStyles.forEach { styleId ->
                                val displayName = styleId
                                    .replace("watermark_master_", "")
                                    .replace("_", " ")
                                    .split(" ")
                                    .joinToString(" ") { it.replaceFirstChar { char -> char.uppercase() } }
                                
                                FilterChip(
                                    selected = uiState.selectedWatermarkStyle == styleId,
                                    onClick = { viewModel.setWatermarkStyle(styleId) },
                                    label = { Text(displayName, maxLines = 1) },
                                    enabled = !uiState.isProcessing,
                                    leadingIcon = if (uiState.selectedWatermarkStyle == styleId) {
                                        { Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(18.dp)) }
                                    } else null
                                )
                            }
                        }
                        
                        if (uiState.selectedWatermarkStyle != null) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "選択中: ${uiState.selectedWatermarkStyle}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    } else {
                        Text(
                            text = "このカテゴリーにはスタイルがありません",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    WatermarkPreviewSection(
                        previewBitmap = uiState.previewBitmap,
                        isLoading = uiState.isPreviewLoading,
                        errorMessage = uiState.previewErrorMessage,
                        selectedStyle = uiState.selectedWatermarkStyle
                    )
                }
            }
            
            // Process button
            FilledTonalButton(
                onClick = { viewModel.processImages() },
                modifier = Modifier.fillMaxWidth(),
                enabled = !uiState.isProcessing && 
                         uiState.selectedImages.isNotEmpty() && 
                         uiState.destinationUri != null
            ) {
                Text(
                    text = if (uiState.isProcessing) {
                        stringResource(R.string.processing)
                    } else {
                        stringResource(R.string.process_images)
                    }
                )
            }
            
            // Progress indicator
            if (uiState.isProcessing) {
                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        LinearProgressIndicator(
                            progress = uiState.processingProgress?.let { (current, total) ->
                                if (total > 0) current.toFloat() / total.toFloat() else 0f
                            } ?: 0f,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        uiState.processingProgress?.let { (current, total) ->
                            Text(
                                text = stringResource(R.string.processing_progress, current, total),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            }
            
            // Success message
            uiState.successMessage?.let { message ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Text(
                        text = message,
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
            
            // Error message
            uiState.errorMessage?.let { message ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Text(
                        text = message,
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
        }
    }
}

@Composable
private fun WatermarkPreviewSection(
    previewBitmap: Bitmap?,
    isLoading: Boolean,
    errorMessage: String?,
    selectedStyle: String?,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = "プレビュー",
            style = MaterialTheme.typography.titleSmall
        )
        Spacer(modifier = Modifier.height(8.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(4f / 3f)
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            when {
                isLoading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }

                previewBitmap != null -> {
                    Image(
                        bitmap = previewBitmap.asImageBitmap(),
                        contentDescription = "透かしプレビュー",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }

                errorMessage != null -> {
                    Text(
                        text = errorMessage,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(16.dp),
                        textAlign = TextAlign.Center
                    )
                }

                else -> {
                    Text(
                        text = "スタイルを選択するとプレビューが表示されます",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(16.dp),
                        textAlign = TextAlign.Center
                    )
                }
            }

            val label = selectedStyle ?: "なし"
            Text(
                text = "スタイル: $label",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(12.dp)
                    .background(
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
                        shape = RoundedCornerShape(12.dp)
                    )
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            )
        }
    }
}
