package com.example.speechapp;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.viewpager2.widget.ViewPager2;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    private static final int SETTINGS_REQUEST_CODE = 125;
    private static final String PREFS_NAME = "SpeechAppPrefs";
    private static final String API_KEY_PREF = "gemini_api_key";
    private static final String DEBUG_MODE_PREF = "debug_mode";
    private static final String LANGUAGE_PREF = "language";

    private EditText chatInput;
    private ImageButton sendButton;
    private boolean quizMode = false;
    private boolean quizPaused = false;
    private GeminiAPI geminiAPI;
    private Handler mainHandler;
    private Menu optionsMenu;
    private long lastEnterTime = 0;
    private static final long DOUBLE_ENTER_THRESHOLD = 500; // milliseconds
    private boolean lastKeyWasEnter = false;
    private String currentLanguage = "english";

    private TabLayout tabLayout;
    private ViewPager2 viewPager;
    private ChatFragment chatFragment;
    private DebugLogFragment debugFragment;
    private boolean isDebugMode = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Load preferences
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        currentLanguage = prefs.getString(LANGUAGE_PREF, "english");
        
        setContentView(R.layout.activity_main);
        mainHandler = new Handler(Looper.getMainLooper());

        // Set up toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }

        // Initialize views
        chatInput = findViewById(R.id.chatInput);
        sendButton = findViewById(R.id.sendButton);
        tabLayout = findViewById(R.id.tabLayout);
        viewPager = findViewById(R.id.viewPager);

        // Initialize fragments
        chatFragment = new ChatFragment();
        debugFragment = new DebugLogFragment();

        // Set up ViewPager
        ViewPagerAdapter pagerAdapter = new ViewPagerAdapter(this, chatFragment, debugFragment);
        viewPager.setAdapter(pagerAdapter);

        // Set up TabLayout
        new TabLayoutMediator(tabLayout, viewPager,
            (tab, position) -> tab.setText(position == 0 ? "Chat" : "Debug")
        ).attach();

        // Check debug mode
        isDebugMode = prefs.getBoolean(DEBUG_MODE_PREF, false);
        Log.d(TAG, "Debug mode is: " + isDebugMode);
        
        // Always show tabs, but only show debug tab if debug mode is enabled
        updateDebugTabVisibility();

        // Initialize Gemini API with language preference
        geminiAPI = new GeminiAPI(this, debugFragment);
        geminiAPI.setLanguage(currentLanguage);

        // Set up chat input
        chatInput.setOnEditorActionListener((v, actionId, event) -> {
            if (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER) {
                long currentTime = System.currentTimeMillis();
                if (lastKeyWasEnter && currentTime - lastEnterTime < DOUBLE_ENTER_THRESHOLD) {
                    // Double enter detected
                    String text = chatInput.getText().toString().trim();
                    if (!text.isEmpty()) {
                        processUserInput(text);
                        chatInput.setText("");
                        lastKeyWasEnter = false;
                        return true;
                    }
                }
                lastKeyWasEnter = true;
                lastEnterTime = currentTime;
            } else {
                lastKeyWasEnter = false;
            }
            return false;
        });

        sendButton.setOnClickListener(v -> {
            String text = chatInput.getText().toString().trim();
            if (!text.isEmpty()) {
                processUserInput(text);
                chatInput.setText("");
            }
        });

        // Check if API key is set
        checkApiKey();

        // Add welcome message
        String message = "नमस्ते! मैं आपकी कैसे मदद कर सकता हूं? क्विज़ शुरू करने के लिए 'Start Quiz' बटन दबाएं।";
        String hindiMessage = transliterateToHindi(message);
        addBotMessage(hindiMessage, message);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.toolbar_menu, menu);
        this.optionsMenu = menu;
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == R.id.action_settings) {
            Intent intent = new Intent(this, SettingsActivity.class);
            startActivityForResult(intent, SETTINGS_REQUEST_CODE);
            return true;
        } else if (itemId == R.id.action_start_quiz) {
            startQuiz();
            return true;
        } else if (itemId == R.id.action_pause_quiz) {
            pauseQuiz();
            return true;
        } else if (itemId == R.id.action_resume_quiz) {
            resumeQuiz();
            return true;
        } else if (itemId == R.id.action_stop_quiz) {
            stopQuiz();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void updateQuizMenuItems(String activeState) {
        if (optionsMenu == null) return;

        optionsMenu.findItem(R.id.action_start_quiz).setVisible(activeState.equals("stopped"));
        optionsMenu.findItem(R.id.action_pause_quiz).setVisible(activeState.equals("running"));
        optionsMenu.findItem(R.id.action_resume_quiz).setVisible(activeState.equals("paused"));
        optionsMenu.findItem(R.id.action_stop_quiz).setVisible(activeState.equals("running") || activeState.equals("paused"));
    }

    private void startQuiz() {
        Log.d(TAG, "Starting quiz");
        quizMode = true;
        quizPaused = false;
        
        // Update menu items
        updateQuizMenuItems("running");
        
        // Start new quiz
        geminiAPI.startNewQuiz(new GeminiAPI.GeminiCallback() {
            @Override
            public void onResponse(String response) {
                Log.d(TAG, "Got initial quiz response");
                runOnUiThread(() -> {
                    // Convert bot response to Hindi
                    String botHindiText = transliterateToHindi(response);
                    addBotMessage(botHindiText, response);
                });
            }

            @Override
            public void onError(String error) {
                Log.e(TAG, "Quiz start error: " + error);
                runOnUiThread(() -> {
                    Toast.makeText(MainActivity.this, error, Toast.LENGTH_SHORT).show();
                    // Reset menu items if there's an error
                    updateQuizMenuItems("stopped");
                });
            }
        });
    }

    private void pauseQuiz() {
        Log.d(TAG, "Pausing quiz");
        quizPaused = true;
        
        // Update menu items
        updateQuizMenuItems("paused");
        
        // Add pause message
        String message = "Quiz paused. Click Resume when you're ready to continue.";
        String hindiMessage = transliterateToHindi(message);
        addBotMessage(hindiMessage, message);
    }

    private void resumeQuiz() {
        Log.d(TAG, "Resuming quiz");
        quizPaused = false;
        
        // Update menu items
        updateQuizMenuItems("running");
        
        // Add resume message
        String message = "Quiz resumed. Let's continue!";
        String hindiMessage = transliterateToHindi(message);
        addBotMessage(hindiMessage, message);
    }

    private void stopQuiz() {
        Log.d(TAG, "Stopping quiz");
        quizMode = false;
        quizPaused = false;
        
        // Update menu items
        updateQuizMenuItems("stopped");
        
        // Add quiz stopped message
        String message = "Quiz stopped. Thank you for participating!";
        String hindiMessage = transliterateToHindi(message);
        addBotMessage(hindiMessage, message);
    }

    private void processUserInput(String text) {
        if (text.trim().isEmpty()) {
            return;
        }

        // Don't process input if quiz is paused
        if (quizMode && quizPaused) {
            String message = "Quiz is paused. Please resume the quiz to continue.";
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
            return;
        }

        // Add user message based on language preference
        switch (currentLanguage) {
            case "hindi":
                addUserMessage(transliterateToHindi(text), text);
                break;
            case "hinglish":
                addUserMessage(text, text); // Keep original Hinglish text
                break;
            default: // english
                addUserMessage(text, text);
                break;
        }

        // Get response from Gemini
        geminiAPI.generateResponse(text, new GeminiAPI.GeminiCallback() {
            @Override
            public void onResponse(String response) {
                Log.d(TAG, "Got Gemini response");
                runOnUiThread(() -> {
                    // Check if quiz is ending
                    if (isQuizEnding(response)) {
                        Log.d(TAG, "Quiz is ending");
                        quizMode = false;
                        quizPaused = false;
                        updateQuizMenuItems("stopped");
                    }
                    
                    // Process response based on language preference
                    switch (currentLanguage) {
                        case "hindi":
                            addBotMessage(transliterateToHindi(response), response);
                            break;
                        case "hinglish":
                            addBotMessage(convertToHinglish(response), response);
                            break;
                        default: // english
                            addBotMessage(response, response);
                            break;
                    }
                });
            }

            @Override
            public void onError(String error) {
                Log.e(TAG, "Gemini response error: " + error);
                runOnUiThread(() -> {
                    Toast.makeText(MainActivity.this, error, Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private String convertToHinglish(String text) {
        // Simple Hinglish conversion rules
        return text.replaceAll("Hello", "Helo")
                  .replaceAll("Please", "Plz")
                  .replaceAll("Thank you", "Thanku")
                  .replaceAll("Good", "Gud")
                  .replaceAll("What", "Wat")
                  .replaceAll("You", "U")
                  .replaceAll("Are", "R")
                  .replaceAll("Why", "Y")
                  .replaceAll("Okay", "Ok");
    }

    private void addUserMessage(String displayText, String originalText) {
        chatFragment.addMessage(new ChatMessage(displayText, originalText, "user", ChatMessage.TYPE_USER));
    }

    private void addBotMessage(String displayText, String originalText) {
        chatFragment.addMessage(new ChatMessage(displayText, originalText, "assistant", ChatMessage.TYPE_BOT));
    }

    public void addDebugLog(String log) {
        Log.d(TAG, "Adding debug log: " + log);
        if (isDebugMode && debugFragment != null) {
            runOnUiThread(() -> debugFragment.addLog(log));
        }
    }

    private void checkApiKey() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        String apiKey = prefs.getString(API_KEY_PREF, "");
        if (apiKey.isEmpty()) {
            Toast.makeText(this, "Please set your API key in settings", Toast.LENGTH_LONG).show();
        }
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
                if (optionsMenu != null) {
                    optionsMenu.findItem(R.id.action_start_quiz).setVisible(true);
                    optionsMenu.findItem(R.id.action_stop_quiz).setVisible(false);
                }
            });
            return true;
        }
        return false;
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
        matras.put("oo", "ू"); matras.put("ū", "ū");
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

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == SETTINGS_REQUEST_CODE) {
            // Reload preferences
            SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
            String newLanguage = prefs.getString(LANGUAGE_PREF, "english");
            if (!newLanguage.equals(currentLanguage)) {
                currentLanguage = newLanguage;
                // Update Gemini API language
                geminiAPI.setLanguage(currentLanguage);
                // Add language change message
                String message = "Switched to " + currentLanguage + " mode";
                switch (currentLanguage) {
                    case "hindi":
                        addBotMessage(transliterateToHindi(message), message);
                        break;
                    case "hinglish":
                        addBotMessage(convertToHinglish(message), message);
                        break;
                    default:
                        addBotMessage(message, message);
                        break;
                }
            }
            
            // Check debug mode changes
            boolean newDebugMode = prefs.getBoolean(DEBUG_MODE_PREF, false);
            if (newDebugMode != isDebugMode) {
                isDebugMode = newDebugMode;
                updateDebugTabVisibility();
            }
        }
    }

    private void updateDebugTabVisibility() {
        if (!isDebugMode) {
            viewPager.setCurrentItem(0);
            tabLayout.getTabAt(1).view.setVisibility(View.GONE);
        } else {
            tabLayout.getTabAt(1).view.setVisibility(View.VISIBLE);
        }
    }
}
