package com.deepseek.chat.ui.chat;

import android.annotation.SuppressLint;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.EditText;
import android.widget.PopupMenu;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.deepseek.chat.R;
import com.deepseek.chat.data.repository.SettingsRepository;
import com.deepseek.chat.domain.model.AppSettings;
import com.deepseek.chat.domain.model.ChatMessage;
import com.deepseek.chat.domain.model.ChatSession;
import com.deepseek.chat.ui.sessions.SessionsAdapter;
import com.deepseek.chat.ui.settings.SettingsActivity;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ChatActivity extends AppCompatActivity {

    private ChatViewModel viewModel;
    private WebView webView;
    private SwipeRefreshLayout swipeRefresh;
    private DrawerLayout drawerLayout;
    private SessionsAdapter sessionsAdapter;
    private FloatingActionButton fabNewChat;

    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        viewModel = new ViewModelProvider(this).get(ChatViewModel.class);
        SettingsRepository settingsRepo = new SettingsRepository(this);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        drawerLayout = findViewById(R.id.drawer_layout);
        webView = findViewById(R.id.web_view);
        swipeRefresh = findViewById(R.id.swipe_refresh);
        fabNewChat = findViewById(R.id.fab_new_chat);
        RecyclerView rvSessions = findViewById(R.id.rv_sessions);
        MaterialButton btnNewSession = findViewById(R.id.btn_new_session);

        toolbar.setNavigationOnClickListener(v -> drawerLayout.open());

        setupWebView();
        setupSessionsList(rvSessions);
        setupObservers();

        swipeRefresh.setOnRefreshListener(() -> webView.reload());
        fabNewChat.setOnClickListener(v -> createNewSession());
        btnNewSession.setOnClickListener(v -> {
            drawerLayout.close();
            createNewSession();
        });

        applyTheme(settingsRepo.getSettings().getTheme());

        long intentSessionId = getIntent().getLongExtra("session_id", -1);
        if (intentSessionId > 0) {
            viewModel.selectSession(intentSessionId);
        } else if (settingsRepo.isFirstLaunch()) {
            showFirstLaunchDialog(settingsRepo);
        } else if (!settingsRepo.getSettings().hasApiKey()) {
            showApiKeyMissingDialog();
        } else if (viewModel.getCurrentSessionId().getValue() == null) {
            createNewSession();
        }
    }

    private void setupWebView() {
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
                loadMessagesIntoWebView();
            }
        });

        webView.addJavascriptInterface(new ChatBridge(), "AndroidBridge");
        swipeRefresh.setRefreshing(true);
        webView.loadUrl("file:///android_asset/chat.html");
    }

    private void setupSessionsList(RecyclerView rvSessions) {
        sessionsAdapter = new SessionsAdapter();
        rvSessions.setLayoutManager(new LinearLayoutManager(this));
        rvSessions.setAdapter(sessionsAdapter);
        sessionsAdapter.setListener(new SessionsAdapter.OnSessionClickListener() {
            @Override
            public void onSessionClick(ChatSession session) {
                viewModel.selectSession(session.getId());
                drawerLayout.close();
            }

            @Override
            public void onSessionOptionsClick(ChatSession session, View anchor) {
                showSessionOptions(session, anchor);
            }
        });
    }

    private void setupObservers() {
        viewModel.getSessions().observe(this, sessions -> {
            sessionsAdapter.submitList(sessions);
            if (viewModel.getCurrentSessionId().getValue() == null && !sessions.isEmpty()) {
                viewModel.selectSession(sessions.get(0).getId());
            }
        });

        viewModel.getMessages().observe(this, messages -> {
            if (!Boolean.TRUE.equals(viewModel.getIsLoading().getValue())) {
                loadMessagesIntoWebView();
            }
        });

        viewModel.getIsLoading().observe(this, isLoading -> {
            evaluateJs("window.setLoading(" + isLoading + ")");
            if (!Boolean.TRUE.equals(isLoading)) {
                evaluateJs("window.onAssistantComplete()");
            }
        });

        viewModel.getStatusMessage().observe(this, status -> {
            evaluateJs("window.setStatus(" + JSONObject.quote(status != null ? status : "") + ")");
        });

        viewModel.getErrorMessage().observe(this, message -> {
            if (message != null && !message.isEmpty()) {
                Toast.makeText(this, message, Toast.LENGTH_LONG).show();
                evaluateJs("window.onError(" + JSONObject.quote(message) + ")");
            }
        });

        viewModel.getCurrentSessionId().observe(this, sessionId -> {
            loadMessagesIntoWebView();
        });

        viewModel.getContentDelta().observe(this, delta -> {
            evaluateJs("window.onContentDelta(" + JSONObject.quote(delta) + ")");
        });

        viewModel.getReasoningDelta().observe(this, delta -> {
            evaluateJs("window.onReasoningDelta(" + JSONObject.quote(delta) + ")");
        });
    }

    private void createNewSession() {
        AppSettings settings = viewModel.getSettings();
        String title = "新对话 " + dateFormat.format(new Date());
        viewModel.createSession(title, settings.getModelId());
    }

    private void showSessionOptions(ChatSession session, View anchor) {
        PopupMenu popup = new PopupMenu(this, anchor);
        popup.getMenuInflater().inflate(R.menu.menu_session_options, popup.getMenu());
        popup.setOnMenuItemClickListener(item -> {
            int id = item.getItemId();
            if (id == R.id.action_rename) {
                showRenameDialog(session);
                return true;
            } else if (id == R.id.action_delete) {
                showDeleteDialog(session);
                return true;
            } else if (id == R.id.action_clear) {
                showClearDialog(session);
                return true;
            }
            return false;
        });
        popup.show();
    }

    private void showRenameDialog(ChatSession session) {
        TextInputEditText input = new TextInputEditText(this);
        input.setText(session.getTitle());
        input.setSelection(session.getTitle().length());

        new AlertDialog.Builder(this)
                .setTitle("重命名会话")
                .setView(input)
                .setPositiveButton("保存", (d, w) -> {
                    String title = input.getText() != null ? input.getText().toString().trim() : "";
                    if (!title.isEmpty()) {
                        viewModel.updateSessionTitle(session.getId(), title);
                    }
                })
                .setNegativeButton("取消", null)
                .show();
    }

    private void showDeleteDialog(ChatSession session) {
        new AlertDialog.Builder(this)
                .setTitle("删除会话")
                .setMessage("确定要删除该会话吗？")
                .setPositiveButton("删除", (d, w) -> viewModel.deleteSession(session.getId()))
                .setNegativeButton("取消", null)
                .show();
    }

    private void showClearDialog(ChatSession session) {
        new AlertDialog.Builder(this)
                .setTitle("清空消息")
                .setMessage("确定要清空该会话的所有消息吗？")
                .setPositiveButton("清空", (d, w) -> viewModel.clearMessages(session.getId()))
                .setNegativeButton("取消", null)
                .show();
    }

    private void showFirstLaunchDialog(SettingsRepository settingsRepo) {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_api_key, null);
        EditText etApiKey = dialogView.findViewById(R.id.et_dialog_api_key);
        MaterialButton btnConfirm = dialogView.findViewById(R.id.btn_confirm);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("配置 API Key")
                .setMessage("请输入 DeepSeek API Key 以开始使用。\n密钥仅保存在本设备上。")
                .setView(dialogView)
                .setCancelable(false)
                .create();

        btnConfirm.setEnabled(false);
        etApiKey.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(Editable s) {
                btnConfirm.setEnabled(s.toString().trim().length() > 0);
            }
        });

        btnConfirm.setOnClickListener(v -> {
            String key = etApiKey.getText().toString().trim();
            if (!key.isEmpty()) {
                settingsRepo.setApiKey(key);
                settingsRepo.setFirstLaunchDone();
                dialog.dismiss();
                createNewSession();
            }
        });

        dialog.show();
    }

    private void showApiKeyMissingDialog() {
        new AlertDialog.Builder(this)
                .setTitle("需要 API Key")
                .setMessage("未检测到 API Key，请前往设置页配置。")
                .setPositiveButton("去设置", (d, w) -> openSettings())
                .setCancelable(false)
                .show();
    }

    private void openSettings() {
        startActivity(new Intent(this, SettingsActivity.class));
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_chat, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            openSettings();
            return true;
        } else if (id == R.id.action_search) {
            startActivity(new Intent(this, SearchActivity.class));
            return true;
        } else if (id == R.id.action_export) {
            exportCurrentChat();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void exportCurrentChat() {
        Long sessionId = viewModel.getCurrentSessionId().getValue();
        if (sessionId == null) {
            Toast.makeText(this, "没有可导出的会话", Toast.LENGTH_SHORT).show();
            return;
        }

        List<ChatMessage> messages = viewModel.getMessages().getValue();
        if (messages == null || messages.isEmpty()) {
            Toast.makeText(this, "当前会话没有消息", Toast.LENGTH_SHORT).show();
            return;
        }

        StringBuilder sb = new StringBuilder();
        sb.append("# DeepSeek Chat Export\n\n");
        for (ChatMessage msg : messages) {
            String role = msg.isUser() ? "User" : "Assistant";
            sb.append("**").append(role).append("**: ").append(msg.getContent()).append("\n\n");
        }

        try {
            File file = new File(getCacheDir(), "chat_export_" + sessionId + ".md");
            FileOutputStream fos = new FileOutputStream(file);
            fos.write(sb.toString().getBytes());
            fos.close();

            Uri uri = FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", file);
            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setType("text/markdown");
            intent.putExtra(Intent.EXTRA_STREAM, uri);
            intent.putExtra(Intent.EXTRA_SUBJECT, "DeepSeek 聊天记录");
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(Intent.createChooser(intent, "导出聊天记录"));
        } catch (IOException e) {
            Toast.makeText(this, "导出失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void loadMessagesIntoWebView() {
        List<ChatMessage> messages = viewModel.getMessages().getValue();
        if (messages == null || webView == null) return;

        try {
            JSONArray array = new JSONArray();
            for (ChatMessage msg : messages) {
                JSONObject obj = new JSONObject();
                obj.put("role", msg.getRole());
                obj.put("content", msg.getContent());
                obj.put("reasoning", msg.getReasoningContent() != null ? msg.getReasoningContent() : "");
                array.put(obj);
            }
            evaluateJs("window.loadHistory(" + array.toString() + ")");
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void evaluateJs(String script) {
        runOnUiThread(() -> {
            if (webView != null) {
                webView.evaluateJavascript(script, null);
            }
        });
    }

    @Override
    public void onBackPressed() {
        if (drawerLayout.isOpen()) {
            drawerLayout.close();
        } else if (webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }

    private void applyTheme(String theme) {
        // 主题已在 Activity 重建时由 AppCompatDelegate 处理，此处可扩展
    }

    public class ChatBridge {

        @JavascriptInterface
        public String getConfig() {
            try {
                AppSettings settings = viewModel.getSettings();
                JSONObject config = new JSONObject();
                config.put("apiKeySet", settings.hasApiKey());
                config.put("model", settings.getModelId());
                config.put("thinkingIntensity", settings.getThinkingIntensity());
                config.put("temperature", settings.getTemperature());
                config.put("loading", viewModel.getIsLoading().getValue() != null ? viewModel.getIsLoading().getValue() : false);
                return config.toString();
            } catch (JSONException e) {
                return "{}";
            }
        }

        @JavascriptInterface
        public void sendMessage(String json) {
            try {
                JSONObject obj = new JSONObject(json);
                String content = obj.optString("content", "");
                if (content.isEmpty()) return;
                viewModel.sendMessage(content);
            } catch (JSONException e) {
                evaluateJs("window.onError('消息格式错误')");
            }
        }

        @JavascriptInterface
        public void stopGeneration() {
            viewModel.stopGeneration();
        }

        @JavascriptInterface
        public void regenerateLastMessage() {
            viewModel.regenerateLastMessage();
        }

        @JavascriptInterface
        public void copyMessage(String content) {
            ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            if (clipboard != null) {
                ClipData clip = ClipData.newPlainText("DeepSeek Chat", content);
                clipboard.setPrimaryClip(clip);
                runOnUiThread(() -> Toast.makeText(ChatActivity.this, "已复制", Toast.LENGTH_SHORT).show());
            }
        }

        @JavascriptInterface
        public void openSettingsPage() {
            openSettings();
        }
    }
}
