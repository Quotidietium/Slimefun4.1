# SlimeFun4.1（非官方维护）

> ⚠️ **重要声明**
>
> 本项目**不是** Slimefun 官方团队在维护的版本，而是基于官方 Slimefun4 的**非官方（Unofficial）尝试性维护分支**。
> 本分支与 Slimefun 官方**无任何隶属关系**，所有修改均由社区 / 个人独立完成。
>
> - **官方仓库**：<https://github.com/Slimefun/Slimefun4>
> - 官方 Wiki：<https://github.com/Slimefun/Slimefun4/wiki>
> - 本分支中的任何问题、Bug、改动，**请勿**提交到官方 Issue Tracker，请在本仓库内反馈。

---

## 一、这是什么

Slimefun 是一个为 Spigot / Paper 服务器提供"**无需安装任何 mod 的整合包体验**"的插件——它为 Minecraft 新增超过 **500 个物品与配方**，涵盖背包、喷气背包、魔法祭坛、电力网络、核反应堆、物流运输系统等。

**`SlimeFun4.1`** 是在官方 Slimefun4 的 `experimental` 分支之上衍生出的非官方维护分支。它的核心目标是：

> 让这份插件能够在较新的 Minecraft 版本（最高至 **1.21.11**）上稳定运行，提供**中文本地化**体验，并移除联网 / 上报行为、做**安全加固**，输出一个可自托管、不联网、可控的单一 jar。

---

## 二、本分支相对官方做了什么

本分支的所有改动均围绕以下六个主题展开（详见 [reason.md](reason.md) 与 git 历史）：

### 1. Minecraft 1.21.11 兼容性

官方分支的 1.21 兼容止步于 1.21.2 早期，在 1.21.11 上会因 Bukkit/Paper API 的二进制不兼容而崩溃。本分支采用**多版本兼容层**方案（保持针对 paper-api 1.21.1 编译，用反射兼容 1.21.2+ 的 API 变更），使同一份 jar 同时支持 1.21.1 与 1.21.11：

- 新增 [`VersionedAttribute`](src/main/java/io/github/thebusybiscuit/slimefun4/utils/compatibility/VersionedAttribute.java)：反射回退 `GENERIC_MAX_HEALTH`（≤1.21.1）→ `MAX_HEALTH`（≥1.21.2），修复 `Attribute` 枚举重命名导致的 `NoSuchFieldError`。
- **自实现 `dough.skins`**：官方依赖的 `dough` 中 `CustomGameProfile extends GameProfile`，而 1.21.2 起 `GameProfile` 被改为 `final`，会在**启动阶段**抛 `IncompatibleClassChangeError`。本分支排除了 `dough-skins` 子模块，自行实现 4 个类，全部改用 Bukkit `PlayerProfile` API，不再继承 `GameProfile`。

> 详见 [reason.md](reason.md) 的根因分析与已实施修复说明。

### 2. 中文本地化（zh-CN）

- **物品名本地化机制**：新增 `LanguageFile.ITEMS` 加载管线，按 `SlimefunItem.getId()` 索引翻译，已内置 **534 条** zh-CN 物品名翻译（见 [languages/zh-CN/items.yml](src/main/resources/languages/zh-CN/items.yml)）。
- **动态 lore 安全短语级本地化**：只替换 lore 中**已知的英文标签子串**，运行时动态填入的数值（电力余量、速度、剩余使用次数等）原样保留，避免静态翻译覆盖抹掉动态数据。

### 3. 移除联网 / 上报行为

- 移除了 UpdaterService 的**联网自动更新**部分。
- 移除了匿名数据上报（bStats Metrics 与 Analytics）。

### 4. 项目改名

编译产物改为 `SlimeFun4.1-<version>`，版本号刻意**不包含** `Dev - `、`RC - `、`UNOFFICIAL` 等标记，使内部 UpdaterService 将其归类为 `UNKNOWN` 而**不会自动联网更新**，保护本 fork 不被官方构建覆盖。

