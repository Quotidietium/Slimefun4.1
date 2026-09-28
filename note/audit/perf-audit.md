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

## 第 2 轮（2026-09-28）：机器 tick 路径每 tick 临时分配消除

**方向**：机器家族每 tick 的临时对象分配与重复解析。A/B 隔离本轮增量：base=Round 1 收官提交（4ccafb3cd，worktree `../sf-perf-r2base`）vs opt=本轮提交（1cb2b95a5），基准以 overlay 同步新场景。

**改动（4 文件）**：

1. `AContainer.tick(Block)`：`inv.getLocation()`（BlockMenu 字段直读零分配）+ 单个 `BlockPosition` 贯穿 getOperation/takeCharge/endOperation/startOperation/findNextRecipeCached（`findNextRecipeCached` 签名改为接收 BlockPosition）——活跃 tick 3-4 个临时对象 → 2 个；无子类覆写 tick，全部 AContainer 子类自动受益。
2. `AGenerator.getGeneratedOutput`：单 BlockPosition 复用（2-3 个 → 1 个）。
3. `Reactor.getGeneratedOutput`：同型，position 经参数传至 createByproduct/burnNextFuel。
4. `GEOMiner.tick`：坐标复用 + 充电闸门迁移到第 1 轮数据复用变体（3 次查找+2 分配 → 1 次查找+0 分配；`start()` 增 position 参数）。

**量化（严格交错 5+5，完整数据见 [benchmark-perf-r2-tick-alloc.md](../report/perf/benchmark-perf-r2-tick-alloc.md)）**：

| 场景 | min Δ | 中位 Δ | 折扣后估计 |
|---|---|---|---|
| machine-processing/active（AContainer.tick） | **-20.4%** | -5% | ~5-15% |
| machine-idle-scan/empty | **-13.1%** | -14% | ~9-14% |
| machine-idle-scan/junk | -30.9% | +11%（噪声） | min 口径正向 |
| generator-tick/burning（新场景，AGenerator） | -2.4% | **-10.6%** | ~6-11% |

控制场景（charge-api/energy-settlement/blockstorage/ticker-run/player-interaction/capacitor/hologram）全部零改动、带内波动。**环境偏向注记**：ticker-run（平凡 ticker，字节级零改动）中位 -4.5%——每对内 opt 恒后跑、会话渐热——目标场景结论已按此折扣，且与消除的分配数（1-3 个年轻代对象 ≈ 50-150ns）定量吻合。

**回归**：全量 3221 项 0 失败；`mvn clean package` BUILD SUCCESS。新增 `BenchGenerator`/`GeneratorTickBench` 场景补齐发电机 tick 量化空白。

## 第 3 轮（2026-09-28）：配方扫描材料预过滤与惰性包装

**方向**：漏斗/货运供料机器的配方扫描路径——输入每次变更使负扫描缓存失效并强制全列表扫描（150 配方档单次 ~10µs，每台每 tick 持续）。A/B：base=Round 2 收官（e8545d4d5，worktree `../sf-perf-r3base`）vs opt=本轮（4dbcfbf14）。

**改动（`AContainer.scanForRecipe`）**：

1. 材料预过滤：输入槽 Material 建 EnumSet，逐配方跳过"所需 Material 不在场"的必不匹配项（isItemSimilar 对异 Material 首判短路）；列表顺序/零输入配方/胜者/matchedNothing 判定逐位不变。
2. `ItemStackWrapper` 由急切全槽构建改为首次比较时惰性逐槽备忘（全败扫描零包装；比较位用的仍是同一包装实例，语义不变）。

**量化（交错 5+5，完整数据见 [benchmark-perf-r3-recipe-scan.md](../report/perf/benchmark-perf-r3-recipe-scan.md)）**：recipe-scan/junk-10 min **-68.6%**/中位 **-78%**；junk-150 min **-47.4%**/中位 **-53.2%**；方向 5 轮全一致。ticker-run 环境指示器持平 → 会话无偏向、无需折扣。控制场景全部带内。

**判别测试**：`TestRecipeScanMaterialPrefilter` 6 项（不虚构匹配/不漏配/同 Material 优先序/多输入部分在场/同 Material 异 meta 仍拒绝/空输入）。全量 **3227 测试 0 失败**；`mvn clean package` BUILD SUCCESS。新增 `BenchHeftyMachine`（150 配方档）与 `recipe-scan` 场景。

