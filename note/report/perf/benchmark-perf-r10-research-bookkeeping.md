# 性能基准报告 · 第 10 轮：科研簿记路径——hasEnabledItems 计算一次缓存 + 玩家科研集合免拷贝计数

- **日期**：2026-09-29
- **A/B**：base = `ef3ccab2a`（r10 bench 提交，worktree `../sf-perf-r10base`）vs opt = 本轮（Research / SlimefunItem / PlayerProfile / PlaceholderAPIIntegration 四文件）。
- **方法**：新场景 `research-progress`（100 科研 × 5 已注册物品；变体 unlock-cycle=2000 次真实拥有权翻转、title=5000 次 getTitle、has-unlocked=200000 次交替门禁查询）。交错 **9 对**（base 先、每 run 独立 JVM），延续 r9 **复合 lean**（逐对取全部未触及场景原始比值的中位数作该对校正因子）。主指标 min-of-runs，次指标 median。

## 优化内容

1. **`Research.hasEnabledItems()` 计算一次缓存**（`volatile Boolean`，null=未算）。原实现每次调用都全量走 `items` LinkedList 找 ENABLED 状态物品——而该值出现在两条热路径的**最内层**：`countNonEmptyResearches` 对注册表**每个**科研调用一次（setResearched 触发 2 次、getTitle 触发 1 次），以及 `sendStats`。
2. **失效钩子全覆盖**：`SlimefunItem.setResearch` 在摘除/挂接两端各失效一次（`Research#addItems` 经由它）；`SlimefunItem.register()` 状态定稿后补一次失效——覆盖"先绑定后注册"的非规范顺序（第一方代码全部是先注册后绑定，此钩子是保险带）。
3. **PlayerProfile 内部计数免拷贝**：setResearched（×2 处）、getTitle、sendStats 改用 `data.getResearches()` 活集合计数，消除每次调用的 `ImmutableSet.copyOf` 整集合分配。公开 `getResearches()` 的防御拷贝契约**原样保留**（API 语义不变，仅内部不再绕经它）。
4. **PlaceholderAPI 集成活视图**：3 个占位符（research-xp-levels / total-unlocked / percentage）改读 `profile.getPlayerData().getResearches()`（既有公开 API），消除记分板轮询期的每次整集合拷贝。

## 正确性论证

- **ItemState 注册后不可变**：状态在 `register()` 内一次性赋值（UNREGISTERED→ENABLED/DISABLED/VANILLA_FALLBACK），此后不变——缓存值不存在"物品状态漂移"输入。
- **绑定唯一入口失效**：第一方全部绑定路径（ResearchSetup、ResearchBuilder、Talisman）经由 `setResearch`/`addItems`，每次都失效；`register()` 体不触碰 research 字段（绑定是注册后的外部动作），保险钩子另覆盖反向顺序。
- **带外契约**（与 ItemFilter/routing 缓存同族）：插件直接 `getAffectedItems()` 变异列表属带外操作，不触发失效——javadoc 已声明。
- **计数不变式**：判别测试 `TestResearchProgressCaching`（4 项）钉死"活集合计数 ≡ 公开防御拷贝计数"（ResearchProgressEvent 的 newCount/totalResearches 与拷贝侧手算逐字相等）、解绑/重绑/迟到注册三路失效、头衔阶梯与 PlayerResearchRankChangeEvent 边界触发。

## 量化结果（交错 9 对，复合 lean 校正）

本会话复合 lean 逐对 0.957–1.029（中位 ~0.99）——lean 池稳定，无 r9 式双峰。

| 指标 | base | opt | 校正中位比值 | 收益 |
|---|---|---|---|---|
| unlock-cycle min | 10 106 ns | 3 836 ns | 0.353 | **-62%** |
| unlock-cycle median | 15 296 ns | 4 400 ns | 0.303 | **-71%** |
| title min | 2 487 ns | 665 ns | 0.267 | **-73%** |
| title median | 3 693 ns | 738 ns | 0.277 | **-80%** |
| has-unlocked min | 0.8 ns | 0.6 ns | — | 平坦守卫（未动），亚 ns 量化噪声 |

- unlock-cycle 原始比值 9/9 全部 < 0.5；title 原始比值 9/9 全部 ≤ 0.50（8/9 在 0.15–0.36 带）。方向全一致。
- has-unlocked 逐对散布 0.12–13×（中位指标）——0.8ns 量级的纯contains门禁，量化抖动主导，判为噪声（该代码路径未动）。

**收益来源分解**（100 科研 × 5 物品尺度）：原 setResearched 每次解锁付 2×（注册表 100 次 LinkedList 全走 ~500 节点 + 玩家集合整拷贝）+ getTitle 同构 2 次；缓存后注册表侧 O(1)、玩家侧零拷贝。尺度越大（实际服务器 ~250 科研）收益越大。

## 守卫（未触及场景，复合 lean 校正中位）

cargo-route ×6 变体 0.93–1.11；cargo-protection ×6 0.95–1.10；protection-query min 档 1.02–1.07（median 档 1.14–1.21，该对亚微秒 median 指标历史抖动大，min 档带内）；ticker-resolution/run ~1.0；blockstorage 0.98–1.00；machine-processing 0.87（偏快侧噪声）；recipe-scan junk-150 0.97 / junk-10 1.13；charge-api 0.96–1.11；hologram/capacitor ~1.0。**无系统性回归**——r10 diff 不触及上述任何场景代码（代码归因排除），离群值双侧分布（0.73–1.41）符合会话噪声特征。

## 结论

**保留（ship）**：科研簿记路径 unlock-cycle **-62~71%**、getTitle **-73~80%**（9/9 对同向），has-unlocked 门禁持平。判别测试 4 项 + 全量 **3254 测试 0 失败**（3250 基线 + 4 新增）；`mvn package` BUILD SUCCESS。原始数据：`benchmark/report/results-r10{base,-}p{1..9}.txt`。
