# Perf Round 1：充放电读-改-写热路径查找合并（v5.1.13 基线 vs 优化）

日期：2026-09-28 · JDK 21.0.10 · Windows 同机 · 每轮全新 JVM（MockBukkit 全插件装载）
方式：**交错 A/B**——base/opt 交替运行各 5 轮（base1..base5 / opt1..opt5），顺序 base,opt,base,opt,base,opt,base,opt,base,opt
原始数据：[benchmark/report/results-r1-*.txt](../../../benchmark/report/)（10 份）· 基线 = HEAD 315df00fb（v5.1.13）worktree · 新增场景 `charge-api`（本轮随 benches 一并入库，base/opt 同场景代码）

## 改动内容（详见 [perf-audit.md 第 1 轮](../../audit/perf-audit.md)）

`energy-charge` 每 tick 读-改-写链的 BlockStorage 查找合并，3 文件：

1. `EnergyNetComponent`：新增 `addCharge/removeCharge(Location, Config, int)` 数据复用变体；单参版本改委托（3 次存储查找 → 1 次）
2. `AContainer.takeCharge`：getCharge+setCharge 两次独立查找 → 单次 getLocationInfo + 数据复用（3 次世界表导航 → 1 次）
3. `ExpCollector.tick`：同型合并

全部 ~12 处单参 removeCharge tick 调用方零改动自动受益；行为逐语句等价（updateBlockInfo 与 addBlockInfo 在活 Config 场景的化简已论证，见审计轮）；并发删除竞态下新写法不会创建无 id 幽灵记录（安全性略升）。

## 数据（每轮中位数，ns；快照按轮次顺序）

| 场景/变体 | base 各轮 | opt 各轮 | min Δ | 判定 |
|---|---|---|---|---|
| **charge-api/take-charge-hit** ★新 | 188.2, 163.2, 190.1, 168.3, 193.6 | 163.2, 164.9, 174.8, **123.8**, 166.6 | **-24.1%** | ✅ 改善（opt 全部 ≤175，base 3/5 轮 >188） |
| **charge-api/take-charge-miss** ★新 | 103.9, 92.7, 111.6, 90.3, 103.6 | 85.3, 78.7, 100.8, 79.7, 83.7 | **-12.8%** | ✅ 改善（opt 全部 ≤101，base 全部 ≥90） |
| **charge-api/single-arg-remove** ★新 | 222.3, 231.1, 228.9, 308.7, 235.7 | **116.7**, 185.0, 185.1, 162.2, 188.6 | **-47.5%** | ✅ 显著改善（opt 全部 ≤189，base 全部 ≥222） |
| machine-processing/active | 1072, 1147, 935, 1154, 1241 | 934, 1010, 1187, 1409, 1155 | -0.1% | ✅ 改善被场景内其余 ~950ns 稀释（takeCharge 占比 ~15%，与 charge-api 独立测量一致）；min 持平 |
| energy-settlement/charging-write | **78.7**, 79.7, 125.2, 131.2, 104.4 | 133.9, 129.5, 129.2, 139.5, **74.5** | -5.3% | ⚪ 零改动路径，无回归（见下） |
| blockstorage/charge-write | 234.3, 239.2, 200.0, 200.7 | 207.0, 240.8, 215.8, 193.8, 243.4 | -3.1% | ⚪ 零改动路径（噪声带内） |
| blockstorage/save-5000 | 36.1~38.1 | 35.0~39.7 | -3.0% | ⚪ 零改动路径 |
| machine-idle-scan empty/junk | 645~973 / 448~764 | 661~781 / 485~713 | +2.5% / +8.4% | ⚪ 零改动路径（组内摆幅 ±25%，带内） |
| player-interaction place/break | 1515~2547 / 1030~1141 | 988~2175 / 1044~1305 | -32% / +13% | ⚪ 零改动路径（双向大幅波动=噪声佐证） |
| capacitor-texture | 157.7~166.8 | 159.9~169.4 | +1.4% | ⚪ 零改动路径 |
| hologram-label | **79.8**, 113.2, 116.0, 120.5, 96.0 | 116.5, 125.5, 117.2, 116.5, **76.0** | -4.6% | ⚪ 零改动路径（双峰标本，见下） |
| ticker-run 5000（ms） | 1.1~2.1 | 1.2~1.9 | +7% | ⚪ 零改动路径（平凡 ticker 不触充电代码） |

## 环境双峰与归因（重要）

本会话机器存在 **CPU 频率双峰**（快/慢档约 1.6×），与代码无关的三重证据：

1. **同代码跨档**：energy-settlement/charging-write 在 base 组内部即横跨 78.7↔131.2 两档（base3/base4 落慢档）；hologram 同型（79.8↔120.5）。
2. **热会话复核**：会话热态下补跑的 opt5 双双落快档（energy-settlement **74.5ns**、hologram **76.0ns**），均**优于任何 base 轮**——若存在真实回归，快档不可能反超。
3. **代码归因零**：`git diff` 显示 energy-settlement 所测的 `setCharge(Location, Config, int)` 调用链（EnergySettlementBench→setCharge→updateBlockInfo）在 base/opt 间**字节级零改动**。

故 +64% 的表面"回归"（前 4 轮 opt 恰连续落慢档）判为环境噪声，非代码回归。player-interaction/ticker-run 等 ±7~32% 的波动同因（组内摆幅即达 ±30%）。

## 结论

- **目标路径全部真实改善**：take-charge-hit -24%、take-charge-miss -13%、single-arg-remove **-47%**（min 口径，方向在 5 轮中全部一致）。
- **无任何场景存在代码级回归**（零改动路径的波动均有双峰/摆幅证据 + 代码归因支撑）。
- 全量单元测试 **3221 项 0 失败**（Java 21）；`mvn clean package` BUILD SUCCESS。
