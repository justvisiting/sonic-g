package com.example.speechapp;

import android.os.Handler;
import android.os.Looper;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import okhttp3.*;
import org.json.JSONObject;
import java.io.IOException;

public class GeminiAPI {
    private static final String API_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-pro:generateContent";
    private static final String API_KEY = "YOUR_API_KEY"; // Replace with your API key
    private final OkHttpClient client;
    private final ExecutorService executor;
    private final Handler mainHandler;

    public interface GeminiCallback {
        void onResponse(String response);
        void onError(String error);
    }

    public GeminiAPI() {
        this.client = new OkHttpClient();
        this.executor = Executors.newSingleThreadExecutor();
        this.mainHandler = new Handler(Looper.getMainLooper());
    }

    public void generateResponse(String prompt, GeminiCallback callback) {
        executor.execute(() -> {
            try {
                JSONObject jsonBody = new JSONObject();
                JSONObject contents = new JSONObject();
                contents.put("role", "user");
                contents.put("parts", new JSONObject().put("text", prompt));
                jsonBody.put("contents", new JSONObject[]{contents});
                
                RequestBody body = RequestBody.create(
                    jsonBody.toString(),
                    MediaType.parse("application/json")
                );

                Request request = new Request.Builder()
                    .url(API_URL + "?key=" + API_KEY)
                    .post(body)
                    .build();

                try (Response response = client.newCall(request).execute()) {
                    if (!response.isSuccessful()) throw new IOException("Unexpected code " + response);

                    String responseData = response.body().string();
                    JSONObject jsonResponse = new JSONObject(responseData);
                    String generatedText = jsonResponse
                        .getJSONArray("candidates")
                        .getJSONObject(0)
                        .getJSONObject("content")
                        .getJSONArray("parts")
                        .getJSONObject(0)
                        .getString("text");

                    mainHandler.post(() -> callback.onResponse(generatedText));
                }
            } catch (Exception e) {
                mainHandler.post(() -> callback.onError(e.getMessage()));
            }
        });
    }
}
