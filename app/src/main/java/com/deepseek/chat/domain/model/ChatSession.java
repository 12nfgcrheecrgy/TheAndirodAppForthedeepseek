package com.deepseek.chat.domain.model;

public class ChatSession {

    private final long id;
    private String title;
    private final String modelId;
    private final long createdAt;
    private long updatedAt;

    public ChatSession(long id, String title, String modelId, long createdAt, long updatedAt) {
        this.id = id;
        this.title = title;
        this.modelId = modelId;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public long getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getModelId() {
        return modelId;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public long getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(long updatedAt) {
        this.updatedAt = updatedAt;
    }
}
