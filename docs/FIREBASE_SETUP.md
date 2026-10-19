# Development Firebase configuration

Android now initializes the supplied `jomnot-merchan` Firebase project natively using `FirebaseOptions`. There is no Flutter runtime dependency and no `google-services.json` or Google Services Gradle plugin requirement for this programmatic Auth setup.

- Configuration and token adapter: `androidApp/src/main/kotlin/org/example/project/auth/DevelopmentFirebase.kt`.
- Application dependencies: `JomnotApplication.kt` initializes the named Firebase app, Auth, application-scoped HTTP client, and parking repository.
- `MainActivity` opens the parking MVVM screen and clears its ViewModels when the Firebase UID changes.
- Firebase Auth comes from the main `firebase-auth` module and Firebase BoM; coroutine Tasks use `await()`.

## Backend URL

Your supplied endpoint is `http://127.0.0.1:8080/v1/app/home`. The client expects only the origin and adds `/v1/...` itself.

The Android debug build defaults to `http://10.0.2.2:8080`, the host computer loopback alias inside the standard Android emulator. Debug network security permits HTTP only for `10.0.2.2`, `127.0.0.1`, and `localhost`. The release manifest has no such exception and the application requires HTTPS when a release URL is configured.

Override the base URL with a Gradle property:

```powershell
.\gradlew.bat :androidApp:assembleDebug -PJOMNOT_API_BASE_URL=https://api.example.com
```

For a physical device connected with ADB, forward the device loopback port and override the base URL:

```powershell
adb reverse tcp:8080 tcp:8080
.\gradlew.bat :androidApp:assembleDebug -PJOMNOT_API_BASE_URL=http://127.0.0.1:8080
```

The release default URL is empty, which opens the original demo. Set an HTTPS URL for release integration. Firebase values in this sample are the development project values you supplied.

## Sign-in and tokens

Initialization does not sign a user in. Anonymous public browsing works immediately. Use the application container's `firebaseAuth` for whichever Firebase sign-in provider your project enables, and the API adapter will obtain that user's current ID token on each request. Because this is a named Firebase app, do not use a separate `FirebaseAuth.getInstance()` default instance.

For example, if Email/Password is enabled, a sign-in repository on Android can call:

```kotlin
suspend fun signIn(auth: FirebaseAuth, email: String, password: String) {
    auth.signInWithEmailAndPassword(email, password).await()
}
```

Keep passwords out of ViewModel saved state and logs. This change does not assume a provider or add a login form.

The app's current Android applicationId is `org.example.project`. The pasted Flutter options do not include the registered Android package name. Confirm this package is registered in Firebase, or obtain the Firebase options for a new Android app registered with this package. Phone/Google sign-in can also require registered signing certificate fingerprints and provider-specific configuration.

The backend must validate Firebase tokens for project `jomnot-merchan`. Firebase initialization and API client tests do not verify that server configuration or perform a real sign-in.

## iOS values and remaining setup

The current native iOS app has no Firebase package dependency and uses bundle identifier `org.example.project.KMM-App1$(TEAM_ID)`. The supplied Firebase registration belongs to `com.example.verygoodcore.donutly.dev`. Configure the appropriate app registration before enabling Auth in that target.

On macOS, add `FirebaseCore` and `FirebaseAuth` from the official `firebase-ios-sdk` Swift package to the Xcode app target. Then initialize once in the Swift app initializer with the supplied iOS values:

```swift
import FirebaseCore

let options = FirebaseOptions(
    googleAppID: "1:1014341937975:ios:401535c27872f3aa5948b2",
    gcmSenderID: "1014341937975"
)
options.apiKey = "YOUR_IOS_FIREBASE_API_KEY"
options.projectID = "jomnot-merchan"
options.storageBucket = "jomnot-merchan.firebasestorage.app"
options.clientID = "1014341937975-cta3takdtaad44ovmu0qobujpv1f68bn.apps.googleusercontent.com"
options.bundleID = "com.example.verygoodcore.donutly.dev"
FirebaseApp.configure(options: options)
```

This is an iOS setup example, not compiled iOS integration. The Swift target still needs a native Firebase-to-Kotlin token provider bridge and feature UI wiring. Google sign-in also needs its SDK and reversed client-ID URL scheme; `iosClientId` alone does not enable it. iOS compilation requires Xcode/macOS and was not run on this Windows host.

References: [Firebase programmatic configuration](https://firebase.google.com/docs/projects/multiprojects), [Firebase Android Auth setup](https://firebase.google.com/docs/auth/android/start), [ID token verification](https://firebase.google.com/docs/auth/admin/verify-id-tokens).

## Live public API check

The supplied localhost backend returned success (`responseCode: "00"`) for `/v1/app/home`, `/v1/parking/categories`, and `/v1/parking/locations?category=mall&q=parking&page=0&size=20`. The search response matched the nested page DTO and decimal price shape. These checks were made from the host; emulator-to-host connectivity and a real Firebase sign-in still need runtime verification.

Android verification passed: `:androidApp:assembleDebug`, `:sharedLogic:testAndroidHostTest`, and `:sharedUI:testAndroidHostTest`. The full rebuild used `--rerun-tasks --max-workers=2 --no-parallel` after transient generated-file failures in the first packaging attempt.
