package com.notara.player.ui.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.notara.player.data.model.AudioEffectsState
import com.notara.player.data.model.EqPreset
import com.notara.player.data.model.ReverbOption
import kotlin.math.roundToInt

private val eqLabels = listOf("60 Hz", "230 Hz", "910 Hz", "3.6 kHz", "14 kHz")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AudioEffectsSheet(
    state: AudioEffectsState,
    onDismiss: () -> Unit,
    onVolumeChange: (Int) -> Unit,
    onLimiterChange: (Boolean) -> Unit,
    onSpeedChange: (Float) -> Unit,
    onPitchChange: (Float) -> Unit,
    onSkipSilenceChange: (Boolean) -> Unit,
    onEqEnabledChange: (Boolean) -> Unit,
    onPresetSelect: (EqPreset) -> Unit,
    onEqBandChange: (Int, Int) -> Unit,
    onBassBoostChange: (Int) -> Unit,
    onReverbSelect: (ReverbOption) -> Unit,
    onResetAll: () -> Unit
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Efek Audio & Volume Boost",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                TextButton(onClick = onResetAll) {
                    Text("Reset")
                }
            }

            // 1. Volume Boost hingga 200%
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Volume Booster (0% - 200%)", fontWeight = FontWeight.SemiBold)
                    Text(
                        text = "${state.volumePercent}%",
                        fontWeight = FontWeight.Bold,
                        color = if (state.volumePercent > 130) Color(0xFFF59E0B) else MaterialTheme.colorScheme.primary
                    )
                }
                Slider(
                    value = state.volumePercent.toFloat(),
                    onValueChange = { onVolumeChange(it.roundToInt()) },
                    valueRange = 0f..200f
                )
                if (state.volumePercent > 130) {
                    Text(
                        text = "Boost di atas 130% aktif. Pastikan Limiter menyala agar suara tidak pecah.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFF59E0B)
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Limiter Anti-Clipping (Android 9+)", style = MaterialTheme.typography.bodyMedium)
                    Switch(checked = state.limiterEnabled, onCheckedChange = onLimiterChange)
                }
            }

            HorizontalDivider()

            // 2. Speed, Pitch, Skip Silence
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Kecepatan Putar (Speed)", fontWeight = FontWeight.SemiBold)
                    Text("%.2fx".format(state.speed), fontWeight = FontWeight.Medium)
                }
                Slider(
                    value = state.speed,
                    onValueChange = { onSpeedChange((it * 20).roundToInt() / 20f) },
                    valueRange = 0.5f..2.0f
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Nada Suara (Pitch)", fontWeight = FontWeight.SemiBold)
                    Text("%.2fx".format(state.pitch), fontWeight = FontWeight.Medium)
                }
                Slider(
                    value = state.pitch,
                    onValueChange = { onPitchChange((it * 20).roundToInt() / 20f) },
                    valueRange = 0.5f..2.0f
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Lewati Bagian Hening (Skip Silence)", style = MaterialTheme.typography.bodyMedium)
                    Switch(checked = state.skipSilence, onCheckedChange = onSkipSilenceChange)
                }
            }

            HorizontalDivider()

            // 3. Equalizer 5-Band
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Equalizer 5-Band", fontWeight = FontWeight.SemiBold)
                    Switch(checked = state.eqEnabled, onCheckedChange = onEqEnabledChange)
                }

                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    EqPreset.entries.forEach { preset ->
                        FilterChip(
                            selected = state.eqEnabled && state.eqPreset == preset,
                            onClick = { onPresetSelect(preset) },
                            label = { Text(preset.label) }
                        )
                    }
                }

                if (state.eqEnabled) {
                    state.eqBandLevelsDb.forEachIndexed { idx, db ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = eqLabels.getOrElse(idx) { "B$idx" },
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(end = 10.dp)
                            )
                            Slider(
                                value = db.toFloat(),
                                onValueChange = { onEqBandChange(idx, it.roundToInt()) },
                                valueRange = -10f..10f,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = "${if (db > 0) "+" else ""}${db}dB",
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(start = 8.dp)
                            )
                        }
                    }
                }
            }

            HorizontalDivider()

            // 4. Bass Boost & Reverb
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Bass Boost", fontWeight = FontWeight.SemiBold)
                    Text("${state.bassBoostStrength / 10}%")
                }
                Slider(
                    value = state.bassBoostStrength.toFloat(),
                    onValueChange = { onBassBoostChange(it.roundToInt()) },
                    valueRange = 0f..1000f
                )

                Text("Efek Ruang (Reverb)", fontWeight = FontWeight.SemiBold)
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ReverbOption.entries.forEach { option ->
                        FilterChip(
                            selected = state.reverb == option,
                            onClick = { onReverbSelect(option) },
                            label = { Text(option.label) }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}
