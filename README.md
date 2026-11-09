# Android TV Box Kumanda Kısayol Tuşlarını Özelleştirme (Root'suz)

> **English:** [README_EN.md](README_EN.md) | Technical docs: [TECHNICAL.md](TECHNICAL.md) (EN) / [TEKNIK.md](TEKNIK.md) (TR)

Android TV / Google TV box kumandalarındaki sabitlenmiş **Netflix, YouTube, Disney+, Prime Video** hızlı tuşlarını istediğiniz uygulamaya veya işleve bağlayın — **root olmadan, PC'siz çalışır**.

Bu proje şunları yapar:

- **Netflix tuşu** → istediğiniz uygulamayı açar (örnekte: Televizo)
- **Prime Video tuşu** → YouTube'da **geri sarma** (N ardışık basış = N×10sn geri)
- **Disney+ tuşu** → YouTube'da **ileri sarma** (N ardışık basış = N×10sn ileri)

Skyworth UHD Google TV STB (Android 14) üzerinde geliştirilip test edilmiştir; aynı `SdtGlobalKey` mekanizmasını kullanan diğer box'larda da çalışması gerekir.

## Nasıl çalışıyor? (kısaca)

Bu box'larda hızlı tuşlar üreticinin `SdtGlobalKeyHandler` sistem uygulaması tarafından yakalanır ve `custom_app_hotkey_package_1..4` ayarlarına bakar. Biz o ayarlara **görünmez mini uygulamalarımızı** yazıyoruz; tuşa basınca uygulama açılır, [Shizuku](https://shizuku.rikka.app/) üzerinden `input keyevent` enjekte eder ve anında kapanır. Sarma işlemi YouTube'a gerçek D-pad tuşu olarak ulaştığı için medya oturumu gerektirmez. Kurulumdan sonra PC gerekmez — her şey box içinde döner.

Akışın özeti:

1. Kumandadaki marka tuşu vendor broadcast'i olarak gelir.
2. Slot ayarındaki görünmez mini-app başlar ve **1 saniyelik pencerede** ardışık basışları sayar.
3. Ön plan uygulaması YouTube değilse işlem iptal edilir — tuşlar **yalnızca YouTube'da** aktiftir.
4. Shizuku, shell yetkisiyle `input keyevent` enjekte eder; YouTube bunu gerçek kumanda basışı gibi algılayıp sarar.

Sistem diyagramları (mermaid): [TEKNIK.md](TEKNIK.md)

## Gereksinimler

- Android TV box (ADB/network erişimi açık olabilen)
- Aynı ağda bir bilgisayar (sadece kurulum için)
- `adb` kurulu (`sudo apt install adb`)

## Kurulum — adım adım

### 1. Box'ta USB hata ayıklamayı açın

`Ayarlar → Sistem → Hakkında → Android TV OS derlemesi` üzerinde OK'a 7 kez basın → geliştirici modu açılır. Sonra `Geliştirici seçenekleri → USB hata ayıklama` açın.

### 2. Box'ın IP'sini bulun ve bağlanın

```bash
# Ağdaki cihazı bulun (box genelde tek Android cihazdır)
nmap -p 8008,8443 --open 192.168.1.0/24

# Bağlanın — TV'de çıkan izin penceresinde "her zaman izin ver" işaretleyin
adb connect 192.168.1.101:5555
adb devices   # "device" görmelisiniz
```

### 3. Shizuku kurun ve başlatın

```bash
# Shizuku APK'sını kurun (apk/shizuku-v13.6.0.apk veya resmi siteden)
adb install apk/shizuku-v13.6.0.apk

# Uygulamayı bir kez açın
adb shell monkey -p moe.shizuku.privileged.api 1
```

Shizuku uygulamasını açın → **"Bilgisayara bağlanarak başlatın" → "Komutu görüntüle"** — gösterdiği komutu kopyalayıp çalıştırın (şuna benzer):

```bash
adb shell /data/app/~~XXXX==/moe.shizuku.privileged.api---XXXX==/lib/arm/libshizuku.so
```

`shizuku_server` çalışmaya başlar. **Box her yeniden başladığında bu komut bir kez tekrarlanmalı.**

### 4. Mini uygulamaları kurun

```bash
adb install apk/seekback.apk   # Prime tuşu → geri sarma
adb install apk/seekfwd.apk    # Disney tuşu → ileri sarma
```

