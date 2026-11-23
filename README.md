# Notara — Pemutar Audio Lokal Android

**Notara** adalah aplikasi pemutar audio lokal Android (100% offline, tanpa izin `INTERNET`) yang terinspirasi dari estetika Material 3 Expressive [ArchiveTune](https://github.com/rukamori/ArchiveTune), dilengkapi fitur penanda detik ala SoundCloud, komentar per segmen, transkrip teks tersinkronisasi, efek suara, dan penguat volume hingga 200%.

## Fitur Utama

- **100% Pemutar File Lokal**: Memindai audio di perangkat melalui `MediaStore.Audio` lengkap dengan filter folder dan pencarian cepat.
- **Penanda Detik & Komentar ala SoundCloud**: Waveform interaktif dengan pin warna-warni di detik tertentu. Ketuk pin untuk langsung melompat ke detik tersebut beserta komentarnya.
- **Transkrip Teks & Mode Tap-to-Sync**: Mendukung impor file `.lrc` dan `.srt`, tempel teks manual, auto-scroll baris aktif ala lirik, serta mode **Tap-to-Sync** untuk menyinkronkan teks polos ke detik audio.
- **Efek Pemutar Suara (Audio DSP)**: Pengaturan kecepatan (0.5x–2.0x), pitch (0.5x–2.0x), *Skip Silence*, Equalizer 5-band dengan preset, Bass Boost, dan Reverb.
- **Volume Boost hingga 200% + Limiter**: Menggunakan `LoudnessEnhancer` dipadukan dengan `DynamicsProcessing.Limiter` (Android 9+) untuk mencegah suara pecah (*clipping*).
- **Backup & Ekspor Lokal**: Ekspor/impor penanda komentar sebagai `.json` dan transkrip sebagai `.lrc`.
