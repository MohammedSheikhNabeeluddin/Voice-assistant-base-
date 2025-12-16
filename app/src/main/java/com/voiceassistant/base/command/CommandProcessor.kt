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

    fun processCommand(command: String, callback: (Boolean) -> Unit) {
        try {
            when {
                // App management
                command.contains("open") && command.contains("app") -> {
                    val appName = extractAppName(command)
                    openApp(appName)
                    callback(true)
                }
                command.contains("close") && command.contains("app") -> {
                    closeCurrentApp()
                    callback(true)
                }
                command.contains("switch to") -> {
                    val appName = extractAppName(command)
                    openApp(appName)
                    callback(true)
                }
                command.contains("download") || command.contains("install") -> {
                    val appName = extractAppName(command)
                    openPlayStore(appName)
                    callback(true)
                }
                command.contains("clear all apps") || command.contains("close all apps") -> {
                    clearAllBackgroundApps()
                    callback(true)
                }
                command.contains("clear background") -> {
                    val appName = extractAppName(command)
                    clearSpecificApp(appName)
                    callback(true)
                }

                // Scrolling and navigation
                command.contains("scroll up") -> {
                    VoiceAccessibilityService.instance?.performScrollUp()
                    callback(true)
                }
                command.contains("scroll down") -> {
                    VoiceAccessibilityService.instance?.performScrollDown()
                    callback(true)
                }
                command.contains("go back") || command.contains("back") -> {
                    VoiceAccessibilityService.instance?.performBack()
                    callback(true)
                }
                command.contains("go home") || command.contains("home") -> {
                    VoiceAccessibilityService.instance?.performHome()
                    callback(true)
                }

                // Text operations
                command.contains("copy") -> {
                    VoiceAccessibilityService.instance?.performCopy()
                    callback(true)
                }
                command.contains("paste") -> {
                    VoiceAccessibilityService.instance?.performPaste()
                    callback(true)
                }
                command.contains("type") -> {
                    val text = extractText(command, "type")
                    VoiceAccessibilityService.instance?.performType(text)
                    callback(true)
                }

                // Communication
                command.contains("call") -> {
                    val contact = extractContactName(command)
                    makeCall(contact)
                    callback(true)
                }
                command.contains("send message") || command.contains("text") -> {
                    val parts = extractMessageParts(command)
                    sendMessage(parts.first, parts.second)
                    callback(true)
                }

                // Alarms and timers
                command.contains("set alarm") -> {
                    val time = extractTime(command)
                    setAlarm(time)
                    callback(true)
                }
                command.contains("set timer") -> {
                    val duration = extractDuration(command)
                    setTimer(duration)
                    callback(true)
                }
                command.contains("start stopwatch") -> {
                    startStopwatch()
                    callback(true)
                }
                command.contains("start countdown") -> {
                    val duration = extractDuration(command)
                    startCountdown(duration)
                    callback(true)
                }

                // System settings
                command.contains("open settings") -> {
                    openSettings()
                    callback(true)
                }
                command.contains("enable accessibility") -> {
                    openAccessibilitySettings()
                    callback(true)
                }

                else -> {
                    callback(false)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            callback(false)
        }
    }

    private fun extractAppName(command: String): String {
        val keywords = listOf("open", "app", "close", "switch to", "download", "install", "clear", "background")
        var appName = command
        keywords.forEach { keyword ->
            appName = appName.replace(keyword, "").trim()
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

    private fun openApp(appName: String) {
        try {
            val packageManager = context.packageManager
            val intent = packageManager.getLaunchIntentForPackage(getPackageNameForApp(appName))
            intent?.let {
                it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(it)
            }
        } catch (e: Exception) {
            e.printStackTrace()
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
        VoiceAccessibilityService.instance?.performBack()
        VoiceAccessibilityService.instance?.performHome()
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

    private fun makeCall(contact: String) {
        try {
            val intent = Intent(Intent.ACTION_CALL).apply {
                data = Uri.parse("tel:$contact")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun sendMessage(recipient: String, message: String) {
        try {
            val smsManager = SmsManager.getDefault()
            smsManager.sendTextMessage(recipient, null, message, null, null)
        } catch (e: Exception) {
            e.printStackTrace()
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
