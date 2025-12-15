package com.voiceassistant.base.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.core.app.NotificationCompat
import com.voiceassistant.base.R
import com.voiceassistant.base.command.CommandProcessor

/**
 * Background service that continuously listens for wake word
 * and processes voice commands
 */
class VoiceAssistantService : Service() {

    private var speechRecognizer: SpeechRecognizer? = null
    private lateinit var commandProcessor: CommandProcessor
    private var isListening = false
    private var isAwake = false

    companion object {
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "voice_assistant_channel"
        const val WAKE_WORD = "hey assistant"
        var isServiceRunning = false
    }

    override fun onCreate() {
        super.onCreate()
        isServiceRunning = true
        commandProcessor = CommandProcessor(this)
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, createNotification("Listening for wake word..."))
        initializeSpeechRecognizer()
        startListening()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Voice Assistant Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Voice assistant background service"
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(content: String): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Voice Assistant")
            .setContentText(content)
            .setSmallIcon(R.drawable.ic_notification)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .build()
    }

    private fun updateNotification(content: String) {
        val notification = createNotification(content)
        val manager = getSystemService(NotificationManager::class.java)
        manager.notify(NOTIFICATION_ID, notification)
    }

    private fun initializeSpeechRecognizer() {
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
                    // Restart listening after error
                    android.os.Handler(mainLooper).postDelayed({
                        if (isServiceRunning) {
                            startListening()
                        }
                    }, 1000)
                }

                override fun onResults(results: android.os.Bundle?) {
                    results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.let { matches ->
                        processVoiceInput(matches)
                    }
                    // Restart listening
                    android.os.Handler(mainLooper).postDelayed({
                        if (isServiceRunning) {
                            startListening()
                        }
                    }, 500)
                }

                override fun onPartialResults(partialResults: android.os.Bundle?) {}

                override fun onEvent(eventType: Int, params: android.os.Bundle?) {}
            })
        }
    }

    private fun startListening() {
        if (!isListening && speechRecognizer != null) {
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 5)
            }
            try {
                speechRecognizer?.startListening(intent)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun processVoiceInput(matches: List<String>) {
        val input = matches.firstOrNull()?.lowercase() ?: return

        if (!isAwake) {
            // Check for wake word
            if (input.contains(WAKE_WORD)) {
                isAwake = true
                updateNotification("Awake - Listening for command...")
            }
        } else {
            // Process command
            updateNotification("Processing command...")
            commandProcessor.processCommand(input) { success ->
                if (success) {
                    updateNotification("Command executed - Sleeping...")
                } else {
                    updateNotification("Command failed - Sleeping...")
                }
                // Return to sleep mode
                android.os.Handler(mainLooper).postDelayed({
                    isAwake = false
                    updateNotification("Listening for wake word...")
                }, 2000)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        isServiceRunning = false
        isListening = false
        speechRecognizer?.destroy()
        speechRecognizer = null
    }
}
