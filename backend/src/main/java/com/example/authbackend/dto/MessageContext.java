package com.example.authbackend.dto;

import java.util.Map;

public class MessageContext {
    public static final String TYPE_TEXT = "text";
    public static final String TYPE_VOICE = "voice";

    private String role;
    private String content;
    private String messageType = TYPE_TEXT;
    private Map<String, Object> audioFeatures;

    public MessageContext() {}

    public MessageContext(String role, String content) {
        this.role = role;
        this.content = content;
        this.messageType = TYPE_TEXT;
    }

    public MessageContext(String role, String content, String messageType) {
        this.role = role;
        this.content = content;
        this.messageType = messageType;
    }

    public MessageContext(String role, String content, String messageType, Map<String, Object> audioFeatures) {
        this.role = role;
        this.content = content;
        this.messageType = messageType;
        this.audioFeatures = audioFeatures;
    }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public String getMessageType() { return messageType; }
    public void setMessageType(String messageType) { this.messageType = messageType; }

    public Map<String, Object> getAudioFeatures() { return audioFeatures; }
    public void setAudioFeatures(Map<String, Object> audioFeatures) { this.audioFeatures = audioFeatures; }

    public boolean isVoice() { return TYPE_VOICE.equals(messageType); }
    public boolean isText() { return TYPE_TEXT.equals(messageType); }
}
