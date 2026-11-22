# RadiOnline proje hafızası

Son güncelleme: 21 Kasım 2026

21 Kasım 2026 katalog durumu: toplam 1591 radyo; son paket sonrası
`newlyAdded: true` Açık Kıyı | Open Shore, Halkın Sesi Radyo, Radyo Venüs
Bursa 105.3, Andromeda | The Play Group, Standart.FM, Radio Evros 97.1 ve
Ereğli FM 91.7, TRT Memleketim FM, TRT Türkiye'nin Sesi Radyosu ve kullanıcı
isteğiyle öne çıkarılan mevcut Radyo Gönül 97.3 kayıtlarındadır. Play Dance Radio,
Play Dance Türk ve Andromeda Türk daha önce katalogda bulunduğundan yeniden
eklenmemiştir. Coastiva Radio katalogda zaten bulunduğu için
mükerrer eklenmemiş; konumu Side ve Manavgat, Antalya, Türkiye olarak
düzeltilmiştir. Bandırma Radyo Venüs 92.2 ile Bursa Radyo Venüs 105.3 ayrı
istasyonlar olarak korunur. Sürüm 1.6.5.41 / 82 olarak korunur.

21 Kasım 2026 Cloudflare notu: katalogdaki Cloudflare logo URL'leri `.webp`
uzantılı olmalıdır. `logos/` altındaki kaynak dosya PNG/JPEG olabilir; GitHub
iş akışı kaynağı WebP'ye dönüştürüp aynı dosya köküyle KV'ye yükler.
Normal GitHub yüklemesi yalnız `stations.json` ile yeni/değişen logo dosyalarını
gönderir; katalog değişikliğinde bütün logolar yeniden yüklenmemelidir. Eski tam
onarım paketi ücretsiz KV'nin günlük 1.000 yazma sınırı nedeniyle otomatik değildir.

18 Kasım 2026 katalog durumu: toplam 1582 radyo; `newlyAdded: true` yalnız
Radio Uno Bogotá 88.9, Musical Stereo, Capital Salsa ve La FM Plus Bogotá
93.9, La FM Bogotá 94.9, Radio Huanta 2000 ve Radio Fiesta Mix Huanta 88.5
ile Radio Beethoven Chile 97.7, Super 45 FM ve Rockaxis Radio kayıtlarındadır.
Estación XLW 100.7 ve Radio Líder San Juan 104.3 de son gruptadır. Radio Plus
Mauritius zaten katalogda bulunduğu için yeniden eklenmemiştir. Paket sürümü
1.6.5.41 / 82'dir.

## Proje ve sürüm

RadiOnline; telefon, Android TV/TV Box, Android Auto, Chromecast ve ana ekran
widget'ında internet radyosu oynatan Android uygulamasıdır.

- Paket: `com.radyo.turkiyeradyolari`
- Çalışma sürümü: 1.6.5.41
- versionCode: 82
- Katalogun çalışma anındaki kaynağı: Cloudflare Worker `stations.json`
- GitHub deposu: `temelturkyilmaz1-dev/radyot-streams` (şu an katalog ve yayın dosyaları; tam Android kaynak projesi henüz depoya yüklenmedi)
- Son teslim: RadiOnline_1.6.5.27_2026-10-22.zip
- Son teslimden sonraki değişiklikler `CHANGELOG_CURRENT.md` içinde tutulur.

## Zaten bulunan özellikler

- TV Box üst çubuğu RadiOnline uygulama logosu, arama, sesli arama, menü,
  ekolayzır ve çıkış denetimleri dar ekranlarda da birlikte sığacak yapıdadır.
- Android Auto oynatma bilgisinde ülke bayrağı radyo adıyla aynı satırda kalır;
  şarkı metadata güncellemeleri bu kimlik satırını kaldırmaz.

- Telefon ve TV Box radyo listeleri, favoriler ve son dinlenenler.
- Liste/döşeme görünümleri ve kategori filtreleri.
- Telefon ve TV Box'ta kırmızı/dolu favori rozeti; oynatıcı, Android Auto ve
  widget'ta favori durumu.
