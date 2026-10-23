# Henüz paketlenmemiş değişiklikler

Son teslim edilen ZIP'ten sonra yapılanlar burada tutulur. Yeni ZIP hazırlanınca
maddeler sürüm notuna aktarılır ve bu dosya yeni dönem için temizlenir.

Başlangıç: `RadiOnline_1.6.5.23_2026-10-15.zip`

## Favori yedekleme
- Ayarlar'a Favori Yedekleme satırı ve Yedekle / Geri Yükle seçenekleri eklendi.
- Sistem dosya seçicisiyle sürümlü JSON dışa/içe aktarılır; depolama izni istenmez.
- Yedek türü radionline_favorites, format sürümü 1; ad, yayın adresleri ve sıra saklanır.
- Geri yükleme mevcut favorileri korur, eksikleri yedek sırasıyla ekler; mükerrer eklemez.
- Önce tam ad, sonra tekil yayın eşleşmesi kullanılır; bulunamayanlar bildirilir.
- Hatalı veya desteklenmeyen yedek favorileri değiştirmez. Boyut üst sınırı 2 MiB.
- Sürüm artırılmadı; yeni ZIP/APK hazırlanmadı.

## Paketleme sırasında doldurulacak

- Yeni sürüm: bir sonraki paket hazırlanırken doldurulacak
- Yeni `versionCode`: bir sonraki paket hazırlanırken doldurulacak
- Bu dönemde gerçekten yeni eklenen radyolar:
- Doğrulanan test/derleme:
- Oluşturulan ZIP yolu:

## Doğrulama
- JSON sıralama, birleştirme, ad değişikliği, belirsiz yayın eşleşmesi ve hatalı format için FavoritesBackupTest eklendi.
- Gradle denendi; ortam proje dizinini yazılabilir görmediği için yapılandırma aşamasında durdu. Kod derleme ve test sonuçları henüz doğrulanmadı; cihaz testi yapılmadı.

## Çatal Medya kontrolü — 16 Ekim 2026
- Resmî kaynak: https://catalmedya.com/index.php
- Sitedeki yayın: https://stream.zeno.fm/0mhfv2bgkf9uv
- Site beyanı: Ankara, Türkiye; damar müzik.
- Mevcut kaynak kataloglarında ad/site/yayın eşleşmesi bulunmadı.
- Yayın HTTPS, HTTP, son eğik çizgi ve sorgu ekli biçimlerde denendi; tamamı HTTP 401 ve 0 ses baytı döndürdü. Tarayıcı User-Agent ve site Referer başlıkları da denendi.
- Resmî sayfada logo dosyası yok; görünen fotoğraf HÜSEYİN / CANLI DJ olarak etiketli, istasyon logosu olarak kullanılmadı.
- Çalışan yayın doğrulanamadığından yeni kanal eklenmedi. Mevcut radyolar değiştirilmedi.

## Münih Radyo — 16 Ekim 2026
- Resmî site: https://www.munihradio.com/; şehir/ülke: Münih, Almanya.
- Resmî oynatıcı yayını https://yayin.munihradio.com/8854/stream HTTP 200, audio/mpeg ve 1024 ses baytıyla doğrulandı.
- Ad, standart ad ve yayın adresinde mevcut kayıt bulunmadı; ana katalog ve stations.json içine eklendi.
- Resmî 1230×467 PNG logo yerel drawable ve ortak logo eşlemesine eklendi; yeniden çizilmedi.
- Bu yeni paket döneminde Son Eklenenler yalnızca Münih Radyo. Önceki paketin beş yeni kaydı ana katalogda korundu.
- Çatal Medya kullanıcı isteğiyle eklenmeyecek.
- Sürüm artırılmadı; ZIP/APK hazırlanmadı.

## Turkuaz FM — 16 Ekim 2026
- Resmî site https://www.turkuazfm.be/; resmî adres Rue Basse Marihaye 376, 4100 Seraing: Seraing, Liège, Belçika.
- Türkçe/Fransızca pop ve uluslararası hitler; resmî oynatıcı https://stream.lfmradio.be/turkuaz HTTP 200, audio/mpeg ve 1024 ses baytıyla doğrulandı.
- Gömülü kaynaklarda gerçek istasyon kaydı yok; ExpandedStationCatalog yalnızca kaynak keşif tanımı içeriyordu. Ana kayıt ve JSON eklendi; çalışma anındaki mevcut yayın tekilleştirmesi korunuyor.
- Resmî oynatıcı logosu 400×145 PNG yerel drawable ve ortak tam ad eşlemelerine kaydedildi. Orijinal yatay logo korundu; 512×512 değil.
- Son Eklenenler bu dönem: Turkuaz FM ve Münih Radyo.
- JSON doğrulandı; sürüm değişmedi, paket hazırlanmadı.

