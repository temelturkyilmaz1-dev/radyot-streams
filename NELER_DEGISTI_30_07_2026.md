# RadyoT – 30.07.2026 geliştirme paketi

## Düzeltilenler

- Ana ekrandaki ses çubuğunun alanını daraltan 116 dp genişliğindeki RadyoT
  logosu 48 dp'ye indirildi. Ses çubuğuna en az 96 dp gerçek dokunma alanı verildi.
- Ayarlar artık uzun tek sayfa değildir. Görüntü Ayarları, Oynatıcı Ayarları,
  Zamanlayıcı ve Radyo Yönetimi ayrı alt ekranlar olarak açılır.
- Ayarlar/Çıkış düğmeleri taşındıktan sonra kaldırılan eski üst çubuğa yapılan
  açılış erişimi temizlendi. İlk test APK'sındaki ana ekran açılış çökmesi giderildi.
- Android Auto ileri/geri komutları artık Android Auto'da son açılan Favoriler,
  Tüm Radyolar veya Son Dinlenenler listesinin sırasını kullanır.
- Manuel ekolayzırda geçersiz/kapanmış ses efekti oturumuna erişim güvenli hale
  getirildi. Desteklenmeyen veya kopan oturum uygulamayı kapatmak yerine kullanıcıya
  bilgi verir.
- Ayarlar ve Çıkış düğmeleri, dikey ekranda yarı yarıya düzen korunarak oynatıcı
  kartının alt kontrol sırasına taşındı.
- Kent FM ana adresi çalışan edge yayınına geçirildi; master ve eski doğrudan
  yayın adresleri yedek olarak korundu.
- Radyo 45'lik için ICY isteyen doğrudan AAC yayın ana adres olarak korundu.
  Sunucu `audio/aac` ve `icy-metaint:16000` ile doğrulandı. Ancak canlı ICY
  içeriği şarkı yerine istasyon sloganı gönderdiği için slogan artık şarkı
  metadata'sı olarak gösterilmez. Ayrı şarkı servisi henüz doğrulanmadı.

## Eklenen radyolar

- Metropol FM 45'lik: Resmî Metropol FM yönlendirme adresi.
- Radyo Evin: Resmî web oynatıcısındaki Shoutcast adresi ve resmî logo.
- Radyo Hêvî: Bilinen yayın adresi katalogda yer alıyor; 30.07.2026 canlı
  kontrolde yayın sunucusu cevap vermediği için çalıştığı doğrulanamadı.

## Ayarlar ve yeni özellikler

- Ayarlar; Görüntü Ayarları, Oynatıcı Ayarları, Dil ve Zamanlayıcı ile Radyo
  Yönetimi başlıkları altında toplandı.
- Dil seçimi: Sistem dili, Türkçe, İngilizce ve Almanca.
- Uyku zamanlayıcısı: 15, 30, 45, 60, 90 ve 120 dakika.
- Radyo alarmı: Saat ve istasyon seçerek bir sonraki uygun saatte yayını başlatma.
- Yeni İstasyon Ekle: İstasyon adı, ana yayın adresi ve isteğe bağlı yedek adres.
  Eklenen istasyonlar kalıcı olarak saklanır.

## Doğrulama

- `assembleDebug` görevi başarıyla tamamlandı.
- Üretilen test APK'sı: `app/build/outputs/apk/debug/app-debug.apk`
# Görsel düzenleme paketi

- Android Auto ileri/geri komutları yalnızca Favoriler listesinden açılan kanallarda etkinleştirildi ve yalnızca favoriler arasında ilerleyecek şekilde sabitlendi. Tüm Radyolar’dan açılan kanallarda bu komutlar sunulmuyor.
- Android Auto albüm kapağı araması telefon ekranından bağımsız olarak medya servisinde çalışacak hale getirildi.
- Radyo ekleme ekranındaki istasyon adı alanı durum çubuğunun altındaki güvenli alana taşındı.
- Radyo Yönetimi bölümüne yalnızca kullanıcının eklediği istasyonları listeleyen ve onayla silen seçenek eklendi.
- Kent FM HLS sunucusunun istediği oturum çerezini korumak için oynatıcıya çerez sürekliliği eklendi; iki HTTPS yayın adresi canlı olarak doğrulandı.
- Tüm Radyolar ekranı artık sistem gezinme alanını gerçek ekran sınırı olarak kullanıyor; dikeyde alt satır, yatayda en sağ sütun gezinme çubuğunun altında kalmıyor.
- Ayarlar ana sayfasındaki “Ayarlar” başlığı kaldırıldı; geri düğmesi saat/durum çubuğunun altındaki güvenli alana taşındı.
- Manuel ekolayzır sürgüsü hareket ederken profil listesinin bant ekranını yeniden oluşturması tamamen kaldırıldı.
- Yatay ekranda sol oynatıcı kartının ilave üst/alt boşlukları kaldırıldı; sağ favoriler bloğuyla aynı üst ve alt seviyeye getirildi.
- Manuel ekolayzır hareketi sırasında oluşabilen ses oturumu/effect yarışması kilitlendi.
- Manuel bant değiştirilirken profil menüsünü eşzamanlı yenileyen işlem kaldırıldı; “Özel” seçimi parmak bırakıldıktan sonra güvenli biçimde yapılıyor.
- Ayarlar ana ekranındaki büyük, düz renkli düğmeler kaldırıldı; yüzey rengi, ince çerçeve, başlık/açıklama ve yön oku kullanan daha sade kartlara dönüştürüldü.
- Ayarlar ve Görüntü Ayarları ekranlarına geri düğmesi eklendi.
- Vurgu rengi seçimi yeniden küçük, gerçek renkli daireler haline getirildi; seçili renk çerçeveyle belirtiliyor.
- Görüntü Ayarları ile Tüm Radyolar ekranının alt içeriğine telefonun gezinme çubuğu dikkate alınarak güvenli boşluk eklendi.
- Yatay ana ekrandaki ayrı üst düğme şeridi kaldırıldı. Oynatıcı ve favoriler yukarı taşındı.
- Yatay ekranda Ayarlar ve Çıkış düğmeleri oynatıcı kartının ses satırına, rahat dokunulabilen 48 dp alanlarla yerleştirildi.
- Uygulama yeniden derlendi ve APK üretimi doğrulandı.
