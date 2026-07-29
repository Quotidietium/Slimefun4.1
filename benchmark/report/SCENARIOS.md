# SlimeFun4.1 基准场景清单与覆盖说明

- **日期**:2026-07-29
- **范围**:本轮测试与基准细化的完整场景清单、典型数据、以及 MockBukkit 环境下的覆盖边界

## 一、基准场景总览（8 个场景）

`benchmark/` 在原有 5 个场景基础上新增 3 个，覆盖机器加工、电网结算、用户交互：

| 场景 | 文件 | 测量目标 | 典型 min |
|---|---|---|---:|
| BlockStorage charge-write | BlockStorageWriteBench | 电网 charge 写入热路径（addBlockInfo） | ~200 ns/写 |
| BlockStorage save | BlockStorageWriteBench | 5000 脏方块落盘 | ~33 ms |
| 空转机器·空输入 | MachineIdleScanBench | 空转配方扫描（负缓存命中） | ~600 ns/tick |
| 空转机器·不匹配输入 | MachineIdleScanBench | 负缓存 + 快照比对 | ~460 ns/tick |
| **机器加工中** ★新 | MachineProcessingBench | takeCharge + 进度 + 配方消费 | ~810 ns/tick |
| **电网结算 charge 写入** ★新 | EnergySettlementBench | setCharge(L,Config)→updateBlockInfo（P2 路径） | ~124 ns/组件 |
| **电网结算·已满跳过** ★新 | EnergySettlementBench | getCharge 等于目标值的短路 | ~72 ns/组件 |
| 电容贴图·同档位 | CapacitorTextureBench | 四档去重后跳过 | ~165 ns/次 |
| 全息标签·未变 | HologramLabelBench | 标签未变直接返回 | ~75 ns/次 |
| ticker-run（5000 块） | TickerRunBench | tick 调度开销（profiler 关闭后） | ~1.0 ms |
| **用户交互·放置** ★新 | PlayerInteractionBench | BlockPlaceEvent→存储+ticker 注册 | ~1.8 μs/次 |
| **用户交互·破坏** ★新 | PlayerInteractionBench | BlockBreakEvent→清除+ticker 注销 | ~1.0 μs/次 |

★新 = 本轮新增。

### 关键对比

- **机器加工中 vs 空转**：加工中 ~810 ns/tick，高于空转的 ~460–600 ns/tick（多出 takeCharge + 进度推进 + 完成时的配方消费/产物推送），符合预期。
- **电网 charge 写入（P2 路径）vs addBlockInfo**：`setCharge(Location, Config)` 经 `updateBlockInfo` 复用 data，~124 ns/组件；对比 `addBlockInfo` 路径 ~200 ns/写，**P2 省约 40%**。已满跳过 ~72 ns（仅 getCharge 比较，不写盘）。
- **ticker-run**：profiler 按需收集后 ~1.0 ms（优化前 ~3 ms）。

## 二、新增单元测试（13 项）

为本轮性能优化补充的回归测试，全部通过：

| 测试类 | 项数 | 覆盖 |
|---|---:|---|
| TestSlimefunProfiler | 5 | profiler 按需收集（平时 newEntry=0 / requestSummary 启用 / endTick 实时更新 getTime / 空闲 endTick 不变状态） |
| TestBlockStoragePerf | 4 | updateBlockInfo（更新已有块 / 无数据回退 / 同引用 setBlockInfo 保持 id）+ getLocationInfo(L, storage) 重载 |
| TestEnergyNetComponent | 4 | setCharge(Location, Config) 重载（写值 / 与单参版一致 / clamp / 相等值 no-op） |

## 三、MockBukkit 环境下的覆盖边界（诚实声明）

部分"完整模拟"在 MockBukkit + bench 环境（默认 Slimefun 物品未注册）下受限，无法可靠构造，故采用等价的组件级/事件级覆盖：

1. **完整 cargo 网络 bench**：`CargoNetworkTask` 为 package-private、`getOwner` 为 private，且 `CargoNet.tick` 硬依赖 `SlimefunItems.CARGO_MANAGER.getItem()`（未注册时 `closeEntry` 的 `Validate.notNull` 抛 NPE）+ cargo 节点 item + 物理拓扑。构造完整 cargo 网络投入产出比低。P4（owner 单 tick 缓存）逻辑简单（HashMap 缓存、单 tick 生命周期、无并发问题），已由代码审查 + 发布说明覆盖。
2. **完整 EnergyNet.tick bench**：`EnergyNet.tick` 硬依赖 `SlimefunItems.ENERGY_REGULATOR.getItem()` + 网络拓扑发现。改用组件级 `EnergySettlementBench` 覆盖其最高频的 per-component charge 写入路径（P2 优化点）。
3. **GUI inventory click**：MockBukkit 的 inventory-click 模拟对 Slimefun 自定义菜单不可靠（现有测试均用直接 `setItem`）。改用方块放置/破坏事件（`PlayerInteractionBench`）覆盖用户交互。

## 四、测试与基准运行方式

- 单元测试：`mvn test`（MockBukkit + JUnit5）。全量 ~1905 项。
- 基准：`cd benchmark && ./run-benchmark.sh ../target/classes <label>`，结果写入 `report/results-<label>.txt`。
- 各场景独立 boot、`min` 为跨轮可信指标（见 [PERFORMANCE-COMPARISON.md](PERFORMANCE-COMPARISON.md) 的方法说明）。
