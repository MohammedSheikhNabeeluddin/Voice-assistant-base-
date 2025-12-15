# Developer Guide - Voice Assistant Base

This guide provides detailed technical information for developers working on the Voice Assistant Base project.

## Project Structure

```
Voice-assistant-base-/
├── app/
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/voiceassistant/base/
│   │   │   │   ├── MainActivity.kt              # Main activity, UI, permissions
│   │   │   │   ├── command/
│   │   │   │   │   └── CommandProcessor.kt      # Voice command processing logic
│   │   │   │   ├── service/
│   │   │   │   │   ├── VoiceAssistantService.kt # Background service for wake word
│   │   │   │   │   └── VoiceAccessibilityService.kt # Accessibility for app control
│   │   │   │   └── util/
│   │   │   │       ├── AppHelper.kt             # App management utilities
│   │   │   │       └── PermissionHelper.kt      # Permission management utilities
│   │   │   ├── res/
│   │   │   │   ├── layout/
│   │   │   │   │   └── activity_main.xml        # Main activity layout
│   │   │   │   ├── values/
│   │   │   │   │   └── strings.xml              # String resources
│   │   │   │   ├── xml/
│   │   │   │   │   └── accessibility_service_config.xml
│   │   │   │   └── drawable/
│   │   │   │       └── ic_notification.xml      # Notification icon
│   │   │   └── AndroidManifest.xml              # App manifest with permissions
│   │   └── build.gradle                         # App-level build configuration
│   └── proguard-rules.pro                       # ProGuard rules
├── build.gradle                                  # Project-level build config
├── settings.gradle                               # Gradle settings
├── gradle.properties                             # Gradle properties
├── .gitignore                                    # Git ignore rules
├── README.md                                     # User documentation
└── DEVELOPER_GUIDE.md                           # This file

```

## Architecture

### Component Overview

#### 1. MainActivity
**Purpose**: Entry point, UI, and permission management

**Key Responsibilities**:
- Request and verify permissions (microphone, overlay, phone, SMS, etc.)
- Start/stop the voice assistant service
- Display service status
- Handle permission request results

**Key Methods**:
- `checkAndRequestPermissions()`: Verifies all required permissions
- `startVoiceAssistant()`: Starts the background service
- `stopVoiceAssistant()`: Stops the background service
- `updateStatus()`: Updates UI based on service state

#### 2. VoiceAssistantService
**Purpose**: Background service for continuous wake word listening

**Key Responsibilities**:
- Run as foreground service with notification
- Initialize and manage SpeechRecognizer
- Detect wake word ("Hey Assistant")
- Pass commands to CommandProcessor
- Manage sleep/wake states

**Key Methods**:
- `initializeSpeechRecognizer()`: Sets up speech recognition
- `startListening()`: Begins listening for voice input
- `processVoiceInput()`: Handles recognized speech
- `createNotification()`: Creates foreground service notification

**State Machine**:
```
[Sleep Mode] -> (Wake Word Detected) -> [Awake Mode] -> (Command Executed) -> [Sleep Mode]
```

#### 3. VoiceAccessibilityService
**Purpose**: Accessibility service for performing actions within apps

**Key Responsibilities**:
- Enable gesture automation (scroll, swipe, click)
- Perform text operations (copy, paste, type)
- Navigate UI elements
- Interact with accessibility nodes

**Key Methods**:
- `performScrollUp/Down()`: Scroll gestures
- `performBack/Home()`: Navigation actions
- `performCopy/Paste()`: Clipboard operations
- `performType()`: Text input
- `performClick()`: Touch gestures
- `performSwipe()`: Swipe gestures

**Important**: Must be manually enabled by user in Accessibility Settings.

#### 4. CommandProcessor
**Purpose**: Parse and execute voice commands

**Key Responsibilities**:
- Pattern matching on voice input
- Extract command parameters (app names, contact names, times, etc.)
- Execute corresponding actions via Android APIs
- Use VoiceAccessibilityService for in-app actions

