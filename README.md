# Lever - 智械

一个 Fabric 版 Minecraft AI Agent 模组（原名 Minecraft AI Agent），支持 OpenAI-compatible 接口、Bot 召唤器假玩家系统、Spotlight 快速输入、聊天栏触发、多语言 UI、多 Profile、流式输出、截图多模态、缓存、费用统计、熔断和服务端安全命令执行。

## 功能概览

- `N` 打开 AI Agent 管理界面。
- `V` 打开 Spotlight 快速输入框。
- `B` 打开 Bot 聊天面板（多轮历史记录、时间戳、多 Bot 切换）。
- 聊天栏 `.ai <内容>` 本地触发 AI，不发送到公共聊天。
- `@Bot名 指令` 在聊天栏快速给 Bot 发指令。
- 服务端 `/ai <内容>` 与 `/ai ask <玩家> <内容>` 支持多人代发。
- 服务端 `/bot list|remove|tp|rename|skin` Bot 管理。
- Bot 召唤器（工作台合成），右键召唤假玩家实体，自动绑定主人。
- 假玩家 Bot 支持完整背包、装备穿戴、AI 配置继承、所有权转移。
- Bot 死亡后物品转移至原地箱子。
- 代发支持目标确认、白名单、OP 策略。
- 三级权限模型：Owner / Whitelist / OP。
- AI 命令执行经过服务端权限校验和高危命令黑名单。
- 内置 `zh_cn` 与 `en_us`。
- 不依赖 YACL / Cloth Config。

## 安装依赖

### 客户端必需

- Minecraft Java Edition `1.21.1` 或当前包支持的 `1.21.x` 范围。
- Java `21`。
- Fabric Loader `0.16+`。
- Fabric API。
- `Minecraft-AI-Agent-vV1.1-mc1.21.1-fabric.jar`。

### 不需要

- YACL / `yet_another_config_lib_v3`。
- Cloth Config。
- 单独安装 JTokkit。
- OpenAI/DeepSeek SDK。

## 快速开始

1. 把 Fabric API 和 Lever jar 放入 `.minecraft/mods`。
2. 启动 Minecraft。
3. 按 `N` 打开配置。
4. 填写 `Base URL`、`API Key`、`Model ID`。
5. 按 `V` 输入任务，或聊天栏输入 `.ai <内容>`。
6. 合成 Bot 召唤器，右键召唤 Bot，按 `B` 打开 Bot 聊天面板。

## 多人服务器

如果需要自动执行命令或使用 `/ai ask` 多人代发，服务器也要安装同一个 jar 和 Fabric API。

常用命令：

```mcfunction
/ai <内容>
/ai ask <玩家> <内容>
/ai allow <玩家>
/ai deny <玩家>
/ai list
/bot list
/bot tp <Bot名>
/bot rename <Bot名> <新名>
/bot skin <Bot名> <玩家名>
/bot remove <Bot名>
```

## 多版本兼容

当前仓库使用 `versions/*.properties` 管理多版本目标。

查看目标：

```powershell
powershell -ExecutionPolicy Bypass -File scripts/package-target.ps1 -List
```

打包稳定目标：

```powershell
powershell -ExecutionPolicy Bypass -File scripts/package-target.ps1 -Target 1.21.1
```

当前状态：

- `1.21.1`：已支持并可打包。
- `1.21.2` 到 `1.21.11`：已注册版本槽位，需逐版本源码适配验证。
- `26.1.x` / `26.2`：已注册版本槽位，但需要 Java 25 和 Mojang 官方命名迁移。

详细说明见 `COMPATIBILITY.md`。

## 构建

```powershell
powershell -ExecutionPolicy Bypass -File scripts/build-and-sync.ps1
```

或标准打包：

```powershell
powershell -ExecutionPolicy Bypass -File scripts/package-target.ps1 -Target 1.21.1
```

输出：

```text
dist/Minecraft-AI-Agent-vV1.1-mc1.21.1-fabric.jar
```

## 试用文档

完整试用流程见 `docs/TRIAL_GUIDE.md`。

## 安全说明

