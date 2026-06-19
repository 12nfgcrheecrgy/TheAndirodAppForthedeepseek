package com.deepseek.chat.ui.settings;

import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

import com.deepseek.chat.BuildConfig;

import com.deepseek.chat.R;
import com.deepseek.chat.data.repository.ModelRepository;
import com.deepseek.chat.data.repository.SettingsRepository;
import com.deepseek.chat.domain.model.AppSettings;
import com.deepseek.chat.domain.model.ModelInfo;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.slider.Slider;
import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.List;

public class SettingsActivity extends AppCompatActivity {

    private TextInputEditText etApiKey;
    private TextInputEditText etBaseUrl;
    private AutoCompleteTextView spinnerModel;
    private AutoCompleteTextView spinnerTheme;
    private MaterialButtonToggleGroup toggleThinking;
    private Slider sliderTemperature;
    private TextView tvTemperatureValue;
    private ProgressBar progressModels;
    private TextView tvThinkingHint;

    private SettingsRepository settingsRepository;
    private ModelRepository modelRepository;
    private List<ModelInfo> fetchedModels = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        settingsRepository = new SettingsRepository(this);
        modelRepository = new ModelRepository(BuildConfig.DEBUG);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        etApiKey = findViewById(R.id.et_api_key);
        etBaseUrl = findViewById(R.id.et_base_url);
        spinnerModel = findViewById(R.id.spinner_model);
        spinnerTheme = findViewById(R.id.spinner_theme);
        toggleThinking = findViewById(R.id.toggle_thinking);
        sliderTemperature = findViewById(R.id.slider_temperature);
        tvTemperatureValue = findViewById(R.id.tv_temperature_value);
        progressModels = findViewById(R.id.progress_models);
        tvThinkingHint = findViewById(R.id.tv_thinking_hint);
        MaterialButton btnSave = findViewById(R.id.btn_save);

        toolbar.setNavigationOnClickListener(v -> finish());

        sliderTemperature.addOnChangeListener((slider, value, fromUser) -> {
            tvTemperatureValue.setText(String.format("%.1f", value));
        });

        btnSave.setOnClickListener(v -> saveSettings());

        spinnerModel.setOnItemClickListener((parent, view, position, id) -> updateThinkingForModel(position));

