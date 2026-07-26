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
# 对当前工作区版本（如 4.9.3）运行
./run-benchmark.sh ../target/classes 4.9.3-optimized

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

## 场景说明（与优化提交一一对应）

| 场景 | 对应优化 | 测量内容 |
|---|---|---|
| `blockstorage` / `charge-write` | `b71f6a07c` BlockStorage 延迟序列化 | 5000 个方块每轮一次的 `energy-charge` 写入（电网真实热路径），中位/最小 纳秒/次 |
| `blockstorage` / `save-5000-dirty-blocks` | 同上 | 5000 个脏方块批量 `save()` 落盘耗时 |
| `machine-idle-scan` | `966051f8b` 空转配方负缓存 | 1000 台电炉空转（空输入 / 不匹配垃圾输入两个变体）每 tick 纳秒 |
| `capacitor-texture` | `8add0ddba` 贴图分档去重 | 2000 个电容同档位重复贴图更新每次调用纳秒；若 MockBukkit 支持 Skull 则使用真实 PLAYER_HEAD，否则退化为纯调度开销（结果中有 NOTE 标明） |
| `hologram-label` | `8f383b2d3` 标签未变跳调度 | 2000 个全息重复推送相同标签每次调用纳秒 |
| `ticker-run` | `83e5e72cc` TickerTask 微优化 | 5000 个平凡 ticker 的一次完整 `TickerTask.run()` 毫秒数（含防定时器干扰的采样过滤） |

## MockBukkit 环境注意事项

- `Slimefun.runSync` 在 UNIT_TEST 模式下**立即在调用线程执行**（见 Slimefun.java），
  因此"调度一次任务"的成本体现为任务分配+派发+立即执行，与生产环境主线程执行的
  绝对成本不同，但新旧版本同环境对比依然公平。
- 插件自身的定时 ticker 每 500ms 真实触发一次；ticker 场景通过 `running` 守卫自旋 +
  过快样本丢弃来排除干扰。
- 每次运行开始时删除 `data-storage/`（BlockStorage 的相对路径落盘目录），
  保证两次运行互不影响；MockBukkit 的插件数据目录在系统临时目录，不触碰主项目。
