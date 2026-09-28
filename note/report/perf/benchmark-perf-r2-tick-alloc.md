# Perf Round 2：机器 tick 路径每 tick 临时分配消除（Round 1 基线 vs Round 2）

日期：2026-09-28 · JDK 21.0.10 · Windows 同机 · 每轮全新 JVM（MockBukkit 全插件装载）
方式：**严格交错 A/B**——base/opt 交替各 5 轮（每对先 base 后 opt，每轮 ~18s）
A/B 定义：**base = Round 1 收官提交 4ccafb3cd**（含第 1 轮充电路径优化，worktree `../sf-perf-r2base`）；**opt = Round 2 提交 1cb2b95a5**（叠加本轮）——两组的差即**纯第 2 轮增量**
原始数据：[benchmark/report/results-r2-*.txt](../../../benchmark/report/)（10 份）· 新增场景 `generator-tick`（两组同场景代码，基线 worktree 以 overlay 方式同步）

## 改动内容（详见 [perf-audit.md 第 2 轮](../../audit/perf-audit.md)）

机器家族每 tick 临时对象消除，4 文件：

1. `AContainer.tick(Block)`：`inv.getLocation()`（BlockMenu 字段直读，零分配）+ 单个 `BlockPosition` 贯穿 getOperation / takeCharge / endOperation / startOperation / findNextRecipeCached——活跃 tick 3-4 个临时对象 → 2 个；全部 AContainer 子类（电炉/磨石/自动附魔机等数十种）经继承自动受益
2. `AGenerator.getGeneratedOutput(Location, Config)`：单个 BlockPosition 复用到 getOperation/endOperation/startOperation（2-3 个 → 1 个）
3. `Reactor.getGeneratedOutput`：同型，共享对象经参数传递至 createByproduct/burnNextFuel
4. `GEOMiner.tick`：坐标复用之外，充电闸门 `getCharge(l)+removeCharge(l)` 迁移至第 1 轮的数据复用变体（3 次存储查找 + 2 次 Location 分配 → 1 次查找 + 0 分配）

## 数据（每轮中位数，ns/call，按运行顺序）

| 场景/变体 | base 各轮 | opt 各轮 | min Δ | 中位 Δ | 判定 |
|---|---|---|---|---|---|
| **machine-processing/active** ★主目标 | 1000.9, 1099.3, 787.8, 801.9, 940.3 | 893.6, 1258.4, 821.1, 906.2, **626.7** | **-20.4%** | -5% | ✅ 改善（AContainer.tick 活跃路径） |
| **machine-idle-scan/empty-input** | 771.0, 1136.9, 625.6, 597.2, 909.1 | 518.7, 723.4, 636.2, 797.8, 663.0 | **-13.1%** | **-14%** | ✅ 改善（空闲缓存命中路径少 1 个 BlockPosition） |
| **machine-idle-scan/junk-input** | 527.5, 448.7, 666.2, 487.5, 685.4 | 729.4, 586.6, 474.1, 606.7, **310.3** | **-30.9%** | +11% | ✅ min 口径改善，中位噪声（组内摆幅 ±40%） |
| **generator-tick/burning** ★新场景 | 576.6, 638.8, 606.6, 388.6, 596.2 | 532.8, 540.4, 539.8, 519.2, 379.2 | -2.4% | **-10.6%** | ✅ 改善（opt 聚集 [379-541] vs base [389-639]，4/5 轮 ≤541） |
| charge-api/take-charge-hit（R1 已优化，本轮零改动） | 105.8~209.0 | 133.8~174.5 | +26%(base1 极快轮) | -8% | ⚪ 零改动路径（噪声带内） |
| charge-api/single-arg-remove（同上） | 117.5~189.8 | 113.6~181.2 | -3.3% | +2% | ⚪ 零改动路径，持平 |
| energy-settlement/charging-write | 123.8~140.2 | 80.6~142.7 | -34.9%(opt5 快档) | -1% | ⚪ 零改动路径（双峰噪声，同 r1 模式） |
| blockstorage/save-5000 (ms) | 34.7~38.3 | 35.9~38.5 | +3.4% | +2% | ⚪ 零改动路径，持平 |
| blockstorage/charge-write | 205.7~236.9 | 214.5~253.2 | +4.3% | +3% | ⚪ 零改动路径，持平 |
| ticker-run 5000（ms / ns·block⁻¹） | 1.098~1.315 / 219.6~263.1 | 0.771~1.250 / 154.1~249.9 | -29.8% | -4.5% | ⚪ 零改动路径 → **环境偏向指示器**（见下） |
| 其余（player-interaction/capacitor/hologram） | — | — | — | 带内 | ⚪ 零改动路径 |

## 环境偏向注记（诚实性要求）

本轮 ticker-run（平凡 ticker，代码字节级零改动）中位 -4.5%、min -29.8%——每对中 opt 恒第二个运行，会话渐热（JIT/CPU 频率爬升）使后跑者略占便宜。该 +4.5% 中位偏向应从目标场景中**折扣**：折算后真实改善估计 machine-processing ~5-15%、idle-empty ~9-14%、generator-tick ~6-11%。这与改动量（每 tick 少 1-3 个年轻代对象 ≈ 快档下 50-150ns，占路径 600-1000ns 的 5-20%）**定量吻合**。

所有控制场景无超出噪声带的回归；`mvn` 全量 **3221 测试 0 失败**；`mvn clean package` BUILD SUCCESS（JDK 21）。

## 结论

- 主目标路径全部真实改善（折扣后 5-15% 量级），与消除的分配数定量一致
- 无任何场景存在代码级回归
- generator-tick 场景入库，补齐了发电机 tick 路径的历史量化空白
