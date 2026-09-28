# 1. 存储层（Storage layer）

日期：2023-11-15
最后更新：2026-09-28（SlimeFun4.1 fork 状态补记）

**在全部工作彻底完成之前，请勿依赖本 ADR 引入的任何 API！**

## 状态（Status）

进行中（Work in progress）

## Fork status (SlimeFun4.1, 2026-09)

Phase 1 已在本 fork 落地，并是**唯一在用的后端**：

- 接口：[`io.github.thebusybiscuit.slimefun4.storage.Storage`](../../src/main/java/io/github/thebusybiscuit/slimefun4/storage/Storage.java)（`@Beta @ThreadSafe`，实际路径与下文设想的 `core.services.storage` 不同）
- 后端：[`io.github.thebusybiscuit.slimefun4.storage.backend.legacy.LegacyStorage`](../../src/main/java/io/github/thebusybiscuit/slimefun4/storage/backend/legacy/LegacyStorage.java)，在 `Slimefun#onEnable` 装配为 `playerStorage`
- Fork 侧加固：`savePlayerData` 增加主线程背包快照参数（异步保存不触碰活 Inventory）；全部 YAML 落盘走 tmp + 原子移入；研究按数字 id 持久化做了 173 冲突迁移兼容（详见 note/audit/）
- Phases 2–6（binary 后端、BlockStorage 迁移等）在本 fork **未开始**；上游 PR #4065 的后续不在本仓库跟进

## 背景（Context）

Slimefun 已存在非常之久，正因如此，我们编写数据持久化的方式也同样古老。
Slimefun 的规模不断增长，而存储层却从未被改造过。
这意味着时至今日，它仍在使用同一套老旧的保存/加载方式。
这本身未必是坏事，但随着 Slimefun 在内容量与服务器装机量两方面都持续增长——我们已经遇到了一些问题。

如今，数据以 YAML 文件保存（有些文件甚至是每行一个 JSON 对象），
这作为配置格式没问题，作为数据存储则很糟糕：文件可能变得非常巨大且容易损坏；
我们的保存方式往往还意味着一次性加载全部数据而非懒加载，总体性能不佳。

很长时间以来，我们一直在讨论以多种形式重写数据存储
（你可能见过 "BlockStorage rewrite" 或 "SQL for PlayerProfiles" 等提法）。
现在是动手的时候了。这将是一次非常大的变更，不会快速完成、也不会仓促上马。

本 ADR 讨论我们数据持久化的未来。

## 决策（Decision）

