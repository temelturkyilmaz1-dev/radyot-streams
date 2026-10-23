# RadyoT 1.6.5.3 — Android Auto ses düzeltmesi

- `versionName`: **1.6.5.3**
- `versionCode`: **44**
- Tarih: **10.09.2026 (Türkiye saati)**

## Düzeltilen sorun

Android Auto bağlantısında radyo ekranda çalıyor görünmesine rağmen sesin gelmemesi;
başka bir medya uygulaması açılıp RadyoT'ye dönüldüğünde sesin başlaması.

## Yapılan değişiklikler

- Android Auto oynatma komutunda medya oturumu açıkça etkinleştiriliyor.
- Araçtan başlatılan istasyonlarda medya ses odağı zorunlu olarak alınıyor.
- Oynatıcı zaten çalışıyorsa kısa bir duraklat/devam ettir işlemiyle araç ses rotası yenileniyor.
- Boşta veya sona ermiş oynatıcı Android Auto'dan devam ettirilirken tekrar hazırlanıyor.
- İleri, geri, arama ve listeden istasyon seçme komutları da aynı güvenli Android Auto yolunu kullanıyor.
- Ağ yayını sırasında cihazın uykuya geçip bağlantıyı kesmesini önlemek için ağ uyandırma modu etkinleştirildi.
- Telefonda kullanılan **Kesintisiz ses** tercihi değiştirilmedi; zorunlu ses odağı yalnızca Android Auto komutlarında uygulanıyor.
- Android Auto oturumu servis oluşturulurken zaten etkinleştirildiği için callback içindeki gereksiz ikinci etkinleştirme kaldırıldı. Böylece callback kapsamındaki `isActive`/`setActive` isim çözümleme hatası giderildi.

## Araçta kontrol listesi

1. RadyoT telefonda kapalıyken Android Auto'ya bağlanıp RadyoT'den bir istasyon açın.
2. RadyoT telefonda çalarken Android Auto'ya bağlanın ve doğrudan RadyoT'yi açın.
3. Direksiyon/araç ekranından duraklatıp tekrar oynatın.
4. Sonraki ve önceki istasyona geçin.
5. Kabloyu veya kablosuz bağlantıyı kesip yeniden bağlanın.

Bu senaryoların her birinde başka bir medya uygulaması açmadan sesin araçtan gelmesi beklenir.
