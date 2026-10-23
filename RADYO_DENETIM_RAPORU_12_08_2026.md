# RadyoT kanal denetimi — 12.08.2026

## Sonuç

- Uygulama içi katalog ve GitHub kataloğu 280 istasyonda eşitlendi.
- Mükerrer istasyon adı, kimliği ve yayın adresi kalmadı.
- Eksik Android logo kaynağı bulunmuyor.
- Tüm yeni logolar 512 × 512 uygulama görseline dönüştürüldü.

## Bu denetimde eklenenler

- LUNA FM: World, Portugal, Brasil, Latino
- Unique: Gold, Classic Rock, Smooth, Offshore Radio Tribute
- Rewind 2000's ve Giant Radio
- Epic Piano: Chillout, Romantic, Solo, Great Concerts, Modern, Coverhits,
  Christmas, Chopin, Tchaikovsky, Grieg ve Liszt
- Radyo Venüs 92.2, Körfez FM 91.1, Radyo Kuzey Ege 90.0, Lokum FM 96.8
- Radyo Desibel 94.0 ve Eskişehir Radyo Ses 98.9
- Radyo90.com ve Radio Ava 104.4
- Efsane Radyo 96.2 ve Mercan FM 101.5
- Meteorolojinin Sesi Radyosu
- Radyo 42 FM Medya Konya

## Düzeltilenler

- LUNA FM Latino, Portugal görselini kullanmıyor; İspanyolca sloganlı gerçek
  Latino logosuna geçirildi.
- Eski “Radyo 90” kaydı, yanlış Doksanlar FM logosuyla Radyo90.com akışını
  kullanıyordu. Yanlış kayıt kaldırıldı; doğrulanmış Radyo90.com kaydı bırakıldı.
- Number1 Türk Rap yanlışlıkla Kiss Türk Rap akışını kullanıyordu. Number1'ın
  resmi HLS yayınına geçirildi; böylece mükerrer yayın adresi giderildi.

## Şimdilik eklenmeyenler

- FG Digital: resmi sayfadaki yayın adresi denetimde HTTP 502 döndürdü.
- Noksanlar Kenti: resmi yayın sunucusu HTTP 400 döndürdü.
- Radyo 10 ve Radyo Seyhan: sayfalarındaki HTTPS yayın uçları HTTP 502 döndürdü.
- SIZE Radio: resmi site denetim sırasında HTTP 502 döndürdü.
- Splash Radio: tek bir ana yayın yerine çok sayıda ayrı tematik istasyon sunuyor;
  rastgele bir alt kanal ana kanal adıyla eklenmedi.
- Romantik Türk: resmi sayfa kullanılabilir doğrudan yayın/metadata vermedi.

Bu kanallar bozuk adresle kataloğa sokulmadı. GitHub sağlık taraması sonraki
çalışmalarında yeniden erişilebilir olurlarsa tekrar değerlendirilebilir.

## Teknik kontrol

- `stations.json`: geçerli JSON
- Kotlin katalog sayısı: 280
- GitHub katalog sayısı: 280
- Mükerrer kimlik/ad/yayın adresi: 0
- Eksik drawable başvurusu: 0
- Android XML kaynakları: 57/57 geçerli

Tam Gradle derlemesi, çalışma ortamında Gradle 9.4.1 dağıtımı bulunmadığı ve
dağıtım indirmesi ağ tarafından engellendiği için bu aşamada çalıştırılamadı.