- API Key 保存在本地配置文件中。
- UI、日志、错误信息会尽量脱敏。
- 服务端不保存玩家 API Key。
- 命令执行以目标玩家权限为准。
- 高危命令会被拒绝。

---

## V1.1 正式发布更新日志

### 多模态动态能力协商

- `ProviderCapabilities` 新增 `supportsMaxTokens` / `maxOutputTokens` / `supportsStreamOptions` 三个能力标志位。
- `OpenAiCompatibleProvider.toPayload()` 实现参数动态清洗：`max_tokens` 仅在 `supportsMaxTokens=true` 时发送并根据 `maxOutputTokens` 自动修剪上限，图像内容仅在 `supportsVision=true` 时下发。彻底解决 DeepSeek、Ollama 等非 OpenAI 端点 HTTP 400 反序列化错误。
- 新增 `stream_options` 条件化（默认关闭），避免非 OpenAI API 拒绝请求。

### 渲染性能修复 (OpenGL + 主线程卡顿)

- `BotSkinFetcher`: 拆分为 `downloadAndDecode()`（后台 ForkJoinPool 执行网络 I/O + 图像解码）与主线程 `registerTexture()`（通过 `thenApplyAsync(MinecraftClient::execute)` 调度），消除 OpenGL 上下文违规导致 JVM 崩溃的致命 Bug。
- `ScreenshotCapture`: 拆分为 `takeScreenshotOnMain()`（主线程仅获取 Framebuffer 像素，<2ms）与 `processAsync()`（后台线程池执行 resize/jpeg编码/base64/SHA-256）。主线程卡顿从 70-270ms 降至 <2ms。
- `BotEntity` 修复 null `SyncedClientOptions` 导致的 NPE——传入完整客户端选项（ChatVisibility.FULL / Arm.RIGHT / skinParts 0x7F）。

### 并发安全加固

- `BotManager`: 3 个核心 Map 从 `LinkedHashMap` 改为 `ConcurrentHashMap`，ownerBots 值列表使用 `Collections.synchronizedList`。消除多线程（UseEntityCallback / BotChatHandler / BotCommand / BotNetworking）并发访问的 `ConcurrentModificationException` 风险。
- `ChatHistoryManager`: 内部列表从 `ArrayList` 改为 `synchronizedList` + `synchronized` 块保护，并发读写安全。
- `AgentRuntime`: 新增 `shutdown()` 优雅关闭线程池（3s 超时），注册 `CLIENT_STOPPING` 生命周期钩子防止线程泄漏。

### API Key 持久化修复

- `AgentConfigStore.load()`: 空文件或 JSON 损坏时不再自动保存默认值（防止覆写有效配置），新增 `.bak` 备份恢复机制。`save()` 实现原子写入：先写新内容、再删除旧备份、最后 rename。

### 安全权限模型

- 新增 `PermissionManager` 统一权限防火墙：三级访问控制 `canAccess`（owner/白名单/OP）、`canModify`（owner/OP）、`canTransfer`（仅限 owner）。消除 OP 可偷 Bot 所有权的安全漏洞。
- 统一的 `BotProfile.isAuthorized()` 委托给 `PermissionManager`，移除 3 处分散的重复权限判断代码。

### GUI 重构

- `AgentConfigScreen`: 动态面板高度（`findContentBottom`）、Toggle 双行网格布局（窄屏不溢出）、深色背景 `0xCC1E1E24`。
- `BotChatScreen`: 完全重写——多行文本渲染（`textRenderer.wrapLines`）、鼠标滚轮滚动、按 Bot 分组存储、时间戳 `[HH:mm]`、发送者颜色区分、Shift+Enter 换行。
- `ChatHistoryManager` + `ChatEntry`: 分层架构，对话历史 JSON 持久化（7 天 TTL + 200 上限 LRU 淘汰）。

### TDD 测试覆盖

- **120/120** 单元测试全部通过，零失败。
- 17 个测试类覆盖：Provider 合规（参数动态协商）、配置持久化往返、权限三级控制、聊天历史数据层、并发安全、截图处理流水线、CommandSafety / ToolParser / TokenEstimator / SecureLog。
