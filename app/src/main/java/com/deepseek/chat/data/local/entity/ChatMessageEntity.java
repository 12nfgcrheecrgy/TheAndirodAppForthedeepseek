package com.deepseek.chat.data.local.entity;

import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(
    tableName = "chat_messages",
    foreignKeys = @ForeignKey(
        entity = ChatSessionEntity.class,
        parentColumns = "id",
        childColumns = "sessionId",
        onDelete = ForeignKey.CASCADE
    ),
    indices = {@Index("sessionId")}
)
public class ChatMessageEntity {

    @PrimaryKey(autoGenerate = true)
    private long id;

    private long sessionId;
    private String role;
    private String content;
    private String reasoningContent;
    private long createdAt;

    public ChatMessageEntity(long sessionId, String role, String content, String reasoningContent, long createdAt) {
        this.sessionId = sessionId;
        this.role = role;
        this.content = content;
        this.reasoningContent = reasoningContent;
        this.createdAt = createdAt;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public long getSessionId() {
        return sessionId;
    }

    public void setSessionId(long sessionId) {
        this.sessionId = sessionId;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getReasoningContent() {
        return reasoningContent;
    }

    public void setReasoningContent(String reasoningContent) {
        this.reasoningContent = reasoningContent;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }
}
