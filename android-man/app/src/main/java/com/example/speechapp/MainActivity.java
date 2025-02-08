package com.example.speechapp;

import android.Manifest;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;
import android.speech.tts.Voice;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class MainActivity extends AppCompatActivity {
    private static final int PERMISSION_REQUEST_CODE = 123;
    private static final int SPEECH_REQUEST_CODE = 124;
    private TextToSpeech textToSpeech;
    private SpeechRecognizer speechRecognizer;
    private EditText editText;
    private TextView hindiTextView;
    private TextView hinglishTextView;
    private Button speakButton;
    private Button listenButton;
    private Button languageButton;
    private boolean isListening = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        editText = findViewById(R.id.editText);
        hindiTextView = findViewById(R.id.hindiTextView);
        hinglishTextView = findViewById(R.id.hinglishTextView);
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

        // Initialize language button text
        updateLanguageButtonText();

        // Set up language button click
        languageButton.setOnClickListener(v -> {
            Toast.makeText(MainActivity.this, "Always showing both Hindi and Hinglish translations", Toast.LENGTH_SHORT).show();
        });

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
                        hinglishTextView.setText(text);
                    }
                }

                @Override
                public void onPartialResults(Bundle bundle) {
                    ArrayList<String> matches = bundle.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                    if (matches != null && !matches.isEmpty()) {
                        String text = matches.get(0);
                        hinglishTextView.setText(text);
                    }
                }

                @Override
                public void onEvent(int i, Bundle bundle) {}
            });
        } else {
            Toast.makeText(this, "Speech recognition not available on this device", Toast.LENGTH_SHORT).show();
            listenButton.setEnabled(false);
        }

        // Set up listen button
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

        // Initialize Text to Speech
        textToSpeech = new TextToSpeech(this, status -> {
            if (status == TextToSpeech.SUCCESS) {
                textToSpeech.setLanguage(new Locale("hi", "IN"));
            }
        });

        // Set up speak button
        speakButton.setOnClickListener(v -> {
            String text = editText.getText().toString();
            if (!text.isEmpty()) {
                textToSpeech.speak(text, TextToSpeech.QUEUE_FLUSH, null, null);
            }
        });
    }

    private void startListening() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-IN");
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak now...");
        
        try {
            startActivityForResult(intent, SPEECH_REQUEST_CODE);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, "Speech recognition not available", Toast.LENGTH_SHORT).show();
        }
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

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == SPEECH_REQUEST_CODE && resultCode == RESULT_OK && data != null) {
            ArrayList<String> results = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
            if (results != null && !results.isEmpty()) {
                String spokenText = results.get(0);
                Log.d("SpeechApp", "Spoken text: " + spokenText);
                
                // Always show both Hindi and Hinglish
                String devanagari = transliterateToHindi(spokenText);
                Log.d("SpeechApp", "Devanagari: " + devanagari);
                
                // Set text on UI thread
                final String finalDevanagari = devanagari;
                runOnUiThread(() -> {
                    hindiTextView.setText(finalDevanagari);
                    hinglishTextView.setText(spokenText);
                    Toast.makeText(MainActivity.this, 
                        "Hindi: " + finalDevanagari + "\nHinglish: " + spokenText, 
                        Toast.LENGTH_LONG).show();
                });
            }
        }
    }

    private void updateLanguageButtonText() {
        languageButton.setText("Mode: Voice to Hindi & Hinglish");
        Toast.makeText(MainActivity.this, "Speak in any language - Will show both Hindi and Hinglish", Toast.LENGTH_SHORT).show();
    }

    // Helper method to transliterate English (romanized) text to Hindi
    private String transliterateToHindi(String englishText) {
        Map<String, String> reverseTranslitMap = new HashMap<>();
        // Basic vowels
        reverseTranslitMap.put("a", "अ"); reverseTranslitMap.put("aa", "आ");
        reverseTranslitMap.put("i", "इ"); reverseTranslitMap.put("ee", "ई");
        reverseTranslitMap.put("u", "उ"); reverseTranslitMap.put("oo", "ऊ");
        reverseTranslitMap.put("e", "ए"); reverseTranslitMap.put("ai", "ऐ");
        reverseTranslitMap.put("o", "ओ"); reverseTranslitMap.put("au", "औ");
        
        // Consonants with inherent 'a'
        reverseTranslitMap.put("ka", "क"); reverseTranslitMap.put("kha", "ख");
        reverseTranslitMap.put("ga", "ग"); reverseTranslitMap.put("gha", "घ");
        reverseTranslitMap.put("cha", "च"); reverseTranslitMap.put("chha", "छ");
        reverseTranslitMap.put("ja", "ज"); reverseTranslitMap.put("jha", "झ");
        reverseTranslitMap.put("ta", "त"); reverseTranslitMap.put("tha", "थ");
        reverseTranslitMap.put("da", "द"); reverseTranslitMap.put("dha", "ध");
        reverseTranslitMap.put("na", "न"); reverseTranslitMap.put("pa", "प");
        reverseTranslitMap.put("pha", "फ"); reverseTranslitMap.put("ba", "ब");
        reverseTranslitMap.put("bha", "भ"); reverseTranslitMap.put("ma", "म");
        reverseTranslitMap.put("ya", "य"); reverseTranslitMap.put("ra", "र");
        reverseTranslitMap.put("la", "ल"); reverseTranslitMap.put("va", "व");
        reverseTranslitMap.put("sha", "श"); reverseTranslitMap.put("sa", "स");
        reverseTranslitMap.put("ha", "ह"); 
        
        // Common words
        reverseTranslitMap.put("main", "मैं");
        reverseTranslitMap.put("hai", "है");
        reverseTranslitMap.put("hoon", "हूं");
        reverseTranslitMap.put("kya", "क्या");
        reverseTranslitMap.put("aap", "आप");
        reverseTranslitMap.put("tum", "तुम");
        reverseTranslitMap.put("kaise", "कैसे");
        reverseTranslitMap.put("namaste", "नमस्ते");
        
        // Simple word-by-word transliteration
        String[] words = englishText.toLowerCase().split("\\s+");
        StringBuilder result = new StringBuilder();
        
        for (String word : words) {
            boolean wordTranslated = false;
            
            // First try to match the whole word
            if (reverseTranslitMap.containsKey(word)) {
                result.append(reverseTranslitMap.get(word));
                wordTranslated = true;
            } else {
                // Try to match parts of the word
                StringBuilder currentWord = new StringBuilder();
                int i = 0;
                while (i < word.length()) {
                    boolean matchFound = false;
                    // Try to match longer sequences first
                    for (int len = Math.min(4, word.length() - i); len > 0; len--) {
                        String part = word.substring(i, i + len);
                        if (reverseTranslitMap.containsKey(part)) {
                            currentWord.append(reverseTranslitMap.get(part));
                            i += len;
                            matchFound = true;
                            break;
                        }
                    }
                    if (!matchFound) {
                        // If no match found, keep the original character
                        currentWord.append(word.charAt(i));
                        i++;
                    }
                }
                result.append(currentWord);
            }
            result.append(" ");
        }
        
        return result.toString().trim();
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