## 第 4 轮（2026-09-28）：保存路径序列化缓冲复用

**方向**：自动保存的逐块序列化。A/B：base=Round 3 收官（20f3a7a0b，worktree `../sf-perf-r4base`）vs opt=本轮（f8bd712f0）。

**改动**：`BlockStorage.save()` 脏块循环复用单个 `StringWriter`（`serializeBlockInfo(cfg, string)` 重载，背衬数组稳定在最大块大小）；单参版本保留给 `getBlockInfoAsJson`。**格式红线记录**：.sfb 的 snakeyaml 全文件转储是剩余大头，自研转储器需逐字节复刻其引号/转义策略，判为锁死项不做。

**量化（交错 5+5，详见 [benchmark-perf-r4-save-buffer.md](../report/perf/benchmark-perf-r4-save-buffer.md)）**：save-5000 min **-3.7%**/中位 **-9.1%**（逆风：指示器 +5.6% 逆向漂移，真实改善估计 4-14%）；零改动场景全部经代码归因隔离（machine-processing +17% 判环境噪声——序列化仅在 save 周期运行，加工测量不触发）。全量 **3227 测试 0 失败**；`mvn clean package` BUILD SUCCESS。

## 第 5 轮（2026-09-28）：BlockStorage 打包 long 键——完整实现后实测回归，撤销（负结果）

**方向**：块表键结构重写（用户授权的结构重写方向）。实现：`storage`/`inventories` 改每世界 26/26/12 位打包坐标键，块对齐语义与磁盘对齐，越界读空/写响亮，getRawStorage 重建视图；3232 测试全绿（含 5 项判别测试）。

**量化否决**：charge-write **+100%**、idle-empty **+120%**——`CHM<Long, …>` 每次 `Long.valueOf` 装箱（块键远超 Long 缓存区间），分配+GC 成本超过省下的 Location.hashCode/equals。**revert 后复核回归消除**（212.7ns / 656.6ns），归因确证。详见 [benchmark-perf-r5-packed-keys-reverted.md](../report/perf/benchmark-perf-r5-packed-keys-reverted.md)。

**教训**：装箱键在高频路径不可用；"更便宜的键"必须配原语键容器（类路径上没有）；打包布局与范围守卫设计已入 git 历史（af9c68b7b），未来引入原语键容器可直接复用。红线 1 优先于理论收益——基准体系正是为此存在。

## 第 6 轮（2026-09-28）：cargo 插入扫描——Material 预检 + 惰性 wrapper + 免拷贝分配

**方向**：货运网络分配/插入路径。A/B：base=v5.1.14 发布提交（3fcd3a75d，worktree `../sf-perf-r6base`）vs opt=本轮。新场景 `cargo-route`（真实 CargoNetworkTask 驱动：同包桥 BenchCargoRoute + CARGO_NODE_INPUT/CARGO_MANAGER 占位物品），5 变体（idle/happy-merge/mixed-bounce/mixed-bounce-smartfill/machine-bounce）。

**改动**：vanilla 与菜单两条插入路径在 `isItemSimilar` 前加 Material 预检（isItemSimilar 首查即 Material，逐位等价——R3 证明模式复用）；`ItemStackWrapper` 从每输出尝试的急切分配改为 vanilla 分支遇同 Material 占用槽才惰性创建（bounce 插入零 wrapper）；非 round-robin 分配免 `ArrayList` 拷贝（逐 tick 局部列表、同步任务内无并发修改）。

**量化（交错 14 对——本轮噪声 ±40%/对，逐对 lean 校正配对统计为准，详见 [benchmark-perf-r6-cargo-insert-scan.md](../report/perf/benchmark-perf-r6-cargo-insert-scan.md)）**：mixed-bounce **-9%**、mixed-bounce-smartfill **-9%**（目标路径，不利漂移下稳定为负）；happy-merge 0%；idle -5%；machine-bounce 0%（首轮校正 +6% 触发定向加测 4 对，中位归 1.0，判估计器噪声）。新增 `TestCargoInsertScanSemantics` 7 项判别（含 lore 敏感匹配钉住惰性 wrapper 语义——cargo 的 isItemSimilar 忽略附魔、比较 lore）。全量 **3234 测试 0 失败**；`mvn package` BUILD SUCCESS。