## Avrupa FM kontrolü — 16 Ekim 2026
- https://www.avrupafm.net/ web aracında askıya alınmış hesap sayfasına yönlendi; doğrudan HTTPS bağlantısı kapandı.
- HTTP çıplak alan adı Natro park sayfası gösterdi; çalışan yayın, resmî logo ve şehir doğrulanamadı.
- İsim benzerliği olan diğer Avrupa radyolarıyla birleştirilmedi; katalog değişmedi.

## Spilarinn / İzlanda — 16 Ekim 2026
- Resmî API https://bakendi.spilarinn.is/api/stations/ üzerinden 22 müzik kanalı ana katalog, JSON, yerel logo ve Son Eklenenler listesine eklendi.
- Tüm yayınlar HTTPS ile HTTP 200, audio/mpeg ve 1024 ses baytıyla doğrulandı.
- Resmî atv logo sürümleri 600×600 PNG olarak korundu; yeniden çizilmedi.
- Türkiye Kiss FM kaydı korunarak yeni kanal Kiss FM İzlanda adıyla eklendi.
- Suðurland FM Selfoss (resmî site adresi); FM Trölli Kópavogur (düzenleyici kaydındaki işletmeci adresi); diğerleri Reykjavík, İzlanda.
- Konum kaynakları: fjolmidlanefnd.is/fjolmidlar/flashback/, kiss-fm-xtra/, trolli-fm-1037/; sudurlandfm.is; RÚV resmî Efstaleiti adresi; K100/Bylgjan adresleri.
- API frekansları arasında sanal değerler olduğundan FM frekansı kopyalanmadı.
- Parlamento, dinî, haber/sohbet ağırlıklı ve temp test kanalları eklenmedi.
- Son Eklenenler bu 22 kanal + Münih Radyo + Turkuaz FM; eski kayıtlar korunuyor.
- JSON toplamı 786. Sürüm artırılmadı; ZIP/APK hazırlanmadı; derleme/cihaz testi yapılmadı.

## KralNews kontrolü — 16 Ekim 2026
- https://www.kralnews.gr/ haber/blog sitesi; KralNews adıyla bağımsız radyo yayını bulunmadı.
- Kenar çubuğunda Star 88.8, Radio Deejay 94.6 ve başka istasyonların Live24 bağlantıları yer alıyor. Bunlar KralNews yayını olarak kaydedilmedi.
- Katalog değiştirilmedi.

## Kral FM 100 İskeçe — 16 Ekim 2026
- KralNews bağımsız radyo değil; sayfadaki KRAL fm 100 bağlantısı Live24 üzerinden resmî https://www.kralfm.gr/ sitesine izlendi.
- Resmî HTML yayın adresi doğrulandı; HTTPS sürümü https://onairmediagroup.live24.gr/kralfm100xanthi HTTP 200, audio/mpeg ve 1024 ses baytı verdi.
- Resmî kral.png 1024×1024 yerel drawable ve tam ad logo eşlemesine kaydedildi.
- İskeçe (Xanthi), Yunanistan; 100.0 FM. Türkiye Kral FM kaydı korunarak ayrı adla eklendi.
- Ana katalog, JSON ve Son Eklenenler güncellendi. KralNews sayfasındaki diğer istasyonlar topluca eklenmedi.
- JSON toplamı 787; yeni dönem Son Eklenenler 25. Sürüm değişmedi, ZIP/APK hazırlanmadı.

## P12 Hitmix — 16 Ekim 2026
- Resmî kaynak https://www.lydenavradio.no/p12/ ve /hjelp/direktelenker-til-vaare-radiokanaler.
- Ana P12_MH HTTP 200 audio/mpeg, yedek P12_AH HTTP 200 audio/aac; her ikisinden 1024 ses baytı alındı.
- P4 Radio Hele Norge AS resmî posta adresine göre Lillehammer, Norveç. P12 stüdyo şehri ayrıca doğrulanmadı.
- Resmî Nuxt kanal verisindeki f7b4d08f-3dac-43ae-9246-08be26469466.png kare 1024×1024 logo yerel kaynak ve ortak tam ad eşlemesine eklendi.
- Ana katalog, JSON ve Son Eklenenler güncellendi; mükerrer ad/yayın bulunmadı. JSON toplamı 788.
- Yayıncının bağlantı sayfası, uygulama/web kullanımında yazılı izin istediğini açıkça belirtiyor. İzin alındığı varsayılmadı; dağıtım öncesi bu şart ele alınmalı.
- Sürüm artırılmadı; ZIP/APK hazırlanmadı. Derleme/cihaz testi yapılmadı.

