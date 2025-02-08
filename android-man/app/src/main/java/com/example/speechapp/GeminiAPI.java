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
    private static final String DEBUG_MODE_PREF = "debug_mode";
    private static final String QUIZ_PROMPT = 
        "You are a friendly quiz master. Follow these rules strictly:\n" +
        "1. Wait for user input before generating the next question\n" +
        "2. For user answers:\n" +
        "   - Evaluate their answer\n" +
        "   - Give encouraging feedback\n" +
        "   - Wait for explicit user confirmation before next question\n" +
        "3. Only ask the next question when user explicitly says 'next' or 'yes'\n" +
        "4. Keep questions varied but simple enough to answer verbally\n" +
        "5. Keep all responses brief and conversational\n" +
        "6. Track question number and mention it (e.g., 'Question 3:')\n" +
        "7. NEVER generate both question and answer in the same response\n" +
        "8. After 5 questions, end the quiz with a friendly message containing one of these phrases:\n" +
        "   - 'Quiz is over'\n" +
        "   - 'Thank you for taking the quiz'\n" +
        "   - 'Quiz complete'\n\n" +
        "Current conversation:\n";
    
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

    public void startNewQuiz(GeminiCallback callback) {
        conversationHistory = new StringBuilder();
        generateResponse("start quiz", callback);
    }

    public void generateResponse(String prompt, GeminiCallback callback) {
        String apiKey = getApiKey();
        if (apiKey.isEmpty()) {
            mainHandler.post(() -> callback.onError("API key not set. Please set it in settings."));
            return;
        }

        executor.execute(() -> {
            try {
                // Only add user input to history if it's not a system command
                if (!prompt.equals("start quiz")) {
                    conversationHistory.append("User: ").append(prompt).append("\n");
                }

                JSONObject jsonBody = new JSONObject();
                JSONArray contents = new JSONArray();
                JSONObject content = new JSONObject();
                JSONArray parts = new JSONArray();
                
                // Add quiz prompt and conversation history
                JSONObject systemPart = new JSONObject();
                String fullPrompt = prompt.equals("start quiz") ? 
                    "Start a new quiz with a friendly introduction and first question only." :
                    QUIZ_PROMPT + conversationHistory.toString();
                systemPart.put("text", fullPrompt);
                parts.put(systemPart);
                
                content.put("parts", parts);
                contents.put(content);
                jsonBody.put("contents", contents);
                
                String requestBody = jsonBody.toString(2); // Pretty print JSON
                
                // Check if debug mode is enabled
                SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
                boolean debugMode = prefs.getBoolean(DEBUG_MODE_PREF, false);
                
                if (debugMode) {
                    String debugRequest = "REQUEST:\n" + prompt;
                    ((MainActivity) context).runOnUiThread(() -> {
                        ((MainActivity) context).addDebugLog(debugRequest);
                    });
                }
                
                RequestBody body = RequestBody.create(
                    requestBody,
                    MediaType.parse("application/json; charset=utf-8")
                );

                Request request = new Request.Builder()
                    .url(API_URL + "?key=" + apiKey)
                    .post(body)
                    .build();

                try (Response response = client.newCall(request).execute()) {
                    String responseBody = response.body().string();
                    
                    if (!response.isSuccessful()) {
                        throw new IOException("Unexpected response " + response);
                    }

                    if (debugMode) {
                        String debugResponse = "RESPONSE:\n" + responseBody;
                        ((MainActivity) context).runOnUiThread(() -> {
                            ((MainActivity) context).addDebugLog(debugResponse);
                        });
                    }

                    JSONObject jsonResponse = new JSONObject(responseBody);
                    String generatedText = jsonResponse
                        .getJSONArray("candidates")
                        .getJSONObject(0)
                        .getJSONObject("content")
                        .getJSONArray("parts")
                        .getJSONObject(0)
                        .getString("text");

                    // Add bot response to history
                    conversationHistory.append("Assistant: ").append(generatedText).append("\n");
                    mainHandler.post(() -> callback.onResponse(generatedText));
                }
            } catch (Exception e) {
                String error = "ERROR:\n" + e.getMessage();
                ((MainActivity) context).runOnUiThread(() -> {
                    ((MainActivity) context).addDebugLog(error);
                });
                Log.e(TAG, "Error: " + e.getMessage());
                mainHandler.post(() -> callback.onError(e.getMessage()));
            }
        });
    }
}