        setupThemeSpinner();
        loadSettings();
    }

    private void setupThemeSpinner() {
        String[] themes = {"跟随系统", "浅色", "深色"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, themes);
        spinnerTheme.setAdapter(adapter);
    }

    private void loadSettings() {
        AppSettings settings = settingsRepository.getSettings();
        etApiKey.setText(settings.getApiKey());
        etBaseUrl.setText(settings.getBaseUrl());
        tvTemperatureValue.setText(String.format("%.1f", settings.getTemperature()));
        sliderTemperature.setValue(settings.getTemperature());

        int checkedId;
        switch (settings.getThinkingIntensity()) {
            case AppSettings.THINKING_LIGHT:
                checkedId = R.id.btn_light;
                break;
            case AppSettings.THINKING_DEEP:
                checkedId = R.id.btn_deep;
                break;
            default:
                checkedId = R.id.btn_balanced;
        }
        toggleThinking.check(checkedId);

        String themeLabel;
        switch (settings.getTheme()) {
            case AppSettings.THEME_LIGHT:
                themeLabel = "浅色";
                break;
            case AppSettings.THEME_DARK:
                themeLabel = "深色";
                break;
            default:
                themeLabel = "跟随系统";
        }
        spinnerTheme.setText(themeLabel, false);

        fetchModels(settings.getApiKey(), settings.getBaseUrl());
    }

    private void fetchModels(String apiKey, String baseUrl) {
        if (apiKey.isEmpty()) {
            setFallbackModels();
            return;
        }

        progressModels.setVisibility(ProgressBar.VISIBLE);
        spinnerModel.setEnabled(false);

        modelRepository.fetchModels(baseUrl, apiKey, new ModelRepository.Callback<List<ModelInfo>>() {
            @Override
            public void onSuccess(List<ModelInfo> models) {
                fetchedModels = models;
                if (models.isEmpty()) {
                    setFallbackModels();
                } else {
                    populateModels(models);
                }
                progressModels.setVisibility(ProgressBar.GONE);
                spinnerModel.setEnabled(true);
            }

            @Override
            public void onError(String message) {
                setFallbackModels();
                progressModels.setVisibility(ProgressBar.GONE);
                spinnerModel.setEnabled(true);
                Toast.makeText(SettingsActivity.this, "模型列表获取失败，使用本地缓存", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setFallbackModels() {
        List<ModelInfo> fallback = new ArrayList<>();
        fallback.add(new ModelInfo("deepseek-chat", "DeepSeek", false));
        fallback.add(new ModelInfo("deepseek-reasoner", "DeepSeek", true));
        fetchedModels = fallback;
        populateModels(fallback);
    }

    private void populateModels(List<ModelInfo> models) {
        String[] ids = new String[models.size()];
        for (int i = 0; i < models.size(); i++) {
            ids[i] = models.get(i).getId();
        }
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_dropdown_item_1line, ids);
        spinnerModel.setAdapter(adapter);

        AppSettings settings = settingsRepository.getSettings();
        String currentModel = settings.getModelId();
        int selectedIndex = -1;
        for (int i = 0; i < models.size(); i++) {
            if (models.get(i).getId().equals(currentModel)) {
                selectedIndex = i;
                break;
            }
        }
        if (selectedIndex >= 0) {
            spinnerModel.setText(models.get(selectedIndex).getId(), false);
            updateThinkingForModel(selectedIndex);
        } else if (models.size() > 0) {
            spinnerModel.setText(models.get(0).getId(), false);
            updateThinkingForModel(0);
        }
    }

    private void updateThinkingForModel(int position) {
        if (position < 0 || position >= fetchedModels.size()) return;
        ModelInfo info = fetchedModels.get(position);

        if (info.isSupportsReasoning()) {
            toggleThinking.setVisibility(MaterialButtonToggleGroup.VISIBLE);
            tvThinkingHint.setVisibility(TextView.GONE);
        } else {
            toggleThinking.setVisibility(MaterialButtonToggleGroup.VISIBLE);
            tvThinkingHint.setVisibility(TextView.VISIBLE);
            tvThinkingHint.setText("该模型通过提示词控制思考深度");
        }
    }

    private void saveSettings() {
        AppSettings settings = settingsRepository.getSettings();
        String apiKey = etApiKey.getText() != null ? etApiKey.getText().toString().trim() : "";
        String baseUrl = etBaseUrl.getText() != null ? etBaseUrl.getText().toString().trim() : "";
        if (baseUrl.isEmpty()) {
            baseUrl = "https://api.deepseek.com";
        }

        String model = spinnerModel.getText().toString().trim();
        if (model.isEmpty() && !fetchedModels.isEmpty()) {
            model = fetchedModels.get(0).getId();
        }

        int intensity;
        int checkedId = toggleThinking.getCheckedButtonId();
        if (checkedId == R.id.btn_light) {
            intensity = AppSettings.THINKING_LIGHT;
        } else if (checkedId == R.id.btn_deep) {
            intensity = AppSettings.THINKING_DEEP;
        } else {
            intensity = AppSettings.THINKING_BALANCED;
        }

        String themeLabel = spinnerTheme.getText().toString();
        String theme;
        switch (themeLabel) {
            case "浅色":
                theme = AppSettings.THEME_LIGHT;
                break;
            case "深色":
                theme = AppSettings.THEME_DARK;
                break;
            default:
                theme = AppSettings.THEME_SYSTEM;
        }

        settings.setApiKey(apiKey);
        settings.setBaseUrl(baseUrl);
        settings.setModelId(model);
        settings.setThinkingIntensity(intensity);
        settings.setTemperature(sliderTemperature.getValue());
        settings.setTheme(theme);
        settingsRepository.saveSettings(settings);

        applyTheme(theme);
        Toast.makeText(this, "已保存", Toast.LENGTH_SHORT).show();
        finish();
    }

    private void applyTheme(String theme) {
        switch (theme) {
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
