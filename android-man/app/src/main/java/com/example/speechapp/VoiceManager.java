package com.example.speechapp;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Locale;

public class VoiceManager implements RecognitionListener {
    private static final String TAG = "VoiceManager";
    private final Context context;
    private SpeechRecognizer speechRecognizer;
    private TextToSpeech textToSpeech;
    private final VoiceCallback callback;
    private boolean isListening = false;
    private boolean isProcessing = false;
    private final Handler handler;
    private ArrayList<String> supportedLanguages;
    private String lastPartialResult = "";

    public interface VoiceCallback {
        void onSpeechResult(String fullText);
        void onPartialSpeechResult(String textSoFar, String newText);
        void onSpeechError(String error);
        void onListeningStarted();
        void onListeningStopped();
        void onStatusChanged(VoiceInputView.VoiceStatus status);
        void onLanguageDetected(String language);
    }

    public VoiceManager(Context context, VoiceCallback callback) {
        this.context = context;
        this.callback = callback;
        this.handler = new Handler(Looper.getMainLooper());
        this.supportedLanguages = new ArrayList<>(Arrays.asList("en", "kn", "kn-in", "kn_in", "kn_IN", "te", "te-in", "te_IN", Locale.getDefault().toString(), "hinglish"));
        initializeSpeechRecognizer();
        initializeTextToSpeech();
        
    }

