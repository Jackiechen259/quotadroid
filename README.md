# QuotaDroid

OpenCode Go 配额的 Android 原生监控客户端：Kotlin + Jetpack Compose。

桌面版 [opencode-quota-checker](https://github.com/Jackiechen259/opencode-quota-checker)（Tauri v2）的移动端对应实现——同样的三个配额窗口、同一个用量接口，改成随身的通知与小组件式体验。

[![CI](https://github.com/Jackiechen259/quotadroid/actions/workflows/ci.yml/badge.svg)](https://github.com/Jackiechen259/quotadroid/actions/workflows/ci.yml)
[![license](https://img.shields.io/badge/license-Apache--2.0-blue)](LICENSE)

## 功能

- 数据源：OpenCode Go 用量接口 `GET https://opencode.ai/zen/go/v1/usage`（Console API Key，Workspace ID 可选）
- 仪表盘展示 **5 小时 / 近一周 / 近一月** 三个窗口的已用百分比、剩余量与重置倒计时
- 最高负载、最近重置与整体健康状态一目了然
- 每 1 秒刷新倒计时，按可配置间隔（15 分钟 – 4 小时）自动刷新
- WorkManager 后台轮询：即使不打开 App 也能收到阈值告警
- 每个窗口独立阈值（50–100%），每个重置周期最多提醒一次
- API Key 用 Android Keystore 的 AES/GCM 加密后保存，明文不落盘、不进日志
- 中英双语界面，跟随系统深色/浅色模式
- 最近一次成功的读数会缓存，离线时仍可查看

## 与桌面版的差异

| 桌面版 | Android 版 |
| --- | --- |
| 系统托盘 + 置顶悬浮窗 | 通知栏告警 + WorkManager 后台任务 |
| 系统钥匙串 | Android Keystore（AES/GCM） |
| 自动更新（GitHub Releases） | 暂无（计划中） |

## 工作原理

用量接口返回三个窗口，每个窗口带 `percent`（已用百分比）、`status`（`ok` / `rate-limited`）与 `resetsAt`（绝对重置时刻）：

```json
{
  "usage": {
    "rolling": { "status": "ok",           "percent": 78,  "resetsAt": "2026-10-27T05:31:52Z" },
    "weekly":  { "status": "ok",           "percent": 52,  "resetsAt": "2026-10-31T00:00:00Z" },
    "monthly": { "status": "rate-limited", "percent": 100, "resetsAt": "2026-11-01T00:00:00Z" }
  }
}
```

接口约定详见 [docs/architecture/usage-api.md](docs/architecture/usage-api.md)。

设计上的两条硬规则：

1. **单一 JSON 契约**：`data/UsageParser.kt` 是唯一的解析入口，未知字段忽略、缺失窗口跳过。
2. **安全失败**：无法识别的响应一律抛 `QuotaError.Parse`，绝不把解析失败当成 0% 用量。

## 架构

```
app/src/main/java/com/jackiechen/quotadroid/
├── data/        OpenCodeClient（OkHttp）、UsageParser（kotlinx.serialization）、
│                QuotaRepository、SettingsStore（DataStore + Keystore）
├── notify/      AlertPolicy（阈值与去重）、Notifications（渠道与通知）
├── work/        QuotaWorker + QuotaScheduler（WorkManager 周期任务）
└── ui/          Compose 界面、QuotaViewModel（StateFlow 单一状态源）
```

业务逻辑不依赖 UI：`data/` 与 `notify/` 都可在纯 JVM 单元测试中验证（见 `app/src/test`）。

## 构建

需要 JDK 17+，Android SDK 35。

```bash
git clone https://github.com/Jackiechen259/quotadroid.git
cd quotadroid
./gradlew :app:testDebugUnitTest
./gradlew :app:assembleDebug
# 产物：app/build/outputs/apk/debug/app-debug.apk
```

安装到设备：

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

CI 会在每次 push 时跑单元测试并构建 debug APK，产物见 Actions 的 `quotadroid-debug` artifact。

## 配置

1. 打开 [OpenCode Console](https://opencode.ai/console) → **Account › Personal API keys**，创建密钥并勾选 **Usage › Read**。
2. 在 App 的「设置」中粘贴密钥（可选填 Workspace ID，用于覆盖密钥绑定的工作区）。
3. 保存后即可在仪表盘看到三个窗口；「测试连接」可在保存前验证密钥。

## 隐私

- API Key 只保存在本机 Keystore 加密后的 DataStore 中，仅作为 `Authorization: Bearer` 发送到 `opencode.ai`。
- 应用不含任何统计、埋点或第三方 SDK（依赖仅有 AndroidX、OkHttp、kotlinx.serialization）。
- 关闭 `android:allowBackup`，密钥不会随备份离开设备。

## 路线图

- [ ] 主屏小组件（Glance）
- [ ] 通知中的快捷刷新动作
- [ ] 历史用量曲线
- [ ] 通过 GitHub Releases 分发签名 APK

## 许可

Apache-2.0，见 [LICENSE](LICENSE)。
