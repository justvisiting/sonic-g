package com.example.speechapp;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import androidx.appcompat.app.AppCompatActivity;

public class SettingsActivity extends AppCompatActivity {
    private static final String PREFS_NAME = "SpeechAppPrefs";
    private static final String API_KEY_PREF = "gemini_api_key";
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        EditText apiKeyInput = findViewById(R.id.apiKeyInput);
        Button getKeyButton = findViewById(R.id.getKeyButton);
        Button saveButton = findViewById(R.id.saveButton);

        // Load existing API key if any
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        String existingKey = prefs.getString(API_KEY_PREF, "");
        apiKeyInput.setText(existingKey);

        // Open API key generation page
        getKeyButton.setOnClickListener(v -> {
            Intent browserIntent = new Intent(Intent.ACTION_VIEW, 
                Uri.parse("https://aistudio.google.com/apikey"));
            startActivity(browserIntent);
        });

        // Save API key
        saveButton.setOnClickListener(v -> {
            String apiKey = apiKeyInput.getText().toString().trim();
            SharedPreferences.Editor editor = prefs.edit();
            editor.putString(API_KEY_PREF, apiKey);
            editor.apply();
            setResult(RESULT_OK);
            finish();
        });
    }
}