    private void initializeSpeechRecognizer() {
        if (speechRecognizer != null) {
            speechRecognizer.destroy();
        }
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context);
        //Log.i(TAG, "isRe")
        speechRecognizer.setRecognitionListener(this);
    }

    private void initializeTextToSpeech() {
        textToSpeech = new TextToSpeech(context, status -> {
            if (status == TextToSpeech.SUCCESS) {
                int result = textToSpeech.setLanguage(Locale.US);
                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    Log.e(TAG, "Language not supported");
                } else {
                    textToSpeech.setSpeechRate(1.0f);
                    textToSpeech.setPitch(1.0f);
                }
            } else {
                Log.e(TAG, "TTS Initialization failed");
            }
        });
    }

    public void startListening() {
        if (!isListening || isProcessing) return;

        isProcessing = true;
        lastPartialResult = ""; // Reset for new session
        
        // Re-initialize speech recognizer to prevent client-side errors
        initializeSpeechRecognizer();
        
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, supportedLanguages);
        intent.putExtra(RecognizerIntent.EXTRA_ENABLE_LANGUAGE_SWITCH, supportedLanguages);
        intent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1);
        intent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true);
        intent.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, context.getPackageName());

        try {
            handler.post(() -> {
                try {
                    speechRecognizer.startListening(intent);
                    callback.onListeningStarted();
                    callback.onStatusChanged(VoiceInputView.VoiceStatus.LISTENING);
                } catch (Exception e) {
                    handleError("Error starting speech recognition: " + e.getMessage());
                }
            });
        } catch (Exception e) {
            handleError("Error starting speech recognition: " + e.getMessage());
        }
    }

    private void handleError(String message) {
        isProcessing = false;
        callback.onSpeechError(message);
        callback.onStatusChanged(VoiceInputView.VoiceStatus.ERROR);
        stopListening();
    }

    public void stopListening() {
        isListening = false;
        isProcessing = false;
        try {
            speechRecognizer.stopListening();
        } catch (Exception e) {
            Log.e(TAG, "Error stopping speech recognition", e);
        }
        callback.onListeningStopped();
    }

    public void toggleListening() {
        if (isListening) {
            stopListening();
        } else {
            isListening = true;
            isProcessing = false;
            startListening();
        }
    }

    public void speak(String text) {
        if (textToSpeech != null) {
            textToSpeech.speak(text, TextToSpeech.QUEUE_FLUSH, null, "utterance_id");
        }
    }

    public void destroy() {
        if (speechRecognizer != null) {
            speechRecognizer.destroy();
            speechRecognizer = null;
        }
        if (textToSpeech != null) {
            textToSpeech.stop();
            textToSpeech.shutdown();
            textToSpeech = null;
        }
        handler.removeCallbacksAndMessages(null);
    }

    // Speech Recognition Listener methods
    @Override
    public void onReadyForSpeech(Bundle params) {
        Log.d(TAG, "Ready for speech");
        isProcessing = false;
    }

    @Override
    public void onBeginningOfSpeech() {
        Log.d(TAG, "Beginning of speech");
        isProcessing = false;
        callback.onStatusChanged(VoiceInputView.VoiceStatus.LISTENING);
    }

    @Override
    public void onRmsChanged(float rmsdB) {
        // Convert RMS dB to amplitude (0-1 range)
        float amplitude = Math.min(1.0f, Math.max(0.0f, rmsdB / 10.0f));
        if (context instanceof MainActivity) {
            ((Activity) context).runOnUiThread(() -> {
                ((MainActivity) context).updateVoiceAmplitude(amplitude);
            });
        }
    }

    @Override
    public void onBufferReceived(byte[] buffer) {
        // Handle speech buffer if needed
    }

    @Override
    public void onEndOfSpeech() {
        Log.d(TAG, "End of speech");
        isProcessing = true;
        callback.onStatusChanged(VoiceInputView.VoiceStatus.PROCESSING);
    }

    @Override
    public void onError(int error) {
        Log.e(TAG, "Speech recognition error: " + error);
        isProcessing = false;
        
        // Only handle error if we're still in listening mode
        if (!isListening) return;

        String errorMessage = null;
        boolean shouldRestart = true;

        switch (error) {
            case SpeechRecognizer.ERROR_AUDIO:
                errorMessage = "Audio recording error";
                shouldRestart = false;
                break;
            case SpeechRecognizer.ERROR_CLIENT:
                // Reinitialize and retry on client error
                initializeSpeechRecognizer();
                handler.postDelayed(this::startListening, 300);
                return;
            case SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS:
                errorMessage = "Insufficient permissions";
                shouldRestart = false;
                break;
            case SpeechRecognizer.ERROR_NETWORK:
                errorMessage = "Network error";
                shouldRestart = false;
                break;
            case SpeechRecognizer.ERROR_NETWORK_TIMEOUT:
                errorMessage = "Network timeout";
                break;
            case SpeechRecognizer.ERROR_NO_MATCH:
                shouldRestart = true;
                break;
            case SpeechRecognizer.ERROR_RECOGNIZER_BUSY:
                // Reinitialize and retry after a longer delay
                initializeSpeechRecognizer();
                handler.postDelayed(this::startListening, 1000);
                return;
            case SpeechRecognizer.ERROR_SERVER:
                errorMessage = "Server error";
                break;
            case SpeechRecognizer.ERROR_SPEECH_TIMEOUT:
                shouldRestart = true;
                break;
            default:
                errorMessage = "Recognition error";
                break;
        }

        if (errorMessage != null) {
            callback.onSpeechError(errorMessage);
            callback.onStatusChanged(VoiceInputView.VoiceStatus.ERROR);
        }

        if (shouldRestart && isListening && !isProcessing) {
            handler.postDelayed(this::startListening, 300);
        }
    }

    @Override
    public void onResults(Bundle results) {
        isProcessing = false;
        ArrayList<String> matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
        if (matches != null && !matches.isEmpty()) {
            String text = matches.get(0);
            callback.onSpeechResult(text);
            stopListening();
        } else if (isListening) {
            handler.postDelayed(this::startListening, 300);
        }
    }

    @Override
    public void onPartialResults(Bundle partialResults) {
        ArrayList<String> results = partialResults.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
        if (results != null && !results.isEmpty()) {
            String partialText = results.get(0);
            Log.d(TAG, "Full Partial Result: " + partialText);
            
            // Log what's new compared to last result
            if (!partialText.equals(lastPartialResult)) {
                String newPart = partialText.substring(lastPartialResult.length()).trim();
                if (newPart.length() > 0) {
                    Log.d(TAG, "New Addition: " + newPart);
                    lastPartialResult = partialText;
                    callback.onPartialSpeechResult(lastPartialResult, newPart);
                }
            }
            
            // Also check for language detection
            String detectedLanguage = partialResults.getString(SpeechRecognizer.DETECTED_LANGUAGE);
            if (detectedLanguage != null) {
                Log.d(TAG, "Detected Language: " + detectedLanguage);
            }
        }
    }

    @Override
    public void onEvent(int eventType, Bundle params) {
        // Handle custom events if needed
    }

    @Override
    public void onLanguageDetection(Bundle results) {
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
}
