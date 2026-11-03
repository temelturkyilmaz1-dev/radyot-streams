# Henüz paketlenmemiş değişiklikler

## 31 Ekim 2026 — Yeni uluslararası radyo grubu

- Canlı yayın ve logo denetimini geçen 24 radyo eklendi; katalog 1319 kayda çıktı. Önceki yeni işaretleri kapatıldı; yalnız bu grup `newlyAdded: true` kaldı.
- Radio Paradise Main Mix adı netleştirildi ve aynı World/Etc yayınının eski düşük bit hızlı mükerrer kaydı kaldırıldı.

## 28 Ekim 2026 — Logo bağlantısı düzeltmeleri

- Kanal listesi ve yayın adresleri değiştirilmeden 7 kaydın sorunlu dış logo bağlantısı gerçek marka görsellerinin Cloudflare kopyalarıyla değiştirildi: JOE (2), Qmusic (4) ve NRJ Russia (1).
- Bağlantısı reddedilen ve logosu görünmeyen eski `Radyo Eylül` mükerrer kaydı kaldırıldı. Yayını çalışan ve resmî logosu erişilebilir olan `Radyo Eylül 99.1` korundu.
- Ekran görüntülerinde görünmeyen veya bulanık olduğu doğrulanan 61 logo kaydı, kanal ve yayın bilgileri değiştirilmeden düzeltildi: BillyBoy340 ailesi (6), Deepinradio (1), 100Hitz (6), 181.FM (47) ve Rock Melodic Radio (1). Gerçek marka görselleri Cloudflare için kalıcı ve yüksek kaliteli dosyalara dönüştürüldü.

## 23 Ekim 2026 — Uzak katalog ve logolar

- Uygulamanın çalışma anındaki katalog adresi GitHub Raw'dan Cloudflare Worker
  `stations.json` uç noktasına çevrildi.
- `RadioRepository` içindeki gömülü 802 istasyonluk liste kaldırıldı. Uygulama,
  Cloudflare'dan gelen doğrulanmış istasyon kayıtlarını kullanıyor; eksik logo
  adresinde genel uygulama simgesini gösteriyor.
- Doğrulanmış son katalog önbelleği süre sınırı olmadan tutuluyor; internet veya
  Cloudflare geçici olarak erişilemezse son başarılı liste kullanılabiliyor.
- İstasyon logosu yükleme zinciri katalogdaki Cloudflare URL'sini esas alıyor;
  uygulama kaynaklarındaki istasyon logoları çalışma anında kullanılmıyor.
- GitHub kataloğunda 799 kanaldan 798 logonun Worker'da olduğu doğrulandı;
  Radyo Eylül logosu hâlâ dış URL'de ve Cloudflare'a aktarılmadı.
- Eski ikinci katalog kaynağı devre dışı bırakıldı; GitHub/Cloudflare kataloğu tek
  istasyon veri kaynağı oldu.
- Android Studio'da debug ve release derlemelerinde kaynak küçültme açıldı; kodda
  kullanılmayan yerel logo kaynaklarının APK'ya girmemesi hedefleniyor.
- Android derlemesi bu ortamda doğrulanamadı: Gradle dağıtımını indirmek için gereken
  ağ bağlantısı mevcut değildi. Windows Android Studio'da derleme kontrolü gerekiyor.

## 22 Ekim 2026

- Android Studio'nun bazı kurulumlarda sürüm kataloğundaki eklenti takma adını
  çözememesi giderildi. Kök ve uygulama Gradle eklenti tanımları aynı AGP
  sürümü korunarak açık `com.android.application` kimliğine geçirildi.
- Kullanıcı isteğiyle kaynak sürümü bir kez artırılarak `1.6.5.27`
  (`versionCode 68`) yapıldı ve `RadiOnline_1.6.5.27_2026-10-22.zip`
  kaynak teslimi hazırlandı.
- Katalogda bulunan Tosya Cihan FM kaydı mükerrer oluşturulmadan
  tamamlandı. Tosya/Kastamonu konumu, 88.5 FM frekansı, kanal bilgi sayfası
  ve Türkçe pop ağırlıklı yayın içeriği mevcut yayın adresine eklendi.
- Fransa/Paris merkezli Size Radio kataloğa eklendi. Resmî sitenin Radiojar
  istasyon kimliğinden doğrulanan ana ve yedek yayın adresleri kaydedildi;
  kullanıcının verdiği kare plak logosu yerel görsel olarak kullanıldı.
