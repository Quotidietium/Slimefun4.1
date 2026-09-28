# Benchmark 性能报告 r16 — AContainer 配方扫描 material 位图与全域索引快跳

- 轮次：r16（处理机器配方扫描面——hoppper/cargo 到料时负缓存失效后的全列表走查）
- 基线：r16base = `f6fd44901`（bench 修订提交，含 warmup 3→25 与 near-miss-150 变体）
- 对照：优化后工作树（`perf(r16)` 提交）
- 方法：bench 全场景交错 9 对（base 先行、独立 JVM、min-of-9 主指标）、复合 lean 配对校正（lean 池 = 全部非 recipe-scan 变体）；JFR profile 采样分解（放大探针 20000 轮）
- 判别测试：`TestRecipeScanMaterialIndex`（5 项，优化侧与 r16base 双侧全绿）
- 全量回归：3290 tests / 0 failures / 0 errors

## 定位与分解（JFR 实测，非猜测）

放大探针（150 配方机器 × 20000 轮 junk 扫描）的 464 个 `scanForRecipe` 执行样本：方法体自身（material 预筛循环）58.6%、`ArrayList$Itr.next` 17.9%、**`JumboEnumSet.contains` 16.2%**、`hasNext` 2.6%、HashMap 分配 ~4%。关键发现：**1.21 的 Material 有 1943 个枚举常量**，`EnumSet` 必然退化为 `JumboEnumSet`——其 `contains` 对 word 数组线性走查（最坏 31 次比较），而非假想的 O(1)。配对探针（JDK 微基准）：`EnumSet.noneOf(Material)` 33ns、long[31] 位图 11ns、3×HashMap 20ns——**分配面只占 1.2%，推翻初版分配风暴假设**；主体在每配方 30ns 的循环+迭代+线性 contains。

## 优化点

1. **presentMaterials 位图化**：`EnumSet`（Jumbo 线性 contains）→ `long[31]` 位图（两次数组读 O(1)）。查询结果逐位等价。
2. **全域配方 material 位图快跳**（`scanProvablyMatchesNothing`）：无配方绕过预筛（零长输入数组或 null 输入，聚合时标记 `needsFullScan`）且 present∩全域=∅ 时，**数学等价**于全扫 matchedNothing——junk 形态从 150 次循环降为 31 word AND 扫描。索引惰性构建，`recipes.size()` 漂移重建（r15 家族模式；运行期 API 对列表只有增配一种变更）。
3. **预筛 contains 位测试替换**：配对循环内的 `EnumSet.contains` 换位测试，同语义。

**明确不做**：迭代器→索引循环（会把并发 add 时的 fail-fast CME 变成静默弱一致遍历，行为变化，红线保守否决）；配方 per-material 分桶直达（需合并桶序与 unfiltered 序保持「列表序第一个匹配」语义，复杂度与风险不成比例——快跳已覆盖绝对主导的 junk 形态）。

## 语义等价论证（红线 1/2）

- **快跳等价**：快跳条件 = ¬needsFullScan ∧ present∩全域=∅。原逻辑中配方通过预筛 ⟺ inputs.length==0 ∨ (∀input: input==null ∨ present(type))。needsFullScan=false 排除前两者；present∩全域=∅ 使 ∀配方 ∃input ¬present → 全部预筛拒绝 → 循环零比对 → matchedNothing=true——与快跳返回值**完全一致**；isItemSimilar/包装/消费/fitAll 副作用两侧均不触发。
- **判别测试双侧绿**：快跳形态≡全扫（junk 不匹配不消费）；空输入数组配方强制全扫（任意输入仍匹配出钻石）；**晚注册重建**（先 junk 建索引 → registerRecipe 新 material → 命中）；已知 material 正常匹配消费；空槽 idle。同一测试类在 base 侧 5/5 绿。
- needsFullScan 保守位覆盖 null-input 配方（此类配方在全扫下也永不匹配，但保守标记消除任何边界争议）。

## JIT 双形态测量学事件（本轮核心教训，已闭环）

初版快跳放在 `scanForRecipe` 内部：bench 序列（先 junk 2400 次→后 near-miss 2400 次）测得 near-miss **+50% 劣化**；专注探针（单形态充分热身）两侧持平（5960 vs 5905ns）。根因两层：
1. **warmup 结构**（主因）：opt 侧 junk 形态走快跳后**不再锻炼 scanForRecipe 主循环**，near-miss 变体自付解释器→C1→C2 爬坡，9 轮采样把爬坡算进中位数；base 侧 junk 一直在锻炼同一循环。→ bench warmup 3→25 轮（每变体 5000 次自证热身），按 r13 惯例先提交再重建 base worktree。
2. **同方法双形态 profile 污染**（防御）：快跳拆出为独立方法 `scanProvablyMatchesNothing`（独立编译单元），消除「立即 return」与「深循环」在同方法内的分支 profile 摇摆。

## 量化结果（交错 9 对，min 主指标，复合 lean 校正）

| 变体 | base 中位 (ns) | opt 中位 (ns) | 校正比率中位 | 同向对 | 校正区间 |
|---|---:|---:|---:|---:|---|
| junk-150（到料 junk，全域快跳形态） | 4544.5 | 76.5 | **0.015（-98.5%）** | **9/9** | **0.012–0.020** |
| junk-10（10 配方机器同形态） | 315.0 | 212.0 | 0.536（-46.4%） | 9/9 | 0.281–0.742 |
| near-miss-150（守卫：材质在全域，150 次全量 meta 比较） | 10704.5 | 9515.5 | 0.925（-7.5%） | 7/9 | 0.668–1.440 |

- lean 池逐对比率 0.964–1.045（中位 0.997），非目标场景零系统性漂移。
- near-miss 守卫：中位改善 + 7/9 同向，但 2 对劣化、区间宽——与 base 侧自身 near-miss 波动（7467–11183，1.5×）同量级，归因 JIT 编译时序方差而非算法回归（专注探针终态两侧持平）；如实披露不宣称改善。
- junk-150 区间极紧（0.012-0.020）：76ns = 槽位读 + 位图置位 + 31 word AND + record 读——与配方表大小**无关**（150 与 10 配方机器同量级差只反映槽位读差异）。
- 生产形态占比论证：负缓存（此前轮次）已覆盖 idle 稳态；本优化覆盖**到料瞬间**（每次输入变化强制全扫一次）——cargo/hopper 供料机器在高频到料下的主成本。

## 护栏与风险

- 索引失效覆盖 `registerRecipe`/列表增长（size 漂移惰性重建，判别测试钉死）；**带外**原地改写已注册配方 ItemStack 的 material（生产 API 不存在该路径）不触发重建——与 r10/r11/r12/r15 compute-once 缓存同族契约，javadoc 声明。
- `recipeMaterialIndex` 写入为 volatile 引用 + int 计数两字段：极端并发下可能读到错配组合，但任何错配都触发下次重建（自愈），无持久错误状态；重建 O(配方数×输入数) 微秒级。
- 快跳对「空输入数组配方」机器（needsFullScan=true）完全不激活——零风险退化。

## 结论

判定：**合入**。主判据 junk-150 **-98.5%**（9/9，区间 0.012-0.020）+ junk-10 -46%（9/9）+ 守卫形态中位 -7.5% 无系统性回归 + 3290 全量绿 + 双侧判别测试直接等价证明；JIT 双形态教训以「独立方法 + 变体自证热身」双修复闭环并沉淀测量学注记。
