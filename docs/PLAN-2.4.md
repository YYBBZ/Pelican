# Pelican 2.4 修改方案

- 文档版本：2.4-方案稿
- 基线版本：Pelican 2.3
- 适用范围：Pelican-2.3-source.zip（Java，package `app.pelican.check`）
- 技术约束：只用 Java；不引入 Kotlin；不新增第三方依赖；不改动本方案未列出的模块

---

## 一、目标

1. 修复输入 Token 告警永远不触发的问题，使异常差额能被界面标出。
2. 拆开历史页顶部两个粘连的入口。
3. 思考强度由 5 档扩到 7 档，并标注实际发送值。
4. 每轮样本数、故障重试次数改为自定义数值。
5. 复杂度自动下限支持系数调节。
6. 题目轮换由「按分钟取模」改为「按轮次顺序表」，并支持自定义题目与排序。

---

## 二、改动清单

| 编号 | 内容 | 主要文件 |
|---|---|---|
| R1 | 输入 Token 异常判定 | InputAudit、TokenGuard、RunDetailActivity、MainActivity、LineConfig、LineEditActivity |
| R2 | 历史页入口拆分 | MainActivity |
| R3 | 思考强度 7 档 | LineConfig、LineEditActivity、ApiClient |
| R4 | 样本数与重试次数 | LineConfig、LineEditActivity、MonitorEngine、RetryPolicy、MainActivity |
| R5 | 复杂度下限系数 | LineConfig、LineEditActivity、MonitorEngine、MainActivity |
| R6 | 题目顺序表 | LineConfig、MonitorEngine、MainActivity、新增 PromptOrderActivity |
| R7 | 轮次推进规则 | MonitorEngine |

（手机上看表格窄，下面每条都用列表重写一遍。）

---

## 三、逐项规格

### R1 输入 Token 异常判定

#### 现状

- `InputAudit.estimate()` 末尾固定写入：
  `e.countReliable=false; e.expectedInputTokens=null;`
- `InputAudit.shouldAlert()` 的前置条件是 `e.countReliable==true`
  且 `e.expectedInputTokens!=null`。
- 结果：告警分支永不成立。实测 请求正文 264 字节、本地估算 26 Token、
  接口回报 26688，界面无任何提示。
- `LineConfig.inputTokenExcess`（默认 500）从未被使用到。

#### 缺陷定性

不是阈值没到，是判定通道被封死。属于逻辑缺陷，不是配置问题。

#### 规格

判定分三级，输入侧只新增「注入」一种标记：

- 正常：不显示标记。
- 异常：显示蓝色「注入」气泡。

判定前提（三者同时成立才参与判定）：

1. `contentVerified == true`（提示词逐字核对通过）
2. `wireBytesVerified == true`（发送字节核对通过）
3. 接口回报的输入 Token > 0

判定模式二选一：

- 数量模式（默认）
  - 条件：`reported - estimated > tokenAlertCount`
  - 默认 `tokenAlertCount = 800`
- 倍率模式
  - 条件：`estimated > 0 && reported >= estimated * tokenAlertRatio`
  - 默认 `tokenAlertRatio = 5.0`

兜底规则（本地分词不可用、`estimated <= 0` 时启用）：

- 条件：`reported > bodyBytes`
- 依据：任何分词器的一个 Token 至少占 1 字节，回报数大于正文字节数即为不相容。

优先级：数量/倍率模式可用时按所选模式判定，不同时叠加；本地估算不可用时才走兜底。

#### 状态解耦（决策）

2.3 中 `inputTokenAlert=true` 会把 `runData.status` 直接置为 `ERROR`。
2.4 改为独立标记：

- 新增 `RunData.inputTokenInjected: boolean`
- 新增 `RunData.SampleData.inputTokenVerdict: String`（`ok` / `suspect`）
- **不再**因为注入把 `status` 改成 `ERROR`
- 原因：用户要求卡片保留「通过」气泡，在它左边另加一个蓝色「注入」气泡。
  两者并存，信息更清楚。

由此带来的既定后果，需在交付说明中写明：

- 注入记录不会进入「异常」状态筛选
- 不会触发「连续异常时通知我」
- 只体现在历史卡片气泡与详情页文字

如后续希望注入同时计为异常，只需在 `TokenGuard.assess()` 里加回一行置 `ERROR` 的判断，不影响其余逻辑。

#### 卡片显示

- 位置：历史卡片右上角，现有状态气泡（如「通过」）的左侧
- 尺寸：与状态气泡同高、同圆角、同字号、同内边距
- 颜色：蓝 `#2F6FB2`，浅底深字，参照现有气泡配色规则
- 文字：「注入」
- 触发：`run.inputTokenInjected == true`
- 间距：两个气泡之间 6dp

