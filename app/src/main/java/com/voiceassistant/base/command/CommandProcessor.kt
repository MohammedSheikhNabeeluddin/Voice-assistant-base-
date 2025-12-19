package com.voiceassistant.base.command

import android.accessibilityservice.AccessibilityService
import android.app.ActivityManager
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.SystemClock
import android.provider.AlarmClock
import android.provider.ContactsContract
import android.provider.Settings
import android.telecom.TelecomManager
import android.telephony.SmsManager
import android.util.Log
import com.voiceassistant.base.service.VoiceAccessibilityService
import com.voiceassistant.base.util.AppHelper
import java.util.Calendar

/**
 * Processes voice commands and executes corresponding actions
 * 
 * Command matching is done in order of specificity to ensure correct execution.
 * More specific commands (like "open settings") are checked before generic ones (like "open").
 */
class CommandProcessor(private val context: Context) {

    private val TAG = "CommandProcessor"
    private val ACCESSIBILITY_NOT_ENABLED_MSG = "Accessibility service not enabled. Please enable it in settings"
    
    /**
     * Checks if accessibility service is available and logs status
     */
    private fun isAccessibilityServiceAvailable(): Boolean {
        val available = VoiceAccessibilityService.instance != null
        if (!available) {
            Log.w(TAG, "Accessibility service is not available - user needs to enable it in settings")
        } else {
            Log.d(TAG, "Accessibility service is available")
        }
        return available
    }