## Radio Bari 88.8 — 16 Ekim 2026
- Resmî radiobari.net oynatıcı ayarı port 9062 ve resmî mediastreaming ssl2.js host s6.mediastreaming.it üzerinden yayın çözüldü.
- HTTPS doğrudan yayın HTTP 200 audio/aacp, 1024 ses baytıyla doğrulandı; oynatıcı proxy biçimi 400 verdi, kullanılmadı.
- Bari, İtalya; 88.8 FM; resmî adres Via Fanelli 206/4.
- Resmî radiobari.png küçük 200×175; yerel drawable ve tam ad eşlemesiyle eklendi, yeniden çizilmedi.
- Ana katalog, JSON ve Son Eklenenler güncellendi. JSON toplamı 789; sürüm değişmedi, paket hazırlanmadı.

## DJ International — 16 Ekim 2026
- Resmî djinternational.it oynatıcı data-qtmplayer-file yayını HTTP 200 audio/aacp ve 1024 ses baytıyla doğrulandı.
- Pescara, İtalya; resmî footer şirket adresi Via Tiburtina V. KM 229,100, CAP 65131.
- Dance, elektronik ve uluslararası pop müzik. Resmî schema Organization logo INTERNATIONAL-7-min-1-scaled.png 2560×2560 yerel drawable ve tam ad eşlemesiyle eklendi.
- Ana katalog, JSON ve Son Eklenenler güncellendi. JSON toplamı 790. Radio Bari ve DJ International adları tekil.
- Sürüm değişmedi; ZIP/APK hazırlanmadı.

## Radio Bari kullanıcı logosu
- Kullanıcının gönderdiği renkli RADIOBARI PNG, radio_bari_official.png yerine kaydedildi. Tasarım ve saydamlık değiştirilmedi; 512×512 olarak işlenmedi.
- Kanal, yayın ve sürüm değiştirilmedi.

## Radio Bari 512×512 standardı
- Yeniden çizim denemesi renk geçişlerini değiştirdiğinden kullanılmadı. Özgün kullanıcı PNG'si yüksek kaliteli bicubic ölçeklemeyle 512×448 olarak büyütüldü, 512×512 saydam kareye ortalandı (üst/alt 32 piksel). Renkler, yazı, tasarım ve oranlar korundu.
- Yerel radio_bari_official.png boyutu doğrulandı: 512×512. Sürüm değişmedi.

## R101 — 16 Ekim 2026
- Resmî r101.it/radio-onair/ audio src: https://icecast.unitedradio.it/r101_mp3; HTTP 200 audio/mpeg, 1024 ses baytıyla doğrulandı.
- Konum Milano, İtalya; resmî Monradio adresi via Paleocapa 3.
- Resmî Logo-R101-1.png 390×390, saydam PNG yerel drawable ve tam ad logo eşlemesine kaydedildi.
- Ana katalog, JSON ve Son Eklenenler güncellendi. Tema web radyoları bu işlemde eklenmedi.
- JSON toplamı 791; sürüm değişmedi, ZIP/APK hazırlanmadı.

## Radio RaBe — 16 Ekim 2026
- Resmî rabe.ch ana sayfası M3U kaynağı çözüldü: https://stream.rabe.ch/livestream/rabe-hd.mp3. HTTP 200 audio/mpeg, 1024 ses baytı doğrulandı.
- Bern, İsviçre; resmî adres Sulgenrain 28, 3007 Bern.
- Resmî /geschichte sayfası müzik çeşitliliğini (hip hop, indie, alternative, metal, reggae vb.) ve çok dilli kültür yayınlarını doğruluyor.
- Logo indirme sayfasındaki sarı/lacivert resmî SVG (2197671e-14ae-481e-afe0-4a9c4ccf0d37) tasarımı korunarak Android VectorDrawable olarak çevrildi. 512dp kare, 2500 birim viewport; beş özgün path ve arka plan korunuyor. Raster çözünürlük sınırı yok.
- Ana katalog, JSON ve Son Eklenenler güncellendi. XML ve JSON doğrulandı; JSON toplamı 792.
- Sürüm değişmedi; ZIP/APK hazırlanmadı. Derleme/cihaz testi yapılmadı.

