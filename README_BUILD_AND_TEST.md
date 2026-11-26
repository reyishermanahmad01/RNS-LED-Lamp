# RNS (RGB LED Multi Controller) — Panduan Build APK & Pengujian

## 1. Struktur Proyek Android Native Kotlin
- `app/src/main/java/com/rns/controller/domain/model/RnsModels.kt`: Model modul (maks. 10 modul), driver (`WS2812B`, `WS2811`, `SK6812_RGB`, `SK6812_RGBW`, `ANALOG_RGB`), zona Kiri/Kanan, status koneksi nyata vs simulator.
- `app/src/main/java/com/rns/controller/domain/catalog/AnimationCatalog.kt`: 100 slot animasi LED (`001`–`100`).
- `app/src/main/java/com/rns/controller/data/protocol/RnsProtocolCodec.kt`: Enkoder paket JSON & biner 24-byte untuk UDP/HTTP/BLE.
- `app/src/main/java/com/rns/controller/data/protocol/RnsQrCodec.kt`: Codec URI/kode QR unik per modul (`rns://module?...` dan kode `RNS-MXX-XXXX`).
- `app/src/main/java/com/rns/controller/data/transport/WifiUdpTransport.kt`: Transportasi Wi-Fi UDP port 4210, HTTP `/api/status` & `/api/wifi` untuk ESP8266/ESP32 (AP & Router).
- `app/src/main/java/com/rns/controller/data/transport/Esp32BleTransport.kt`: Klien BLE GATT untuk ESP32.
- `app/src/main/java/com/rns/controller/data/simulator/LedSimulatorEngine.kt`: Engine simulator 100 animasi tanpa perangkat Wemos.
- `app/src/main/java/com/rns/controller/ui/screens/*`: Antarmuka Jetpack Compose Material 3 dengan 5 tab berbeda fungsi: `DashboardScreen` (Home), `ColorControlScreen` (Color), `AnimationsScreen` + `ZonesScreen` (Animation & Kanan-Kiri), `SimulatorScreen` (Simulator LED), dan `MultiControlScreen` (Device Scanner QR & 10 Modul).

## 2. Cara Build Menjadi APK Android
1. Unduh atau buka folder `android-rns` di **Android Studio Ladybug / Meerkat** (atau lebih baru) dengan **JDK 17**.
2. Biarkan Gradle melakukan sync (`libs.versions.toml` menggunakan AGP 8.7.2, Kotlin 2.0.21, Compose BOM 2024.11.00).
3. Jalankan unit test terlebih dahulu:
   ```bash
   ./gradlew testDebugUnitTest
   ```
4. Bangun APK Debug untuk pengujian di ponsel Android:
   ```bash
   ./gradlew assembleDebug
   ```
   Hasil APK berada di: `app/build/outputs/apk/debug/app-debug.apk`.
5. Bangun APK Release (siap distribusi setelah ditandatangani keystore):
   ```bash
   ./gradlew assembleRelease
   ```

## 3. Petunjuk Pengujian Hardware, QR Scanner & Simulator
1. **Home (Dashboard)**:
   - Menampilkan status modul aktif, tombol power cepat, ringkasan koneksi Wi-Fi AP/Router/BLE/Simulator, dan pintasan menuju fitur utama.
2. **Color Control**:
   - Mengatur RGB/RGBW via Color Wheel, tombol preset, slider kecerahan utama, serta pemilihan target Semua / Zona Kiri / Zona Kanan.
3. **Animation & Zona Kanan-Kiri**:
   - Memilih 100 slot animasi LED secara manual, mengaktifkan pergantian animasi otomatis, dan mengatur kombinasi lampu Zona Kiri & Zona Kanan secara terpisah.
4. **Simulator (Tanpa Perangkat Wemos)**:
   - Buka tab **Simulator** untuk memantau pratinjau dual-zone real-time 60fps beserta inspeksi paket 24-byte tanpa hardware fisik.
5. **Device Scanner & Multi-Control (Hingga 10 Modul)**:
   - Di tab **Scanner**, gunakan input/pemindai QR (`rns://module?...` atau kode unik `RNS-MXX-XXXX`) untuk menambahkan atau mengaktifkan modul dengan identitas unik, lihat QR tiap modul, atau scan ESP32 BLE & ESP8266 Wi-Fi AP/Router (`192.168.4.1`).
   - Pengaturan tiap modul (driver `WS2812B`, `WS2811`, `SK6812 RGB`, `SK6812 RGBW`, `Analog RGB`, GPIO pin, Wi-Fi AP/STA) tersimpan secara independen hingga 10 modul.

