# Paper 1.21.11 实机端到端回归 Runbook

> 状态：**第 1 节已实机通过（2026-08-27，本地 Paper 1.21.11 build 132 + Java 21.0.10 headless）**——部署 ✅、加载日志判据全过（必须出现 3/3、不得出现 0 命中，含 shaded 依赖 LinkageError 零出现）、研究重复告警零出现；启动日志存档 [1.21.11-boot-log.txt](1.21.11-boot-log.txt)。
> 第 2-4 节（游戏内三链路冒烟/老存档迁移）仍需客户端交互或带老存档环境，保持待执行。
> 构建产物：`target/SlimeFun4.1-5.1.1.jar`（2.9MB，含 21 轮审计全部修复；版本号待回归通过后再行 bump 与 Release）。
> 环境：Paper 1.21.11 + Java 21（`F:\Java\21`）。

## 0. 部署

1. 备份服务器 `plugins/Slimefun/` 与 `plugins/` 下旧版 jar（含 `data-storage/`）。
2. 移除旧版 Slimefun jar，放入 `SlimeFun4.1-5.1.1.jar`。
3. 全新安装则跳过备份；老存档回归须额外观察第 4 节的迁移项。

## 1. 加载日志检查（无兼容性报错判据）

`latest.log` 中：

- ✅ 必须出现：`Slimefun v5.1.1` 启动横幅、`Available languages: ... zh-CN ...`、`Loaded language "zh-CN"`。
- ❌ 不得出现：`Failed to hook into`（集成降级可接受但需记录）、`Maybe consider updating`、`Asynchronous entity add`、`Unable to find handler list`、任何 `NoSuchMethodError/NoClassDefFoundError/ClassNotFoundException`（重点关注 shaded dough/paperlib 路径）。
- ⚠️ 记录不阻塞：`Two researches share the same legacy id`（若出现说明附属引入重复 id，告警按设计工作）。

## 2. 核心链路冒烟——太阳能发电（回归 5.1.1 修复主题）

1. `/sf cheat` → 放置 **太阳能发电机 + 电缆 + 能量调节器 + 充电的物品（如喷气背包）**。
2. 游戏时间调正午（`/time set noon`），露天。
3. 判据：调节器全息显示网络在线与发电量；**后接入**太阳能（网络先建、发电机后放）同样在数 tick 内激活；背包充能行数值上升。
4. 对应回归测试：`TestEnergyNetActivation` 5 场景（MockBukkit 层已绿，此处验真实 Bukkit 的 `getLocation()` 防御性副本行为）。

## 3. 核心链路冒烟——货运传输

1. 放置 **货运管理器 + 输入节点（接箱子 A）+ 输出节点（接箱子 B）**，同频道。
2. A 放一组煤 → 观察 B 在数秒内收货；拔掉管理器再放回，传输恢复。
3. 判据：无 SEVERE 刷屏（损坏频率限流）、无物品消失（第 6/7 轮菜单同步化在真实 Inventory 上的行为）。

## 4. 核心链路冒烟——机器菜单 + 老存档迁移项

1. 放置电炉并打开 GUI：中文标题/按钮正常；放入铁矿通电 → 产物进输出槽（机器处理路径含 `pushItem` 同步化）。
2. 老存档专项：若沿用 5.0.x 前存档且玩家曾解锁煤发电机/生物反应器——两者应**同时保持解锁**（`researches.173` 双恢复兼容）。
3. 破坏正在工作的机器：库存掉落完整（爆炸/破坏路径）。

## 5. 通过后收尾

全绿后：bump 版本（12 项修复，建议 5.1.2）→ `note/release/5.1.2.md` → 细粒度提交 → GitHub Release（附件即本 jar）。
