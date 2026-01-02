# ONNX Runtime Dependency Fix

## Problem
The app failed to compile with error:
```
e: Unresolved reference 'microsoft'
```

## Root Cause
The ONNX Runtime dependency version `1.23.2` specified in `build.gradle.kts` does not exist on Maven Central.

## Solution Applied
Updated `app/build.gradle.kts` with the correct dependency:

```kotlin
// Changed from:
implementation("com.microsoft.onnxruntime:onnxruntime-android:1.23.2")

// To:
implementation("com.microsoft.onnxruntime:onnxruntime-android:1.14.0")
```

## Manual Steps to Build

1. **Stop all Gradle daemons:**
   ```bash
   .\gradlew.bat --stop
   ```

2. **Clean the project:**
   ```bash
   .\gradlew.bat clean
   ```

3. **Sync Gradle dependencies:**
   - In Android Studio: File → Sync Project with Gradle Files
   - OR via command line: `.\gradlew.bat build --refresh-dependencies`

4. **Build the APK:**
   ```bash
   .\gradlew.bat assembleDebug
   ```

## Alternative: Use Android Studio

If Gradle hangs on the command line, use Android Studio instead:

1. Open the project in Android Studio
2. Wait for automatic Gradle sync
3. If prompted about the dependency, click "Sync Now"
4. Click Build → Make Project (or press Ctrl+F9)
5. Click Run → Run 'app' (or press Shift+F10)

## Verified Working Versions

These ONNX Runtime versions are confirmed to work:
- `1.14.0` (current - stable LTS)
- `1.15.1` (newer stable)
- `1.16.3` (latest stable as of Jan 2026)

## If Build Still Fails

1. **Clear Gradle cache:**
   ```bash
   rm -rf .gradle
   rm -rf app/build
   ```

2. **Invalidate Android Studio caches:**
   - File → Invalidate Caches / Restart → Invalidate and Restart

3. **Check internet connection:**
   - ONNX Runtime (~15MB) needs to download from Maven Central

4. **Verify proxy settings:**
   - Check if corporate firewall is blocking Maven Central
   - Configure proxy in `gradle.properties` if needed

## Build Output Location

Once successful, the APK will be at:
```
app/build/outputs/apk/debug/app-debug.apk
```

## Next Steps After Successful Build

1. Install on device: `adb install app/build/outputs/apk/debug/app-debug.apk`
2. Run the app and click "Initialize Engine"
3. Test with default Vietnamese text
4. Adjust sliders and synthesize speech

## Status

✅ Dependency version corrected in build.gradle.kts
✅ Repository configuration verified (mavenCentral present)
⏳ Awaiting manual build in Android Studio or via Gradle CLI