**Command Categories**:
1. **App Management**: open, close, switch, download, clear
2. **Navigation**: scroll, back, home
3. **Text Operations**: copy, paste, type
4. **Communication**: call, message
5. **Time Management**: alarm, timer, stopwatch, countdown
6. **System**: settings, accessibility

**Key Methods**:
- `processCommand()`: Main command routing logic
- `extractAppName()`: Parse app name from command
- `openApp()`: Launch application
- `makeCall()`: Initiate phone call
- `setAlarm()`: Set alarm using AlarmClock intent

#### 5. AppHelper
**Purpose**: Utility for app-related operations

**Key Features**:
- Map app names to package names
- Search for apps by name
- Check if app is installed
- Get list of installed apps
- Common app package mappings

#### 6. PermissionHelper
**Purpose**: Centralized permission management

**Key Features**:
- Check if all permissions are granted
- Get list of required permissions
- Get list of missing permissions

## Key Android APIs Used

### 1. SpeechRecognizer
```kotlin
val recognizer = SpeechRecognizer.createSpeechRecognizer(context)
recognizer.setRecognitionListener(listener)
val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
recognizer.startListening(intent)
```

**Used For**: Wake word detection and command recognition

### 2. AccessibilityService
```kotlin
class VoiceAccessibilityService : AccessibilityService() {
    override fun onAccessibilityEvent(event: AccessibilityEvent?) { }
    override fun onInterrupt() { }
}
```

**Used For**: Performing actions within other apps

### 3. Foreground Service
```kotlin
startForeground(NOTIFICATION_ID, notification)
```

**Used For**: Keeping service running in background

### 4. Activity Manager
```kotlin
val am = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
am.killBackgroundProcesses(packageName)
```

**Used For**: Clearing background apps

### 5. Intent System
```kotlin
val intent = packageManager.getLaunchIntentForPackage(packageName)
startActivity(intent)
```

**Used For**: Launching apps, making calls, sending messages

## Building the Project

### Requirements
- Android Studio Arctic Fox or newer
- JDK 17
- Android SDK 34
- Gradle 8.2+

### Build Commands

```bash
# Clean build
./gradlew clean

# Debug build
./gradlew assembleDebug

# Release build
./gradlew assembleRelease

# Install on connected device
./gradlew installDebug

# Run tests
./gradlew test

# Check for updates
./gradlew dependencyUpdates
```

## Testing

### Manual Testing Checklist

- [ ] Service starts successfully
- [ ] Wake word is detected
- [ ] Commands are recognized
- [ ] Apps open correctly
- [ ] Scrolling works in various apps
- [ ] Copy/paste operations work
- [ ] Calls can be initiated
- [ ] Messages can be sent
- [ ] Alarms/timers are set correctly
- [ ] Background apps can be cleared
- [ ] Service returns to sleep after command

### Testing Commands

```
"Hey Assistant, open Chrome"
"Hey Assistant, scroll down"
"Hey Assistant, go back"
"Hey Assistant, set timer for 1 minute"
"Hey Assistant, clear all apps"
```

## Adding New Commands

### Step 1: Update CommandProcessor

Add a new condition in `processCommand()`:

```kotlin
when {
    // Existing commands...
    
    command.contains("your new trigger") -> {
        // Extract parameters
        val param = extractParameter(command)
        
        // Execute action
        yourNewAction(param)
        
        callback(true)
    }
}
```

### Step 2: Implement Action Method

```kotlin
private fun yourNewAction(param: String) {
    try {
        // Implementation
    } catch (e: Exception) {
        e.printStackTrace()
    }
}
```

### Step 3: Add Parameter Extraction (if needed)

```kotlin
private fun extractParameter(command: String): String {
    // Parse command and extract relevant data
    return command.replace("your new trigger", "").trim()
}
```

