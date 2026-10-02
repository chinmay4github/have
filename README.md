# Roadwise Drive Assist (Android)

A native Android proof-of-concept for foreground driving assistance. The app combines a CameraX rear-camera preview, on-device text recognition, GPS speed monitoring, spoken cues, and local audio playback.

## What it does

- Requests a CameraX frame-rate range up to **120 FPS when the selected camera and preview/analysis/video session support it**. CameraX/Android may fall back to a lower rate; the app reports the requested range. It is not possible to guarantee 120 FPS on every Android phone.
- Samples camera images for OCR at about **4 frames per second** to limit heat and battery use. It does not run neural-network OCR 120 times per second.
- Uses the bundled, on-device ML Kit Latin text-recognition model. A conservative interpreter speaks speed-limit text when OCR includes speed/unit context, plus common text signs such as STOP, YIELD, SCHOOL, CROSSING, ROAD WORK, and road-direction words. Numeric-only text is not treated as a speed limit. Camera frames are processed on-device and are not uploaded by this app.
- Monitors foreground GPS speed and speaks when its smoothed estimate is **above 80 km/h**, then repeats at a limited interval while the estimate remains above the threshold.
- Reads short, road-related OCR lines aloud and shows the most recent cue.
- Provides an opt-in silent dashcam recording control using CameraX VideoCapture. Files are saved to `Movies/Roadwise` on Android 10+ (app-specific external storage on Android 9 and older). The live camera view overlays GPS speed; that overlay is not burned into the saved video.
- When event clips are armed, a linear accelerometer impulse of about 5 m/s² or a smoothed GPS-speed drop of at least 15 km/h within 2.5 seconds can start a 20-second clip. These are heuristics, not crash detection, and bumps/GPS noise can cause false triggers. Event clips begin after the trigger; the app has no pre-event circular buffer. Continuous recording is manual and stops when the app leaves the foreground.
- Provides destination handoff to Google Maps or another installed map app. This is not an embedded route renderer; leaving Roadwise stops camera/GPS monitoring.
- Plays a user-selected audio file through Android's document picker and Media3/ExoPlayer. Audio focus is enabled so spoken alerts can take priority.
- Stops GPS monitoring and dashcam recording when the app leaves the foreground. No background location or camera service is used.

## Important model and safety limits

This is **not a certified ADAS, collision-warning system, speedometer, or autonomous-driving feature**. The bundled ML model is a general text-recognition model, not a custom-trained traffic-sign detector. It cannot reliably identify sign shapes, colors, or every speed-limit sign; OCR may miss or misread signs, and GPS speed can be delayed or inaccurate. The 80 km/h threshold is a fixed prototype alert, not a statement about the legal limit on a road.

Mount the phone securely before moving, keep the camera view clear, and never interact with the phone while driving. Always watch the road and obey posted signs and local law. Do not rely on this prototype for safety-critical decisions. Validate on a parked vehicle and closed course before any further use.

## Build

Open this folder in Android Studio with JDK 17, Gradle 8.9, and Android SDK 35 installed, then sync and run the `app` configuration on a physical Android device. A rear camera, location services, and Android text-to-speech engine are needed for the corresponding features. The repository does not include a Gradle wrapper yet.

Unit tests cover conservative sign-text interpretation in `app/src/test`. Run them from Android Studio or with Gradle using `:app:testDebugUnitTest`.

## GitHub APK workflow (download an installable APK)

`.github/workflows/android-signed-apk.yml` (display name **Android APK**) runs on pushes to `master` and `arena/**` branches and can also be started with **Actions → Android APK → Run workflow**. It runs the unit tests, builds `:app:assembleRelease`, verifies the APK signature with `apksigner`, and uploads the artifact **`roadwise-apk`** (the APK plus `BUILD-INFO.txt` with commit, signing mode, and SHA-256). Push a tag such as `v1.0.0` to additionally attach the APK to a GitHub Release.

Download it: **Actions → Android APK → latest run → Artifacts → roadwise-apk**, and keep the `.apk` inside. Unzip on the phone or sideload with `adb install -r app-release.apk`. Android will ask you to allow installing from the source you use.

Signing has two modes:

- **Ephemeral (default, no setup):** with no secrets configured the workflow generates a temporary keystore, so every run still produces an installable APK. This is fine for testing but the signature changes per run, so Android will refuse in-place updates and may request an uninstall first.
- **Stable release (recommended for sharing):** add these repository Actions secrets (Settings → Secrets and variables → Actions) and the same workflow signs with your own key:

  - `ANDROID_KEYSTORE_BASE64` — base64 of a persistent release keystore
  - `ANDROID_KEYSTORE_PASSWORD`
  - `ANDROID_KEY_ALIAS`
  - `ANDROID_KEY_PASSWORD`

Generate the keystore once with `keytool` (for example `keytool -genkeypair -v -keystore roadwise-release.jks -alias roadwise -keyalg RSA -keysize 2048 -validity 10000`), encode it locally (`base64 -w0 roadwise-release.jks` on Linux), and store the file and passwords somewhere safe. Do not commit the keystore or paste signing secrets into chat. Keep using the same keystore for future updates; a newly generated key cannot update an APK signed with the old key. A `.jks` keystore works with `keytool`, but Gradle's PKCS12 loader only applies the supplied passwords directly to PKCS12 stores, so prefer exporting to `.p12`/PKCS12 (`keytool -importkeystore -srckeystore roadwise-release.jks -destkeystore roadwise-release.p12 -deststoretype PKCS12`) if signing fails with a wrong-password error.
