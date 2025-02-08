package com.example.speechapp;

public class ChatMessage {
    public static final int TYPE_USER = 0;
    public static final int TYPE_BOT = 1;

    private String message;
    private String hindiText;
    private String hinglishText;
    private int type;
    private long timestamp;

    public ChatMessage(String message, String hindiText, String hinglishText, int type) {
        this.message = message;
        this.hindiText = hindiText;
        this.hinglishText = hinglishText;
        this.type = type;
        this.timestamp = System.currentTimeMillis();
    }

    public String getMessage() {
        return message;
    }

    public String getHindiText() {
        return hindiText;
    }

    public String getHinglishText() {
        return hinglishText;
    }

    public int getType() {
        return type;
    }

    public long getTimestamp() {
        return timestamp;
    }
}