**过程记录**：判别测试初版 4 败全为测试自身缺陷（输出节点坐标与箱子坐标重叠致 `setType(STONE)` 覆盖箱子、附魔/lore 语义记错、余量算错、world 未注册 BlockStorage），经探针逐层定位修正；产品代码 diff 自始未变——判别测试同样会"发现"测试自己的 bug，修复要先归因再动手。

## 环境清单（滚动）

- 基线 worktree `../sf-perf-baseline`（v5.1.13，315df00fb）——v5.1.14 发布判定后已移除。
- 增量基线 worktree（各轮建、各轮清）：r2base–r13base 均已用毕移除（r11base=322cd4b62、r12base=3ae83e3f7、r13base=3a52e23be bench 修订版）。

## 第 7 轮（2026-09-29）：cargo 路由映射缓存——每 tick 重建改失效驱动

**方向**：CargoNet.tick 的 mapInputNodes/mapOutputNodes（默认 cargo-ticker-delay=0 下每 tick 每 network 全量重建，每节点一次 BlockStorage 读 + 频道解析）。A/B：base=10388e681（r7 bench 提交，worktree `../sf-perf-r7base`）vs opt=本轮。新场景 `cargo-mapping`（真实 CargoNet + 同包桥反射驱动，变体 mapping-100/400）。

**改动**：cachedInputs/cachedOutputs 缓存；三点失效（markCargoNodeConfigurationDirty 既有契约 / onClassificationChange 节点集变化 / applyChannelChange 点击即失效保语义）；copy-on-write 交接（重建发布新实例，已交接任务只读）；routingGeneration 代数计数闭合发布竞态（重建期间失效则结果作废下 tick 重建）。分组与损坏上报逻辑逐字保留。

**量化（交错 5 对，详见 [benchmark-perf-r7-routing-cache.md](../report/perf/benchmark-perf-r7-routing-cache.md)）**：mapping-100 **-98%**、mapping-400 **-99.9%**（47µs→0.9µs、93µs→0.1µs/tick），5 对全一致；非目标场景带内。新增 `TestCargoRoutingCache` 5 项判别（真实网络发现路径驱动）。全量 **3239 测试 0 失败**；`mvn package` BUILD SUCCESS。

**契约记录**：带外 BlockStorage 直写 frequency 在下次失效前不生效——与 ItemFilter 缓存同契约；第一方路径全部即时失效。

## 第 8 轮（2026-09-28）：cargo 保护查询逐对缓存——每路由一查改每 (owner, target) 对一查

**方向**：CargoNetworkTask 对保护模块的 hasPermission 查询（输入侧每输入一次 + 输出侧每输出×每输入一次）。A/B：base=1df91ea49（r8 bench 提交，worktree `../sf-perf-r8base`）vs opt=本轮。新场景 `cargo-protection`（反射注入真实 ProtectionManager + 可控拒绝/成本模块）与 `protection-query` 微基准（三档模块栈成本标定）。

**改动**：任务实例内 `HashMap<PermissionQuery, Boolean>`，键 = (owner UUID, target Location) record；`isAllowed()` 收拢两个原内联查询点；null-owner 短路逐字保留。**正确性论证**：任务在主线程同步执行，同 run 内无插件代码可变更保护状态（缓存值恒等于新鲜查询）；缓存生命周期 = 单 tick（跨 tick 权限变化下一 tick 生效，判别测试 4 钉死）。

**量化（交错 9 对——含 idle 守卫信号加测 4 对，lean 校正配对统计，详见 [benchmark-perf-r8-permission-cache.md](../report/perf/benchmark-perf-r8-permission-cache.md)）**：模块查询 144 → 24（**-83%**，判别测试计数钉死：4×2 布局 12→6）；现实档（cheap ~40ns/查）持平（0.992–1.017）；应力档（xheavy ~3µs/查）端到端 **-20%**（1owner min 0.798 / median 0.775），验证"节省 = 120 × 单查成本"线性缩放律。守卫全带内；idle +6.5% 经 diff 归因（无主路径指令级未变）+ mixed-bounce（同早期路径更重负载）-3% 反号交叉检查判为噪声。新增 `TestCargoPermissionCache` 5 项判别。全量 **3245 测试 0 失败**；`mvn package` BUILD SUCCESS。

