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
import com.voiceassistant.base.service.VoiceAccessibilityService
import com.voiceassistant.base.util.AppHelper
import java.util.Calendar

/**
 * Processes voice commands and executes corresponding actions
 */
class CommandProcessor(private val context: Context) {

    private val TAG = "CommandProcessor"
    private val ACCESSIBILITY_NOT_ENABLED_MSG = "Accessibility service not enabled. Please enable it in settings"

    fun processCommand(command: String, callback: (Boolean, String) -> Unit) {
        android.util.Log.d(TAG, "Processing command: $command")
        try {
            when {
                // System settings (check before general "open" to avoid conflicts)
                command.contains("open settings") -> {
                    openSettings()
                    callback(true, "Opening settings")
                }
                command.contains("enable accessibility") || command.contains("open accessibility") -> {
                    openAccessibilitySettings()
                    callback(true, "Opening accessibility settings")
                }
                
                // App management - "open" command now works without requiring "app" keyword
                command.contains("open") -> {
                    val appName = extractAppName(command)
                    val response = openApp(appName)
                    callback(response != null, response ?: "Failed to open app")
                }
                command.contains("close app") || command.contains("close current") -> {
                    closeCurrentApp()
                    callback(true, "Closing current app")
                }
                command.contains("switch to") || command.contains("switch") -> {
                    val appName = extractAppName(command)
                    val response = openApp(appName)
                    callback(response != null, response ?: "Failed to switch to app")
                }
                command.contains("download") || command.contains("install") -> {
                    val appName = extractAppName(command)
                    openPlayStore(appName)
                    callback(true, "Opening Play Store for $appName")
                }
                command.contains("clear all apps") || command.contains("close all apps") -> {
                    clearAllBackgroundApps()
                    callback(true, "Clearing all background apps")
                }
                command.contains("clear background") -> {
                    val appName = extractAppName(command)
                    clearSpecificApp(appName)
                    callback(true, "Clearing $appName from background")
                }

                // Scrolling and navigation
                command.contains("scroll up") -> {
                    if (VoiceAccessibilityService.instance != null) {
                        VoiceAccessibilityService.instance?.performScrollUp()
                        callback(true, "Scrolling up")
                    } else {
                        callback(false, ACCESSIBILITY_NOT_ENABLED_MSG)
                    }
                }
                command.contains("scroll down") -> {
                    if (VoiceAccessibilityService.instance != null) {
                        VoiceAccessibilityService.instance?.performScrollDown()
                        callback(true, "Scrolling down")
                    } else {
                        callback(false, ACCESSIBILITY_NOT_ENABLED_MSG)
                    }
                }
                command.contains("go back") || command.contains("back") -> {
                    if (VoiceAccessibilityService.instance != null) {
                        VoiceAccessibilityService.instance?.performBack()
                        callback(true, "Going back")
                    } else {
                        callback(false, ACCESSIBILITY_NOT_ENABLED_MSG)
                    }
                }
                command.contains("go home") || command.contains("home") -> {
                    if (VoiceAccessibilityService.instance != null) {
                        VoiceAccessibilityService.instance?.performHome()
                        callback(true, "Going home")
                    } else {
                        callback(false, ACCESSIBILITY_NOT_ENABLED_MSG)
                    }
                }

                // Text operations
                command.contains("copy") -> {
                    if (VoiceAccessibilityService.instance != null) {
                        VoiceAccessibilityService.instance?.performCopy()
                        callback(true, "Copying text")
                    } else {
                        callback(false, ACCESSIBILITY_NOT_ENABLED_MSG)
                    }
                }
                command.contains("paste") -> {
                    if (VoiceAccessibilityService.instance != null) {
                        VoiceAccessibilityService.instance?.performPaste()
                        callback(true, "Pasting text")
                    } else {
                        callback(false, ACCESSIBILITY_NOT_ENABLED_MSG)
                    }
                }
                command.contains("type") -> {
                    val text = extractText(command, "type")
                    if (VoiceAccessibilityService.instance != null) {
                        VoiceAccessibilityService.instance?.performType(text)
                        callback(true, "Typing: $text")
                    } else {
                        callback(false, ACCESSIBILITY_NOT_ENABLED_MSG)
                    }
                }

                // Communication
                command.contains("call") -> {
                    val contact = extractContactName(command)
                    val response = makeCall(contact)
                    callback(response != null, response ?: "Failed to make call")
                }
                command.contains("send message") || command.contains("text") -> {
                    val parts = extractMessageParts(command)
                    val response = sendMessage(parts.first, parts.second)
                    callback(response != null, response ?: "Failed to send message")
                }

                // Alarms and timers
                command.contains("set alarm") -> {
                    val time = extractTime(command)
                    setAlarm(time)
                    callback(true, "Setting alarm for ${time.first}:${String.format("%02d", time.second)}")
                }
                command.contains("set timer") -> {
                    val duration = extractDuration(command)
                    setTimer(duration)
                    val minutes = duration / 60
                    callback(true, "Setting timer for $minutes minute${if (minutes != 1) "s" else ""}")
                }
                command.contains("start stopwatch") -> {
                    startStopwatch()
                    callback(true, "Starting stopwatch")
                }
                command.contains("start countdown") -> {
                    val duration = extractDuration(command)
                    startCountdown(duration)
                    val minutes = duration / 60
                    callback(true, "Starting countdown for $minutes minute${if (minutes != 1) "s" else ""}")
                }

                else -> {
                    callback(false, "I don't understand that command. Please try again")
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
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
        return appName.trim()
    }

    private fun extractText(command: String, prefix: String): String {
        return command.substringAfter(prefix).trim()
    }

    private fun extractContactName(command: String): String {
        return command.replace("call", "").trim()
    }

    private fun extractMessageParts(command: String): Pair<String, String> {
        // Extract recipient and message from command like "send message to John saying Hello"
        val toPattern = "to\\s+(.+?)\\s+saying\\s+(.+)".toRegex(RegexOption.IGNORE_CASE)
        val match = toPattern.find(command)
        
        if (match != null) {
            val recipient = match.groupValues[1].trim()
            val message = match.groupValues[2].trim()
            return Pair(recipient, message)
        }
        
        // Fallback: try simple "to" split
        val parts = command.split(" to ", ignoreCase = true)
        val recipient = parts.getOrNull(1)?.split(" saying ", ignoreCase = true)?.get(0)?.trim() ?: ""
        val message = parts.getOrNull(1)?.split(" saying ", ignoreCase = true)?.getOrNull(1)?.trim() ?: ""
        return Pair(recipient, message)
    }

    private fun extractTime(command: String): Pair<Int, Int> {
        // Simple extraction - in production, use more sophisticated parsing
        val timePattern = "(\\d{1,2}):(\\d{2})".toRegex()
        val match = timePattern.find(command)
        return if (match != null) {
            Pair(match.groupValues[1].toInt(), match.groupValues[2].toInt())
        } else {
            Pair(8, 0) // Default 8:00 AM
        }
    }

    private fun extractDuration(command: String): Int {
        // Extract duration in seconds
        val numberPattern = "(\\d+)".toRegex()
        val match = numberPattern.find(command)
        val number = match?.value?.toInt() ?: 60
        return when {
            command.contains("minute") -> number * 60
            command.contains("hour") -> number * 3600
            else -> number
        }
    }

    private fun openApp(appName: String): String? {
        try {
            android.util.Log.d(TAG, "Attempting to open app: $appName")
            val packageName = getPackageNameForApp(appName)
            android.util.Log.d(TAG, "Resolved package name: $packageName")
            
            val packageManager = context.packageManager
            val intent = packageManager.getLaunchIntentForPackage(packageName)
            
            return if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
                android.util.Log.d(TAG, "Successfully started app: $appName")
                "Opening $appName"
            } else {
                android.util.Log.w(TAG, "No launch intent found for package: $packageName")
                "Could not find $appName. Please make sure it's installed"
            }
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Error opening app: $appName", e)
            e.printStackTrace()
            return "Error opening $appName: ${e.message}"
        }
    }

    private fun getPackageNameForApp(appName: String): String {
        // Try common app mappings first
        AppHelper.getCommonAppPackage(appName)?.let { return it }
        
        // Try to find app by searching installed apps
        AppHelper.findAppPackage(context, appName)?.let { return it }
        
        // Return as-is if not found
        return appName
    }

    private fun closeCurrentApp() {
        // Check if accessibility service is available before using it
        if (VoiceAccessibilityService.instance != null) {
            VoiceAccessibilityService.instance?.performBack()
            VoiceAccessibilityService.instance?.performHome()
        }
    }

    private fun openPlayStore(appName: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse("market://search?q=$appName")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            // Fallback to web browser
            val intent = Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse("https://play.google.com/store/search?q=$appName")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        }
    }

    private fun clearAllBackgroundApps() {
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        activityManager.runningAppProcesses?.forEach { processInfo ->
            if (processInfo.processName != context.packageName) {
                activityManager.killBackgroundProcesses(processInfo.processName)
            }
        }
    }

    private fun clearSpecificApp(appName: String) {
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val packageName = getPackageNameForApp(appName)
        activityManager.killBackgroundProcesses(packageName)
    }

    private fun makeCall(contact: String): String? {
        try {
            val intent = Intent(Intent.ACTION_CALL).apply {
                data = Uri.parse("tel:$contact")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            return "Calling $contact"
        } catch (e: SecurityException) {
            e.printStackTrace()
            return "Phone permission not granted. Please grant call permission"
        } catch (e: Exception) {
            e.printStackTrace()
            return "Error making call: ${e.message}"
        }
    }

    private fun sendMessage(recipient: String, message: String): String? {
        try {
            if (recipient.isEmpty() || message.isEmpty()) {
                return "Please specify both recipient and message"
            }
            val smsManager = SmsManager.getDefault()
            smsManager.sendTextMessage(recipient, null, message, null, null)
            return "Sending message to $recipient"
        } catch (e: SecurityException) {
            e.printStackTrace()
            return "SMS permission not granted. Please grant SMS permission"
        } catch (e: Exception) {
            e.printStackTrace()
            return "Error sending message: ${e.message}"
        }
    }

    private fun setAlarm(time: Pair<Int, Int>) {
        try {
            val intent = Intent(AlarmClock.ACTION_SET_ALARM).apply {
                putExtra(AlarmClock.EXTRA_HOUR, time.first)
                putExtra(AlarmClock.EXTRA_MINUTES, time.second)
                putExtra(AlarmClock.EXTRA_SKIP_UI, false)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun setTimer(durationSeconds: Int) {
        try {
            val intent = Intent(AlarmClock.ACTION_SET_TIMER).apply {
                putExtra(AlarmClock.EXTRA_LENGTH, durationSeconds)
                putExtra(AlarmClock.EXTRA_SKIP_UI, false)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun startStopwatch() {
        try {
            val intent = Intent(AlarmClock.ACTION_SHOW_TIMERS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun startCountdown(durationSeconds: Int) {
        setTimer(durationSeconds)
    }

    private fun openSettings() {
        try {
            val intent = Intent(Settings.ACTION_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun openAccessibilitySettings() {
        try {
            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
