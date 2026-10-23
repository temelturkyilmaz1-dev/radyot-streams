# RadyoT 1.6 - ANR / Açılış Düzeltmesi

Bu paket `RadyoT_v1.6_1000plus_logo_duzeltmeleri_20260907.zip` tabanından hazırlanmıştır.

Düzeltmeler:
- 1000+ geniş katalog cache'i artık MainActivity açılmadan senkron parse edilmez.
- Büyük katalog yükleme/parse işlemi arka planda yapılır; UI önce gömülü katalogla açılır.
- MediaBrowserService başlangıcındaki senkron geniş katalog cache yüklemesi kaldırıldı.
- Büyük katalog birleştirmesi hash tabanlı O(n) yapıya çevrildi; binlerce kayıtta gereksiz tekrar taramalar azaltıldı.
- UI, oynatma, swipe, Chromecast, metadata ve logo düzeltmeleri korunmuştur.
- Sürüm: 1.6 / versionCode 40.
