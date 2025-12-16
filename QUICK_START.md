# Quick Start Guide - Voice Assistant Base

Get up and running with the Voice Assistant in 5 minutes!

## Prerequisites

- Android device running Android 8.0 (Oreo, API 26) or higher
- USB cable for installation (or ability to transfer APK)
- Microphone access

## Installation Steps

### 1. Build the APK

```bash
# Clone the repository
git clone https://github.com/MohammedSheikhNabeeluddin/Voice-assistant-base-.git
cd Voice-assistant-base-

# Build debug APK
./gradlew assembleDebug

# APK will be at: app/build/outputs/apk/debug/app-debug.apk
```

### 2. Install on Your Device

**Option A - Via USB:**
```bash
# Connect device via USB
# Enable USB debugging on device
./gradlew installDebug
```

**Option B - Manual Install:**
1. Copy `app/build/outputs/apk/debug/app-debug.apk` to your device
2. Enable "Install from unknown sources" in Settings
3. Tap the APK file to install

### 3. Grant Permissions

When you first launch the app, it will request several permissions:

1. **Microphone** - Required for voice recognition ✓
2. **Overlay Permission** - Required to work over other apps ✓
3. **Phone** - Required for making calls ✓
4. **SMS** - Required for sending messages ✓
5. **Contacts** - Required for finding contact names ✓

Tap "Allow" for all permissions.

### 4. Enable Accessibility Service

This is crucial for in-app control:

1. Tap "Start Voice Assistant" button
2. You'll be prompted to enable accessibility
3. Go to: **Settings → Accessibility → Voice Assistant Base**
4. Toggle the switch to **ON**
5. Confirm the permission

### 5. Start the Service

1. Return to the Voice Assistant app
2. Tap "Start Voice Assistant"
3. You'll see a notification: "Listening for wake word..."

## Usage

### Basic Workflow

1. **Say the wake word**: "Hey Assistant"
2. **Wait for notification** to change to "Awake - Listening for command..."
3. **Give your command**: e.g., "open Chrome"
4. **Service executes** and returns to sleep automatically

### Example Commands

#### Open Apps
```
"Hey Assistant, open Chrome"
"Hey Assistant, open Gmail"
"Hey Assistant, open YouTube"
```

#### Navigation
```
"Hey Assistant, scroll down"
"Hey Assistant, go back"
"Hey Assistant, go home"
```

#### Text Operations
```
"Hey Assistant, copy"
"Hey Assistant, paste"
"Hey Assistant, type hello world"
```

#### Communication
```
"Hey Assistant, call John"
"Hey Assistant, send message to Mom saying I'll be late"
```

#### Time Management
```
"Hey Assistant, set alarm for 7:30"
"Hey Assistant, set timer for 5 minutes"
"Hey Assistant, start stopwatch"
```

#### System Control
```
"Hey Assistant, clear all apps"
"Hey Assistant, open settings"
```

## Troubleshooting

### "Commands not recognized"
- **Check microphone**: Ensure it's working and not blocked
- **Speak clearly**: Use a normal pace and volume
- **Check notification**: Ensure it says "Awake" after wake word

### "Actions not working"
- **Enable accessibility**: Go to Settings → Accessibility → Voice Assistant Base → ON
- **Grant overlay permission**: Settings → Apps → Voice Assistant Base → Display over other apps → Allow
- **Restart service**: Stop and start the voice assistant

### "Service stops unexpectedly"
- **Disable battery optimization**: Settings → Battery → Battery optimization → Voice Assistant Base → Don't optimize
- **Check permissions**: Ensure all permissions are granted
- **View notification**: Service shows status in notification

### "App won't open"
- **Check installation**: Ensure app is installed on device
- **Try package name**: Some apps need specific package names
- **Check Play Store**: Download app if not installed

## Tips for Best Results

1. **Clear Speech**: Speak clearly at normal pace
2. **Quiet Environment**: Reduce background noise
3. **Wait for Confirmation**: Check notification after wake word
4. **Battery Optimization**: Disable for uninterrupted service
5. **Common Names**: Use common app names (Chrome, Gmail, etc.)

## Voice Command Patterns

### App Control
- "open [app name]"
- "switch to [app name]"
- "close app"
- "download [app name]"

### Navigation
- "scroll up/down"
- "go back"
- "go home"

### Text
- "copy"
- "paste"
- "type [text]"

### Communication
- "call [contact]"
- "send message to [contact] saying [message]"

### Time
- "set alarm for [time]"
- "set timer for [duration] minutes"
- "start stopwatch"

### System
- "clear all apps"
- "clear background [app name]"
- "open settings"

## Next Steps

- Read [README.md](README.md) for detailed features and documentation
- Check [DEVELOPER_GUIDE.md](DEVELOPER_GUIDE.md) if you want to modify or extend
- Review [IMPLEMENTATION_SUMMARY.md](IMPLEMENTATION_SUMMARY.md) for technical details

## Support

- **GitHub Issues**: Report bugs or request features
- **Documentation**: Comprehensive guides available
- **Logs**: Use `adb logcat` to view detailed logs

## Safety Notes

⚠️ **Important**: This app requests powerful permissions including:
- Making phone calls
- Sending SMS messages
- Controlling other apps
- System-level access

Only use commands you intend to execute. The app processes all voice input locally on your device.

## Quick Reference Card

| Command Category | Example | Result |
|-----------------|---------|--------|
| Wake | "Hey Assistant" | Activates assistant |
| Open App | "open Chrome" | Launches Chrome |
| Navigate | "scroll down" | Scrolls down |
| Text | "type hello" | Types "hello" |
| Call | "call John" | Calls John |
| Message | "send message to Mom saying hi" | Sends SMS |
| Alarm | "set alarm for 7:30" | Sets alarm |
| Timer | "set timer for 5 minutes" | Sets timer |
| Clear | "clear all apps" | Closes background apps |

---

**Ready to Go!** 🎤

Say "Hey Assistant" and start controlling your device with your voice!
