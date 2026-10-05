<div align="center">

# Coin Set

**A native Android app for coin collectors: browse a catalogue by country, era and ruler, and keep track of your own collection. Kotlin + Jetpack Compose, backed by its own FastAPI service.**

[Landing page: coinset.bluecat.cc](https://coinset.bluecat.cc) ·
[Live API docs (Swagger)](https://coinset.bluecat.cc/docs) ·
[Backend repo: coinset-api](https://github.com/Jony251/coinset-api)

![Kotlin 2.0](https://img.shields.io/badge/Kotlin-2.0-7F52FF?logo=kotlin&logoColor=white)
![Jetpack Compose](https://img.shields.io/badge/Jetpack%20Compose-Material%203-4285F4?logo=jetpackcompose&logoColor=white)
![Android](https://img.shields.io/badge/Android-minSdk%2024%20%C2%B7%20target%2034-3DDC84?logo=android&logoColor=white)
![Retrofit](https://img.shields.io/badge/Retrofit-2.9-48B983)

</div>

## What it does

Coin Set is the mobile half of a two-part system. The app talks to a REST API that I also built ([coinset-api](https://github.com/Jony251/coinset-api), FastAPI + PostgreSQL), running in production at `coinset.bluecat.cc`.

- **Catalogue:** country → era → ruler → coin type → coin detail, with metal, rarity, years of striking, mint, mint master, edge and images.
- **Collection:** add a coin with its condition and purchase price, attach your own photo, edit or remove it, keep a wishlist, and see a summary of how many coins you own and what you paid.
- **Account:** registration and login with JWT; the session survives restarts.
- **Languages:** English, Russian and Hebrew, switchable inside the app, with a right-to-left layout for Hebrew.

## Highlights

- **Jetpack Compose + Material 3** throughout, with its own design tokens (`Color`, `Type`, `Shape`, `Spacing`) and light/dark themes that follow the system setting.
- **Two-level navigation.** A root graph switches between the auth flow and the main app; the main app has a bottom navigation bar with its own nested graph, and tapping the active tab returns to that tab's root.
- **Silent token refresh.** An OkHttp `Authenticator` catches a 401, refreshes the access token once (synchronised, so parallel requests don't trigger several refreshes) and retries the original request. Tokens are stored in Jetpack DataStore.
- **Complete data, not just the first page.** The repository layer pages through the API (100 coins per request, with an upper bound as a safety guard), so long reigns are never silently cut off.
- **Product feedback loop.** Country searches that find nothing are logged to the backend, which shows which countries collectors are missing from the catalogue.
- **Per-app language.** Uses the AndroidX per-app locale API (`AppCompatDelegate.setApplicationLocales`), with 184 string resources kept in parity across English, Russian and Hebrew (`values-he` and the legacy `values-iw`).
- **Images** load with Coil; catalogue photos can be absolute URLs or files served by the backend's `/uploads`.

## Tech stack

**Language and UI:** Kotlin 2.0 · Jetpack Compose (BOM 2024.09) · Material 3 · Material Icons Extended · Navigation Compose · AppCompat (per-app locales)
**Networking and data:** Retrofit 2 + Gson · OkHttp 4 (logging interceptor, token authenticator) · Jetpack DataStore (Preferences) · Coil
**Build:** Gradle Kotlin DSL with a version catalog (`gradle/libs.versions.toml`) · Android Gradle Plugin 8.7
**Backend:** [coinset-api](https://github.com/Jony251/coinset-api): FastAPI · SQLAlchemy · PostgreSQL · JWT

## Project structure

```
app/src/main/java/com/example/coinset/
  MainActivity.kt          root navigation (auth / main) and bottom navigation bar
  api/
    CoinsetApi.kt          Retrofit interface: auth, countries, periods, rulers,
                           coins, user-coins, news, premium status
    RetrofitClient.kt      base URL, OkHttp client, image URL helper
    TokenAuthenticator.kt  refresh-on-401 and retry
    TokenManager.kt        access/refresh tokens in DataStore
    Repositories.kt        Result-returning repositories, pagination
    ApiModels.kt           request/response models
  ui/
    auth/                  login and registration
    home/                  home: your coins, wishlist and news
    catalog/               countries, eras, rulers, coin types, coin detail
    collection/            "My collection"
    settings/              settings, language picker, premium screen
    components/            shared UI and numismatic components
    theme/                 color, typography, shapes, spacing, theme
app/src/main/res/values{,-ru,-he,-iw}/strings.xml
```

## Getting started

Requires Android Studio (with its bundled JDK 17) and an emulator or a device on Android 7.0 (API 24) or newer.

```bash
git clone https://github.com/Jony251/coin_set.git
cd coin_set
./gradlew assembleDebug     # build the debug APK
./gradlew installDebug      # install on a connected device or running emulator
```

Or open the project in Android Studio and run the `app` configuration.

The app points to the production API by default (`BASE_URL` in `app/src/main/java/com/example/coinset/api/RetrofitClient.kt`). To use a local [coinset-api](https://github.com/Jony251/coinset-api) instead, change `BASE_URL` to your server's address (from the Android emulator, the host machine is `10.0.2.2`). Android blocks plain-HTTP traffic by default, so use an HTTPS endpoint or allow cleartext traffic for your debug build.

## Author

**Evgeny Levitan**, full-stack developer (web and Android), Israel. Open to full-stack and frontend roles and to freelance projects.

[bluecat.cc](https://bluecat.cc) · [LinkedIn](https://www.linkedin.com/in/evgeny-nemchenko) · [nevgeny90@gmail.com](mailto:nevgeny90@gmail.com) · [GitHub @Jony251](https://github.com/Jony251)
