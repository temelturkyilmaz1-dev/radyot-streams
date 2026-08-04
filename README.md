# RadyoT Streams

RadyoT yayın adresleri ve otomatik sağlık kontrol altyapısı.

## Dosyalar

- `stations.json`: Uygulamadaki 175 istasyonun ana ve yedek yayın adresleri.
- `scripts/check_streams.py`: Adreslere gerçek `GET` isteği gönderir; HLS/playlist yayınlarında ilk medya öğesini de doğrular.
- `health.json`: GitHub Actions ilk kez çalıştıktan sonra otomatik oluşur.
- `.github/workflows/check-streams.yml`: Her saat ve elle çalıştırılabilen kontrol görevi.

## Güvenlik yaklaşımı

Kontrol sistemi yayın adreslerini kendiliğinden değiştirmez ve istasyon silmez. Yalnızca durum raporu üretir. Bir istasyon art arda üç kontrolde açılamazsa `offline`, ilk iki başarısızlıkta `warning` olarak işaretlenir.

## Elle çalıştırma

GitHub'da **Actions → Radyo yayınlarını kontrol et → Run workflow** yolunu kullanın. İlk çalışma tamamlandığında `health.json` depoya eklenir.

## Uygulama entegrasyonu

Bu ilk aşama yalnızca izleme altyapısıdır. RadyoT henüz bu dosyaları uzaktan okuyup listesini otomatik değiştirmez. Uygulama entegrasyonu ayrıca, yerel önbellek ve gömülü yedek liste korunarak yapılacaktır.