- FM 23 Elazığ'ın yerel logosu kullanıcının verdiği kare, yüksek
  okunabilirlikli resmî marka görseliyle değiştirildi; kanal ve yayın
  bilgilerine dokunulmadı.
- Katalogda zaten bulunan Radyo Gündem kaydı mükerrer oluşturulmadan
  güncellendi. Resmî sitesi ve logosu, Çorlu/Tekirdağ konumu ile 92.8 FM
  frekansı eklendi; sitede çalışan mevcut yayın adresi korundu.
- Radyo FEF 101.5 kataloğa eklendi. Resmî sitedeki canlı oynatıcının
  kullandığı 9306 numaralı yayın, yerel logo, Milas/Muğla konumu ve
  frekans bilgisi kaydedildi; mevcut katalogda mükerrer kanal oluşturulmadı.
- İşleyen FM 96.6 kataloğa eklendi. Sayfadaki oynatıcıdan doğrulanan
  `live.radyositesihazir.com/8060/stream` yayını, Ankara konumu, frekansı ve
  istasyon logosu kaydedildi; mevcut katalogda aynı kanal bulunmadığı doğrulandı.
- Katalogda zaten bulunan `Radyo Slayt` kaydı mükerrer oluşturulmadan
  güncellendi. Resmî sitesi ve logosu eklendi; resmî oynatıcıyla eşleşen
  mevcut `live.radyo.in/8050/stream` yayın adresi korundu.
- Radyo Eylül'ün resmî sitesindeki oynatıcı doğrulandı. Sakarya 99.1 MHz
  kaydı; resmî `anadolu.liderhost.com.tr/8036/stream` yayını, sitesi ve
  512×512 yerel logosuyla kataloğa eklendi.
- TV Box ana ekranındaki canlı yayın durum alanı liste sayacından ayrıldı.
  Tüm Radyolar, Favoriler, yazılı arama veya sesli arama seçildiğinde
  `CANLI • ANA YAYIN` / `CANLI • YEDEK YAYIN` bilgisi artık kanal sayısıyla
  ezilmiyor.
- TV Box ana ekranı artık katalog yüklendikten sonra Favoriler bölümüyle
  açılıyor. Cihazda hiç favori yoksa Tüm Radyolar listesine geri dönülüyor.
- TV Box oynatma alanına canlı durum rozetinden bağımsız bağlantı
  kalitesi satırı eklendi. Oynatıcının hazırlanma ve yeniden tamponlama
  durumuna göre Bağlanıyor, İyi, Orta veya Zayıf bilgisi; akış sağlıyorsa
  codec, bitrate, örnekleme ve kanal bilgisi gösteriliyor.

Başlangıç: RadiOnline_1.6.5.24_2026-10-16.zip

Paketlendi: RadiOnline_1.6.5.25_2026-10-16.zip

Son paket: RadiOnline_1.6.5.27_2026-10-22.zip (`versionCode 68`)

## Görsel ve bakım özellikleri

- Ayarlar'a, bütün istasyon logolarını eşit çerçevede gösteren ayrı `Logo Denetimi` ekranı eklendi.
- Telefon ve TV Box oynatıcısına ana/yedek yayın durum göstergesi eklendi.
- Oynatıcı arka plan gölgesinin yerel radyo logosundan güvenli koyu renk üretmesi eklendi; Görüntü Ayarları'ndan kapatılabilir.
- TV Box'ta yayın çalarken 90 saniye işlem yapılmazsa saat, büyük logo, radyo/şarkı ve favori-yayın durumunu gösteren tam ekran mod eklendi. Herhangi bir kumanda tuşuyla kapanır ve ayarlardan devre dışı bırakılabilir.
- Radyo listesi, oynatma sırası, Android Auto kataloğu, sürüm ve Son Eklenenler değiştirilmedi.

## 16 Ekim 2026

- Akdeniz Türkü FM ile Arabesk FM'in canlı sunucu kimlikleri karşılaştırıldı.
  Farklı görünen iki adresin de `ARABESK FM` adlı aynı 64 kbps AAC+ yayınına
  bağlandığı doğrulandı. Akdeniz Türkü FM'in eski alan adı çözülmüyor ve bilinen
  alternatif yayını cevap vermiyor. Yanlış mükerrer kayıt gömülü katalogdan ve
  `stations.json` dosyasından kaldırıldı; doğrulanmış ayrı yayın bulunana kadar
  uzak katalogdan aynı adla geri eklenmesi engellendi. Arabesk FM korundu.
