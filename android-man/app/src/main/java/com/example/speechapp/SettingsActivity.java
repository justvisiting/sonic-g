package com.example.speechapp;

import android.content.SharedPreferences;
import android.os.Bundle;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;
import androidx.preference.SwitchPreference;
import androidx.appcompat.app.AppCompatActivity;

public class SettingsActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getSupportFragmentManager()
                .beginTransaction()
                .replace(android.R.id.content, new SettingsFragment())
                .commit();
    }

    public static class SettingsFragment extends PreferenceFragmentCompat implements SharedPreferences.OnSharedPreferenceChangeListener {
        private GeminiAPI geminiAPI;

        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.preferences, rootKey);
            geminiAPI = new GeminiAPI(requireContext());

            // Initialize quiz mode preference
            SwitchPreference quizPref = findPreference("quiz_mode");
            if (quizPref != null) {
                quizPref.setChecked(geminiAPI.isQuizMode());
            }

            // Update language preference summary when changed
            Preference languagePref = findPreference("language");
            if (languagePref != null) {
                languagePref.setOnPreferenceChangeListener((preference, newValue) -> {
                    preference.setSummary(newValue.toString());
                    return true;
                });
                
                // Set initial summary
                SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(requireContext());
                languagePref.setSummary(prefs.getString("language", "english"));
            }
        }

        @Override
        public void onResume() {
            super.onResume();
            getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
        }

        @Override
        public void onPause() {
            super.onPause();
            getPreferenceScreen().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
        }

        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
            if (key.equals("api_key")) {
                String apiKey = sharedPreferences.getString(key, "");
                geminiAPI.setApiKey(apiKey);
            } else if (key.equals("quiz_mode")) {
                boolean quizMode = sharedPreferences.getBoolean(key, false);
                geminiAPI.setQuizMode(quizMode);
            } else if (key.equals("language")) {
                String language = sharedPreferences.getString(key, "english");
                geminiAPI.setLanguage(language);
            }
        }
    }
}