**方法论注记**：(1) 微基准标定 + 应力点的组合——当优化收益与目标成本线性相关而现实档成本低于噪声底时，用标定值锚定现实、用应力点验证机制，比强行加对更诚实；(2) MockBukkit 环境注记：`closeEntry` 的非空校验先于 no-op 早退，单测直接驱动 `CargoNetworkTask#run()` 需注册 CARGO_NODE_INPUT/CARGO_MANAGER 替身物品（`classifyLocation` 走字符串 ID，此前测试从未暴露此依赖）。

## 第 9 轮（2026-09-29）：TickerTask 分发链解析缓存——每 tick 重解析改随 tick 注册表携带

**方向**：tickLocation 每 tick 每方块的 id 提取 + 物品注册表查找 + ticker/sync 解析（数据→id→物品→ticker→标志四连依赖链）。A/B：base=f09cd2e57（r9 bench 提交，worktree `../sf-perf-r9base`）vs opt=本轮。新场景 `ticker-resolution`（info-get / full-chain 两变体标定解析链成本）。

**改动**：注册表 `Map<ChunkPosition, Map<Location, TickingBlock>>`，TickingBlock 携带不可变 Resolved record（item/ticker/synchronised）；enableTicker 插入即解析（第一方路径全部数据先存后启用）、重复 enable 以 put 刷新；disableTicker/move 两端/destroy=true 删除整体摘除；destroy=false 删除在两个队列排空点清回 null 走惰性重解析（与旧"空数据每 tick 早退"精确等价）；**活 Config 每 tick 现读——机器经由它读写数据，缓存引用=静默 stale-write，明确不做**；getLocations() 公开形状保持。

**量化（交错 9 对，复合 lean 校正，详见 [benchmark-perf-r9-ticker-dispatch.md](../report/perf/benchmark-perf-r9-ticker-dispatch.md)）**：ticker-run **-17%（min）/ -15%（median）**，原始比值 9/9 全部 <1.0；微基准钉死下界 ≥7%（12ns/块），超额部分归因于被移除链的延迟受限性（紧凑微基准的迭代重叠掩盖依赖延迟）。守卫带内（idle +22% 为会话噪声极值——diff 仅 TickerTask，cargo 代码逐字未动，代码归因排除）。新增 `TestTickerResolutionCache` 6 项判别（含活数据三连改写钉死 Config 不缓存）。全量 **3251 测试 0 失败**；`mvn package` BUILD SUCCESS。

**方法论沉淀**：单场景 lean 本会话失稳（目标场景不可自校；亚微秒场景量化抖动 ±40%）——改用**未触及场景组逐对原始比值的中位数（复合 lean）**，稳健性显著提升；"微基准下界 + 端到端实测"的双锚点继续生效（本轮端到端超出下界，差异方向有物理解释：依赖链延迟 vs 吞吐重叠）。

## 第 10 轮（2026-09-29）：科研簿记路径——hasEnabledItems 计算一次缓存 + 玩家科研集合免拷贝计数

**方向**：解锁/头衔/统计的簿记链。A/B：base=ef3ccab2a（r10 bench 提交，worktree `../sf-perf-r10base`）vs opt=本轮。新场景 `research-progress`（100 科研 × 5 已注册物品；unlock-cycle 2000 次真实拥有权翻转 / title 5000 次 / has-unlocked 200000 次门禁）。

**改动（4 文件）**：(1) `Research.hasEnabledItems()` 计算一次 `volatile Boolean` 缓存——原实现处于 `countNonEmptyResearches` 最内层，对注册表每科研每次调用都全量走 items LinkedList；(2) 失效钩子：`SlimefunItem.setResearch` 摘除/挂接两端 + `register()` 状态定稿后保险钩子（覆盖先绑定后注册的非规范顺序，第一方路径均为先注册后绑定）；(3) PlayerProfile 的 setResearched×2/getTitle/sendStats 改 `data.getResearches()` 活集合计数，公开 `getResearches()` 防御拷贝契约原样保留；(4) PlaceholderAPI 3 个占位符改 `getPlayerData().getResearches()` 活视图，消除记分板轮询期整集合拷贝。

**量化（交错 9 对，复合 lean 校正，详见 [benchmark-perf-r10-research-bookkeeping.md](../report/perf/benchmark-perf-r10-research-bookkeeping.md)）**：unlock-cycle **-62%（min，10.1µs→3.8µs）/ -71%（median）**，原始比值 9/9 全部 <0.5；title **-73% / -80%**（2.5µs→0.66µs），9/9 同向；has-unlocked 门禁持平（0.8ns，亚 ns 量化噪声）。守卫无系统性回归（本会话复合 lean 逐对 0.957–1.029，离群值双侧分布）。新增 `TestResearchProgressCaching` 4 项判别（活集合计数 ≡ 公开拷贝计数的不变式、三路失效、头衔阶梯与 RankChangeEvent 边界）。全量 **3254 测试 0 失败**；`mvn package` BUILD SUCCESS。

