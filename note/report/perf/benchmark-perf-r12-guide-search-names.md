# 性能基准报告 · 第 12 轮：指南搜索路径——物品显示名与可搜索名计算一次缓存

- **日期**：2026-09-29
- **A/B**：base = `3ae83e3f7`（r12 bench 提交，worktree `../sf-perf-r12base`）vs opt = 本轮（`SlimefunItem.java` 名字缓存 + `SurvivalSlimefunGuide.java` 过滤器一行）。
- **方法**：新场景 `guide-search`（500 物品 = 中型附属服形态；search-miss 全量走零命中 / search-hit 稀疏 needle 命中 34 项防早退 / item-name 微基准）。交错 **9 对**，复合 lean（非 guide-search 全场景）。主指标 min-of-runs。

## 优化内容

1. **`SlimefunItem.getItemName()` 计算一次缓存**（`volatile String cachedItemName`）。原实现每次调用走 `ItemUtils.getItemName(itemStackTemplate)` —— 字节码证实为 `getItemMeta()` 全量往返 + `hasDisplayName` + `getDisplayName`（Paper 上每次分配 meta 拷贝；MockBukkit 上 ~1µs）。搜索循环对每个启用物品各付一次。
2. **新增 `getSearchableName()`**：`stripColor(getItemName()).toLowerCase(ROOT)` 的规范化形式，同样计算一次（`volatile String cachedSearchableName`）。原 `isSearchFilterApplicable` 对每物品每搜索做正则 stripColor + toLowerCase 分配。
3. **零失效钩子**：`itemStackTemplate` 为 `private final`、构造后从不替换（无 setter，全部为读）——比 r10 的 hasEnabledItems 缓存前提更强（后者需要 setResearch/register 失效钩子），缓存输入严格不可变。
4. **附带收益**：`getItemName()` 的其他调用方（canUse 未解锁提示、give 命令、附魔机提示等）自动受益。

## 判别测试（TestSearchNameCaching，3 项）

缓存值 ≡ 未缓存计算（ItemUtils 直算对照）；可搜索名无色码、小写、跨调用稳定；**功能等价**：40 物品（4 个稀疏 needle）经真实 `SlimefunGuide.openSearch` 走完整匹配循环，打开的菜单中恰好 4 个结果（类型计数断言——显示名经本地化层，单测环境为既有 "Error: No language present" 上游行为，非本轮引入）。

## 量化结果（交错 9 对，复合 lean 校正；本会话 lean 逐对 0.989–1.032）

| 指标 | base | opt | 校正中位比值 | 收益 |
|---|---|---|---|---|
| search-miss min | 774 954 ns | 57 195 ns | 0.063 | **-93.7%** |
| search-miss median | 974 458 ns | 68 902 ns | 0.069 | **-93.1%** |
| search-hit min | 1 359 114 ns | 407 119 ns | 0.295 | **-70.5%** |
| search-hit median | 1 635 043 ns | 492 434 ns | 0.309 | **-69.1%** |
| item-name min | 940 ns | 5.3 ns | 0.005 | **-99.4%** |

- search-miss 原始比值 9/9 对 0.052–0.094；search-hit 9/9 对 0.267–0.418；item-name 9/9 对 0.003–0.010——全部同向无例外。
- search-hit 的残余 407µs 主要为 34 个结果物品构建（CustomItemStack 克隆 + meta，r11 已定性的 mock 膨胀面，本轮不动）+ 剩余每物品 ~115ns 的 contains/equals 匹配。
- **生产缩放**：中型服 500 物品搜索 775µs→57µs；3000 物品大型附属服线性外推 ~4.6ms→~0.34ms——每次玩家搜索提交的主线程停顿从可感知降到不可感知。

## 守卫（未触及场景）

校正中位双侧分布 0.73–1.23（research-progress unlock-cycle 0.75 偏快侧、protection-query xheavy median 1.23 偏慢侧——均为本会话噪声特征，与前几轮同场景的逐轮摆动一致）；r12 diff 不触及任何守卫场景路径（代码归因排除）。

## 结论

**保留（ship）**：搜索全量走 **-93%**（775µs→57µs/500 物品）、命中路径 **-70%**、物品名读取 **-99.4%**（meta 往返消除），9/9 对全部同向；缓存输入严格不可变（private final 模板），零失效负担。判别测试 3 项 + 全量 **3261 测试 0 失败**（3258 + 3 新增）；`mvn package` BUILD SUCCESS。原始数据：`benchmark/report/results-r12{base,-}p{1..9}.txt`。