#### 详情页文字

`RunDetailActivity` 中现有代码：

```java
String count = audit.countReliable && audit.expectedInputTokens != null
    ? "预检输入 " + audit.expectedInputTokens + " Token"
    : "本地估算 " + (audit.estimatedInputTokens > 0 ? audit.estimatedInputTokens : "—")
      + " Token（不据此告警）";
```

改为三行结构：

```
本地估算 26 Token
接口回报 26688 输入 Token
多出 26662 · 数量模式容许 800 → 判为异常
```

- 未触发时第三行为：`多出 26662 · 数量模式容许 800 → 正常`
- 走兜底时第三行为：`回报 26688 > 请求正文 264 字节 → 判为异常（本地估算不可用）`
- 走倍率模式时为：`约为估算的 1026 倍 · 倍率模式容许 5 倍 → 判为异常`
- 删除「（不据此告警）」字样

#### 设置项

「04 输入检查」分组内调整为：

- 开关：`启用输入 Token 异常标记`
- 模式：数量 / 倍率（单选）
- 数量模式的输入框：`最多允许多出 __ Token`，默认 800，范围 0～10,000,000
- 倍率模式的输入框：`达到估算的 __ 倍`,默认 5.0，范围 1.1～1000.0
- 切换模式时只显示对应输入框，另一项保留原值不丢

### R2 历史页入口拆分

#### 现状

`MainActivity` 历史页顶部，「趋势」与「批量管理」两个胶囊紧贴，共用外框，
视觉上连成一整块。

#### 规格

- 拆成两个等宽按钮，横排，各占可用宽度的 50%
- 中间间距 12dp
- 各自独立背景与圆角，不再共用外层容器
- 外观延续：米白底、墨绿字、圆角胶囊
- 点击行为不变：左进趋势页，右进批量管理页
- 与下方「线路 / 模型 / 状态筛选」保持 12dp 间距

### R3 思考强度扩到 7 档

#### 取值与显示

下拉项显示为「中文（英文）」，英文即实际发送值：

- 服务端默认（auto）→ 不发送该参数
- 关闭（none）
- 低（low）
- 中（medium）
- 高（high）
- 超高（xhigh）
- 最大（max）

#### 各协议发送规则

- `openai`：`reasoning_effort` 原样发送取值字符串
- `responses`：`reasoning.effort` 原样发送
- `anthropic`：该协议无 xhigh / max 字样，转为思考预算
  - low → 4000（原有）
  - medium → 10000（原有）
  - high → 24000（原有）
  - xhigh → 48000（新增）
  - max → 64000（新增）

#### 约束

- anthropic 下仍要求 `max_tokens > budget_tokens`，不满足抛出既有异常
  「输出上限必须大于所选思考预算」，**不自动抬高 max_tokens**
- 选中 xhigh / max 时，字段下方显示一行小字，写明实际发送内容：
  - openai / responses：`实际发送 reasoning_effort = xhigh`
  - anthropic：`实际发送 thinking.budget_tokens = 48000`

#### 校验说明

`InputAudit.freeze()` 的白名单校验的是**字段名**（`reasoning`、
`reasoning_effort`、`thinking`），不校验取值，新增两个取值不会被拦。
无需改动白名单。

### R4 样本数与重试次数

#### 现状

- 每轮样本：下拉 1～5 张
- 本轮最多补试：下拉 0～2 次
- 且 `MonitorEngine.prepare()` 中有
  `Math.max(1, Math.min(5, lineConfig.sampleCount))`，超过 5 会被静默截断
- `RetryPolicy.maxAttempts()` 中有 `Math.min(2, i) + 1`，超过 2 会被静默截断

#### 规格

- 每轮样本：数字输入，范围 1～99，默认 1
- 本轮最多补试：数字输入，范围 0～20，默认 2
- 超范围：红字提示并阻止保存，不静默纠正
- 补试次数 > 5 时，输入框下显示一行：
  「补试次数较多，每次补试都可能重复计费」
- 必改的两处硬上限：
  - `MonitorEngine.prepare()`：`Math.min(5, …)` → `Math.min(99, …)`
  - `RetryPolicy.maxAttempts()`：`Math.min(2, i)` → `Math.min(20, i)`
- `MainActivity` 里「批量修改定时规则」对话框若仍用 0～2 的下拉，同步改为数字输入

### R5 复杂度下限系数

#### 现状

- 每条线路跑满 5 轮后自动算出 `baseline.lower` 作为复杂度下限
- `LineConfig.baselineLowerOverride` 可手填固定值覆盖

#### 规格