**契约记录**：ItemState 注册后不可变是缓存前提（判别测试钉死非 ENABLED 状态不计入）；插件直接 `getAffectedItems()` 变异属带外操作不触发失效（与 ItemFilter/routing 缓存同族契约，javadoc 已声明）。

## 第 11 轮（2026-09-29）：指南本地化读路径记忆化——字符串查找三级缓存 + lore 行级 memo

**方向**：生存指南渲染链的本地化读（每分类页 36 显示物品 × 名字查找 + lore 短语扫描 + 每按钮消息查找——本 fork 默认 zh-CN，27 条 lore 短语 + 534 条物品名翻译全部激活）。A/B：base=322cd4b62（r11 bench 提交，worktree `../sf-perf-r11base`）vs opt=本轮（仅 SlimefunLocalization 单文件）。新场景 `guide-render`（category-open 端到端 + localized-item/item-name-lookup/item-clone/lore-translate/message-lookup 微基准与标定锚点）。

**改动**：(1) `getStringOrNull` 内部收口 `cachedString`——`Language→LanguageFile→path` 三级嵌套 CHM，null 也 memo，命中零分配；(2) `translateLore(Language,…)` 行级 memo（lore 行跨物品高度重复）；(3) 显式 `invalidateTranslationCaches()` 带外契约钩子；(4) **明确不做**：getLocalizedItem 共享显示实例（引用语义红线）与 ItemMetaSnapshot 作 lore 源（快照陈旧风险）。

**量化（交错 9 对，复合 lean 校正，详见 [benchmark-perf-r11-guide-localization-memo.md](../report/perf/benchmark-perf-r11-guide-localization-memo.md)）**：lore-translate **-95.5/-96.4%**（1.16µs→47ns，9/9 对 0.031–0.071）、message-lookup **-91.5/-92.2%**（176→14ns，9/9 同向）；端到端 localized-item -5.6% / category-open -2.0%——分母被 MockBukkit `ItemMetaMock.getLore()` 每行 Gson 反序列化+Legacy 序列化支配（字节码证实，6 行 ≈40µs，占 95%），生产 Paper 该成本不存在，锚点分解推算生产比例 **30–55%**。守卫全带内（diff 单文件，代码归因排除）。新增 `TestGuideLocalizationCaching` 4 项判别（含文件域键隔离与带外契约钉死）。全量 **3258 测试 0 失败**；`mvn package` BUILD SUCCESS。

**MockBukkit 陷阱注记（bench 侧已固化）**：(1) 单测环境无语言装载——反射 `addLanguage` 注入 zh-CN+en 并反射置 defaultLanguage；(2) bench 世界默认视为禁用——须显式 `getWorldSettingsService().setEnabled(world, true)`；(3) 启动后注册的物品不走 load pass——须补 `item.load()` 入组，否则指南页空渲染（nonNull=18/papers=0 陷阱，本轮曾中招）；(4) **测量学**：ItemMetaMock 的 getLore/setLore 每行 Gson 往返使 meta 密集路径膨胀 ~40µs/6 行——凡触及 ItemMeta 读写的场景，mock 数字只可作保守下界，生产比例须锚点分解推算。

## 第 12 轮（2026-09-29）：指南搜索路径——物品显示名与可搜索名计算一次缓存

**方向**：生存指南搜索（openSearch 对每个启用物品做 getItemName 的 ItemMeta 全量往返 + stripColor 正则 + toLowerCase——附属服 1000+ 物品时每次搜索 1-15ms 主线程）。A/B：base=3ae83e3f7（r12 bench 提交，worktree `../sf-perf-r12base`）vs opt=本轮（SlimefunItem 名字缓存 + 搜索过滤器一行）。新场景 `guide-search`（500 物品：search-miss 全量走零命中/search-hit 稀疏命中/item-name 微基准）。

