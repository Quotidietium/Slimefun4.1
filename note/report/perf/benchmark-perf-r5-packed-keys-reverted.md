# Perf Round 5：BlockStorage 打包 long 键（尝试→实测回归→撤销，负结果归档）

日期：2026-09-28 · JDK 21.0.10 · 交错 A/B 5+5（base=Round 4 收官 ec47802c0 vs opt=打包键提交 af9c68b7b）

## 尝试的改动

`BlockStorage` 的 `storage`/`inventories` 由 `ConcurrentHashMap<Location, …>` 改为每世界打包坐标键 `ConcurrentHashMap<Long, …>`（26/26/12 位 x/z/y，块对齐语义与磁盘 serializeLocation 对齐，越界读返回空/写响亮失败，getRawStorage 重建 Location 视图）。实现完整、3232 测试全绿（含 5 项判别测试）——**但基准否决了它**。

## 实测数据（每轮中位数）

| 场景 | base | opt（打包键） | 判定 |
|---|---|---|---|
| blockstorage/charge-write | 204.7~241.8 | **411.3~559.8** | **+100% 回归** |
| machine-idle-scan/empty | 507.3~712.4 | **1107.0~1358.6** | **+120% 回归** |
| machine-processing/active | 870~1239 | 863~1188 | 持平（场景含大量非 map 工作，稀释） |
| player-interaction/place | 743~1487 | 1238~1974 | 回归 |
| 其余（charge-api/energy-settlement/generator/ticker/recipe-scan） | — | — | 带内/混合 |

## 根因

`CHM<Long, …>` 的每次 `get/put` 都要 `Long.valueOf(long)` **装箱**——块键远超 Long 缓存区间（-128..127），每次操作都是一次年轻代分配。分配+写屏障+GC 压力的成本**超过**了省下的 `Location.hashCode/equals`（5 个 doubleToLongBits 字段）成本。逃逸分析未能穿过 CHM 调用消除该装箱。

**撤销验证**：revert 后单轮复核 charge-write 回到 212.7ns、idle-empty 回到 656.6ns——归因确证。

## 教训与遗留选项

1. 装箱键在高频路径上不可用——任何"更便宜的键"方案必须**原语键**容器。
2. 类路径上无可用的原语键并发 Map（Guava 不在依赖内；自研开放寻址并发表与本项目审计文化不兼容）。
3. 若未来引入原语键容器（如 fastutil 的 `Long2ObjectOpenHashMap` + 外部条带锁，或 JDK 新型 Map），此轮的打包布局（26/26/12，含范围守卫与磁盘对齐论证）可直接复用——已随 revert 归入 git 历史（af9c68b7b）。

## 结论

红线 1（不引入回归）优先于任何理论收益。该轮以**完整实现的负结果**归档：代码可从 git 历史找回，量化证据留存 benchmark/report/results-r5-*.txt。
