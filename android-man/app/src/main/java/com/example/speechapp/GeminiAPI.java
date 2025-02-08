package com.example.speechapp;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class GeminiAPI {
    private static final String TAG = "GeminiAPI";
    private static final String API_KEY_PREF = "gemini_api_key";
    private static final String QUIZ_PROMPT = 
        "You are a friendly quiz master. Start a fun quiz about general knowledge. " +
        "Ask one question at a time. Wait for the user's answer before proceeding to the next question. " +
        "Give encouraging feedback for both correct and incorrect answers. After 5 questions, end the quiz with a summary of performance.\n\n";

    private final Context context;
    private final ExecutorService executor;
    private final Handler mainHandler;
    private StringBuilder conversationHistory;
    private String selectedLanguage = "english";
    private final DebugLogFragment debugLogFragment;
    private final OkHttpClient client;

    public GeminiAPI(Context context) {
        this(context, null);
    }

    public GeminiAPI(Context context, DebugLogFragment debugLogFragment) {
        this.context = context;
        this.debugLogFragment = debugLogFragment;
        this.executor = Executors.newSingleThreadExecutor();
        this.mainHandler = new Handler(Looper.getMainLooper());
        this.conversationHistory = new StringBuilder();
        this.client = new OkHttpClient();
    }

    public void setLanguage(String language) {
        this.selectedLanguage = language;
    }

    private String getLanguageInstruction() {
        switch (selectedLanguage) {
            case "hindi":
                return "Please respond in simple English that can be easily transliterated to Hindi. " +
                       "Use short, clear sentences and avoid complex words.";
            case "hinglish":
                return "Please respond in Hinglish (mix of Hindi and English). " +
                       "Use casual language with common Hindi words mixed with English. " +
                       "For example: 'Kya plan hai weekend ka?' or 'Movie kaisi thi?'";
            default: // english
                return "Please respond in clear English.";
        }
    }

    private String getApiKey() {
        SharedPreferences prefs = context.getSharedPreferences("SpeechAppPrefs", Context.MODE_PRIVATE);
        return prefs.getString(API_KEY_PREF, "");
    }

    public void startNewQuiz(GeminiCallback callback) {
        conversationHistory = new StringBuilder();
        String languagePrompt = getLanguageInstruction();
        String prompt = "You are a friendly quiz master. " + languagePrompt + " " +
                "Start a fun quiz about general knowledge. Ask one question at a time. " +
                "Wait for the user's answer before proceeding to the next question. " +
                "Give encouraging feedback for both correct and incorrect answers. " +
                "After 5 questions, end the quiz with a summary of performance.";
        generateResponse("start quiz", callback);
    }

    public void generateResponse(String userInput, GeminiCallback callback) {
        String apiKey = getApiKey();
        if (apiKey.isEmpty()) {
            if (callback != null) {
                callback.onError("API key not set. Please set it in settings.");
            }
            return;
        }

        if (debugLogFragment != null) {
            debugLogFragment.appendLog("User Input: " + userInput);
        }

        executor.execute(() -> {
            try {
                JSONObject requestBody = new JSONObject();
                JSONArray contents = new JSONArray();
                JSONObject content = new JSONObject();
                JSONArray parts = new JSONArray();

                // Add system prompt with language instruction
                JSONObject systemPart = new JSONObject();
                String systemPrompt = userInput.equals("start quiz") ? 
                    "Start a new quiz with a friendly introduction and first question only." :
                    QUIZ_PROMPT + getLanguageInstruction() + "\n\n" + conversationHistory.toString();
                systemPart.put("role", "user");
                systemPart.put("parts", new JSONArray().put(new JSONObject().put("text", systemPrompt)));
                contents.put(systemPart);

                // Add user input
                JSONObject userPart = new JSONObject();
                userPart.put("role", "user");
                userPart.put("parts", new JSONArray().put(new JSONObject().put("text", userInput)));
                contents.put(userPart);

                requestBody.put("contents", contents);
                requestBody.put("safetySettings", new JSONArray());
                requestBody.put("generationConfig", new JSONObject()
                    .put("temperature", 0.7)
                    .put("topK", 1)
                    .put("topP", 1)
                    .put("maxOutputTokens", 800));

                String url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-pro:generateContent?key=" + apiKey;
                
                // Log the full request details
                if (debugLogFragment != null) {
                    debugLogFragment.appendLog("\n=== REQUEST DETAILS ===");
                    debugLogFragment.appendLog("URL: " + url.replace(apiKey, "[API_KEY]"));
                    debugLogFragment.appendLog("System Prompt: " + systemPrompt);
                    debugLogFragment.appendLog("User Input: " + userInput);
                    debugLogFragment.appendLog("Language Mode: " + selectedLanguage);
                    debugLogFragment.appendLog("Full Request Body: " + requestBody.toString(2));
                    debugLogFragment.appendLog("=== END REQUEST ===\n");
                }

                Request request = new Request.Builder()
                    .url(url)
                    .post(RequestBody.create(requestBody.toString(), MediaType.parse("application/json")))
                    .build();

                try (Response response = client.newCall(request).execute()) {
                    String responseBody = response.body().string();
                    
                    // Log the full response
                    if (debugLogFragment != null) {
                        debugLogFragment.appendLog("\n=== RESPONSE DETAILS ===");
                        debugLogFragment.appendLog("Response Code: " + response.code());
                        debugLogFragment.appendLog("Response Message: " + response.message());
                        debugLogFragment.appendLog("Response Body: " + responseBody);
                        debugLogFragment.appendLog("=== END RESPONSE ===\n");
                    }

                    if (!response.isSuccessful()) {
                        throw new Exception("HTTP " + response.code() + ": " + responseBody);
                    }

                    JSONObject jsonResponse = new JSONObject(responseBody);
                    String generatedText = jsonResponse
                        .getJSONArray("candidates")
                        .getJSONObject(0)
                        .getJSONObject("content")
                        .getJSONArray("parts")
                        .getJSONObject(0)
                        .getString("text");

                    // Update conversation history
                    conversationHistory.append("User: ").append(userInput).append("\n");
                    conversationHistory.append("Assistant: ").append(generatedText).append("\n\n");

                    mainHandler.post(() -> {
                        if (callback != null) {
                            callback.onResponse(generatedText);
                        }
                    });
                }
            } catch (Exception e) {
                Log.e(TAG, "Error generating response", e);
                if (debugLogFragment != null) {
                    debugLogFragment.appendLog("\n=== ERROR DETAILS ===");
                    debugLogFragment.appendLog("Error Type: " + e.getClass().getSimpleName());
                    debugLogFragment.appendLog("Error Message: " + e.getMessage());
                    
                    // Get the full stack trace
                    StringWriter sw = new StringWriter();
                    PrintWriter pw = new PrintWriter(sw);
                    e.printStackTrace(pw);
                    debugLogFragment.appendLog("Stack Trace:\n" + sw.toString());
                    debugLogFragment.appendLog("=== END ERROR ===\n");
                }
                mainHandler.post(() -> {
                    if (callback != null) {
                        callback.onError("Error: " + e.getMessage());
                    }
                });
            }
        });
    }

    public interface GeminiCallback {
        void onResponse(String response);
        void onError(String error);
    }
}
