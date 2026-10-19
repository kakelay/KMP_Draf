# Jomnot API: KMP MVVM review sample

This sample adds real HTTP requests, not an in-memory catalog. Response models match the neighboring `jomnot-backend` Java records inspected during implementation. The supplied development Firebase configuration and localhost API are wired into the Android host; see [Firebase setup](FIREBASE_SETUP.md). Protected endpoints still need a signed-in Firebase user.

## MVVM structure

```text
sharedLogic/.../parking/
  domain/
    ParkingModels.kt               # UI-independent models and search validation
    ParkingRepository.kt           # Contract consumed by the ViewModel
    ParkingException.kt            # Domain failures
  data/
    remote/
      ApiModels.kt                 # JSON DTOs, envelope, DTO-to-domain mapping
      JomnotApi.kt                  # HTTP endpoints and Firebase token interface
    repository/
      ApiParkingRepository.kt      # Implementation and HTTP-to-domain errors
sharedUI/.../parking/
  presentation/
    ParkingScreen.kt               # View and lifecycle-aware route
    ParkingUiState.kt              # Immutable view state
    ParkingViewModel.kt            # Events, requests, state updates
androidApp/.../
  auth/DevelopmentFirebase.kt      # Native configuration and token adapter
  JomnotApplication.kt             # Dependency composition root
  MainActivity.kt                  # Host and Firebase session lifecycle
```

Read `domain/ParkingRepository.kt`, then `data/repository/ApiParkingRepository.kt`, then `presentation/ParkingViewModel.kt` and `presentation/ParkingScreen.kt` to follow the feature.

Flow: View events -> ViewModel -> repository interface -> API repository -> Ktor/Firebase -> backend. Domain objects and domain errors return to the ViewModel, which updates StateFlow for the View. The ViewModel imports only domain types for this feature; HTTP status codes, JSON DTOs, and Firebase SDK objects stay in the data layer or Android host. Presentation depends on the domain contract, and the application wires the implementation.

The screen demonstrates parking search and favorites. Home, account, details, customer favorites, zones, and notifications have typed client methods ready for their own repository interfaces and screens. Add feature repositories when those screens need them; avoid a repository that merely duplicates every API method.

## Connect the sample

Create the client and repository once in your application dependency container, outside composition:

```kotlin
val httpClient = createParkingHttpClient()
val api = JomnotApi(
    client = httpClient,
    baseUrl = "https://YOUR_BACKEND_HOST", // origin, without /v1
    tokens = firebaseTokenProvider,
)
val parkingRepository = ApiParkingRepository(api)
```

Pass the repository from the Android entry point:

```kotlin
setContent {
    App(parkingRepository = parkingRepository)
}
```

`App(parkingRepository)` opens the parking screen. If the repository is null because no API URL is configured, it shows a configuration message. The Android preview renders `ParkingScreen` directly without making API calls. The original banking demo has been removed. All network/domain code is in commonMain, with OkHttp on Android and Darwin on iOS. This project's sharedUI currently configures only Android; the sample does not add an iOS Compose entry point or change that target configuration. The API layer can already be consumed by the existing SharedLogic iOS framework.

Keep the client alive at application scope and close it when its owning container is disposed. Never create clients during recomposition. This sample has no credential storage or custom login endpoint.

## Firebase adapter

Implement `FirebaseTokenProvider` with your existing Firebase SDK. Resolve the current user and obtain a fresh usable **ID token** on each invocation. Do not return an OAuth access token or Firebase custom token. Firebase handles sign-in and registration.

For other Android hosts, an adapter can use `firebase-auth` plus `kotlinx-coroutines-play-services` (add these dependencies to your app's Android module):

```kotlin
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.tasks.await

val firebaseTokenProvider = FirebaseTokenProvider {
    FirebaseAuth.getInstance().currentUser
        ?.getIdToken(false)?.await()?.token
}
```

The Firebase SDK refreshes expired tokens for `getIdToken(false)`. Forward SDK failures instead of turning them into anonymous sessions. On iOS, implement the same suspend interface using your Firebase SDK bridge. Anonymous browsing uses `FirebaseTokenProvider { null }`; protected methods then fail before sending a request.

Public requests also receive the token when signed in, so parking cards receive personalized `isFavorite` values. On sign-in, sign-out, or account switch, clear the authenticated navigation destination and create a new destination with its own ViewModelStoreOwner. This clears the previous user's cards and cancels their in-flight work. Have the host's Firebase auth observer manage this transition. A Compose `key` alone does not clear a lifecycle ViewModel. The sample route assumes the Firebase identity stays the same during its lifetime.

A 401 is surfaced to the UI; this sample does not automatically replay mutations. Have your host sign-in flow handle reauthentication. The server enforces owner/admin access for `customerFavorites`; hiding UI is not authorization.

## Endpoint examples

```kotlin
api.home()
api.categories()
api.search(ParkingSearch(category = "mall", query = "parking", page = 0, size = 20))
api.location("parking-id")
api.account()
api.favorites(page = 0, size = 20)
api.customerFavorites("own-username-or-firebase-uid")
api.saveFavorite("parking-id")
api.removeFavorite("parking-id")
api.zones()
api.selectZone("zone-id")
api.notifications(skip = 0, take = 20)
api.markRead("notification-id")
```

The response envelope uses `responseCode == "00"` for success. Parking and favorite pages contain a second `data` list inside the envelope's `data` object. DELETE success may omit `data`. Unknown JSON fields are ignored, while required fields fail decoding. Monetary values are preserved as decimal text rather than converted to floating point. Nullable notification `readAt` may be omitted by the server.

For Android emulator development the host computer is usually `10.0.2.2`, not `localhost`. Android INTERNET permission is included. Prefer HTTPS; local HTTP requires a debug-only network security configuration. Do not globally enable cleartext traffic in release builds.

For a physical Android phone connected over USB, add `JOMNOT_API_BASE_URL=http://127.0.0.1:8080` to the ignored `local.properties` file, run `adb reverse tcp:8080 tcp:8080`, then rebuild and run the debug app. Keep the backend running on computer port 8080. Repeat `adb reverse` after reconnecting or restarting the phone. If using the local backend on port 8081, use port 8081 in both the URL and the reverse command. A `-PJOMNOT_API_BASE_URL=...` command-line property takes precedence over `local.properties`; without either setting, debug builds use the emulator address.

## Verification

```powershell
.\gradlew.bat :sharedLogic:testAndroidHostTest :sharedUI:testAndroidHostTest
```

API tests use Ktor MockEngine to verify URL parameters, current tokens on public requests, anonymous requests, protected-call rejection, decimal mapping, pagination, null DELETE data, and backend error references. ViewModel tests cover failed favorite writes and pagination retaining the submitted search when draft text changes. These are deterministic client tests, not proof that a deployed backend is reachable.

Manual live check after configuration: browse anonymously, search by category, load another page, sign in with Firebase, save a favorite, refresh and confirm it remains saved, remove it, then sign out and confirm customer calls require sign-in.

References: [Ktor requests](https://ktor.io/docs/client-requests.html), [Ktor client testing](https://ktor.io/docs/client-testing.html), [Multiplatform ViewModel](https://kotlinlang.org/docs/multiplatform/compose-viewmodel.html).

The Android host now includes `AndroidFirebaseTokenProvider` and supplies the configured named Firebase Auth instance. It also manages identity changes by clearing the Activity ViewModelStore and recreating the route. Use that instance for sign-in instead of the default instance in the generic example above.
