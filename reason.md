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

## 八、已实施的兼容性修复（2026-07-25）

采用**多版本兼容层**方案：保持针对 paper-api 1.21.1 编译，用反射兼容层处理 1.21.2+ 的 API 变更，使同一份 jar 同时支持 1.21.1 与 1.21.11。

1. **新增 [`VersionedAttribute`](src/main/java/io/github/thebusybiscuit/slimefun4/utils/compatibility/VersionedAttribute.java)**
   —— 仿照 [`VersionedPotionType`](src/main/java/io/github/thebusybiscuit/slimefun4/utils/compatibility/VersionedPotionType.java) 的反射模式，先尝试新名 `MAX_HEALTH`（1.21.2+），失败再回退 `GENERIC_MAX_HEALTH`（≤1.21.1）。全程不直接引用任何字段名，从根源上避免 `NoSuchFieldError`，补上了 [`utils/compatibility/`](src/main/java/io/github/thebusybiscuit/slimefun4/utils/compatibility/) 包此前缺失的 `Attribute` 适配。

2. **替换 4 处硬编码** `Attribute.GENERIC_MAX_HEALTH` → `VersionedAttribute.MAX_HEALTH`：
   - [`VampireBlade.java:48`](src/main/java/io/github/thebusybiscuit/slimefun4/implementation/items/weapons/VampireBlade.java#L48)
   - [`Bandage.java:45`](src/main/java/io/github/thebusybiscuit/slimefun4/implementation/items/medical/Bandage.java#L45)
   - [`Splint.java:34`](src/main/java/io/github/thebusybiscuit/slimefun4/implementation/items/medical/Splint.java#L34)
   - [`MedicalSupply.java:76`](src/main/java/io/github/thebusybiscuit/slimefun4/implementation/items/medical/MedicalSupply.java#L76)

3. **其他疑似 breaking 点经核实无需修复**——逐项对照 Paper **1.21.11 官方 javadoc** 确认字段在 1.21.11 仍然存在（仅个别 deprecated，不会触发链接期 `NoSuchFieldError`）：
   - `ItemFlag.HIDE_ATTRIBUTES` / `HIDE_ENCHANTS`：仍为 enum 常量。
   - `Enchantment.KNOCKBACK` / `THORNS` / `FIRE_ASPECT`：仍为 class 静态字段。
   - `PotionEffectType.SATURATION` / `WITHER` / `BLINDNESS` / `SLOW_FALLING` / `WEAKNESS` / `ABSORPTION` 等：字段全部存在。
   - [`SlimefunUtils.java`](src/main/java/io/github/thebusybiscuit/slimefun4/utils/SlimefunUtils.java#L495-L509) 的药水比较已是版本分支兼容写法。

4. **验证**：`mvn clean package` 重新编译——**BUILD SUCCESS**，全部 **1786 个测试通过**（Failures: 0, Errors: 0, Skipped: 7），分析类数 630 → 631（新增 `VersionedAttribute`）。产物在 [build/Slimefun v4.9-UNOFFICIAL.jar](build/)。

### 已知局限

- 本次修复覆盖的是**本项目源码中唯一确定的致命 breaking 点**（`Attribute.GENERIC_MAX_HEALTH`）。shaded 进来的第三方依赖（`dough-api`、`paperlib` 1.0.8）以及遗留 `me.mrCookieSlime.*` 代码若内部也引用了 1.21.2+ 已移除的符号，仍可能在 1.21.11 上报错，需上真实服务器后观察日志。
- 本环境**没有运行中的 Minecraft 1.21.11 服务端**，未能做端到端实测；完成判据为「源码层不再引用 1.21.11 已移除的符号 + 编译与全部单元测试通过」。
- `paperlib` 1.0.8 较旧，其版本探测若无法识别 1.21.11，主类会走"假设支持"分支（不会崩溃，仅失去部分性能优化）。

### 参考（1.21.11 API 核实）

- Paper 1.21.11 Attribute javadoc（字段为 `MAX_HEALTH`，无 `GENERIC_`）：https://jd.papermc.io/paper/1.21.11/org/bukkit/attribute/Attribute.html
- Paper 1.21.11 ItemFlag javadoc（`HIDE_*` 常量仍在）：https://jd.papermc.io/paper/1.21.11/org/bukkit/inventory/ItemFlag.html
- Paper 1.21.11 Enchantment javadoc：https://jd.papermc.io/paper/1.21.11/org/bukkit/enchantments/Enchantment.html
- Paper 1.21.11 PotionEffectType javadoc：https://jd.papermc.io/paper/1.21.11/org/bukkit/potion/PotionEffectType.html

## 九、修复 dough `CustomGameProfile` / `GameProfile` final（1.21.11 启动崩溃）

实际在 Paper 1.21.11 运行时，插件在**启动阶段**即崩溃（比第三节所述的 Attribute 问题更早、更致命）：

```
IncompatibleClassChangeError: class ...libraries.dough.skins.CustomGameProfile
cannot inherit from final class com.mojang.authlib.GameProfile
```

**根因**：shaded 进来的 dough（旧版 `com.github.Slimefun.dough:cb22e71335`）中，`CustomGameProfile` 声明为 `extends com.mojang.authlib.GameProfile`。而 Minecraft 1.21.2 起 `GameProfile` 被改为 `final`，JVM 加载 `CustomGameProfile` 时直接抛 `IncompatibleClassChangeError`。调用链：`onEnable → LocalizationService → Language.<init> → SlimefunUtils.getCustomHead → PlayerSkin.fromBase64 → CustomGameProfile`。

### 方案取舍

- **升级 dough 到上游 `baked-libs` `f8ff25187d`**：该版本已修复 GameProfile final，但引发了**几十处**其它 API 不兼容（`ItemStackEditor` 删除、`ProgrammableAndroid` / `Reactor` / `RecipeType` / `Talisman` 等大量签名变化），相当于同步上游大规模重构，风险与工作量都过高，**放弃**。
- **最终方案**：保留 dough `cb22e71335`（API 与现有代码兼容），在 [pom.xml](pom.xml) 中**排除 `dough-skins` 子模块**，并**自行实现** `io.github.bakedlibs.dough.skins` 下的 4 个类，全部改用 Bukkit/Paper 的 `PlayerProfile` API，完全不继承 `GameProfile`：

| 文件 | 作用 |
|---|---|
| [CustomGameProfile.java](src/main/java/io/github/bakedlibs/dough/skins/CustomGameProfile.java) | 持有 uuid / base64 贴图 / skin url；`apply(SkullMeta)` 用 `setOwnerProfile`（item） |
| [PlayerSkin.java](src/main/java/io/github/bakedlibs/dough/skins/PlayerSkin.java) | `fromBase64` / `fromHashCode` / `fromPlayerUUID` 等工厂方法（API 与 dough 一致） |
| [PlayerHead.java](src/main/java/io/github/bakedlibs/dough/skins/PlayerHead.java) | `getItemStack(skin)`（item）、`setSkin(block, skin, ...)`（block，用 Paper `PlayerProfile`） |
| [UUIDLookup.java](src/main/java/io/github/bakedlibs/dough/skins/UUIDLookup.java) | `getUuidFromUsername`（playerdb.co） |

此外 [ColoredFireworkStar.java](src/main/java/io/github/thebusybiscuit/slimefun4/utils/itemstack/ColoredFireworkStar.java) 原用了 dough 的 `ItemStackEditor`，一并改为直接 `ItemMeta` 操作 + `ChatColors`，对外签名不变。

### 验证

`mvn clean package`：**BUILD SUCCESS**，全部 **1787 个测试通过**（Failures: 0, Errors: 0, Skipped: 7），产物在 [build/Slimefun v4.9-UNOFFICIAL.jar](build/)。

### 已知行为差异

- 自实现的 `PlayerHead.setSkin`（设置头**方块**贴图）改用 Paper `Skull.setPlayerProfile`，不再走 dough 的 NMS 反射 `PlayerHeadAdapter`——行为等价且更稳定。
- 贡献者头像（`GitHubTask`）依赖 mojang sessionserver / playerdb.co 在线查询，受限流/网络影响时该头像跳过（与 dough 原有行为一致，非本次引入）。

## 参考来源

- Minecraft Wiki — Attribute（1.21.2 移除属性前缀）：https://minecraft.wiki/w/Attribute
- PaperMC 1.21.1 Attribute javadoc（仍为 `GENERIC_*`）：https://jd.papermc.io/paper/1.21.1/org/bukkit/attribute/Attribute.html
- PaperMC 1.21.11 DataComponentTypes javadoc：https://jd.papermc.io/paper/1.21.11/io/papermc/paper/datacomponent/DataComponentTypes.html
- Paper 1.21.2 / 1.21.3 更新跟踪 issue：https://github.com/PaperMC/Paper/issues/11511
- PaperMC ItemFlag.HIDE_ATTRIBUTES 行为变化：https://github.com/PaperMC/Paper/issues/10693
- Paper Registries 开发文档：https://docs.papermc.io/paper/dev/registries/
- Paper 1.21.11 发布说明（Mojang 映射）：https://papermc.io/news/1-21-11
- Spigot FieldRename（compatibility 包引用的依据）：https://hub.spigotmc.org/stash/projects/SPIGOT/repos/craftbukkit/browse/src/main/java/org/bukkit/craftbukkit/legacy/FieldRename.java
