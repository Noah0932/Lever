# Minecraft AI Agent 多版本兼容说明

## 兼容策略

本项目采用“同一仓库、按目标版本分别打包”的策略，而不是把所有 Minecraft 版本写进同一个 jar。这样可以避免 Fabric API、Minecraft 命名空间、Java 版本和网络/UI API 差异导致的假兼容。

## 目标矩阵

运行以下命令查看本地注册目标：

```powershell
powershell -ExecutionPolicy Bypass -File scripts/package-target.ps1 -List
```

当前注册目标：

| 目标 | 状态 | Java | 说明 |
| --- | --- | --- | --- |
| `1.21.1` | `stable` | 21 | 当前稳定可打包目标，输出 `v1.0.3` jar。 |
| `1.21.2` | `source-port-required` | 21 | 已注册坐标，需要源码/API 差异验证后启用。 |
| `1.21.3` | `source-port-required` | 21 | 已注册坐标，需要源码/API 差异验证后启用。 |
| `1.21.4` | `source-port-required` | 21 | 已注册坐标，需要源码/API 差异验证后启用。 |
| `1.21.5` | `source-port-required` | 21 | 已注册坐标，需要源码/API 差异验证后启用。 |
| `1.21.6` | `source-port-required` | 21 | 已注册坐标，需要源码/API 差异验证后启用。 |
| `1.21.7` | `source-port-required` | 21 | 已注册坐标，需要源码/API 差异验证后启用。 |
| `1.21.8` | `source-port-required` | 21 | 已注册坐标，需要源码/API 差异验证后启用。 |
| `1.21.9` | `source-port-required` | 21 | 已注册坐标，需要源码/API 差异验证后启用。 |
| `1.21.10` | `source-port-required` | 21 | 已注册坐标，需要源码/API 差异验证后启用。 |
| `1.21.11` | `source-port-required` | 21 | 最后一批 Yarn 命名目标之一，需单独适配验证。 |
| `26.1` | `mojang-port-required` | 25 | 需要迁移到 Mojang 官方命名。 |
| `26.1.1` | `mojang-port-required` | 25 | 需要迁移到 Mojang 官方命名。 |
| `26.1.2` | `mojang-port-required` | 25 | 需要迁移到 Mojang 官方命名。 |
| `26.2` | `mojang-port-required` | 25 | 需要迁移到 Mojang 官方命名。 |

## 为什么不能单 jar 覆盖所有版本

- `1.21.x` 小版本之间 Fabric API、payload、命令、屏幕和客户端事件 API 可能有差异。
- `26.1.x` 开始使用无混淆/官方命名，旧 Yarn 命名源码不能直接复用。
- `26.1.x` 需要 Java 25；当前稳定包使用 Java 21。
- 如果把不兼容版本写进 `fabric.mod.json`，玩家会遇到启动崩溃而不是清晰的依赖提示。

## 构建命令

打包当前稳定目标：

```powershell
powershell -ExecutionPolicy Bypass -File scripts/package-target.ps1 -Target 1.21.1
```

打包全部稳定目标：

```powershell
powershell -ExecutionPolicy Bypass -File scripts/package-target.ps1 -AllStable
```

非稳定目标默认会拒绝构建。完成对应源码适配后，可用：

```powershell
powershell -ExecutionPolicy Bypass -File scripts/package-target.ps1 -Target 1.21.4 -AllowExperimental
```

## 新版本适配流程

1. 在 `versions/<目标>.properties` 中登记 Minecraft、Fabric Loader、Fabric API、Java、mappings 模式。
2. 如果源码可直接编译，改 `support_state=stable` 且 `requires_source_port=false`。
3. 如果需要专用源码，新增目标 source set 或兼容适配层。
4. 执行 `scripts/package-target.ps1 -Target <目标>`。
5. 检查 jar 内 `fabric.mod.json` 的 `minecraft` 和 `java` 依赖是否只覆盖该目标。
6. 在实际客户端启动一次，验证 UI、聊天入口、网络包、`/ai` 命令和命令安全执行。

## 26.1.x 迁移要求

1. 安装 JDK 25。
2. 将 Minecraft/Yarn 命名迁移到 26.1.x Mojang 官方命名。
3. 重新验证 Fabric API 事件、payload codec、命令注册、截图 API 和屏幕渲染 API。
4. 将对应 `versions/26.1.x.properties` 改为 `stable` 后再发布 jar。
