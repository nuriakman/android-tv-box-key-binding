# Proje Kuralları (android-tv-box-key-binding)

Bu dosya, bu projede çalışan her geliştirici ve AI ajanı için bağlayıcı kuralları içerir.

## Proje Özeti

Android TV / Google TV box kumandalarındaki sabitlenmiş marka tuşlarını (Netflix, Prime Video, Disney+) **root'suz** yeniden atamak için geliştirilmiş bir proje. Vendor `SdtGlobalKeyHandler` sistem uygulamasının baktığı `custom_app_hotkey_package_1..4` ayarlarına görünmez mini-app'ler atanır; tuşa basılınca app açılır, **Shizuku** üzerinden `input keyevent` enjekte eder ve kapanır. Prime → YouTube'da geri sarma, Disney → ileri sarma, Netflix → seçilen uygulamayı açma.

- **Stack:** Java (Android), Python 3 (opsiyonel PC-daemon), Bash/ADB.
- **Gradle yok.** Derleme manuel araç zinciriyle yapılır: `javac` → `d8` → `aapt2 link` → `apksigner` (detay: `TEKNIK.md` §4).
- Bağımlılık: Shizuku API AAR'ları (`dev.rikka.shizuku:13.1.5`, Maven Central'dan indirilip `classes.jar` çıkarılır).
- Test cihazı: Skyworth UHD Google TV STB, Android 14 (`minSdk 23`, `targetSdk 34`).

## Dizin Yapısı

```text
apk/          Hazır APK'lar (seekback, seekfwd, shizuku-v13.6.0) — git'e commit'lenmiş binary'ler
src/seekback/ MainActivity.java + AndroidManifest.xml (Prime tuşu → DPAD_LEFT burst)
src/seekfwd/  MainActivity.java + AndroidManifest.xml (Disney tuşu → DPAD_RIGHT burst)
scripts/      tvseek_daemon.py — PC-daemon alternatifi (HTTP → adb input köprüsü)
README.md     Kullanıcı dokümanı (Türkçe)
README_EN.md  Kullanıcı dokümanı (İngilizce)
TEKNIK.md     Teknik doküman: vendor mekanizması, derleme, diyagramlar (Türkçe)
TECHNICAL.md  Teknik doküman (İngilizce)
```

## Mimari Notlar

- Her mini-app tek `Activity`'dir: `Theme.NoDisplay` (pencere açmaz), `onCreate` içinde `finish()` **zorunludur** (aksi halde `IllegalStateException`).
- Basış sayacı `SharedPreferences` ile tutulur; 1000 ms pencerede ardışık basışlar sayılır, son basıştan sonra `cnt × input keyevent 21/22` + `126 (MEDIA_PLAY)` tek `sh -c` komutuyla gönderilir.
- Komut başındaki `dumpsys activity activities | grep topResumed | grep -q com.google.android.youtube.tv || exit 0` kontrolü tuşları yalnızca YouTube'da aktif tutar — bu guard kaldırılmamalı/değiştirilmemeli (kullanıcı onayı olmadan).
- `Shizuku.newProcess` v13 API'sinde private'tır; resmi pratik **reflection** ile çağırmaktır (`getDeclaredMethod` + `setAccessible`). Kodda bu bilinçli seçimdir, "düzeltme" olarak değiştirilmez.
- Manifest'te `rikka.shizuku.ShizukuProvider` provider'ı ve `moe.shizuku.manager.permission.API_V23` izni zorunludur; provider `authorities` değeri paket adına özeldir (`dev.nuri.seekback.shizuku` gibi) — yeni app eklenirse çakışmamalı.
- `seekback` ve `seekfwd` kodları neredeyse aynıdır; tek fark keycode (`21`↔`22`) ve paket adıdır. Birinde yapılan mantık değişikliği diğerine de uygulanmalıdır (iki dosya senkron tutulur).

## Build / Test

