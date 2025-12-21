# Voice Assistant Base

A comprehensive mobile voice assistant for Android that runs continuously in the background and activates with a wake word. It can perform a wide range of tasks using only voice commands.

## Features

### App Management
- **Open Apps**: Open any installed application by name
- **Switch Apps**: Switch between running applications
- **Close Apps**: Close current or specific apps
- **Download Apps**: Search and download apps from Play Store
- **Clear Background Apps**: Clear all or specific background apps to free memory

### In-App Actions
- **Scrolling**: Scroll up and down within apps and websites
- **Navigation**: Go back, go home, or open recent apps
- **Text Operations**: Copy, paste, and type text
- **Click Actions**: Perform clicks on UI elements

### Communication
- **Make Calls**: Make phone calls to contacts using voice
- **Send Messages**: Send SMS messages to contacts
- **Messaging Apps**: Works with popular messaging apps

### Time Management
- **Alarms**: Set alarms for specific times
- **Timers**: Set countdown timers
- **Stopwatch**: Start and control stopwatch
- **Countdowns**: Start custom countdowns

### Voice Activation
- **Wake Word**: Say "Hey Assistant" to activate
- **Sleep Mode**: Automatically returns to sleep after completing tasks
- **Background Operation**: Runs continuously in the background with minimal battery impact

## Setup Instructions

### Prerequisites
- Android device running Android 8.0 (API 26) or higher
- Microphone permission
- Accessibility service permission
- Overlay permission
- Other permissions as requested by the app

### Installation

1. **Clone the repository**:
   ```bash
   git clone https://github.com/MohammedSheikhNabeeluddin/Voice-assistant-base-.git
   cd Voice-assistant-base-
   ```

2. **Build the project**:
   ```bash
   ./gradlew build
   ```

3. **Install on device**:
   ```bash
   ./gradlew installDebug
   ```

### Configuration

1. **Launch the app** on your Android device

2. **Grant permissions**:
   - Microphone access (required for voice recognition)
   - Overlay permission (required for floating UI)
   - Phone and SMS permissions (for calls and messages)
   - Contact access (for finding contacts)

3. **Enable Accessibility Service**:
   - Go to Settings → Accessibility
   - Find "Voice Assistant Base"
   - Enable the service
   - This allows the assistant to perform actions within apps

4. **Start the Service**:
   - Tap "Start Voice Assistant" in the app
   - The service will run in the background
   - You'll see a notification indicating it's listening

## Usage

### Basic Commands

#### App Management
- "Hey Assistant, open Chrome"
- "Hey Assistant, switch to Gmail"
- "Hey Assistant, close app"
- "Hey Assistant, download WhatsApp"
- "Hey Assistant, clear all apps"
- "Hey Assistant, clear background Gmail"

#### In-App Actions
- "Hey Assistant, scroll down"
- "Hey Assistant, scroll up"
- "Hey Assistant, go back"
- "Hey Assistant, go home"
- "Hey Assistant, copy"
- "Hey Assistant, paste"
- "Hey Assistant, type Hello World"

#### Communication
- "Hey Assistant, call John"
- "Hey Assistant, send message to Mom saying I'll be home soon"

#### Time Management
- "Hey Assistant, set alarm for 7:30"
- "Hey Assistant, set timer for 5 minutes"
- "Hey Assistant, start stopwatch"
- "Hey Assistant, start countdown for 10 minutes"

#### System
- "Hey Assistant, open settings"
- "Hey Assistant, enable accessibility"

## Permissions Required

| Permission | Purpose |
|------------|---------|
| RECORD_AUDIO | Listen for wake word and voice commands |
| FOREGROUND_SERVICE | Run continuously in background |
| SYSTEM_ALERT_WINDOW | Display overlay UI |
| BIND_ACCESSIBILITY_SERVICE | Perform actions within apps |
| CALL_PHONE | Make phone calls |
| SEND_SMS | Send text messages |
| READ_CONTACTS | Access contact information |
| SCHEDULE_EXACT_ALARM | Set alarms and timers |
| KILL_BACKGROUND_PROCESSES | Clear background apps |
| INTERNET | Download apps from Play Store |

## Architecture

```
Voice Assistant Base
├── MainActivity - Main UI and permission handling
├── VoiceAssistantService - Background service for wake word detection
├── VoiceAccessibilityService - Accessibility service for app control
├── CommandProcessor - Processes and executes voice commands
└── Resources - UI layouts, strings, and configurations
```

## How It Works

1. **Wake Word Detection**: The service continuously listens for the wake word "Hey Assistant" using Android's SpeechRecognizer API
2. **Command Processing**: Once activated, it listens for a command and processes it through the CommandProcessor
3. **Action Execution**: Commands are executed using appropriate Android APIs:
   - Accessibility Service for in-app actions
   - Intent system for launching apps
   - AlarmManager for time-based tasks
   - ActivityManager for app management
4. **Sleep Mode**: After executing a command, the assistant returns to listening for the wake word

## Technical Details

- **Language**: Kotlin
- **Min SDK**: 26 (Android 8.0)
- **Target SDK**: 34 (Android 14)
- **Key Technologies**:
  - Android SpeechRecognizer API
  - Accessibility Service
  - Foreground Service
  - Intent System
  - Activity Manager

## Energy Efficiency

The assistant is designed to be energy-efficient:
- Uses wake word detection to minimize processing
- Foreground service ensures reliable operation
- Automatically sleeps when idle
- Optimized speech recognition intervals

## Privacy & Security

- All voice processing happens on-device
- No voice data is sent to external servers
- Permissions are only used for intended features
- User has full control over service activation

## Limitations

- Wake word detection accuracy depends on device microphone quality
- Some actions require specific Android permissions
- App-specific actions may vary based on app implementation
- Background app clearing has limitations on modern Android versions

## Troubleshooting

### Service not starting
- Ensure all permissions are granted
- Check that accessibility service is enabled
- Verify microphone is working

### Commands not recognized
- Speak clearly and at normal pace
- Ensure wake word is detected (notification will update)
- Check microphone permissions

### Actions not working
- Enable accessibility service in system settings
- Grant overlay permission
- Check app-specific permissions

## Contributing

Contributions are welcome! Please feel free to submit issues and pull requests.

## License

This project is open source and available for educational purposes.

## Support

For issues and questions, please open an issue on GitHub.

## Future Enhancements

- Multi-language support
- Custom wake word configuration
- Advanced gesture controls
- Integration with more apps
- Smart home device control
- Calendar and email management
- Web search and information retrieval
# Build Fri Dec 19 14:00:40 IST 2025
# Build Fri Dec 19 14:02:47 IST 2025
# Build Sat Dec 20 21:46:40 IST 2025
# Build Sun Dec 21 07:35:23 IST 2025
