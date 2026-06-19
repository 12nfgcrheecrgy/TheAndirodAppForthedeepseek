package com.deepseek.chat.data.local;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.deepseek.chat.data.local.entity.ChatMessageEntity;

import java.util.List;

@Dao
public interface ChatMessageDao {

    @Insert
    long insert(ChatMessageEntity message);

    @Update
    void update(ChatMessageEntity message);

    @Query("SELECT * FROM chat_messages WHERE sessionId = :sessionId ORDER BY createdAt ASC")
    LiveData<List<ChatMessageEntity>> getMessagesForSession(long sessionId);

    @Query("SELECT * FROM chat_messages WHERE sessionId = :sessionId ORDER BY createdAt ASC")
    List<ChatMessageEntity> getMessagesForSessionSync(long sessionId);

    @Query("SELECT * FROM chat_messages WHERE id = :id LIMIT 1")
    ChatMessageEntity getMessageById(long id);

    @Query("DELETE FROM chat_messages WHERE id = :id")
    void deleteMessageById(long id);

    @Query("DELETE FROM chat_messages WHERE sessionId = :sessionId")
    void deleteMessagesForSession(long sessionId);

    @Query("SELECT * FROM chat_messages WHERE content LIKE '%' || :query || '%' ORDER BY createdAt DESC")
    LiveData<List<ChatMessageEntity>> searchMessages(String query);
}
