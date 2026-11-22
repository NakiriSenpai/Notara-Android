package com.notara.player.data.model

enum class EqPreset(val label: String, val bandsDb: List<Int>) {
    FLAT("Flat", listOf(0, 0, 0, 0, 0)),
    VOCAL("Vokal Jernih", listOf(-2, 1, 4, 3, 1)),
    PODCAST("Podcast / Belajar", listOf(-3, 2, 5, 3, -1)),
    BASS_BOOST("Bass Mantap", listOf(5, 3, 0, 1, 2)),
    TREBLE("Treble Cerah", listOf(-1, 0, 1, 4, 5)),
    CUSTOM("Kustom", listOf(0, 0, 0, 0, 0))
}

enum class ReverbOption(val label: String, val presetValue: Short) {
    NONE("Mati", 0),
    SMALL_ROOM("Ruang Kecil", 1),
    MEDIUM_ROOM("Ruang Sedang", 2),
    LARGE_HALL("Aula Besar", 5),
    PLATE("Studio Plate", 6)
}

data class AudioEffectsState(
    val volumePercent: Int = 100, // 0..200%
    val limiterEnabled: Boolean = true,
    val speed: Float = 1.0f, // 0.5x..2.0x
    val pitch: Float = 1.0f, // 0.5x..2.0x
    val skipSilence: Boolean = false,
    val eqEnabled: Boolean = false,
    val eqPreset: EqPreset = EqPreset.FLAT,
    val eqBandLevelsDb: List<Int> = listOf(0, 0, 0, 0, 0), // -10..+10 dB
    val bassBoostStrength: Int = 0, // 0..1000
    val reverb: ReverbOption = ReverbOption.NONE
)
