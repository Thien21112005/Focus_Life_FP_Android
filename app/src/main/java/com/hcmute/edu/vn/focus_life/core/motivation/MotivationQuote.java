package com.hcmute.edu.vn.focus_life.core.motivation;

public class MotivationQuote {
    private final String text;
    private final String author;

    public MotivationQuote(String text, String author) {
        this.text = text;
        this.author = author;
    }

    public String getText() {
        return text;
    }

    public String getAuthor() {
        return author;
    }
}
