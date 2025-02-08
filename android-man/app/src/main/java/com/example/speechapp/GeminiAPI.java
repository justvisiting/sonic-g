package com.example.speechapp;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import okhttp3.*;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.IOException;

public class GeminiAPI {
    private static final String TAG = "GeminiAPI";
    private static final String API_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-pro:generateContent";
    private static final String PREFS_NAME = "SpeechAppPrefs";
    private static final String API_KEY_PREF = "gemini_api_key";
    private static final String QUIZ_PROMPT = 
        "You are a friendly quiz master. Follow these rules strictly:\n" +
        "1. If this is the start (no previous context), introduce yourself briefly and ask the first question.\n" +
        "2. If this is a user's answer:\n" +
        "   - Evaluate their answer\n" +
        "   - Give encouraging feedback\n" +
        "   - Ask if they want to move to the next question (say 'yes' to continue)\n" +
        "3. If user says 'yes' after feedback:\n" +
        "   - Ask the next question\n" +
        "4. Keep questions varied but simple enough to answer verbally\n" +
        "5. Keep all responses brief and conversational\n" +
        "6. Track question number and mention it (e.g., 'Question 3:')\n\n" +
        "Previous conversation context:\n";
    
    private final Context context;
    private final OkHttpClient client;
    private final ExecutorService executor;
    private final Handler mainHandler;
    private StringBuilder conversationHistory;

    public interface GeminiCallback {
        void onResponse(String response);
        void onError(String error);
    }

    public GeminiAPI(Context context) {
        this.context = context;
        this.client = new OkHttpClient();
        this.executor = Executors.newSingleThreadExecutor();
        this.mainHandler = new Handler(Looper.getMainLooper());
        this.conversationHistory = new StringBuilder();
    }

    private String getApiKey() {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getString(API_KEY_PREF, "");
    }

    public void startNewQuiz() {
        conversationHistory = new StringBuilder();
        generateResponse("start quiz", null);
    }

    public void generateResponse(String prompt, GeminiCallback callback) {
        String apiKey = getApiKey();
        if (apiKey.isEmpty()) {
            mainHandler.post(() -> callback.onError("API key not set. Please set it in settings."));
            return;
        }

        executor.execute(() -> {
            try {
                // Update conversation history
                conversationHistory.append("User: ").append(prompt).append("\n");

                JSONObject jsonBody = new JSONObject();
                JSONArray contents = new JSONArray();
                JSONObject content = new JSONObject();
                JSONArray parts = new JSONArray();
                
                // Add quiz prompt and conversation history
                JSONObject systemPart = new JSONObject();
                systemPart.put("text", QUIZ_PROMPT + conversationHistory.toString());
                parts.put(systemPart);
                
                content.put("parts", parts);
                contents.put(content);
                jsonBody.put("contents", contents);
                
                String requestBody = jsonBody.toString(2); // Pretty print JSON
                Log.d(TAG, "Request to Gemini API:\n" + requestBody);
                
                RequestBody body = RequestBody.create(
                    requestBody,
                    MediaType.parse("application/json")
                );

                Request request = new Request.Builder()
                    .url(API_URL + "?key=" + apiKey)
                    .post(body)
                    .build();

                try (Response response = client.newCall(request).execute()) {
                    String responseData = response.body() != null ? response.body().string() : "";
                    Log.d(TAG, "Response from Gemini API:\n" + responseData);

                    if (!response.isSuccessful()) {
                        mainHandler.post(() -> callback.onError("Error: " + response.code() + " - " + responseData));
                        return;
                    }

                    JSONObject jsonResponse = new JSONObject(responseData);
                    
                    if (!jsonResponse.has("candidates") || jsonResponse.getJSONArray("candidates").length() == 0) {
                        mainHandler.post(() -> callback.onError("No response from API"));
                        return;
                    }

                    String generatedText = jsonResponse
                        .getJSONArray("candidates")
                        .getJSONObject(0)
                        .getJSONObject("content")
                        .getJSONArray("parts")
                        .getJSONObject(0)
                        .getString("text");

                    // Update conversation history with response
                    conversationHistory.append("Assistant: ").append(generatedText).append("\n");

                    if (callback != null) {
                        mainHandler.post(() -> callback.onResponse(generatedText.trim()));
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Error calling Gemini API", e);
                if (callback != null) {
                    mainHandler.post(() -> callback.onError("Error: " + e.getMessage()));
                }
            }
        });
    }
}
