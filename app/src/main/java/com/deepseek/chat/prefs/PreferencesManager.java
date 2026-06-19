package com.deepseek.chat.prefs;

import android.content.Context;
import android.content.SharedPreferences;

public class PreferencesManager {
    private static final String PREFS_NAME = "deepseek_chat_prefs";
    private static final String KEY_API_KEY = "api_key";
    private static final String KEY_MODEL = "model";
    private static final String KEY_THINKING_INTENSITY = "thinking_intensity";
    private static final String KEY_TEMPERATURE = "temperature";
    private static final String KEY_FIRST_LAUNCH = "first_launch_done";

    public static final String MODEL_DEEPSEEK_CHAT = "deepseek-chat";
    public static final String MODEL_DEEPSEEK_REASONER = "deepseek-reasoner";

    public static final int THINKING_LIGHT = 0;
    public static final int THINKING_BALANCED = 1;
    public static final int THINKING_DEEP = 2;

    private final SharedPreferences prefs;

    public PreferencesManager(Context context) {
        this.prefs = context.getApplicationContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public void setApiKey(String apiKey) {
        prefs.edit().putString(KEY_API_KEY, apiKey).apply();
    }

    public String getApiKey() {
        return prefs.getString(KEY_API_KEY, "");
    }

    public boolean hasApiKey() {
        return !getApiKey().isEmpty();
    }

    public void setModel(String model) {
        prefs.edit().putString(KEY_MODEL, model).apply();
    }

    public String getModel() {
        return prefs.getString(KEY_MODEL, MODEL_DEEPSEEK_CHAT);
    }

    public void setThinkingIntensity(int intensity) {
        prefs.edit().putInt(KEY_THINKING_INTENSITY, intensity).apply();
    }

    public int getThinkingIntensity() {
        return prefs.getInt(KEY_THINKING_INTENSITY, THINKING_BALANCED);
    }

    public void setTemperature(float temperature) {
        prefs.edit().putFloat(KEY_TEMPERATURE, temperature).apply();
    }

    public float getTemperature() {
        return prefs.getFloat(KEY_TEMPERATURE, 0.7f);
    }

    public boolean isFirstLaunch() {
        return !prefs.getBoolean(KEY_FIRST_LAUNCH, false);
    }

    public void setFirstLaunchDone() {
        prefs.edit().putBoolean(KEY_FIRST_LAUNCH, true).apply();
    }
}
