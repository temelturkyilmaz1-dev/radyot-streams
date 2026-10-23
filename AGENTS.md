# RadiOnline çalışma talimatı

Bu dosya yeni bir Codex sohbetinde ilk olarak okunmalıdır. Her işleme başlamadan önce
`PROJECT_MEMORY.md` ve `CHANGELOG_CURRENT.md` dosyalarını da oku. Bu üç dosya ZIP'e
dahil edilmeli ve proje hakkındaki kalıcı hafıza olarak korunmalıdır.

## Değişmez kurallar

- Kullanıcı istemeden APK veya ZIP hazırlama.
- Sürüm numarasını yalnızca kullanıcı ZIP/APK istediğinde bir kez artır; eklenen her
  radyo için artırma.
- `Son Eklenenler` listesine yalnızca son paket döneminde gerçekten yeni eklenen
  radyoları koy. Güncellenen veya logosu düzeltilen eski radyoları koyma.
- Yeni radyo eklemeden önce ad, standartlaştırılmış ad ve yayın adresiyle mevcut
  kayıtları kontrol et. Varsa yeni kayıt açma; mevcut kaydı güncelle.
- Yayın, logo ve web adresinde önce radyonun resmî kaynağını kullan. Rehber
  sitelerini yalnızca doğrulama veya son çare olarak kullan.
- Resmî logo bulunamazsa kullanıcıya söyle. Baş harflerden sahte logo üretme.
- Telefon, TV Box ve Android Auto aynı radyo kaydını ve logo çözümleme zincirini
  kullanmalıdır.
- Çalışmıyor görünen radyoyu tek kısa kontrolden sonra silme; yedek adresleri,
  geçici sunucu sorununu ve GitHub sağlık geçmişini kontrol et.
- Her maddi değişikliği aynı oturumda `CHANGELOG_CURRENT.md` dosyasına ekle.
  Mimari veya davranış kuralı değiştirildiyse `PROJECT_MEMORY.md` dosyasını da
  güncelle.

## Teslimden önce

- Eksik drawable başvurusu, mükerrer ad/yayın, `recentlyAddedStationKeys` ve arama
  davranışını kontrol et.
- Mümkünse birim testlerini ve Android derlemesini çalıştır. Ortam engelini başarılı
  test gibi sunma.
- ZIP kaynak kodu, bu hafıza dosyalarını ve gerekli logoları içermeli; gereksiz
  derleme önbelleklerini içermemelidir.
