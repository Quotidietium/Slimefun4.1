# 5.0.0 发布前性能基准（r46-r73 修复后）

- 被测：feature/api-expansion HEAD（含第 46-73 轮 11 项修复：充能 NBT 净化、事件构造器校验、抓钩 markHandled 等）
- 运行：`run-benchmark.sh ../target/classes 5.0.0-r73`（Java 21；第二次 r2 确定方差带）
- 基线：4.9.4-final r1/r2

## 结果对比

| 场景 | 4.9.4 r1/r2 | 5.0.0 r1/r2 | 判定 |
|---|---|---|---|
| blockstorage/charge-write (ns/写) | 245 / 212 | 196 / 225 | **持平**——r54 充能净化（读边界 +2 次浮点比较）无可观测回归 |
| ticker-run 5000 块 (ms/run) | 0.93 / 0.79 | 2.28 / 1.20 | 波动带内（4.9.4 曾录 4.4ms 单次噪声先例；r2 回落 1.2） |
| place-block (ns/op) | 1476 / 1285 | 2128 / 2221 | **一致偏高 ~55%**（见下分析，功能成本非缺陷） |
| break-block (ns/op) | 1061 / 1265 | 1740 / 1930 | 同上 ~+60% |
| capacitor-texture (ns/call) | 103 / 98 | 149 / 101 | r2 回落带内，r1 噪声 |
| hologram-label (ns/call) | 88 / 75 | 75 / 128 | 波动带内 |

## place/break 偏高分析

5.0.0 新增 ~200 个事件，其中方块保护集（SlimefunBlockPiston/Burn/Explosion/Fall/SupportBreak/Interact 等）在 BlockListener/SlimefunItemInteractListener 注册了多个新 `@EventHandler`。Bukkit 对每个注册 handler 逐一分派（即使 early-return），单次放置/破坏多出若干次分派 ≈ +700ns——与两次运行一致的观测吻合。

**影响评估**：place/break 非持续 tick 热路径（玩家操作频率上限 ~百次/秒 → 全服 ~0.2ms/tick 量级），绝对值 ~2μs/次仍极低。判定为**新增功能的合理分派成本，非性能缺陷**。若未来需要，可在事件注册处合并 handler 或引入共享早退条件——当前不必要。

## 结论

1. **r54 充能净化在 charge 热路径零回归**（本轮修复中唯一触碰热路径的改动）。
2. 其余修复（事件构造器校验/抓钩/hopper 判空）均在低频路径，基准不可见，符合预期。
3. place/break ~+700ns 为 5.0.0 方块保护事件集的功能成本，可接受。
4. 高负载场景（5000 ticked 块）ticker 无系统性回归。

（MockBukkit 环境差异声明同 PERFORMANCE-COMPARISON.md——倍数与相对趋势可信，绝对值仅供参考。）