### 5. Tuşları atayın

```bash
# Prime Video tuşu (keycode 138, slot 3) → geri sarma
adb shell settings put global custom_app_hotkey_package_3 dev.nuri.seekback

# Disney+ tuşu (keycode 136, slot 4) → ileri sarma
adb shell settings put global custom_app_hotkey_package_4 dev.nuri.seekfwd

# Netflix tuşu (keycode 265, slot 2) → istediğiniz uygulama (örn. Televizo)
adb shell settings put global custom_app_hotkey_package_2 com.ottplay.ottplay
```

> **Slot eşlemesi box modelinize göre değişebilir.** Kendi tuşlarınızı bulmak için `adb logcat | grep SdtGlobalKey` açıkken kumandadan tuşlara basın — hangi keycode'un hangi uygulamayı açtığını görürsünüz. Detay: [TEKNIK.md](TEKNIK.md)

### 6. Shizuku iznini onaylayın (bir kerelik)

Kumandadan Prime ve Disney tuşlarına birer kez basın — ekranda **"…uygulamasına Shizuku'ya erişim için izin verilsin mi?"** diyaloğu çıkar, **"Her zaman izin ver"** deyin. İki uygulama için ayrı ayrı çıkar.

**Kurulum bitti.** Artık PC kapalı olsa bile tuşlar çalışır.

## Kullanım

| Tuş                          | Davranış                                    |
| ---------------------------- | ------------------------------------------- |
| Netflix                      | Televizo'yu açar (veya atadığınız uygulama) |
| Prime Video                  | YouTube'da 10sn geri                        |
| Disney+                      | YouTube'da 10sn ileri                       |
| Prime/Disney ardışık N basış | **N × 10sn** geri/ileri                     |

Sarma tuşları **yalnızca YouTube açıkken** çalışır — başka uygulamadayken hiçbir şey yapmaz. Her basış sonrası uygulama **1 saniye bekler**; bu sürede yeni basış gelirse sayaç artar ve bekleme sıfırlanır, gelmezse basış sayısı × 10sn tek seferde sarar ve `MEDIA_PLAY` ile oynatmayı sürdürür.

## Sorun giderme

- **Tuş hiçbir şey yapmıyor:** Shizuku sunucusu çalışmıyor olabilir (box yeniden başlatıldıysa adım 3'teki komutu tekrarlayın). `adb shell ps -ef | grep shizuku_server` ile kontrol edin.
- **Prime/Disney uygulaması açılıyor:** Slot ataması yanlış olabilir — `adb shell settings get global custom_app_hotkey_package_3` ile kontrol edin.
- **İzin diyaloğu tekrar çıkıyor:** "Her zaman izin ver" seçilmemiştir; bir kez daha basıp işaretleyin.
- **Eski davranışa dönmek:**

```bash
adb shell settings delete global custom_app_hotkey_package_3
adb shell settings delete global custom_app_hotkey_package_4
```

## Dosyalar

| Yol         | İçerik                                                        |
| ----------- | ------------------------------------------------------------- |
| `apk/`      | Kurulacak hazır APK'lar (seekback, seekfwd, shizuku)          |
| `src/`      | Mini uygulamaların Java kaynak kodu + manifestler             |
| `scripts/`  | Kurulum yardımcıları ve PC-daemon alternatifi                 |
| `TEKNIK.md` | Vendor tuş mekanizması, protokol detayları, build talimatları (TR) |
| `TECHNICAL.md` | Teknik dokümantasyonun İngilizce sürümü (EN) |

Teknik detaylar (vendor broadcast mekanizması, keycode keşfi, Shizuku entegrasyonu, APK'ları kaynaktan derleme) için: **[TEKNIK.md](./TEKNIK.md)**

## Güvenlik notları

- Shizuku, shell yetkisiyle çalışan bir köprüdür; izin verdiğiniz uygulamalar sistem seviyesinde komut çalıştırabilir. Sadece güvendiğiniz uygulamalara "Her zaman izin ver" deyin.
- `input keyevent` enjeksiyonu yalnızca tuş simülasyonudur; veri toplamaz.

## Lisans

Kod MIT lisansıdır. Shizuku, RikkaApps'in projesidir ve kendi lisansına tabidir.
