package com.deepseek.chat;

import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.deepseek.chat.prefs.PreferencesManager;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.slider.Slider;
import com.google.android.material.textfield.TextInputEditText;

public class SettingsActivity extends AppCompatActivity {

    private TextInputEditText etApiKey;
    private AutoCompleteTextView spinnerModel;
    private MaterialButtonToggleGroup toggleThinking;
    private Slider sliderTemperature;
    private TextView tvTemperatureValue;

    private PreferencesManager prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        prefs = new PreferencesManager(this);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        etApiKey = findViewById(R.id.et_api_key);
        spinnerModel = findViewById(R.id.spinner_model);
        toggleThinking = findViewById(R.id.toggle_thinking);
        sliderTemperature = findViewById(R.id.slider_temperature);
        tvTemperatureValue = findViewById(R.id.tv_temperature_value);
        MaterialButton btnSave = findViewById(R.id.btn_save);

        toolbar.setNavigationOnClickListener(v -> finish());

        String[] models = {PreferencesManager.MODEL_DEEPSEEK_CHAT, PreferencesManager.MODEL_DEEPSEEK_REASONER};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, models);
        spinnerModel.setAdapter(adapter);

        sliderTemperature.addOnChangeListener((slider, value, fromUser) -> {
            tvTemperatureValue.setText(String.format("%.1f", value));
        });

        btnSave.setOnClickListener(v -> saveSettings());

        loadSettings();
    }

    private void loadSettings() {
        etApiKey.setText(prefs.getApiKey());
        spinnerModel.setText(prefs.getModel(), false);

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

        sliderTemperature.setValue(prefs.getTemperature());
        tvTemperatureValue.setText(String.format("%.1f", prefs.getTemperature()));
    }

    private void saveSettings() {
        String apiKey = etApiKey.getText() != null ? etApiKey.getText().toString().trim() : "";
        String model = spinnerModel.getText().toString().trim();
        if (model.isEmpty()) {
            model = PreferencesManager.MODEL_DEEPSEEK_CHAT;
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
