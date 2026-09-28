# 更新流程（Update Procedure）

日期：2024-01-15
最后更新：2026-09-28（SlimeFun4.1 fork 适配）

> **Fork notice (SlimeFun4.1)：** 本 SOP 继承自官方 Slimefun4 仓库，仅作流程参考。在本 fork 中实际约定为：
> - pom 中的版本属性是 **`paper.version`**（当前 `1.21.1`），不存在 `spigot.version`；
> - 实机回归按 [note/report/runbook-1.21.11-regression.md](../../note/report/runbook-1.21.11-regression.md) 执行（本 fork 的既有实践：§1 加载判据 + 浸泡零错误 + 优雅停机）；
> - 发布说明写入 [note/release/](../../note/release/)，审计记录写入 [note/audit/](../../note/audit/)；
> - 测试须以 **Java 21** 运行（`JAVA_HOME` 指向 JDK 21；系统默认 PATH 若为其它大版本会触发 MockBukkit "No jar file selected" 级联失败）；
> - PR/Issue 只提交到本仓库，不走官方 Discord/PR 流程。

## 目标

本 SOP 讲解如何将 Slimefun 更新到最新的 Minecraft 版本。其中大部分内容只适用于大版本更新，但我们也见过小版本更新导致破坏的情况，因此请通读整个 SOP，并确保完成所有适用步骤。

## 更新

### 更新 Bukkit/Spigot

第一步只需在 pom.xml 中更新 Spigot 依赖版本。只有以下两种情况才应这么做：
* 出现了新的大版本（指 MC 意义上的大版本，例如 1.19 -> 1.20）；
* MC 或 Bukkit/Spigot 内部发生了破坏 API 的变更。

要更新版本，请打开 `pom.xml` 找到 `paper.version` 属性（本 fork 基于 paper-api 编译；上游称之为 `spigot.version`），它位于 `properties` 属性块中。将其改为目标 MC 版本即可（如 `1.20`，小版本则为 `1.20.4`）。

更新之后，**务必执行一次构建**以检查编译失败：`mvn clean package -DskipTests=true`。测试的问题我们下一步再谈。

### 更新测试

下一步是确认现有测试仍然正常工作。运行 `mvn test` 并验证所有测试全部通过、无失败或错误。

如果出现失败，需要逐一排查——最好一次只跑一个测试，避免测试间相互污染的可能。如果发现测试本身有问题，请修复它，并记得在 PR 中添加评论说明该测试为何被修改。

如果你在修测试时需要帮助，请在**本仓库**提交 Issue 并附上失败输出。

全部测试通过后，检查 [MockBukkit](https://github.com/MockBukkit/MockBukkit) 是否发布了新版本——它是承担我们测试中 Bukkit 侧的框架。很可能没有新版本（他们通常滞后一段时间），这完全没问题，只需在 PR 中注明即可。

### 游戏内测试

最后也是最关键的一步：在游戏内实测。虽然我们希望测试尽善尽美，但它们做不到（在 MockBukkit 尚未跟进更新时尤其如此）。在发布新版本之前，必须确保一切在游戏内正常工作。

具体做法：用 `mvn clean package` 构建插件，把 `target/` 下的 jar 复制到服务器的 `plugins/` 目录，然后启动服务器。需要测试的内容很多，但以下几项必须覆盖：
* 命令：验证若干命令可用
  * `/sf versions`
  * `/sf cheat`
  * `/sf search`
* 物品：验证若干物品可用（可从 `/sf cheat` 获取）
  * 风之法杖（Wind staff）
  * 任一护符（talisman）
  * 任一背包（backpack）
* 方块：验证可放置、可破坏，且全部正常工作
  * 远古祭坛（Ancient altar）
  * 洗矿机（Ore washer）
  * 煤炭发电机（Coal generator）

务必验证头颅（heads）仍然正常（属于能源网络和煤炭发电机的一部分）。如果头颅皮肤没有加载，应视为 bug：尝试定位问题原因；若不确定成因，请在**本仓库**提交 Issue。

同时确保控制台没有任何报错——此处出现的任何错误都应排查并修复。

如果发现问题，请修复它，并记得在 PR 中添加评论说明为何需要该修复。

> **注意**
> 这一类问题通常意味着我们需要更新 Dough。若是如此，请先向 Dough 提交 PR，然后将 `pom.xml` 中的 Dough 版本号更新为新版本。完成后，务必再执行一次构建确认一切正常。

### 收尾步骤

一切验证通过后，即可提交 PR。我们会尽快处理 :)

PR 开启期间，请确认 E2E 测试通过，并核对它们的输出。E2E 测试结果无误后，最后我们再更新它们。

#### 更新 E2E 测试

**仅在 MC 大版本更新时需要**

在 `e2e-testing.yml` 文件中更新矩阵策略：请把旧大版本的最新小版本加进去（例如 1.21 发布后，加入 1.20.x，x 取该系列已发布的最新版本）。如果 MC 要求新的 Java 版本，也请一并更新 `latest` 一项。

更新完成后推送，并再次确认 E2E 测试仍然通过。
