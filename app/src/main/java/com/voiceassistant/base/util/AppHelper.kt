package com.voiceassistant.base.util

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager

/**
 * Helper class for app-related operations
 */
object AppHelper {

    /**
     * Get list of all installed apps
     */
    fun getInstalledApps(context: Context): List<ApplicationInfo> {
        val packageManager = context.packageManager
        return packageManager.getInstalledApplications(PackageManager.GET_META_DATA)
            .filter { it.flags and ApplicationInfo.FLAG_SYSTEM == 0 }
    }

    /**
     * Get app name from package name
     */
    fun getAppName(context: Context, packageName: String): String? {
        return try {
            val packageManager = context.packageManager
            val applicationInfo = packageManager.getApplicationInfo(packageName, 0)
            packageManager.getApplicationLabel(applicationInfo).toString()
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Check if app is installed
     */
    fun isAppInstalled(context: Context, packageName: String): Boolean {
        return try {
            context.packageManager.getPackageInfo(packageName, 0)
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Search for app package by name
     */
    fun findAppPackage(context: Context, appName: String): String? {
        val installedApps = getInstalledApps(context)
        val searchTerm = appName.lowercase()
        
        return installedApps.firstOrNull { app ->
            val name = getAppName(context, app.packageName)?.lowercase() ?: ""
            name.contains(searchTerm) || app.packageName.lowercase().contains(searchTerm)
        }?.packageName
    }

    /**
     * Common app package mappings
     */
    fun getCommonAppPackage(appName: String): String? {
        return when (appName.lowercase().trim()) {
            "chrome", "browser" -> "com.android.chrome"
            "gmail", "mail", "email" -> "com.google.android.gm"
            "maps", "google maps" -> "com.google.android.apps.maps"
            "youtube" -> "com.google.android.youtube"
            "whatsapp" -> "com.whatsapp"
            "instagram", "insta" -> "com.instagram.android"
            "facebook" -> "com.facebook.katana"
            "twitter" -> "com.twitter.android"
            "messenger" -> "com.facebook.orca"
            "telegram" -> "org.telegram.messenger"
            "snapchat", "snap" -> "com.snapchat.android"
            "spotify" -> "com.spotify.music"
            "netflix" -> "com.netflix.mediaclient"
            "amazon" -> "com.amazon.mShop.android.shopping"
            "play store", "store" -> "com.android.vending"
            "settings" -> "com.android.settings"
            "camera" -> "com.android.camera2"
            "photos", "gallery" -> "com.google.android.apps.photos"
            "calendar" -> "com.google.android.calendar"
            "clock" -> "com.google.android.deskclock"
            "calculator" -> "com.google.android.calculator"
            "contacts" -> "com.google.android.contacts"
            "messages", "sms" -> "com.google.android.apps.messaging"
            "phone", "dialer" -> "com.google.android.dialer"
            else -> null
        }
    }
}