- Telefon ve tablette çift görünen `Can Radyo 93.3` incelendi. Referans katalogdaki
  `İzmir Radyo Can` kaydının aynı 3124 numaralı yayını kullanan eski ad kopyası
  olduğu belirlendi. Eski kopya kaldırıldı; `Can Radyo`, `Can Radyo 93.3`,
  `Can Radyo İzmir` ve `İzmir Radyo Can` adları bütün katalog yollarında aynı
  istasyon kimliğine bağlandı. Resmî site ve yerel logolu `Can Radyo 93.3` korundu.
- Edirne Radyo SU 92.2'nin bozuk favicon logosu kaldırıldı ve kullanıcının verdiği
  logo yerel kaynağa bağlandı. Eşleme tam istasyon adına yapıldı; ayrı istasyon olan
  Ankara Radyo SU kaydı ve logosu değiştirilmedi.
# 2026-10-16 — Ülke/şehir bilgisinin boş kalması

- Telefon, tablet ve TV Box'ın ortak kullandığı `StationInfoResolver` içindeki konum çözümleme düzeltildi.
- Eski katalogdan gelen `Ulusal`, `Yurtdışı`, `Çevrimiçi` ve `İnternet Radyosu` sınıflandırmaları artık doğrudan konum gibi gösterilmiyor; güvenilir ad/alan adı bilgisiyle ülkeye dönüştürülüyor.
- `Ulusal` istasyonlar şehir adı istasyon isminden bulunamıyorsa `Türkiye` olarak işaretleniyor.
- Ülke tablosuna İzlanda, Slovakya, Kazakistan, Bosna-Hersek, Trinidad ve Tobago, Şili, Dominik Cumhuriyeti, Küba, Malta, Azerbaycan, Güney Kore, Fas ve KKTC eklendi.
- TV Box'ta şehir bilinmediği halde ülke biliniyorsa bayrağın altındaki satır artık gizlenmiyor; ülke adını gösteriyor.
- Derleme denetimi: `compileDebugKotlin` başarılı.

# 2026-10-16 — Kozan FM logosu

- Kullanıcının verdiği 512 × 512 Kozan FM 90.00 logosu uygulamaya yerel kaynak olarak eklendi.
- `Kozan FM` ve aynı yayının katalogdaki `Adana Kozan FM` adı tek yerel logoya bağlandı.
- Eski uzaktan logo bağlantısı kaldırıldı; telefon, tablet, TV Box ve Android Auto aynı gömülü logoyu kullanıyor.

# 2026-10-16 — Yol Arkadaşım logosu

- Kullanıcının verdiği Yol Arkadaşım / Castrol görseli yerel logo olarak eklendi.
- Ana kayıt ve katalogdaki ad varyasyonu aynı gömülü logoya bağlandı; eski uzaktan logo devreden çıkarıldı.
- Telefon, tablet, TV Box ve Android Auto aynı logo kaynağını kullanıyor.


- Yol Arkadaşım görselindeki geniş beyaz boşluk azaltıldı ve logo kare alanda daha büyük görünecek şekilde yeniden çerçevelendi.

# 2026-10-17 — Mükerrer ve logo denetimi

- Radyo Deva/Adana, Radyo Aktif Bursa/92.6, Radyo 16/Bursa, Mercan FM/101.5,
  Mega RD/Mega Radyo İskenderun ve Radio Ava/104.4 ad varyasyonları ortak
  istasyon kimliğine bağlandı. Kütahya Radyo Aktif ile Bursa Radyo Aktif ayrı
  istasyon olarak korundu.
- Kocaeli Radyo Extra ile Tokat Radyo Extra'nın yayın adresleri farklı olduğu
  için birleştirilmedi; ayrı istasyon olarak korundu.
- Kullanıcının verdiği Radio La Belle Aventure logosu yerel kaynağa
  bağlandı. Playdanceradio adı mevcut gerçek yerel logosuna eşlendi.
- Radyo Hür, Radyo Kafses, Huzur Radyo, Mersin FM, Pink Radio, Radio Vaterland,
  Radyo Antalya ve Kütahya Radyo Aktif'in geniş/küçük logoları marka korunarak
  ortak 512×512 açık zeminli çerçeveye alındı.