    fun processCommand(command: String, callback: (Boolean, String) -> Unit) {
        Log.d(TAG, "============================================")
        Log.d(TAG, "Processing command: '$command'")
        Log.d(TAG, "============================================")
        
        if (command.isBlank()) {
            Log.w(TAG, "Received empty command, ignoring")
            callback(false, "No command received")
            return
        }
        
        val normalizedCommand = command.lowercase().trim()
        Log.d(TAG, "Normalized command: '$normalizedCommand'")
        
        try {
            when {
                // System settings (check before general "open" to avoid conflicts)
                normalizedCommand.contains("open settings") -> {
                    Log.d(TAG, "Matched: open settings")
                    val result = openSettings()
                    callback(result.first, result.second)
                }
                normalizedCommand.contains("enable accessibility") || normalizedCommand.contains("open accessibility") -> {
                    Log.d(TAG, "Matched: open accessibility settings")
                    val result = openAccessibilitySettings()
                    callback(result.first, result.second)
                }
                
                // App management - "open" command now works without requiring "app" keyword
                normalizedCommand.contains("open") -> {
                    val appName = extractAppName(normalizedCommand)
                    Log.d(TAG, "Matched: open app, extracted app name: '$appName'")
                    val result = openApp(appName)
                    callback(result.first, result.second)
                }
                normalizedCommand.contains("close app") || normalizedCommand.contains("close current") -> {
                    Log.d(TAG, "Matched: close current app")
                    val result = closeCurrentApp()
                    callback(result.first, result.second)
                }
                normalizedCommand.contains("switch to") || normalizedCommand.contains("switch") -> {
                    val appName = extractAppName(normalizedCommand)
                    Log.d(TAG, "Matched: switch to app, extracted app name: '$appName'")
                    val result = openApp(appName)
                    callback(result.first, result.second)
                }
                normalizedCommand.contains("download") || normalizedCommand.contains("install") -> {
                    val appName = extractAppName(normalizedCommand)
                    Log.d(TAG, "Matched: download/install app, extracted app name: '$appName'")
                    val result = openPlayStore(appName)
                    callback(result.first, result.second)
                }
                normalizedCommand.contains("clear all apps") || normalizedCommand.contains("close all apps") -> {
                    Log.d(TAG, "Matched: clear all background apps")
                    val result = clearAllBackgroundApps()
                    callback(result.first, result.second)
                }
                normalizedCommand.contains("clear background") -> {
                    val appName = extractAppName(normalizedCommand)
                    Log.d(TAG, "Matched: clear specific app from background, app: '$appName'")
                    val result = clearSpecificApp(appName)
                    callback(result.first, result.second)
                }

                // Scrolling and navigation - check more specific patterns first
                normalizedCommand.contains("scroll up") -> {
                    Log.d(TAG, "Matched: scroll up")
                    if (isAccessibilityServiceAvailable()) {
                        val success = VoiceAccessibilityService.instance?.performScrollUp()
                        if (success == true) {
                            Log.d(TAG, "Scroll up executed successfully")
                            callback(true, "Scrolling up")
                        } else {
                            Log.w(TAG, "Scroll up action failed")
                            callback(false, "Failed to scroll up")
                        }
                    } else {
                        callback(false, ACCESSIBILITY_NOT_ENABLED_MSG)
                    }
                }
                normalizedCommand.contains("scroll down") -> {
                    Log.d(TAG, "Matched: scroll down")
                    if (isAccessibilityServiceAvailable()) {
                        val success = VoiceAccessibilityService.instance?.performScrollDown()
                        if (success == true) {
                            Log.d(TAG, "Scroll down executed successfully")
                            callback(true, "Scrolling down")
                        } else {
                            Log.w(TAG, "Scroll down action failed")
                            callback(false, "Failed to scroll down")
                        }
                    } else {
                        callback(false, ACCESSIBILITY_NOT_ENABLED_MSG)
                    }
                }
                normalizedCommand.contains("go back") || (normalizedCommand.contains("back") && !normalizedCommand.contains("background")) -> {
                    Log.d(TAG, "Matched: go back")
                    if (isAccessibilityServiceAvailable()) {
                        val success = VoiceAccessibilityService.instance?.performBack()
                        if (success == true) {
                            Log.d(TAG, "Back navigation executed successfully")
                            callback(true, "Going back")
                        } else {
                            Log.w(TAG, "Back navigation failed")
                            callback(false, "Failed to go back")
                        }
                    } else {
                        callback(false, ACCESSIBILITY_NOT_ENABLED_MSG)
                    }
                }
                normalizedCommand.contains("go home") || normalizedCommand == "home" || normalizedCommand.contains("home screen") -> {
                    Log.d(TAG, "Matched: go home")
                    if (isAccessibilityServiceAvailable()) {
                        val success = VoiceAccessibilityService.instance?.performHome()
                        if (success == true) {
                            Log.d(TAG, "Home navigation executed successfully")
                            callback(true, "Going home")
                        } else {
                            Log.w(TAG, "Home navigation failed")
                            callback(false, "Failed to go home")
                        }
                    } else {
                        callback(false, ACCESSIBILITY_NOT_ENABLED_MSG)
                    }
                }

                // Text operations
                normalizedCommand.contains("copy") -> {
                    Log.d(TAG, "Matched: copy")
                    if (isAccessibilityServiceAvailable()) {
                        val success = VoiceAccessibilityService.instance?.performCopy()
                        if (success == true) {
                            Log.d(TAG, "Copy action executed successfully")
                            callback(true, "Copying text")
                        } else {
                            Log.w(TAG, "Copy action failed")
                            callback(false, "Failed to copy text")
                        }
                    } else {
                        callback(false, ACCESSIBILITY_NOT_ENABLED_MSG)
                    }
                }
                normalizedCommand.contains("paste") -> {
                    Log.d(TAG, "Matched: paste")
                    if (isAccessibilityServiceAvailable()) {
                        val success = VoiceAccessibilityService.instance?.performPaste()
                        if (success == true) {
                            Log.d(TAG, "Paste action executed successfully")
                            callback(true, "Pasting text")
                        } else {
                            Log.w(TAG, "Paste action failed")
                            callback(false, "Failed to paste text")
                        }
                    } else {
                        callback(false, ACCESSIBILITY_NOT_ENABLED_MSG)
                    }
                }
                normalizedCommand.contains("type") -> {
                    val text = extractText(normalizedCommand, "type")
                    Log.d(TAG, "Matched: type, text to type: '$text'")
                    if (isAccessibilityServiceAvailable()) {
                        val success = VoiceAccessibilityService.instance?.performType(text)
                        if (success == true) {
                            Log.d(TAG, "Type action executed successfully")
                            callback(true, "Typing: $text")
                        } else {
                            Log.w(TAG, "Type action failed")
                            callback(false, "Failed to type text")
                        }
                    } else {
                        callback(false, ACCESSIBILITY_NOT_ENABLED_MSG)
                    }
                }

                // Communication
                normalizedCommand.contains("call") -> {
                    val contact = extractContactName(normalizedCommand)
                    Log.d(TAG, "Matched: call, contact: '$contact'")
                    val result = makeCall(contact)
                    callback(result.first, result.second)
                }
                normalizedCommand.contains("send message") || normalizedCommand.contains("text") -> {
                    val parts = extractMessageParts(normalizedCommand)
                    Log.d(TAG, "Matched: send message, recipient: '${parts.first}', message: '${parts.second}'")
                    val result = sendMessage(parts.first, parts.second)
                    callback(result.first, result.second)
                }

                // Alarms and timers - check more specific patterns first
                normalizedCommand.contains("set alarm") -> {
                    val time = extractTime(normalizedCommand)
                    Log.d(TAG, "Matched: set alarm, time: ${time.first}:${String.format("%02d", time.second)}")
                    val result = setAlarm(time)
                    callback(result.first, result.second)
                }
                normalizedCommand.contains("set timer") -> {
                    val duration = extractDuration(normalizedCommand)
                    Log.d(TAG, "Matched: set timer, duration: $duration seconds")
                    val result = setTimer(duration)
                    callback(result.first, result.second)
                }
                normalizedCommand.contains("start stopwatch") -> {
                    Log.d(TAG, "Matched: start stopwatch")
                    val result = startStopwatch()
                    callback(result.first, result.second)
                }
                normalizedCommand.contains("start countdown") -> {
                    val duration = extractDuration(normalizedCommand)
                    Log.d(TAG, "Matched: start countdown, duration: $duration seconds")
                    val result = startCountdown(duration)
                    callback(result.first, result.second)
                }

                else -> {
                    Log.w(TAG, "No matching command pattern found for: '$normalizedCommand'")
                    callback(false, "I don't understand that command. Please try again")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error executing command: '$command'", e)
            callback(false, "Error executing command: ${e.message}")
        }
    }

    private fun extractAppName(command: String): String {
        val keywords = listOf("open", "app", "close", "current", "switch to", "switch", "download", "install", "clear", "background")
        var appName = command
        keywords.forEach { keyword ->
            // Use regex with word boundaries to avoid partial replacements
            appName = appName.replace("\\b$keyword\\b".toRegex(RegexOption.IGNORE_CASE), "").trim()
        }
        val result = appName.trim()
        Log.d(TAG, "extractAppName: input='$command', output='$result'")
        return result
    }

    private fun extractText(command: String, prefix: String): String {
        val result = command.substringAfter(prefix).trim()
        Log.d(TAG, "extractText: input='$command', prefix='$prefix', output='$result'")
        return result
    }

    private fun extractContactName(command: String): String {
        val result = command.replace("call", "").trim()
        Log.d(TAG, "extractContactName: input='$command', output='$result'")
        return result
    }

    private fun extractMessageParts(command: String): Pair<String, String> {
        // Extract recipient and message from command like "send message to John saying Hello"
        val toPattern = "to\\s+(.+?)\\s+saying\\s+(.+)".toRegex(RegexOption.IGNORE_CASE)
        val match = toPattern.find(command)
        
        if (match != null) {
            val recipient = match.groupValues[1].trim()
            val message = match.groupValues[2].trim()
            Log.d(TAG, "extractMessageParts (regex): recipient='$recipient', message='$message'")
            return Pair(recipient, message)
        }
        
        // Fallback: try simple "to" split
        val parts = command.split(" to ", ignoreCase = true)
        val recipient = parts.getOrNull(1)?.split(" saying ", ignoreCase = true)?.get(0)?.trim() ?: ""
        val message = parts.getOrNull(1)?.split(" saying ", ignoreCase = true)?.getOrNull(1)?.trim() ?: ""
        Log.d(TAG, "extractMessageParts (fallback): recipient='$recipient', message='$message'")
        return Pair(recipient, message)
    }

    private fun extractTime(command: String): Pair<Int, Int> {
        // Simple extraction - in production, use more sophisticated parsing
        val timePattern = "(\\d{1,2}):(\\d{2})".toRegex()
        val match = timePattern.find(command)
        val result = if (match != null) {
            Pair(match.groupValues[1].toInt(), match.groupValues[2].toInt())
        } else {
            // Try to extract natural language time like "7 am" or "10 pm"
            val naturalTimePattern = "(\\d{1,2})\\s*(am|pm)?".toRegex(RegexOption.IGNORE_CASE)
            val naturalMatch = naturalTimePattern.find(command)
            if (naturalMatch != null) {
                var hour = naturalMatch.groupValues[1].toInt()
                val period = naturalMatch.groupValues[2].lowercase()
                if (period == "pm" && hour < 12) hour += 12
                if (period == "am" && hour == 12) hour = 0
                Pair(hour, 0)
            } else {
                Log.w(TAG, "Could not parse time from command, using default 8:00")
                Pair(8, 0) // Default 8:00 AM
            }
        }
        Log.d(TAG, "extractTime: input='$command', output=${result.first}:${String.format("%02d", result.second)}")
        return result
    }

    private fun extractDuration(command: String): Int {
        // Extract duration in seconds
        // NOTE: When no time unit is specified (minute/hour), defaults to minutes
        // Example: "set timer 5" becomes 5 minutes, "set timer 5 minutes" becomes 5 minutes
        val numberPattern = "(\\d+)".toRegex()
        val match = numberPattern.find(command)
        val number = match?.value?.toInt() ?: 1
        val result = when {
            command.contains("minute") -> number * 60
            command.contains("hour") -> number * 3600
            else -> number * 60 // Default to minutes if no unit specified
        }
        Log.d(TAG, "extractDuration: input='$command', number=$number, result=$result seconds")
        return result
    }

    private fun openApp(appName: String): Pair<Boolean, String> {
        Log.d(TAG, "openApp: Attempting to open app: '$appName'")
        
        if (appName.isBlank()) {
            Log.w(TAG, "openApp: App name is empty")
            return Pair(false, "Please specify which app to open")
        }
        
        try {
            val packageName = getPackageNameForApp(appName)
            Log.d(TAG, "openApp: Resolved package name: '$packageName'")
            
            val packageManager = context.packageManager
            val intent = packageManager.getLaunchIntentForPackage(packageName)
            
            return if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
                Log.i(TAG, "openApp: Successfully started app: '$appName' (package: $packageName)")
                Pair(true, "Opening $appName")
            } else {
                Log.w(TAG, "openApp: No launch intent found for package: '$packageName'")
                Pair(false, "Could not find $appName. Please make sure it's installed")
            }
        } catch (e: Exception) {
            Log.e(TAG, "openApp: Error opening app: '$appName'", e)
            return Pair(false, "Error opening $appName: ${e.message}")
        }
    }

    private fun getPackageNameForApp(appName: String): String {
        // Try common app mappings first
        AppHelper.getCommonAppPackage(appName)?.let { 
            Log.d(TAG, "getPackageNameForApp: Found common mapping for '$appName' -> '$it'")
            return it 
        }
        
        // Try to find app by searching installed apps
        AppHelper.findAppPackage(context, appName)?.let { 
            Log.d(TAG, "getPackageNameForApp: Found installed app for '$appName' -> '$it'")
            return it 
        }
        
        // Return as-is if not found
        Log.d(TAG, "getPackageNameForApp: No mapping found, using app name as package: '$appName'")
        return appName
    }

    private fun closeCurrentApp(): Pair<Boolean, String> {
        Log.d(TAG, "closeCurrentApp: Attempting to close current app")
        
        // Check if accessibility service is available before using it
        if (!isAccessibilityServiceAvailable()) {
            return Pair(false, ACCESSIBILITY_NOT_ENABLED_MSG)
        }
        
        try {
            val backSuccess = VoiceAccessibilityService.instance?.performBack()
            val homeSuccess = VoiceAccessibilityService.instance?.performHome()
            
            if (backSuccess == true && homeSuccess == true) {
                Log.i(TAG, "closeCurrentApp: Successfully closed current app")
                return Pair(true, "Closing current app")
            } else {
                Log.w(TAG, "closeCurrentApp: Some actions may have failed (back=$backSuccess, home=$homeSuccess)")
                return Pair(true, "Closing current app")
            }
        } catch (e: Exception) {
            Log.e(TAG, "closeCurrentApp: Error closing app", e)
            return Pair(false, "Error closing app: ${e.message}")
        }
    }

    private fun openPlayStore(appName: String): Pair<Boolean, String> {
        Log.d(TAG, "openPlayStore: Opening Play Store for '$appName'")
        
        try {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse("market://search?q=$appName")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            Log.i(TAG, "openPlayStore: Successfully opened Play Store for '$appName'")
            return Pair(true, "Opening Play Store for $appName")
        } catch (e: Exception) {
            Log.w(TAG, "openPlayStore: Play Store not available, falling back to web browser", e)
            // Fallback to web browser
            try {
                val intent = Intent(Intent.ACTION_VIEW).apply {
                    data = Uri.parse("https://play.google.com/store/search?q=$appName")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
                Log.i(TAG, "openPlayStore: Successfully opened Play Store in browser for '$appName'")
                return Pair(true, "Opening Play Store for $appName")
            } catch (e2: Exception) {
                Log.e(TAG, "openPlayStore: Failed to open Play Store", e2)
                return Pair(false, "Error opening Play Store: ${e2.message}")
            }
        }
    }

    private fun clearAllBackgroundApps(): Pair<Boolean, String> {
        Log.d(TAG, "clearAllBackgroundApps: Clearing all background apps")
        
        try {
            val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            var clearedCount = 0
            
            activityManager.runningAppProcesses?.forEach { processInfo ->
                if (processInfo.processName != context.packageName) {
                    activityManager.killBackgroundProcesses(processInfo.processName)
                    clearedCount++
                    Log.d(TAG, "clearAllBackgroundApps: Killed process '${processInfo.processName}'")
                }
            }
            
            Log.i(TAG, "clearAllBackgroundApps: Cleared $clearedCount background processes")
            return Pair(true, "Cleared $clearedCount background apps")
        } catch (e: Exception) {
            Log.e(TAG, "clearAllBackgroundApps: Error clearing background apps", e)
            return Pair(false, "Error clearing background apps: ${e.message}")
        }
    }

    private fun clearSpecificApp(appName: String): Pair<Boolean, String> {
        Log.d(TAG, "clearSpecificApp: Clearing '$appName' from background")
        
        try {
            val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            val packageName = getPackageNameForApp(appName)
            activityManager.killBackgroundProcesses(packageName)
            Log.i(TAG, "clearSpecificApp: Killed background process for '$packageName'")
            return Pair(true, "Clearing $appName from background")
        } catch (e: Exception) {
            Log.e(TAG, "clearSpecificApp: Error clearing '$appName'", e)
            return Pair(false, "Error clearing $appName: ${e.message}")
        }
    }

    private fun makeCall(contact: String): Pair<Boolean, String> {
        Log.d(TAG, "makeCall: Attempting to call '$contact'")
        
        if (contact.isBlank()) {
            Log.w(TAG, "makeCall: Contact is empty")
            return Pair(false, "Please specify who to call")
        }
        
        try {
            val intent = Intent(Intent.ACTION_CALL).apply {
                data = Uri.parse("tel:$contact")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            Log.i(TAG, "makeCall: Successfully initiated call to '$contact'")
            return Pair(true, "Calling $contact")
        } catch (e: SecurityException) {
            Log.e(TAG, "makeCall: Phone permission not granted", e)
            return Pair(false, "Phone permission not granted. Please grant call permission")
        } catch (e: Exception) {
            Log.e(TAG, "makeCall: Error making call to '$contact'", e)
            return Pair(false, "Error making call: ${e.message}")
        }
    }

    private fun sendMessage(recipient: String, message: String): Pair<Boolean, String> {
        Log.d(TAG, "sendMessage: Attempting to send message to '$recipient': '$message'")
        
        if (recipient.isEmpty()) {
            Log.w(TAG, "sendMessage: Recipient is empty")
            return Pair(false, "Please specify a recipient")
        }
        
        if (message.isEmpty()) {
            Log.w(TAG, "sendMessage: Message is empty")
            return Pair(false, "Please specify a message")
        }
        
        try {
            val smsManager = SmsManager.getDefault()
            smsManager.sendTextMessage(recipient, null, message, null, null)
            Log.i(TAG, "sendMessage: Successfully sent message to '$recipient'")
            return Pair(true, "Sending message to $recipient")
        } catch (e: SecurityException) {
            Log.e(TAG, "sendMessage: SMS permission not granted", e)
            return Pair(false, "SMS permission not granted. Please grant SMS permission")
        } catch (e: Exception) {
            Log.e(TAG, "sendMessage: Error sending message", e)
            return Pair(false, "Error sending message: ${e.message}")
        }
    }

    private fun setAlarm(time: Pair<Int, Int>): Pair<Boolean, String> {
        Log.d(TAG, "setAlarm: Setting alarm for ${time.first}:${String.format("%02d", time.second)}")
        
        try {
            val intent = Intent(AlarmClock.ACTION_SET_ALARM).apply {
                putExtra(AlarmClock.EXTRA_HOUR, time.first)
                putExtra(AlarmClock.EXTRA_MINUTES, time.second)
                putExtra(AlarmClock.EXTRA_SKIP_UI, false)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            Log.i(TAG, "setAlarm: Successfully opened alarm app to set alarm")
            return Pair(true, "Setting alarm for ${time.first}:${String.format("%02d", time.second)}")
        } catch (e: Exception) {
            Log.e(TAG, "setAlarm: Error setting alarm", e)
            return Pair(false, "Error setting alarm: ${e.message}")
        }
    }

    private fun setTimer(durationSeconds: Int): Pair<Boolean, String> {
        val minutes = durationSeconds / 60
        Log.d(TAG, "setTimer: Setting timer for $durationSeconds seconds ($minutes minutes)")
        
        try {
            val intent = Intent(AlarmClock.ACTION_SET_TIMER).apply {
                putExtra(AlarmClock.EXTRA_LENGTH, durationSeconds)
                putExtra(AlarmClock.EXTRA_SKIP_UI, false)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            Log.i(TAG, "setTimer: Successfully opened timer app")
            return Pair(true, "Setting timer for $minutes minute${if (minutes != 1) "s" else ""}")
        } catch (e: Exception) {
            Log.e(TAG, "setTimer: Error setting timer", e)
            return Pair(false, "Error setting timer: ${e.message}")
        }
    }

    private fun startStopwatch(): Pair<Boolean, String> {
        Log.d(TAG, "startStopwatch: Starting stopwatch")
        
        try {
            val intent = Intent(AlarmClock.ACTION_SHOW_TIMERS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            Log.i(TAG, "startStopwatch: Successfully opened stopwatch/timer app")
            return Pair(true, "Starting stopwatch")
        } catch (e: Exception) {
            Log.e(TAG, "startStopwatch: Error starting stopwatch", e)
            return Pair(false, "Error starting stopwatch: ${e.message}")
        }
    }

    private fun startCountdown(durationSeconds: Int): Pair<Boolean, String> {
        Log.d(TAG, "startCountdown: Starting countdown for $durationSeconds seconds")
        return setTimer(durationSeconds)
    }

    private fun openSettings(): Pair<Boolean, String> {
        Log.d(TAG, "openSettings: Opening system settings")
        
        try {
            val intent = Intent(Settings.ACTION_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            Log.i(TAG, "openSettings: Successfully opened system settings")
            return Pair(true, "Opening settings")
        } catch (e: Exception) {
            Log.e(TAG, "openSettings: Error opening settings", e)
            return Pair(false, "Error opening settings: ${e.message}")
        }
    }

    private fun openAccessibilitySettings(): Pair<Boolean, String> {
        Log.d(TAG, "openAccessibilitySettings: Opening accessibility settings")
        
        try {
            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            Log.i(TAG, "openAccessibilitySettings: Successfully opened accessibility settings")
            return Pair(true, "Opening accessibility settings")
        } catch (e: Exception) {
            Log.e(TAG, "openAccessibilitySettings: Error opening accessibility settings", e)
            return Pair(false, "Error opening accessibility settings: ${e.message}")
        }
    }
}
