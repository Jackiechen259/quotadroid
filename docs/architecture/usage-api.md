# 用量接口契约

```
GET https://opencode.ai/zen/go/v1/usage
Authorization: Bearer <Console API Key>
x-opencode-org-id: <Workspace ID>   # 可选
Accept: application/json
User-Agent: QuotaDroid/<version> (Android)
```

## 响应

```json
{
  "usage": {
    "rolling": { "status": "ok",           "percent": 78,  "resetsAt": "2026-10-27T05:31:52Z" },
    "weekly":  { "status": "ok",           "percent": 52,  "resetsAt": "2026-10-31T00:00:00Z" },
    "monthly": { "status": "rate-limited", "percent": 100, "resetsAt": "2026-11-01T00:00:00Z" }
  }
}
```

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `usage.rolling` | object? | 5 小时窗口 |
| `usage.weekly` | object? | 近一周窗口 |
| `usage.monthly` | object? | 近一月窗口 |
| `percent` | number | 已用百分比，0–100 |
| `status` | string | `ok` 或 `rate-limited`（额度耗尽） |
| `resetsAt` | string | RFC 3339 绝对重置时刻 |

## 归一化规则

- 模型固定为 100 分制：`used = percent`、`remaining = 100 - used`，两者都夹取到 `[0, 100]`。
- `resetsAt` 直接采用服务端时间戳，不用相对倒计时推算。
- `status == "rate-limited"` 是权威的「额度耗尽」标志，参与健康判定，不靠百分比猜测。
- 缺失的窗口直接跳过；三个窗口都缺失视为解析失败。
- 未知字段忽略（接口可能扩展）。

## 错误映射

| HTTP | `QuotaError` | 界面提示 |
| --- | --- | --- |
| 401 | `Unauthorized` | 密钥被拒绝，检查密钥与权限 |
| 403 | `Forbidden` | 工作区未开通 Go，或密钥无权限 |
| 429 | `Throttled` | 触发限流，稍后重试 |
| 其他 4xx/5xx | `Http(code, body)` | 展示状态码，正文截断到 500 字符 |
| 网络异常 | `Network(detail)` | 展示网络错误 |

解析失败（`Parse`）永远不会被展示为 0% 用量。
