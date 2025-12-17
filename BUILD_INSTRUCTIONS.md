# Build Instructions - Voice Assistant Base

This document provides instructions for building the Voice Assistant APK and creating a ZIP archive containing all source code and the build output.

## Prerequisites

1. **Java Development Kit (JDK) 17**
   ```bash
   # Ubuntu/Debian
   sudo apt-get update
   sudo apt-get install openjdk-17-jdk
   
   # Verify installation
   java -version
   ```

2. **Android SDK** (or Android Studio)
   - Download from: https://developer.android.com/studio
   - Set `ANDROID_HOME` environment variable

3. **Gradle** (bundled with project via gradlew)

## Building the APK

### Quick Build Commands

```bash
# Navigate to project directory
cd Voice-assistant-base-

# Make gradlew executable (Unix/Mac)
chmod +x gradlew

# Clean previous builds
./gradlew clean

# Build debug APK
./gradlew assembleDebug

# Build release APK
./gradlew assembleRelease

# Build both debug and release
./gradlew assemble
```

### APK Output Locations

After successful build, APKs are located at:

- **Debug APK**: `app/build/outputs/apk/debug/VoiceAssistant-1.0-debug.apk`
- **Release APK**: `app/build/outputs/apk/release/VoiceAssistant-1.0-release.apk`

### Install on Device

```bash
# Install debug APK
./gradlew installDebug

# Or use adb directly
adb install -r app/build/outputs/apk/debug/VoiceAssistant-1.0-debug.apk
```

## Creating the ZIP Archive

After building, create a ZIP file containing all source code and the APK:

### Unix/Mac/Linux

```bash
# Build the release APK first
./gradlew assembleRelease

# Create output directory
mkdir -p release_package

# Copy the APK to package directory
cp app/build/outputs/apk/release/VoiceAssistant-1.0-release.apk release_package/

# Create ZIP with source and APK
zip -r VoiceAssistant-Complete.zip \
    app/src \
    app/build.gradle \
    app/proguard-rules.pro \
    build.gradle \
    settings.gradle \
    gradle.properties \
    gradlew \
    gradlew.bat \
    gradle \
    README.md \
    DEVELOPER_GUIDE.md \
    BUILD_INSTRUCTIONS.md \
    release_package/VoiceAssistant-1.0-release.apk

# Clean up temporary directory
rm -rf release_package

echo "ZIP file created: VoiceAssistant-Complete.zip"
```

### Windows (PowerShell)

```powershell
# Build the release APK first
.\gradlew.bat assembleRelease

# Create output directory
New-Item -ItemType Directory -Force -Path release_package

# Copy the APK
Copy-Item "app\build\outputs\apk\release\VoiceAssistant-1.0-release.apk" -Destination "release_package\"

# Create ZIP
Compress-Archive -Path @(
    "app\src",
    "app\build.gradle",
    "app\proguard-rules.pro",
    "build.gradle",
    "settings.gradle",
    "gradle.properties",
    "gradlew",
    "gradlew.bat",
    "gradle",
    "README.md",
    "DEVELOPER_GUIDE.md",
    "BUILD_INSTRUCTIONS.md",
    "release_package\VoiceAssistant-1.0-release.apk"
) -DestinationPath "VoiceAssistant-Complete.zip" -Force

# Clean up
Remove-Item -Recurse -Force release_package

Write-Host "ZIP file created: VoiceAssistant-Complete.zip"
```

## One-Line Build + Package Command

### Unix/Mac/Linux
```bash
./gradlew clean assembleRelease && mkdir -p release_package && cp app/build/outputs/apk/release/*.apk release_package/ && zip -r VoiceAssistant-Complete.zip app/src app/build.gradle app/proguard-rules.pro build.gradle settings.gradle gradle.properties gradlew gradlew.bat gradle README.md DEVELOPER_GUIDE.md BUILD_INSTRUCTIONS.md release_package/*.apk && rm -rf release_package
```

### Windows (Command Prompt)
```cmd
gradlew.bat clean assembleRelease && mkdir release_package && copy app\build\outputs\apk\release\*.apk release_package\ && powershell Compress-Archive -Path app\src,app\build.gradle,app\proguard-rules.pro,build.gradle,settings.gradle,gradle.properties,gradlew,gradlew.bat,gradle,README.md,DEVELOPER_GUIDE.md,BUILD_INSTRUCTIONS.md,release_package\*.apk -DestinationPath VoiceAssistant-Complete.zip -Force && rmdir /s /q release_package
```

## Gradle Tasks Reference

| Task | Description |
|------|-------------|
| `./gradlew clean` | Clean build artifacts |
| `./gradlew assembleDebug` | Build debug APK |
| `./gradlew assembleRelease` | Build release APK |
| `./gradlew assemble` | Build all variants |
| `./gradlew installDebug` | Install debug APK on device |
| `./gradlew installRelease` | Install release APK on device |
| `./gradlew lint` | Run Android lint checks |
| `./gradlew tasks` | List all available tasks |

## Troubleshooting

### Common Issues

1. **JAVA_HOME not set**
   ```bash
   export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64
   ```

2. **ANDROID_HOME not set**
   ```bash
   export ANDROID_HOME=$HOME/Android/Sdk
   export PATH=$PATH:$ANDROID_HOME/tools:$ANDROID_HOME/platform-tools
   ```

3. **Permission denied on gradlew**
   ```bash
   chmod +x gradlew
   ```

4. **Gradle version mismatch**
   ```bash
   ./gradlew wrapper --gradle-version 8.2
   ```

5. **SDK license not accepted**
   ```bash
   yes | $ANDROID_HOME/cmdline-tools/latest/bin/sdkmanager --licenses
   ```

## ZIP Archive Contents

The final `VoiceAssistant-Complete.zip` contains:

```
VoiceAssistant-Complete.zip
├── app/
│   ├── src/
│   │   └── main/
│   │       ├── java/com/voiceassistant/base/
│   │       │   ├── MainActivity.kt
│   │       │   ├── command/CommandProcessor.kt
│   │       │   ├── service/
│   │       │   │   ├── VoiceAssistantService.kt
│   │       │   │   └── VoiceAccessibilityService.kt
│   │       │   └── util/
│   │       │       ├── AppHelper.kt
│   │       │       └── PermissionHelper.kt
│   │       ├── res/
│   │       │   ├── layout/activity_main.xml
│   │       │   ├── values/strings.xml
│   │       │   ├── drawable/ic_notification.xml
│   │       │   └── xml/accessibility_service_config.xml
│   │       └── AndroidManifest.xml
│   ├── build.gradle
│   └── proguard-rules.pro
├── build.gradle
├── settings.gradle
├── gradle.properties
├── gradlew
├── gradlew.bat
├── gradle/
├── README.md
├── DEVELOPER_GUIDE.md
├── BUILD_INSTRUCTIONS.md
└── release_package/
    └── VoiceAssistant-1.0-release.apk
```

## Release Signing (Production)

For production releases, create a keystore:

```bash
keytool -genkey -v -keystore my-release-key.jks -keyalg RSA -keysize 2048 -validity 10000 -alias my-alias
```

Then update `app/build.gradle` signingConfigs:

```groovy
signingConfigs {
    release {
        storeFile file("path/to/my-release-key.jks")
        storePassword "your-store-password"
        keyAlias "my-alias"
        keyPassword "your-key-password"
    }
}
```

**Note:** Never commit keystore passwords to version control. Use environment variables or a local properties file.
