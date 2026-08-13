# ZTE 286R Veri Kullanımı Widget'ı

ZTE MF286R (286R) 4G router'ın web arayüzündeki "Statistics" bölümünde gösterilen veri kullanımını (örn: "199.88GB Used") Android ana ekranında widget olarak gösteren uygulama.

## Özellikler

- 📊 **Veri Kullanımı Gösterimi**: Router'ın toplam kullanılan veri miktarını widget üzerinde gösterir
- 🔄 **Otomatik Güncelleme**: Widget belirli aralıklarla otomatik güncellenir
- 🔘 **Manuel Yenileme**: Widget üzerindeki yenileme butonu ile anında güncelleme
- ⚙️ **Kolay Kurulum**: Router IP, kullanıcı adı ve şifre ayarları uygulama içinden yapılır
- 🔌 **Bağlantı Testi**: Ayarlar ekranından router bağlantısını test edebilme

## Kurulum

### Gereksinimler
- Android 7.0 (API 24) veya üzeri
- ZTE MF286R router'a ağ üzerinden erişim
- Router yönetim kullanıcı adı ve şifresi

### Derleme (PC'ye hiçbir şey kurmadan - Online)

Proje, **GitHub Actions** ile otomatik APK derleme desteği içerir. PC'ye Android Studio veya herhangi bir yazılım kurmanıza gerek yoktur.

**Adımlar:**
1. [GitHub.com](https://github.com) adresinde ücretsiz hesap oluşturun
2. Yeni bir repository (depo) oluşturun (örn: `zte286r-widget`)
3. `ZTE286R-Widget` klasöründeki tüm dosyaları bu repoya yükleyin
4. Repo sayfasında **Actions** sekmesine tıklayın
5. "Build APK" workflow'unu seçip **Run workflow** butonuna tıklayın
6. Derleme bittikten sonra **Artifacts** bölümünden `ZTE286R-Widget-APK` dosyasını indirin
7. İndirilen APK'yı Android telefonunuza kurun

**Alternatif (PC'de derleme):**
```bash
cd ZTE286R-Widget
./gradlew assembleDebug
```

APK dosyası `app/build/outputs/apk/debug/app-debug.apk` konumunda oluşur.

### Kurulum Adımları
1. APK'yı Android cihaza yükleyin
2. Uygulamayı açın ve "Ayarlar" butonuna tıklayın
3. Router IP adresini girin (varsayılan: `192.168.1.1`)
4. Kullanıcı adını girin (varsayılan: `admin`)
5. Router yönetim şifresini girin
6. "Bağlantıyı Test Et" butonu ile bağlantıyı doğrulayın
7. "Kaydet" butonuna tıklayın
8. Ana ekrana widget ekleyin:
   - Ana ekranda boş bir alana uzun basın
   - "Widget'lar" seçeneğine tıklayın
   - "ZTE 286R Veri Kullanımı" widget'ını seçin

## Nasıl Çalışır?

Uygulama, ZTE MF286R router'ın web arayüzünün kullandığı `goform` API endpoint'lerini kullanır:

1. **Login**: `POST /goform/goform_set_cmd_process` endpoint'ine şifre base64 kodlanmış olarak gönderilir
2. **Veri Çekme**: `GET /goform/goform_get_cmd_process?cmd=traffic_stat` endpoint'inden trafik istatistikleri alınır
3. **Gösterim**: Alınan byte değerleri okunabilir formata (GB, MB) çevrilerek widget'ta gösterilir

## Proje Yapısı

```
ZTE286R-Widget/
├── app/
│   ├── src/main/
│   │   ├── java/com/zte286r/widget/
│   │   │   ├── MainActivity.java          # Ana aktivite
│   │   │   ├── SettingsActivity.java      # Ayarlar ekranı
│   │   │   ├── DataUsageWidgetProvider.java # Widget provider
│   │   │   ├── RouterApiClient.java       # Router API istemcisi
│   │   │   └── service/
│   │   │       └── WidgetUpdateService.java # Widget güncelleme servisi
│   │   ├── res/
│   │   │   ├── layout/
│   │   │   │   ├── activity_main.xml      # Ana ekran layout
│   │   │   │   ├── activity_settings.xml  # Ayarlar layout
│   │   │   │   └── widget_data_usage.xml  # Widget layout
│   │   │   ├── values/                    # String, renk, tema tanımları
│   │   │   ├── drawable/                  # Görsel kaynaklar
│   │   │   └── xml/
│   │   │       └── data_usage_widget_info.xml # Widget yapılandırması
│   │   └── AndroidManifest.xml
│   └── build.gradle
├── build.gradle
├── settings.gradle
└── README.md
```

## Güvenlik Notları

- Router şifresi cihazda `SharedPreferences` içinde düz metin olarak saklanır
- Uygulama yalnızca yerel ağdaki router'a bağlanır
- Şifre, router'a gönderilmeden önce base64 ile kodlanır (ZTE protokolü gereği)

## Sorun Giderme

| Sorun | Çözüm |
|-------|-------|
| "Bağlanılamadı" hatası | Router IP adresini kontrol edin, telefonun router'a bağlı olduğundan emin olun |
| Login başarısız | Kullanıcı adı ve şifreyi kontrol edin |
| Widget "Yükleniyor..." gösteriyor | Ayarları kaydettiğinizden emin olun, widget'ı yenileyin |
| Veri güncellenmiyor | Widget'ı kaldırıp tekrar ekleyin |

## Lisans

Bu proje kişisel kullanım için geliştirilmiştir. ZTE markası ve ürün adları ilgili sahiplerinin ticari markalarıdır.