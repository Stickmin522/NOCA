# NOCA

An offline NFC reader for Japanese transit IC cards, built with Rust and Jetpack Compose.

[Download APK](https://github.com/Stickmin522/NOCA/releases/latest) · [中文](#中文) · [日本語](#日本語) · [한국어](#한국어) · [Français](#français) · [Español](#español) · [العربية](#العربية)

## Features

- Read the balance and recent transactions from compatible FeliCa transit cards.
- See entry and exit stations and their lines for rail journeys, using a bundled offline station directory.
- Remember cards with a name and artwork; scan them again to update their balance and history.
- Keep previously read transactions on your device, even after they leave the card's history.
- Choose a card design or import your own photo. Delete saved cards and their history at any time.
- High-resolution card artwork with realistic printed plastic surfaces and physical card outlines.
- Material 3 Expressive interface with raised buttons, press feedback, and ticket-shaped balances.
- Lime, Mist Blue, and Coral themes with light, dark, and system modes.
- System fonts, automatic language selection, and a searchable language picker.
- Tap the balance to return to the animated contactless reading prompt.
- A full-turn reading animation leads into the balance. Optional sounds play two short beeps for success and one long beep for failure.

Language selection follows the device's preferences, with English as a fallback when a translation is unavailable.

## Getting started

Install the **arm64-v8a APK** on an NFC-F-capable phone running **Android 10 or later**. Enable NFC, open NOCA, and hold one transit card against the phone's NFC antenna. Choose **Remember this card** to add it to your wallet.

The available history depends on the card. Transaction amounts use the balance difference between neighbouring card history blocks, including fare deductions and top-ups. Missing blocks and an unavailable older balance are shown as a dash. NOCA reads card data; it does not top up cards or replace a physical transit pass.

Station names are shown in Japanese. Unknown station codes remain visible without guessing a name; bus records do not use the rail station directory.

## Privacy

NOCA works without an account or internet access. Card information and imported artwork stay in the app's private storage. The app has no internet permission, advertising, analytics, or cloud backup. Imported photos are saved without location or camera metadata.

## Build

Use JDK 21 and the pinned Rust and Gradle versions. Install Android SDK Platform 37.0, Build Tools 36.0.0, NDK 28.2.13676358, and CMake 3.22.1, then set `ANDROID_HOME` to your SDK directory.

```sh
rustup target add aarch64-linux-android
cargo test --manifest-path native/rust/Cargo.toml --locked
./scripts/build-android.sh :app:testDebugUnitTest :app:lintRelease :app:assembleRelease
```

Release builds are unsigned unless `NOCA_KEYSTORE_PATH`, `NOCA_KEYSTORE_PASSWORD`, `NOCA_KEY_ALIAS`, and `NOCA_KEY_PASSWORD` are provided. Keep signing files and passwords outside the repository.

The source code is licensed under [MIT](LICENSE). Card images and issuer trademarks belong to their respective owners; see [artwork credits](docs/ASSETS.md).
The bundled station directory is also MIT-licensed; see [station data](docs/STATIONS.md).

## 中文

NOCA 是一款离线的日本交通 IC 卡读取应用。将交通卡贴近 Android 手机，即可查看余额和最近交易；保存卡片后可自定义名称与卡面，再次贴卡时自动更新。支持自定义照片、三种主题配色、明暗模式和语言选择，所有卡片数据仅保存在本机。

需要 Android 10 或更高版本，以及支持 NFC-F 的 ARM64 手机。

## 日本語

NOCA は、日本の交通系 IC カードの残高と利用履歴をオフラインで確認できる Android アプリです。カードをかざして読み取り、名前や券面を設定して保存できます。同じカードを再びかざすと残高と履歴を更新します。写真による券面のカスタマイズ、3 種類の配色、ライト・ダークモード、言語選択に対応し、カード情報は端末内だけに保存されます。

Android 10 以降と、NFC-F に対応した ARM64 端末が必要です。

## 한국어

NOCA는 일본 교통 IC 카드의 잔액과 최근 이용 내역을 오프라인으로 확인하는 Android 앱입니다. 카드를 휴대전화에 대어 읽고, 이름과 카드 디자인을 지정해 저장할 수 있습니다. 같은 카드를 다시 읽으면 잔액과 내역이 갱신됩니다. 사진으로 카드 디자인을 꾸미고 세 가지 테마 색상, 밝은 모드와 어두운 모드, 앱 언어를 선택할 수 있습니다. 카드 정보는 기기에만 저장됩니다.

Android 10 이상 및 NFC-F를 지원하는 ARM64 휴대전화가 필요합니다.

## Français

NOCA permet de consulter hors ligne le solde et les transactions récentes des cartes de transport IC japonaises. Approchez une carte de votre téléphone Android, puis enregistrez-la avec un nom et un visuel. Une nouvelle lecture actualise son solde et son historique. Personnalisez vos cartes avec des photos et choisissez parmi trois couleurs, les modes clair et sombre et plusieurs langues. Les données restent sur votre appareil.

Android 10 ou version ultérieure et un téléphone ARM64 compatible NFC-F sont nécessaires.

## Español

NOCA permite consultar sin conexión el saldo y las transacciones recientes de las tarjetas IC de transporte japonesas. Acerca una tarjeta a tu teléfono Android y guárdala con un nombre y una imagen. Al volver a leerla, se actualizan el saldo y el historial. Puedes usar tus propias fotos y elegir entre tres colores, los modos claro y oscuro y varios idiomas. Los datos permanecen en tu dispositivo.

Requiere Android 10 o posterior y un teléfono ARM64 compatible con NFC-F.

## العربية

يتيح NOCA قراءة رصيد بطاقات النقل اليابانية IC ومعاملاتها الأخيرة دون اتصال بالإنترنت. قرّب البطاقة من هاتف Android، ثم احفظها باسم وصورة من اختيارك. عند قراءتها مجددًا، يُحدّث الرصيد وسجل المعاملات تلقائيًا. يمكنك تخصيص صورة البطاقة واختيار أحد الألوان الثلاثة والوضع الفاتح أو الداكن ولغة التطبيق. تبقى جميع بيانات البطاقات على جهازك فقط.

يتطلب Android 10 أو أحدث وهاتفًا بمعمارية ARM64 يدعم NFC-F.
