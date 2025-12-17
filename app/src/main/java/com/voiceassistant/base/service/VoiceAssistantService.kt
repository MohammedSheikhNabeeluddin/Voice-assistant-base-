package com.voiceassistant.base.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.core.app.NotificationCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.voiceassistant.base.MainActivity
import com.voiceassistant.base.R
import com.voiceassistant.base.command.CommandProcessor

/**
 * Background foreground service that continuously listens for wake word
 * and processes voice commands. Works on lock screen and in background.
 */
class VoiceAssistantService : Service() {

    private var speechRecognizer: SpeechRecognizer? = null
    private lateinit var commandProcessor: CommandProcessor
    private var isListening = false
    private var isAwake = false
    private val handler = android.os.Handler(android.os.Looper.getMainLooper())
    private var wakeLock: PowerManager.WakeLock? = null

    companion object {
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "voice_assistant_channel"
        const val WAKE_WORD = "hey assistant"
        var isServiceRunning = false
        
        // Broadcast actions for live transcript
        const val ACTION_TRANSCRIPT_UPDATE = "com.voiceassistant.base.TRANSCRIPT_UPDATE"
        const val ACTION_STATUS_UPDATE = "com.voiceassistant.base.STATUS_UPDATE"
        const val EXTRA_TRANSCRIPT_TEXT = "transcript_text"
        const val EXTRA_IS_USER = "is_user"
    }

