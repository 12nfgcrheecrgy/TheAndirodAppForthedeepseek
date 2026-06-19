package com.deepseek.chat.ui.chat;

import android.app.Application;

import androidx.annotation.NonNull;

import com.deepseek.chat.BuildConfig;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;

import com.deepseek.chat.data.remote.ChatApiClient;
import com.deepseek.chat.data.repository.ChatRepository;
import com.deepseek.chat.data.repository.SettingsRepository;
import com.deepseek.chat.domain.model.AppSettings;
import com.deepseek.chat.domain.model.ChatMessage;
import com.deepseek.chat.domain.model.ChatSession;
import com.deepseek.chat.ui.common.SingleLiveEvent;

import java.util.ArrayList;
import java.util.List;

public class ChatViewModel extends AndroidViewModel {

    private final ChatRepository chatRepository;
    private final SettingsRepository settingsRepository;
    private final ChatApiClient chatApiClient;

    private final MutableLiveData<Long> currentSessionId = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();
    private final MutableLiveData<String> statusMessage = new MutableLiveData<>();
    private final SingleLiveEvent<String> contentDelta = new SingleLiveEvent<>();
    private final SingleLiveEvent<String> reasoningDelta = new SingleLiveEvent<>();

    private long assistantMessageId = -1;
    private final StringBuilder currentContent = new StringBuilder();
    private final StringBuilder currentReasoning = new StringBuilder();

    public ChatViewModel(@NonNull Application application) {
        super(application);
        this.chatRepository = new ChatRepository(application);
        this.settingsRepository = new SettingsRepository(application);
        this.chatApiClient = new ChatApiClient(BuildConfig.DEBUG);
    }

    public LiveData<List<ChatSession>> getSessions() {
        return chatRepository.getAllSessions();
    }

    public LiveData<List<ChatMessage>> getMessages() {
        return Transformations.switchMap(currentSessionId, chatRepository::getMessagesForSession);
    }

    public LiveData<Boolean> getIsLoading() {
        return isLoading;
    }

    public LiveData<String> getErrorMessage() {
        return errorMessage;
    }

    public LiveData<String> getStatusMessage() {
        return statusMessage;
    }

    public LiveData<Long> getCurrentSessionId() {
        return currentSessionId;
    }

    public SingleLiveEvent<String> getContentDelta() {
        return contentDelta;
    }

    public SingleLiveEvent<String> getReasoningDelta() {
        return reasoningDelta;
    }

    public AppSettings getSettings() {
        return settingsRepository.getSettings();
    }

    public void createSession(String title, String modelId) {
        chatRepository.createSession(title, modelId, id -> currentSessionId.postValue(id));
    }

    public void selectSession(long sessionId) {
        currentSessionId.setValue(sessionId);
    }

    public void updateSessionTitle(long sessionId, String title) {
        chatRepository.updateSessionTitle(sessionId, title);
    }

    public void deleteSession(long sessionId) {
        chatRepository.deleteSession(sessionId);
        Long current = currentSessionId.getValue();
        if (current != null && current == sessionId) {
            currentSessionId.setValue(null);
        }
    }

    public void clearMessages(long sessionId) {
        chatRepository.deleteMessagesForSession(sessionId);
    }

    public void sendMessage(String content) {
        AppSettings settings = settingsRepository.getSettings();
        if (!settings.hasApiKey()) {
            errorMessage.setValue("请先设置 API Key");
            return;
        }

        Long sessionId = currentSessionId.getValue();
        if (sessionId == null) {
            errorMessage.setValue("请先创建或选择会话");
            return;
        }

        chatRepository.insertMessage(sessionId, ChatMessage.ROLE_USER, content, null, id -> {
            loadAndSend(sessionId, settings);
        });
    }

    private void loadAndSend(long sessionId, AppSettings settings) {
        List<ChatMessage> history = chatRepository.getMessagesForSessionSync(sessionId);
        List<ChatMessage> requestMessages = new ArrayList<>(history);

        isLoading.postValue(true);
        statusMessage.postValue("正在思考…");
        currentContent.setLength(0);
        currentReasoning.setLength(0);
        assistantMessageId = -1;

        chatRepository.insertMessage(sessionId, ChatMessage.ROLE_ASSISTANT, "", null, id -> {
            assistantMessageId = id;
            chatApiClient.sendMessage(settings, requestMessages, new ChatApiClient.StreamListener() {
                @Override
                public void onContentDelta(String delta) {
                    currentContent.append(delta);
                    contentDelta.postValue(delta);
                    chatRepository.updateMessage(assistantMessageId, currentContent.toString(), currentReasoning.toString());
                }

                @Override
                public void onReasoningDelta(String delta) {
                    currentReasoning.append(delta);
                    reasoningDelta.postValue(delta);
                    chatRepository.updateMessage(assistantMessageId, currentContent.toString(), currentReasoning.toString());
                }

                @Override
                public void onError(String message) {
                    isLoading.postValue(false);
                    statusMessage.postValue("");
                    errorMessage.postValue(message);
                    chatRepository.updateMessage(assistantMessageId, currentContent.toString() + "\n\n[错误: " + message + "]", currentReasoning.toString());
                }

                @Override
                public void onComplete() {
                    isLoading.postValue(false);
                    statusMessage.postValue("");
                }
            });
        });
    }

    public void stopGeneration() {
        chatApiClient.cancel();
        isLoading.setValue(false);
        statusMessage.setValue("已停止");
    }

    public void regenerateLastMessage() {
        Long sessionId = currentSessionId.getValue();
        if (sessionId == null) return;

        List<ChatMessage> messages = chatRepository.getMessagesForSessionSync(sessionId);
        if (messages.isEmpty()) return;

        ChatMessage last = messages.get(messages.size() - 1);
        if (last.isAssistant()) {
            // 删除最后一条助手消息，然后重新生成
            chatRepository.deleteMessage(last.getId());
            AppSettings settings = settingsRepository.getSettings();
            loadAndSend(sessionId, settings);
        }
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        chatApiClient.cancel();
    }
}
