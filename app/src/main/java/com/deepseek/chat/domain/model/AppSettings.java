package com.deepseek.chat.domain.model;

public class AppSettings {

    public static final String THEME_SYSTEM = "system";
    public static final String THEME_LIGHT = "light";
    public static final String THEME_DARK = "dark";

    public static final int THINKING_LIGHT = 0;
    public static final int THINKING_BALANCED = 1;
    public static final int THINKING_DEEP = 2;

    private String apiKey;
    private String modelId;
    private int thinkingIntensity;
    private float temperature;
    private String baseUrl;
    private String theme;
    private boolean firstLaunch;

    public AppSettings(String apiKey, String modelId, int thinkingIntensity, float temperature,
                       String baseUrl, String theme, boolean firstLaunch) {
        this.apiKey = apiKey;
        this.modelId = modelId;
        this.thinkingIntensity = thinkingIntensity;
        this.temperature = temperature;
        this.baseUrl = baseUrl;
        this.theme = theme;
        this.firstLaunch = firstLaunch;
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public String getModelId() {
        return modelId;
    }

    public void setModelId(String modelId) {
        this.modelId = modelId;
    }

    public int getThinkingIntensity() {
        return thinkingIntensity;
    }

    public void setThinkingIntensity(int thinkingIntensity) {
        this.thinkingIntensity = thinkingIntensity;
    }

    public float getTemperature() {
        return temperature;
    }

    public void setTemperature(float temperature) {
        this.temperature = temperature;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String getTheme() {
        return theme;
    }

    public void setTheme(String theme) {
        this.theme = theme;
    }

    public boolean isFirstLaunch() {
        return firstLaunch;
    }

    public void setFirstLaunch(boolean firstLaunch) {
        this.firstLaunch = firstLaunch;
    }

    public boolean hasApiKey() {
        return apiKey != null && !apiKey.isEmpty();
    }
}
