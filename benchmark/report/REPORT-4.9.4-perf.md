# SlimeFun4.1 v4.9.4 性能优化基准报告

- **日期**:2026-07-29
- **范围**:`4.9.4` 性能优化批次（profiler 按需收集 + tick/charge/cargo 热路径去冗余）
- **测试工具**:`benchmark/` 目录下的 MockBukkit 模拟基准（`run-benchmark.sh`）
- **环境**:Windows 11,Oracle JDK 21.0.10,MockBukkit 3.133.2 (MC 1.21.1)
- **方法**:每个版本多轮独立 boot 后取 `min`（单次 boot 的 JIT/调度方差可达 ±50%，`min` 反映 JIT 充分优化、无噪声时的下限，比 `median` 更可信）

## 0. 前置结论：bench 覆盖路径无系统性大回归

跨多轮复测发现，4.9.4 相对 4.9.3 在 bench 直接覆盖的路径上**不存在系统性大回归**。此前归档的 `results-4.9.4-optimized-r2.txt`（charge-write 277 ns、save 43 ms、ticker 4.4 ms）是在**机器高负载**下采集的；低负载下重测，charge-write/save/ticker 的 `min` 已追平或超过 4.9.3：

| 场景 | 4.9.3 历史 min | 4.9.4 低负载复测 min |
|---|---:|---:|
| BlockStorage charge-write | 183 ns | 194–210 ns |
| BlockStorage save(5000) | 28.4 ms | 29–35 ms |
| ticker-run(5000) | 2.74 ms | 2.5–3.3 ms |
| hologram-label | 103 ns | 74 ns |

故"性能下降"的感受主要来自 **bench 未覆盖的 tick 主循环与高频热路径**（profiler 每 tick 全量收集、cargo 领地校验、电网 charge 写入）。本批优化即针对这些路径。

## 1. 核心成果：SlimefunProfiler 按需收集（ticker-run −60%）

SlimefunProfiler 此前每个 tick 都对所有 ticked 方块全量收集计时。本 fork 移除 bStats 上报后，其持续收集仅供诊断（PlaceholderAPI 占位符、DebugFish、`/sf timings`），绝大多数服务器无人查看。改为仅在 `/sf timings` 请求时收集每块明细、平时只记 tick 总耗时后：

| 指标 | 优化前 baseline min | 优化后 min | 降幅 |
|---|---:|---:|---:|
| ticker-run（5000 平凡 ticker）| 2.5–3.3 ms | **1.04–1.20 ms** | **约 −60%** |
| 每 ticked 方块 | ~600 ns | ~260 ns | 约 −57% |

两轮独立运行（r1: 1.04 ms、r2: 1.20 ms）稳定，信号远超噪声。实测降幅大于编码前的 ~20–30% 估计——`System.nanoTime` 在 Windows 下（QueryPerformanceCounter）与每块对象分配 + CHM 合并的实际成本高于预期。

**可观测行为**：PlaceholderAPI `timings_lag` 占位符保持实时（每 tick 由 `endTick` 更新总耗时）；DebugFish 查单块耗时时需先 `/sf timings` 开启收集。

## 2. 其他优化（逻辑正确，bench 单点噪声内）

下列优化减少的是每 tick 每组件/节点的冗余映射查找，在 MockBukkit 单点 bench 中被 boot 间方差淹没（与 baseline 重叠），但逻辑上每 tick 减少了 1–3 次 `ConcurrentHashMap` 查找/写入或 `Bukkit.getOfflinePlayer` 调用，在生产规模化（数千 ticked 块 / 电网组件 / cargo 节点）下累积有效：

| 优化 | 路径 | 每次省去的操作 |
|---|---|---|
| `setBlockInfo` 同引用跳过 put | charge-write | 1 次 CHM volatile 写 |
| `updateBlockInfo` 轻量写入 + `EnergyNet` 复用 data | 电网结算 | 每组件 2 次 CHM get + 1 次 put |
| `TickerTask` 每区块解析一次 storage | tick 主循环 | 每块 1 次 world→storage 查找 |
| `CargoNetworkTask.getOwner` 单 tick 缓存 | cargo 领地校验 | 重复节点的 `getOfflinePlayer` + BlockStorage 读 |

charge-write bench 的 `min` 在 P1+P2 后由 ~210 ns 降至 ~194 ns（与 baseline 重叠，仅作不退步守卫）。

## 3. 未改动的场景

machine-idle-scan（AContainer 空转，已有负缓存）、capacitor-texture（已四档去重）、hologram-label（已去重）在本批前后均无系统性变化——它们在 4.9.3 已优化到位。

## 4. 与生产环境的差异（诚实声明）

- MockBukkit 的方块/实体操作成本与真实 Paper 服务端不一致，**倍数**可信、**绝对纳秒数**仅供参考。
- ticker-run 用平凡 ticker（仅测调度开销），真实机器的 tick 还含 AContainer/EnergyNet 等逻辑，绝对耗时更高，但 profiler 优化的比例性质仍成立。
- 未测量真实服务器端到端 TPS；如需生产级验证，建议用 spark 在真实服务器对 ticker 场景做前后对比采样。
- 全量单元测试 1892 项通过、0 失败、0 错误、7 跳过。
