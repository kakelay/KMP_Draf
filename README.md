This is a Kotlin Multiplatform project targeting Android, iOS.

* [/iosApp](./iosApp/iosApp) contains an iOS application. Even if you’re sharing your UI with Compose Multiplatform,
  you need this entry point for your iOS app. This is also where you should add SwiftUI code for your project.

* [/sharedLogic](./sharedLogic/src) is for the code that will be shared between app targets in the project.
  The most important subfolder is [commonMain](./sharedLogic/src/commonMain/kotlin). If preferred, you
  can add code to the platform-specific folders here too.

* [/sharedUI](./sharedUI/src) is for code that will be shared across your Compose Multiplatform applications.
  It contains several subfolders:
  - [commonMain](./sharedUI/src/commonMain/kotlin) is for code that’s common for all targets.
  - Other folders are for Kotlin code that will be compiled for only the platform indicated in the folder name.
    For example, if you want to use Apple’s CoreCrypto for the iOS part of your Kotlin app,
    the [iosMain](./sharedUI/src/iosMain/kotlin) folder would be the right place for such calls.
    Similarly, if you want to edit the Desktop (JVM) specific part, the [jvmMain](./sharedUI/src/jvmMain/kotlin)
    folder is the appropriate location.

### Running the apps

Use the run configurations provided by the run widget in your IDE's toolbar. You can also use these commands and options:

- Android app: `./gradlew :androidApp:assembleDebug`
- iOS app: open the [/iosApp](./iosApp) directory in Xcode and run it from there.

#### Android Studio Run button

1. Open the project root in Android Studio and wait for Gradle sync to finish.
2. Select the `androidApp` run configuration and an emulator or connected Android phone.
3. Start the backend and configure its URL as described below.
4. Click **Run** (the green triangle), or press **Shift + F10**.

If the run configuration is missing, use **Run > Edit Configurations > + > Android App** and select the `androidApp` module. The `assembleDebug` command above only builds the APK; it does not launch the app.

#### Local backend connection

Set `JOMNOT_API_BASE_URL` in the ignored `local.properties` file to the backend origin, without `/v1`:

| Android device | URL for a backend on computer port 8080 | Additional setup |
| --- | --- | --- |
| Standard Android emulator | `http://10.0.2.2:8080` | None |
| Physical phone connected over USB | `http://127.0.0.1:8080` | Run `adb reverse tcp:8080 tcp:8080` |

For a USB phone, run this in the IDE terminal before launching the app:

```powershell
adb reverse tcp:8080 tcp:8080
```

Repeat the command after reconnecting or restarting the phone. Keep the backend running on port 8080. Rebuild and run the app after changing the URL. A Gradle `-PJOMNOT_API_BASE_URL=...` argument takes precedence over `local.properties`; if neither is set, debug builds use `http://10.0.2.2:8080`.

If the screen shows **Could not load parking. Check your connection and retry.**, verify the backend is running and the device connection matches the table, then tap **Retry**. Running iOS requires macOS and Xcode.

### Backend API calls

The shared [JomnotApi client](sharedLogic/src/commonMain/kotlin/org/example/project/parking/data/remote/JomnotApi.kt) appends `/v1` to `JOMNOT_API_BASE_URL` and supports these endpoints:

| Method | Endpoint | Firebase sign-in required |
| --- | --- | --- |
| GET | `/v1/app/home` | No |
| GET | `/v1/parking/categories` | No |
| GET | `/v1/parking/locations` | No |
| GET | `/v1/parking/locations/{id}` | No |
| GET | `/v1/customer/account` | Yes |
| GET | `/v1/customer/favorites` | Yes |
| GET | `/v1/customer/favorites/customer/{customerId}` | Yes; server enforces access permissions |
| PUT / DELETE | `/v1/customer/favorites/{id}` | Yes |
| GET | `/v1/zone/customer/find-zones` | Yes |
| PUT | `/v1/zone/customer/selected-zone/{id}` | Yes |
| GET | `/v1/notifications` | Yes |
| PUT | `/v1/notifications/{id}/read` | Yes |

Parking search accepts `page`, `size`, and optional `category`, `q`, `latitude`, and `longitude`. Favorites lists accept `page` and `size`; notifications use `skip` and `take`.

For example, from the computer hosting the backend:

```powershell
Invoke-RestMethod 'http://127.0.0.1:8080/v1/parking/locations?page=0&size=20'
```

When signed in, the client sends the current Firebase ID token as `Authorization: Bearer <ID_TOKEN>`, including on public requests for personalized results. Protected requests require sign-in. A successful response must have an HTTP 2xx status and `responseCode: "00"`.

These are the client's supported endpoints; individual screens may use only a subset. Firebase API keys configure the Firebase SDK and are separate from the parking backend URL and user ID tokens. Keep real keys and tokens out of README examples. See [Firebase setup](docs/FIREBASE_SETUP.md) for configuration details.

### Parking search appearance

The search field in [ParkingSearchForm.kt](sharedUI/src/commonMain/kotlin/org/example/project/parking/presentation/widgets/ParkingSearchForm.kt) keeps the placeholder **Search parking by name or place** and no longer displays the floating label **Where are you heading?**.

When the search field has focus, its border and cursor use `AppColor`. Its focused label color is also configured as `AppColor` if a label is added later. Selected category chips use `AppColor` with white text.

### Running tests

Use the run button in your IDE's editor gutter, or run tests using Gradle tasks:

- Android tests: `./gradlew :sharedUI:testAndroidHostTest :sharedLogic:testAndroidHostTest`
- iOS tests: `./gradlew :sharedLogic:iosSimulatorArm64Test`

---

Learn more about [Kotlin Multiplatform](https://www.jetbrains.com/help/kotlin-multiplatform-dev/get-started.html)…
### MVVM real API sample

See [the integration and code review guide](docs/MVVM_API_SAMPLE.md) for the parking MVVM feature, all Jomnot client endpoints, Firebase token adapter, and configuration.
