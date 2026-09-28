# Benchmark 性能报告 r14 — item 身份解析记忆化与模板比较元缓存

- 轮次：r14（物品身份解析与比较路径——cargo 路由/配方匹配/背包校验共用的工作horse）
- 基线：r14base = `286f85a83`（bench 提交点，含未优化解析路径）
- 对照：优化后工作树（`perf(r14)` 提交）
- 方法：bench 全场景交错 9 对（base 先行、独立 JVM、min-of-9 主指标）、复合 lean 配对校正（lean 池 = 全部非 item-compare 变体）
- 判别测试：`TestItemIdentityResolutionMemo`（6 项全绿）
- 全量回归：3278 tests / 0 failures / 0 errors

## 优化点

1. **`SlimefunItem.getByItem(ItemStack)` 身份记忆化**：稳定长寿命堆栈实例（cargo 过滤器、配方缓存、玩家手持、菜单槽位——Paper 事件下发 live 引用）被反复解析，每次未命中都为读 PDC 付一次 `getItemMeta()` 完整克隆（真实 Paper 上是 NBT 深拷）。改为 Guava `MapMaker().weakKeys().weakValues()` 并发 map：键**身份比较**（`==`，可变堆栈永不互串）、键值皆弱引用（随堆栈 GC，不泄漏）；null 结果也 memo（`Optional` 包装）。
2. **`SlimefunItem` 模板比较元缓存**：`isItemSimilar` 的"香草同材质物品 vs 注册模板"分支（cargo 过滤器处理香草物品的常态路径）原每次比较 `sf_sfitem.getItem().getItemMeta()` = 模板栈克隆 + 元克隆**双重分配**，改为 `getTemplateItemMeta()` 计算一次的 `volatile` 缓存——与 r12 名字缓存同一不可变前提（`itemStackTemplate` 严格不可变、无 setter），零失效钩子。

## 语义等价论证（红线 1/2）

- 解析结果 memo 前后逐例相同：身份键消除可变堆串扰；判别测试钉死注册解析≡memo 解析、香草/异材质 null 稳定。
- **失效钩子**：(a) `register()` 尾部全清——晚注册（启动后启用的附属插件）会把先前 null 解析翻转为非 null，注册是稀有事件，全清优于追踪；(b) 公开 `invalidateItemResolutionCache()` 供带外重打标（附属经 `CustomItemDataService#setItemData` 改写既有实例的 id PDC——第一方路径从不在创建后改写 id，与 r11 `invalidateTranslationCaches()` 同族契约）。
- **明确不做**：`equalsItemMeta(ItemMeta, ItemMetaSnapshot, …)` 快照重载虽在仓库中存在且模板侧 displayName 预计算更优，但**缺药水基类型检查**（#3133 修复）——采用即功能丢失，红线否决，本轮保留 (ItemMeta, ItemMeta) 双元重载仅换元来源。
- 比较语义逐字保留：药水检查（判别测试：同名同基类型匹配、异基类型拒绝——注意比较对象是**注册模板**的元，测试已按此语义钉死）、lore/CMD 检查、DistinctiveItem 分支（PDC 同 id 走注册模板元为 pre-existing 上游语义，测试文档化；区分力在无 PDC lookalike 分支经实参元行使——测试钉死）。
- 弱键 map 的 GC 正确性：Guava `weakKeys()` 用 `System.identityHashCode` + 引用队列到期清扫，无强引用泄漏；并发安全（`makeMap()` 返回 ConcurrentMap）。

## 量化结果（交错 9 对，min 主指标，复合 lean 校正）

| 变体 | base 中位 (ns) | opt 中位 (ns) | 校正比率中位 | 同向对 | 校正区间 |
|---|---:|---:|---:|---:|---|
| resolve-template（稳定模板实例） | 128.8 | 27.9 | **0.216（-78.4%）** | **9/9** | 0.197–0.262 |
| sim-sf-hit（PDC 双侧 ID 命中） | 295.1 | 95.3 | **0.322（-67.8%）** | **9/9** | 0.008–0.413 |
| resolve-vanilla-shared（同材质香草） | 65.9 | 24.7 | 0.374（-62.6%） | 9/9 | 0.322–0.563 |
| resolve-vanilla-foreign（守卫：材质快负查） | 49.9 | 21.9 | 0.433（-56.7%） | 9/9 | 0.332–0.617 |
| sim-vanilla-miss（香草 vs 模板全链） | 1926.3 | 1552.0 | 0.791（-20.9%） | 8/9 | 0.613–0.995 |
| resolve-fresh（守卫：每次新实例） | 263.6 | 456.0 | **1.624（+62.4%）** | 0/9 | 1.230–2.434 |

- lean 池逐对比率 0.962–1.053（中位 1.008），无系统性漂移。
- 五个命中形态变体全部 8-9/9 同向；主判据 resolve-template 区间极紧（0.197–0.262）。
- sim-vanilla-miss 剩余成本为被测侧元克隆 + 双侧 displayName/lore 的 Component 读（MockBukkit 下每行 Gson 往返，r11 已证）——真实 Paper 上模板侧克隆消除的占比更大。

## 测量学与守卫披露

- **resolve-fresh +62%（0/9）为诚实代价**：纯 churn 形态（每次全新实例解析一次即弃）每次多付 ~190ns（弱键插入 + 到期清扫；变体含 bench 侧 clone，两侧同付故 delta 纯为 memo 开销）。**生产占比论证**：MockBukkit 的 meta 克隆仅 ~120ns，插入成本相对显大；真实 Paper 的 `getItemMeta()` 为 NBT 深拷（µs 级），插入 <20% of miss 总成本。生产解析绝大多数为稳定实例（cargo 每 tick 重探同槽位、过滤器/事件持 live 引用）→ 命中形态主导。
- MockBukkit 保守下界方向反转注记：r11 起"mock 数字为保守下界"适用于克隆消除类收益（mock 克隆便宜 → 显示收益偏小）；**插入/分配类守卫成本在 mock 下占比偏大**——两个方向都需锚点分解，不可硬解读。
- `getSlimefunItemMaterials()` 已为并发缓存集合（非每调用重建），此前担心的 O(N) 隐藏成本不存在。

## 护栏与风险

- `getTemplateItemMeta()` 为公开只读契约（javadoc 声明禁止变异）——仅被 `SlimefunUtils` 私有只读比较器消费，变异面为理论新增。
- memo 失效覆盖：晚注册（钩子）+ 带外重打标（公开方法+文档）；物品注销路径在本代码库不存在（物品仅启动期注册）。
- 守卫回归以绝对值计 ~190ns/解析，仅影响纯 churn 形态；权衡已量化披露，如后续 profiling 显示 churn 形态在生产中占比超预期，可回退单点（memo 保留模板元缓存）。

## 结论

判定：**合入**。9/9 同向、区间紧的主判据（-78%）+ 全家族命中变体同向 + 3278 全量回归绿 + 语义等价判别测试（含药水检查存活、带外契约、晚注册失效）；守卫 churn 形态 +62% 已按测量学论证其在真实 Paper 下的占比 <20%，权衡披露在案。