- 新增 `LineConfig.baselineLowerFactor: double`，默认 **0.9**，范围 0.5～1.5
- 生效下限计算：
  - `baselineLowerOverride != null` → 用固定值，**不乘系数**
  - 否则 → `baseline.lower * baselineLowerFactor`
- 未建好基线（`lower == null`）时不生效，行为与现在一致
- 两处计算必须同时改且逻辑一致：
  - `MonitorEngine.prepare()` 中 `runData.lowerUsed` 的计算
  - `MonitorEngine` 跑完后的判定分支中对 `d2` 的计算
- 界面显示算式：
  - `生效下限 87 × 0.9 = 78.3`
  - 手填时：`手动下限 72`
  - 基线未建成时：`基线样本 3/5`
- `MainActivity` 监控卡片里「自动下限 87」这类文案同步带上系数

#### 语义说明

系数 < 1 表示放宽（更不容易判为不合格），> 1 表示收紧。
默认 0.9 即放宽一成。

### R6 题目顺序表

#### 现状缺陷

`LineConfig.promptForRun(long j)`：

```java
return PROMPTS[(int) Math.floorMod(j / 60000, PROMPTS.length - 1)];
```

`j` 为本轮开始时间戳，`j/60000` 为「1970 年至今的分钟数」。
即题目由挂钟时间决定，与第几轮无关。由此产生五个问题：

1. **不可预期**：同一时刻永远同一题；换个分钟数就换一道。
2. **间隔决定能跑到哪几题**（最严重）：
   - 间隔 5 分钟：步长 1，四题轮流，正常
   - 间隔 10 分钟：步长 2，只在四题里跳两道，另两道永远轮不到
   - 间隔 15 分钟：步长 3，四题都出现但顺序倒着走
   - 间隔 20 分钟：步长 4，整除，永远是同一题，轮换失效
3. **自定义题永不生效**：`PROMPTS.length - 1 == 4`，下标 0～3，
   第 5 项（自定义题目）永远选不到。
4. **基线成本翻倍**：基线按题目分开存，四题轮换需 20 轮才全部建成。
5. **趋势被切碎**：趋势按 线路+模型+题目 分组，一轮一换导致同线路曲线断裂。

#### 规格

- 新增 `LineConfig.promptOrder: List<PromptRef>`
  - `PromptRef { String id; String label; String text; boolean builtin; }`
  - 默认包含 4 条内置题：鹈鹕英文、鹈鹕中文、土星环、加菲猫
  - 可追加任意条自定义题，各自有名称与内容
- 新增 `LineConfig.promptCursor: int`，默认 0，按线路保存
- 新增 `LineConfig.promptCursorNote: String`，记录「上一轮失败，未前进」之类的提示
- `promptForRun()` 改为：
  - 取 `promptOrder[promptCursor % promptOrder.size()]`
  - **完全不再读取时间**
  - 整轮所有样本共用同一道题（现有 `runPrepared` 只取一次 promptText，天然满足）
- 轮次推进（见 R7）：一轮成功结束后 `promptCursor + 1` 并保存
- 保留 `promptMode` 字段做兼容：
  - `"fixed"` → 顺序表只放当前 `promptId` 那一道
  - `"rotate"` → 首次打开顺序表页时，用当前 `PROMPTS` 生成初始顺序表
  - 新逻辑不再使用 `promptMode` 决定用题，仅用于迁移

#### 新增页面「题目顺序」

- 入口：编辑线路 → 03 或 04 分组内，按钮「题目顺序」
- 布局：竖向列表
  - 顶部固定一行标注「下一轮」并指向当前 `promptCursor` 那一项
  - 每项显示序号、题目名、来源（内置 / 自定义）
  - 左侧拖拽手柄，长按可拖动排序
  - 右侧上移 / 下移箭头（手机上比拖拽更好点）
  - 右侧删除按钮，至少保留 1 条，删除最后一条时禁用
- 底部「＋ 添加一道题」：弹窗填名称 + 内容，保存为自定义题
- 内置题可勾选是否参与顺序表
- 顺序表按线路独立保存
- 变更顺序表不重置 `promptCursor`，但若游标越界则归零

#### 显示

`MainActivity` 中「轮换题目 · 按题目分别建立基线」等文案改为：
- 顺序表 1 道：`固定题目 · 第 1/1`
- 顺序表多道：`顺序出题 · 第 2/4（加菲猫骑缅因猫）`
- 上一轮整轮失败：追加 `· 上轮未前进`

### R7 轮次推进规则

#### 规则

- 一轮结束后统计成功样本数（`requestOk == true` 的样本数）
- 成功样本数 ≥ 1：`promptCursor + 1`，保存
- 成功样本数 == 0（整轮全失败）：`promptCursor` **不变**，
  并写入 `promptCursorNote = "上一轮失败，未前进"`
