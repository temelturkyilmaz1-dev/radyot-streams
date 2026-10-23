# RadyoT GitHub entegrasyonu — 05.08.2026

- Uygulama `radyot-streams/stations.json` dosyasını açılışta kontrol eder.
- Yalnızca şema ve asgari kayıt sayısı doğrulanan JSON kabul edilir.
- GitHub ulaşılamazsa APK içindeki 209 kanallı katalog aynen çalışır.
- Son doğrulanmış katalog yedi gün boyunca cihazda önbellekte tutulur.
- GitHub yeni logo üretemez; uzaktan yalnızca APK'da bulunan kanalların yayın adresi ve etkinlik durumu yönetilir.
- `health.json` saatte bir arka planda kontrol edilir.
- Üç ardışık GitHub kontrolünde `offline` olan istasyonlar için tek bildirim gönderilir.
- Yayın yeniden açıldığında iyileşme bildirimi gönderilir.
- Aynı arıza için tekrar tekrar bildirim gönderilmez.
- Android 13 ve üzerindeki cihazlarda bildirim izni ilk açılışta istenir.
- Zorunlu arka plan oynatma bildirimi sessiz ve sabittir; şarkı değişiminde yenilenmez.
- Arıza ve düzelme uyarıları yalnızca `health.json` bildirim kanalından gelir.
- XML, tema, ekran ölçüleri ve oynatıcı düzeni değiştirilmedi.

## GitHub tarafında bekleyen tek işlem

Depodaki `stations.json` hâlâ 175 kanallıdır. Güncel 209 kanallı dosya
`/tmp/radyot_stations_209.json` olarak üretildi; bağlı GitHub uygulaması yazma
yetkisi vermediği için sunucuya gönderilemedi (HTTP 403).
