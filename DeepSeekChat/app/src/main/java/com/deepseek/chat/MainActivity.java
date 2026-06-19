package com.deepseek.chat;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.deepseek.chat.api.DeepSeekApiService;
import com.deepseek.chat.model.Message;
import com.deepseek.chat.prefs.PreferencesManager;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private WebView webView;
    private SwipeRefreshLayout swipeRefresh;
    private PreferencesManager prefs;
    private DeepSeekApiService apiService;
    private final List<Message> messages = new ArrayList<>();

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        prefs = new PreferencesManager(this);
        apiService = new DeepSeekApiService(BuildConfig.DEBUG);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        swipeRefresh = findViewById(R.id.swipe_refresh);
        webView = findViewById(R.id.web_view);
        FloatingActionButton fabNewChat = findViewById(R.id.fab_new_chat);

        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setCacheMode(WebSettings.LOAD_NO_CACHE);

        webView.setWebChromeClient(new WebChromeClient());
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                return false;
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                swipeRefresh.setRefreshing(false);
            }
        });

        webView.addJavascriptInterface(new ChatBridge(), "AndroidBridge");

        swipeRefresh.setOnRefreshListener(() -> webView.reload());
        fabNewChat.setOnClickListener(v -> clearChat());

        loadChatPage();

        if (!prefs.hasApiKey()) {
            showApiKeyDialog();
        }
    }

    private void loadChatPage() {
        swipeRefresh.setRefreshing(true);
        webView.loadUrl("file:///android_asset/chat.html");
    }

    private void clearChat() {
        messages.clear();
        runOnUiThread(() -> webView.evaluateJavascript("window.clearChat()", null));
    }

    private void showApiKeyDialog() {
        new AlertDialog.Builder(this)
                .setTitle("需要配置 API Key")
                .setMessage("首次使用请前往设置页配置 DeepSeek API Key。")
                .setPositiveButton("去设置", (d, w) -> openSettings())
                .setCancelable(false)
                .show();
    }

    private void openSettings() {
        startActivity(new Intent(this, SettingsActivity.class));
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.action_settings) {
            openSettings();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }

    private void evaluateJs(String script) {
        runOnUiThread(() -> {
            if (webView != null) {
                webView.evaluateJavascript(script, null);
            }
        });
    }

    public class ChatBridge {

        @JavascriptInterface
        public String getConfig() {
            try {
                JSONObject config = new JSONObject();
                config.put("apiKeySet", prefs.hasApiKey());
                config.put("model", prefs.getModel());
                config.put("thinkingIntensity", prefs.getThinkingIntensity());
                config.put("temperature", prefs.getTemperature());
                return config.toString();
            } catch (JSONException e) {
                return "{}";
            }
        }

        @JavascriptInterface
        public void sendMessage(String json) {
            if (!prefs.hasApiKey()) {
                evaluateJs("window.onError('请先设置 API Key')");
                showApiKeyDialog();
                return;
            }

            try {
                JSONObject obj = new JSONObject(json);
                String content = obj.optString("content", "");
                if (content.isEmpty()) return;

                messages.add(new Message(Message.ROLE_USER, content));
                evaluateJs("window.onUserMessage(" + JSONObject.quote(content) + ")");

                List<Message> requestMessages = new ArrayList<>();
                requestMessages.addAll(messages);

                evaluateJs("window.onAssistantStart()");

                apiService.streamChat(
                        prefs.getApiKey(),
                        prefs.getModel(),
                        prefs.getTemperature(),
                        prefs.getThinkingIntensity(),
                        requestMessages,
                        new DeepSeekApiService.StreamCallback() {
                            private final StringBuilder contentBuilder = new StringBuilder();
                            private final StringBuilder reasoningBuilder = new StringBuilder();

                            @Override
                            public void onContentDelta(String delta) {
                                contentBuilder.append(delta);
                                evaluateJs("window.onContentDelta(" + JSONObject.quote(delta) + ")");
                            }

                            @Override
                            public void onReasoningDelta(String delta) {
                                reasoningBuilder.append(delta);
                                evaluateJs("window.onReasoningDelta(" + JSONObject.quote(delta) + ")");
                            }

                            @Override
                            public void onError(String message) {
                                evaluateJs("window.onError(" + JSONObject.quote(message) + ")");
                            }

                            @Override
                            public void onComplete() {
                                String fullContent = contentBuilder.toString();
                                if (!fullContent.isEmpty()) {
                                    messages.add(new Message(Message.ROLE_ASSISTANT, fullContent));
                                }
                                evaluateJs("window.onAssistantComplete()");
                            }
                        }
                );
            } catch (JSONException e) {
                evaluateJs("window.onError('消息格式错误')");
            }
        }

        @JavascriptInterface
        public void openSettingsPage() {
            openSettings();
        }
    }

}
