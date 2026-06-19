package com.deepseek.chat.data.remote;

import android.os.Handler;
import android.os.Looper;

import com.deepseek.chat.domain.model.AppSettings;
import com.deepseek.chat.domain.model.ChatMessage;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
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

public class ChatApiClient {

    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

    private final OkHttpClient client;
    private final Handler mainHandler;
    private Call currentCall;

    public interface StreamListener {
        void onContentDelta(String delta);
        void onReasoningDelta(String delta);
        void onError(String message);
        void onComplete();
    }

    public ChatApiClient(boolean enableLogging) {
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

    public void sendMessage(AppSettings settings, List<ChatMessage> messages, StreamListener listener) {
        if (currentCall != null) {
            currentCall.cancel();
        }

        try {
            String url = settings.getBaseUrl().endsWith("/")
                    ? settings.getBaseUrl() + "chat/completions"
                    : settings.getBaseUrl() + "/chat/completions";

            JSONObject body = new JSONObject();
            body.put("model", settings.getModelId());
            body.put("stream", true);
            body.put("temperature", settings.getTemperature());

            JSONArray msgs = new JSONArray();
            String systemPrompt = buildSystemPrompt(settings.getThinkingIntensity());
            if (!systemPrompt.isEmpty()) {
                msgs.put(new JSONObject().put("role", "system").put("content", systemPrompt));
            }
            for (ChatMessage msg : messages) {
                JSONObject o = new JSONObject();
                o.put("role", msg.getRole());
                o.put("content", msg.getContent());
                msgs.put(o);
            }
            body.put("messages", msgs);

            Request request = new Request.Builder()
                    .url(url)
                    .addHeader("Authorization", "Bearer " + settings.getApiKey())
                    .addHeader("Content-Type", "application/json")
                    .post(RequestBody.create(body.toString(), JSON))
                    .build();

            currentCall = client.newCall(request);
            currentCall.enqueue(new Callback() {
                @Override public void onFailure(Call call, IOException e) {
                    if (call.isCanceled()) return;
                    postError(listener, "网络请求失败: " + e.getMessage());
                }

                @Override public void onResponse(Call call, Response response) throws IOException {
                    if (call.isCanceled()) return;
                    if (!response.isSuccessful()) {
                        String err = response.body() != null ? response.body().string() : "Unknown";
                        postError(listener, "API 错误 (" + response.code() + "): " + err);
                        return;
                    }
                    ResponseBody body = response.body();
                    if (body == null) {
                        postError(listener, "响应为空");
                        return;
                    }
                    try {
                        String line;
                        while ((line = body.source().readUtf8Line()) != null) {
                            if (line.isEmpty()) continue;
                            if (!line.startsWith("data: ")) continue;
                            String data = line.substring(6);
                            if ("[DONE]".equals(data)) {
                                postComplete(listener);
                                return;
                            }
                            parseDelta(data, listener);
                        }
                        postComplete(listener);
                    } catch (Exception e) {
                        postError(listener, "解析错误: " + e.getMessage());
                    }
                }
            });
        } catch (JSONException e) {
            postError(listener, "构造请求失败: " + e.getMessage());
        }
    }

    public void cancel() {
        if (currentCall != null) {
            currentCall.cancel();
            currentCall = null;
        }
    }

    private String buildSystemPrompt(int thinkingIntensity) {
        switch (thinkingIntensity) {
            case AppSettings.THINKING_LIGHT:
                return "请给出简洁、直接的回答，不需要过多解释。";
            case AppSettings.THINKING_DEEP:
                return "请进行深度、严谨、逐步的推理，充分分析后再给出答案。";
            case AppSettings.THINKING_BALANCED:
            default:
                return "";
        }
    }

    private void parseDelta(String data, StreamListener listener) throws JSONException {
        JSONObject obj = new JSONObject(data);
        JSONArray choices = obj.optJSONArray("choices");
        if (choices == null || choices.length() == 0) return;
        JSONObject delta = choices.getJSONObject(0).optJSONObject("delta");
        if (delta == null) return;
        String content = delta.optString("content", null);
        String reasoning = delta.optString("reasoning_content", null);
        if (content != null && !content.isEmpty()) {
            postContent(listener, content);
        }
        if (reasoning != null && !reasoning.isEmpty()) {
            postReasoning(listener, reasoning);
        }
    }

    private void postContent(StreamListener listener, String delta) {
        mainHandler.post(() -> listener.onContentDelta(delta));
    }

    private void postReasoning(StreamListener listener, String delta) {
        mainHandler.post(() -> listener.onReasoningDelta(delta));
    }

    private void postError(StreamListener listener, String message) {
        mainHandler.post(() -> listener.onError(message));
    }

    private void postComplete(StreamListener listener) {
        mainHandler.post(listener::onComplete);
    }
}
