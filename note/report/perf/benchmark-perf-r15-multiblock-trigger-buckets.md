# Benchmark 性能报告 r15 — MultiBlock 右键触发材质分桶预筛

- 轮次：r15（多方块结构交互分发面——玩家右键任意方块都会走到的监听器热路径）
- 基线：r15base = `f6d05df89`（bench 提交点，含未优化的全注册表扫描）
- 对照：优化后工作树（`perf(r15)` 提交）
- 方法：bench 全场景交错 9 对（base 先行、独立 JVM、min-of-9 主指标）、复合 lean 配对校正（lean 池 = 全部非 multiblock-interact 变体）
- 判别测试：`TestMultiblockBucketPrefilter`（7 项，优化侧与 r15base 剥离 r15 专有断言后双侧全绿——桶路径 ≡ 旧全扫路径的直接等价证明）
- 全量回归：3285 tests / 0 failures / 0 errors

## 优化点

**`MultiBlockListener.onRightClick` 全注册表扫描 → 触发材质分桶预筛**。上游实现里，玩家对**任意方块**的每次右键（主手）都遍历**全部**已注册 `MultiBlock`，每台做一次 `getRelative(trigger)` + `compareMaterials`（中列 + 两侧列共最多 9+ 次 `getType()`、逐 tag 对称比较）。Slimefun 自身注册约 40 台多方块结构——玩家在机器农场右键箱子、按钮、门等无关方块时，这 40 台全部白扫。

优化：只有「点击格材质 = 该多方块被点格的期望材质（直接相等或共享受支持 tag）」的多方块才可能匹配。`MultiBlockMachine#postRegister` 注册钩子将每台多方块按 `MultiBlock#getClickMaterial()`（SELF→`blocks[4]`、UP→`blocks[7]`（点击格为中列底格）、DOWN→`blocks[1]`（顶格））索引进 `EnumMap<Material, List<MultiBlock>>` 桶，并按 `getSupportedTags()`（LOGS、WOODEN_TRAPDOORS、WOODEN_SLABS、WOODEN_FENCES、FIRE）做对称扩张——与监听器 `equals(Material, Material)` 的语义完全一致（两边都用 tag 双侧同属判定，无活塞特例）。触发格为 null 通配的多方块无法预筛，进入 `unbinnedMultiblocks` 尾扫（上游结构里仅装饰格可为 null，触发格非空的占绝对多数）。

选择语义逐字保留：旧实现收集全部匹配后取**注册表序最后一个**；桶把注册表序碎片化后，用 `IdentityHashMap<MultiBlock, Integer>` 记录注册序号、`findBetterMatch` 保留序号更大者——含桶内/unbinned 交错情形（判别测试钉死）。`compareMaterials` / `compareMaterialsVertical` / `equals(Material, Material)` 比较核心**零改动**。

**自愈护栏**：绕过钩子直接 `getMultiBlocks().add(...)` 的路径（测试、非常规附属）由尺寸漂移检测兜底——每次事件开头比对 `bucketedMultiblockCount != multiblocks.size()` 即整体重建（判别测试钉死：直接 add 后下一次右键即恢复匹配）。

## 语义等价论证（红线 1/2）

- **双侧等价证明**：`TestMultiblockBucketPrefilter` 在测试内复刻旧全扫循环为 oracle（同一比较逻辑、同一注册表序取尾），断言新路径的匹配结果与 oracle 逐例相同。同一测试类在优化侧 7/7 绿、在 r15base（剥离仅优化侧存在的 `isMultiblockBucketStale` 断言）7/7 绿——两侧行为一致即旧≡新的直接证明。
- 覆盖面：SELF/UP/DOWN 三种 trigger 的完整匹配（索引映射钉死）；tag 等价（OAK_LOG 结构响应 SPRUCE_LOG 点击）；null 通配触发格走 unbinned 仍匹配；多匹配时注册表序最后一个胜出（binned 与 unbinned 交错两种排布各钉一次）；直接 add 漂移自愈；无人使用的材质点击零事件。
- 索引映射推导（报告存档）：结构数组 9 格 = 3 列 × 3 行，`blocks[1]/[4]/[7]` 为中列上/中/下。SELF 点击中列中格（center=clicked）；UP 时 center=clicked.getRelative(UP) 即点击格为中列**底**格 `blocks[7]`；DOWN 时点击格为中列**顶**格 `blocks[1]`——与旧 `getRelative(trigger)` 逻辑代数等价。
- 无功能新增面：分桶是注册表派生只读索引，`getMultiBlocks()` 原列表照旧暴露；重建发生在注册钩子（启动期）或首次检测到漂移的事件（稀有），事件路径正常态零分配（候选列表为既有 ArrayList 只读遍历；空材质直接返回 `Collections.emptyList()`）。

