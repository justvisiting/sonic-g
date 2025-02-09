package com.example.speechapp;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.util.Log;
import android.media.AudioManager;

public class VoiceManager implements RecognitionListener {
    private static final String TAG = "VoiceManager";
    private static final int MAX_RETRIES = 3;
    private final Context context;
    private final VoiceCallback callback;
    private SpeechRecognizer speechRecognizer;
    private final Handler handler;
    private boolean isListening = false;
    private String sessionPartialText = "";
    private String lastPartialResult = "";
    private final String[] supportedLanguages = {"en-US", "hi-IN", "kn-IN", "te-IN"};
    private int retryCount = 0;
    private AudioManager audioManager;

    public interface VoiceCallback {
        void onPartialSpeechResult(String textSoFar, String newText);
        void onSpeechResult(String fullText);
        void onSpeechError(String error);
        void onListeningStarted();
        void onListeningStopped();
        void onLanguageDetected(String language);
        void onStatusChanged(VoiceInputView.VoiceStatus status);
    }

    public VoiceManager(Context context, VoiceCallback callback) {
        this.context = context;
        this.callback = callback;
        this.handler = new Handler(Looper.getMainLooper());
        audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        if (audioManager != null) {
            // Disable system sounds for speech recognition
            audioManager.adjustStreamVolume(AudioManager.STREAM_SYSTEM, AudioManager.ADJUST_MUTE, 0);
        }
    }

