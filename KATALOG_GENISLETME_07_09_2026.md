# RadyoT — Büyük Katalog Genişletmesi (07.09.2026)

Temel proje: `RadyoT_v1.5_678_kanal_vurgu_ambiyans_20260905.zip`

## Uygulanan katalog politikası

- Çalışan gerçek yayın akışı zorunlu.
- Kullanılabilir gerçek logo/favikon zorunlu.
- Metadata zorunlu değil; varsa kullanılır.
- Ana formatı haber/gündem olan kanallar eklenmez.
- Ana formatı dini yayın olan kanallar eklenmez.
- Aynı isim veya aynı stream ile gelen mükerrerler birleştirilir.
- Resmî ayrı stream + logo sunan alt/yan müzik kanalları bağımsız radyo olarak eklenir.

## Bu paket ne değiştirdi?

1. 678 kanallık eski gömülü tabandan TRT Radyo Haber, Habertürk Radyo, CGTN Türk Radyo ve Anlatılanın Ötesi çıkarıldı. Statik temiz taban 674 kanaldır.
2. Radyo Gökçeada çalışan stream + resmî logo ile garanti seed kanal olarak eklendi.
3. `ExpandedStationCatalog` eklendi. Uygulama ilk açılışta ve önbellek yenilemelerinde kullanıcının büyük listesinde yer alan onaylı ağ/kaynakları Radio Browser sağlık verisi üzerinden genişletir.
4. 181.FM, Big R Radio, 1.FM, 100Hitz, AceRadio, NRJ, FFH, ROCK ANTENNE, Totally Radio, FluxFM, Qmusic, JOE, Loops, CueLights, Capital, NRG Play, Dinamo, RadyoHome, Turkuvaz, Mersin/Tepe Medya ve diğer doğrulanmış kaynaklar kapsama alındı.
5. Türkiye için kullanıcının büyük listesindeki müzik ağırlıklı resmî alan adları allowlist olarak eklendi. Haber/dini/hijack olmuş kaynaklar listede yoktur.
6. Dinamik eklenen kanallarda Radio Browser'ın çalışan stream kontrolü ve favicon/logo alanı zorunlu tutulur.
7. Uzaktan logo URL'leri ana uygulama listesinde, oynatıcıda, Chromecast'te ve Android Auto medya açıklamalarında kullanılabilir hale getirildi.
8. GitHub `stations.json` artık çalışan stream + `logo` alanı olan yeni istasyonları da APK güncellemesi olmadan ekleyebilir.
9. Kullanıcıdan kanal ekleme ekranında metadata zorunluluğu kaldırıldı: stream çalışıyorsa ve logo doğrulanıyorsa metadata gelmese de kayıt yapılabilir.
10. Büyük katalog sonucu cihazda önbelleğe alınır. Radio Browser erişilemezse gömülü taban + son başarılı önbellek kullanılmaya devam eder.

## Kanal sayısı

Statik temiz taban: **674**.
Radyo Gökçeada seed ile ağ erişimi olmadan dahi **675** kayıt görünür.
Büyük ağlar yenilendiğinde önceki araştırmada doğrulanan net-yeni alt sınır yaklaşık **670 kanal** olduğundan hedef katalog **1.300+** seviyesindedir. Gerçek sayı ilk çevrimiçi katalog yenilemesinde güncel Radio Browser sonuçlarına, mükerrer ve haber/dini filtrelerine göre hesaplanır.

## Not

Bu çalışma ortamı Gradle 9.4.1 dağıtımını `services.gradle.org` üzerinden indiremiyor. Bu nedenle burada APK derlemesi tamamlanamadı. Kaynak paketinde Gradle wrapper ve Android Studio proje yapısı korunmuştur.
