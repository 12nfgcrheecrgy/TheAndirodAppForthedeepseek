# DeepSeek Chat

Android 客户端，基于 DeepSeek API 的智能对话应用。

## 功能特性

- 流式对话，实时显示回复内容
- 支持 DeepSeek 全部模型，联网自动获取模型列表
- 思考强度可调（轻量 / 均衡 / 深度），根据模型能力动态适配
- Temperature 参数调节
- 思考过程（Reasoning）折叠展示
- Markdown 渲染（代码块、标题、行内代码）
- 黑白极简商业风格，支持系统深色模式
- 首次启动引导输入 API Key，密钥仅保存在本地

## 技术栈

- 语言：Java
- 最低支持：Android 7.0 (API 24)
- 目标版本：Android 14 (API 34)
- UI 框架：Material Design 3
- 网络：OkHttp 4.12
- 聊天界面：WebView + JS Bridge

## 项目结构

```
app/src/main/
├── java/com/deepseek/chat/
│   ├── MainActivity.java          # 主界面（WebView 聊天）
│   ├── SettingsActivity.java      # 原生设置页
│   ├── api/
│   │   └── DeepSeekApiService.java  # API 服务（流式聊天 + 模型列表）
│   ├── model/
│   │   └── Message.java           # 消息模型
│   └── prefs/
│       └── PreferencesManager.java  # 配置持久化
├── assets/
│   ├── chat.html                  # 聊天页面
│   ├── styles.css                 # 样式（黑白配色 + 深色模式）
│   └── app.js                     # 前端逻辑
└── res/
    ├── layout/                    # 布局文件
    ├── values/                    # 字符串、颜色、主题
    └── drawable/                  # 图标资源
```

## 构建与运行

1. 使用 Android Studio 打开项目根目录
2. 等待 Gradle 同步完成
3. 连接设备或启动模拟器，点击 Run

## 使用说明

1. 首次启动会弹出 API Key 输入框，填入你的 DeepSeek API Key（sk-...）
2. 进入设置页可切换模型、调整思考强度和 Temperature
3. 模型列表从 DeepSeek API 实时获取，无需手动更新

## 许可证

MIT License
