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
import android.provider.Settings
import android.text.SpannableStringBuilder
import android.text.style.ForegroundColorSpan
import android.widget.Button
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.voiceassistant.base.service.VoiceAccessibilityService
import com.voiceassistant.base.service.VoiceAssistantService

class MainActivity : AppCompatActivity() {

    private lateinit var statusText: TextView
    private lateinit var transcriptText: TextView
    private lateinit var transcriptScroll: ScrollView
    private lateinit var startButton: Button
    private lateinit var stopButton: Button
    private lateinit var permissionButton: Button
    private lateinit var accessibilityButton: Button

    private val PERMISSION_REQUEST_CODE = 100
    
    private val requiredPermissions = arrayOf(
        Manifest.permission.RECORD_AUDIO,
        Manifest.permission.CALL_PHONE,
        Manifest.permission.SEND_SMS,
        Manifest.permission.READ_CONTACTS,
        Manifest.permission.CAMERA
    )

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
        updateStatus()
        
        val filter = IntentFilter().apply {
            addAction(VoiceAssistantService.ACTION_TRANSCRIPT_UPDATE)
            addAction(VoiceAssistantService.ACTION_STATUS_UPDATE)
        }
        LocalBroadcastManager.getInstance(this).registerReceiver(transcriptReceiver, filter)
    }

    override fun onDestroy() {
        super.onDestroy()
        LocalBroadcastManager.getInstance(this).unregisterReceiver(transcriptReceiver)
    }

    override fun onResume() {
        super.onResume()
        updateStatus()
    }

    private fun initializeViews() {
        statusText = findViewById(R.id.statusText)
        transcriptText = findViewById(R.id.transcriptText)
        transcriptScroll = findViewById(R.id.transcriptScroll)
        startButton = findViewById(R.id.startButton)
        stopButton = findViewById(R.id.stopButton)
        permissionButton = findViewById(R.id.permissionButton)
        accessibilityButton = findViewById(R.id.accessibilityButton)
    }

    private fun setupClickListeners() {
        startButton.setOnClickListener {
            if (checkAllPermissions()) {
                startVoiceService()
            } else {
                requestPermissions()
            }
        }

        stopButton.setOnClickListener {
            stopVoiceService()
        }

        permissionButton.setOnClickListener {
            requestPermissions()
        }

        accessibilityButton.setOnClickListener {
            openAccessibilitySettings()
        }
    }

    private fun checkAllPermissions(): Boolean {
        for (permission in requiredPermissions) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                return false
            }
        }
        return true
    }

    private fun requestPermissions() {
        val permissionsToRequest = requiredPermissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }.toTypedArray()

        if (permissionsToRequest.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, permissionsToRequest, PERMISSION_REQUEST_CODE)
        }

        // Request overlay permission for Android M+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")
            )
            startActivity(intent)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_CODE) {
            updateStatus()
            if (checkAllPermissions()) {
                Toast.makeText(this, "All permissions granted!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun isAccessibilityServiceEnabled(): Boolean {
        return VoiceAccessibilityService.instance != null
    }

    private fun openAccessibilitySettings() {
        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
        startActivity(intent)
        Toast.makeText(
            this,
            "Please find 'Voice Assistant' and enable it",
            Toast.LENGTH_LONG
        ).show()
    }

    private fun startVoiceService() {
        if (!isAccessibilityServiceEnabled()) {
            Toast.makeText(
                this,
                "Please enable accessibility service first",
                Toast.LENGTH_LONG
            ).show()
            openAccessibilitySettings()
            return
        }

        val serviceIntent = Intent(this, VoiceAssistantService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent)
        } else {
            startService(serviceIntent)
        }
        
        updateStatus()
        Toast.makeText(this, "Voice Assistant started", Toast.LENGTH_SHORT).show()
    }

    private fun stopVoiceService() {
        val serviceIntent = Intent(this, VoiceAssistantService::class.java)
        stopService(serviceIntent)
        updateStatus()
        Toast.makeText(this, "Voice Assistant stopped", Toast.LENGTH_SHORT).show()
    }

    private fun updateStatus() {
        val isRunning = VoiceAssistantService.isServiceRunning
        val accessibilityEnabled = isAccessibilityServiceEnabled()
        val permissionsGranted = checkAllPermissions()

        val statusBuilder = StringBuilder()
        statusBuilder.append("Service: ${if (isRunning) "Running ✓" else "Stopped ✗"}\n")
        statusBuilder.append("Accessibility: ${if (accessibilityEnabled) "Enabled ✓" else "Disabled ✗"}\n")
        statusBuilder.append("Permissions: ${if (permissionsGranted) "Granted ✓" else "Missing ✗"}")

        statusText.text = statusBuilder.toString()

        startButton.isEnabled = !isRunning
        stopButton.isEnabled = isRunning
        accessibilityButton.isEnabled = !accessibilityEnabled
    }

    private fun addTranscriptEntry(text: String, isUser: Boolean) {
        val prefix = if (isUser) "You: " else "Assistant: "
        val color = if (isUser) {
            ContextCompat.getColor(this, android.R.color.holo_blue_dark)
        } else {
            ContextCompat.getColor(this, android.R.color.holo_green_dark)
        }

        val spannable = SpannableStringBuilder()
        spannable.append(prefix)
        spannable.append(text)
        spannable.append("\n\n")
        spannable.setSpan(
            ForegroundColorSpan(color),
            0,
            prefix.length + text.length,
            SpannableStringBuilder.SPAN_EXCLUSIVE_EXCLUSIVE
        )

        transcriptText.append(spannable)

        // Auto-scroll to bottom
        transcriptScroll.post {
            transcriptScroll.fullScroll(ScrollView.FOCUS_DOWN)
        }
    }
}
