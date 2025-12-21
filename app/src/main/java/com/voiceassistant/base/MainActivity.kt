package com.voiceassistant.base

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.text.SpannableStringBuilder
import android.text.style.ForegroundColorSpan
import android.widget.Button
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.voiceassistant.base.service.VoiceAccessibilityService
import com.voiceassistant.base.service.VoiceAssistantService

/**
 * Main Activity for Voice Assistant
 * Handles UI, permissions, and service control
 */
class MainActivity : AppCompatActivity() {

    // UI Elements
    private lateinit var statusText: TextView
    private lateinit var transcriptText: TextView
    private lateinit var transcriptScroll: ScrollView
    private lateinit var startButton: Button
    private lateinit var stopButton: Button
    private lateinit var permissionButton: Button
    private lateinit var accessibilityButton: Button
    private lateinit var clearButton: Button

    // Permission request code
    private val PERMISSION_REQUEST_CODE = 100
    private val OVERLAY_PERMISSION_REQUEST_CODE = 101

    // Handler for UI updates
    private val handler = Handler(Looper.getMainLooper())

    // Required permissions list
    private val requiredPermissions = arrayOf(
        Manifest.permission.RECORD_AUDIO,
        Manifest.permission.CALL_PHONE,
        Manifest.permission.SEND_SMS,
        Manifest.permission.READ_CONTACTS,
        Manifest.permission.CAMERA
    )

