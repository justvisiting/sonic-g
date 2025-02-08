package com.example.speechapp;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;
import android.speech.tts.Voice;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.Locale;
import java.util.Set;

public class MainActivity extends AppCompatActivity {
    private static final int PERMISSION_REQUEST_CODE = 123;
    private TextToSpeech textToSpeech;
    private SpeechRecognizer speechRecognizer;
    private EditText editText;
    private TextView textView;
    private Button speakButton;
    private Button listenButton;
    private Button languageButton;
    private boolean isListening = false;
    private String currentLanguage = "en-IN"; // Default to Indian English
    private int currentMode = 0; // 0: Indian English, 1: Hindi, 2: Hinglish

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        editText = findViewById(R.id.editText);
        textView = findViewById(R.id.textView);
        speakButton = findViewById(R.id.speakButton);
        listenButton = findViewById(R.id.listenButton);
        languageButton = findViewById(R.id.languageButton);

        // Configure EditText for hardware keyboard
        editText.setShowSoftInputOnFocus(true);
        editText.setRawInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        editText.setImeOptions(android.view.inputmethod.EditorInfo.IME_ACTION_DONE | android.view.inputmethod.EditorInfo.IME_FLAG_NO_EXTRACT_UI);
        
        // Disable suggestions
        editText.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE | android.text.InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            editText.setImportantForAutofill(View.IMPORTANT_FOR_AUTOFILL_NO);
        }

        // Initialize TTS with Indian accent
        initializeTextToSpeech();

        // Initialize Speech Recognizer
        if (SpeechRecognizer.isRecognitionAvailable(this)) {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);
            speechRecognizer.setRecognitionListener(new RecognitionListener() {
                @Override
                public void onReadyForSpeech(Bundle bundle) {
                    Toast.makeText(MainActivity.this, "Listening...", Toast.LENGTH_SHORT).show();
                }

                @Override
                public void onBeginningOfSpeech() {}

                @Override
                public void onRmsChanged(float v) {}

                @Override
                public void onBufferReceived(byte[] bytes) {}

                @Override
                public void onEndOfSpeech() {
                    isListening = false;
                    listenButton.setText("Start Listening");
                }

                @Override
                public void onError(int error) {
                    isListening = false;
                    listenButton.setText("Start Listening");
                    String message;
                    switch (error) {
                        case SpeechRecognizer.ERROR_AUDIO:
                            message = "Audio recording error";
                            break;
                        case SpeechRecognizer.ERROR_CLIENT:
                            message = "Client side error";
                            break;
                        case SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS:
                            message = "Insufficient permissions";
                            break;
                        case SpeechRecognizer.ERROR_NETWORK:
                            message = "Network error";
                            break;
                        case SpeechRecognizer.ERROR_NETWORK_TIMEOUT:
                            message = "Network timeout";
                            break;
                        case SpeechRecognizer.ERROR_NO_MATCH:
                            message = "No speech input";
                            break;
                        case SpeechRecognizer.ERROR_RECOGNIZER_BUSY:
                            message = "Recognition service busy";
                            break;
                        case SpeechRecognizer.ERROR_SERVER:
                            message = "Server error";
                            break;
                        case SpeechRecognizer.ERROR_SPEECH_TIMEOUT:
                            message = "No speech input";
                            break;
                        default:
                            message = "Unknown error occurred";
                            break;
                    }
                    Toast.makeText(MainActivity.this, message, Toast.LENGTH_SHORT).show();
                }

                @Override
                public void onResults(Bundle bundle) {
                    ArrayList<String> matches = bundle.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                    if (matches != null && !matches.isEmpty()) {
                        String text = matches.get(0);
                        textView.setText(text);
                    }
                }

                @Override
                public void onPartialResults(Bundle bundle) {
                    ArrayList<String> matches = bundle.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                    if (matches != null && !matches.isEmpty()) {
                        String text = matches.get(0);
                        textView.setText(text);
                    }
                }

                @Override
                public void onEvent(int i, Bundle bundle) {}
            });
        } else {
            Toast.makeText(this, "Speech recognition not available on this device", Toast.LENGTH_SHORT).show();
            listenButton.setEnabled(false);
        }

        // Language Toggle Button
        updateLanguageButtonText();
        languageButton.setOnClickListener(v -> {
            currentMode = (currentMode + 1) % 3;
            switch (currentMode) {
                case 0: // Indian English
                    currentLanguage = "en-IN";
                    break;
                case 1: // Hindi
                    currentLanguage = "hi-IN";
                    break;
                case 2: // Hinglish
                    currentLanguage = "en-IN,hi-IN";
                    break;
            }
            updateLanguageButtonText();
        });

        // Speak Button Click Listener
        speakButton.setOnClickListener(v -> {
            String text = editText.getText().toString();
            if (!text.isEmpty()) {
                textToSpeech.speak(text, TextToSpeech.QUEUE_FLUSH, null, null);
            } else {
                Toast.makeText(MainActivity.this, "Please enter text to speak", Toast.LENGTH_SHORT).show();
            }
        });

        // Listen Button Click Listener
        listenButton.setOnClickListener(v -> {
            if (checkPermission()) {
                if (!isListening) {
                    startListening();
                } else {
                    stopListening();
                }
            } else {
                requestPermission();
            }
        });
    }

    private void initializeTextToSpeech() {
        textToSpeech = new TextToSpeech(this, status -> {
            if (status == TextToSpeech.SUCCESS) {
                // Try to set Indian English voice
                int result = textToSpeech.setLanguage(new Locale("en", "IN"));
                
                // Set Indian voice if available
                Set<Voice> voices = textToSpeech.getVoices();
                if (voices != null) {
                    for (Voice voice : voices) {
                        Locale locale = voice.getLocale();
                        if (locale.getCountry().equals("IN")) {
                            textToSpeech.setVoice(voice);
                            break;
                        }
                    }
                }
                
                // Set speech rate and pitch for more natural Indian accent
                textToSpeech.setSpeechRate(0.85f);  // Slightly slower
                textToSpeech.setPitch(0.95f);       // Slightly lower pitch
                
                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    // If Indian English is not available, try Hindi
                    result = textToSpeech.setLanguage(new Locale("hi", "IN"));
                    if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                        Toast.makeText(MainActivity.this, "Indian language pack not installed", Toast.LENGTH_SHORT).show();
                    }
                }
            } else {
                Toast.makeText(MainActivity.this, "Text to Speech initialization failed", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void startListening() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);

        if (currentMode == 2) { // Hinglish mode
            // Support both Indian English and Hindi
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-IN,hi-IN");
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "en-IN");
            intent.putExtra(RecognizerIntent.EXTRA_ONLY_RETURN_LANGUAGE_PREFERENCE, false);
            // Add supported languages
            ArrayList<String> languages = new ArrayList<>();
            languages.add("en-IN");
            languages.add("hi-IN");
            intent.putStringArrayListExtra(RecognizerIntent.EXTRA_SUPPORTED_LANGUAGES, languages);
        } else {
            // Single language mode
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, currentLanguage);
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, currentLanguage);
            intent.putExtra(RecognizerIntent.EXTRA_ONLY_RETURN_LANGUAGE_PREFERENCE, true);
        }

        intent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true);
        intent.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, 5000);
        intent.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 1500);
        intent.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 1500);
        intent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3);
        intent.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, getPackageName());

        isListening = true;
        listenButton.setText("Stop Listening");
        speechRecognizer.startListening(intent);
    }

    private void stopListening() {
        isListening = false;
        listenButton.setText("Start Listening");
        speechRecognizer.stopListening();
    }

    private boolean checkPermission() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) 
                == PackageManager.PERMISSION_GRANTED;
    }

    private void requestPermission() {
        ActivityCompat.requestPermissions(this, 
                new String[]{Manifest.permission.RECORD_AUDIO}, 
                PERMISSION_REQUEST_CODE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, 
            @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startListening();
            } else {
                Toast.makeText(this, "Permission Denied", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void updateLanguageButtonText() {
        switch (currentMode) {
            case 0:
                languageButton.setText("Mode: Indian English");
                Toast.makeText(MainActivity.this, "Switched to Indian English", Toast.LENGTH_SHORT).show();
                if (textToSpeech != null) {
                    textToSpeech.setLanguage(new Locale("en", "IN"));
                }
                break;
            case 1:
                languageButton.setText("Mode: Hindi");
                Toast.makeText(MainActivity.this, "Switched to Hindi", Toast.LENGTH_SHORT).show();
                if (textToSpeech != null) {
                    textToSpeech.setLanguage(new Locale("hi", "IN"));
                }
                break;
            case 2:
                languageButton.setText("Mode: Hinglish");
                Toast.makeText(MainActivity.this, "Switched to Hinglish", Toast.LENGTH_SHORT).show();
                if (textToSpeech != null) {
                    // Use Hindi for better pronunciation of mixed language
                    textToSpeech.setLanguage(new Locale("hi", "IN"));
                }
                break;
        }
    }

    @Override
    protected void onDestroy() {
        if (textToSpeech != null) {
            textToSpeech.stop();
            textToSpeech.shutdown();
        }
        if (speechRecognizer != null) {
            speechRecognizer.destroy();
        }
        super.onDestroy();
    }
}
