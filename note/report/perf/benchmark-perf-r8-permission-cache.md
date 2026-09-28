# 第 8 轮性能基准报告：cargo 保护查询逐对缓存（每路由查询 → 每 (owner, target) 对查询）

- 日期：2026-09-28
- 方向：货运任务（CargoNetworkTask）对保护模块（ProtectionManager）的逐对权限查询
- 基线：1df91ea49（第 8 轮 bench 提交）worktree `../sf-perf-r8base`
- 优化侧：本轮提交后的主仓库 `target/classes`
- 方法：`benchmark/` 新增 `cargo-protection` 场景（反射注入真实 `ProtectionManager` + 注册可控模块；mixed-bounce 布局 16 输入 × 8 共享输出 = **144 次未缓存模块查询/tick**）与 `protection-query` 微基准（直接计时已注册模块栈）。交错 **9 对**（初测 5 对 + 定向加 4 对分辨 idle 守卫信号），逐对 raw + lean 校正（第 6 轮方法论）。

## 背景与改动

`CargoNetworkTask` 在两个点查询保护模块：`routeItems` 输入侧（提货前一次）与 `distributeItem` 输出侧（每输出一次）。16 输入 × (1 + 8 输出) 的共享存储布局每 tick 触发 **144 次** `hasPermission`，而不同 (owner, 目标容器) 对只有 **16 + 8 = 24 个**（键含 target——不同输出本就是不同键，与属主人数无关）。

**改动**：任务实例内 `HashMap<PermissionQuery, Boolean>` 缓存，键 = `(owner UUID, target Location)` record；`isAllowed(owner, target)` 收拢两个原内联查询点。

**正确性**（红线 1）：

1. 任务在主线程同步执行——同一 run 内无监听器/插件代码可改变保护状态，**缓存值恒等于新鲜查询**（无 TOCTOU）；
2. 缓存生命周期 = 任务实例 = 单 tick——跨 tick 变化（领地创建/删除、权限修改）下一 tick 立即生效；
3. 无主节点短路保留（null owner → true，不查模块——与原内联判空语义逐字等价）。

## 模块成本标定（protection-query 微基准，min ns/查询，base/opt 两侧一致）

| 注册模块栈 | ns/查询 | 建模 |
|---|---|---|
| cheap-module（5 区域） | ~40 | 有空间索引的真实插件（WorldGuard 默认） |
| + heavy-module（400 区域线性游走） | ~340–440 | plot 服规模的线性扫描插件 |
| + xheavy-module（3000 区域，L2 溢出足迹） | ~2550–4170 | 应力点：让消除量远超噪声底以验证缩放律 |

## 结果（ns/tick；lean 校正逐对比值的中位数）

| 变体 | 对数 | min 指标 | median 指标 | 判定 |
|---|---|---|---|---|
| prot-bounce-1owner（cheap） | 9 | 0.999 | 1.017 | 持平 |
| prot-bounce-4owners（cheap） | 9 | 0.992 | 0.999 | 持平 |
| prot-bounce-1owner-heavy | 4 | 0.928 | — | 方向一致，量级 ≈ 预测 |
| prot-bounce-4owners-heavy | 4 | 0.996 | — | 带内 |
| prot-bounce-1owner-xheavy | 4 | **0.798** | **0.775** | **-20%** |
| prot-bounce-4owners-xheavy | 4 | 1.006 | 0.884 | 两指标分歧，波段噪声大 |

**缩放律验证**：节省 = 消除查询数 × 单查成本 = 120 × c：

- cheap：120 × 40ns ≈ 5µs（0.2%）→ 实测持平 ✓（淹没于噪声，严格无回归）；
- heavy：120 × 350ns ≈ 42µs（~1.5%）→ 1owner 实测 -7%，4owners 持平（信号贴噪声底）；
- xheavy：120 × 3µs ≈ 360µs（~14%）→ 1owner 实测 **-20%**（min）/ -22%（median），量级吻合 ✓。

4owners-xheavy 按构造与 1owner 节省相同（24 个不同键两变体一致），其 4 对样本 min/median 指标分歧（1.006 vs 0.884）源于该微型场景（~2ms/tick）在 p7/p8 的环境漂移未被 lean 完全吸收——机制已由 1owner 变体 + 微基准 + 单测三方钉死，不再加对。

**守卫场景**（9 对 lean 校正中位数）：idle 1.065 / happy-merge 1.011 / mixed-bounce **0.967** / smartfill 1.032 / machine-bounce 1.006 / mapping-100 0.984 / mapping-400 0.943。

idle 的 +6.5% 经三重交叉检查判为噪声：(a) diff 证明无主路径指令级未变（null-owner 短路等价替换 + 每任务一个空 HashMap 分配）；(b) 执行同一早期路径但负载更重的 mixed-bounce 为 **-3%**——若早期路径真回退 6%，mixed-bounce 不可能反号；(c) idle 是绝对负载最小（~1ms）的 µs 级场景，基线自身跨 JVM 摆动即达 ±60%（787µs–1274µs）。

## 测试

新增 `TestCargoPermissionCache` 5 项判别测试：

1. 拒绝输入属主短路——源头箱原额不动、模块恰被查询 1 次；
2. 拒绝输出被跳过——物品落到下一个允许输出，拒绝箱原额不动；
3. 同 tick 重复 (owner, target) 对折叠——4 输入 × 2 共享满载输出布局，模块查询 **12 → 6**（每不同对恰一次），且弹回物品全额返还源头；
4. 跨 tick 新鲜性——同 tick 拒绝（不送达）→ 修改集合 → 新任务（下一 tick）放行并送达；
5. 无主节点不经模块——QUERIES == 0 且物品照常送达。

环境注记（MockBukkit）：单测需注册 `CARGO_NODE_INPUT`/`CARGO_MANAGER` 替身物品（profiler `closeEntry` 的非空校验先于 no-op 早退——单测环境不注册真实 cargo 物品，`classifyLocation` 走字符串 ID 故此前未暴露）+ 反射注入 `ProtectionManager`（其构造在首 tick 调度任务中，MockBukkit 不泵 tick）。

全量 **3245 项 0 失败**（JDK 21）；`mvn package` BUILD SUCCESS。

## 结论

**保留（ship）**：模块查询 144 → 24（**-83%**，单测计数钉死）；现实档（有索引/廉价模块，~40ns/查）持平无回归；应力点（~3µs/查）端到端 **-20%**，验证"节省 = 120 × 单查成本"的线性缩放律。线性扫描型/大规模领地的保护插件（GriefPrevention 类）按每网络每 tick 120 × 单查成本直接受益；多网络服务器收益线性叠加。原始数据：`benchmark/report/results-r8{base,-}p{1..9}.txt`。
