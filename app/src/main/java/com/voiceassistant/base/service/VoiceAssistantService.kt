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
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.voiceassistant.base.MainActivity
import com.voiceassistant.base.R
import com.voiceassistant.base.command.CommandProcessor
import java.util.Locale

/**
 * Background foreground service that continuously listens for wake word
 * and processes voice commands. Works on lock screen and in background.
 * 
 * Flow:
 * 1. Service starts and begins listening for wake word ("hey assistant")
 * 2. When wake word is detected, service becomes "awake" and waits for command
 * 3. Command is passed to CommandProcessor for execution
 * 4. Response is provided via TTS and transcript
 * 5. Service returns to listening for wake word
 */
class VoiceAssistantService : Service() {

    private var speechRecognizer: SpeechRecognizer? = null
    private lateinit var commandProcessor: CommandProcessor
    private var textToSpeech: TextToSpeech? = null
    private var isTtsReady = false
    private var isListening = false
    private var isAwake = false
    private var isSpeaking = false
    private val handler = android.os.Handler(android.os.Looper.getMainLooper())
    private var wakeLock: PowerManager.WakeLock? = null

    companion object {
        private const val TAG = "VoiceAssistantService"
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
        Log.i(TAG, "onCreate: Starting Voice Assistant Service")
        isServiceRunning = true
        commandProcessor = CommandProcessor(this)
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, createNotification("Listening for wake word..."))
        acquireWakeLock()
        initializeTextToSpeech()
        initializeSpeechRecognizer()
        startListening()
        broadcastStatusUpdate()
        Log.i(TAG, "onCreate: Voice Assistant Service initialized successfully")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "onStartCommand: Service start requested")
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
     * Initialize Text-to-Speech engine
     */
    private fun initializeTextToSpeech() {
        textToSpeech = TextToSpeech(this) { status ->
            if (status == TextToSpeech.SUCCESS) {
                val result = textToSpeech?.setLanguage(Locale.US)
                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    broadcastTranscript("Text-to-Speech language not supported", false)
                    isTtsReady = false
                } else {
                    isTtsReady = true
                    // Set TTS parameters for better responsiveness
                    textToSpeech?.setSpeechRate(1.0f)
                    textToSpeech?.setPitch(1.0f)
                    
                    // Set utterance listener to know when speaking is done
                    textToSpeech?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                        override fun onStart(utteranceId: String?) {
                            isSpeaking = true
                        }

                        override fun onDone(utteranceId: String?) {
                            isSpeaking = false
                        }

                        override fun onError(utteranceId: String?) {
                            isSpeaking = false
                        }
                    })
                }
            } else {
                broadcastTranscript("Text-to-Speech initialization failed", false)
                isTtsReady = false
            }
        }
    }
    
    /**
     * Speak response using Text-to-Speech
     */
    private fun reply(message: String) {
        if (isTtsReady && textToSpeech != null) {
            // Stop any ongoing speech
            textToSpeech?.stop()
            
            // Speak the message
            val params = android.os.Bundle()
            params.putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "assistantReply")
            textToSpeech?.speak(message, TextToSpeech.QUEUE_FLUSH, params, "assistantReply")
        }
        
        // Always broadcast transcript even if TTS fails
        broadcastTranscript(message, false)
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
        Log.d(TAG, "initializeSpeechRecognizer: Initializing speech recognizer")
        
        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            Log.e(TAG, "initializeSpeechRecognizer: Speech recognition not available on this device")
            broadcastTranscript("Speech recognition not available on this device", false)
            return
        }
        
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this).apply {
            setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: android.os.Bundle?) {
                    Log.d(TAG, "RecognitionListener: Ready for speech")
                    isListening = true
                }

                override fun onBeginningOfSpeech() {
                    Log.d(TAG, "RecognitionListener: Beginning of speech detected")
                }

                override fun onRmsChanged(rmsdB: Float) {}

                override fun onBufferReceived(buffer: ByteArray?) {}

                override fun onEndOfSpeech() {
                    Log.d(TAG, "RecognitionListener: End of speech")
                    isListening = false
                }

                override fun onError(error: Int) {
                    isListening = false
                    val errorMessage = getErrorMessage(error)
                    Log.w(TAG, "RecognitionListener: Error occurred - $errorMessage (code=$error)")
                    
                    // Restart listening after error with appropriate delay
                    val delayMs = when (error) {
                        SpeechRecognizer.ERROR_NO_MATCH,
                        SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> 500L
                        SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> 2000L
                        else -> 1000L
                    }
                    
                    Log.d(TAG, "RecognitionListener: Will restart listening after ${delayMs}ms")
                    handler.postDelayed({
                        if (isServiceRunning) {
                            startListening()
                        }
                    }, delayMs)
                }

                override fun onResults(results: android.os.Bundle?) {
                    Log.d(TAG, "RecognitionListener: Results received")
                    results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.let { matches ->
                        Log.d(TAG, "RecognitionListener: Matches count=${matches.size}")
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
                        Log.v(TAG, "RecognitionListener: Partial result - '$input'")
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
        Log.d(TAG, "startListening: isListening=$isListening, speechRecognizer=${speechRecognizer != null}")
        
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
                Log.d(TAG, "startListening: Speech recognizer started successfully")
            } catch (e: Exception) {
                Log.e(TAG, "startListening: Error starting speech recognizer", e)
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
        Log.i(TAG, "processVoiceInput: Received input: '$input'")
        Log.d(TAG, "processVoiceInput: isAwake=$isAwake, wake_word=$WAKE_WORD")
        
        // Broadcast user input to transcript
        broadcastTranscript(matches.firstOrNull() ?: input, true)

        if (!isAwake) {
            // Check for wake word
            val wakeWordIndex = input.indexOf(WAKE_WORD)
            if (wakeWordIndex >= 0) {
                Log.i(TAG, "processVoiceInput: Wake word detected!")
                isAwake = true
                updateNotification("Awake - Listening for command...")
                
                // Extract command after the wake word (e.g., "Hey Assistant open Chrome" -> "open Chrome")
                val command = input.substring(wakeWordIndex + WAKE_WORD.length).trim()
                
                // Check if command was spoken together with wake word
                if (command.isNotEmpty()) {
                    Log.i(TAG, "processVoiceInput: Command detected with wake word: '$command'")
                    // Process the command immediately
                    executeCommand(command)
                } else {
                    // Only wake word was detected, wait for actual command
                    Log.d(TAG, "processVoiceInput: Wake word only, waiting for command")
                    reply("Yes?")
                }
            } else {
                Log.d(TAG, "processVoiceInput: No wake word in input, continuing to listen")
            }
        } else {
            // Already awake, process command (the input IS the command)
            Log.i(TAG, "processVoiceInput: Processing command (already awake)")
            
            // When already awake, the input is the command itself (no wake word needed)
            val command = input.trim()
            
            if (command.isNotEmpty()) {
                executeCommand(command)
            } else {
                // Empty command, continue listening
                Log.d(TAG, "processVoiceInput: Empty command, waiting for command")
                reply("Listening for your command...")
            }
        }
    }
    
    /**
     * Execute a voice command through the CommandProcessor
     */
    private fun executeCommand(command: String) {
        Log.i(TAG, "executeCommand: Sending command to processor: '$command'")
        updateNotification("Processing command...")
        
        commandProcessor.processCommand(command) { success, responseMessage ->
            Log.i(TAG, "executeCommand: Command result - success=$success, response='$responseMessage'")
            // Use the reply function to provide verbal and text feedback
            reply(responseMessage)
            
            if (success) {
                Log.i(TAG, "executeCommand: Command executed successfully")
                updateNotification("Command executed - Sleeping...")
            } else {
                Log.w(TAG, "executeCommand: Command execution failed")
                updateNotification("Command failed - Sleeping...")
            }
            
            // Return to sleep mode after a delay to allow TTS to complete
            // Use a minimum of 2 seconds, plus extra time if message is long
            val delayMs = 2000L + (responseMessage.length * 50L).coerceAtMost(3000L)
            Log.d(TAG, "executeCommand: Returning to sleep in ${delayMs}ms")
            handler.postDelayed({
                isAwake = false
                Log.d(TAG, "executeCommand: Now sleeping, listening for wake word")
                updateNotification("Listening for wake word...")
            }, delayMs)
        }
    }
    
    /**
     * Broadcast transcript update to MainActivity
     */
    private fun broadcastTranscript(text: String, isUser: Boolean) {
        Log.d(TAG, "broadcastTranscript: ${if (isUser) "USER" else "ASSISTANT"}: $text")
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
        Log.d(TAG, "broadcastStatusUpdate: Sending status update")
        val intent = Intent(ACTION_STATUS_UPDATE)
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent)
    }

    override fun onDestroy() {
        Log.i(TAG, "onDestroy: Stopping Voice Assistant Service")
        super.onDestroy()
        isServiceRunning = false
        isListening = false
        releaseWakeLock()
        handler.removeCallbacksAndMessages(null)
        speechRecognizer?.destroy()
        speechRecognizer = null
        
        // Cleanup Text-to-Speech
        textToSpeech?.stop()
        textToSpeech?.shutdown()
        textToSpeech = null
        
        broadcastStatusUpdate()
    }
}
