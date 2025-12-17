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
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.voiceassistant.base.service.VoiceAccessibilityService
import com.voiceassistant.base.service.VoiceAssistantService

/**
 * Main activity for the Voice Assistant
 * Handles permission requests, service initialization, permission dashboard, and live transcript
 */
class MainActivity : AppCompatActivity() {

    private lateinit var statusText: TextView
    private lateinit var startButton: Button
    private lateinit var stopButton: Button
    
    // Permission Dashboard
    private lateinit var micPermissionStatus: TextView
    private lateinit var overlayPermissionStatus: TextView
    private lateinit var accessibilityPermissionStatus: TextView
    private lateinit var notificationPermissionStatus: TextView
    private lateinit var requestMicPermission: Button
    private lateinit var requestOverlayPermission: Button
    private lateinit var requestAccessibilityPermission: Button
    private lateinit var requestNotificationPermission: Button
    private lateinit var notificationPermissionRow: LinearLayout
    
    // Live Transcript
    private lateinit var transcriptText: TextView
    private lateinit var transcriptScrollView: ScrollView
    private lateinit var clearTranscriptButton: Button
    
    private val transcriptBuilder = SpannableStringBuilder()

    private val PERMISSION_REQUEST_CODE = 123
    private val OVERLAY_PERMISSION_REQUEST_CODE = 124
    private val MIC_PERMISSION_REQUEST_CODE = 125
    private val NOTIFICATION_PERMISSION_REQUEST_CODE = 126
    
    // Debounce handler to prevent double-tap issues
    private val debounceHandler = Handler(Looper.getMainLooper())
    private var isStartButtonEnabled = true
    private val DEBOUNCE_DELAY_MS = 1000L
    
    // Broadcast receiver for transcript updates
    private val transcriptReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                VoiceAssistantService.ACTION_TRANSCRIPT_UPDATE -> {
                    val text = intent.getStringExtra(VoiceAssistantService.EXTRA_TRANSCRIPT_TEXT) ?: return
                    val isUser = intent.getBooleanExtra(VoiceAssistantService.EXTRA_IS_USER, true)
                    addTranscriptEntry(text, isUser)
                }
                VoiceAssistantService.ACTION_STATUS_UPDATE -> {
                    updateStatus()
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initializeViews()
        setupClickListeners()
        updatePermissionDashboard()
        updateStatus()
        
        // Register for transcript updates
        val filter = IntentFilter().apply {
            addAction(VoiceAssistantService.ACTION_TRANSCRIPT_UPDATE)
            addAction(VoiceAssistantService.ACTION_STATUS_UPDATE)
        }
        LocalBroadcastManager.getInstance(this).registerReceiver(transcriptReceiver, filter)
    }
    
