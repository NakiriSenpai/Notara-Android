package com.notara.player.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Backup
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Restore
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.notara.player.data.LibraryFilterConfig
import com.notara.player.ui.i18n.AppLanguage
import com.notara.player.ui.theme.AppThemePreset

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsSheet(
    themePreset: AppThemePreset,
    language: AppLanguage,
    filterConfig: LibraryFilterConfig,
    allFolders: List<String>,
    onOpenThemePicker: () -> Unit,
    onOpenLanguagePicker: () -> Unit,
    onMinDurationSelect: (Int) -> Unit,
    onToggleHideFolder: (String) -> Unit,
    onExportFullBackup: () -> Unit,
    onRestoreFullBackup: () -> Unit,
    onRescanLibrary: () -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = "Notara Settings",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            // Tema & Bahasa
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onDismiss(); onOpenThemePicker() }
                ) {
                    Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.Palette, null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text("Theme", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(themePreset.displayName, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, maxLines = 1)
                        }
                    }
                }
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onDismiss(); onOpenLanguagePicker() }
                ) {
                    Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.Language, null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text("Language", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(language.nativeName, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, maxLines = 1)
                        }
                    }
                }
            }

            HorizontalDivider()

            // Filter Durasi Minimum Audio
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Filter Durasi Minimum Audio", fontWeight = FontWeight.SemiBold)
                Text(
                    text = "Sembunyikan rekaman pendek atau nada notifikasi di bawah durasi ini.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(3, 15, 30, 60).forEach { sec ->
                        FilterChip(
                            selected = filterConfig.minDurationSec == sec,
                            onClick = { onMinDurationSelect(sec) },
                            label = { Text(">= ${sec}s") }
                        )
                    }
                }
            }

            // Sembunyikan Folder (Hide Folders)
            if (allFolders.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "Sembunyikan Folder (${filterConfig.hiddenFolders.size} disembunyikan)",
                        fontWeight = FontWeight.SemiBold
                    )
                    allFolders.take(12).forEach { folder ->
                        val isHidden = folder in filterConfig.hiddenFolders
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onToggleHideFolder(folder) }
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = folder,
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (isHidden) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
                            )
                            Switch(checked = isHidden, onCheckedChange = { onToggleHideFolder(folder) })
                        }
                    }
                }
            }

            HorizontalDivider()

            // Backup & Restore Penuh (1 File)
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Backup & Restore Penuh (1 File)", fontWeight = FontWeight.SemiBold)
                Text(
                    text = "Simpan seluruh playlist, favorit, pin + deskripsi, transkrip, dan filter folder ke 1 file JSON.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    FilledTonalButton(
                        onClick = { onDismiss(); onExportFullBackup() },
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Rounded.Backup, null)
                        Spacer(Modifier.width(6.dp))
                        Text("Backup")
                    }
                    OutlinedButton(
                        onClick = { onDismiss(); onRestoreFullBackup() },
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Rounded.Restore, null)
                        Spacer(Modifier.width(6.dp))
                        Text("Restore")
                    }
                }
            }

            OutlinedButton(
                onClick = { onDismiss(); onRescanLibrary() },
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Rounded.Refresh, null)
                Spacer(Modifier.width(6.dp))
                Text("Pindai Ulang Audio Lokal")
            }

            Spacer(Modifier.height(6.dp))
        }
    }
}
