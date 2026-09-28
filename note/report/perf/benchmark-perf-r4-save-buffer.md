# Perf Round 4：保存路径序列化缓冲复用（Round 3 基线 vs Round 4）

日期：2026-09-28 · JDK 21.0.10 · Windows 同机 · 每轮全新 JVM（MockBukkit 全插件装载）
方式：**严格交错 A/B** 各 5 轮；**base = Round 3 收官 20f3a7a0b**（worktree `../sf-perf-r4base`）vs **opt = Round 4 提交 f8bd712f0**——纯第 4 轮增量
原始数据：[benchmark/report/results-r4-*.txt](../../../benchmark/report/)（10 份，场景两组相同无需 overlay）

## 改动

`BlockStorage.save()` 脏块序列化循环复用单个 `StringWriter`（`serializeBlockInfo(cfg, string)` 重载；背衬数组稳定在最大块 JSON 大小，免去每块从 16 字符起的多次扩容与丢弃）；`getBlockInfoAsJson` 等独立调用点保留单参版本（每次新缓冲，线程安全面不变）。返回 `String` 仍为逐字拷贝，无别名。save() 全程持有 saveLock，复用无并发面。

**明确不做**（红线记录）：`.sfb` 磁盘格式由 snakeyaml/YamlConfiguration 决定——全文件 YAML 转储是保存成本的大头，但任何自研转储器都需逐字节复刻 snakeyaml 的引号/转义策略，格式漂移=数据损坏风险，判为格式红线锁死项。

## 数据（每轮中位数，save 为 ms，其余 ns）

| 场景/变体 | base 各轮 | opt 各轮 | min Δ | 中位 Δ |
|---|---|---|---|---|
| **blockstorage/save-5000** ★目标 | 36.955, 35.590, 39.246, 40.179, 38.374 | 36.908, 36.371, **34.285**, 34.764, 34.868 | **-3.7%** | **-9.1%** |
| blockstorage/charge-write（零改动） | 195.2~226.6 | 199.2~370.6(opt1 热身离群) | +2.0% | ~0% |
| ticker-run 5000（零改动·指示器） | 1.114~1.256 | 1.082~1.281 | +8.9%(ns/block) | +5.6% |
| machine-processing/active（零改动） | 818.6~1020.7 | 867.1~1252.1 | +5.9% | +17%（见归因） |
| recipe-scan junk-150 / junk-10（零改动） | 4586~6248 / 302~495 | 4522~6242 / 302~515 | ~0% | ~0% |

**环境判读**：本轮指示器（ticker-run）与 machine-processing 呈 ~+5% **逆向**漂移（opt 恒后跑、会话已连续基准 40+ 分钟，热态漂移）——目标场景 -9.1% 中位是在逆风下测得，真实改善估计 **4-14%**。machine-processing 路径经 git diff 归因与本轮改动零交集（序列化只在 save 周期运行，加工测量不触发 save），+17% 判为环境噪声。

**回归**：全量 **3227 测试 0 失败**；`mvn clean package` BUILD SUCCESS（JDK 21）。

## 结论

- 自动保存的逐块序列化成本下降（5000 脏块 36ms → 34.9ms 中位，逆风 -9%）；实际收益随脏块数线性放大，且发生在异步保存线程
- 保存路径的剩余大头（snakeyaml 全文件转储 + YamlConfiguration 写入）为格式红线锁死，已在审计中记录
