package com.deepseek.chat;

import android.app.Application;

import androidx.appcompat.app.AppCompatDelegate;

import com.deepseek.chat.data.repository.SettingsRepository;
import com.deepseek.chat.domain.model.AppSettings;

public class DeepSeekApp extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        applyThemeFromSettings();
    }

    private void applyThemeFromSettings() {
        SettingsRepository settingsRepository = new SettingsRepository(this);
        AppSettings settings = settingsRepository.getSettings();
        switch (settings.getTheme()) {
            case AppSettings.THEME_LIGHT:
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                break;
            case AppSettings.THEME_DARK:
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                break;
            default:
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
        }
    }
}
