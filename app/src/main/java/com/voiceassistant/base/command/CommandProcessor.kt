package com.voiceassistant.base.command

import android.app.ActivityManager
import android.app.SearchManager
import android.content.Context
import android.content.Intent
import android.hardware.camera2.CameraManager
import android.media.AudioManager
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.provider.AlarmClock
import android.provider.Settings
import android.telephony.SmsManager
import android.util.Log
import com.voiceassistant.base.service.VoiceAccessibilityService
import com.voiceassistant.base.util.AppHelper

/**
 * Processes voice commands and executes corresponding actions
 */
class CommandProcessor(private val context: Context) {

    private val TAG = "CommandProcessor"
    private val ACCESSIBILITY_NOT_ENABLED_MSG = "Accessibility service not enabled. Please enable it in settings"
    private val handler = Handler(Looper.getMainLooper())
    
    // Track flashlight state
    private var isFlashlightOn = false

    /**
     * Checks if accessibility service is available
     */
    private fun isAccessibilityServiceAvailable(): Boolean {
        val available = VoiceAccessibilityService.instance != null
        if (!available) {
            Log.w(TAG, "Accessibility service is not available")
        }
        return available
    }

    /**
     * Main command processing function
     */
    fun processCommand(command: String, callback: (Boolean, String) -> Unit) {
        Log.d(TAG, "============================================")
        Log.d(TAG, "Processing command: '$command'")
        Log.d(TAG, "============================================")

        if (command.isBlank()) {
            Log.w(TAG, "Received empty command")
            callback(false, "No command received")
            return
        }

        val normalizedCommand = command.lowercase().trim()
        Log.d(TAG, "Normalized command: '$normalizedCommand'")

        try {
            when {
                // System settings (check before general "open")
                normalizedCommand.contains("open settings") -> {
                    Log.d(TAG, "Matched: open settings")
                    val result = openSettings()
                    callback(result.first, result.second)
                }

                normalizedCommand.contains("enable accessibility") || 
                normalizedCommand.contains("open accessibility") -> {
                    Log.d(TAG, "Matched: open accessibility settings")
                    val result = openAccessibilitySettings()
                    callback(result.first, result.second)
                }

                // App management
                normalizedCommand.contains("open") -> {
                    val appName = extractAppName(normalizedCommand, "open")
                    Log.d(TAG, "Matched: open app '$appName'")
                    val result = openApp(appName)
                    callback(result.first, result.second)
                }

                normalizedCommand.contains("close app") || 
                normalizedCommand.contains("close current") -> {
                    Log.d(TAG, "Matched: close current app")
                    closeCurrentApp(callback)
                }

                normalizedCommand.contains("switch to") -> {
                    val appName = extractAppName(normalizedCommand, "switch to")
                    Log.d(TAG, "Matched: switch to app '$appName'")
                    val result = openApp(appName)
                    callback(result.first, result.second)
                }

                normalizedCommand.contains("download") || 
                normalizedCommand.contains("install") -> {
                    val keyword = if (normalizedCommand.contains("download")) "download" else "install"
                    val appName = extractAppName(normalizedCommand, keyword)
                    Log.d(TAG, "Matched: download/install app '$appName'")
                    val result = openPlayStore(appName)
                    callback(result.first, result.second)
                }

                normalizedCommand.contains("clear all apps") || 
                normalizedCommand.contains("close all apps") -> {
                    Log.d(TAG, "Matched: clear all background apps")
                    val result = clearAllBackgroundApps()
                    callback(result.first, result.second)
                }

                normalizedCommand.contains("clear background") -> {
                    val appName = extractAppName(normalizedCommand, "clear background")
                    Log.d(TAG, "Matched: clear specific app '$appName'")
                    val result = clearSpecificApp(appName)
                    callback(result.first, result.second)
                }

                // Scrolling and navigation
                normalizedCommand.contains("scroll up") -> {
                    Log.d(TAG, "Matched: scroll up")
                    if (isAccessibilityServiceAvailable()) {
                        val success = VoiceAccessibilityService.instance?.performScrollUp() ?: false
                        if (success) {
                            callback(true, "Scrolling up")
                        } else {
                            callback(false, "Failed to scroll up")
                        }
                    } else {
                        callback(false, ACCESSIBILITY_NOT_ENABLED_MSG)
                    }
                }

                normalizedCommand.contains("scroll down") -> {
                    Log.d(TAG, "Matched: scroll down")
                    if (isAccessibilityServiceAvailable()) {
                        val success = VoiceAccessibilityService.instance?.performScrollDown() ?: false
                        if (success) {
                            callback(true, "Scrolling down")
                        } else {
                            callback(false, "Failed to scroll down")
                        }
                    } else {
                        callback(false, ACCESSIBILITY_NOT_ENABLED_MSG)
                    }
                }

                normalizedCommand.contains("scroll left") -> {
                    Log.d(TAG, "Matched: scroll left")
                    if (isAccessibilityServiceAvailable()) {
                        val success = VoiceAccessibilityService.instance?.performScrollLeft() ?: false
                        if (success) {
                            callback(true, "Scrolling left")
                        } else {
                            callback(false, "Failed to scroll left")
                        }
                    } else {
                        callback(false, ACCESSIBILITY_NOT_ENABLED_MSG)
                    }
                }

                normalizedCommand.contains("scroll right") -> {
                    Log.d(TAG, "Matched: scroll right")
                    if (isAccessibilityServiceAvailable()) {
                        val success = VoiceAccessibilityService.instance?.performScrollRight() ?: false
                        if (success) {
                            callback(true, "Scrolling right")
                        } else {
                            callback(false, "Failed to scroll right")
                        }
                    } else {
                        callback(false, ACCESSIBILITY_NOT_ENABLED_MSG)
                    }
                }

                normalizedCommand.contains("go back") || 
                (normalizedCommand.contains("back") && !normalizedCommand.contains("background")) -> {
                    Log.d(TAG, "Matched: go back")
                    if (isAccessibilityServiceAvailable()) {
                        val success = VoiceAccessibilityService.instance?.performBack() ?: false
                        if (success) {
                            callback(true, "Going back")
                        } else {
                            callback(false, "Failed to go back")
                        }
                    } else {
                        callback(false, ACCESSIBILITY_NOT_ENABLED_MSG)
                    }
                }

                normalizedCommand.contains("go home") || 
                normalizedCommand == "home" || 
                normalizedCommand.contains("home screen") -> {
                    Log.d(TAG, "Matched: go home")
                    if (isAccessibilityServiceAvailable()) {
                        val success = VoiceAccessibilityService.instance?.performHome() ?: false
                        if (success) {
                            callback(true, "Going to home screen")
                        } else {
                            callback(false, "Failed to go home")
                        }
                    } else {
                        callback(false, ACCESSIBILITY_NOT_ENABLED_MSG)
                    }
                }

                normalizedCommand.contains("recent") || 
                normalizedCommand.contains("recents") -> {
                    Log.d(TAG, "Matched: show recents")
                    if (isAccessibilityServiceAvailable()) {
                        val success = VoiceAccessibilityService.instance?.performRecents() ?: false
                        if (success) {
                            callback(true, "Showing recent apps")
                        } else {
                            callback(false, "Failed to show recent apps")
                        }
                    } else {
                        callback(false, ACCESSIBILITY_NOT_ENABLED_MSG)
                    }
                }

                // Text operations
                normalizedCommand.contains("copy") -> {
                    Log.d(TAG, "Matched: copy")
                    if (isAccessibilityServiceAvailable()) {
                        val success = VoiceAccessibilityService.instance?.performCopy() ?: false
                        if (success) {
                            callback(true, "Copying text")
                        } else {
                            callback(false, "Failed to copy text")
                        }
                    } else {
                        callback(false, ACCESSIBILITY_NOT_ENABLED_MSG)
                    }
                }

                normalizedCommand.contains("paste") -> {
                    Log.d(TAG, "Matched: paste")
                    if (isAccessibilityServiceAvailable()) {
                        val success = VoiceAccessibilityService.instance?.performPaste() ?: false
                        if (success) {
                            callback(true, "Pasting text")
                        } else {
                            callback(false, "Failed to paste text")
                        }
                    } else {
                        callback(false, ACCESSIBILITY_NOT_ENABLED_MSG)
                    }
                }

                normalizedCommand.contains("select all") -> {
                    Log.d(TAG, "Matched: select all")
                    if (isAccessibilityServiceAvailable()) {
                        val success = VoiceAccessibilityService.instance?.performSelectAll() ?: false
                        if (success) {
                            callback(true, "Selecting all text")
                        } else {
                            callback(false, "Failed to select all")
                        }
                    } else {
                        callback(false, ACCESSIBILITY_NOT_ENABLED_MSG)
                    }
                }

                normalizedCommand.contains("type") -> {
                    val text = extractTextAfterKeyword(normalizedCommand, "type")
                    Log.d(TAG, "Matched: type '$text'")
                    if (text.isNotEmpty()) {
                        if (isAccessibilityServiceAvailable()) {
                            val success = VoiceAccessibilityService.instance?.performType(text) ?: false
                            if (success) {
                                callback(true, "Typing: $text")
                            } else {
                                callback(false, "Failed to type text")
                            }
                        } else {
                            callback(false, ACCESSIBILITY_NOT_ENABLED_MSG)
                        }
                    } else {
                        callback(false, "Please specify what to type")
                    }
                }

                // Click operations
                normalizedCommand.contains("click") || normalizedCommand.contains("tap") -> {
                    val target = extractTextAfterKeyword(normalizedCommand, 
                        if (normalizedCommand.contains("click")) "click" else "tap")
                    Log.d(TAG, "Matched: click '$target'")
                    if (target.isNotEmpty()) {
                        if (isAccessibilityServiceAvailable()) {
                            val success = VoiceAccessibilityService.instance?.performClickOnText(target) ?: false
                            if (success) {
                                callback(true, "Clicking on $target")
                            } else {
                                callback(false, "Could not find '$target' to click")
                            }
                        } else {
                            callback(false, ACCESSIBILITY_NOT_ENABLED_MSG)
                        }
                    } else {
                        callback(false, "Please specify what to click")
                    }
                }

                // Communication - Call
                normalizedCommand.contains("call") -> {
                    val contact = extractTextAfterKeyword(normalizedCommand, "call")
                    Log.d(TAG, "Matched: call '$contact'")
                    val result = makeCall(contact)
                    callback(result.first, result.second)
                }

                // Communication - Message
                normalizedCommand.contains("send message") || 
                normalizedCommand.contains("text") ||
                normalizedCommand.contains("message") -> {
                    Log.d(TAG, "Matched: send message")
                    val result = parseAndSendMessage(normalizedCommand)
                    callback(result.first, result.second)
                }

                // Timer and Alarm
                normalizedCommand.contains("set alarm") -> {
                    Log.d(TAG, "Matched: set alarm")
                    val time = parseTime(normalizedCommand)
                    if (time != null) {
                        val result = setAlarm(time)
                        callback(result.first, result.second)
                    } else {
                        callback(false, "Could not understand the time. Please say like 'set alarm for 7:30'")
                    }
                }

                normalizedCommand.contains("set timer") -> {
                    Log.d(TAG, "Matched: set timer")
                    val duration = parseDuration(normalizedCommand)
                    if (duration > 0) {
                        val result = setTimer(duration)
                        callback(result.first, result.second)
                    } else {
                        callback(false, "Could not understand the duration. Please say like 'set timer for 5 minutes'")
                    }
                }

                normalizedCommand.contains("start stopwatch") || 
                normalizedCommand.contains("stopwatch") -> {
                    Log.d(TAG, "Matched: start stopwatch")
                    val result = startStopwatch()
                    callback(result.first, result.second)
                }

                normalizedCommand.contains("countdown") -> {
                    Log.d(TAG, "Matched: start countdown")
                    val duration = parseDuration(normalizedCommand)
                    if (duration > 0) {
                        val result = startCountdown(duration)
                        callback(result.first, result.second)
                    } else {
                        callback(false, "Could not understand the duration")
                    }
                }

                // Web browsing
                normalizedCommand.contains("search for") || 
                normalizedCommand.contains("search") -> {
                    val query = extractTextAfterKeyword(normalizedCommand, 
                        if (normalizedCommand.contains("search for")) "search for" else "search")
                    Log.d(TAG, "Matched: search '$query'")
                    val result = webSearch(query)
                    callback(result.first, result.second)
                }

                normalizedCommand.contains("go to") && 
                (normalizedCommand.contains(".com") || 
                 normalizedCommand.contains(".org") || 
                 normalizedCommand.contains(".net") ||
                 normalizedCommand.contains("website")) -> {
                    val url = extractUrl(normalizedCommand)
                    Log.d(TAG, "Matched: go to website '$url'")
                    val result = openWebsite(url)
                    callback(result.first, result.second)
                }

                normalizedCommand.contains("open website") || 
                normalizedCommand.contains("open site") -> {
                    val url = extractUrl(normalizedCommand)
                    Log.d(TAG, "Matched: open website '$url'")
                    val result = openWebsite(url)
                    callback(result.first, result.second)
                }

                // Volume controls
                normalizedCommand.contains("volume up") || 
                normalizedCommand.contains("increase volume") -> {
                    Log.d(TAG, "Matched: volume up")
                    val result = adjustVolume(true)
                    callback(result.first, result.second)
                }

                normalizedCommand.contains("volume down") || 
                normalizedCommand.contains("decrease volume") -> {
                    Log.d(TAG, "Matched: volume down")
                    val result = adjustVolume(false)
                    callback(result.first, result.second)
                }

                normalizedCommand.contains("mute") -> {
                    Log.d(TAG, "Matched: mute")
                    val result = muteVolume()
                    callback(result.first, result.second)
                }

                // Brightness
                normalizedCommand.contains("brightness") -> {
                    Log.d(TAG, "Matched: brightness control")
                    val result = openBrightnessSettings()
                    callback(result.first, result.second)
                }

                // Flashlight/Torch
                normalizedCommand.contains("flashlight") || 
                normalizedCommand.contains("torch") -> {
                    Log.d(TAG, "Matched: flashlight toggle")
                    val turnOn = normalizedCommand.contains("on") || 
                                 (!normalizedCommand.contains("off") && !isFlashlightOn)
                    val result = toggleFlashlight(turnOn)
                    callback(result.first, result.second)
                }

                // Take screenshot
                normalizedCommand.contains("screenshot") || 
                normalizedCommand.contains("screen shot") -> {
                    Log.d(TAG, "Matched: take screenshot")
                    if (isAccessibilityServiceAvailable()) {
                        val success = VoiceAccessibilityService.instance?.performScreenshot() ?: false
                        if (success) {
                            callback(true, "Taking screenshot")
                        } else {
                            callback(false, "Failed to take screenshot. This feature requires Android 9 or higher")
                        }
                    } else {
                        callback(false, ACCESSIBILITY_NOT_ENABLED_MSG)
                    }
                }

                // Notification panel
                normalizedCommand.contains("notification") || 
                normalizedCommand.contains("notifications") -> {
                    Log.d(TAG, "Matched: open notifications")
                    if (isAccessibilityServiceAvailable()) {
                        val success = VoiceAccessibilityService.instance?.openNotifications() ?: false
                        if (success) {
                            callback(true, "Opening notifications")
                        } else {
                            callback(false, "Failed to open notifications")
                        }
                    } else {
                        callback(false, ACCESSIBILITY_NOT_ENABLED_MSG)
                    }
                }

                // Quick settings
                normalizedCommand.contains("quick settings") -> {
                    Log.d(TAG, "Matched: open quick settings")
                    if (isAccessibilityServiceAvailable()) {
                        val success = VoiceAccessibilityService.instance?.openQuickSettings() ?: false
                        if (success) {
                            callback(true, "Opening quick settings")
                        } else {
                            callback(false, "Failed to open quick settings")
                        }
                    } else {
                        callback(false, ACCESSIBILITY_NOT_ENABLED_MSG)
                    }
                }

                // Unknown command fallback
                else -> {
                    Log.w(TAG, "Unknown command: '$normalizedCommand'")
                    callback(false, "Sorry, I didn't understand '$command'. Try saying 'open Chrome' or 'scroll down'")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error processing command: '$command'", e)
            callback(false, "Error processing command: ${e.message}")
        }
    }

    // ==================== HELPER METHODS ====================

    private fun extractAppName(command: String, keyword: String): String {
        val afterKeyword = command.substringAfter(keyword).trim()
        return afterKeyword
            .replace("the ", "")
            .replace("app ", "")
            .replace("application ", "")
            .trim()
    }

    private fun extractTextAfterKeyword(command: String, keyword: String): String {
        return command.substringAfter(keyword).trim()
    }

    private fun parseTime(command: String): Pair<Int, Int>? {
        val timePatterns = listOf(
            Regex("""(\d{1,2}):(\d{2})"""),
            Regex("""(\d{1,2})\s+(\d{2})"""),
            Regex("""(\d{1,2})(\d{2})\b"""),
            Regex("""(\d{1,2})\s*(am|pm)""", RegexOption.IGNORE_CASE),
            Regex("""(\d{1,2})\s*o'?\s*clock""", RegexOption.IGNORE_CASE)
        )

        for (pattern in timePatterns) {
            val match = pattern.find(command)
            if (match != null) {
                val groups = match.groupValues
                return when {
                    groups.size >= 3 && groups[2].matches(Regex("\\d+")) -> {
                        Pair(groups[1].toInt(), groups[2].toInt())
                    }
                    groups.size >= 3 && groups[2].lowercase() in listOf("am", "pm") -> {
                        var hour = groups[1].toInt()
                        if (groups[2].lowercase() == "pm" && hour < 12) hour += 12
                        if (groups[2].lowercase() == "am" && hour == 12) hour = 0
                        Pair(hour, 0)
                    }
                    groups.size >= 2 -> {
                        Pair(groups[1].toInt(), 0)
                    }
                    else -> null
                }
            }
        }
        return null
    }

    private fun parseDuration(command: String): Int {
        val patterns = listOf(
            Regex("""(\d+)\s*hour""", RegexOption.IGNORE_CASE) to 3600,
            Regex("""(\d+)\s*minute""", RegexOption.IGNORE_CASE) to 60,
            Regex("""(\d+)\s*second""", RegexOption.IGNORE_CASE) to 1,
            Regex("""(\d+)\s*min""", RegexOption.IGNORE_CASE) to 60,
            Regex("""(\d+)\s*sec""", RegexOption.IGNORE_CASE) to 1,
            Regex("""(\d+)\s*hr""", RegexOption.IGNORE_CASE) to 3600
        )

        var totalSeconds = 0
        for ((pattern, multiplier) in patterns) {
            val match = pattern.find(command)
            if (match != null) {
                totalSeconds += match.groupValues[1].toInt() * multiplier
            }
        }

        if (totalSeconds == 0) {
            val justNumber = Regex("""(\d+)""").find(command)
            if (justNumber != null) {
                totalSeconds = justNumber.groupValues[1].toInt() * 60
            }
        }

        return totalSeconds
    }

    private fun extractUrl(command: String): String {
        val domainPattern = Regex("""(\w+\.(com|org|net|io|co|in|edu|gov)[\w/.]*)""", RegexOption.IGNORE_CASE)
        val match = domainPattern.find(command)
        if (match != null) {
            var url = match.value
            if (!url.startsWith("http")) {
                url = "https://$url"
            }
            return url
        }
        
        val afterKeyword = command
            .substringAfter("go to", command)
            .substringAfter("open website", command)
            .substringAfter("open site", command)
            .trim()
        
        return if (afterKeyword.isNotEmpty() && afterKeyword != command) {
            val cleaned = afterKeyword.replace(" ", "")
            "https://www.$cleaned.com"
        } else {
            ""
        }
    }

    private fun parseAndSendMessage(command: String): Pair<Boolean, String> {
        val sayingPattern = Regex(
            """(?:send message to|text|message)\s+(.+?)\s+(?:saying|that|with message)\s+(.+)""", 
            RegexOption.IGNORE_CASE
        )
        val match = sayingPattern.find(command)
        
        return if (match != null) {
            val contact = match.groupValues[1].trim()
            val message = match.groupValues[2].trim()
            sendMessage(contact, message)
        } else {
            val simplePattern = Regex(
                """(?:send message to|text|message)\s+(.+)""", 
                RegexOption.IGNORE_CASE
            )
            val simpleMatch = simplePattern.find(command)
            if (simpleMatch != null) {
                val parts = simpleMatch.groupValues[1].split(" ", limit = 2)
                if (parts.size >= 2) {
                    sendMessage(parts[0], parts[1])
                } else {
                    Pair(false, "Please say 'send message to [name] saying [message]'")
                }
            } else {
                Pair(false, "Please say 'send message to [name] saying [message]'")
            }
        }
    }

    // ==================== ACTION METHODS ====================

    private fun openApp(appName: String): Pair<Boolean, String> {
        if (appName.isBlank()) {
            return Pair(false, "Please specify an app name")
        }

        Log.d(TAG, "openApp: Attempting to open '$appName'")

        return try {
            val packageName = getPackageNameForApp(appName)
            Log.d(TAG, "openApp: Resolved package name '$packageName'")

            val launchIntent = context.packageManager.getLaunchIntentForPackage(packageName)
            if (launchIntent != null) {
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(launchIntent)
                Log.i(TAG, "openApp: Successfully opened '$appName'")
                Pair(true, "Opening $appName")
            } else {
                Log.w(TAG, "openApp: Could not find launch intent for '$packageName'")
                Pair(false, "Could not find $appName. Please make sure it's installed")
            }
        } catch (e: Exception) {
            Log.e(TAG, "openApp: Error opening '$appName'", e)
            Pair(false, "Error opening $appName: ${e.message}")
        }
    }

    private fun getPackageNameForApp(appName: String): String {
        AppHelper.getCommonAppPackage(appName)?.let { return it }
        AppHelper.findAppPackage(context, appName)?.let { return it }
        return appName
    }

    // Fixed: No longer blocks the main thread
    private fun closeCurrentApp(callback: (Boolean, String) -> Unit) {
        if (!isAccessibilityServiceAvailable()) {
            callback(false, ACCESSIBILITY_NOT_ENABLED_MSG)
            return
        }

        try {
            VoiceAccessibilityService.instance?.performBack()
            // Use handler instead of Thread.sleep
            handler.postDelayed({
                VoiceAccessibilityService.instance?.performHome()
                callback(true, "Closing current app")
            }, 200)
        } catch (e: Exception) {
            Log.e(TAG, "closeCurrentApp: Error", e)
            callback(false, "Error closing app: ${e.message}")
        }
    }

    private fun openPlayStore(appName: String): Pair<Boolean, String> {
        if (appName.isBlank()) {
            return Pair(false, "Please specify an app to download")
        }

        return try {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse("market://search?q=$appName")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            Pair(true, "Opening Play Store for $appName")
        } catch (e: Exception) {
            try {
                val intent = Intent(Intent.ACTION_VIEW).apply {
                    data = Uri.parse("https://play.google.com/store/search?q=$appName")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
                Pair(true, "Opening Play Store for $appName")
            } catch (e2: Exception) {
                Pair(false, "Error opening Play Store: ${e2.message}")
            }
        }
    }

    private fun clearAllBackgroundApps(): Pair<Boolean, String> {
        return try {
            val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            val runningApps = activityManager.runningAppProcesses
            
            var clearedCount = 0
            runningApps?.forEach { process ->
                if (process.importance > ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND) {
                    try {
                        activityManager.killBackgroundProcesses(process.processName)
                        clearedCount++
                    } catch (e: Exception) {
                        Log.w(TAG, "Could not kill process: ${process.processName}")
                    }
                }
            }
            
            Pair(true, "Cleared $clearedCount background apps")
        } catch (e: Exception) {
            Log.e(TAG, "clearAllBackgroundApps: Error", e)
            Pair(false, "Error clearing background apps: ${e.message}")
        }
    }

    private fun clearSpecificApp(appName: String): Pair<Boolean, String> {
        if (appName.isBlank()) {
            return Pair(false, "Please specify an app to clear")
        }

        return try {
            val packageName = getPackageNameForApp(appName)
            val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            activityManager.killBackgroundProcesses(packageName)
            Pair(true, "Cleared $appName from background")
        } catch (e: Exception) {
            Log.e(TAG, "clearSpecificApp: Error", e)
            Pair(false, "Error clearing $appName: ${e.message}")
        }
    }

    private fun makeCall(contact: String): Pair<Boolean, String> {
        if (contact.isBlank()) {
            return Pair(false, "Please specify who to call")
        }

        return try {
            val phoneNumber = if (contact.matches(Regex("[0-9+\\-\\s]+"))) {
                contact.replace(Regex("[\\s\\-]"), "")
            } else {
                lookupContactNumber(contact) ?: contact
            }

            val intent = Intent(Intent.ACTION_CALL).apply {
                data = Uri.parse("tel:$phoneNumber")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            Pair(true, "Calling $contact")
        } catch (e: SecurityException) {
            try {
                val phoneNumber = contact.replace(Regex("[\\s\\-]"), "")
                val intent = Intent(Intent.ACTION_DIAL).apply {
                    data = Uri.parse("tel:$phoneNumber")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
                Pair(true, "Opening dialer for $contact")
            } catch (e2: Exception) {
                Pair(false, "Error making call: ${e2.message}")
            }
        } catch (e: Exception) {
            Pair(false, "Error making call: ${e.message}")
        }
    }

    private fun lookupContactNumber(name: String): String? {
        return try {
            val cursor = context.contentResolver.query(
                android.provider.ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                arrayOf(android.provider.ContactsContract.CommonDataKinds.Phone.NUMBER),
                "${android.provider.ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} LIKE ?",
                arrayOf("%$name%"),
                null
            )
            cursor?.use {
                if (it.moveToFirst()) {
                    it.getString(0)
                } else null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error looking up contact", e)
            null
        }
    }

    @Suppress("DEPRECATION")
    private fun sendMessage(recipient: String, message: String): Pair<Boolean, String> {
        if (recipient.isBlank()) {
            return Pair(false, "Please specify a recipient")
        }
        if (message.isBlank()) {
            return Pair(false, "Please specify a message")
        }

        return try {
            val phoneNumber = if (recipient.matches(Regex("[0-9+\\-\\s]+"))) {
                recipient.replace(Regex("[\\s\\-]"), "")
            } else {
                lookupContactNumber(recipient) ?: recipient
            }

            val smsManager = SmsManager.getDefault()
            smsManager.sendTextMessage(phoneNumber, null, message, null, null)
            Pair(true, "Sending message to $recipient")
        } catch (e: SecurityException) {
            try {
                val intent = Intent(Intent.ACTION_SENDTO).apply {
                    data = Uri.parse("smsto:$recipient")
                    putExtra("sms_body", message)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
                Pair(true, "Opening messaging app for $recipient")
            } catch (e2: Exception) {
                Pair(false, "Error sending message: ${e2.message}")
            }
        } catch (e: Exception) {
            Pair(false, "Error sending message: ${e.message}")
        }
    }

    private fun setAlarm(time: Pair<Int, Int>): Pair<Boolean, String> {
        return try {
            val intent = Intent(AlarmClock.ACTION_SET_ALARM).apply {
                putExtra(AlarmClock.EXTRA_HOUR, time.first)
                putExtra(AlarmClock.EXTRA_MINUTES, time.second)
                putExtra(AlarmClock.EXTRA_SKIP_UI, false)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            val timeStr = String.format("%d:%02d", time.first, time.second)
            Pair(true, "Setting alarm for $timeStr")
        } catch (e: Exception) {
            Pair(false, "Error setting alarm: ${e.message}")
        }
    }

    private fun setTimer(durationSeconds: Int): Pair<Boolean, String> {
        return try {
            val intent = Intent(AlarmClock.ACTION_SET_TIMER).apply {
                putExtra(AlarmClock.EXTRA_LENGTH, durationSeconds)
                putExtra(AlarmClock.EXTRA_SKIP_UI, false)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            
            val minutes = durationSeconds / 60
            val seconds = durationSeconds % 60
            val timeStr = when {
                minutes > 0 && seconds > 0 -> "$minutes minutes and $seconds seconds"
                minutes > 0 -> "$minutes minute${if (minutes > 1) "s" else ""}"
                else -> "$seconds seconds"
            }
            Pair(true, "Setting timer for $timeStr")
        } catch (e: Exception) {
            Pair(false, "Error setting timer: ${e.message}")
        }
    }

    private fun startStopwatch(): Pair<Boolean, String> {
        return try {
            val intent = Intent(AlarmClock.ACTION_SHOW_TIMERS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            Pair(true, "Starting stopwatch")
        } catch (e: Exception) {
            Pair(false, "Error starting stopwatch: ${e.message}")
        }
    }

    private fun startCountdown(durationSeconds: Int): Pair<Boolean, String> {
        return setTimer(durationSeconds)
    }

    private fun openSettings(): Pair<Boolean, String> {
        return try {
            val intent = Intent(Settings.ACTION_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            Pair(true, "Opening settings")
        } catch (e: Exception) {
            Pair(false, "Error opening settings: ${e.message}")
        }
    }

    private fun openAccessibilitySettings(): Pair<Boolean, String> {
        return try {
            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            Pair(true, "Opening accessibility settings")
        } catch (e: Exception) {
            Pair(false, "Error opening accessibility settings: ${e.message}")
        }
    }

    private fun webSearch(query: String): Pair<Boolean, String> {
        if (query.isBlank()) {
            return Pair(false, "Please specify what to search for")
        }

        return try {
            val intent = Intent(Intent.ACTION_WEB_SEARCH).apply {
                putExtra(SearchManager.QUERY, query)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            Pair(true, "Searching for $query")
        } catch (e: Exception) {
            try {
                val intent = Intent(Intent.ACTION_VIEW).apply {
                    data = Uri.parse("https://www.google.com/search?q=${Uri.encode(query)}")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
                Pair(true, "Searching for $query")
            } catch (e2: Exception) {
                Pair(false, "Error searching: ${e2.message}")
            }
        }
    }

    private fun openWebsite(url: String): Pair<Boolean, String> {
        if (url.isBlank()) {
            return Pair(false, "Please specify a website")
        }

        return try {
            var finalUrl = url
            if (!finalUrl.startsWith("http")) {
                finalUrl = "https://$finalUrl"
            }
            
            val intent = Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse(finalUrl)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            Pair(true, "Opening $url")
        } catch (e: Exception) {
            Pair(false, "Error opening website: ${e.message}")
        }
    }

    private fun adjustVolume(increase: Boolean): Pair<Boolean, String> {
        return try {
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            val direction = if (increase) 
                AudioManager.ADJUST_RAISE 
            else 
                AudioManager.ADJUST_LOWER
            
            audioManager.adjustVolume(direction, AudioManager.FLAG_SHOW_UI)
            Pair(true, if (increase) "Increasing volume" else "Decreasing volume")
        } catch (e: Exception) {
            Pair(false, "Error adjusting volume: ${e.message}")
        }
    }

    private fun muteVolume(): Pair<Boolean, String> {
        return try {
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            audioManager.adjustVolume(AudioManager.ADJUST_MUTE, AudioManager.FLAG_SHOW_UI)
            Pair(true, "Muting volume")
        } catch (e: Exception) {
            Pair(false, "Error muting: ${e.message}")
        }
    }

    private fun openBrightnessSettings(): Pair<Boolean, String> {
        return try {
            val intent = Intent(Settings.ACTION_DISPLAY_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            Pair(true, "Opening display settings")
        } catch (e: Exception) {
            Pair(false, "Error opening settings: ${e.message}")
        }
    }

    // Fixed: Now properly toggles flashlight on/off
    private fun toggleFlashlight(turnOn: Boolean): Pair<Boolean, String> {
        return try {
            val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
            val cameraId = cameraManager.cameraIdList.firstOrNull() ?: return Pair(false, "No camera found")
            
            cameraManager.setTorchMode(cameraId, turnOn)
            isFlashlightOn = turnOn
            
            Pair(true, if (turnOn) "Turning on flashlight" else "Turning off flashlight")
        } catch (e: Exception) {
            Pair(false, "Error with flashlight: ${e.message}")
        }
    }
}
