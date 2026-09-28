# Benchmark 性能报告 r13 — 玩家数据 load/save 配置走查消除

- 轮次：r13（LegacyStorage 玩家数据持久化路径）
- 基线：r13base = `3a52e23be`（bench 修订版提交点，含旧走查实现的 LegacyStorage）
- 对照：优化后工作树（`perf(r13)` 提交）
- 方法：bench 全场景交错 9 对（base 先行、每对独立 JVM、min-of-9-rounds 为主指标）、复合 lean 配对校正（lean 池 = 同对内全部非 player-data 变体 raw 比率中位数）
- 判别测试：`TestLegacyPlayerDataWalkEquivalence`（8 项，**新旧两侧实现均全绿**——直接等价性证明）
- 全量回归：3272 tests / 0 failures / 0 errors

## 优化点

`LegacyStorage`（`data-storage/Slimefun/Players/<uuid>.yml` + waypoints 的读写实现）：

1. **load 侧科研段**：原实现按注册表逐科研探测 `playerFile.contains("researches." + id)`（每次一条 config 路径走查，O(registry)），改为一次 `getKeys("researches")` 读出全部解锁 id 建 `HashSet`，注册表匹配变为 O(1) 查找。`researches.173` 兼容分支改为集合成员判断，行为不变。
2. **load 侧背包 contents**：原实现按 `0..size-1` 逐槽 `getItem` 探测（含空槽的路径走查），改为迭代 section 在键、只反序列化存在的槽位。空槽的"缺键"与"null 值"在 `PlayerBackpack#setContents`（`contents.get(i)`）下同义，严格等价；越界/非规范键（如 `"007"`、`"junk"`）按原探测语义跳过。
3. **save 侧死分支删除**：`setValue("researches", null)` / `setValue("backpacks", null)` 预清空后（字节码验证：dough `Config.setValue(path,null)` 直通 `FileConfiguration.set(path,null)` = Bukkit 移除节点语义），后续 `contains(...)` else-if 分支恒为 false——两个死分支（科研分支内含一个 O(unlocked) 的 `stream().anyMatch`，背包分支为逐空槽 contains 走查）整体删除，同时消除了每锁定科研/每空槽一次的配置走查。插件重复 id 守卫的意图（同 id 任一解锁则键保留）由"预清空 + 只写解锁项"构造性保留。

## 语义等价论证（红线 1/2）

- `setValue(path, null)` 的移除语义经 javap 字节码验证（`ifnonnull` 分支 → `store(path, null)` → 原生 `FileConfiguration.set`）。
- 判别测试 8 项在**新旧两侧实现**均全绿：科研解锁往返/重新锁定键清除（钉死移除语义不因死分支删除而退化）、173 共享 id 兼容、非数字与非规范（`"00910022"`）科研键忽略、背包**仅填充槽落键**（snakeyaml 裸解析断言文件键集）、移除背包整段消失（物品复制守卫）、外键/越界 contents 键忽略、损坏 size 从最高槽位推断。
- 文件格式不变：预清空 + 逐键重写的落盘字节集与旧实现一致（死分支本就不产生写入）；外键（附加插件藏于玩家文件的键）经由每 save 重读文件的读-改-写路径保留——**跨 save 缓存 Config 的方案已评估并否决**（会丢弃运行期外部手改/恢复的文件，行为变更）。
- 原子写（tmp + ATOMIC_MOVE 回退 REPLACE_EXISTING）、快照序列化路径、waypoints 未加载世界回写——全部未动。

## 测量学与噪声发现

首版 9 对（`results-r13p*`，registry=250 现实形态）各变体仅 -2~-10%、方向 5-6/9：**机制级差异（数百 ns × 数百次 ≈ 数十 µs）被 ms 级磁盘 I/O 噪声吞没**，且 load-backpacks 单次运行间摆动达 3×（18.3ms→6.5ms，同一份代码）。处理：按协议补充注册表规模放大变体（2500 科研的插件极端形态；走查成本随注册表线性放大，抬升到噪声底之上），提交 bench 修订后重建 base worktree 重跑 9 对（`results-r13v2p*`）。两版原始数据均存档。

## 量化结果（交错 9 对，min 主指标，复合 lean 校正）

| 变体 | base 中位 (µs) | opt 中位 (µs) | 校正比率中位 | 同向对 | 校正区间 |
|---|---:|---:|---:|---:|---|
| load-scale-sparse（2500 科研表，10 解锁） | 522.9 | 249.0 | **0.484（-51.6%）** | **9/9** | 0.381–0.927 |
| save-scale-sparse（2500 科研表，10 解锁） | 3228.8 | 2734.1 | **0.895（-10.5%）** | 8/9 | 0.687–1.064 |
| save-research-heavy（250 表/230 解锁） | 3669.9 | 3534.5 | 0.952（-4.8%） | 7/9 | 0.828–1.069 |
| load-research（230 键文件） | 598.1 | 558.5 | 0.928（-7.2%） | 6/9 | 0.749–1.563 |
| save-research-sparse（250 表/10 解锁） | 2877.3 | 2690.8 | 0.997（-0.3%） | 6/9 | 0.878–1.108 |
| load-backpacks（3×54 格） | 6527.1 | 6473.8 | 0.986（-1.4%） | 5/9 | 0.764–1.672 |
| save-backpacks（3×54 格，36 填充） | 12987.3 | 13296.0 | 1.036（+3.6%） | 4/9 | 0.850–1.239 |

- lean 池每对比率 0.951–1.032（中位 1.002），校正幅度小、无系统性漂移。
- 主判据 `load-scale-sparse` 9/9 全部同向且量级与机制推算一致（2490 次注册表 contains 探测 ≈ 0.16µs/次 ≈ 400µs，叠加 getKeys 一次替换 240 次 contains）。
- 现实规模（250 科研）收益 -4~-7%（7/9、6/9 同向）：与机制预期（~40µs / 数 ms 磁盘主导操作）相符，属真实但小额的改进；`save-backpacks`/`load-backpacks` 处噪声平面（4-5/9，区间跨 1.0），背包侧改动（54 次空槽 contains 消除 + 在键迭代）在该规模下低于噪声分辨力。
- 生产外推：自动保存每玩家每周期一次 save + 登录时一次 load。2500 科研插件服上每玩家 save -0.5ms、load -0.27ms；现实 250 科研服约 -40µs（save）/ -40µs（load）量级。

## 护栏与风险

- 量级判定：**本轮为算法卫生轮**——消除 O(registry) 与 O(空槽) 的配置走查、删除字节码验证确证的死代码；现实规模收益小额（磁盘 I/O 与 YAML 解析序列化为格式锁定项，与 r4 sfb 结论同族），极端插件规模下 load -52% 显著。
- MockBukkit 环境测量缺口（记录于 perf-audit）：`ItemStackMock` 活库存路径序列化为类名形态 `==` 标签导致解析期构造异常；dough Config 重载静默丢弃 item 节点（contents 段变空）。两者为 pre-existing mock 缺陷，新旧实现同样命中（A/B 计时同构、判别测试以文件键级断言绕开）。
- 判别测试中背包内容断言采用 snakeyaml 裸解析 + 字符串值存活键的混合策略，已在测试类 javadoc 记录原因。

## 结论

判定：**合入**。9/9 同向的规模放大主判据 + 新旧双实现全绿的判别测试 + 3272 全量回归绿 + 文件字节集等价，满足红线 1（安全/稳定/兼容）与红线 2（零功能损失）；性能收益在现实规模小额、算法依赖消除在极端规模显著。
