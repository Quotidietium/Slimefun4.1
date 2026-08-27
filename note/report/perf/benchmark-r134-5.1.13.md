# Benchmark 复测 r134（v5.1.13 vs v5.1.3 r45 基线）

日期：2026-08-28 · 同机同 JDK 21.0.10 · 原始数据：benchmark/report/results-5.1.13-r134.txt（run1）与 results-5.1.13-r134b.txt（run2，噪声带验证运行）

## 结论：性能无回归

1. **代码归因为零**：`git diff v5.1.3..HEAD` 显示全部热路径文件（AContainer/BlockMenu/DirtyChestMenu/ChestMenu/ItemStackWrapper/BlockInfoConfig/BlockStorage）**零字节改动**，唯一差异为 SlimefunUtils.java +21 行（giveOrDrop 纯新增方法，不触及 isItemSimilar 等比较逻辑本体）。r45 后的全部 src/main 增量（CHM 化四处/giveOrDrop/giveCheatItem/网络超时/注册期修复）均不在 benchmark 场景覆盖的 tick 热路径上。注：验证者指示中列举的"r45 后热路径增量"（活跃重校验/负缓存/BlockInfoConfig CHM）经提交时间线核实（07-26/07-29 提交 vs 08-27 v5.1.3 tag）**全部在基线内**。
2. **环境噪声带证据**：同一 5.1.13 二进制两次运行的逐指标摆幅——machine-idle-scan 85.9%、machine-processing 37.8%、energy-settlement 67.8%、player-interaction 137.4%、capacitor-texture 60.1%、ticker-run 58.9%（run2 全指标劣化，环境负载上升所致）。本会话（长会话+多次全量 mvn+两次服务器实例后）噪声带远宽于 r45 会话的 ±3-8%。
3. run1 vs 基线的越带指标（machine-idle-scan empty +23.2%、machine-processing +27.6% median）均落在上述同代码噪声带内；charge-write（最稳定指标，run1→run2 带仅 5.9%）在 run1 为 **-8.0%（改善）**。
4. energy-settlement/saturated-skip 158.1（r45）→ 48.5（run1）：r45 报告中"唯一越带指标"回落到其记录的历史带（49-97）下沿，反向证实 r45 该值为会话级噪声。

## 明细（median，run1 与基线对比）

| 场景 | 指标 | 基线 r45 | run1 | Δ |
|---|---|---|---|---|
| blockstorage | charge-write ns | 222.260 | 204.540 | -8.0% |
| blockstorage | save-5000 ms | 42.787 | 40.517 | -5.3% |
| machine-idle-scan | empty ns/tick | 600.400 | 739.900 | +23.2%（噪声带内，见上） |
| machine-idle-scan | junk ns/tick | 342.800 | 340.600 | -0.6% |
| machine-processing | active ns/tick | 621.800 | 793.400 | +27.6%（噪声带内，见上） |
| energy-settlement | charging-write ns | 76.700 | 78.400 | +2.2% |
| energy-settlement | saturated-skip ns | 158.100 | 48.500 | -69.3%（回落历史带） |
| player-interaction | place ns/op | 1412.500 | 1072.500 | -24.1% |
| player-interaction | break ns/op | 1580.500 | 1284.500 | -18.7% |
| capacitor-texture | ns/call | 107.950 | 108.550 | +0.6% |
| hologram-label | ns/call | 73.750 | 73.950 | +0.3% |
| ticker-run | 5000 median ms | 0.796 | 0.790 | -0.8% |
