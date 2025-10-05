package com.oneplus.exifpatcher

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.oneplus.exifpatcher.ui.theme.OnePlusExifPatcherTheme

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

@OptIn(ExperimentalMaterial3Api::class)
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
