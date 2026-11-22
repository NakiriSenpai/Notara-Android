# Notara — Pemutar Audio Lokal Android

**Notara** adalah aplikasi pemutar audio lokal Android (100% offline, tanpa izin `INTERNET`) yang terinspirasi dari estetika Material 3 Expressive [ArchiveTune](https://github.com/rukamori/ArchiveTune), dilengkapi fitur penanda detik ala SoundCloud, komentar per segmen, transkrip teks tersinkronisasi, efek suara, dan penguat volume hingga 200%.

## Fitur Utama

- **100% Pemutar File Lokal**: Memindai audio di perangkat melalui `MediaStore.Audio` lengkap dengan filter folder dan pencarian cepat.
- **Penanda Detik & Komentar ala SoundCloud**: Waveform interaktif dengan pin warna-warni di detik tertentu. Ketuk pin untuk langsung melompat ke detik tersebut beserta komentarnya.
- **Transkrip Teks & Mode Tap-to-Sync**: Mendukung impor file `.lrc` dan `.srt`, tempel teks manual, auto-scroll baris aktif ala lirik, serta mode **Tap-to-Sync** untuk menyinkronkan teks polos ke detik audio.
- **Efek Pemutar Suara (Audio DSP)**: Pengaturan kecepatan (0.5x–2.0x), pitch (0.5x–2.0x), *Skip Silence*, Equalizer 5-band dengan preset, Bass Boost, dan Reverb.
- **Volume Boost hingga 200% + Limiter**: Menggunakan `LoudnessEnhancer` dipadukan dengan `DynamicsProcessing.Limiter` (Android 9+) untuk mencegah suara pecah (*clipping*).
- **Backup & Ekspor Lokal**: Ekspor/impor penanda komentar sebagai `.json` dan transkrip sebagai `.lrc`.

## Cara Build APK Menggunakan VS Code + GitHub Actions (Tanpa Android Studio)

1. Ekstrak `Notara-Android.zip` lalu buka foldernya di **VS Code**.
2. Buat repositori baru di GitHub (boleh **Private** maupun Public).
3. Buka terminal VS Code dan jalankan:
   ```bash
   git init
   git add .
   git commit -m "Initial Notara Android project"
   git branch -M main
   git remote add origin https://github.com/USERNAME_KAMU/notara-android.git
   git push -u origin main
   ```
4. Buka repositori di browser, masuk ke tab **Actions**, lalu klik workflow **Build Notara APK** yang berjalan otomatis (atau klik **Run workflow** secara manual).
5. Setelah status hijau (biasanya 2–4 menit), gulir ke bagian **Artifacts** dan unduh **Notara-Debug-APK**.
6. Ekstrak file zip artifact tersebut, pindahkan `app-debug.apk` ke HP Android (Android 9 / API 28 ke atas), lalu install.

## Struktur Proyek

```text
Notara-Android/
├── .github/workflows/build-apk.yml   # Workflow GitHub Actions pembuat APK otomatis
├── gradle/libs.versions.toml         # Versi dependensi (Kotlin 2.0.21, Media3 1.5.1, Room 2.6.1)
└── app/src/main/
    ├── AndroidManifest.xml           # Tanpa izin INTERNET (100% offline)
    └── java/com/notara/player/
        ├── MainActivity.kt           # Izin runtime, Dynamic Color Palette, SAF import/export
        ├── data/                     # LocalAudioScanner, Room DB, TranscriptParser, BackupExporter
        ├── playback/                 # NotaraPlaybackService, AudioDspController, WaveformExtractor
        └── ui/                       # LibraryScreen, PlayerScreen, WaveformSeekBar, AudioEffectsSheet
```
