package com.deepseek.chat.api;

import android.os.Handler;
import android.os.Looper;

import com.deepseek.chat.model.Message;
import com.deepseek.chat.prefs.PreferencesManager;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okhttp3.logging.HttpLoggingInterceptor;

public class DeepSeekApiService {
    private static final String BASE_URL = "https://api.deepseek.com";
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

    private final OkHttpClient client;
    private final Handler mainHandler;

    public interface StreamCallback {
        void onContentDelta(String delta);
        void onReasoningDelta(String delta);
        void onError(String message);
        void onComplete();
    }

    public interface ModelListCallback {
        void onSuccess(List<ModelInfo> models);
        void onError(String message);
    }

    public static class ModelInfo {
        public final String id;
        public final String ownedBy;
        public final boolean supportsReasoning;

        public ModelInfo(String id, String ownedBy, boolean supportsReasoning) {
            this.id = id;
            this.ownedBy = ownedBy;
            this.supportsReasoning = supportsReasoning;
        }

        @Override
        public String toString() {
            return id;
        }
    }

    public DeepSeekApiService(boolean enableLogging) {
        OkHttpClient.Builder builder = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS);
        if (enableLogging) {
            HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
            logging.setLevel(HttpLoggingInterceptor.Level.BODY);
            builder.addInterceptor(logging);
        }
        this.client = builder.build();
        this.mainHandler = new Handler(Looper.getMainLooper());
    }

    public void fetchModels(String apiKey, ModelListCallback callback) {
        Request request = new Request.Builder()
                .url(BASE_URL + "/models")
                .addHeader("Authorization", "Bearer " + apiKey)
                .get()
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                mainHandler.post(() -> callback.onError("获取模型列表失败: " + e.getMessage()));
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (!response.isSuccessful()) {
                    String errBody = response.body() != null ? response.body().string() : "";
                    mainHandler.post(() -> callback.onError("API 错误 (" + response.code() + "): " + errBody));
                    return;
                }
                ResponseBody body = response.body();
                if (body == null) {
                    mainHandler.post(() -> callback.onError("响应为空"));
                    return;
                }
                try {
                    String json = body.string();
                    List<ModelInfo> models = parseModelList(json);
                    mainHandler.post(() -> callback.onSuccess(models));
                } catch (Exception e) {
                    mainHandler.post(() -> callback.onError("解析模型列表失败: " + e.getMessage()));
                }
            }
        });
    }

    private List<ModelInfo> parseModelList(String json) throws JSONException {
        List<ModelInfo> result = new ArrayList<>();
        JSONObject root = new JSONObject(json);
        JSONArray data = root.optJSONArray("data");
        if (data == null) return result;

        for (int i = 0; i < data.length(); i++) {
            JSONObject item = data.getJSONObject(i);
            String id = item.optString("id", "");
            String ownedBy = item.optString("owned_by", "DeepSeek");
            if (id.isEmpty()) continue;
            // 判断该模型是否支持深度推理（reasoner 系列或带有 reasoning 能力）
            boolean supportsReasoning = id.contains("reasoner") || id.contains("r1");
            result.add(new ModelInfo(id, ownedBy, supportsReasoning));
        }
        return result;
    }

    public void streamChat(
            String apiKey,
            String model,
            float temperature,
            int thinkingIntensity,
            List<Message> messages,
            StreamCallback callback
    ) {
        try {
            JSONObject body = new JSONObject();
            body.put("model", model);
            body.put("stream", true);
            body.put("temperature", temperature);

            JSONArray msgs = new JSONArray();
            String systemPrompt = buildSystemPrompt(thinkingIntensity);
            if (!systemPrompt.isEmpty()) {
                msgs.put(new JSONObject().put("role", "system").put("content", systemPrompt));
            }
            for (Message msg : messages) {
                JSONObject o = new JSONObject();
                o.put("role", msg.getRole());
                o.put("content", msg.getContent());
                msgs.put(o);
            }
            body.put("messages", msgs);

            Request request = new Request.Builder()
                    .url(BASE_URL + "/chat/completions")
                    .addHeader("Authorization", "Bearer " + apiKey)
                    .addHeader("Content-Type", "application/json")
                    .post(RequestBody.create(body.toString(), JSON))
                    .build();

            client.newCall(request).enqueue(new Callback() {
                @Override public void onFailure(Call call, IOException e) {
                    postError(callback, "网络请求失败: " + e.getMessage());
                }

                @Override public void onResponse(Call call, Response response) throws IOException {
                    if (!response.isSuccessful()) {
                        String err = response.body() != null ? response.body().string() : "Unknown";
                        postError(callback, "API 错误 (" + response.code() + "): " + err);
                        return;
                    }
                    ResponseBody body = response.body();
                    if (body == null) {
                        postError(callback, "响应为空");
                        return;
                    }
                    try {
                        String line;
                        while ((line = body.source().readUtf8Line()) != null) {
                            if (line.isEmpty()) continue;
                            if (!line.startsWith("data: ")) continue;
                            String data = line.substring(6);
                            if ("[DONE]".equals(data)) {
                                postComplete(callback);
                                return;
                            }
                            parseDelta(data, callback);
                        }
                        postComplete(callback);
                    } catch (Exception e) {
                        postError(callback, "解析错误: " + e.getMessage());
                    }
                }
            });
        } catch (JSONException e) {
            postError(callback, "构造请求失败: " + e.getMessage());
        }
    }

    private String buildSystemPrompt(int thinkingIntensity) {
        switch (thinkingIntensity) {
            case PreferencesManager.THINKING_LIGHT:
                return "请给出简洁、直接的回答，不需要过多解释。";
            case PreferencesManager.THINKING_DEEP:
                return "请进行深度、严谨、逐步的推理，充分分析后再给出答案。";
            case PreferencesManager.THINKING_BALANCED:
            default:
                return "";
        }
    }

    private void parseDelta(String data, StreamCallback callback) throws JSONException {
        JSONObject obj = new JSONObject(data);
        JSONArray choices = obj.optJSONArray("choices");
        if (choices == null || choices.length() == 0) return;
        JSONObject delta = choices.getJSONObject(0).optJSONObject("delta");
        if (delta == null) return;
        String content = delta.optString("content", null);
        String reasoning = delta.optString("reasoning_content", null);
        if (content != null && !content.isEmpty()) {
            postContent(callback, content);
        }
        if (reasoning != null && !reasoning.isEmpty()) {
            postReasoning(callback, reasoning);
        }
    }

    private void postContent(StreamCallback callback, String delta) {
        mainHandler.post(() -> callback.onContentDelta(delta));
    }

    private void postReasoning(StreamCallback callback, String delta) {
        mainHandler.post(() -> callback.onReasoningDelta(delta));
    }

    private void postError(StreamCallback callback, String message) {
        mainHandler.post(() -> callback.onError(message));
    }

    private void postComplete(StreamCallback callback) {
        mainHandler.post(callback::onComplete);
    }
}