### 5. 安全加固

围绕"玩家可控数据不可信"这一威胁模型，针对玩家可篡改的三类载体（物品 NBT/PDC、物品 lore、方块 `owner` 数据）做了系统性防御，例如：

- **PDC 钳制**：有限次使用物品从 PDC 读取的使用次数强制限定在合法区间，防止伪造大数实现无限使用。
- **物品身份双重确认**：`SlimefunItem.getByItem()` 在读取 PDC 物品 ID 之外，额外校验 `Material` 类型一致，防止用 NBT 工具把高价值物品 ID 塞到廉价 Material 上冒充复制。
- **Soulbound 双重确认**：绑定判定要求 PDC 标志位与 lore 同时自洽，仅凭铁砧打出一行 "Soulbound" 文本不再生效。
- **背包归属 IDOR 修复**：打开背包前校验 `当前玩家 == 背包 ownerId`，阻止伪造 lore 打开他人背包。
- **伪造数据防御**：电量 lore 解析、UUID 解析等增加 try/catch，避免非法输入触发异常崩溃。
- **监听器边界加固**：关键监听器补上 `ignoreCancelled = true` 与 `clickedInventory` 空值守卫。

### 6. NPE / 并发修复

修复了大量机器、物品、网络在边界条件下的空指针与并发缺陷（如 GPS / 电梯 / 传送板 `owner` 数据缺失导致的 NPE 与日志刷屏 DoS、监听器与周期任务的取消语义与状态缺陷等）。

---

## 三、兼容性

| 项目 | 要求 |
|---|---|
| **服务端** | Spigot / Paper 或其分支 |
| **Minecraft 版本** | **1.21.1 ~ 1.21.11**（同一份 jar） |
| **编译目标** | paper-api 1.21.1 |
| **Java（运行）** | **16 及以上**（推荐 17+） |
| **Java（测试）** | 21 |

> 说明：本环境未做端到端的 1.21.11 实机回归。完成判据为"源码层不再引用 1.21.11 已移除的符号 + 编译与全部单元测试通过"。第三方 shaded 依赖（旧版 `dough-api`、`paperlib` 1.0.8）若内部引用了 1.21.2+ 已移除的符号，仍可能在实机上报错，需上真实服务器后观察日志。

---

## 四、如何构建

本分支**不提供官方构建站点的下载链接**（那些是官方的构建）。请在本地自行编译：

```bash
mvn clean package
```

构建成功后，产物位于：

```
target/SlimeFun4.1-4.9.1.jar
```

将该 jar 放入服务器的 `plugins/` 目录，重启服务器即可。

> 如果你只想验证编译是否通过（跳过测试）：
> ```bash
> mvn clean package -DskipTests=true
> ```
> 运行单元测试：`mvn test`（基于 MockBukkit）。

---

## 五、可选的第三方集成

本插件以 **soft-depend**（软依赖）方式集成以下插件，安装与否均可运行：

- PlaceholderAPI
- WorldEdit
- ClearLag
- mcMMO
- ItemsAdder
- Orebfuscator

---

## 六、版权与致谢

- 本项目继承自官方 Slimefun4，遵循 **GNU General Public License v3.0**（见 [LICENSE](LICENSE)）。
- Slimefun 最初由 **TheBusyBiscuit** 创建，并由庞大的 Slimefun 社区与数百位贡献者持续维护至今。
- 本非官方分支的所有改动均建立在他们的工作之上，谨致谢意。

---

## 七、免责声明

- 本分支为**非官方维护**，与 Slimefun 官方团队无隶属关系。
- 使用本分支的风险由使用者自行承担；建议在用于生产环境前做好数据备份。
- 本分支的 Bug、建议、Pull Request 请**直接提交到本仓库**，请勿提交到官方 Slimefun4 的 Issue Tracker，以免给官方维护者造成干扰。