**改动**：(1) `getItemName()` 计算一次 volatile 缓存（原每调用 ItemUtils.getItemName → getItemMeta 往返 + getDisplayName，dough 字节码证实）；(2) 新增 `getSearchableName()` = stripColor+toLowerCase(ROOT) 规范化形式，同样计算一次，`isSearchFilterApplicable` 改用之；(3) **零失效钩子**——`itemStackTemplate` 为 private final 且无 setter，缓存输入严格不可变（比 r10 hasEnabledItems 的前提更强）。

**量化（交错 9 对，复合 lean 校正，详见 [benchmark-perf-r12-guide-search-names.md](../report/perf/benchmark-perf-r12-guide-search-names.md)）**：search-miss **-93.7/-93.1%**（775µs→57µs/500 物品，9/9 对 0.052–0.094）；search-hit **-70.5/-69.1%**；item-name **-99.4%**（940ns→5.3ns，meta 往返消除）。3000 物品服线性外推 ~4.6ms→~0.34ms/搜索。守卫双侧 0.73–1.23（会话噪声特征，diff 不触及守卫路径）。新增 `TestSearchNameCaching` 3 项判别（缓存≡直算、可搜索名性质、40 物品稀疏 needle 功能等价——注记：单测环境显示名经本地化层为既有 "Error: No language present" 上游行为）。全量 **3261 测试 0 失败**；`mvn package` BUILD SUCCESS。

## 第 13 轮（2026-09-29）：玩家数据 load/save 配置走查消除——注册表探测改键集匹配 + 死分支删除

**方向**：LegacyStorage 玩家数据持久化（每玩家每自动保存周期一次 save + 登录一次 load；原实现 load 侧按注册表逐科研 `contains` 探测 + 背包逐槽 0..size-1 `getItem` 探测，save 侧预清空后仍保留两处恒 false 的 `contains` else-if 死分支每锁定科研/每空槽走查一次）。A/B：base=3a52e23be（r13 bench 修订版提交，worktree `../sf-perf-r13base`）vs opt=本轮。新场景 `player-data`（save-research-heavy/sparse、save-backpacks、load-research/backpacks + 规模放大 save/load-scale-sparse@2500 科研表）。

**改动（单文件 LegacyStorage）**：(1) load 科研段一次 `getKeys("researches")` 建 HashSet、注册表匹配 O(1)（173 兼容分支改集合成员判断）；(2) load 背包 contents 改迭代在键、只反序列化存在槽位（缺键≡null 值在 `PlayerBackpack#setContents` 下同义；越界/非规范键按原探测语义跳过）；(3) save 侧两个死分支删除（字节码验证：dough `setValue(path,null)` 直通 `FileConfiguration.set` = 移除节点语义，预清空后 `contains` 恒 false）——插件重复 id 守卫意图由"预清空+只写解锁项"构造性保留。**明确不做**：跨 save 缓存 Config（丢弃运行期外部手改/恢复文件的键，行为变更，红线否决）。

**量化（交错 9 对 ×2 版，复合 lean 校正，详见 [benchmark-perf-r13-player-data-walks.md](../report/perf/benchmark-perf-r13-player-data-walks.md)）**：load-scale-sparse **-51.6%**（523µs→249µs，**9/9 全部同向**）；save-scale-sparse **-10.5%**（8/9）；现实 250 科研形态 -4~-7%（6-7/9 同向，与机制推算 ~40µs 相符）；背包变体处噪声平面。**首版 9 对噪声发现**：现实形态机制差异（数十 µs）被 ms 级磁盘 I/O 噪声吞没（load-backpacks 单次运行间摆动 3×），按协议补充规模放大变体（走查成本随注册表线性放大）重跑——bench 修订先提交再重建 base worktree，方法论闭环。新增 `TestLegacyPlayerDataWalkEquivalence` 8 项判别，**在新旧两侧实现均全绿**（直接等价性证明；背包内容断言用 snakeyaml 裸解析+字符串值存活键策略绕开 mock 缺口）。全量 **3272 测试 0 失败**；`mvn package` BUILD SUCCESS。

**MockBukkit 缺口注记（本轮新发现，pre-existing、新旧实现同样命中）**：(1) `ItemStackMock` 活库存路径序列化为类名形态 `==` 标签，paper-api YamlConstructor 解析期按类名找不到注册抛 "Could not deserialize object"（MockBukkit 仅注册别名）；(2) dough Config 重载**静默丢弃 item 节点**（contents 段变空、无异常）——凡涉玩家文件 ItemStack 往返的 mock 断言，须用文件键级（snakeyaml 裸解析）或非 item 值策略。
