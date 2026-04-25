package com.hcmute.edu.vn.focus_life.ui.ai;

public class ChatMessage {
    public static final int TYPE_USER = 1;
    public static final int TYPE_AI = 2;

    private String content;
    private int type;
    private long timestamp;

    public ChatMessage(String content, int type) {
        this.content = content;
        this.type = type;
        this.timestamp = System.currentTimeMillis();
    }

    public String getContent() { return content; }
    public int getType() { return type; }
    public long getTimestamp() { return timestamp; }
}