## 量化结果（交错 9 对，min 主指标，复合 lean 校正）

| 变体 | base 中位 (ns) | opt 中位 (ns) | 校正比率中位 | 同向对 | 校正区间 |
|---|---:|---:|---:|---:|---|
| click-no-match（无人使用的材质，生产常态） | 3111.4 | 0.0 (min) / ~18 (median) | **~0.004（-99.6%）** | **9/9**（median） | median 比率 0.0033–0.0061 |
| click-near-miss（材质对、环境不对） | 3296.2 | 769.7 | **0.233（-76.7%）** | **9/9** | 0.171–0.362 |
| click-full-match（完整结构命中并分发事件） | 3691.8 | 1065.4 | **0.307（-69.3%）** | **9/9** | 0.246–0.445 |

- median 指标配对比率：no-match 0.0039（9/9）、near-miss 0.2631（9/9）、full-match 0.3114（9/9）——与 min 主指标同向同量级，结论对指标选取不敏感。
- lean 池逐对比率 0.9887–1.0187（中位 ≈1.002），全部其他 bench 场景无系统性漂移——本轮改动对非目标路径零回归。
- no-match 的 min=0 为 JIT 完全消除（桶查询返回共享空列表 + 无匹配即返回），median 仍保留 ~15-25ns 的稳定残留；-99.6% 以 median 口径陈述。
- bench 环境注册 40 台多方块（贴近真实插件规模）；真实服务器玩家右键绝大多数为 no-match 形态——这正是消除的目标形态。

## 测量学与守卫披露

- **维护成本口径**：每次注册 `rebuildMultiblockBuckets()` 为 O(N×T)（N=多方块数、T=受支持 tag 数 ≤5），40 台量级下微秒级、仅启动期发生；事件路径漂移检测为一次 int 比较。桶内存为 N 个条目的引用复用（`MultiBlock` 实例本身不复制）。
- **MOCK 注记**：MockBukkit 的 `Block.getType()` 便宜（无 chunk 触发），真实 Paper 上未命中材质点击仍会触发区块保活检查差异——mock 数字对「消除了多少次 getType」方向为**保守下界**（真纸每次 getRelative+getType 更贵，实际收益更大）。
- near-miss/full-match 的 opt 侧残留成本 = 桶内候选的 compareMaterials（材质命中但结构不匹配的少数几台）+ 命中路径的事件构造与分发（不变部分），符合分桶只消除「不可能匹配」扫描的设计预期。

## 护栏与风险

- 漂移自愈覆盖直接 `list.add`；直接 `list.remove`（生产不存在该路径）同样触发尺寸变化被重建捕获。对列表的**原地置换**（remove+add 同尺寸）理论上可逃逸检测——该操作在生产与附属 API 中均不存在（注册表只增不减），判别测试文档化此前提。
- `multiblockOrder` 为 `IdentityHashMap`：MultiBlock 未重写 equals/hashCode 语义参与其中（同实例同序号），与注册表顺序语义一致。
- 后注册（启动后加载的附属）经 `postRegister` 钩子自动重建，无晚注册丢失匹配风险（自愈测试同时覆盖钩子与绕过两条路径）。

## 结论

判定：**合入**。生产常态形态（无关方块右键）-99.6% 且 9/9 同向、其余两形态 -69%/-77% 均 9/9 同向、lean 池零回归、3285 全量绿、双侧判别测试直接证明行为等价；维护成本仅启动期微秒级重建，事件路径正常态零分配。
