# RadyoT Geliştirme Paketi — 29.07.2026

## Açılış

- RadyoT logosuna yumuşak belirme ve hafif büyüme animasyonu eklendi.
- Vurgu rengini kullanan genişleyen halka efekti eklendi.
- Animasyondan ana ekrana yumuşak geçiş eklendi.
- Ana ekranın mevcut yerleşimi değiştirilmedi.
- Derlenen kurulum dosyasının adı `RadyoT.apk` olarak ayarlandı.

## Ayarlar

- Görsel seçenekler **Görüntü Ayarları** altında toplandı.
- Ekran görünümü, vurgu rengi ve fluluk seçenekleri bu bölüme taşındı.
- Arka plan ve son çalanlar saydamlık seviyeleri 0, 20, 40, 60, 80 ve 100 olarak düzenlendi.
- Kesintisiz ses, pil optimizasyonu ve ekolayzır seçenekleri korundu.

## Android Auto

- Şarkı geçmişi desteği eklendi.
- Favorilere ekleme/çıkarma eylemi eklendi.
- Çıkış eylemi eklendi.
- Radyo logosu ve albüm kapağının dönüşümlü gösterimi eklendi.

## Radyo listesi

- Çalışmayan çok sayıda yayın adresi güncellendi.
- Uygun istasyonlara yedek yayın adresleri eklendi.
- Number1 Türk mükerrer kaydı temizlendi.
- Number1 FM yayını ve logosu güncellendi.
- Fenomen Türk logo eşlemesi düzeltildi.
- Pal Nostalji, Pal Doğa, Seymen FM ve diğer eksik istasyon logoları güncellendi.
- TRT Radyo 3, TRT Türkü ve TRT Nağme yayınları yenilendi.
- Radyo ODTÜ'nün bulunan ek kanalları eklendi.
- Radio Voyage yeniden eklendi.
- Resmî veya canlı sesle doğrulanabilen çok sayıda ulusal, yerel ve dijital radyo eklendi.
- Son kontrolde toplam 141 radyo bulundu ve mükerrer normalleştirilmiş ad tespit edilmedi.

Son eklenen doğrulanmış istasyonlardan bazıları:

- Dedemin Radyosu
- Hoş Mix
- Radyo Kilim
- Radyo Hayat
- Number1 Türk Rap
- Mydonose Türk
- Barış Radyo
- DT Nostalji
- Yol Arkadaşım
- Joy Jazz
- Power Greece
- Gönül Türküleri
- Radyo 90
- Kiss Club FM

## Doğrulama

- Android kaynak dosyalarındaki görsel başvuruları kontrol edildi; eksik drawable bulunmadı.
- Proje `:app:assembleDebug` göreviyle başarıyla derlendi.
- Oluşan APK: `RadyoT.apk`

## Logo paketi güncellemesi

- Kullanıcının gönderdiği logo arşivindeki 72 kullanılabilir görsel Android kaynak adlandırmasına uygun hâle getirildi.
- Türkçe karakterler, boşluklar ve büyük harfler güvenli dosya adlarına dönüştürüldü.
- Mevcut istasyonlarla kesin eşleşen logolar radyo listesine bağlandı.
- Number1 FM, Number1 Türk, Fenomen grubu, Power grubu, Radyo ODTÜ alt kanalları, Pal Doğa, Kiss Club FM ve çok sayıda yerel/dijital istasyonun logo eşlemeleri yenilendi.
- Belirsiz eşleşmeler zorla uygulanmadı. Aşk FM ve Radyo Mydonose için uygun kesin logo bulunamadığından mevcut genel görsel korundu.
- Arşivde Kiss FM logosu bulunmasına rağmen doğrulanmış yayın adresi olmadığı için yalnızca görsel kaynak olarak saklandı; sahte veya çalışmayan istasyon eklenmedi.
- Toplam 137 radyo kaydı kontrol edildi; mükerrer ad ve eksik görsel kaynağı bulunmadı.

## Son düzeltmeler

- Kullanıcının yeniden gönderdiği Aşk FM logosu eşleştirildi.
- Kullanıcının yeniden gönderdiği Radyo Mydonose logosu eşleştirildi.
- Kiss FM'in resmi web uygulamasındaki radyo servisinden gerçek yayın adresi çıkarıldı.
- Kiss FM, doğrulanan `https://kiss.radyotvonline.net/kissfm` MP3 yayını ve kendi logosuyla eklendi.
- Kiss FM'in resmi radyo listesindeki 22 Kiss kanalı, resmi yayın adresleri ve logolarıyla eklendi.
- Eski harici Kiss Club FM kaydı kaldırılarak resmi Kiss Club yayınına geçirildi; mükerrer kayıt oluşturulmadı.
- Kiss grubundaki 22 yayının tamamı canlı bağlantı kontrolünden geçti: 22/22 yayın HTTP 200 ve geçerli ses türü döndürdü.
- Son listede 158 radyo bulunuyor; mükerrer ad ve eksik görsel kaynağı bulunmuyor.

## 07.09.2026 — Büyük katalog / 1000+ kanal altyapısı
- 678 tabandan 4 haber/gündem kanalı çıkarıldı.
- Metadata zorunluluğu kaldırıldı; çalışan yayın + logo kuralı uygulandı.
- Onaylı ana ağların alt/yan müzik kanallarını çalışan stream ve logo ile dinamik ekleyen katalog genişletici eklendi.
- Radio Browser çoklu mirror + önbellek + mükerrer/haber/dini filtreleme eklendi.
- Uzaktan logo desteği ana UI, Chromecast ve Android Auto açıklamalarına genişletildi.
- GitHub stations.json yeni kanalları `logo` alanı ile ekleyebilir hale getirildi.
# 1.6.5.23 — 15.10.2026

- Telefon, TV Box ve Android Auto araması kelimeleri metnin herhangi bir yerinde
  arayacak biçimde ortaklaştırıldı.
- Android Auto ileri/geri oynatma ekranı ve eski Radyo Burada önbelleği düzeltildi.
- Beş yeni radyo Son Eklenenler listesine alındı; mevcut istasyon güncellemeleri
  bu listeye eklenmedi.
- Çok sayıda yerel radyo logosu düzeltildi; Armoni FM, Devrek FM, Radyo Kulüp,
  Ereğli FM ve Enerji FM resmî kaynaklarla güncellendi.
- Proje hafızası ve paketler arası değişiklik günlüğü eklendi.

