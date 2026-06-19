package com.deepseek.chat.data.repository;

import android.content.Context;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.Transformations;

import com.deepseek.chat.data.local.AppDatabase;
import com.deepseek.chat.data.local.ChatMessageDao;
import com.deepseek.chat.data.local.ChatSessionDao;
import com.deepseek.chat.data.local.entity.ChatMessageEntity;
import com.deepseek.chat.data.local.entity.ChatSessionEntity;
import com.deepseek.chat.domain.model.ChatMessage;
import com.deepseek.chat.domain.model.ChatSession;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ChatRepository {

    private final ChatSessionDao sessionDao;
    private final ChatMessageDao messageDao;
    private final ExecutorService executor;

    public ChatRepository(Context context) {
        AppDatabase db = AppDatabase.getInstance(context);
        this.sessionDao = db.chatSessionDao();
        this.messageDao = db.chatMessageDao();
        this.executor = Executors.newSingleThreadExecutor();
    }

    public LiveData<List<ChatSession>> getAllSessions() {
        return Transformations.map(sessionDao.getAllSessions(), entities -> {
            List<ChatSession> result = new ArrayList<>();
            for (ChatSessionEntity e : entities) {
                result.add(toSession(e));
            }
            return result;
        });
    }

    public LiveData<List<ChatMessage>> getMessagesForSession(long sessionId) {
        return Transformations.map(messageDao.getMessagesForSession(sessionId), entities -> {
            List<ChatMessage> result = new ArrayList<>();
            for (ChatMessageEntity e : entities) {
                result.add(toMessage(e));
            }
            return result;
        });
    }

    public List<ChatMessage> getMessagesForSessionSync(long sessionId) {
        List<ChatMessageEntity> entities = messageDao.getMessagesForSessionSync(sessionId);
        List<ChatMessage> result = new ArrayList<>();
        for (ChatMessageEntity e : entities) {
            result.add(toMessage(e));
        }
        return result;
    }

    public void createSession(String title, String modelId, Callback<Long> callback) {
        executor.execute(() -> {
            long now = System.currentTimeMillis();
            ChatSessionEntity entity = new ChatSessionEntity(title, modelId, now, now);
            long id = sessionDao.insert(entity);
            callback.onResult(id);
        });
    }

    public void updateSessionTitle(long sessionId, String title) {
        executor.execute(() -> sessionDao.updateTitle(sessionId, title, System.currentTimeMillis()));
    }

    public void deleteSession(long sessionId) {
        executor.execute(() -> {
            ChatSessionEntity entity = sessionDao.getSessionById(sessionId);
            if (entity != null) {
                sessionDao.delete(entity);
            }
        });
    }

    public void insertMessage(long sessionId, String role, String content, String reasoningContent, Callback<Long> callback) {
        executor.execute(() -> {
            ChatMessageEntity entity = new ChatMessageEntity(
                    sessionId, role, content, reasoningContent, System.currentTimeMillis());
            long id = messageDao.insert(entity);
            sessionDao.updateTimestamp(sessionId, System.currentTimeMillis());
            callback.onResult(id);
        });
    }

    public void updateMessage(long messageId, String content, String reasoningContent) {
        executor.execute(() -> {
            ChatMessageEntity entity = messageDao.getMessageById(messageId);
            if (entity != null) {
                entity.setContent(content);
                entity.setReasoningContent(reasoningContent);
                messageDao.update(entity);
            }
        });
    }

    public void deleteMessage(long messageId) {
        executor.execute(() -> messageDao.deleteMessageById(messageId));
    }

    public void deleteMessagesForSession(long sessionId) {
        executor.execute(() -> messageDao.deleteMessagesForSession(sessionId));
    }

    public LiveData<List<ChatMessage>> searchMessages(String query) {
        return Transformations.map(messageDao.searchMessages(query), entities -> {
            List<ChatMessage> result = new ArrayList<>();
            for (ChatMessageEntity e : entities) {
                result.add(toMessage(e));
            }
            return result;
        });
    }

    private ChatSession toSession(ChatSessionEntity e) {
        return new ChatSession(e.getId(), e.getTitle(), e.getModelId(), e.getCreatedAt(), e.getUpdatedAt());
    }

    private ChatMessage toMessage(ChatMessageEntity e) {
        return new ChatMessage(e.getId(), e.getSessionId(), e.getRole(), e.getContent(), e.getReasoningContent(), e.getCreatedAt());
    }

    public interface Callback<T> {
        void onResult(T result);
    }
}
