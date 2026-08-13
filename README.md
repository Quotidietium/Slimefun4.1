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

在官方代码的基础上做了一点点改动，大致如下（细节见 git 历史）：

- 添加了对较高 Minecraft 版本（1.21.2+）的兼容，目前同一份 jar 可在 1.21.1 至 1.21.11 上运行
- 补充了中文（zh-CN）本地化
- 移除了联网自动更新与匿名数据上报
- 编译产物改名为 `SlimeFun4.1-<version>`，避免被官方构建覆盖
- 修复了一些边界条件下的空指针与数据校验问题
- 近期做了一点性能上的小优化（有兴致可自行查看 [benchmark/](benchmark/) 目录下的对比数据）

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
target/SlimeFun4.1-5.0.0.jar
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