## Permissions

### Required Permissions

| Permission | API Level | Rationale |
|------------|-----------|-----------|
| RECORD_AUDIO | All | Voice input |
| FOREGROUND_SERVICE | 26+ | Background operation |
| FOREGROUND_SERVICE_MICROPHONE | 34+ | Foreground service type |
| SYSTEM_ALERT_WINDOW | All | Overlay UI |
| BIND_ACCESSIBILITY_SERVICE | All | App control |
| CALL_PHONE | All | Make calls |
| SEND_SMS | All | Send messages |
| READ_CONTACTS | All | Find contacts |
| SCHEDULE_EXACT_ALARM | 31+ | Set alarms |
| KILL_BACKGROUND_PROCESSES | All | Clear apps |

### Requesting Permissions

```kotlin
ActivityCompat.requestPermissions(
    activity,
    arrayOf(Manifest.permission.RECORD_AUDIO),
    REQUEST_CODE
)
```

## Debugging

### Enable Logging

Add logging to track command processing:

```kotlin
import android.util.Log

private val TAG = "VoiceAssistant"

Log.d(TAG, "Command received: $command")
Log.e(TAG, "Error processing command", exception)
```

### View Logs

```bash
# View all logs
adb logcat

# Filter by tag
adb logcat -s VoiceAssistant

# Clear logs
adb logcat -c
```

### Common Issues

1. **Service not starting**: Check permissions and manifest
2. **Commands not recognized**: Test microphone, check speech recognizer initialization
3. **Actions not executing**: Verify accessibility service is enabled
4. **App crashes**: Check logcat for stack traces

## Performance Optimization

### Battery Optimization
- Use efficient wake word detection
- Minimize wake locks
- Optimize speech recognition intervals
- Use appropriate service types

### Memory Management
- Recycle AccessibilityNodeInfo objects
- Properly destroy services
- Avoid memory leaks in listeners

## Security Considerations

1. **Permission Minimization**: Only request necessary permissions
2. **On-Device Processing**: All speech recognition happens locally
3. **Secure Storage**: Don't store sensitive voice data
4. **Permission Checks**: Always verify permissions before operations

## Future Enhancements

### Planned Features
- [ ] Custom wake word configuration
- [ ] Multi-language support
- [ ] Voice feedback/TTS responses
- [ ] Learning user preferences
- [ ] Integration with more apps
- [ ] Web search capabilities
- [ ] Smart home integration
- [ ] Calendar management
- [ ] Email reading/sending

### Technical Improvements
- [ ] Add unit tests
- [ ] Add UI tests
- [ ] Implement continuous integration
- [ ] Add analytics
- [ ] Improve command parsing with NLP
- [ ] Add offline voice recognition
- [ ] Optimize battery usage

## Contributing

### Code Style
- Follow Kotlin coding conventions
- Use meaningful variable names
- Add comments for complex logic
- Keep methods focused and small

### Pull Request Process
1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Test thoroughly
5. Submit pull request with description

### Commit Messages
Use clear, descriptive commit messages:
```
Add: New feature description
Fix: Bug description
Update: Change description
Refactor: Refactoring description
```

## Resources

### Android Documentation
- [SpeechRecognizer](https://developer.android.com/reference/android/speech/SpeechRecognizer)
- [AccessibilityService](https://developer.android.com/reference/android/accessibilityservice/AccessibilityService)
- [Foreground Services](https://developer.android.com/guide/components/foreground-services)
- [Permissions](https://developer.android.com/guide/topics/permissions/overview)

### Useful Tools
- [Android Studio](https://developer.android.com/studio)
- [ADB](https://developer.android.com/studio/command-line/adb)
- [Gradle](https://gradle.org/)

## License

This project is open source and available for educational purposes.

## Support

For technical questions and issues:
- Open an issue on GitHub
- Check existing documentation
- Review Android documentation
