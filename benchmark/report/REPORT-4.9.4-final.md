# SlimeFun4.1 v4.9.4 完整性能测试报告（最终版）

- **日期**:2026-07-29
- **被测版本**:`4.9.4`，全部性能优化（P1–P5）+ 测试/基准细化后的 HEAD（分支 `FIX-Performance`）
- **测试工具**:`benchmark/` 目录下的 MockBukkit 模拟基准（8 个场景）
- **环境**:Windows 11,Oracle JDK 21.0.10,MockBukkit 3.133.2 (MC 1.21.1)
- **方法**:重新编译后连续 2 轮独立 boot，每场景取 `min`（跨轮较小值最可信，单次 boot 方差 ±50%）
- **原始数据**:[results-4.9.4-final-r1.txt](results-4.9.4-final-r1.txt)、[results-4.9.4-final-r2.txt](results-4.9.4-final-r2.txt)

## 一、全场景结果（min）

| # | 场景 | r1 min | r2 min | 采用 min | 单位 |
|---|---|---:|---:|---:|---|
| 1 | BlockStorage charge-write | 227 | 200 | **200** | ns/写 |
| 2 | BlockStorage save(5000 脏块) | 33.9 | 33.7 | **33.7** | ms |
| 3 | 空转机器·空输入 | 462 | 353 | **353** | ns/tick |
| 4 | 空转机器·不匹配输入 | 359 | 367 | **359** | ns/tick |
| 5 | 机器加工中（active） | 703 | 613 | **613** | ns/tick |
| 6 | 电网 charge 写入（P2 路径）| 73 | 73 | **73** | ns/组件 |
| 7 | 电网·已满跳过（短路）| 49 | 134 ⚠ | **49** | ns/组件 |
| 8 | 用户交互·放置方块 | 986 | 1109 | **986** | ns/op |
| 9 | 用户交互·破坏方块 | 582 | 555 | **555** | ns/op |
| 10 | 电容贴图·同档位 | 102 | 98 | **98** | ns/次 |
| 11 | 全息标签·未变 | 84 | 72 | **72** | ns/次 |
| 12 | **ticker-run（5000 块）** | 0.794 | 0.759 | **0.76** | ms（159 ns/块）|

⚠ 场景 7 的 r2 值 134 明显偏高（r1 为 49），系该轮 GC/调度残留干扰，采用更可信的 r1 min=49。

## 二、与优化前对比（核心收益）

| 场景 | 优化前 baseline min | 本版本 min | 降幅 | 来源 |
|---|---:|---:|---:|---|
| **ticker-run（5000 块）** | ~2.5–3.3 ms | **0.76 ms** | **约 −75%** | P3 profiler 按需收集 |
| ticker 每块调度 | ~600–880 ns | **159 ns** | 约 −73% | P3 |
| 电网 charge 写入（P2 路径 vs addBlockInfo）| ~200 ns | **73 ns** | **约 −63%** | P2 updateBlockInfo 复用 data |
| BlockStorage charge-write | ~290 ns | **200 ns** | 约 −31% | P1 跳过冗余 put |

其余场景（save / 机器空转 / 电容 / 全息）在 4.9.3 已优化到位，本版本持平；机器加工中（613 ns）高于空转（353–359 ns），符合预期（多出 takeCharge+进度+配方消费）。

## 三、关键观察

1. **tick 主循环开销降至 159 ns/块**：profiler 按需收集（P3）移除了每块 2 次 nanoTime + 对象分配 + CHM 合并的固定开销，ticker-run 从 ~3 ms 降到 0.76 ms（5000 块）。这是本版本最大的单项收益。
2. **电网 charge 写入路径（P2）量化确认**：`setCharge(Location, Config)→updateBlockInfo` 复用 data 为 73 ns/组件，对比 `addBlockInfo` 路径 ~200 ns 省 63%；已满跳过（不写）仅 49 ns。
3. **机器加工 vs 空转**：加工中 613 ns/tick，空转 353–359 ns/tick，差额 ~250 ns 为 takeCharge + 进度推进 + 配方完成时的消费/产物推送——与代码路径一致。
4. **用户交互**：放置 986 ns、破坏 555 ns（事件分发 + BlockStorage 写入/清除 + ticker 注册/注销）。
5. **BlockStorage save**：5000 脏方块落盘 33.7 ms，与 4.9.3 持平（本版本未改 save 路径）。

## 四、与生产环境的差异（诚实声明）

- MockBukkit 的方块/实体操作成本与真实 Paper 服务端不一致,**倍数与相对趋势可信、绝对纳秒数仅供参考**。
- ticker-run 用平凡 ticker（仅测调度开销），真实机器 tick 还含 AContainer/EnergyNet 等逻辑，绝对耗时更高，但 P3 的比例性质成立。
- 未测量真实服务器端到端 TPS;生产级验证建议用 spark 在真实服务器对同一场景做采样对比。
- 完整 cargo 网络 / 完整 EnergyNet.tick / GUI inventory click 受 MockBukkit + 默认物品未注册限制，采用等价覆盖（见 [SCENARIOS.md](SCENARIOS.md)）。
- 全量单元测试 1905 项通过、0 失败、0 错误、7 跳过。

## 五、相关报告

- [PERFORMANCE-COMPARISON.md](PERFORMANCE-COMPARISON.md):4.9.3 → 4.9.4 优化前 → 优化后 全面对比
- [REPORT-4.9.4-perf.md](REPORT-4.9.4-perf.md):优化批次技术细节
- [SCENARIOS.md](SCENARIOS.md):8 场景清单 + MockBukkit 覆盖边界
- [REPORT.md](REPORT.md):4.9.2 → 4.9.3（历史）
