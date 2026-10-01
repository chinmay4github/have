# Roadwise Drive Assist (Android)

A native Android proof-of-concept for foreground driving assistance. The app combines a CameraX rear-camera preview, on-device text recognition, GPS speed monitoring, spoken cues, and local audio playback.

## What it does

- Requests a CameraX frame-rate range up to **120 FPS when the selected camera and preview/analysis session support it**. CameraX/Android may fall back to a lower rate; the app reports the requested range. It is not possible to guarantee 120 FPS on every Android phone.
- Samples camera images for OCR at about **4 frames per second** to limit heat and battery use. It does not run neural-network OCR 120 times per second.
- Uses the bundled, on-device ML Kit Latin text-recognition model. A conservative interpreter speaks speed-limit text when OCR includes speed/unit context, plus common text signs such as STOP, YIELD, SCHOOL, CROSSING, ROAD WORK, and road-direction words. Numeric-only text is not treated as a speed limit. Camera frames are processed on-device and are not uploaded by this app.
- Monitors foreground GPS speed and speaks when its smoothed estimate is **above 80 km/h**, then repeats at a limited interval while the estimate remains above the threshold.
- Reads short, road-related OCR lines aloud and shows the most recent cue.
- Plays a user-selected audio file through Android's document picker and Media3/ExoPlayer. Audio focus is enabled so navigation speech can take priority.
- Stops GPS monitoring when the app leaves the foreground. No background location service is used.

## Important model and safety limits

This is **not a certified ADAS, collision-warning system, speedometer, or autonomous-driving feature**. The bundled ML model is a general text-recognition model, not a custom-trained traffic-sign detector. It cannot reliably identify sign shapes, colors, or every speed-limit sign; OCR may miss or misread signs, and GPS speed can be delayed or inaccurate. The 80 km/h threshold is a fixed prototype alert, not a statement about the legal limit on a road.

Mount the phone securely before moving, keep the camera view clear, and never interact with the phone while driving. Always watch the road and obey posted signs and local law. Do not rely on this prototype for safety-critical decisions. Validate on a parked vehicle and closed course before any further use.

## Build

Open this folder in Android Studio with JDK 17, Gradle 8.9, and Android SDK 35 installed, then sync and run the `app` configuration on a physical Android device. A rear camera, location services, and Android text-to-speech engine are needed for the corresponding features. The repository does not include a Gradle wrapper yet.

Unit tests cover conservative sign-text interpretation in `app/src/test`. Run them from Android Studio or with Gradle using `:app:testDebugUnitTest`.

## GitHub signed APK workflow

`.github/workflows/android-signed-apk.yml` runs on pushes to the Arena session branch and can also be started with **Actions → Android signed APK → Run workflow**. It runs the unit tests, builds `:app:assembleRelease`, verifies the APK signature, and uploads `roadwise-signed-release-apk` as a workflow artifact.

Before the workflow can produce a signed APK, add these repository Actions secrets (Settings → Secrets and variables → Actions):

- `ANDROID_KEYSTORE_BASE64` — base64 of a persistent release `.jks` keystore
- `ANDROID_KEYSTORE_PASSWORD`
- `ANDROID_KEY_ALIAS`
- `ANDROID_KEY_PASSWORD`

Generate the keystore once with `keytool`, encode it locally (for example, `base64 -w0 roadwise-release.jks` on Linux), and store the keystore backup safely. Do not commit the keystore or paste signing secrets into chat. Keep using the same keystore for future app updates; a newly generated key cannot update an APK signed with the old key. The workflow fails early with a clear error if the required secrets are missing.