    override fun onCreate() {
        super.onCreate()
        isServiceRunning = true
        commandProcessor = CommandProcessor(this)
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, createNotification("Listening for wake word..."))
        acquireWakeLock()
        initializeSpeechRecognizer()
        startListening()
        broadcastStatusUpdate()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }
    
    /**
     * Acquire partial wake lock to keep service running on lock screen
     * Using 3 minutes timeout, renewed as needed
     */
    private fun acquireWakeLock() {
        val powerManager = getSystemService(POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "VoiceAssistant:WakeLock"
        ).apply {
            acquire(3 * 60 * 1000L) // 3 minutes, renewed during listening
        }
    }
    
    /**
     * Release wake lock
     */
    private fun releaseWakeLock() {
        wakeLock?.let {
            if (it.isHeld) {
                it.release()
            }
        }
        wakeLock = null
    }
    
    /**
     * Renew wake lock to keep service running
     */
    private fun renewWakeLock() {
        wakeLock?.let {
            if (it.isHeld) {
                // Wake lock already active, no need to renew
                return
            }
        }
        // Re-acquire if not held
        acquireWakeLock()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Voice Assistant Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Voice assistant background service"
                setShowBadge(false)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(content: String): Notification {
        // Create intent to open MainActivity when notification is tapped
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Voice Assistant")
            .setContentText(content)
            .setSmallIcon(R.drawable.ic_notification)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .setContentIntent(pendingIntent)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .build()
    }

    private fun updateNotification(content: String) {
        val notification = createNotification(content)
        val manager = getSystemService(NotificationManager::class.java)
        manager.notify(NOTIFICATION_ID, notification)
    }

    private fun initializeSpeechRecognizer() {
        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            broadcastTranscript("Speech recognition not available on this device", false)
            return
        }
        
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this).apply {
            setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: android.os.Bundle?) {
                    isListening = true
                }

                override fun onBeginningOfSpeech() {}

                override fun onRmsChanged(rmsdB: Float) {}

                override fun onBufferReceived(buffer: ByteArray?) {}

                override fun onEndOfSpeech() {
                    isListening = false
                }

                override fun onError(error: Int) {
                    isListening = false
                    val errorMessage = getErrorMessage(error)
                    
                    // Restart listening after error with appropriate delay
                    val delayMs = when (error) {
                        SpeechRecognizer.ERROR_NO_MATCH,
                        SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> 500L
                        SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> 2000L
                        else -> 1000L
                    }
                    
                    handler.postDelayed({
                        if (isServiceRunning) {
                            startListening()
                        }
                    }, delayMs)
                }

                override fun onResults(results: android.os.Bundle?) {
                    results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.let { matches ->
                        processVoiceInput(matches)
                    }
                    // Restart listening
                    handler.postDelayed({
                        if (isServiceRunning) {
                            startListening()
                        }
                    }, 500)
                }

                override fun onPartialResults(partialResults: android.os.Bundle?) {
                    // Handle partial results for better responsiveness
                    partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.let { matches ->
                        val input = matches.firstOrNull()?.lowercase() ?: return
                        // Check for wake word in partial results for faster response
                        if (!isAwake && input.contains(WAKE_WORD)) {
                            isAwake = true
                            updateNotification("Awake - Listening for command...")
                            broadcastTranscript("Wake word detected", false)
                        }
                    }
                }

                override fun onEvent(eventType: Int, params: android.os.Bundle?) {}
            })
        }
    }
    
    private fun getErrorMessage(errorCode: Int): String {
        return when (errorCode) {
            SpeechRecognizer.ERROR_AUDIO -> "Audio recording error"
            SpeechRecognizer.ERROR_CLIENT -> "Client error"
            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Insufficient permissions"
            SpeechRecognizer.ERROR_NETWORK -> "Network error"
            SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network timeout"
            SpeechRecognizer.ERROR_NO_MATCH -> "No match found"
            SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Recognizer busy"
            SpeechRecognizer.ERROR_SERVER -> "Server error"
            SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "Speech timeout"
            else -> "Unknown error"
        }
    }

    private fun startListening() {
        if (!isListening && speechRecognizer != null) {
            // Renew wake lock while actively listening
            renewWakeLock()
            
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 5)
                putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 3000L)
                putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, 1500L)
            }
            try {
                speechRecognizer?.startListening(intent)
            } catch (e: Exception) {
                e.printStackTrace()
                // Retry after delay
                handler.postDelayed({
                    if (isServiceRunning) {
                        startListening()
                    }
                }, 2000)
            }
        }
    }

    private fun processVoiceInput(matches: List<String>) {
        val input = matches.firstOrNull()?.lowercase() ?: return
        
        // Broadcast user input to transcript
        broadcastTranscript(matches.firstOrNull() ?: input, true)

        if (!isAwake) {
            // Check for wake word
            if (input.contains(WAKE_WORD)) {
                isAwake = true
                updateNotification("Awake - Listening for command...")
                broadcastTranscript("Ready for your command...", false)
            }
        } else {
            // Process command
            updateNotification("Processing command...")
            
            // Extract command (remove wake word if present)
            val command = input.replace(WAKE_WORD, "").trim()
            
            if (command.isNotEmpty()) {
                commandProcessor.processCommand(command) { success ->
                    val response = if (success) {
                        "Command executed successfully"
                    } else {
                        "Command not recognized"
                    }
                    broadcastTranscript(response, false)
                    
                    if (success) {
                        updateNotification("Command executed - Sleeping...")
                    } else {
                        updateNotification("Command failed - Sleeping...")
                    }
                    
                    // Return to sleep mode
                    handler.postDelayed({
                        isAwake = false
                        updateNotification("Listening for wake word...")
                    }, 2000)
                }
            } else {
                // Only wake word was detected, wait for actual command
                broadcastTranscript("Listening for your command...", false)
            }
        }
    }
    
    /**
     * Broadcast transcript update to MainActivity
     */
    private fun broadcastTranscript(text: String, isUser: Boolean) {
        val intent = Intent(ACTION_TRANSCRIPT_UPDATE).apply {
            putExtra(EXTRA_TRANSCRIPT_TEXT, text)
            putExtra(EXTRA_IS_USER, isUser)
        }
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent)
    }
    
    /**
     * Broadcast status update to MainActivity
     */
    private fun broadcastStatusUpdate() {
        val intent = Intent(ACTION_STATUS_UPDATE)
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent)
    }

    override fun onDestroy() {
        super.onDestroy()
        isServiceRunning = false
        isListening = false
        releaseWakeLock()
        handler.removeCallbacksAndMessages(null)
        speechRecognizer?.destroy()
        speechRecognizer = null
        broadcastStatusUpdate()
    }
}