我们要建立一套新的存储层抽象与实现：
向后兼容，同时为 Slimefun 内部数据存储打开新的可能。
最终目标是让我们能够快速、便捷地支持新的存储后端（如二进制存储、SQL 等），
服务于 [PlayerProfile](https://github.com/Slimefun/Slimefun4/blob/bbfb9734b9f549d7e82291eff041f9b666a61b63/src/main/java/io/github/thebusybiscuit/slimefun4/api/player/PlayerProfile.java)、[BlockStorage](https://github.com/Slimefun/Slimefun4/blob/bbfb9734b9f549d7e82291eff041f9b666a61b63/src/main/java/me/mrCookieSlime/Slimefun/api/BlockStorage.java) 等对象。

我们也希望在保存与加载数据的方式上整体更高效。
今天，我们加载的内容远超所需。
按需加载、仅在需要时加载，可以改善内存占用。

我们将以渐进方式推进，且最初先在实验性语境下进行。
为此，我们应尽量缩小影响面（blast radius），并尽可能多地上提抽象。

### 变更速览

* 在存储之上建立新抽象，便捷地支持多种后端。
* 逐步摆脱基于 YAML 的遗留存储。
* 数据懒加载、懒保存，更高效地管理数据生命周期。

### 实现细节

新增一个名为 [`Storage`](TBD) 的接口，所有存储后端都将实现它。
该接口将包含加载与保存
[PlayerProfile](https://github.com/Slimefun/Slimefun4/blob/bbfb9734b9f549d7e82291eff041f9b666a61b63/src/main/java/io/github/thebusybiscuit/slimefun4/api/player/PlayerProfile.java)、[BlockStorage](https://github.com/Slimefun/Slimefun4/blob/bbfb9734b9f549d7e82291eff041f9b666a61b63/src/main/java/me/mrCookieSlime/Slimefun/api/BlockStorage.java) 等对象的方法。

随后由各后端实现这些方法
（例如 [`LegacyStorageBackend`](TBD)（即今天的 YAML 现状）），
从而提供相应能力。
并非所有存储后端都必须支持每一种数据类型。
例如 SQL 后端可以不支持 [BlockStorage](https://github.com/Slimefun/Slimefun4/blob/bbfb9734b9f549d7e82291eff041f9b666a61b63/src/main/java/me/mrCookieSlime/Slimefun/api/BlockStorage.java)。


## 附加插件（Addons）

目标是让 Addons 在愿意时能够使用乃至实现新的存储后端，
并可被扩展，按自身意愿加载/保存数据。

最初几轮迭代不会聚焦 Addon 支持。我们要先确保
这个新存储层能正常工作、能满足我们当下的需求。

待我们正式支持 Addons 时，会更新本 ADR。

## 权衡与考量（Considerations）

这是一次大变更，因此我们会尽可能渐进地推进。
变更会在 PR 阶段接受测试，并尽可能合入 Dev 版本发布。
如有必要，我们可能发布一个实验性版本。

各 Phase 不会（而且极不可能）在单个 PR 内完成，也不会附带任何时间表。

当前计划如下：

* Phase 1 - 为 [PlayerProfile](https://github.com/Slimefun/Slimefun4/blob/bbfb9734b9f549d7e82291eff041f9b666a61b63/src/main/java/io/github/thebusybiscuit/slimefun4/api/player/PlayerProfile.java) 实现遗留数据后端。
  * 我们要用新存储层、配合现行数据体系来加载玩家数据。
  * 持续监控可能出现的任何问题，并总体上打磨
    这套系统应有的形态
* Phase 2 - 为 [PlayerProfile](https://github.com/Slimefun/Slimefun4/blob/bbfb9734b9f549d7e82291eff041f9b666a61b63/src/main/java/io/github/thebusybiscuit/slimefun4/api/player/PlayerProfile.java) 实现新的实验性二进制后端。
  * 创建一个二进制存储的新后端
  * 以实验性质实现，允许用户主动选择启用
    * 提供警告：这是**实验性**功能，会有 bug。
  * 为所使用的存储后端实现新的统计指标
* Phase 3 - 将新后端标记为 [PlayerProfile](https://github.com/Slimefun/Slimefun4/blob/bbfb9734b9f549d7e82291eff041f9b666a61b63/src/main/java/io/github/thebusybiscuit/slimefun4/api/player/PlayerProfile.java) 的稳定后端。
  * 确认一切正常工作后，将其标记为稳定并移除警告
  * 为当前使用 "legacy" 的用户建立迁移路径
  * 对新服务器默认启用
* Phase 4 - 将 [BlockStorage](https://github.com/Slimefun/Slimefun4/blob/bbfb9734b9f549d7e82291eff041f9b666a61b63/src/main/java/me/mrCookieSlime/Slimefun/api/BlockStorage.java) 迁移到新存储层。
  * 重头戏！我们将着手把存储层接入 BlockStorage。
    这很可能是一次大变更，此处需要尽可能小心。
  * 为 BlockStorage 实现 `legacy` 与 `binary` 实验性存储后端，
    允许用户主动选择启用
    * 提供警告：这是**实验性**功能，会有 bug。
* Phase 5 - 将新存储层标记为 [BlockStorage](https://github.com/Slimefun/Slimefun4/blob/bbfb9734b9f549d7e82291eff041f9b666a61b63/src/main/java/me/mrCookieSlime/Slimefun/api/BlockStorage.java) 的稳定存储层。
  * 确认一切正常工作后，将其标记为稳定并移除警告
  * 确保此处同样具备迁移路径。
  * 对新服务器默认启用
* Phase 6 - 收尾，迁移其余想要迁移的内容
  * 把我们其余的数据存储迁到新层
  * 大概仍会走 实验 -> 稳定 的流程，但前置周期应会
    更短。

## State of work（上游历史快照——本 fork 现状见顶部「Fork status」节）

* Phase 1: In progress（上游 PR：https://github.com/Slimefun/Slimefun4/pull/4065）
* Phase 2: Not started
* Phase 3: Not started
* Phase 4: Not started
* Phase 5: Not started
* Phase 6: Not started