    private fun initializeViews() {
        statusText = findViewById(R.id.statusText)
        startButton = findViewById(R.id.startButton)
        stopButton = findViewById(R.id.stopButton)
        
        // Permission Dashboard
        micPermissionStatus = findViewById(R.id.micPermissionStatus)
        overlayPermissionStatus = findViewById(R.id.overlayPermissionStatus)
        accessibilityPermissionStatus = findViewById(R.id.accessibilityPermissionStatus)
        notificationPermissionStatus = findViewById(R.id.notificationPermissionStatus)
        requestMicPermission = findViewById(R.id.requestMicPermission)
        requestOverlayPermission = findViewById(R.id.requestOverlayPermission)
        requestAccessibilityPermission = findViewById(R.id.requestAccessibilityPermission)
        requestNotificationPermission = findViewById(R.id.requestNotificationPermission)
        notificationPermissionRow = findViewById(R.id.notificationPermissionRow)
        
        // Live Transcript
        transcriptText = findViewById(R.id.transcriptText)
        transcriptScrollView = findViewById(R.id.transcriptScrollView)
        clearTranscriptButton = findViewById(R.id.clearTranscriptButton)
        
        // Show notification permission row only on Android 13+
        notificationPermissionRow.visibility = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            View.VISIBLE
        } else {
            View.GONE
        }
    }
    
    private fun setupClickListeners() {
        // Start button with debouncing to fix single-tap bug
        startButton.setOnClickListener {
            if (!isStartButtonEnabled) return@setOnClickListener
            
            // Disable button temporarily to prevent double-tap
            isStartButtonEnabled = false
            startButton.isEnabled = false
            
            if (checkAllPermissionsGranted()) {
                startVoiceAssistant()
            } else {
                requestMissingPermissions()
            }
            
            // Re-enable after debounce delay
            debounceHandler.postDelayed({
                isStartButtonEnabled = true
                updateStatus()
            }, DEBOUNCE_DELAY_MS)
        }

        stopButton.setOnClickListener {
            stopVoiceAssistant()
        }
        
        // Permission Dashboard buttons
        requestMicPermission.setOnClickListener {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.RECORD_AUDIO),
                MIC_PERMISSION_REQUEST_CODE
            )
        }
        
        requestOverlayPermission.setOnClickListener {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")
            )
            startActivityForResult(intent, OVERLAY_PERMISSION_REQUEST_CODE)
        }
        
        requestAccessibilityPermission.setOnClickListener {
            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
            startActivity(intent)
            Toast.makeText(this, "Please enable Voice Assistant Base accessibility service", Toast.LENGTH_LONG).show()
        }
        
        requestNotificationPermission.setOnClickListener {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    NOTIFICATION_PERMISSION_REQUEST_CODE
                )
            }
        }
        
        clearTranscriptButton.setOnClickListener {
            clearTranscript()
        }
    }
    
    private fun updatePermissionDashboard() {
        // Microphone permission
        val hasMicPermission = ContextCompat.checkSelfPermission(
            this, Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
        updatePermissionStatus(micPermissionStatus, requestMicPermission, hasMicPermission)
        
        // Overlay permission
        val hasOverlayPermission = Settings.canDrawOverlays(this)
        updatePermissionStatus(overlayPermissionStatus, requestOverlayPermission, hasOverlayPermission)
        
        // Accessibility service
        val hasAccessibilityService = isAccessibilityServiceEnabled()
        updatePermissionStatus(accessibilityPermissionStatus, requestAccessibilityPermission, hasAccessibilityService)
        
        // Notification permission (Android 13+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val hasNotificationPermission = ContextCompat.checkSelfPermission(
                this, Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            updatePermissionStatus(notificationPermissionStatus, requestNotificationPermission, hasNotificationPermission)
        }
    }
    
    private fun updatePermissionStatus(statusView: TextView, grantButton: Button, isGranted: Boolean) {
        if (isGranted) {
            statusView.text = "✓"
            statusView.setTextColor(ContextCompat.getColor(this, android.R.color.holo_green_light))
            grantButton.visibility = View.GONE
        } else {
            statusView.text = "✗"
            statusView.setTextColor(ContextCompat.getColor(this, android.R.color.holo_red_light))
            grantButton.visibility = View.VISIBLE
        }
    }
    
    private fun isAccessibilityServiceEnabled(): Boolean {
        return VoiceAccessibilityService.instance != null
    }
    
    private fun checkAllPermissionsGranted(): Boolean {
        val hasMic = ContextCompat.checkSelfPermission(
            this, Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
        
        val hasOverlay = Settings.canDrawOverlays(this)
        
        val hasNotification = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                this, Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
        
        return hasMic && hasOverlay && hasNotification
    }
    
    private fun requestMissingPermissions() {
        val permissions = mutableListOf<String>()
        
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) 
            != PackageManager.PERMISSION_GRANTED) {
            permissions.add(Manifest.permission.RECORD_AUDIO)
        }
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) 
                != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
        
        // Add other permissions
        listOf(
            Manifest.permission.CALL_PHONE,
            Manifest.permission.READ_CONTACTS,
            Manifest.permission.SEND_SMS
        ).forEach { permission ->
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                permissions.add(permission)
            }
        }
        
        if (permissions.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, permissions.toTypedArray(), PERMISSION_REQUEST_CODE)
        } else if (!Settings.canDrawOverlays(this)) {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")
            )
            startActivityForResult(intent, OVERLAY_PERMISSION_REQUEST_CODE)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        
        updatePermissionDashboard()
        
        when (requestCode) {
            PERMISSION_REQUEST_CODE -> {
                if (grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                    Toast.makeText(this, "Permissions granted", Toast.LENGTH_SHORT).show()
                    if (Settings.canDrawOverlays(this)) {
                        startVoiceAssistant()
                    } else {
                        val intent = Intent(
                            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                            Uri.parse("package:$packageName")
                        )
                        startActivityForResult(intent, OVERLAY_PERMISSION_REQUEST_CODE)
                    }
                } else {
                    Toast.makeText(this, "Some permissions were denied", Toast.LENGTH_LONG).show()
                }
            }
            MIC_PERMISSION_REQUEST_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(this, "Microphone permission granted", Toast.LENGTH_SHORT).show()
                }
            }
            NOTIFICATION_PERMISSION_REQUEST_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(this, "Notification permission granted", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        
        updatePermissionDashboard()
        
        if (requestCode == OVERLAY_PERMISSION_REQUEST_CODE) {
            if (Settings.canDrawOverlays(this)) {
                Toast.makeText(this, "Overlay permission granted", Toast.LENGTH_SHORT).show()
                if (checkAllPermissionsGranted()) {
                    startVoiceAssistant()
                }
            } else {
                Toast.makeText(this, "Overlay permission required for background operation", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun startVoiceAssistant() {
        val intent = Intent(this, VoiceAssistantService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
        updateStatus()
        Toast.makeText(this, "Voice Assistant Started - Say wake word to activate", Toast.LENGTH_LONG).show()
    }

    private fun stopVoiceAssistant() {
        val intent = Intent(this, VoiceAssistantService::class.java)
        stopService(intent)
        updateStatus()
        Toast.makeText(this, "Voice Assistant Stopped", Toast.LENGTH_SHORT).show()
    }

    private fun updateStatus() {
        val isRunning = VoiceAssistantService.isServiceRunning
        statusText.text = if (isRunning) {
            "Status: Running\nListening for wake word..."
        } else {
            "Status: Stopped"
        }
        startButton.isEnabled = !isRunning && isStartButtonEnabled
        stopButton.isEnabled = isRunning
    }
    
    private fun addTranscriptEntry(text: String, isUser: Boolean) {
        val label = if (isUser) getString(R.string.user_label) else getString(R.string.assistant_label)
        val color = if (isUser) {
            ContextCompat.getColor(this, android.R.color.holo_blue_dark)
        } else {
            ContextCompat.getColor(this, android.R.color.holo_green_dark)
        }
        
        // Clear placeholder if this is the first entry
        if (transcriptBuilder.isEmpty()) {
            transcriptText.text = ""
        }
        
        val entry = SpannableStringBuilder()
        val labelSpan = SpannableStringBuilder(label)
        labelSpan.setSpan(
            ForegroundColorSpan(color),
            0, label.length,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        entry.append(labelSpan)
        entry.append(text)
        entry.append("\n")
        
        transcriptBuilder.append(entry)
        transcriptText.text = transcriptBuilder
        
        // Auto-scroll to bottom
        transcriptScrollView.post {
            transcriptScrollView.fullScroll(View.FOCUS_DOWN)
        }
    }
    
    private fun clearTranscript() {
        transcriptBuilder.clear()
        transcriptText.text = getString(R.string.transcript_placeholder)
    }

    override fun onResume() {
        super.onResume()
        updatePermissionDashboard()
        updateStatus()
    }
    
    override fun onDestroy() {
        super.onDestroy()
        LocalBroadcastManager.getInstance(this).unregisterReceiver(transcriptReceiver)
        debounceHandler.removeCallbacksAndMessages(null)
    }
}