    // Broadcast receiver for transcript updates from service
    private val transcriptReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent == null) return
            
            when (intent.action) {
                VoiceAssistantService.ACTION_TRANSCRIPT_UPDATE -> {
                    val text = intent.getStringExtra(VoiceAssistantService.EXTRA_TRANSCRIPT_TEXT)
                    val isUser = intent.getBooleanExtra(VoiceAssistantService.EXTRA_IS_USER, true)
                    if (text != null) {
                        addTranscriptEntry(text, isUser)
                    }
                }
                VoiceAssistantService.ACTION_STATUS_UPDATE -> {
                    updateStatus()
                }
            }
        }
    }

    // ==================== LIFECYCLE METHODS ====================

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialize all views
        initializeViews()
        
        // Setup button click listeners
        setupClickListeners()
        
        // Update UI status
        updateStatus()
        
        // Register broadcast receiver for transcript updates
        registerTranscriptReceiver()
        
        // Show welcome message
        addTranscriptEntry("Welcome to Voice Assistant!", false)
        addTranscriptEntry("Say \"Hey Assistant\" to activate.", false)
    }

    override fun onResume() {
        super.onResume()
        // Update status when app comes to foreground
        updateStatus()
    }

    override fun onDestroy() {
        super.onDestroy()
        // Unregister broadcast receiver
        try {
            LocalBroadcastManager.getInstance(this).unregisterReceiver(transcriptReceiver)
        } catch (e: Exception) {
            // Receiver was not registered, ignore
        }
    }

    // ==================== INITIALIZATION ====================

    /**
     * Initialize all UI views
     */
    private fun initializeViews() {
        statusText = findViewById(R.id.statusText)
        transcriptText = findViewById(R.id.transcriptText)
        transcriptScroll = findViewById(R.id.transcriptScroll)
        startButton = findViewById(R.id.startButton)
        stopButton = findViewById(R.id.stopButton)
        permissionButton = findViewById(R.id.permissionButton)
        accessibilityButton = findViewById(R.id.accessibilityButton)
        clearButton = findViewById(R.id.clearButton)
    }

    /**
     * Setup click listeners for all buttons
     */
    private fun setupClickListeners() {
        // Start button
        startButton.setOnClickListener {
            startVoiceAssistant()
        }

        // Stop button
        stopButton.setOnClickListener {
            stopVoiceAssistant()
        }

        // Permission button
        permissionButton.setOnClickListener {
            requestAllPermissions()
        }

        // Accessibility button
        accessibilityButton.setOnClickListener {
            openAccessibilitySettings()
        }

        // Clear transcript button
        clearButton.setOnClickListener {
            clearTranscript()
        }
    }

    /**
     * Register broadcast receiver for transcript updates
     */
    private fun registerTranscriptReceiver() {
        val filter = IntentFilter().apply {
            addAction(VoiceAssistantService.ACTION_TRANSCRIPT_UPDATE)
            addAction(VoiceAssistantService.ACTION_STATUS_UPDATE)
        }
        LocalBroadcastManager.getInstance(this).registerReceiver(transcriptReceiver, filter)
    }

    // ==================== PERMISSION HANDLING ====================

    /**
     * Check if all required permissions are granted
     */
    private fun checkAllPermissions(): Boolean {
        for (permission in requiredPermissions) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                return false
            }
        }
        return true
    }

    /**
     * Check if a specific permission is granted
     */
    private fun isPermissionGranted(permission: String): Boolean {
        return ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Request all required permissions
     */
    private fun requestAllPermissions() {
        // Find permissions that need to be requested
        val permissionsToRequest = requiredPermissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }.toTypedArray()

        // Request runtime permissions
        if (permissionsToRequest.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, permissionsToRequest, PERMISSION_REQUEST_CODE)
        } else {
            Toast.makeText(this, "All permissions already granted!", Toast.LENGTH_SHORT).show()
        }

        // Request overlay permission for Android M+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!Settings.canDrawOverlays(this)) {
                showOverlayPermissionDialog()
            }
        }
    }

    /**
     * Show dialog explaining overlay permission
     */
    private fun showOverlayPermissionDialog() {
        AlertDialog.Builder(this)
            .setTitle("Overlay Permission Required")
            .setMessage("This permission allows the voice assistant to display over other apps. Please enable it in the next screen.")
            .setPositiveButton("Open Settings") { _, _ ->
                val intent = Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:$packageName")
                )
                startActivityForResult(intent, OVERLAY_PERMISSION_REQUEST_CODE)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    /**
     * Handle permission request results
     */
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        
        if (requestCode == PERMISSION_REQUEST_CODE) {
            // Check if all permissions were granted
            val allGranted = grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED }
            
            if (allGranted) {
                Toast.makeText(this, "All permissions granted!", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Some permissions were denied. Some features may not work.", Toast.LENGTH_LONG).show()
            }
            
            // Update UI
            updateStatus()
        }
    }

    /**
     * Handle activity results (for overlay permission)
     */
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        
        if (requestCode == OVERLAY_PERMISSION_REQUEST_CODE) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (Settings.canDrawOverlays(this)) {
                    Toast.makeText(this, "Overlay permission granted!", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "Overlay permission denied.", Toast.LENGTH_SHORT).show()
                }
            }
            updateStatus()
        }
    }

    // ==================== ACCESSIBILITY SERVICE ====================

    /**
     * Check if accessibility service is enabled
     */
    private fun isAccessibilityServiceEnabled(): Boolean {
        return VoiceAccessibilityService.instance != null
    }

    /**
     * Open accessibility settings
     */
    private fun openAccessibilitySettings() {
        AlertDialog.Builder(this)
            .setTitle("Enable Accessibility Service")
            .setMessage("To control your device with voice commands, you need to enable the Voice Assistant accessibility service.\n\n1. Find 'Voice Assistant' in the list\n2. Tap on it\n3. Turn it ON\n4. Confirm when prompted")
            .setPositiveButton("Open Settings") { _, _ ->
                val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(intent)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    // ==================== SERVICE CONTROL ====================

    /**
     * Start the voice assistant service
     */
    private fun startVoiceAssistant() {
        // Check permissions first
        if (!checkAllPermissions()) {
            Toast.makeText(this, "Please grant all permissions first", Toast.LENGTH_LONG).show()
            requestAllPermissions()
            return
        }

        // Check accessibility service
        if (!isAccessibilityServiceEnabled()) {
            Toast.makeText(this, "Please enable accessibility service first", Toast.LENGTH_LONG).show()
            openAccessibilitySettings()
            return
        }

        // Start the service
        try {
            val serviceIntent = Intent(this, VoiceAssistantService::class.java)
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(serviceIntent)
            } else {
                startService(serviceIntent)
            }
            
            Toast.makeText(this, "Voice Assistant started!", Toast.LENGTH_SHORT).show()
            addTranscriptEntry("Voice Assistant started. Say \"Hey Assistant\" to activate.", false)
            
            // Update UI after a short delay
            handler.postDelayed({
                updateStatus()
            }, 500)
            
        } catch (e: Exception) {
            Toast.makeText(this, "Error starting service: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    /**
     * Stop the voice assistant service
     */
    private fun stopVoiceAssistant() {
        try {
            val serviceIntent = Intent(this, VoiceAssistantService::class.java)
            stopService(serviceIntent)
            
            Toast.makeText(this, "Voice Assistant stopped", Toast.LENGTH_SHORT).show()
            addTranscriptEntry("Voice Assistant stopped.", false)
            
            // Update UI after a short delay
            handler.postDelayed({
                updateStatus()
            }, 500)
            
        } catch (e: Exception) {
            Toast.makeText(this, "Error stopping service: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    // ==================== UI UPDATES ====================

    /**
     * Update the status display
     */
    private fun updateStatus() {
        val isServiceRunning = VoiceAssistantService.isServiceRunning
        val isAccessibilityEnabled = isAccessibilityServiceEnabled()
        val arePermissionsGranted = checkAllPermissions()

        // Build status text
        val statusBuilder = StringBuilder()
        
        // Service status
        if (isServiceRunning) {
            statusBuilder.append("🟢 Service: Running\n")
        } else {
            statusBuilder.append("🔴 Service: Stopped\n")
        }
        
        // Accessibility status
        if (isAccessibilityEnabled) {
            statusBuilder.append("🟢 Accessibility: Enabled\n")
        } else {
            statusBuilder.append("🔴 Accessibility: Disabled\n")
        }
        
        // Permissions status
        if (arePermissionsGranted) {
            statusBuilder.append("🟢 Permissions: Granted")
        } else {
            statusBuilder.append("🟡 Permissions: Some missing")
        }

        // Update status text
        statusText.text = statusBuilder.toString()

        // Update button states
        startButton.isEnabled = !isServiceRunning
        stopButton.isEnabled = isServiceRunning
        
        // Change button appearance based on state
        if (isServiceRunning) {
            startButton.alpha = 0.5f
            stopButton.alpha = 1.0f
        } else {
            startButton.alpha = 1.0f
            stopButton.alpha = 0.5f
        }
        
        // Update accessibility button
        if (isAccessibilityEnabled) {
            accessibilityButton.text = "Accessibility ✓"
            accessibilityButton.alpha = 0.7f
        } else {
            accessibilityButton.text = "Enable Accessibility"
            accessibilityButton.alpha = 1.0f
        }
        
        // Update permission button
        if (arePermissionsGranted) {
            permissionButton.text = "Permissions ✓"
            permissionButton.alpha = 0.7f
        } else {
            permissionButton.text = "Grant Permissions"
            permissionButton.alpha = 1.0f
        }
    }

    /**
     * Add a transcript entry to the conversation view
     */
    private fun addTranscriptEntry(text: String, isUser: Boolean) {
        // Run on UI thread
        runOnUiThread {
            val prefix = if (isUser) "🎤 You: " else "🤖 Assistant: "
            val color = if (isUser) {
                ContextCompat.getColor(this, android.R.color.holo_blue_dark)
            } else {
                ContextCompat.getColor(this, android.R.color.holo_green_dark)
            }

            // Create spannable text with color
            val spannable = SpannableStringBuilder()
            val startIndex = spannable.length
            spannable.append(prefix)
            spannable.append(text)
            spannable.append("\n\n")
            
            spannable.setSpan(
                ForegroundColorSpan(color),
                startIndex,
                startIndex + prefix.length + text.length,
                SpannableStringBuilder.SPAN_EXCLUSIVE_EXCLUSIVE
            )

            // Append to transcript
            transcriptText.append(spannable)

            // Auto-scroll to bottom
            transcriptScroll.post {
                transcriptScroll.fullScroll(ScrollView.FOCUS_DOWN)
            }
        }
    }

    /**
     * Clear the transcript
     */
    private fun clearTranscript() {
        transcriptText.text = ""
        addTranscriptEntry("Transcript cleared.", false)
    }
}
