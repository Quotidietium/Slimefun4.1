# Contributing to Slimefun

> **本仓库为非官方维护分支（SlimeFun4.1）——请先阅读本节，再参考下文指南。**
>
> - **Issue 与 Pull Request 请直接提交到本仓库**；请勿提交到官方 Slimefun4 的 Issue Tracker、Discord 或 Crowdin（官方不会处理本分支的问题，README 免责声明同样适用）。
> - **翻译**：本分支的 zh-CN 本地化是仓库内置资源（`src/main/resources/languages/zh/`），不走 Crowdin——直接以 PR 修改语言文件与 `SlimefunItems` 等显示层定义即可；注意 5.1.0 汉化的两条红线（存档兼容/交互一致，见 [note/release/5.1.0.md](note/release/5.1.0.md)）。
> - **构建/测试**：`mvn clean package`、`mvn test`（JUnit 5 + MockBukkit，全量 3287 项，须以 **Java 21** 运行——`JAVA_HOME` 指向 JDK 21，否则 MockBukkit 会级联报错）；产物为 `target/SlimeFun4.1-<version>.jar`。
> - **发布流程**：见 [note/report/runbook-1.21.11-regression.md](note/report/runbook-1.21.11-regression.md) §5 与 [note/release/](note/release/) 既有版本记录；审计规范见 [note/audit/](note/audit/)。

本文档说明你可以通过哪些方式为 Slimefun 做贡献、把这个项目变得更好。<br>
所有贡献须符合我们的[行为准则](.github/CODE_OF_CONDUCT.md)与[许可证](LICENSE)。
请同时遵循我们提供的 Issue 与 Pull Request 模板。

> **已清理章节说明（2026-09-28）**：原文档的"社区参与方式"章节（官方 Issue Tracker 报 bug / Discord 建议投票 / Crowdin 翻译与语言组长 / 官方 Wiki 贡献 / sonarcloud 代码质量入口）描述的是官方 Slimefun4 仓库的流程，对本分支不适用，已整体移除——历史内容可在 git 中找回：`git log -- CONTRIBUTING.md` 后 `git show <提交>:CONTRIBUTING.md`。本分支的参与方式即顶部说明：发现问题 → 本仓库 Issue；修复/改进 → 本仓库 PR（全量测试通过 + 遵循下方风格规范）。