    private void initializeSpeechRecognizer() {
        if (speechRecognizer != null) {
            speechRecognizer.destroy();
        }
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context);
        speechRecognizer.setRecognitionListener(this);
    }

    private void startNewRecognition() {
        lastPartialResult = "";
        initializeSpeechRecognizer();

        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, supportedLanguages);
        intent.putExtra(RecognizerIntent.EXTRA_ENABLE_LANGUAGE_SWITCH, supportedLanguages);
        intent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1);
        intent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true);
        intent.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, context.getPackageName());
        intent.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 5000);
        //intent.putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true);

        try {
            speechRecognizer.startListening(intent);
        } catch (Exception e) {
            Log.e(TAG, "Error starting speech recognition", e);
            handleError("Error starting speech recognition", -1, false, true, false);
        }
    }

    public void startListening() {
        if (isListening) return;
        isListening = true;
        sessionPartialText = "";
        retryCount = 0;  // Reset retry count when starting new listening session
        callback.onListeningStarted();
        callback.onStatusChanged(VoiceInputView.VoiceStatus.LISTENING);
        startNewRecognition();
    }

    public void stopListening() {
        if (!isListening) return;
        isListening = false;
        if (speechRecognizer != null) {
            try {
                speechRecognizer.stopListening();  // Try to stop gracefully first
                handler.postDelayed(() -> {
                    if (speechRecognizer != null) {
                        speechRecognizer.destroy();
                        speechRecognizer = null;
                    }
                }, 100);  // Give it a moment to stop gracefully
            } catch (Exception e) {
                Log.e(TAG, "Error stopping speech recognizer", e);
                if (speechRecognizer != null) {
                    speechRecognizer.destroy();
                    speechRecognizer = null;
                }
            }
        }
        if (audioManager != null) {
            audioManager.adjustStreamVolume(AudioManager.STREAM_SYSTEM, AudioManager.ADJUST_UNMUTE, 0);
        }
        callback.onListeningStopped();
        callback.onStatusChanged(VoiceInputView.VoiceStatus.IDLE);
    }

    public void toggleListening() {
        if (isListening) {
            stopListening();
        } else {
            startListening();
        }
    }

    private void handleError(String message, int messageCode, boolean canRestart, boolean shouldReinitSpeechRecognizer, boolean wasSpeechTimeout){ 
        boolean shouldShowErrorToUser = true;

        if (canRestart && isListening && retryCount < MAX_RETRIES) {
            if (!(wasSpeechTimeout || messageCode == SpeechRecognizer.ERROR_SERVER_DISCONNECTED)) {
                retryCount++;
            }
            if (shouldReinitSpeechRecognizer) {
                speechRecognizer.destroy();
                speechRecognizer = null;
                //startNewRecognition();
                handler.postDelayed(this::startNewRecognition, 100);
            }
        } else {
            //if (message.length() > 0) {
                callback.onSpeechError(message);
            //}

            callback.onStatusChanged(VoiceInputView.VoiceStatus.ERROR);
            
            isListening = false;
            callback.onListeningStopped();
            callback.onStatusChanged(VoiceInputView.VoiceStatus.IDLE);
        }
    }

    // Speech Recognition Listener methods
    @Override
    public void onReadyForSpeech(Bundle params) {}

    @Override
    public void onBeginningOfSpeech() {}

    @Override
    public void onRmsChanged(float rmsdB) {
        if (isListening) {
            float normalizedRms = Math.min(1.0f, Math.max(0.0f, rmsdB / 10.0f));
            callback.onStatusChanged(VoiceInputView.VoiceStatus.LISTENING);
        }
    }

    @Override
    public void onLanguageDetection(Bundle results) {
        Log.w(TAG, "onLanguageDetection: " + results);
        String detectedLanguage = results.getString(SpeechRecognizer.DETECTED_LANGUAGE);
        if (detectedLanguage == null) return;

        int confidence = results.getInt(SpeechRecognizer.LANGUAGE_DETECTION_CONFIDENCE_LEVEL, -1);
        if (confidence > SpeechRecognizer.LANGUAGE_DETECTION_CONFIDENCE_LEVEL_HIGHLY_CONFIDENT) {
            if (detectedLanguage.startsWith("hi-")) {
                detectedLanguage = "hinglish";
            } else if (detectedLanguage.startsWith("en-")) {
                detectedLanguage = "english";
            }

            callback.onLanguageDetected(detectedLanguage);
        }
    }


    @Override
    public void onBufferReceived(byte[] buffer) {}

    @Override
    public void onEndOfSpeech() {
        //callback.onStatusChanged(VoiceInputView.VoiceStatus.PROCESSING);
        if (isListening) {
            startNewRecognition();
        }
    }

    @Override
    public void onError(int error) {
        Log.e(TAG, "Speech recognition error: " + error);
        // Only handle error if we're still in listening mode
        if (!isListening) return;

        String errorMessage = null;
        boolean canRestart = true;
        boolean shouldReinitSpeechRecognizer = false;
        boolean wasSpeechTimeout = false;
        boolean shouldPreferOfflineMode = false;
        switch (error) {
            case SpeechRecognizer.ERROR_NO_MATCH:
                // No speech detected, just restart without counting as retry
                errorMessage = "";
                canRestart = true;
                wasSpeechTimeout = true;
                shouldReinitSpeechRecognizer = true;
                break;
            case SpeechRecognizer.ERROR_SPEECH_TIMEOUT:
                // Speech timeout, just restart without counting as retry
                errorMessage = "";
                canRestart = true;
                wasSpeechTimeout = true;
                shouldReinitSpeechRecognizer = true;
                break;
            case SpeechRecognizer.ERROR_SERVER_DISCONNECTED:
                // Server disconnected, try to restart
                errorMessage = "Server disconnected";
                canRestart = true;
                shouldReinitSpeechRecognizer = true;
                break;
            case SpeechRecognizer.ERROR_RECOGNIZER_BUSY:
                // Recognizer busy, destroy and retry
                errorMessage = "Recognition service busy";
                canRestart = true;
                shouldReinitSpeechRecognizer = true;
                break;
            case SpeechRecognizer.ERROR_CLIENT:
                // Client error, try to reinit and restart
                errorMessage = "Client error";
                canRestart = true;
                shouldReinitSpeechRecognizer = true;
                break;
            default:
                // For all other errors, handle as non-recoverable
                errorMessage = "Recognition error";
                canRestart = false;
                shouldReinitSpeechRecognizer = false;
                break;
        }
        handleError(errorMessage, error, canRestart, shouldReinitSpeechRecognizer, wasSpeechTimeout);
    }

    @Override
    public void onResults(Bundle results) {
        if (results != null && results.containsKey(SpeechRecognizer.RESULTS_RECOGNITION)) {
            String text = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION).get(0);
            callback.onSpeechResult(text);
            if (isListening) {
                startNewRecognition();
            }
        }
    }

    @Override
    public void onPartialResults(Bundle partialResults) {
        if (partialResults != null && partialResults.containsKey(SpeechRecognizer.RESULTS_RECOGNITION)) {
            String partialText = partialResults.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION).get(0);
            String newText = partialText.substring(lastPartialResult.length()).trim();
            sessionPartialText += newText;
            if (!newText.isEmpty()) {
                callback.onPartialSpeechResult(sessionPartialText, newText);
                lastPartialResult = partialText;
            }
        }
    }

    @Override
    public void onEvent(int eventType, Bundle params) {}

    public void destroy() {
        stopListening();
        handler.removeCallbacksAndMessages(null);
    }
}
