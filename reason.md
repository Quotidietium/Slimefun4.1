# Slimefun4 不支持 Minecraft 1.21.11 的原因分析

> 仓库：`experimental` 分支，版本 `4.9-UNOFFICIAL`，编译目标 **paper-api 1.21.1**。
> 分析日期：2026-07-25

## 结论

Slimefun4 在 Minecraft **1.21.11** 上无法正常工作，**根本原因是 Bukkit/Paper API 的二进制不兼容**：插件加载阶段的版本检测**不会**拒绝 1.21.11，但插件运行到具体功能时会因 Minecraft **1.21.2** 起引入的 API breaking change 而崩溃（最典型的是 `NoSuchFieldError`）。一句话——本插件的 1.21 兼容范围止于 **1.21.2**，而 1.21.11 远超此范围。

---

## 一、排除：并非“版本门控拒绝加载”

直觉上会以为插件用版本判断把 1.21.11 挡掉了，但代码并非如此。

1. `MinecraftVersion` 枚举中 [`MINECRAFT_1_21(21, 0, "1.21.x")`](src/main/java/io/github/thebusybiscuit/slimefun4/api/MinecraftVersion.java#L62) 走的是双参构造函数，`maxMinorVersion = -1`（**无上限**）。匹配方法 [`isMinecraftVersion(int, int)`](src/main/java/io/github/thebusybiscuit/slimefun4/api/MinecraftVersion.java#L217-L222) 对输入 `(21, 11)` 三个条件全部满足 → 返回 `true`。也就是说**版本枚举在逻辑上接受整个 1.21.x 系列**。
2. 主类 [`Slimefun.isVersionUnsupported()`](src/main/java/io/github/thebusybiscuit/slimefun4/implementation/Slimefun.java#L518-L559) 仅在能确定版本、且无枚举匹配时才拒绝；若 PaperLib 无法识别版本（返回 `0`），它反而打印“无法确定版本”并“假设支持”直接返回 `false`。

→ 因此插件**会正常通过版本检测并完成加载**。1.21.11 不是“被拒绝加载”，而是“加载后运行时崩溃”。

---

## 二、根本原因：针对 paper-api 1.21.1 编译，而 API 在 1.21.2+ 已变

[`pom.xml`](pom.xml) 第 32 行 `paper.version = 1.21.1`，整个插件（含 shaded 的 `dough-api`、`paperlib`）是用 **1.21.1** 的 Bukkit/Paper API 编译的。而 Minecraft **1.21.2** 起，Paper/Spigot API 发生了多次 breaking change，到 1.21.11 这些变更已固化。插件在编译期引用的 1.21.1 API 符号（字段 / 方法 / 枚举常量），在 1.21.11 的运行时库里要么改名、要么移除，JVM 在链接这些符号时即抛出 `NoSuchFieldError` / `NoSuchMethodError` / `IncompatibleClassChangeError`。

---

## 三、最致命的具体崩溃点：`Attribute.GENERIC_MAX_HEALTH`

Minecraft **1.21.2** 移除了所有属性的 `generic.` / `player.` / `zombie.` 前缀（来源：[minecraft.wiki/w/Attribute](https://minecraft.wiki/w/Attribute) — “Removed the `generic.`, `player.`, or `zombie.` prefixes from all the attributes.”）。映射到 Bukkit：

- 1.21.1 API：`Attribute.GENERIC_MAX_HEALTH`（见 [PaperMC 1.21.1 javadoc](https://jd.papermc.io/paper/1.21.1/org/bukkit/attribute/Attribute.html)）
- 1.21.2+ API：`Attribute.MAX_HEALTH`（`GENERIC_` 前缀常量被废弃并在后续移除）

本插件有 **4 处**直接硬编码引用 `Attribute.GENERIC_MAX_HEALTH`，在 1.21.11 上运行时会触发 `NoSuchFieldError`：

| 文件 | 行 | 功能 |
|---|---|---|
| [`VampireBlade.java`](src/main/java/io/github/thebusybiscuit/slimefun4/implementation/items/weapons/VampireBlade.java#L48) | 48 | 吸血鬼之刃——攻击回血读取最大生命 |
| [`MedicalSupply.java`](src/main/java/io/github/thebusybiscuit/slimefun4/implementation/items/medical/MedicalSupply.java#L76) | 76 | 医疗用品 |
| [`Splint.java`](src/main/java/io/github/thebusybiscuit/slimefun4/implementation/items/medical/Splint.java#L34) | 34 | 夹板 |
| [`Bandage.java`](src/main/java/io/github/thebusybiscuit/slimefun4/implementation/items/medical/Bandage.java#L45) | 45 | 绷带 |

---

## 四、为什么没有被兼容代码保护

本插件有一套成熟的跨版本兼容机制——[`utils/compatibility/`](src/main/java/io/github/thebusybiscuit/slimefun4/utils/compatibility/) 包，专门用反射隔离枚举常量的跨版本重命名：

- [`VersionedEnchantment.java`](src/main/java/io/github/thebusybiscuit/slimefun4/utils/compatibility/VersionedEnchantment.java)（附魔 1.20.5 改名：`DIG_SPEED→EFFICIENCY` 等）
- [`VersionedPotionType.java`](src/main/java/io/github/thebusybiscuit/slimefun4/utils/compatibility/VersionedPotionType.java) / [`VersionedPotionEffectType.java`](src/main/java/io/github/thebusybiscuit/slimefun4/utils/compatibility/VersionedPotionEffectType.java)（药水 1.20.5 改名）
- [`VersionedItemFlag.java`](src/main/java/io/github/thebusybiscuit/slimefun4/utils/compatibility/VersionedItemFlag.java)（ItemFlag 1.20.5 改名）
- [`VersionedParticle.java`](src/main/java/io/github/thebusybiscuit/slimefun4/utils/compatibility/VersionedParticle.java)、[`VersionedEntityType.java`](src/main/java/io/github/thebusybiscuit/slimefun4/utils/compatibility/VersionedEntityType.java)

**关键缺陷有二：**

1. 这些兼容类**全部止步于 1.20.5**（源码注释均为 “renamed in 1.20.5”，引用 Spigot 的 `FieldRename.java`），**没有任何针对 1.21.2 breaking change 的适配**。
2. 该包里**没有 `VersionedAttribute`**——也就是说，作者为附魔、药水、ItemFlag、粒子、实体都做了版本隔离，**唯独 `Attribute` 漏掉**，导致第三节那 4 处直接硬编码 `Attribute.GENERIC_*`，没有任何回退保护。

---

## 五、其他受 1.21.2+ 影响的 API 面（潜在崩溃点）

除 `Attribute` 外，1.21.2 / 1.21.3 还有若干 breaking change 会波及本插件（总览见 [Paper 1.21.2 / 1.21.3 tracking issue #11511](https://github.com/PaperMC/Paper/issues/11511)）：

- **ItemFlag / ItemMeta**：`ItemFlag.HIDE_ATTRIBUTES` 在 1.21.2 行为改变，属性与 hide-flag 合并到同一个 data component（[PaperMC#10693](https://github.com/PaperMC/Paper/issues/10693)）。本插件 [`VersionedItemFlag.java`](src/main/java/io/github/thebusybiscuit/slimefun4/utils/compatibility/VersionedItemFlag.java) 只处理到 1.20.5 的 `HIDE_POTION_EFFECTS→HIDE_ADDITIONAL_TOOLTIP`，未覆盖 1.21.2 的这次变化。
- **DataComponent 迁移**：物品附加数据持续从传统 ItemMeta 方法迁移到 `DataComponentTypes`（见 [1.21.11 DataComponentTypes javadoc](https://jd.papermc.io/paper/1.21.11/io/papermc/paper/datacomponent/DataComponentTypes.html)）。
- **Registry API 演进**：附魔、药水、属性全面 Registry 化；[Registries 开发文档](https://docs.papermc.io/paper/dev/registries/) 明确标注为 experimental，跨版本会变。
- **Mojang 映射过渡**：1.21.x 期间 Paper 持续转向 Mojang-mapped 非混淆服务端（[papermc.io/news/1-21-11](https://papermc.io/news/1-21-11)），依赖 NMS 反射 / 混淆名的代码会断裂。

此外，shaded 进 jar 的 `dough-api`、`paperlib`，以及遗留的 `me.mrCookieSlime.*` 代码同样基于旧版 API，运行时同样可能触发上述任一问题。

---

## 六、佐证

- [`pom.xml`](pom.xml#L32) 第 32 行：`paper.version = 1.21.1`（编译目标）。
- git 历史：
  - `bb38e5db7` — “Support 1.21-1.21.2”，**明确声明兼容范围只到 1.21.2**；
  - `3ea21da4f` — “Fix tests in 1.21”；
  - `3544459ba` — “Update to 1.21 (#4248)”。
  
  这些提交表明本分支的 1.21 适配停留在 1.21.x 早期。
- [`ExplosiveTool.java`](src/main/java/io/github/thebusybiscuit/slimefun4/implementation/items/tools/ExplosiveTool.java#L57) 第 57 行用反射处理 1.21 引入的 `BlockExplodeEvent` 构造函数变更——证明作者已知 1.21 系列内部存在 API 变动、需要特判，但这类特判并未覆盖到 1.21.2 之后的 `Attribute` 等变化。

---

## 七、修复方向（供参考）

1. 把 [`pom.xml`](pom.xml) 的 `paper.version` 升级到 1.21.11（或目标服务器版本），重新编译。
2. 新增 `VersionedAttribute`（仿照 [`VersionedEnchantment.java`](src/main/java/io/github/thebusybiscuit/slimefun4/utils/compatibility/VersionedEnchantment.java)，用 `Registry.ATTRIBUTE.get(NamespacedKey.minecraft("max_health"))`，并对 1.21.1 旧名做回退），替换第三节那 4 处 `Attribute.GENERIC_MAX_HEALTH` 硬编码。
3. 把 [`utils/compatibility/`](src/main/java/io/github/thebusybiscuit/slimefun4/utils/compatibility/) 包扩展到 1.21.2 的 ItemFlag / Attribute / Registry 变更。
4. 对 shaded 依赖 `dough-api`、`paperlib` 与遗留 `me.mrCookieSlime.*` 代码在 1.21.11 上做回归。

---

## 参考来源

- Minecraft Wiki — Attribute（1.21.2 移除属性前缀）：https://minecraft.wiki/w/Attribute
- PaperMC 1.21.1 Attribute javadoc（仍为 `GENERIC_*`）：https://jd.papermc.io/paper/1.21.1/org/bukkit/attribute/Attribute.html
- PaperMC 1.21.11 DataComponentTypes javadoc：https://jd.papermc.io/paper/1.21.11/io/papermc/paper/datacomponent/DataComponentTypes.html
- Paper 1.21.2 / 1.21.3 更新跟踪 issue：https://github.com/PaperMC/Paper/issues/11511
- PaperMC ItemFlag.HIDE_ATTRIBUTES 行为变化：https://github.com/PaperMC/Paper/issues/10693
- Paper Registries 开发文档：https://docs.papermc.io/paper/dev/registries/
- Paper 1.21.11 发布说明（Mojang 映射）：https://papermc.io/news/1-21-11
- Spigot FieldRename（compatibility 包引用的依据）：https://hub.spigotmc.org/stash/projects/SPIGOT/repos/craftbukkit/browse/src/main/java/org/bukkit/craftbukkit/legacy/FieldRename.java
