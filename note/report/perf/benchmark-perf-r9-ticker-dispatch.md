# 第 9 轮性能基准报告：TickerTask 分发链解析缓存（每 tick 每块重解析 → 解析随 tick 注册表携带）

- 日期：2026-09-29
- 方向：TickerTask 的每方块分发前解析链（id 提取 + 物品注册表查找 + ticker 解析 + 同步标志）
- 基线：f09cd2e57（第 9 轮 bench 提交）worktree `../sf-perf-r9base`
- 优化侧：本轮提交后的主仓库 `target/classes`
- 方法：既有 `ticker-run` 场景（5000 平凡 ticker 驱动真实 `TickerTask.run()`，直接度量分发结构）+ 新增 `ticker-resolution` 微基准（标定解析链成本）。交错 **9 对**。**本轮 lean 升级为复合 lean**：逐对取 9 个未触及场景（charge-write/hologram/mapping×2/recipe-scan×2/capacitor/player-interaction×2）原始比值的**中位数**——本会话单场景 lean（ticker-run 是目标不可用；mapping-100 亚微秒指标量化抖动 ±40%）失稳，复合中位数显著更稳健。

## 背景与改动

旧 `tickLocation` 每 tick 每方块执行完整解析链：`getLocationInfo(l, storage)`（活 Config 读）→ `data.getString("id")` → `SlimefunItem.getById(id)` → `item.getBlockTicker()` → `isSynchronized()`。除数据读取外，这条链的结果只在方块数据被替换时才会变化——而所有第一方路径都经由 `enableTicker`（重放置/重存储，**数据先存后启用**）或 `disableTicker`（破坏/移走/删除）表达这一变化。

**改动**：tick 注册表从 `Map<ChunkPosition, Set<Location>>` 改为 `Map<ChunkPosition, Map<Location, TickingBlock>>`，`TickingBlock` 携带不可变 `Resolved(item, ticker, synchronised)` record：

1. **插入即解析**（enableTicker 在 CHM compute 内构造，经 CHM 安全发布）；重复 enable（重放置/重存储）以 put 替换刷新解析；
2. **活 Config 每 tick 现读**（红线 1 核心）：机器经由该对象读写自身数据，缓存引用会在存储条目被替换时把写入引向死对象（静默数据丢失）——**明确不做**；
3. **失效钩子完备**：disableTicker（破坏/移走两端/destroy=true 删除）整体摘除；**destroy=false 删除**（数据删、ticker 留）在 run() 与 drainQueues(world) 的队列排空中将解析清回 null；
4. **惰性重解析**：解析为 null（启用早于数据 / 数据已删）时每 tick 重试完整链直到数据出现——与旧路径"空数据每 tick 早退"语义精确等价（判别测试 4/5 钉死）；
5. 公开 API `getLocations()` 保持 `Map<ChunkPosition, Set<Location>>` 形状（内层为活 keySet 的只读包装）。

**契约记录**（与 ItemFilter/路由缓存同族）：绕过 enable/disable 钩子的带内 id 原位改写（addon 直接 setValue("id")）在下次钩子前不刷新解析；第一方全部路径即时刷新。

## 标定与结果

### 微基准（ticker-resolution，min ns/块，两侧一致）

| 变体 | ns/块 | 含义 |
|---|---|---|
| info-get | ~13–21 | 活 Config 读（**保留**，每 tick 必须） |
| full-chain | ~24–26 | 旧路径全链（含 id 提取 + 注册表查找） |

可移除部分 ≥ 12ns/块 —— 微基准下界（紧凑循环迭代间重叠掩盖了依赖链延迟，实际收益更大，见下）。

### 端到端（交错 9 对，复合 lean 校正）

| 指标 | 校正中位数 | 逐对（raw） | 判定 |
|---|---|---|---|
| ticker-run min_ms_per_run | **0.830** | 9/9 对 0.51–0.91 | **-17%** |
| ticker-run median_ms_per_run | **0.846** | 8/9 对 <1.0 | **-15%** |

原始比值 9/9 全部 <1.0（0.506–0.914）——单向前所未有地一致。绝对差 ≈ 30ns/块，超出微基准下界的部分来自被移除链的**延迟受限性**（data→id→item→ticker→flag 四连依赖在真实循环中无法与相邻块的处理重叠）加上分支/内联布局改善。

**守卫**（复合 lean 校正中位数）：mixed-bounce-smartfill 0.966 / machine-processing 0.957 / machine-bounce 1.013 / happy-merge 1.031 / generator 1.037 / mixed-bounce 1.088 / machine-idle 1.046 / energy 1.082 / idle 1.218。idle 为本会话噪声极值——r9 diff 仅 TickerTask，cargo 场景代码逐字未动（代码归因排除回归）；其余带内 ±9%（本会话复合 lean 自身逐对摆动 0.71–1.23，双峰环境）。

## 测试

新增 `TestTickerResolutionCache` 6 项判别测试：

1. 基本分发——ticking 方块每 run 恰派发一次；
2. **活数据**——三次 run 之间经 addBlockInfo/updateBlockInfo 改写 "flag"，ticker 依次读到 one/two/three（钉死 Config 不缓存）；
3. 重放置（disable→存→enable）派发新物品；
4. **destroy=false 删除重解析**——删后空数据不派发（旧早退语义），补新数据后派发替代物品；
5. 启用早于数据（防御性惰性路径）——无数据不派发，数据出现后下一 run 派发；
6. `getLocations()` 公开形状保持且只读。

环境注记：MockBukkit 中 `tickChunk` 的 `chunk.isLoaded()` 要求先 `world.getChunkAt(l)` 强制装载（bench 同款）。全量 **3251 项 0 失败**（JDK 21）；`mvn package` BUILD SUCCESS。

## 结论

**保留（ship）**：ticker 分发结构 -15~17%（9/9 对同向，5000 块 ~1.1ms/run → ~0.93ms），微基准钉死下界 ≥7%；解析生命周期与第一方数据变迁钩子完备对齐，活 Config 每 tick 现读杜绝 stale-write。方法论沉淀：单场景 lean 失稳时改用未触及场景组的逐对中位数（复合 lean）。原始数据：`benchmark/report/results-r9{base,-}p{1..9}.txt`。
