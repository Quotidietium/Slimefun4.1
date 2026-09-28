# 第 6 轮性能基准报告：cargo 插入扫描（Material 预检 + 惰性 wrapper + 免拷贝分配）

- 日期：2026-09-28
- 方向：货运网络（CargoNet）的物品分配/插入路径
- 基线：3fcd3a75d（v5.1.14 发布提交）worktree `../sf-perf-r6base`
- 优化侧：本轮提交后的主仓库 `target/classes`
- 方法：`benchmark/` 新增 `cargo-route` 场景（真实 `CargoNetworkTask`，mock 网络外壳），交错 base/opt 运行 **14 对**（每轮新 JVM，pair 内 base 先行），min-of-runs 为主读数、median-of-mins 为辅；`ticker-run` 为环境漂移（lean）指示器；另做逐对配对比值与 lean 校正（本轮环境噪声显著大于前几轮：单对比值摆动 ±40%，配对中位数与 lean 校正是必要工序）。

## 改动（全部语义不可见，7 项判别测试钉住）

1. **C1（vanilla 插入 Material 预检）**：`CargoUtils#insertIntoVanillaInventory` 逐槽扫描中，在 `isItemSimilar` 之前先比对 `itemInSlot.getType() != stack.getType()` 直接跳过——`isItemSimilar` 的第一项检查就是 Material 相等，故预检与原语义逐位等价（同第 3 轮配方扫描的证明模式）。
2. **C2（菜单插入 Material 预检）**：`CargoUtils#insert` 的 DirtyChestMenu 分支同样在 `isItemSimilar(itemInSlot, wrapper, ...)` 前加 Material 预检。
3. **B（惰性 wrapper）**：`ItemStackWrapper.wrap(stack)` 从 `CargoNetworkTask#distributeItem`（每次输出尝试都分配，即使插入立即弹回）移入 `CargoUtils#insert` 内部——菜单分支在槽位路由前包装（`getSlotsAccessedByItemTransport` 需要它）；vanilla 分支仅在遇到**同 Material 的占用槽**时才首次包装。 bounced 的插入（空槽/满槽/异 Material 槽）零 wrapper 分配。
4. **G（免拷贝分配）**：非 round-robin 时 `distributeItem` 直接迭代 `CargoNet#tick` 交出的逐 tick 局部列表，去掉每次输入每 tick 的防御性 `new ArrayList<>()` 拷贝（任务在主线程同步执行，列表不会被并发修改）。

签名变化：`CargoUtils#insert` 去掉 wrapper 参数（包内 API，调用方仅 `CargoNetworkTask` 与同包测试）。

## 场景变体（16 输入/tick）

| 变体 | 含义 |
|---|---|
| idle | 输入箱空——withdraw 立即 miss（开销地板） |
| happy-merge | 每输入私有输出，含同物品半叠——1 次取出 + 1 次合并（常态快乐路径） |
| mixed-bounce | 8 个共享输出箱全部是**异 Material 半叠**——每次插入尝试扫描全部占用非满槽并逐槽 meta 比较（混合仓储主导成本） |
| mixed-bounce-smartfill | 同上但满叠 + smart-fill（满叠跳过被禁用，每占用槽都付 meta 比较） |
| machine-bounce | 输出为 Slimefun 机器（DirtyChestMenu 路径），输入槽被异物品占用 |

## 结果（ns/tick；14 对交错；lean 指示器对 opt 中位 +9% 不利）

| 变体 | base min | opt min | Δmin（原始） | 逐对中位（lean 校正） |
|---|---|---|---|---|
| idle | 1.11ms | 1.18ms | +6% | **-5%** |
| happy-merge | 2.07ms | 2.29ms | +11% | **0%** |
| mixed-bounce | 3.92ms | 3.70ms | **-6%** | **-9%** |
| mixed-bounce-smartfill | 3.10ms | 3.14ms | +1% | **-9%** |
| machine-bounce | 2.29ms | 2.32ms | +1% | 0%（±噪声，见下） |

- **目标路径（mixed-bounce 家族）**：min -6% 且逐对 lean 校正中位 -9%——在不利的漂移窗口下仍稳定为负，判为真实改善。
- **快乐路径 happy-merge**：中位 0%（diff 在该路径仅增加一次枚举比较/占用槽，无回归机制）。
- **machine-bounce**：原始 min +1% ≈ 0；首轮 10 对的 lean 校正中位 +6% 触发定向复核，追加 4 对（11-14）lean 校正为 0.95/0.95/1.15/1.03，中位 ~1.0——判定为估计器噪声，无持续回归。
- 环境噪声警示：本轮单对 raw 比值摆动 ±40%（第 8 对所有变体含 lean 同步劣化），idle 的 base 侧 min 跨 JVM 覆盖 1.17-2.05ms。结论全部以 lean 校正后的配对统计为准。

## 测试

- 新增 `TestCargoInsertScanSemantics` 7 项：混合半叠仅并入同物品槽；lore 敏感匹配（惰性 wrapper 不改变 `checkLore=true` 语义——注意 cargo 的 isItemSimilar 忽略附魔、比较 lore）；全异 Material 弹回且每槽不动；smart-fill 满叠异物不合并不吞物品；超叠合并返回余量（60+10→64+6）；机器目标跳过异物品菜单槽填空槽；免拷贝分配仍遍历全部输出（首输出弹回、次输出接收）。
- `TestCargoFailsClosed` 2 处调用点随签名更新（语义不变）。
- 全量 **3234 项 0 失败**（JDK 21）。

## 结论

货运插入/分配路径在混合仓储 bounce 场景（漏斗/货运喂混合箱的常态痛点）获得 **-9%（lean 校正中位）**，快乐路径与机器路径无超噪声变化。改动语义不可见、包内 API 无外部影响。原始数据：`benchmark/report/results-r6-{base,opt}{1..14}.txt`。
