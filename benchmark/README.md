# SlimeFun4.1 性能基准测试工具

独立的模拟性能测试程序，用于量化 SlimeFun4.1 各性能优化点的实际收益。
基于 MockBukkit 在 JVM 内启动完整插件，模拟数千机器/电容/全息的负载。

## 隔离性说明

- 本目录是**独立的 Maven 工程**，不是主构建的模块（主 `pom.xml` 无 `<modules>`），
  不参与 `mvn package`，也不会被打进插件 jar。
- 不修改主项目的任何源码；通过 system 作用域依赖加载被测版本的**未 shade 类目录**
  （某个 Slimefun 构建的 `target/classes`，与项目自身测试套件的运行方式一致），
  由 `run-benchmark.sh` 自动打包为 `target/slimefun-under-test-<标签>.jar`。
- 运行产物 `target/`、`data-storage/` 已在 `.gitignore` 中忽略；
  **`report/` 不忽略**——各版本的测试报告保存在其中并随 git 追踪，以备后查。

## 运行方法

前置条件：Java 21 + Maven，且被测版本已执行过 `mvn compile`（存在 `target/classes`）。

```bash
# 对当前工作区版本（如 5.1.15）运行
./run-benchmark.sh ../target/classes 5.1.15-current

# 对基线版本（如用 git worktree 检出的 4.9.2）运行
./run-benchmark.sh ../../sf-4.9.2-baseline/target/classes 4.9.2-baseline
```

也可以直接用 Maven（需先自行把类目录打包为 jar）:

```bash
jar cf target/slimefun-under-test.jar -C <类目录> .
mvn compile exec:java -Dsf.jar=target/slimefun-under-test.jar \
  -Dbench.label=<标签> -Dbench.out=report/results-<标签>.txt
```

每次运行在一个全新的 JVM 中启动 MockBukkit、加载完整插件（与项目测试套件相同的
`MockBukkit.mock()` + `MockBukkit.load(Slimefun.class)` 引导方式），依次执行全部场景，
结果以机器可读格式写入 `report/results-<标签>.txt` 并同时打印到控制台：

```
RESULT|<标签>|<场景>|<变体>|<指标>|<单位>|<值>
```

## 场景说明

当前共 **21 个场景类**（`scenarios/` 目录，与 `BenchMain` 逐个注册）。按加入批次：

**初始批次（4.9.3，5 类）**

| 场景 | 对应优化/来源 | 测量内容 |
|---|---|---|
| `blockstorage` / `charge-write`、`save-5000-dirty-blocks` | `b71f6a07c` BlockStorage 延迟序列化 | 5000 方块 `energy-charge` 写入 ns/次；5000 脏方块批量 `save()` 落盘 ms |
| `machine-idle-scan` / `empty-input`、`junk-input` | `966051f8b` 空转配方负缓存 | 1000 台电炉空转（空输入 / 不匹配垃圾输入）每 tick ns |
| `capacitor-texture` / `same-stage-dispatch-only`、`same-stage-real-heads` | `8add0ddba` 贴图分档去重 | 2000 电容同档位重复贴图更新 ns/次（real-heads 变体在 MockBukkit 支持时用真实头颅） |
| `hologram-label` / `unchanged-label` | `8f383b2d3` 标签未变跳调度 | 2000 全息重复推送相同标签 ns/次 |
| `ticker-run` / `5000-trivial-tickers` | `83e5e72cc` TickerTask 微优化 | 5000 平凡 ticker 的一次完整 `TickerTask.run()` ms（含防定时器干扰的采样过滤） |

**4.9.4 防御加固批次（+3 类）**

| 场景 | 测量内容 |
|---|---|
| `machine-processing` / `active` | 活跃机器 takeCharge + 进度 + 配方消费每 tick ns |
| `energy-settlement` / `charging-write`、`saturated-skip` | 电网结算 charge 写入 / 已满跳过 ns/组件 |
| `player-interaction` | 放置 / 破坏 SF 方块（事件→存储+ticker 注册/注销）µs/次 |

**性能优化专项 r1-r16（+13 类，2026-09；逐轮分析与交错 A/B 数据见 [note/report/perf/](../note/report/perf/) 与 [report/](report/) 下 `r*-anal.awk`）**

| 场景 | 变体 | 来源轮 | 测量内容 |
|---|---|---|---|
| `charge-api` | `take-charge-hit`、`take-charge-miss`、`single-arg-remove` | r1 | 充放电读-改-写热路径（数据复用变体收益隔离） |
| `generator-tick` | `burning` | r2 | AGenerator 燃烧发电每 tick ns |
| `recipe-scan` | `junk-10`、`junk-150`、`near-miss-150` | r3（near-miss r16） | 处理机器配方扫描（负缓存失效后的全列表走查；near-miss 为同材质异 meta 全拒守卫） |
| `cargo-route` | `idle`、`happy-merge`、`mixed-bounce`、`mixed-bounce-smartfill`、`machine-bounce` | r6 | 真实 CargoNetworkTask 驱动的货运传输端到端 ns/tick |
| `cargo-mapping` | `mapping-100`、`mapping-400` | r7 | CargoNet 路由映射（100/400 节点网络）ns/tick |
| `cargo-protection` | `prot-bounce-{1,4}owner[s]{,-heavy,-xheavy}` | r8 | 保护模块 hasPermission 查询（cheap/heavy/xheavy 三档模块栈成本标定）端到端 |
| `ticker-resolution` | `info-get`、`full-chain` | r9 | TickerTask 分发链解析（数据读取 / id→物品→ticker 四连）ns/块 |
| `research-progress` | `unlock-cycle`、`title`、`has-unlocked` | r10 | 科研解锁/头衔/门禁簿记链 |
| `guide-render` | `category-open`、`localized-item`、`item-clone`、`item-name-lookup`、`lore-translate`、`message-lookup` | r11 | 生存指南渲染链本地化读（端到端 + 微基准标定锚点） |
| `guide-search` | `search-miss`、`search-hit`、`item-name` | r12 | 指南搜索（500 物品全量走/稀疏命中）与物品名计算 |
| `player-data` | `save-research-{heavy,sparse}`、`save-backpacks`、`save-scale-sparse`、`load-research`、`load-backpacks`、`load-scale-sparse`、`r13-*` | r13 | 玩家数据 load/save 持久化（含 2500 科研规模放大变体） |
| `item-compare` | `sim-vanilla-miss`、`sim-sf-hit`、`resolve-template`、`resolve-vanilla-shared`、`resolve-vanilla-foreign`、`resolve-fresh` | r14 | 物品身份解析与 isItemSimilar 比较（churn 守卫） |
| `multiblock-interact` | `click-no-match`、`click-near-miss`、`click-full-match` | r15 | MultiBlock 右键触发分发（40 台机器贴近真实规模） |

## MockBukkit 环境注意事项

- `Slimefun.runSync` 在 UNIT_TEST 模式下**立即在调用线程执行**（见 Slimefun.java），
  因此"调度一次任务"的成本体现为任务分配+派发+立即执行，与生产环境主线程执行的
  绝对成本不同，但新旧版本同环境对比依然公平。
- 插件自身的定时 ticker 每 500ms 真实触发一次；ticker 场景通过 `running` 守卫自旋 +
  过快样本丢弃来排除干扰。
- 每次运行开始时删除 `data-storage/`（BlockStorage 的相对路径落盘目录），
  保证两次运行互不影响；MockBukkit 的插件数据目录在系统临时目录，不触碰主项目。
