package com.deepseek.chat;

import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.deepseek.chat.api.DeepSeekApiService;
import com.deepseek.chat.prefs.PreferencesManager;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.slider.Slider;
import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.List;

public class SettingsActivity extends AppCompatActivity {

    private TextInputEditText etApiKey;
    private AutoCompleteTextView spinnerModel;
    private MaterialButtonToggleGroup toggleThinking;
    private Slider sliderTemperature;
    private TextView tvTemperatureValue;
    private ProgressBar progressModels;
    private TextView tvThinkingHint;

    private PreferencesManager prefs;
    private DeepSeekApiService apiService;
    private List<DeepSeekApiService.ModelInfo> fetchedModels = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        prefs = new PreferencesManager(this);
        apiService = new DeepSeekApiService(BuildConfig.DEBUG);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        etApiKey = findViewById(R.id.et_api_key);
        spinnerModel = findViewById(R.id.spinner_model);
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

        spinnerModel.setOnItemClickListener((parent, view, position, id) -> {
            updateThinkingForModel(position);
        });

        loadSettings();
    }

    private void loadSettings() {
        etApiKey.setText(prefs.getApiKey());
        tvTemperatureValue.setText(String.format("%.1f", prefs.getTemperature()));
        sliderTemperature.setValue(prefs.getTemperature());

        int checkedId;
        switch (prefs.getThinkingIntensity()) {
            case PreferencesManager.THINKING_LIGHT:
                checkedId = R.id.btn_light;
                break;
            case PreferencesManager.THINKING_DEEP:
                checkedId = R.id.btn_deep;
                break;
            default:
                checkedId = R.id.btn_balanced;
        }
        toggleThinking.check(checkedId);

        fetchModels();
    }

    private void fetchModels() {
        String apiKey = etApiKey.getText() != null ? etApiKey.getText().toString().trim() : "";
        if (apiKey.isEmpty()) {
            // 无 API Key 时用本地兜底
            setFallbackModels();
            return;
        }

        progressModels.setVisibility(ProgressBar.VISIBLE);
        spinnerModel.setEnabled(false);

        apiService.fetchModels(apiKey, new DeepSeekApiService.ModelListCallback() {
            @Override
            public void onSuccess(List<DeepSeekApiService.ModelInfo> models) {
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
        List<DeepSeekApiService.ModelInfo> fallback = new ArrayList<>();
        fallback.add(new DeepSeekApiService.ModelInfo(
                PreferencesManager.MODEL_DEEPSEEK_CHAT, "DeepSeek", false));
        fallback.add(new DeepSeekApiService.ModelInfo(
                PreferencesManager.MODEL_DEEPSEEK_REASONER, "DeepSeek", true));
        fetchedModels = fallback;
        populateModels(fallback);
    }

    private void populateModels(List<DeepSeekApiService.ModelInfo> models) {
        String[] ids = new String[models.size()];
        for (int i = 0; i < models.size(); i++) {
            ids[i] = models.get(i).id;
        }
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_dropdown_item_1line, ids);
        spinnerModel.setAdapter(adapter);

        // 尝试选中当前已保存的模型
        String currentModel = prefs.getModel();
        int selectedIndex = -1;
        for (int i = 0; i < models.size(); i++) {
            if (models.get(i).id.equals(currentModel)) {
                selectedIndex = i;
                break;
            }
        }
        if (selectedIndex >= 0) {
            spinnerModel.setText(models.get(selectedIndex).id, false);
            updateThinkingForModel(selectedIndex);
        } else if (models.size() > 0) {
            spinnerModel.setText(models.get(0).id, false);
            updateThinkingForModel(0);
        }
    }

    private void updateThinkingForModel(int position) {
        if (position < 0 || position >= fetchedModels.size()) return;
        DeepSeekApiService.ModelInfo info = fetchedModels.get(position);

        if (info.supportsReasoning) {
            // 推理模型：深度思考是内置能力，思考强度对该模型有意义
            toggleThinking.setVisibility(MaterialButtonToggleGroup.VISIBLE);
            tvThinkingHint.setVisibility(TextView.GONE);
        } else {
            // 普通模型：思考强度通过 system prompt 实现，提示用户
            toggleThinking.setVisibility(MaterialButtonToggleGroup.VISIBLE);
            tvThinkingHint.setVisibility(TextView.VISIBLE);
            tvThinkingHint.setText("该模型通过提示词控制思考深度");
        }
    }

    private void saveSettings() {
        String apiKey = etApiKey.getText() != null ? etApiKey.getText().toString().trim() : "";
        String model = spinnerModel.getText().toString().trim();
        if (model.isEmpty() && !fetchedModels.isEmpty()) {
            model = fetchedModels.get(0).id;
        }

        int intensity;
        int checkedId = toggleThinking.getCheckedButtonId();
        if (checkedId == R.id.btn_light) {
            intensity = PreferencesManager.THINKING_LIGHT;
        } else if (checkedId == R.id.btn_deep) {
            intensity = PreferencesManager.THINKING_DEEP;
        } else {
            intensity = PreferencesManager.THINKING_BALANCED;
        }

        prefs.setApiKey(apiKey);
        prefs.setModel(model);
        prefs.setThinkingIntensity(intensity);
        prefs.setTemperature(sliderTemperature.getValue());

        Toast.makeText(this, "已保存", Toast.LENGTH_SHORT).show();
        finish();
    }
}