- Radyo Evin, Radio Nova Bulgaria, Rádio Studio Flashback, pmr.lt, Ibiza X Radio,
  İmaj FM Erzurum, Ostim Radyo, Polis Radyosu, Öncü Radyo ve Radyo AK için
  ağ logosu yerine mevcut kaliteli yerel logo zorunlu kılındı.
- Logo Denetimi ekranının başlığına sistem çubuğu güvenli alanı eklendi;
  başlık diğer ayar ekranlarıyla aynı dikey hizaya getirildi.
- Arka plan oynatması denetlendi: oynatıcı ağ uyandırma modu, kalıcı medya
  bildirimi ve `START_STICKY` servisle çalışıyor; Activity arka plana geçince
  yayını durduran bir çağrı bulunmadı.
- Android kaynak işleme ve `compileDebugKotlin` denetimi başarıyla tamamlandı.
- Radyo Hisar'ın 981×859 resmî site logosu alındı, uygulamaya yerel
  kaynak olarak eklendi ve uzak katalog görselinin yerine sabitlendi.
- Radyo Çınar'ın gerçek 102.1 marka logosu yerel kaynağa alındı ve
  katalogdaki bozuk uzak görselin yerine sabitlendi.
- Power FM/PowerTürk kanal ailesi, Mercan FM ad varyasyonları ve Radio Ava ad
  varyasyonları mevcut gerçek yerel logolarına zorunlu olarak bağlandı. Uzak
  katalog artık bu logoları farklı boyut veya kalitedeki görsellerle ezemiyor.
- Aydın Radyo Flaş/Radyo Flas, NRT FM Nizip ve Mood Rock mevcut gerçek yerel
  logolarına sabitlendi.
- Yalnızca uzak katalog logosu bulunan sorunlu kayıtlar için ortak logo sunumu
  iyileştirildi: sekiz dp güvenli boşluk ve açık zemin uygulanıyor. Bu sayede
  kenara dayanan, kırpılan veya koyu temada kaybolan logo davranışı telefon,
  tablet, TV Box ve Android Auto'nun ortak logo zincirinde giderildi.
- Son değişikliklerden sonra `compileDebugKotlin` başarıyla tamamlandı.
- Kullanıcının verdiği Radyo Fon Malatya 101.0 logosu kalite korunarak
  512×512 yerel kaynağa dönüştürüldü ve istasyon kaydına sabitlendi.
- Kullanıcının verdiği 512×512 General FM 98.7 logosu yerel kaynağa
  eklendi; `General FM` ve `General FM 98.7` adları aynı logoya bağlandı.
- Kullanıcının verdiği Radyo Akademi görseli, ana marka ve slogan korunarak
  sol ağırlı kare kadraja alındı, 512×512 yerel logoya dönüştürüldü ve
  yalnızca `Radyo Akademi` kaydına bağlandı.
- Kullanıcının verdiği Radio Veronika logosu yüksek kaliteli ölçekleme ile
  512×512 yerel kaynağa dönüştürüldü ve Bulgaristan `Radio Veronika`
  kaydına sabitlendi.
- Kullanıcının verdiği yatay Radyo 49 logosu kırpılmadan, marka kırmızısı
  ile tamamlanan 512×512 kare alana yerleştirildi; Muş Radyo 49 ad
  varyasyonları aynı yerel logoya bağlandı.
- Kullanıcının verdiği 512×512 Radyo 72 92.2 FM logosu yerel kaynağa
  eklendi ve yalnızca Batman `Radyo 72` ad varyasyonlarına bağlandı.
- Kullanıcının verdiği 512×512 Radyo Ahenk logosu yerel kaynağa eklendi
  ve yalnızca `Radyo Ahenk` kaydına sabitlendi.
# 2026-10-17