Derleme ortamı: JDK 17+, Android SDK `platforms/android-34/android.jar`, `build-tools/34.0.0` (`aapt2`, `d8.jar`, `apksigner`), debug keystore (`keytool`, parola `android`). Tam komut dizisi `TEKNIK.md` §4'te belgelenmiştir — oradaki pipeline'ı temel al, dokümanla çelişen komut üretme.

```bash
# Söz dizimi kontrolü (SDK gerektirmez)
javac -version

# Gerçek derleme → TEKNIK.md §4'teki javac/d8/aapt2/apksigner adımları
```

- Otomatik test altyapısı yoktur. Doğrulama, APK'nın box'a kurulup kumanda ile denenmesiyle yapılır.
- Build/derleme denemeleri ve `javac` çalıştırmaları **subagent** üzerinden yapılır.

## Cihaz / ADB Komut Kuralları

- **Read-only adb komutları** serbesttir: `adb devices`, `settings get`, `logcat | grep SdtGlobalKey`, `ps`, `dumpsys`.
- **Cihazın durumunu değiştiren komutlar** (`settings put/delete`, `adb install/uninstall`, `input keyevent`, monkey ile app başlatma, Shizuku starter komutu) **kullanıcı onayı olmadan çalıştırılmaz.** Bu komutlar gerçek TV box'ı etkiler.
- `scripts/tvseek_daemon.py` içindeki `BOX_IP`/`ADB_TARGET` sabitleri cihaza özeldir; değiştirmeden önce sorulur.

## Dokümantasyon Kuralları (Çift Dil — Zorunlu)

- Dokümanlar **Türkçe/İngilizce çiftli** tutulur: `README.md`↔`README_EN.md`, `TEKNIK.md`↔`TECHNICAL.md`.
- Bir dilde yapılan içerik değişikliği **diğer dile de yansıtılır**; iki sürüm senkron kalır.
- Markdown kod bloklarında dil belirteci zorunludur (`bash`, `java`, `python`, `mermaid`); dil yoksa `text` kullanılır.
- Diyagramlar `mermaid` ile yazılır; iki dildeki diyagram yapıları aynı kalır.

## Kod Konvansiyonları

- Java tarafı kasıtlı olarak minimal/kompakt yazılmıştır (tek dosya, az import, kısa isimler). Yeni özellik eklenirken bu sadelik korunur; gereksiz soyutlama/katman eklenmez (KISS/YAGNI).
- Yeni bir seek uygulaması eklenirse: `src/<ad>/MainActivity.java` + `AndroidManifest.xml`, paket `dev.nuri.<ad>`, Shizuku provider `authorities="dev.nuri.<ad>.shizuku"`.
- Tuş/slot eşlemesi ve keycode'lar box modeline özeldir; kodda değişiklik yapmadan önce kullanıcının cihazında `adb logcat | grep SdtGlobalKey` ile doğrulanır.
- Süreler (1000 ms pencere, `sleep 0.2` aralığı, 10 sn sarma adımı) iş kuralıdır; değişiklik kullanıcı onayı ister ve README/TEKNIK'te de güncellenir.

## Git Kuralları

- **KESİNLİKLE uzak depoya push yapılmaz** (`origin` GitHub'a tanımlı ama push yasak).
- Commit yalnızca kullanıcı onayıyla ve Türkçe mesajla atılır; farklı konular ayrı commit'lerde gruplanır.
- Taşıma işlemleri `git mv` ile yapılır.
- Kullanıcı söylemeden dosya silinmez; `apk/` altındaki binary'ler de silinmez.

## Genel Kurallar

- Tüm cevaplar, açıklamalar ve doküman içerikleri **Türkçe** olmalı (İngilizce doküman sürümleri hariç — onlar İngilizce kalır).
- Terminal örneklerinde `vi` kullanılır, `nano` değil.
- Sunucu/daemon ajan tarafından başlatılmaz (`tvseek_daemon.py` dahil); gerekirse kullanıcı başlatır.
- Varsayıma dayalı cevap verilmez; net olmayan nokta kullanıcıya sorulur.
- Uzun bekleme/`sleep`/polling döngüsü kurulmaz; koşul yoksa adım atlanır ve raporlanır.