## Argovia / Radio TOP / Gloria — 16 Ekim 2026
- Radio Argovia: Aarau, İsviçre (resmî Neumattstrasse 1 adresi); resmî HTML audioPlayer.streams.21 yayını stream.streambase.ch/argovia/mp3-192/direct/ HTTP 200 audio/mpeg ve 1024 ses baytıyla doğrulandı.
- Argovia resmî kanal görseli n-mobile2x-1x1 sürümü 512×512 yerel kaydedildi.
- Radio TOP İsviçre: Winterthur (resmî topmedien.ch/impressum adresi); resmî streaming2.toponline.ch/pdf/radiotopdoku2020.pdf kaynağındaki HTTP yayın adresi çalıştı: 200 audio/mpeg, 1024 bayt. HTTPS denemeleri zaman aşımı; kullanılmadı.
- Radio TOP güncel resmî topmedien.ch/radiotop sayfasındaki RT_Logo_gelb_rgb.png yerel kaydedildi; eski 2021 logosu kullanılmadı.
- Her iki kayıt ana katalog/JSON/Son Eklenenler/ortak yerel logo eşlemesine eklendi; mükerrer yayın bulunmadı.
- Radio Gloria resmî sitesinde Katolik içerikler ve Hristiyan müziği olarak tanımlanıyor; kullanıcının dinî yayın eklememe tercihi gereği eklenmedi.
- JSON toplamı 794. Sürüm değişmedi; ZIP/APK hazırlanmadı.

## Sunshine / İspanya radyoları — 16 Ekim 2026
- Sunshine Live CH: Zürih, İsviçre; resmî /ch sayfası Nuxt verisindeki schweiz/mp3-192/homepage yayını 200 audio/mpeg, 2048 bayt doğrulandı. İsviçre şirketi konumu resmî yarışma koşulları /teilnahmebedingungen-thaibreak üzerinden doğrulandı.
- Rock FM İspanya: Madrid; resmî site kimlik/posta adresi ve logo-amp.png. Yayın flucast-rb01-01.flumotion.com/cope/rockfm.aac 200 audio/aacp, 2048 bayt.
- Catalunya Música: Barcelona; resmî 3Cat müzik sayfası ve Diagonal stüdyo adresi. TDTChannels resmî yayın alan adına giden güncel adresi bulmak için yardımcı kaynak oldu; HLS master, alt liste ve TS parçası 200/1024 bayt doğrulandı.
- Loca Urban: resmî /loca-urban/player.html data-streaming adresi 200 audio/mpeg, 2048 bayt. Konum Arroyomolinos, Madrid (resmî aviso-legal şirket adresi). Kullanıcı URL'si /loca-urban/ 404 olduğundan çalışan resmî player sayfası kullanıldı.
- Cadena 100: Madrid; resmî ana sayfa ve logo-amp.png. HLS kaynağı TDTChannels yardımıyla bulundu; master/alt liste/medya 200 audio/aac, 1024 bayt doğrulandı.
- Beşinin resmî kaynak logoları yeniden çizilmeden 512×512 saydam kareye, oran korunarak yerleştirildi (sharp); Loca SVG özgün vektörden rasterleştirildi. Catalunya favicon kaynağı 192×192 olduğu için büyütüldü; kaynak kalitesi sınırlı.
- Beş kayıt ana katalog, JSON, ortak yerel logo ve Son Eklenenler listelerine eklendi; ad/yayın eşleşmesi bulunmadı. JSON toplamı 799.
- Melodía FM: resmî site ve logo bulundu, konum San Sebastián de los Reyes, Madrid. HLS adayları 403/451 ve resmî Atres canlı yönlendiricisi 403 döndürdü; bu ağdan ses doğrulanamadığı için eklenmedi.
- Radio Clásica: resmî RTVE sayfası ve SVG logo bulundu. rtvelivestream.rtve.es resmî yayın alanına bağlantı kapandı; alternatifler de doğrulanamadı. Bu erişim sorunu yayının çalışmadığı anlamına gelmez. Kataloğa eklenmedi.
- Vida FM resmî sayfasında inanç/dinî program ağırlığı; Radio Estel resmî program açıklamasında dinî bilgi ve litürji ağırlığı bulunduğundan eklenmedi.
- Kaynaklar work/sunshine.html, rockfm.html, catalunya.html, loca-player.html, cadena100.html ve tdt-radio.md içinde saklandı.
- Sürüm artırılmadı; ZIP/APK hazırlanmadı; derleme ve cihaz testi yapılmadı.

## Paket doğrulama
- 799 JSON kaydı, 37 yeni kanal; önceki 762 kaydın tamamı korunuyor. Son Eklenenler ve drawable başvuruları doğrulandı.
- Android derlemesi ve birim test çalıştırması önceki ortam engeli nedeniyle doğrulanmadı; cihaz testi yapılmadı.
- Yeni sürüm 1.6.5.24 / versionCode 65; ZIP kaynak kod paketidir.