- 下一轮成功后清除该提示

#### 理由

- 避免连续失败后指针滑走，用户回头误判测的是哪道题
- 基线按题目统计，失败轮次本就不计入基线，指针不动可避免
  「基线 0/5 但指针已绕一圈」的矛盾状态
- 三张里只要有一张成功，即视为该题已获得有效结果，正常前进

---

## 四、数据结构变更汇总

### LineConfig 新增

- `tokenAlertMode` — String — `"count"` — 判定模式
- `tokenAlertCount` — int — `800` — 数量模式阈值
- `tokenAlertRatio` — double — `5.0` — 倍率模式阈值
- `baselineLowerFactor` — double — `0.9` — 复杂度下限系数
- `promptOrder` — List<PromptRef> — 4 条内置题 — 出题顺序
- `promptCursor` — int — `0` — 当前轮次位置
- `promptCursorNote` — String — `""` — 游标未前进的说明

### LineConfig 保留不动

- `inputTokenExcess`：保留字段仅供旧数据读取，新逻辑不再使用
- `tokenAuditEnabled`：保留，含义改为「启用输入 Token 异常标记」
- `sampleCount`、`retryCount`：字段不变，仅取值范围扩大
- `promptMode`、`promptId`、`customPrompt`：保留，仅用于迁移

### RunData 新增

- `inputTokenInjected` — boolean — 本轮是否命中注入判定
- `inputTokenVerdictNote` — String — 判定依据的文字说明

### RunData.SampleData 新增

- `inputTokenVerdict` — String — `ok` / `suspect`
- `inputTokenExcessValue` — long — 回报减估算的实际差额

---

## 五、兼容与迁移

- 序列化仍走 Gson，新增字段缺失时取默认值，旧记录可正常读取
- 2.3 的 `inputTokenAlert` 字段保留，旧记录读取后仅用于展示，不参与新判定
- `promptMode == "rotate"` 的旧线路，首次进入顺序表页时用内置题生成初始顺序
- `promptMode == "fixed"` 的旧线路，顺序表只含其原本选中的那一题，行为不变
- 旧记录没有 `inputTokenVerdict`，卡片不显示「注入」气泡，不误报
- 数据库表结构不变，仅 JSON 内容字段增加

---

## 六、验收标准

1. 编译通过，能产出可安装的 APK
2. 不联网、不调用真实接口（测试额度已用完），只做编译与本地校验
3. 2.3 的既有数据可正常打开，不崩溃
4. 用构造数据走一遍 R1 判定：
   - 估算 26、回报 26688、数量模式 800 → 判定异常
   - 估算 26、回报 700、数量模式 800 → 判定正常
   - 估算为 0、回报 2000、正文 264 字节 → 走兜底判定异常
5. R4 构造 `sampleCount = 10`，确认实际跑 10 个样本而非 5 个
6. R5 构造 `lower = 87`、系数 0.9，确认生效值 78.3
7. R6 构造顺序表 A/B/C，间隔分别设 5 / 10 / 20 分钟，
   确认第 1、2、3 轮均为 A、B、C，不受间隔影响
8. 交付时逐条列出：改了哪个文件、哪一处、为什么

---

## 七、不改动范围

- 提示词逐字核对、单条 user 消息校验、JSON/UTF-8 往返校验
- 请求正文字节数与 SHA-256 记录
- 非标 usage 字段的记录方式
- 故障重试分类：断线、超时、5xx、429 等临时故障才重试；
  密钥错误、余额不足、模型不存在直接报错，不重复请求
- 输出上限、温度的现有校验规则
- 后台服务、通知、备份、清理等模块

---

## 八、已知限制与风险

- **本地估算精度**：使用 JTokkit 的 o200k_base。对 Claude、Gemini 等模型
  的真实分词器有偏差，中文文本偏差更明显。数量模式在短提示词下（几十 Token）
  800 的阈值足够宽松；若提示词很长，建议改用倍率模式。
- **xhigh / max 的兼容性**：部分中转站不认这两个值，会返回 400 参数错误。
  属于明确错误，不重试、不消耗额外额度。
- **anthropic 预算约束**：选 最大（64000）时，输出上限须大于 64000，
  否则保存被拒。不会自动抬高。
- **基线成本**：顺序表放 N 道题，需 N × 5 轮才能把基线全部建成。
  想尽快看到复杂度判断，可先只放 1 道题跑满 5 轮。
- **注入判定不是定论**：本地估算与接口计数的差额存在合理误差空间，
  蓝色「注入」气泡只表示「用量异常」，最终定性仍需人工核对该次实际请求。
