package com.example.speechapp;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import okhttp3.*;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.IOException;

public class GeminiAPI {
    private static final String API_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-pro:generateContent";
    private static final String PREFS_NAME = "SpeechAppPrefs";
    private static final String API_KEY_PREF = "gemini_api_key";
    private static final String SYSTEM_PROMPT = 
        "You are a friendly and helpful voice assistant. " +
        "Keep your responses brief, natural, and conversational - like how people speak in everyday conversations. " +
        "Use simple language and short sentences. " +
        "Respond in a way that sounds natural when spoken out loud. " +
        "If you need to explain something complex, break it down into simple terms. " +
        "Always maintain a warm and engaging tone.";
    
    private final Context context;
    private final OkHttpClient client;
    private final ExecutorService executor;
    private final Handler mainHandler;

    public interface GeminiCallback {
        void onResponse(String response);
        void onError(String error);
    }

    public GeminiAPI(Context context) {
        this.context = context;
        this.client = new OkHttpClient();
        this.executor = Executors.newSingleThreadExecutor();
        this.mainHandler = new Handler(Looper.getMainLooper());
    }

    private String getApiKey() {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getString(API_KEY_PREF, "");
    }

    public void generateResponse(String prompt, GeminiCallback callback) {
        String apiKey = getApiKey();
        if (apiKey.isEmpty()) {
            mainHandler.post(() -> callback.onError("API key not set. Please set it in settings."));
            return;
        }

        executor.execute(() -> {
            try {
                JSONObject jsonBody = new JSONObject();
                JSONArray contents = new JSONArray();
                JSONObject content = new JSONObject();
                JSONArray parts = new JSONArray();
                
                // Add system prompt and user prompt as separate parts
                JSONObject systemPart = new JSONObject();
                systemPart.put("text", SYSTEM_PROMPT + "\n\nUser: " + prompt);
                parts.put(systemPart);
                
                content.put("parts", parts);
                contents.put(content);
                jsonBody.put("contents", contents);
                
                RequestBody body = RequestBody.create(
                    jsonBody.toString(),
                    MediaType.parse("application/json")
                );

                Request request = new Request.Builder()
                    .url(API_URL + "?key=" + apiKey)
                    .post(body)
                    .build();

                try (Response response = client.newCall(request).execute()) {
                    if (!response.isSuccessful()) {
                        String errorBody = response.body() != null ? response.body().string() : "Unknown error";
                        mainHandler.post(() -> callback.onError("Error: " + response.code() + " - " + errorBody));
                        return;
                    }

                    String responseData = response.body().string();
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

                    mainHandler.post(() -> callback.onResponse(generatedText.trim()));
                }
            } catch (Exception e) {
                mainHandler.post(() -> callback.onError("Error: " + e.getMessage()));
            }
        });
    }
}
