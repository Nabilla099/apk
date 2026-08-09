# Kh-Loader — ringkasan perubahan dari JL-Mod

## 1. Rebrand
- `settings.gradle.kts`: nama project → `Kh-Loader` (dipakai sebagai `app_name`)
- `app/build.gradle.kts`: `applicationId` → `com.khloader.app`
- Palet warna baru (modern minimalis, aksen ungu `#7C5CFC`) di
  `values/colors.xml` & `values-night/colors.xml`
- Ikon launcher baru (adaptive icon vector, monogram "Kh") di
  `res/drawable/ic_launcher_foreground.xml` + fallback PNG di semua
  `mipmap-*dpi` untuk Android lawas (minSdk 14)
- Radius card & tombol diperbesar (`bg_config_card.xml`, `bg_button.xml`)
  supaya kesan lebih modern

## 2. Game log (mirip KEmulator)
- `ru.playsoftware.j2meloader.util.GameLogger` — pencatat log in-memory +
  file (`khloader_log.txt` di data dir MIDlet)
- Di-hook ke:
  - `Displayable.setTitle`, `Alert.setString`, `StringItem.setText`,
    `TextBox.setString` → mencatat semua teks dialog yang ditampilkan game
  - `AppClassLoader.getResourceAsStream/getResourceAsBytes` → mencatat
    setiap file resource yang diminta game (path lengkap + status ketemu)
  - `Image.createImage(String)` → mencatat path gambar + ukurannya
- `LogViewerActivity` (menu **View game log** saat main game) — lihat log
  langsung di app, share sebagai teks, atau clear
- Log tersimpan otomatis, bisa juga diambil manual dari file di atas

## 3. Cheat menu
- **Bukan** memory-scan mentah ala Cheat Engine/GameGuardian (itu perlu
  root + kode native untuk baca `/proc/pid/mem`, jauh di luar scope aman).
  Sebagai gantinya: `CheatEngine` menelusuri object graph MIDlet yang
  sedang berjalan lewat reflection (root: instance MIDlet + Displayable
  aktif), mengumpulkan semua field numerik/boolean/String, lalu
  mendukung alur "scan nilai → scan ulang untuk mempersempit → edit nilai"
  — perilakunya setara cheat engine, tanpa root.
- `CheatActivity` (menu **Cheat menu** saat main game): input nilai saat
  ini (misal HP/gold), "New scan", "Next scan" untuk mempersempit hasil,
  tap salah satu hasil untuk ubah nilainya langsung di game yang jalan.

## 4. Menu in-game
- `midlet_displayable.xml` menu baru: **View game log** & **Cheat menu**
- Kedua activity baru berjalan di proses `:midlet` yang sama dengan game
  supaya cheat engine bisa akses object MIDlet yang sedang aktif.

## 5. GitHub Actions
- `.github/workflows/android.yml` & `nightly.yml` ditulis ulang:
  - Build otomatis jalan **tanpa perlu setup keystore** (fallback ke
    APK debug bila secret `SIGNING_KEY`/`KEYSTORE_PROPERTIES` belum diisi)
  - Kalau secret sudah diisi di repo GitHub kamu (Settings → Secrets),
    otomatis build APK release bertanda tangan + draft GitHub Release
  - Nama artifact otomatis diberi prefix `Kh-Loader-<versi>-<sha>`

## Yang belum / bisa dikembangkan lagi
- Cheat engine saat ini hanya menelusuri field milik object MIDlet &
  Displayable aktif (kedalaman 6 level) — cukup untuk sebagian besar game
  J2ME sederhana, tapi field yang disimpan di static class lain yang
  tidak terhubung dari root tersebut belum ikut ter-scan otomatis.
- Desain home masih pakai layout asli J2ME-Loader, baru warna/bentuk yang
  diperbarui — kalau mau redesign layout total (bukan cuma tema), itu
  pekerjaan terpisah yang lebih besar.
- Belum sempat coba compile langsung (sandbox ini tidak ada akses
  internet buat download dependency Gradle) — build sebenarnya akan
  terjadi di GitHub Actions begitu kamu push. Kalau ada error compile,
  kirim log Actions-nya ke saya, saya bantu perbaiki.
