# RadyoT – 01.08.2026 Toplu Güncelleme

## Android Auto ve kanal geçişi

- Telefon ve Android Auto ileri/geri düğmeleri aynı `Tüm Radyolar` sırasına bağlandı.
- İleri/geri eylemleri favori sayısından bağımsız olarak her zaman yayımlanıyor.
- Android Auto kuyruğu tek ve tutarlı bir radyo listesi kullanıyor.
- Albüm kapağı Android Auto servisi tarafından önceden indirilip doğrudan MediaSession'a aktarılıyor.

## Ayarlar

- `Hakkında`, Radyo Yönetimi içinden çıkarılıp ana Ayarlar ekranına ayrı kart olarak eklendi.
- Görüntü Ayarları; tema/dil, vurgu rengi, arka plan, oynatıcı öğeleri ve radyo görünümü olarak açılıp kapanan kartlara ayrıldı.
- Dil seçimi, ayrı ve büyük bir düğme yerine tema bölümü içinde tek seçimli listeye dönüştürüldü.
- Zamanlayıcı ekranı modern kartlar, dakika seçim çipleri ve ayrı alarm kartıyla yenilendi.

## Radyolar

- Kent FM, ses segmenti canlı olarak doğrulanan Wowza yayınına geçirildi ve resmî logosu yenilendi.
- Radio DRK, doğrulanmış Zeno AAC yayını ve resmî logosuyla eklendi.
- Radyo SU, doğrulanmış yayını, ICY metadata desteği ve resmî logosuyla eklendi.
- Radyo 45'lik yayını tekrar analiz edildi. Sunucu ICY alanında gerçek parça yerine sabit istasyon sloganı gönderiyor; doğrulanmamış haricî servis eklenmedi.

## Doğrulama

- `assembleDebug` başarıyla tamamlandı.
- APK paket kimliği ve Android bildirimi `aapt2` ile kontrol edildi.
- Kent FM AAC segmenti, Radio DRK AAC akışı ve Radyo SU yayın/metadata yanıtı canlı olarak doğrulandı.

