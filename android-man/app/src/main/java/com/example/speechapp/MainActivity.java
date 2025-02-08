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
        
        // Vowels and Matras
        reverseTranslitMap.put("a", "अ"); 
        reverseTranslitMap.put("aa", "आ"); reverseTranslitMap.put("ā", "आ");
        reverseTranslitMap.put("i", "इ"); 
        reverseTranslitMap.put("ee", "ई"); reverseTranslitMap.put("ī", "ई");
        reverseTranslitMap.put("u", "उ"); 
        reverseTranslitMap.put("oo", "ऊ"); reverseTranslitMap.put("ū", "ऊ");
        reverseTranslitMap.put("ri", "ऋ");
        reverseTranslitMap.put("e", "ए"); 
        reverseTranslitMap.put("ai", "ऐ");
        reverseTranslitMap.put("o", "ओ"); 
        reverseTranslitMap.put("au", "औ");
        reverseTranslitMap.put("am", "ं"); reverseTranslitMap.put("an", "ं");
        
        // Consonants
        reverseTranslitMap.put("k", "क्"); reverseTranslitMap.put("ka", "क");
        reverseTranslitMap.put("kh", "ख्"); reverseTranslitMap.put("kha", "ख");
        reverseTranslitMap.put("g", "ग्"); reverseTranslitMap.put("ga", "ग");
        reverseTranslitMap.put("gh", "घ्"); reverseTranslitMap.put("gha", "घ");
        reverseTranslitMap.put("ng", "ङ्"); reverseTranslitMap.put("nga", "ङ");
        
        reverseTranslitMap.put("ch", "च्"); reverseTranslitMap.put("cha", "च");
        reverseTranslitMap.put("chh", "छ्"); reverseTranslitMap.put("chha", "छ");
        reverseTranslitMap.put("j", "ज्"); reverseTranslitMap.put("ja", "ज");
        reverseTranslitMap.put("jh", "झ्"); reverseTranslitMap.put("jha", "झ");
        reverseTranslitMap.put("ny", "ञ्"); reverseTranslitMap.put("nya", "ञ");
        
        reverseTranslitMap.put("t", "त्"); reverseTranslitMap.put("ta", "त");
        reverseTranslitMap.put("th", "थ्"); reverseTranslitMap.put("tha", "थ");
        reverseTranslitMap.put("d", "द्"); reverseTranslitMap.put("da", "द");
        reverseTranslitMap.put("dh", "ध्"); reverseTranslitMap.put("dha", "ध");
        reverseTranslitMap.put("n", "न्"); reverseTranslitMap.put("na", "न");
        
        reverseTranslitMap.put("p", "प्"); reverseTranslitMap.put("pa", "प");
        reverseTranslitMap.put("ph", "फ्"); reverseTranslitMap.put("pha", "फ");
        reverseTranslitMap.put("f", "फ्"); reverseTranslitMap.put("fa", "फ");
        reverseTranslitMap.put("b", "ब्"); reverseTranslitMap.put("ba", "ब");
        reverseTranslitMap.put("bh", "भ्"); reverseTranslitMap.put("bha", "भ");
        reverseTranslitMap.put("m", "म्"); reverseTranslitMap.put("ma", "म");
        
        reverseTranslitMap.put("y", "य्"); reverseTranslitMap.put("ya", "य");
        reverseTranslitMap.put("r", "र्"); reverseTranslitMap.put("ra", "र");
        reverseTranslitMap.put("l", "ल्"); reverseTranslitMap.put("la", "ल");
        reverseTranslitMap.put("v", "व्"); reverseTranslitMap.put("va", "व");
        reverseTranslitMap.put("w", "व्"); reverseTranslitMap.put("wa", "व");
        
        reverseTranslitMap.put("sh", "श्"); reverseTranslitMap.put("sha", "श");
        reverseTranslitMap.put("s", "स्"); reverseTranslitMap.put("sa", "स");
        reverseTranslitMap.put("h", "ह्"); reverseTranslitMap.put("ha", "ह");
        
        // Matras (vowel marks)
        Map<String, String> matras = new HashMap<>();
        matras.put("a", ""); // No matra for 'a'
        matras.put("aa", "ा"); matras.put("ā", "ा");
        matras.put("i", "ि"); 
        matras.put("ee", "ी"); matras.put("ī", "ी");
        matras.put("u", "ु"); 
        matras.put("oo", "ू"); matras.put("ū", "ू");
        matras.put("ri", "ृ");
        matras.put("e", "े"); 
        matras.put("ai", "ै");
        matras.put("o", "ो"); 
        matras.put("au", "ौ");
        
        // Common words
        Map<String, String> commonWords = new HashMap<>();
        commonWords.put("main", "मैं");
        commonWords.put("mai", "मैं");
        commonWords.put("mein", "में");
        commonWords.put("hai", "है");
        commonWords.put("hain", "हैं");
        commonWords.put("he", "है");
        commonWords.put("hoon", "हूं");
        commonWords.put("hun", "हूं");
        commonWords.put("ho", "हो");
        commonWords.put("kya", "क्या");
        commonWords.put("kaise", "कैसे");
        commonWords.put("kaisa", "कैसा");
        commonWords.put("aap", "आप");
        commonWords.put("tum", "तुम");
        commonWords.put("ap", "आप");
        commonWords.put("namaste", "नमस्ते");
        commonWords.put("namaskar", "नमस्कार");
        commonWords.put("dhanyawad", "धन्यवाद");
        commonWords.put("shukriya", "शुक्रिया");
        
        // First try to match common words
        String[] words = englishText.toLowerCase().split("\\s+");
        StringBuilder result = new StringBuilder();
        
        for (String word : words) {
            // First check if it's a common word
            if (commonWords.containsKey(word)) {
                result.append(commonWords.get(word));
            } else {
                // Process the word character by character
                StringBuilder currentWord = new StringBuilder();
                int i = 0;
                while (i < word.length()) {
                    boolean found = false;
                    // Try to match longer sequences first (up to 4 characters)
                    for (int len = Math.min(4, word.length() - i); len > 0 && !found; len--) {
                        String part = word.substring(i, i + len);
                        // Check for consonant + vowel combinations
                        if (reverseTranslitMap.containsKey(part)) {
                            if (currentWord.length() > 0 && part.startsWith("a")) {
                                // If we have a consonant and the next part starts with 'a',
                                // we don't need to add anything (implicit 'a' sound)
                                i += 1;
                            } else {
                                currentWord.append(reverseTranslitMap.get(part));
                                i += len;
                            }
                            found = true;
                        }
                        // Check for matras (vowel marks)
                        else if (currentWord.length() > 0 && matras.containsKey(part)) {
                            currentWord.append(matras.get(part));
                            i += len;
                            found = true;
                        }
                    }
                    if (!found) {
                        // If no match found, just keep the character
                        currentWord.append(word.charAt(i));
                        i++;
                    }
                }
                result.append(currentWord);
            }
            result.append(" ");
        }
        
        // Clean up the text by handling half letters correctly
        String finalText = result.toString().trim()
            .replace("्ा", "ा")
            .replace("्े", "े")
            .replace("्ै", "ै")
            .replace("्ो", "ो")
            .replace("्ौ", "ौ")
            .replace("्ं", "ं");
            
        return finalText;
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
