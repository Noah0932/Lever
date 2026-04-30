# Minecraft AI Agent

一个 Fabric 版 Minecraft AI Agent 模组，支持 OpenAI-compatible 接口、Spotlight 快速输入、聊天栏触发、多语言 UI、多 Profile、流式输出、截图多模态、缓存、费用统计、熔断和服务端安全命令执行。

## 功能概览

- `N` 打开 AI Agent 管理界面。
- `V` 打开 Spotlight 快速输入框。
- 聊天栏 `.ai <内容>` 本地触发 AI，不发送到公共聊天。
- 服务端 `/ai <内容>` 与 `/ai ask <玩家> <内容>` 支持多人代发。
- 代发支持目标确认、白名单、OP 策略。
- AI 命令执行经过服务端权限校验和高危命令黑名单。
- 内置 `zh_cn` 与 `en_us`。
- 不依赖 YACL / Cloth Config。

## 安装依赖

### 客户端必需

- Minecraft Java Edition `1.21.1` 或当前包支持的 `1.21.x` 范围。
- Java `21`。
- Fabric Loader `0.16+`。
- Fabric API。
- `Minecraft-AI-Agent-v1.0.3-mc1.21.1-fabric.jar`。

### 不需要

- YACL / `yet_another_config_lib_v3`。
- Cloth Config。
- 单独安装 JTokkit。
- OpenAI/DeepSeek SDK。

## 快速开始

1. 把 Fabric API 和 AI Agent jar 放入 `.minecraft/mods`。
2. 启动 Minecraft。
3. 按 `N` 打开配置。
4. 填写 `Base URL`、`API Key`、`Model ID`。
5. 按 `V` 输入任务，或聊天栏输入 `.ai <内容>`。

## 多人服务器

如果需要自动执行命令或使用 `/ai ask` 多人代发，服务器也要安装同一个 jar 和 Fabric API。

常用命令：

```mcfunction
/ai <内容>
/ai ask <玩家> <内容>
/ai allow <玩家>
/ai deny <玩家>
/ai list
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

当前稳定构建：

```powershell
powershell -ExecutionPolicy Bypass -File scripts/package-target.ps1 -Target 1.21.1
```

输出：

```text
dist/Minecraft-AI-Agent-v1.0.3-mc1.21.1-fabric.jar
```

## 试用文档

完整试用流程见 `docs/TRIAL_GUIDE.md`。

## 安全说明

- API Key 保存在本地配置文件中。
- UI、日志、错误信息会尽量脱敏。
- 服务端不保存玩家 API Key。
- 命令执行以目标玩家权限为准。
- 高危命令会被拒绝。
