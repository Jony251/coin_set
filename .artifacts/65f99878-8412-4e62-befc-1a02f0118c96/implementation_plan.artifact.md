# Migration to Amazon-hosted API

The goal is to migrate the application from using Firebase (Firestore/Auth/Storage) to the new API hosted on AWS (https://coinset.bluecat.cc/).

## User Review Required

> [!IMPORTANT]
> This migration will replace Firebase Authentication with the custom API authentication. Users will need to log in again using their API credentials.
>
> [!WARNING]
> Some Firebase-specific features like "find_arr" (wishlist for missing countries) might need to be implemented on the backend or kept in Firebase if the API doesn't support them yet. For now, I'll focus on the core functionality (Catalog and Collection).

## Proposed Changes

### [API Layer]

#### [MODIFY] [RetrofitClient.kt](file:///E:/coinset/app/src/main/java/com/example/coinset/api/RetrofitClient.kt)
- Update `BASE_URL` to `https://coinset.bluecat.cc/`.

### [Authentication]

#### [MODIFY] [AuthScreens.kt](file:///E:/coinset/app/src/main/java/com/example/coinset/ui/auth/AuthScreens.kt)
- Update `LoginScreen` and `RegisterScreen` to use `AuthRepository` (Retrofit) instead of `Firebase.auth`.

#### [MODIFY] [MainActivity.kt](file:///E:/coinset/app/src/main/java/com/example/coinset/MainActivity.kt)
- Update `RootNavigation` to check `TokenManager` for a saved access token instead of checking `Firebase.auth.currentUser`.

### [Catalog]

#### [MODIFY] [CatalogScreens.kt](file:///E:/coinset/app/src/main/java/com/example/coinset/ui/catalog/CatalogScreens.kt)
- Migrate `CoinListScreen`, `CoinTypeScreen`, and `CoinDetailScreen` to use `CatalogRepository`.
- Ensure all screens handle IDs as `Int` where required by the API.

### [Collection]

#### [MODIFY] [CollectionScreens.kt](file:///E:/coinset/app/src/main/java/com/example/coinset/ui/collection/CollectionScreens.kt)
- Migrate `MyCollectionScreen` to use `CollectionRepository` for fetching the user's coins and statistics.

## Verification Plan

### Automated Tests
- N/A (Manual verification on device/emulator is preferred for UI changes).

### Manual Verification
1. Deploy the app to an emulator.
2. Verify registration and login flows (check if tokens are saved).
3. Verify Catalog browsing (Countries -> Rulers -> Categories -> Coins).
4. Verify adding a coin to the collection.
5. Verify the collection screen shows the added coins and correct statistics.
