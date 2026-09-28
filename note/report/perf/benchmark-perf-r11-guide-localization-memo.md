# 性能基准报告 · 第 11 轮：指南本地化读路径记忆化——字符串查找三级缓存 + lore 行级 memo

- **日期**：2026-09-29
- **A/B**：base = `322cd4b62`（r11 bench 提交，worktree `../sf-perf-r11base`）vs opt = 本轮（仅 `SlimefunLocalization.java` 单文件）。
- **方法**：新场景 `guide-render`（36 物品满页分类 + 微基准锚点）。交错 **9 对**（base 先、每 run 独立 JVM），复合 lean（逐对取全部非 guide-render 场景原始比值中位数）。主指标 min-of-runs，次指标 median。

## 优化内容

1. **字符串查找读穿透 memo**：`getStringOrNull` 的内部收口为 `cachedString(language, file, path)`——`Map<Language, Map<LanguageFile, ConcurrentHashMap<path, Optional<String>>>>` 三级嵌套（命中零分配，键天然按文件隔离，`null` 结果也 memo）。收益面覆盖 `getMessage`（指南每按钮）、`getItemName`（每显示物品）、`getItemGroupName`、`getResearchName`、`getResourceString` 全部纯读路径。
2. **lore 翻译行级 memo**：`translateLore(Language, phrases, lore)` 对每条 lore 行查 `Map<Language, ConcurrentHashMap<String,String>>`——lore 行跨物品高度重复（同等级机器共享 LoreBuilder 标签），短语扫描每语言每行只跑一次；短语表为空的语言保持原 `new ArrayList<>(lore)` 快路径。
3. **显式失效钩子** `invalidateTranslationCaches()`：清三级缓存 + 行 memo + 短语表。语言文件启动装载后第一方代码不重写（`lorePhraseCache` 同一前提的既有先例）；插件运行时 `Language#setFile` 换文件属带外操作，须调用此钩子——与 ItemFilter/routing/hasEnabledItems 缓存同族契约。
4. **明确不做**：`getLocalizedItem` 仍每次返回新鲜克隆（共享显示实例会改变插件可观测的引用语义——红线）；`ItemMetaSnapshot` 作为 lore 源（快照对直改 delegate meta 的带外变异有陈旧语义风险）。

## 判别测试（TestGuideLocalizationCaching，4 项）

行 memo 正确性 + 返回列表防御新鲜性（篡改第二次返回不污染 memo）+ 失效后重算正确；带外契约钉死（config 直改→memo 稳定→invalidate 后可见）；**文件域键隔离**（items 与 messages 同路径互不串值）；getLocalizedItem 每次新鲜克隆 + lore 已翻译。测试环境与 bench 同法注入 zh-CN + en（生产 `addLanguage` 装载器）。

## 量化结果（交错 9 对，复合 lean 校正；本会话 lean 逐对 0.949–1.030）

| 指标 | base | opt | 校正中位比值 | 收益 |
|---|---|---|---|---|
| lore-translate min | 1 158 ns | 46.7 ns | 0.046 | **-95.5%** |
| lore-translate median | 1 387 ns | 47.6 ns | 0.036 | **-96.4%** |
| message-lookup min | 176 ns | 13.6 ns | 0.086 | **-91.5%** |
| message-lookup median | 244 ns | 17.0 ns | 0.078 | **-92.2%** |
| item-name-lookup min | 69 ns | 64 ns | 0.893 | -11%（小收益，亚百 ns 量化带） |
| localized-item min | 33.9 µs | 31.7 µs | 0.948 | -5.6%（mock 膨胀分母，见下） |
| category-open min | 1.208 ms | 1.185 ms | 0.985 | -2.0%（同上） |
| item-clone（锚点） | 441 ns | 412 ns | 0.991 | 不变（meta 地板未被触碰）✓ |

- lore-translate 9/9 对原始比值 0.031–0.071、message-lookup 9/9 对 0.049–0.144——方向完全一致。
- 守卫（非 guide-render 全场景）校正中位 ±10% 带内、方向双侧（0.79–1.31 离群值均为噪声特征）；r11 diff 仅单文件 SlimefunLocalization，charge/generator/cargo/ticker 路径零触及（代码归因排除）。

## MockBukkit 膨胀注记（本轮最重要的测量学发现）

`localized-item`/`category-open` 的分母被 **MockBukkit `ItemMetaMock.getLore()` 的实现支配**：字节码证实其对**每条 lore 行**做 `GsonComponentSerializer.gson().deserialize(line)` → `LegacyComponentSerializer.legacySection().serialize(...)` 往返（6 行 ≈ 40µs，占 localized-item 33.9µs 中的 ~32µs，95%）。生产 Paper 的 `ItemMeta.getLore()` 是廉价拷贝，该成本不存在。

**生产比例推算**（用锚点分解）：mock 上 localized-item = meta 地板 0.44µs + 翻译 1.16µs + 名字 0.07µs + **meta Gson 往返 ~32µs**。Paper 上同路径 ≈ 克隆+meta 读写 1–3µs + 翻译 1.16µs + 名字 0.07µs ≈ 2.2–4.2µs，其中本轮消除的 ~1.2µs 占 **30–55%**；分类页打开（36 项）每页省 ~43µs 纯字符串工作。micro 基准（lore-translate/message-lookup）无 meta 参与，数字直接可信。

## 结论

**保留（ship）**：lore 翻译扫描 **-95~96%**（1.16µs → 47ns/行组）、消息查找 **-91~92%**（176 → 14ns），9/9 对同向；名字查找小赢；端到端在 mock 分母下 -2~-6%（生产比例 30–55%，见注记）。全量 **3258 测试 0 失败**（3254 + 4 新增）；`mvn package` BUILD SUCCESS。原始数据：`benchmark/report/results-r11{base,-}p{1..9}.txt`。
