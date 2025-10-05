// 透かし選択UIコンポーネント - MainActivity.ktに追加するコード

// ===== インポート文に追加 =====
import com.oneplus.exifpatcher.watermark.parser.WatermarkStyleParser
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check

// ===== モデル名設定カードの後、Process buttonの前に追加 =====

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
                        
                        // Style chips in a scrollable row
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
                                    .joinToString(" ") { it.capitalize() }
                                
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
                }
            }
