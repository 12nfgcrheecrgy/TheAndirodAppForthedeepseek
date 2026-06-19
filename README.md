# DeepSeek Chat

面向商业场景打造的 Android 智能对话客户端，基于 DeepSeek API 构建。

## 功能特性

- 多会话管理：左侧抽屉展示所有会话，支持新建、重命名、清空、删除
- 本地消息持久化：基于 Room 数据库存储聊天记录
- 流式对话：实时显示助手回复与思考过程
- 模型切换：联网从 DeepSeek API 拉取可用模型列表
- 思考强度：根据模型能力动态提示，通过 system prompt 或内置推理能力控制
- 参数调节：Temperature、自定义 API 基础地址
- 消息操作：复制、重新生成、停止生成
- 搜索历史：按内容搜索所有会话的消息
- 导出分享：将当前会话导出为 Markdown 文件
- 主题设置：跟随系统 / 浅色 / 深色
- 安全配置：API Key 使用 EncryptedSharedPreferences 加密存储
- 首次启动：强制弹窗引导输入 API Key，不可跳过

## 技术栈

- 语言：Java 11
- 最低支持：Android 7.0 (API 24)
- 目标版本：Android 14 (API 34)
- 架构：MVVM + Repository 模式
- UI：Material Design 3 + WebView 聊天界面
- 本地存储：Room
- 安全：AndroidX Security (EncryptedSharedPreferences)
- 网络：OkHttp 4.12
- 测试：JUnit + Mockito + Robolectric

## 项目结构

```
app/src/main/java/com/deepseek/chat/
├── DeepSeekApp.java
├── data/
│   ├── local/           # Room 数据库、DAO、Entity
│   ├── remote/          # DeepSeek API 客户端
│   └── repository/      # Chat / Settings / Model Repository
├── domain/
│   └── model/           # 领域模型
└── ui/
    ├── chat/            # 聊天页 (ChatActivity + ChatViewModel)
    ├── search/          # 搜索页 (SearchActivity)
    ├── sessions/        # 会话列表适配器
    ├── settings/        # 设置页 (SettingsActivity)
    └── common/          # SingleLiveEvent 等通用组件

app/src/main/assets/
├── chat.html            # WebView 聊天界面
├── styles.css           # 黑白配色 + 深色模式
└── app.js               # 聊天前端逻辑
```

## 构建与运行

1. 使用 Android Studio 打开项目根目录
2. 等待 Gradle 同步完成
3. 连接设备或启动模拟器，点击 Run

## 使用说明

1. 首次启动会弹出 API Key 输入框，填入 DeepSeek API Key
2. 点击左上角菜单可打开会话列表抽屉
3. 右下角按钮可快速新建会话
4. 进入设置页可切换模型、调整思考强度、Temperature 和主题
5. 长按或点击消息旁的按钮可复制或重新生成

## 许可证

MIT License