## :toolbox: 如何编译 SlimeFun4.1
Slimefun 使用 Java 编写，并使用 [Maven](https://maven.apache.org/) 编译。<br>
自行编译请按以下步骤：

1. 通过 git 克隆本项目<br>
`$ git clone https://github.com/Quotidietium/Slimefun4.1/`
2. 使用 Maven 编译（需 JDK 21 作为 `JAVA_HOME`）<br>
`$ mvn clean package`
3. 从 `/target/` 目录取得编译产物 `SlimeFun4.1-<version>.jar`。

如果你已经在使用 IDE，请通过 git 导入本项目并设置为 *Maven 项目*。
之后即可用 Maven 的 `clean package` 目标构建。

如有其它问题，请在本仓库开 Issue。

### :repeat: 提交与构建规范（本分支强约定）

以下约定自 2026-09-28 起为本分支的**强制度量**（含性能优化在内的任何改动均适用）：

1. **每次改动后必须编译打包**：一组逻辑改动完成后即执行 `mvn clean package`（或至少 `mvn compile` + 相关测试），确认 BUILD SUCCESS 才能提交。禁止将未经验证编译的改动直接入档。
2. **细粒度提交**：每个提交只承载一个完整、可独立回溯的改动单元（一项修复 / 一项优化 / 一组配套测试 / 一处文档），提交信息按 `type(scope): 主题——细节` 的既有惯例（见 `git log`）。禁止把多个不相关改动混入同一提交。
3. **测试须以 JDK 21 运行**（`JAVA_HOME` 指向 JDK 21）：系统 PATH 若为其它大版本会触发 MockBukkit "No jar file selected" 级联失败。
4. **性能改动须量化**：触及运行时代码的性能改动须用 [benchmark/](benchmark/) 前后对比（同会话交错运行），对比报告归档至 `note/report/perf/`，审计记录追加至 `note/audit/`；不因性能优化牺牲功能、安全性、稳定性与兼容性（含数据格式与公开 API）。
5. **远端推送失败不阻塞**：GitHub 推送若失败（网络/权限），可暂时搁置，本地细粒度提交照常进行，事后补推。

## :star: Pull Request：代码质量
我们始终欢迎代码质量改进。
但请注意：部分设计模式被附属插件依赖，不宜过于激进地改动。
如果你计划做较大规模的重构，建议先开 Issue 说明意图再动手。

#### 文档
代码文档同样是提升可维护性的好方式。
1. 每个类、每个公开方法都应有对应的 Javadoc。
2. 类应包含 `@author` 标签以标明作者。
3. 方法与参数应标注 `@Nullable` 或 `@Nonnull`，说明是否接受 null。

可以通过 `mvn javadoc:javadoc` 在本地生成 Javadoc。

#### 单元测试
单元测试帮助我们以自动化方式验证项目按预期工作。<br>
单元测试越多越好，欢迎提交测试并放入 [src/test/java](src/test/java) 目录。

测试环境使用 [Junit 5 - Jupiter](https://github.com/junit-team/junit5/) 与 [MockBukkit](https://github.com/MockBukkit/MockBukkit)。<br>
每个新单元测试都应带 `@DisplayName` 注解，用纯文本描述该测试验证的内容。

## :black_nib: 代码风格指南
代码风格的核心要义：**保持一致！**<br>
请与你周围的代码保持同风格——一整个包甚至单个文件里混杂多种不一致的代码风格会非常难读难维护。因此请所有人遵循以下原则。

*注意：这些只是指引。如果我们认为有必要，可能会在你的 PR 中要求修改；
但不会因为少量风格不一致而完全拒绝你的 PR——代码总可以之后再重构。
不过还是请尽量遵守我们的代码风格。*

#### 1. 导入（Imports）
* 不要使用通配符（`*`）导入！
* 不要导入未使用的类！
* 不要使用静态导入！
* 一律使用导入，即使在 javadoc 中也不要写出类的完整路径。
#### 2. 注解（Annotations）
* 方法与参数应标注 `@Nullable`（`javax.annotation.Nullable`）或 `@Nonnull`（`javax.annotation.Nonnull`）！
* 覆写方法必须标注 `@Override`！
* 只有一个方法的接口应标注 `@FunctionalInterface`！
* 弃用方法时，应在 javadoc 中添加 `@deprecated` 小节说明原因。
#### 3. 文档（Documentation）
* 每个类、每个公开方法都应有 Javadoc。
* 新包应有 `package-info.java` 文件说明包的用途。
* 类应包含 `@author` 标签。
* 如果与你相关的其它类，请用 `@see` 标签引用。
#### 4. 单元测试（Unit Tests）
* 尽可能编写单元测试。
* 单元测试类与方法不应带访问修饰符——不要 `public`、`protected` 也不要 `private`。
* 每个测试都应带纯文本的 `@DisplayName` 注解！
#### 5. 通用最佳实践
* 不要使用 `Collection#forEach(x -> ...)`，请用规范的 `for (...)` 循环！
* 不要 new `Random` 对象，请使用 `ThreadLocalRandom.current()`！
* 声明 Map 或集合时一律使用基类型！（例如 `List<String> list = new ArrayList<>();`）
* 做字符串大小写转换（如 `String#toUppercase()`）时，务必以 `Locale.ROOT` 为参数！
* 读写文件时，务必用 `StandardCharsets.UTF_8` 指定编码！
* 不要在同一行声明多个字段/变量！（例如不要写：`int x, y, z;`）
* 使用 Logger，避免 `System.out.println(...)` 与 `Throwable#printStacktrace()`，改用 `Logger#log`！
* 不要用异常校验数据，空 catch 块是极坏的习惯——请用正则等其它手段校验。
* 参数标注了 `@Nonnull` 时，应通过 `Validate.notNull(variable, "...");` 强制该行为，并给出有意义的错误消息。
* 任何 `switch/case` 末尾都必须有 `default:` 分支。
* 使用必须关闭的资源时，请用 `try/with-resource`，它会在结束时自动关闭资源。（例如 `try (InputStream stream = ...) {`）
* 数组方括号应跟在类型后面，而不是变量名后面。（例如 `int[] myArray`）
* 枚举必须用 `==` 比较，不要用 `.equals()`！
* 避免直接的字符串拼接，请使用 `StringBuilder`！
* 同时需要 Map 的键与值时，请使用 `Map#entrySet()`！
#### 6. 命名约定
* 类名使用 *PascalCase*（例如 `MyAwesomeClass`）
* 枚举常量使用 *SCREAMING_SNAKE_CASE*（例如 `MY_ENUM_CONSTANT`）
* 常量（`static final` 字段）使用 *SCREAMING_SNAKE_CASE*（例如 `MY_CONSTANT_FIELD`）
* 变量、参数与字段使用 *camelCase*（例如 `myVariableOrField`）
* 所有方法使用 *camelCase*（例如 `myMethod`）
* 包名全部小写，一般应避免连续单词。（例如 `io.github.thebusybiscuit.slimefun4.core.something`）
#### 7. 风格偏好
* 使用**空格**缩进，不要用 Tab！
* 每个文件一个类！请不要把多个类放进一个文件，枚举也一样——新类或新枚举请另建文件。
* 尽量少用三目运算符，只在 return 语句中使用。（例如避免写：`int y = x == null ? 1: 2`）
* 尽量少用所谓"卫语句块"（guard block）。一个卫语句没问题，但在进入正式代码前堆叠多个卫语句……你可能需要重构。示例：
```java
// guard block
if (something) {
  return;
}

// Actual code...
```
* if/else 语句必须带花括号，请避免单行语句。（例如避免写：`if (x == 0) return;`）
* 我们不强制列宽限制，只需尽量避免过长的行。同时请避免折行。
* 修饰方法返回类型的注解应写在行内；修饰方法本身的注解写在上一行：
```java
@Override // <- 描述方法本身。`@Nullable` 只描述返回类型。
public @Nullable String getString() {
  // [...]
}
```
* 注释永远不要与代码同行！写在代码上方或下方。
* 偏离本风格时，请添加 formatter 注释并说明原因。示例：
```java
// @formatter:off - This array represents a 3x3 grid and should be shown as such.
String[] arrays = {
    "1", "2", "3",
    "4", "5", "6",
    "7", "8", "9",
};
// @formatter:on
```
* 确保空行是真正的空行，其中不应包含任何空白字符。
* 空代码块（如构造器）不应超过一行。（例如 `private MyClass() {}`）
* 类与字段的修饰符必须遵循此顺序：<br>
`[public/protected/private] [abstract] [static] [final]`
* 我们推荐如下空格用法：
  * 变量赋值：`int x = 123;`
  * for 循环：`for (int i = 0; i < 10; i++) {`
  * 语句括号前后：`if (x != null) {`
  * 数组初始化内部：`int[] array = { 1, 2, 3 };`
  * 注释的双斜线后：`// This is a comment`
* Slimefun 遵循 **1TBS / OTBS** 括号风格标准（One true brace style）：
```java
private void example(int x) {
    if (x < 0) {
        // x < 0
    } else if (x > 0) {
        // x > 0
    } else {
        // x == 0
    }
}
```