- Android Auto katalog, favoriler, arama, ileri/geri, logo/kapak ve şarkı geçmişi.
- Chromecast, uyku zamanlayıcısı, radyo alarmı, ekolayzır ve ses kontrolleri.
- Ana yayın başarısız olursa `backupUrls` sırasıyla yedek yayına geçiş.
- Ada ve standartlaştırılmış yayın adresine göre mükerrer temizleme.
- Radyo listesi ve istasyon logoları uygulamaya gömülmez; katalog Cloudflare'dan,
  her logo da katalogdaki uzak URL'sinden alınır. Hedef, bütün görsel dosyalarının
  Cloudflare'da olmasıdır.
- Son doğrulanmış katalog cihazda önbellekte tutulur ve çevrimdışıyken kullanılabilir.
- Android Studio arayüz/kod değişiklikleri bilgisayardaki tam kaynak projede yapılır;
  kaynak, katalog ve logo dosyaları GitHub'da yedeklenir.
- GitHub Actions katalogu ve logo dosyalarını Cloudflare KV'ye aktarır.
- GitHub sağlık sistemi ayrıca `health.json` üretir ve uygulama tarafından okunur.
- Ayarlar'da tüm istasyon görsellerini eşit boyutta gösteren Logo Denetimi ekranı.
- Telefon ve TV Box'ta ana/yedek yayın rozeti ve logodan otomatik koyu arka plan rengi.
- TV Box'ta 90 saniye hareketsizlikten sonra açılan, kumanda tuşuyla kapanan tam ekran radyo modu.
- TV Box oynatıcısındaki canlı yayın durum alanı yalnızca oynatma
  durumunu gösterir; liste ve arama sonuç sayıları bu alanı ezmez.
- TV Box ana ekranı Favoriler listesiyle açılır; favori yoksa Tüm Radyolar'a
  güvenli olarak döner.
- TV Box oynatma alanında canlı rozetinden ayrı bağlantı kalitesi satırı
  bulunur. Bu satır oynatıcı tamponlama/kesinti durumundan İyi, Orta, Zayıf
  sonucunu üretir ve mevcutsa gerçek ses biçimi/bitrate bilgisini de gösterir.

## GitHub katalog ve sağlık sistemi

17 Kasım 2026 itibarıyla katalog toplamı 1569'dur. Son gruptaki 15 istasyon

`newlyAdded: true`; önceki işaretler kapalıdır. VisiOnAir Radio zaten katalogda

bulunduğundan yeniden eklenmemiştir.

Depo: `temelturkyilmaz1-dev/radyot-streams`

- Uygulama `stations.json` dosyasını Cloudflare Worker'dan açılışta kontrol eder
  ve doğrulanmış son sonucu süre sınırı olmadan cihaz önbelleğinde tutar.
- `health.json` uygulama tarafından saatte bir okunur.
- GitHub tarafındaki denetimde üç ardışık kontrolde `offline` kalan mevcut
  radyolar bildirilir; düzelenler için iyileşme bildirimi gelir.
- Cloudflare erişilemezse cihazdaki son doğrulanmış katalog çalışmaya devam eder.
- Uygulama sağlık sonucunu tüketir; bütün kataloğu telefondan yeniden tarayan ana
  sistem değildir.

Bağlantılı kod: `RemoteStationCatalog.kt` ve `StreamHealthMonitor.kt`.

## Veri ve liste kuralları

- `Radio` alanları: ad, ana yayın, yedek yayınlar, web sitesi, içerik,
  konum, frekans ve uzak logo yolu.
- Konum boş bırakılmamalıdır; TV Box bayrağının altındaki şehir/ülke buradan
  beslenir.
- Aynı markanın farklı şehirlerdeki radyoları zorla birleştirilmemelidir.
- Aynı yayın adresini kullanan farklı adlar genellikle mükerrerdir; marka alt
  kanalları doğrulanmadan silinmemelidir.
- `recentlyAddedStationKeys`, son teslimden sonra gerçekten eklenen radyoları temsil
  eder. Güncellenen eski radyolar burada yer almaz.
- Logo görünümü için uygulamada yalnızca genel yedek simge bulunur. Logo indirmeleri
  Glide önbelleğini kullanır; Android Auto katalogdaki uzak logo URL'sini kullanır.
- 23 Ekim 2026 kontrolünde GitHub kataloğundaki 799 kanalın 798 logosu Worker
  URL'sindeydi; Radyo Eylül logosu dış URL'deydi ve Cloudflare'a taşınmayı bekliyor.

