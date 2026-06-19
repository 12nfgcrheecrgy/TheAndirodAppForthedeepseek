package com.deepseek.chat.data.repository;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKey;

import com.deepseek.chat.domain.model.AppSettings;

import java.io.IOException;
import java.security.GeneralSecurityException;

public class SettingsRepository {

    private static final String PREFS_FILE = "deepseek_secure_settings";
    private static final String KEY_API_KEY = "api_key";
    private static final String KEY_MODEL_ID = "model_id";
    private static final String KEY_THINKING_INTENSITY = "thinking_intensity";
    private static final String KEY_TEMPERATURE = "temperature";
    private static final String KEY_BASE_URL = "base_url";
    private static final String KEY_THEME = "theme";
    private static final String KEY_FIRST_LAUNCH_DONE = "first_launch_done";

    private static final String DEFAULT_MODEL = "deepseek-chat";
    private static final String DEFAULT_BASE_URL = "https://api.deepseek.com";
    private static final String DEFAULT_THEME = AppSettings.THEME_SYSTEM;

    private final SharedPreferences prefs;

    public SettingsRepository(Context context) {
        this.prefs = createSecurePreferences(context);
    }

    private SharedPreferences createSecurePreferences(Context context) {
        try {
            MasterKey masterKey = new MasterKey.Builder(context)
                    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                    .build();

            return EncryptedSharedPreferences.create(
                    context,
                    PREFS_FILE,
                    masterKey,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            );
        } catch (GeneralSecurityException | IOException e) {
            // 如果加密失败，降级到普通 SharedPreferences（不应该发生）
            return context.getApplicationContext().getSharedPreferences(PREFS_FILE, Context.MODE_PRIVATE);
        }
    }

    public AppSettings getSettings() {
        return new AppSettings(
                prefs.getString(KEY_API_KEY, ""),
                prefs.getString(KEY_MODEL_ID, DEFAULT_MODEL),
                prefs.getInt(KEY_THINKING_INTENSITY, AppSettings.THINKING_BALANCED),
                prefs.getFloat(KEY_TEMPERATURE, 0.7f),
                prefs.getString(KEY_BASE_URL, DEFAULT_BASE_URL),
                prefs.getString(KEY_THEME, DEFAULT_THEME),
                !prefs.getBoolean(KEY_FIRST_LAUNCH_DONE, false)
        );
    }

    public void saveSettings(AppSettings settings) {
        prefs.edit()
                .putString(KEY_API_KEY, settings.getApiKey())
                .putString(KEY_MODEL_ID, settings.getModelId())
                .putInt(KEY_THINKING_INTENSITY, settings.getThinkingIntensity())
                .putFloat(KEY_TEMPERATURE, settings.getTemperature())
                .putString(KEY_BASE_URL, settings.getBaseUrl())
                .putString(KEY_THEME, settings.getTheme())
                .putBoolean(KEY_FIRST_LAUNCH_DONE, !settings.isFirstLaunch())
                .apply();
    }

    public String getApiKey() {
        return prefs.getString(KEY_API_KEY, "");
    }

    public void setApiKey(String apiKey) {
        prefs.edit().putString(KEY_API_KEY, apiKey).apply();
    }

    public void setFirstLaunchDone() {
        prefs.edit().putBoolean(KEY_FIRST_LAUNCH_DONE, true).apply();
    }

    public boolean isFirstLaunch() {
        return !prefs.getBoolean(KEY_FIRST_LAUNCH_DONE, false);
    }
}
