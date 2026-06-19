package com.deepseek.chat.data.local;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import com.deepseek.chat.data.local.entity.ChatMessageEntity;
import com.deepseek.chat.data.local.entity.ChatSessionEntity;

@Database(
    entities = {ChatSessionEntity.class, ChatMessageEntity.class},
    version = 1,
    exportSchema = false
)
public abstract class AppDatabase extends RoomDatabase {

    private static final String DATABASE_NAME = "deepseek_chat.db";
    private static volatile AppDatabase instance;

    public abstract ChatSessionDao chatSessionDao();
    public abstract ChatMessageDao chatMessageDao();

    public static AppDatabase getInstance(Context context) {
        if (instance == null) {
            synchronized (AppDatabase.class) {
                if (instance == null) {
                    instance = Room.databaseBuilder(
                            context.getApplicationContext(),
                            AppDatabase.class,
                            DATABASE_NAME
                    ).build();
                }
            }
        }
        return instance;
    }
}
