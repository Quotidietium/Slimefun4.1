# 第 7 轮性能基准报告：cargo 路由映射缓存（每 tick 重建 → 失效驱动重建）

- 日期：2026-09-29
- 方向：货运网络（CargoNet）的每 tick 路由映射构建
- 基线：10388e681（第 7 轮 bench 提交）worktree `../sf-perf-r7base`
- 优化侧：本轮提交后的主仓库 `target/classes`
- 方法：`benchmark/` 新增 `cargo-mapping` 场景（真实 `CargoNet`，同包桥反射填充节点集 + BlockStorage 频道数据，驱动 `mapInputNodes()`+`mapOutputNodes()`——与 `CargoNet#tick` 每 tick 所为一致；反射开销两侧恒定）。交错 5 对，逐对 raw + lean 校正（第 6 轮方法论）。

## 背景与改动

默认 `networks.cargo-ticker-delay: 0` 下，`CargoNet.tick` **每个游戏 tick**都从 BlockStorage 全量重建输入/输出路由映射（每节点一次 Config 读取 + 频道字符串解析），而映射仅在节点增删或节点配置变更时才真正变化。

**改动**：`cachedInputs`/`cachedOutputs` 缓存 + 失效驱动重建：

1. **失效点（三条，覆盖全部第一方变更路径）**：
   - `markCargoNodeConfigurationDirty`（节点配置变更——与 `ItemFilter` 缓存相同的既有契约；菜单关闭钩子走此路径）；
   - `onClassificationChange`（节点放置/破坏/类型变化改变节点集）；
   - `AbstractCargoNode#applyChannelChange`（频道选择器点击后立即失效——**保持"点击即下一 tick 生效"的原语义**，不等菜单关闭）。
2. **Copy-on-write 交接**：重建永远发布新 map 实例；已交接给主线程 `CargoNetworkTask` 的旧实例只读不改（第 6 轮 G 优化迭代的是缓存列表，同样无任务侧写入）。
3. **发布竞态闭合**：`routingGeneration` 代数计数器——重建期间主线程发生失效（BlockStorage 写 + 计数器自增）则重建结果作废，下一 tick 从新数据重建；过期路由永不发布。
4. 分组逻辑（频道 0-15 过滤、CORRUPTED_FREQUENCY_REPORTED 去重上报）逐字保留，仅执行时机从"每 tick"变为"失效后首次"。

**语义契约**（与 ItemFilter 缓存一致，审计记录）：绕过失效钩子的带外 BlockStorage 直写（addon 硬改 frequency 字段）在下次失效前不生效——上游 filter 缓存已是同一契约；第一方路径（菜单、事件）全部即时失效。

## 结果（ns/tick；交错 5 对）

| 变体 | base min | opt min | 逐对 raw 比值 | 判定 |
|---|---|---|---|---|
| mapping-100 | ~47µs | ~0.9µs | 0.014–0.020 | **-98%** |
| mapping-400 | ~93µs | ~0.1–0.9µs | 0.001–0.010 | **-99.9%** |

- 5 对全部一致，lean 校正后不变（信号远超噪声）。
- 残余成本 = 两次反射调用 + volatile 读（生产路径为直接方法调用 + null 检查，更低）。
- 重建成本转移至失效事件（节点增删/配置变更），量级 = 基线单次重建成本（一次性）。
- 非目标场景全部带内：cargo-route idle -2%、mixed-bounce +1.6%、recipe-scan junk-150 -3.7%、charge-write +0.9%（本轮未触及 BlockStorage/插入路径）。

## 测试

- 新增 `TestCargoRoutingCache` 5 项：缓存持有（带外写不渗入）；`markCargoNodeConfigurationDirty` 重建新数据；`applyChannelChange` 下一 tick 即生效（真实网络发现路径：regulator+双节点经 `network.tick()` 增量分类）；分类变更（节点移除）即刻出表；copy-on-write 交接（重建发布新实例）。每测试先归一化频率状态（JUnit 方法序任意，网络静态共享）。
- 全量 **3239 项 0 失败**（JDK 21）；`mvn package` BUILD SUCCESS（SlimeFun4.1-5.1.15.jar）。

## 结论

每 tick 每网络的 O(节点数) 映射重建成本被全额消除（大网络 93µs/tick → 亚微秒），失效路径完备（三点钩子 + 代数竞态闭合），第一方语义不变。原始数据：`benchmark/report/results-r7-{base,opt}{1..5}.txt`。
