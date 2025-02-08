package com.example.speechapp;

import android.Manifest;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.speech.RecognizerIntent;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class MainActivity extends AppCompatActivity {
    private static final int PERMISSION_REQUEST_CODE = 123;
    private static final int SPEECH_REQUEST_CODE = 124;
    private static final int SETTINGS_REQUEST_CODE = 125;
    private static final String PREFS_NAME = "SpeechAppPrefs";
    private static final String API_KEY_PREF = "gemini_api_key";
    private static final String LANGUAGE_PREF = "tts_language";
    
    private TextToSpeech textToSpeech;
    private TextView hindiTextView;
    private TextView hinglishTextView;
    private Button speakButton;
    private Button listenButton;
    private Button settingsButton;
    private Button stopButton;
    private Button startQuizButton;
    private boolean isListening = false;
    private boolean isSpeaking = false;
    private boolean quizMode = false;
    private RecyclerView chatRecyclerView;
    private ChatAdapter chatAdapter;
    private List<ChatMessage> messages;
    private GeminiAPI geminiAPI;
    private Map<String, Locale> languageMap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize language map
        languageMap = new LinkedHashMap<>();
        languageMap.put("Hindi (India)", new Locale("hi", "IN"));
        languageMap.put("English (US)", new Locale("en", "US"));
        languageMap.put("English (UK)", new Locale("en", "GB"));
        languageMap.put("English (India)", new Locale("en", "IN"));
        languageMap.put("English (Australia)", new Locale("en", "AU"));
        languageMap.put("Spanish (Spain)", new Locale("es", "ES"));
        languageMap.put("Spanish (Mexico)", new Locale("es", "MX"));
        languageMap.put("French (France)", new Locale("fr", "FR"));
        languageMap.put("German (Germany)", new Locale("de", "DE"));
        languageMap.put("Italian (Italy)", new Locale("it", "IT"));
        languageMap.put("Japanese (Japan)", new Locale("ja", "JP"));
        languageMap.put("Korean (Korea)", new Locale("ko", "KR"));
        languageMap.put("Chinese (China)", new Locale("zh", "CN"));
        languageMap.put("Russian (Russia)", new Locale("ru", "RU"));
        languageMap.put("Arabic (Saudi Arabia)", new Locale("ar", "SA"));

        // Initialize views
        hindiTextView = findViewById(R.id.hindiTextView);
        hinglishTextView = findViewById(R.id.hinglishTextView);
        speakButton = findViewById(R.id.speakButton);
        listenButton = findViewById(R.id.listenButton);
        settingsButton = findViewById(R.id.settingsButton);
        stopButton = findViewById(R.id.stopButton);
        startQuizButton = findViewById(R.id.startQuizButton);
        chatRecyclerView = findViewById(R.id.chatRecyclerView);

        // Initialize chat
        messages = new ArrayList<>();
        chatAdapter = new ChatAdapter(messages);
        chatRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        chatRecyclerView.setAdapter(chatAdapter);

        // Initialize Gemini API
        geminiAPI = new GeminiAPI(this);

        // Initialize Text to Speech with current language
        initializeTextToSpeech();

        // Set up listen button
        listenButton.setOnClickListener(v -> {
            if (checkPermission()) {
                startListening();
            } else {
                requestPermission();
            }
        });

        // Set up speak button
        speakButton.setOnClickListener(v -> {
            String text = hinglishTextView.getText().toString();
            if (!text.isEmpty()) {
                Bundle params = new Bundle();
                params.putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "messageId");
                textToSpeech.speak(text, TextToSpeech.QUEUE_FLUSH, params, "messageId");
            }
        });

        // Set up stop button
        stopButton.setOnClickListener(v -> {
            if (isSpeaking) {
                textToSpeech.stop();
                isSpeaking = false;
                stopButton.setVisibility(View.GONE);
                speakButton.setVisibility(View.VISIBLE);
            }
        });

        // Set up settings button
        settingsButton.setOnClickListener(v -> {
            Intent intent = new Intent(this, SettingsActivity.class);
            startActivityForResult(intent, SETTINGS_REQUEST_CODE);
        });

        // Set up start quiz button
        startQuizButton.setOnClickListener(v -> {
            Log.d("MainActivity", "Starting quiz");
            quizMode = true;
            messages.clear();
            chatAdapter.notifyDataSetChanged();
            
            // Start new quiz with Gemini
            geminiAPI.startNewQuiz(new GeminiAPI.GeminiCallback() {
                @Override
                public void onResponse(String response) {
                    Log.d("MainActivity", "Got initial quiz response");
                    runOnUiThread(() -> {
                        // Convert bot response to Hindi
                        String botHindiText = transliterateToHindi(response);
                        addBotMessage(botHindiText, response);
                        
                        // Speak the response
                        Bundle params = new Bundle();
                        params.putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "quizStart");
                        Log.d("MainActivity", "Starting TTS for quiz start");
                        int result = textToSpeech.speak(response, TextToSpeech.QUEUE_FLUSH, params, "quizStart");
                        if (result == TextToSpeech.ERROR) {
                            Log.e("MainActivity", "Error starting TTS");
                        }
                    });
                }

                @Override
                public void onError(String error) {
                    Log.e("MainActivity", "Quiz start error: " + error);
                    runOnUiThread(() -> {
                        Toast.makeText(MainActivity.this, error, Toast.LENGTH_SHORT).show();
                    });
                }
            });
        });

        // Check if API key is set
        checkApiKey();

        // Add welcome message
        addBotMessage("नमस्ते! मैं आपकी कैसे मदद कर सकता हूं? क्विज़ शुरू करने के लिए 'Start Quiz' बटन दबाएं।", 
                     "Namaste! Main aapki kaise madad kar sakta hoon? Quiz shuru karne ke liye 'Start Quiz' button dabayen.");
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

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == SPEECH_REQUEST_CODE && resultCode == RESULT_OK && data != null) {
            ArrayList<String> results = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
            if (results != null && !results.isEmpty()) {
                String spokenText = results.get(0);
                processUserInput(spokenText);
            }
        } else if (requestCode == SETTINGS_REQUEST_CODE && resultCode == RESULT_OK) {
            // Reinitialize TTS with new language
            initializeTextToSpeech();
        }
    }

    private void processUserInput(String spokenText) {
        Log.d("MainActivity", "Processing user input: " + spokenText);
        // Convert to Hindi and add to chat
        String hindiText = transliterateToHindi(spokenText);
        addUserMessage(hindiText, spokenText);

        // Get response from Gemini
        geminiAPI.generateResponse(spokenText, new GeminiAPI.GeminiCallback() {
            @Override
            public void onResponse(String response) {
                Log.d("MainActivity", "Got Gemini response");
                runOnUiThread(() -> {
                    // Convert bot response to Hindi
                    String botHindiText = transliterateToHindi(response);
                    addBotMessage(botHindiText, response);
                    
                    // Speak the response
                    Bundle params = new Bundle();
                    params.putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "messageId");
                    Log.d("MainActivity", "Starting TTS for response");
                    int result = textToSpeech.speak(response, TextToSpeech.QUEUE_FLUSH, params, "messageId");
                    if (result == TextToSpeech.ERROR) {
                        Log.e("MainActivity", "Error starting TTS");
                    }
                });
            }

            @Override
            public void onError(String error) {
                Log.e("MainActivity", "Gemini response error: " + error);
                runOnUiThread(() -> {
                    Toast.makeText(MainActivity.this, error, Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private void addUserMessage(String hindiText, String hinglishText) {
        ChatMessage message = new ChatMessage(hinglishText, hindiText, hinglishText, ChatMessage.TYPE_USER);
        runOnUiThread(() -> {
            messages.add(message);
            chatAdapter.notifyItemInserted(messages.size() - 1);
            chatRecyclerView.smoothScrollToPosition(messages.size() - 1);
            hindiTextView.setText(hindiText);
            hinglishTextView.setText(hinglishText);
        });
    }

    private void addBotMessage(String hindiText, String hinglishText) {
        ChatMessage message = new ChatMessage(hinglishText, hindiText, hinglishText, ChatMessage.TYPE_BOT);
        runOnUiThread(() -> {
            messages.add(message);
            chatAdapter.notifyItemInserted(messages.size() - 1);
            chatRecyclerView.smoothScrollToPosition(messages.size() - 1);
            hindiTextView.setText(hindiText);
            hinglishTextView.setText(hinglishText);
        });
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

    private void checkApiKey() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        String apiKey = prefs.getString(API_KEY_PREF, "");
        if (apiKey.isEmpty()) {
            new AlertDialog.Builder(this)
                .setTitle("API Key Required")
                .setMessage("You need a Gemini API key to use this app. Would you like to set it up now?")
                .setPositiveButton("Yes", (dialog, which) -> {
                    Intent intent = new Intent(this, SettingsActivity.class);
                    startActivityForResult(intent, SETTINGS_REQUEST_CODE);
                })
                .setNegativeButton("Later", null)
                .show();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (textToSpeech != null) {
            textToSpeech.stop();
            textToSpeech.shutdown();
        }
    }

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

    private void initializeTextToSpeech() {
        textToSpeech = new TextToSpeech(this, status -> {
            if (status == TextToSpeech.SUCCESS) {
                // Set up TTS with current language
                SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
                String languageKey = prefs.getString(LANGUAGE_PREF, "English (US)");
                Locale locale = languageMap.get(languageKey);
                if (locale != null) {
                    int result = textToSpeech.setLanguage(locale);
                    if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                        Log.e("MainActivity", "Language not supported");
                    }
                }
                
                // Set up utterance progress listener
                textToSpeech.setOnUtteranceProgressListener(new UtteranceProgressListener() {
                    @Override
                    public void onStart(String utteranceId) {
                        Log.d("MainActivity", "TTS Started: " + utteranceId);
                    }

                    @Override
                    public void onDone(String utteranceId) {
                        Log.d("MainActivity", "TTS Done: " + utteranceId);
                        if (quizMode && (utteranceId.equals("quizStart") || utteranceId.equals("messageId"))) {
                            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                                runOnUiThread(() -> {
                                    Log.d("MainActivity", "Starting listening after TTS");
                                    if (checkPermission()) {
                                        startListening();
                                    } else {
                                        Log.e("MainActivity", "Missing permission for listening");
                                    }
                                });
                            }, 1000);
                        }
                    }

                    @Override
                    public void onError(String utteranceId) {
                        Log.e("MainActivity", "TTS Error: " + utteranceId);
                    }
                });
            } else {
                Log.e("MainActivity", "TTS Initialization failed");
            }
        });
    }
}
