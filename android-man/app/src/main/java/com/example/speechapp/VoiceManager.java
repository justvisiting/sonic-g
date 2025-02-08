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
import android.speech.tts.UtteranceProgressListener;
import android.util.Log;
import java.util.ArrayList;
import java.util.Locale;

public class VoiceManager implements RecognitionListener {
    private static final String TAG = "VoiceManager";
    private final Context context;
    private final SpeechRecognizer speechRecognizer;
    private TextToSpeech textToSpeech;
    private final VoiceCallback callback;
    private boolean isListening = false;
    private final Handler handler;
    private static final int ERROR_TIMEOUT = 3000; // 3 seconds

    public interface VoiceCallback {
        void onSpeechResult(String text);
        void onSpeechError(String error);
        void onListeningStarted();
        void onListeningStopped();
        void onStatusChanged(VoiceInputView.VoiceStatus status);
    }

    public VoiceManager(Context context, VoiceCallback callback) {
        this.context = context;
        this.callback = callback;
        this.handler = new Handler(Looper.getMainLooper());

        // Initialize speech recognizer
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context);
        speechRecognizer.setRecognitionListener(this);

        // Initialize text to speech
        initializeTextToSpeech();
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
        if (!isListening) return;
        
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
        intent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1);
        intent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false);

        try {
            speechRecognizer.startListening(intent);
            callback.onListeningStarted();
            callback.onStatusChanged(VoiceInputView.VoiceStatus.LISTENING);
            
            // Set a timeout for listening
            handler.postDelayed(() -> {
                if (isListening) {
                    stopListening();
                    callback.onStatusChanged(VoiceInputView.VoiceStatus.ERROR);
                    callback.onSpeechError("Listening timeout");
                }
            }, ERROR_TIMEOUT);
        } catch (Exception e) {
            callback.onSpeechError("Error starting speech recognition: " + e.getMessage());
            callback.onStatusChanged(VoiceInputView.VoiceStatus.ERROR);
        }
    }

    public void stopListening() {
        isListening = false;
        speechRecognizer.stopListening();
        callback.onListeningStopped();
    }

    public void toggleListening() {
        isListening = !isListening;
        if (isListening) {
            startListening();
        } else {
            stopListening();
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
        }
        if (textToSpeech != null) {
            textToSpeech.stop();
            textToSpeech.shutdown();
        }
        handler.removeCallbacksAndMessages(null);
    }

    // Speech Recognition Listener methods
    @Override
    public void onReadyForSpeech(Bundle params) {
        Log.d(TAG, "Ready for speech");
    }

    @Override
    public void onBeginningOfSpeech() {
        Log.d(TAG, "Beginning of speech");
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
        callback.onStatusChanged(VoiceInputView.VoiceStatus.PROCESSING);
    }

    @Override
    public void onError(int error) {
        String errorMessage;
        switch (error) {
            case SpeechRecognizer.ERROR_AUDIO:
                errorMessage = "Audio recording error";
                break;
            case SpeechRecognizer.ERROR_CLIENT:
                errorMessage = "Client side error";
                break;
            case SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS:
                errorMessage = "Insufficient permissions";
                break;
            case SpeechRecognizer.ERROR_NETWORK:
                errorMessage = "Network error";
                break;
            case SpeechRecognizer.ERROR_NETWORK_TIMEOUT:
                errorMessage = "Network timeout";
                break;
            case SpeechRecognizer.ERROR_NO_MATCH:
                errorMessage = "No recognition match";
                break;
            case SpeechRecognizer.ERROR_RECOGNIZER_BUSY:
                errorMessage = "Recognition service busy";
                break;
            case SpeechRecognizer.ERROR_SERVER:
                errorMessage = "Server error";
                break;
            case SpeechRecognizer.ERROR_SPEECH_TIMEOUT:
                errorMessage = "No speech input";
                break;
            default:
                errorMessage = "Unknown error";
                break;
        }
        callback.onSpeechError(errorMessage);
        callback.onStatusChanged(VoiceInputView.VoiceStatus.ERROR);
        
        // Restart listening if still in listening mode
        if (isListening) {
            handler.postDelayed(this::startListening, 1000);
        }
    }

    @Override
    public void onResults(Bundle results) {
        ArrayList<String> matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
        if (matches != null && !matches.isEmpty()) {
            String text = matches.get(0);
            callback.onSpeechResult(text);
        }

        // Continue listening if in listening mode
        if (isListening) {
            handler.postDelayed(this::startListening, 1000);
        }
    }

    @Override
    public void onPartialResults(Bundle partialResults) {
        // Handle partial results if needed
    }

    @Override
    public void onEvent(int eventType, Bundle params) {
        // Handle custom events if needed
    }
}
