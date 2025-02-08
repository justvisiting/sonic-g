package com.example.speechapp;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import androidx.appcompat.app.AppCompatActivity;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Locale;

public class SettingsActivity extends AppCompatActivity {
    private static final String PREFS_NAME = "SpeechAppPrefs";
    private static final String API_KEY_PREF = "gemini_api_key";
    private static final String LANGUAGE_PREF = "tts_language";
    
    private Map<String, Locale> languageMap;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        EditText apiKeyInput = findViewById(R.id.apiKeyInput);
        Button getKeyButton = findViewById(R.id.getKeyButton);
        Button saveButton = findViewById(R.id.saveButton);
        Spinner languageSpinner = findViewById(R.id.languageSpinner);

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

        // Set up language spinner
        List<String> languages = new ArrayList<>(languageMap.keySet());
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, languages);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        languageSpinner.setAdapter(adapter);

        // Load existing settings
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        String existingKey = prefs.getString(API_KEY_PREF, "");
        apiKeyInput.setText(existingKey);

        String savedLanguage = prefs.getString(LANGUAGE_PREF, "Hindi (India)");
        int languagePosition = languages.indexOf(savedLanguage);
        if (languagePosition >= 0) {
            languageSpinner.setSelection(languagePosition);
        }

        // Open API key generation page
        getKeyButton.setOnClickListener(v -> {
            Intent browserIntent = new Intent(Intent.ACTION_VIEW, 
                Uri.parse("https://aistudio.google.com/apikey"));
            startActivity(browserIntent);
        });

        // Save settings
        saveButton.setOnClickListener(v -> {
            String apiKey = apiKeyInput.getText().toString().trim();
            String selectedLanguage = (String) languageSpinner.getSelectedItem();
            
            SharedPreferences.Editor editor = prefs.edit();
            editor.putString(API_KEY_PREF, apiKey);
            editor.putString(LANGUAGE_PREF, selectedLanguage);
            editor.apply();
            
            setResult(RESULT_OK);
            finish();
        });
    }
}
