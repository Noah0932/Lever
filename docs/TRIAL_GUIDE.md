# Minecraft AI Agent 试用文档

## 1. 试用目标

本指南用于在 Minecraft 1.21.1/Fabric 客户端中试用 Minecraft AI Agent 的核心能力：配置 API、使用 Spotlight、通过聊天栏触发 AI、多人代发、费用熔断和安全命令执行。

## 2. 环境准备

### 客户端

- Minecraft Java Edition `1.21.1` 或同一 `1.21.x` 兼容包支持范围内的版本。
- Java `21`。
- Fabric Loader `0.16+`。
- Fabric API，对应你的 Minecraft 版本。
- `Minecraft-AI-Agent-v1.0.3-mc1.21.1-fabric.jar`。

### 服务端（多人代发/安全执行需要）

- 与客户端一致的 Minecraft/Fabric/Fabric API。
- 安装同一个 AI Agent jar。
- 如果服务端未安装本模组，客户端仍可使用 `.ai` 和 `V` 本地入口，但 `/ai ask` 与自动安全执行会降级。

## 3. 安装步骤

1. 关闭游戏和服务端。
2. 删除 `.minecraft/mods` 或服务器 `mods` 中旧版 `Minecraft-AI-Agent-v1.0.0/v1.0.1/v1.0.2` jar。
3. 放入 `Minecraft-AI-Agent-v1.0.3-mc1.21.1-fabric.jar`。
4. 确认 Fabric API 已放入同一 `mods` 目录。
5. 启动游戏。

## 4. 首次配置

1. 进入游戏后按 `N` 打开配置界面。
2. 填写：
   - `Base URL`：例如 `https://api.openai.com/v1` 或兼容服务地址。
   - `API Key`：你的密钥。
   - `Model ID`：例如 `gpt-4o-mini`、DeepSeek 或 Ollama 兼容模型名。
3. 按需要调整：`Temperature`、`Max Tokens`、每日限额、Token 单价、汇率。
4. 保存配置。

## 5. 单人试用流程

### Spotlight 输入

1. 按 `V` 打开 Spotlight。
2. 输入：`帮我把时间设置为白天`。
3. 按 Enter。
4. 观察状态：选择渠道、思考、流式输出、执行或完成。

### 聊天栏输入

1. 打开聊天栏。
2. 输入 `.ai 帮我生成一个设置晴天的命令`。
3. 这条消息会被客户端拦截，不会发到公共聊天。
4. AI Agent 使用本地配置启动。

## 6. 多人代发试用

多人代发要求服务端和目标玩家客户端都安装本模组。

### 请求别人执行 AI

```mcfunction
/ai ask 玩家名 帮我设置晴天
```

目标玩家会收到确认弹窗。接受后，目标玩家自己的客户端会用自己的 API Key、费用限额和权限运行 Agent。

### 白名单代发

目标玩家执行：

```mcfunction
/ai allow 好友名
```

之后该好友可直接代发请求，无需每次确认。

移除权限：

```mcfunction
/ai deny 好友名
```

查看列表：

```mcfunction
/ai list
```

### OP 代发

如果目标玩家配置允许 OP 代发，服务器 OP 发起的 `/ai ask` 可自动通过。AI 仍以目标玩家身份执行，不会绕过目标玩家权限和命令黑名单。

## 7. 安全规则

- 禁止执行 `/stop`、`/op`、`/deop`、`/ban`、`/kick`、`/whitelist`、`/reload`、`/function`、`/datapack` 等高风险命令。
- 多人服务器上，命令执行以目标玩家权限为准。
- API Key 只保存在目标玩家本地配置中，服务器不接触密钥。
- 日志和错误提示会脱敏密钥。

## 8. 费用与熔断

- 输入前显示预估费用。
- 响应后读取 API `usage` 字段进行统计。
- 达到每日 CNY 限额后，Spotlight 和 Agent 请求会进入熔断状态。
- 统计文件位于 `config/ai_agent/stats.json`。

## 9. 多语言

- 已内置 `en_us` 和 `zh_cn`。
- 游戏语言切到简体中文时，界面和提示会显示中文。
- 若看到乱码，请确认安装的是 `v1.0.3` 或更新版本，并删除旧 jar。

## 10. 常见问题

### 提示缺少 YACL / yet_another_config_lib_v3

你仍在使用旧版 jar。删除旧版，只保留 `v1.0.3`。

### `.ai` 消息发到了公共聊天

确认客户端安装了本模组，并且配置中的聊天前缀仍为 `.ai`。

### `/ai ask` 提示目标不可用

目标玩家未安装客户端模组、未进入服务器，或服务端未安装本模组。

### 26.1.x 能否使用

当前仓库已注册 26.1.x 目标，但源码仍需迁移到 Mojang 官方命名并使用 Java 25。请查看 `COMPATIBILITY.md`。