- Kullanıcının sağladığı düşük çözünürlüklü gerçek marka görselleri korunarak Radyo Düet ve Dümensiz Radyo logoları temizlenip yüksek çözünürlüklü yerel varlıklara dönüştürüldü.
- İmparator FM Sakarya görselindeki iletişim satırı çıkarıldı; gerçek ana amblem netleştirilerek yerel logo eşlemesine bağlandı.
- Kullanıcının sağladığı yüksek çözünürlüklü özgün Kolaylı Radyo logosu değiştirilmeden yerel eşlemeye eklendi.
- Kral Rizeli FM'in düşük çözünürlüklü gerçek logosu, siyah-altın marka yapısı korunarak temizlendi ve yerel eşlemeye bağlandı.
- Kullanıcının sağladığı özgün KU Radyo logosu yerel varlığa eklendi ve ad yazım varyantlarıyla eşlendi.
- Katalogdaki gerçek fakat düşük çözünürlüklü Mavi Nokta FM logosu özgün daire, renk ve yazı düzeni korunarak netleştirildi ve yerel eşlemeye bağlandı.
- Mega Radyo İskenderun'un düşük çözünürlüklü gerçek 97.7 FM logosu marka yapısı korunarak netleştirildi; Mega RD adı da aynı logoya bağlandı.
- Kullanıcının sağladığı yüksek çözünürlüklü Merkez FM Samsun 88.4 logosu özgün haliyle yerel eşlemeye eklendi.
- Moda FM Ünye'nin düşük çözünürlüklü gerçek logosu özgün kırmızı amblem ve yazı düzeni korunarak netleştirildi.
- Kullanıcının sağladığı yüksek çözünürlüklü Nevşehir Radyo Mega 100.5 logosu değiştirilmeden yerel eşlemeye eklendi.
- Nurs FM 106.6'nın düşük çözünürlüklü gerçek logosu; kalp, kulaklık, frekans ve slogan korunarak netleştirildi ve yerel eşlemeye bağlandı.
- Örnek Radyo Muğla 97.7'nin düşük çözünürlüklü gerçek logosu, ekran görüntüsü çerçevesinden arındırılıp marka yapısı korunarak netleştirildi.
- Radyo Çetin 102.5'in düşük çözünürlüklü gerçek logosu; turkuaz-mor renkleri, notaları, yazısı ve frekansı korunarak netleştirildi.

# 2026-10-18

- Radyo Energy İstanbul ve Radyo Diyalog logoları, kullanıcı görsellerindeki gerçek marka yapısı korunarak netleştirildi ve yerel eşlemeye bağlandı.
- İki ayrı Extra kaydı karıştırılmadan ayrıldı: `Radyo Extra` Kocaeli logosuna, `Radyo Extra Tokat` ise kullanıcının belirttiği 90.5 Radyo Exfra logosuna bağlandı.
- Kocaeli `Radyo Extra` kaydının yanlışlıkla Süper 2 çalan yayın adresi kaldırıldı; istasyonun yayımladığı resmi `dinle.radyoextra.com.tr/mp3/1/` akışına ve resmi sitesine bağlandı. Tokat kaydının ayrı resmi akışı korunmuştur.
- Sekiz yeni yabancı istasyon, doğrulanmış doğrudan akışları, resmî siteleri, özgün logo adresleri ve ülke/şehir bilgileriyle eklendi: Birmingham Mountain Radio, Oyente Stereo, Classic Country 104.9, Radio Woodstock, GDRadio.net, Radio Zvezda, Bassdrive ve Rock The Bones Radio.
- `KRAKØN Radio` zaten katalogda bulunduğu için mükerrer eklenmedi. P66 Garage / Action60s Live365 akışı Türkiye bağlantısında 401 verdiği için çalışmayan kayıt oluşturulmadı.
- Kaynak paket sürümü kullanıcı isteğiyle bir kez artırılarak `1.6.5.26` (`versionCode 67`) yapıldı ve `RadiOnline_1.6.5.26_2026-10-18.zip` teslimi hazırlandı.

# 2026-10-23 — Cloudflare katalog mimarisi hazırlığı

- Android uygulamasının istasyon kataloğu Cloudflare Worker `/stations.json`
  adresinden alınacak şekilde düzenlendi; son geçerli katalog çevrimdışı kullanım
  için cihazda saklanıyor.
- Çalışma zamanındaki gömülü kanal listesi ve yerel logo eşlemeleri kaldırıldı;
  logo katalogdaki uzak adresten yükleniyor. Projedeki logo kaynak dosyaları tam
  kaynak arşivinde korunuyor.
- Kullanılmayan Android logo kaynaklarının APK'ya girmemesi için debug ve release
  kaynak küçültme etkinleştirildi.
- Tam Android kaynak projesinin GitHub'a yüklenmesi ve cihazda test edilmesi henüz
  yapılmadı. Gradle indirme erişimi olmadığı için derleme doğrulanamadı.

