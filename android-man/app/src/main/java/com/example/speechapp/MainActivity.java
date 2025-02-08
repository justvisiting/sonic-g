package com.example.speechapp;

import android.Manifest;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class MainActivity extends AppCompatActivity {
    private static final int PERMISSION_REQUEST_CODE = 123;
    private static final int SETTINGS_REQUEST_CODE = 125;
    private static final String PREFS_NAME = "SpeechAppPrefs";
    private static final String API_KEY_PREF = "gemini_api_key";
    private static final String LANGUAGE_PREF = "tts_language";
    private TextView hindiTextView;
    private TextView hinglishTextView;
    private Button startQuizButton;
    private Button stopQuizButton;
    private Button settingsButton;
    private EditText chatInput;
    private ImageButton sendButton;
    private boolean quizMode = false;
    private RecyclerView chatRecyclerView;
    private ChatAdapter chatAdapter;
    private List<ChatMessage> messages;
    private GeminiAPI geminiAPI;
    private Handler mainHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mainHandler = new Handler(Looper.getMainLooper());

        // Initialize views
        hindiTextView = findViewById(R.id.hindiTextView);
        hinglishTextView = findViewById(R.id.hinglishTextView);
        startQuizButton = findViewById(R.id.startQuizButton);
        stopQuizButton = findViewById(R.id.stopQuizButton);
        settingsButton = findViewById(R.id.settingsButton);
        chatRecyclerView = findViewById(R.id.chatRecyclerView);
        chatInput = findViewById(R.id.chatInput);
        sendButton = findViewById(R.id.sendButton);

        // Initialize chat
        messages = new ArrayList<>();
        chatAdapter = new ChatAdapter(messages);
        chatRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        chatRecyclerView.setAdapter(chatAdapter);

        // Initialize Gemini API
        geminiAPI = new GeminiAPI(this);

        // Set up chat input
        sendButton.setOnClickListener(v -> {
            String text = chatInput.getText().toString().trim();
            if (!text.isEmpty()) {
                processUserInput(text);
                chatInput.setText("");
            }
        });

        chatInput.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_SEND) {
                String text = chatInput.getText().toString().trim();
                if (!text.isEmpty()) {
                    processUserInput(text);
                    chatInput.setText("");
                }
                return true;
            }
            return false;
        });

        // Set up start quiz button
        startQuizButton.setOnClickListener(v -> {
            Log.d("MainActivity", "Starting quiz");
            quizMode = true;
            
            // Update button visibility
            startQuizButton.setVisibility(View.GONE);
            stopQuizButton.setVisibility(View.VISIBLE);
            
            // Start new quiz
            geminiAPI.startNewQuiz(new GeminiAPI.GeminiCallback() {
                @Override
                public void onResponse(String response) {
                    Log.d("MainActivity", "Got initial quiz response");
                    runOnUiThread(() -> {
                        // Convert bot response to Hindi
                        String botHindiText = transliterateToHindi(response);
                        addBotMessage(botHindiText, response);
                    });
                }

                @Override
                public void onError(String error) {
                    Log.e("MainActivity", "Quiz start error: " + error);
                    runOnUiThread(() -> {
                        Toast.makeText(MainActivity.this, error, Toast.LENGTH_SHORT).show();
                        // Reset buttons if there's an error
                        startQuizButton.setVisibility(View.VISIBLE);
                        stopQuizButton.setVisibility(View.GONE);
                    });
                }
            });
        });

        // Set up stop quiz button
        stopQuizButton.setOnClickListener(v -> {
            Log.d("MainActivity", "Stopping quiz");
            quizMode = false;
            
            // Add quiz stopped message
            String message = "Quiz stopped. Thank you for participating!";
            String hindiMessage = transliterateToHindi(message);
            addBotMessage(hindiMessage, message);
            
            // Update button visibility
            startQuizButton.setVisibility(View.VISIBLE);
            stopQuizButton.setVisibility(View.GONE);
        });

        // Set up settings button
        settingsButton.setOnClickListener(v -> {
            Intent intent = new Intent(this, SettingsActivity.class);
            startActivityForResult(intent, SETTINGS_REQUEST_CODE);
        });

        // Check if API key is set
        checkApiKey();

        // Add welcome message
        addBotMessage("नमस्ते! मैं आपकी कैसे मदद कर सकता हूं? क्विज़ शुरू करने के लिए 'Start Quiz' बटन दबाएं।", 
                     "Namaste! Main aapki kaise madad kar sakta hoon? Quiz shuru karne ke liye 'Start Quiz' button dabayen.");
    }

    private void checkApiKey() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        String apiKey = prefs.getString(API_KEY_PREF, "");
        // Always show dialog if API key is empty
        if (apiKey.isEmpty()) {
            Intent intent = new Intent(this, SettingsActivity.class);
            startActivityForResult(intent, SETTINGS_REQUEST_CODE);
            Toast.makeText(this, "Please set your Gemini API key to enable chat", Toast.LENGTH_LONG).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == SETTINGS_REQUEST_CODE) {
            checkApiKey();
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
        
        // Consonants
        reverseTranslitMap.put("k", "क्"); 
        reverseTranslitMap.put("kh", "ख्");
        reverseTranslitMap.put("g", "ग्"); 
        reverseTranslitMap.put("gh", "घ्");
        reverseTranslitMap.put("ng", "ङ्");
        reverseTranslitMap.put("ch", "च्"); 
        reverseTranslitMap.put("chh", "छ्");
        reverseTranslitMap.put("j", "ज्"); 
        reverseTranslitMap.put("jh", "झ्");
        reverseTranslitMap.put("ny", "ञ्");
        reverseTranslitMap.put("t", "त्"); 
        reverseTranslitMap.put("th", "थ्");
        reverseTranslitMap.put("d", "द्"); 
        reverseTranslitMap.put("dh", "ध्");
        reverseTranslitMap.put("n", "न्");
        reverseTranslitMap.put("p", "प्"); 
        reverseTranslitMap.put("ph", "फ्");
        reverseTranslitMap.put("b", "ब्"); 
        reverseTranslitMap.put("bh", "भ्");
        reverseTranslitMap.put("m", "म्");
        reverseTranslitMap.put("y", "य्");
        reverseTranslitMap.put("r", "र्");
        reverseTranslitMap.put("l", "ल्");
        reverseTranslitMap.put("v", "व्");
        reverseTranslitMap.put("w", "व्");
        reverseTranslitMap.put("sh", "श्");
        reverseTranslitMap.put("s", "स्");
        reverseTranslitMap.put("h", "ह्");
        
        // Matras (vowel marks)
        Map<String, String> matras = new HashMap<>();
        matras.put("a", "");  // Implicit 'a' sound
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
        
        // Process text word by word
        StringBuilder result = new StringBuilder();
        String[] words = englishText.toLowerCase().split("\\s+");
        
        for (String word : words) {
            StringBuilder currentWord = new StringBuilder();
            int i = 0;
            while (i < word.length()) {
                boolean found = false;
                // Try longer sequences first
                for (int len = Math.min(4, word.length() - i); len > 0 && !found; len--) {
                    String part = word.substring(i, i + len);
                    // Check for consonants
                    if (reverseTranslitMap.containsKey(part)) {
                        // If it's the last character or next character is a consonant,
                        // add full form
                        if (i + len >= word.length() || 
                            (i + len < word.length() && reverseTranslitMap.containsKey(word.substring(i + len, i + len + 1)))) {
                            currentWord.append(reverseTranslitMap.get(part).replace("्", ""));
                            i += len;
                            found = true;
                        } 
                        // Otherwise add with halant
                        else {
                            currentWord.append(reverseTranslitMap.get(part));
                            i += len;
                            found = true;
                        }
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

    private void processUserInput(String text) {
        Log.d("MainActivity", "Processing user input: " + text);
        // Convert to Hindi and add to chat
        String hindiText = transliterateToHindi(text);
        addUserMessage(hindiText, text);

        // Get response from Gemini
        geminiAPI.generateResponse(text, new GeminiAPI.GeminiCallback() {
            @Override
            public void onResponse(String response) {
                Log.d("MainActivity", "Got Gemini response");
                runOnUiThread(() -> {
                    // Check if quiz is ending
                    if (isQuizEnding(response)) {
                        Log.d("MainActivity", "Quiz is ending");
                        quizMode = false;
                    }
                    
                    // Convert bot response to Hindi
                    String botHindiText = transliterateToHindi(response);
                    addBotMessage(botHindiText, response);
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
        });
    }

    private void addBotMessage(String hindiText, String hinglishText) {
        ChatMessage message = new ChatMessage(hinglishText, hindiText, hinglishText, ChatMessage.TYPE_BOT);
        runOnUiThread(() -> {
            messages.add(message);
            chatAdapter.notifyItemInserted(messages.size() - 1);
            chatRecyclerView.smoothScrollToPosition(messages.size() - 1);
        });
    }

    private boolean isQuizEnding(String response) {
        String lowerResponse = response.toLowerCase();
        if (lowerResponse.contains("quiz is over") || 
            lowerResponse.contains("quiz has ended") ||
            lowerResponse.contains("end of quiz") ||
            lowerResponse.contains("quiz complete") ||
            lowerResponse.contains("quiz finished") ||
            lowerResponse.contains("thank you for taking the quiz")) {
            
            // Update button visibility when quiz ends
            runOnUiThread(() -> {
                startQuizButton.setVisibility(View.VISIBLE);
                stopQuizButton.setVisibility(View.GONE);
            });
            return true;
        }
        return false;
    }
}
