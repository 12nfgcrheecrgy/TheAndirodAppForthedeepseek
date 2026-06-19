package com.deepseek.chat;

import android.content.Context;

import androidx.test.core.app.ApplicationProvider;

import com.deepseek.chat.data.repository.SettingsRepository;
import com.deepseek.chat.domain.model.AppSettings;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
public class SettingsRepositoryTest {

    private SettingsRepository repository;

    @Before
    public void setUp() {
        Context context = ApplicationProvider.getApplicationContext();
        repository = new SettingsRepository(context);
    }

    @Test
    public void testDefaultSettings() {
        AppSettings settings = repository.getSettings();
        assertNotNull(settings);
        assertEquals("deepseek-chat", settings.getModelId());
        assertEquals(AppSettings.THINKING_BALANCED, settings.getThinkingIntensity());
        assertEquals(0.7f, settings.getTemperature(), 0.01f);
        assertTrue(settings.isFirstLaunch());
    }

    @Test
    public void testSaveAndRetrieveApiKey() {
        repository.setApiKey("sk-test-key");
        AppSettings settings = repository.getSettings();
        assertEquals("sk-test-key", settings.getApiKey());
        assertTrue(settings.hasApiKey());
    }

    @Test
    public void testFirstLaunchDone() {
        repository.setFirstLaunchDone();
        assertFalse(repository.isFirstLaunch());
    }
}
