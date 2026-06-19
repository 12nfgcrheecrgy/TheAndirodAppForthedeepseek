package com.deepseek.chat.data.local;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.deepseek.chat.data.local.entity.ChatSessionEntity;

import java.util.List;

@Dao
public interface ChatSessionDao {

    @Insert
    long insert(ChatSessionEntity session);

    @Update
    void update(ChatSessionEntity session);

    @Delete
    void delete(ChatSessionEntity session);

    @Query("SELECT * FROM chat_sessions ORDER BY updatedAt DESC")
    LiveData<List<ChatSessionEntity>> getAllSessions();

    @Query("SELECT * FROM chat_sessions WHERE id = :id LIMIT 1")
    ChatSessionEntity getSessionById(long id);

    @Query("UPDATE chat_sessions SET title = :title, updatedAt = :updatedAt WHERE id = :id")
    void updateTitle(long id, String title, long updatedAt);

    @Query("UPDATE chat_sessions SET updatedAt = :updatedAt WHERE id = :id")
    void updateTimestamp(long id, long updatedAt);
}
