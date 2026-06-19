package com.deepseek.chat.data.repository;

import android.os.Handler;
import android.os.Looper;

import com.deepseek.chat.domain.model.ModelInfo;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okhttp3.logging.HttpLoggingInterceptor;

public class ModelRepository {

    private final OkHttpClient client;
    private final Handler mainHandler;

    public interface Callback<T> {
        void onSuccess(T result);
        void onError(String message);
    }

    public ModelRepository(boolean enableLogging) {
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

    public void fetchModels(String baseUrl, String apiKey, Callback<List<ModelInfo>> callback) {
        String url = baseUrl.endsWith("/") ? baseUrl + "models" : baseUrl + "/models";
        Request request = new Request.Builder()
                .url(url)
                .addHeader("Authorization", "Bearer " + apiKey)
                .get()
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                postError(callback, "获取模型列表失败: " + e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (!response.isSuccessful()) {
                    String errBody = response.body() != null ? response.body().string() : "";
                    postError(callback, "API 错误 (" + response.code() + "): " + errBody);
                    return;
                }
                ResponseBody body = response.body();
                if (body == null) {
                    postError(callback, "响应为空");
                    return;
                }
                try {
                    String json = body.string();
                    List<ModelInfo> models = parseModelList(json);
                    postSuccess(callback, models);
                } catch (Exception e) {
                    postError(callback, "解析模型列表失败: " + e.getMessage());
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
            boolean supportsReasoning = id.contains("reasoner") || id.contains("r1");
            result.add(new ModelInfo(id, ownedBy, supportsReasoning));
        }
        return result;
    }

    private void postSuccess(Callback<List<ModelInfo>> callback, List<ModelInfo> models) {
        mainHandler.post(() -> callback.onSuccess(models));
    }

    private void postError(Callback<List<ModelInfo>> callback, String message) {
        mainHandler.post(() -> callback.onError(message));
    }
}
