# SlimeFun4.1 性能优化循环追踪

延续 [5.1-audit.md](5.1-audit.md)（稳定性审计循环，137 轮收官）之后启动的**性能优化专项循环**。目标与红线：

> 在**不影响安全性、稳定性、兼容性**（红线 1）与**不丢失任何功能**（红线 2）的前提下持续优化性能。允许重写内部实现，但公开 API 语义、磁盘数据格式、既有行为必须保持。

## 方法论

- **每轮一个方向**：每轮从不同的优化面出发（充放电热路径 / 分配消除 / 持久化写路径 / ……），该方向的全部优化点必须做完，不允许半途换向。
- **量化闭环**：每轮改动用 `benchmark/`（独立 Maven 工程 + MockBukkit 全插件装载）做**同会话交错 A/B**（base/opt 各 ≥3 轮交替运行，同 JDK 21 同机），报告归档 [note/report/perf/](../report/perf/)。
- **归属优先**：对"未改动路径出现大幅波动"的场景，先做代码归因（git diff 证明路径字节级一致 → 波动判为环境噪声），不把噪声记作战果或回归。
- **环境噪声双峰**：本机（Windows/笔电级环境）存在 ~1.6x 的 CPU 频率双峰（快档 ~79ns / 慢档 ~125ns 量级，跨 base/opt 对称出现），故交错多轮 + min 统计 + 代码归因三件套为标准判据。
- **验证门槛**：每轮全量单元测试（3221 项基线）必须 0 失败；`mvn clean package` BUILD SUCCESS；触及行为的改动须补判别测试。
- **环境清理**：benchmark 运行产物（`benchmark/data-storage/`、`benchmark/plugins/`）与临时 worktree 用后即清（gitignored 但磁盘清理）；一次性基线 worktree 在循环收尾时 `git worktree remove`。

## 第 1 轮（2026-09-28）：充放电读-改-写热路径查找合并

**方向**：全插件最高频的每 tick 写路径——每台活跃机器、每个电网组件、每 tick 都要走一遍的 `energy-charge` 读-改-写链。此前每次调用要重复导航 `BlockStorage` 的世界表（String 键 CHM）与方块存储表（Location 键 CHM）多次。

**改动（3 文件）**：

1. `EnergyNetComponent`（core/attributes）：新增 `addCharge(Location, Config, int)` / `removeCharge(Location, Config, int)` 数据复用变体（镜像既有 `setCharge(Location, Config, int)` 模式）；单参版本改为委托（原来 checkID + getCharge + addBlockInfo 三次独立查找 → 一次 getLocationInfo + 一次 updateBlockInfo 快路径写入）。幽灵记录防线语义保留（`data.getString("id") == null` 早退，等价原 `checkID(l) == null`）；并发删除方块的竞态下新写法只会写向已脱离存储的 Config，不再可能像旧路径那样在窗口内新建无 id 记录（安全性略升）。全部 ~12 处单参 removeCharge tick 调用方（加速器/GEOMiner/FluidPump/AutoCrafter/GPSTransmitter 等）自动受益，零调用方改动。
2. `AContainer.takeCharge`（每台加工机器每 tick 的耗电闸门）：`getCharge(l)` + `setCharge(l, …)` 两次独立查找（共 3 次世界表导航 + 2 次字符串解析）→ 单次 `getLocationInfo` + 数据复用变体（1 次查找 + 1 次解析）。
3. `ExpCollector.tick`：同型合并（getCharge + removeCharge 共用一次 getLocationInfo）。

**行为等价性论证**：`addBlockInfo(l, key, value, false)` 在"已存在活 Config"时可精确化简为 `cfg.setValue + dirtyBlocks.add`（setBlockInfo 内部 previous==cfg → 跳过 put、id 不变 → 跳过 preset 分支），与 `updateBlockInfo(l, data, key, value)` 逐语句等价；`data == emptyBlockData` 时 id 守卫已先行返回，不触达两条路径的差异面。

**量化（benchmark/，base/opt 交错 5+5 轮，JDK 21，新增 charge-api 场景隔离本路径；完整数据与方法论见 [benchmark-perf-r1-charge-path.md](../report/perf/benchmark-perf-r1-charge-path.md)）**：

| 场景/变体 | base min（各轮中位） | opt min（各轮中位） | min Δ | 判定 |
|---|---|---|---|---|
| charge-api/take-charge-hit | 163.2（163~194） | **123.8**（124~175） | **-24.1%** | 改善（opt 全部 ≤175，base 3/5 轮 >188） |
| charge-api/take-charge-miss | 90.3（90~112） | **78.7**（79~101） | **-12.8%** | 改善 |
| charge-api/single-arg-remove | 222.3（222~309） | **116.7**（117~189） | **-47.5%** | 显著改善（opt 全部 ≤189） |
| machine-processing/active | 935 | 934 | -0.1% | 改善被场景内 ~950ns 其它工作稀释，min 持平、中位向好 |
| energy-settlement/charging-write | 78.7 | **74.5**（opt5 热会话轮） | -5.3% | **非回归**：路径 git diff 字节级零改动；base3/base4 自身落慢档（125/131ns），opt5 快档反超所有 base 轮 → 双峰环境噪声排除 |
| 其余零改动场景（charge-write/save-5000/idle-scan/place/break/capacitor/hologram/ticker-run） | — | — | -32%~+13% | 均为噪声带内波动（组内摆幅 ±25~30%，代码归因零改动） |

**回归**：全量 **3221 项测试通过、0 失败、7 跳过**（BUILD SUCCESS，Java 21）；`mvn clean package` 产物正常。

**环境注记**：①本会话环境噪声显著宽于 r134 会话（CPU 频率双峰），绝对值不可跨会话比较；②中途一次 TaskStop 杀 shell 未及杀子 JVM，导致一轮 base 结果文件被双进程交错写入（缺 2 行 charge-write RESULT），已确认无残留进程并作废该轮该场景数据。

## 环境清单（滚动）

- 基线 worktree：`../sf-perf-baseline`（HEAD=315df00fb，即 v5.1.13）——多轮复用，循环收尾时移除。