## 2026-10-23 — Cloudflare katalog mimarisi hazırlığı

- Android çalışma kataloğu Cloudflare Worker `/stations.json` adresinden alınacak
  şekilde güncellendi; geçerli son katalog cihazda önbellekte kalır.
- Yerel gömülü istasyon listesi ve logo eşlemeleri çalışma kaynağı olmaktan çıkarıldı.
  Android kaynak ağacındaki mevcut logo dosyaları arşiv/yedek olarak korunur; APK
  derlemesinde kullanılmayan kaynakların küçültülmesi açıldı.
- Uygulama logosunu katalogdaki uzak `logo` adresinden yükler. GitHub kataloğu ve
  Cloudflare yayınlama iş akışı mevcut olsa da tam Android kaynak projesi henüz
  GitHub deposuna eklenmemiştir. Bu çalışma kopyası hazırlanmıştır; GitHub'a aktarım
  ve cihazda doğrulama henüz yapılmamıştır.
- Derleme denemesi Gradle dağıtımını ağdan indiremediği için tamamlanmadı;
  derleme başarılı sayılmamalıdır.

## Yakın zamanda giderilen yapısal sorunlar

- Android Auto'da ileri/geri sonrası oynatma ekranının Tüm Radyolar'a dönmesinin
  nedeni, istasyon değişirken MediaSession metadata'sının geçici olarak `null`
  yapılmasıydı. Ara boşaltma kaldırıldı; metadata atomik değiştiriliyor.
- Telefon, TV Box ve Android Auto araması `RadioSearch.kt` üzerinde birleştirildi.
  Arama, yazılan bütün kelimeleri ad, frekans, konum, içerik ve web adresinde arar;
  yalnızca ilk harf veya başlangıca bakmaz. Türkçe karakterler normalleştirilir.
- Telefon ve Android Auto ortak logo çözümleme zincirini kullanır.
- Katalogdan kaldırılmış Radyo Burada, eski Android Auto önbelleğinde görüldüğü
  için MediaBrowser kök kimliği `radyot_root_v2` olarak sürümlendi. Gelecekte Android
  Auto eski katalog gösterirse kaydı tekrar silmek yerine önce kaynak liste ve medya
  ağacı önbelleği ayrı ayrı kontrol edilmelidir.

## Son ZIP'ten sonraki logo düzeltmeleri

Adıyaman Güney Radyo, Adıyaman Radyo Tek, Hatay Akdeniz FM 89.2, Akdeniz Türkü
FM, Alaşehir Damar FM, Alanya GO, Alanya FM, Akustik Türküler, Akgül FM ve
Tekirdağ Armoni FM.

Hatay Akdeniz FM, Adana Akdeniz FM 105.7 ile; Tekirdağ Armoni FM, Afyon Armoni FM
ile karıştırılmamalıdır. Eşlemeler tam ada bağlıdır.

## Bilinen mimari eksik

Kalıcı bir `stationId` henüz yoktur. Favoriler ve bazı bağlantılar radyo adını
kimlik olarak kullanır. Kontrollü `stationId` geçişi geniş bir veri değişikliğidir;
kullanıcı istemeden yapılmaz.

## Derleme notu

1.6.5.23 paketlemesinde Android Studio JBR ve yerel Android SDK ile Kotlin kaynak
derlemesi başarıyla tamamlandı. `testDebugUnitTest` aşamasında kaynaklar ve testler
derlendi; fakat Windows Gradle test işçisi `GradleWorkerMain` sınıf yolunu açamadığı
için test süreci başlayamadan ortam hatasıyla durdu.

## Favori yedekleme (1.6.5.24)
Ayarlar → Favori Yedekleme: JSON yedekleme ve birleştirerek geri yükleme.
FavoritesBackup.kt format ve eşleştirmeyi yönetir. Sistem dosya seçicisi kullanılır.
Mevcut favoriler korunur; yeni eşleşmeler yedek sırasıyla sona eklenir.

1.6.5.24: 37 yeni radyo eklendi; ayrıntılar SURUM_NOTLARI_1.6.5.24.md içinde. Katalog JSON toplamı 799. Derleme ve cihaz doğrulaması tamamlanmadı.
