# Contributing to Slimefun

> **本仓库为非官方维护分支（SlimeFun4.1）——请先阅读本节，再参考下文继承自官方的指南。**
>
> - **Issue 与 Pull Request 请直接提交到本仓库**；请勿提交到官方 Slimefun4 的 Issue Tracker、Discord 或 Crowdin（官方不会处理本分支的问题，README 免责声明同样适用）。
> - **翻译**：本分支的 zh-CN 本地化是仓库内置资源（`src/main/resources/languages/zh/`），不走 Crowdin——直接以 PR 修改语言文件与 `SlimefunItems` 等显示层定义即可；注意 5.1.0 汉化的两条红线（存档兼容/交互一致，见 [note/release/5.1.0.md](note/release/5.1.0.md)）。
> - **构建/测试**：`mvn clean package`、`mvn test`（JUnit 5 + MockBukkit，全量 3221 项，须以 **Java 21** 运行——`JAVA_HOME` 指向 JDK 21，否则 MockBukkit 会级联报错）；产物为 `target/SlimeFun4.1-<version>.jar`。
> - **发布流程**：见 [note/report/runbook-1.21.11-regression.md](note/report/runbook-1.21.11-regression.md) §5 与 [note/release/](note/release/) 既有版本记录；审计规范见 [note/audit/](note/audit/)。
> - 下文的代码风格、Javadoc、单元测试规范照常适用。

This document outlines various ways how you can help contribute to Slimefun and make this a bigger and better project.<br>
All contributions must be inline with our [Code of Conduct](.github/CODE_OF_CONDUCT.md) and [License](LICENSE).
Please also follow the templates for Issues and Pull Requests we provide.

> **已清理章节说明（2026-09-28）**：原文档的"社区参与方式"章节（官方 Issue Tracker 报 bug / Discord 建议投票 / Crowdin 翻译与语言组长 / 官方 Wiki 贡献 / sonarcloud 代码质量入口）描述的是官方 Slimefun4 仓库的流程，对本分支不适用，已整体移除——历史内容可在 git 中找回：`git log -- CONTRIBUTING.md` 后 `git show <提交>:CONTRIBUTING.md`。本分支的参与方式即顶部说明：发现问题 → 本仓库 Issue；修复/改进 → 本仓库 PR（全量测试通过 + 遵循下方风格规范）。

## :toolbox: How to compile SlimeFun4.1
Slimefun is written in Java and uses [Maven](https://maven.apache.org/) for compilation.<br>
To compile it yourself, follow these steps:

1. Clone the project via git<br>
`$ git clone https://github.com/Quotidietium/Slimefun4.1/`
2. Compile the project using Maven（需 JDK 21 作为 `JAVA_HOME`）<br>
`$ mvn clean package`
3. Extract the compiled `SlimeFun4.1-<version>.jar` from your `/target/` directory.

If you are already using an IDE, make sure to import the project via git and set it up as a *Maven project*.
Then you should be able build it via Maven using the goals `clean package`.

If you have any further questions, please open an Issue on this repository.

## :star: Pull Requests: Code Quality
We always welcome quality improvements to the code.
But please keep in mind that some design patterns may not be changed too abruptly if an addon depends on them.
If you plan a larger refactor, consider opening an Issue first to discuss the intended changes.

#### Documentation
Code documentation is also a great way to improve the maintainability of the project.
1. Every class and every public method should have a Javadocs section assigned to it.
2. Classes should also include an `@author` tag to indicate who worked on that class.
3. Methods and parameters should be annotated with `@Nullable` or `@Nonnull` to indicate whether or not null values are accepted.

You can generate the Javadocs locally via `mvn javadoc:javadoc`.

#### Unit Tests
Unit Tests help us test the project to work as intended in an automated manner.<br>
More or better Unit Tests are always good to have, so feel free to submit a Test and place it in our [src/test/java](src/test/java) directory.

We are using [Junit 5 - Jupiter](https://github.com/junit-team/junit5/) and [MockBukkit](https://github.com/MockBukkit/MockBukkit) as our testing environment.<br>
Every new Unit Test should have a `@DisplayName` annotation with a plain text description on what the Unit Test tests.

## :black_nib: Code Style guidelines
The general gist when it comes to code style: **Try to be consistent!**.<br>
Try to stay inline with the code that surrounds you, having an entire package or even a single file that's filled with plenty of different and inconsistent code styles is just hard to read or maintain. That's why we wanna make sure everyone follows these principles.

*Note that these are just guidelines, we may request changes on your pull request if we think there are changes necessary.
But we won't reject your Pull Request completely due to a few styling inconsistencies, we can always refactor code later.
But do try to follow our code style as best as you can.*

#### 1. Imports
* Don't use wildcard (`*`) imports!
* Don't import unused classes!
* Don't use static imports!
* Always use imports, even in javadocs, don't write out the full location of a class.
#### 2. Annotations
* Methods and parameters should be annotated with `@Nullable` (`javax.annotation.Nullable`) or `@Nonnull`(`javax.annotation.Nonnull`)!
* Methods that override a method must be annotated with `@Override`!
* Interfaces with only one method should be annotated using `@FunctionalInterface`!
* If you deprecate a method, add an `@deprecated` section to the javadocs explaining why you did it.
#### 3. Documentation
* Every class and every public method should have a Javadocs section assigned to it.
* New packages should have a `package-info.java` file with documentation about the package.
* Classes should have an `@author` tag.
* If there are any other relevant classes related to yours, add them using the `@see` tag.
#### 4. Unit Tests
* Try to write Unit Tests where possible.
* Unit Test classes and methods should have no access modifier, not `public`, `protected` nor `private`.
* Each Test should have a plain text `@DisplayName` annotation!
#### 5. General best-practices
* Do not use `Collection#forEach(x -> ...)`, use a proper `for (...)` loop!
* Do not create new `Random` objects, use `ThreadLocalRandom.current()` instead!
* Always declare Maps or Collections using their base type! (e.g. `List<String> list = new ArrayList<>();`)
* When doing String operations like `String#toUppercase()`, always specify `Locale.ROOT` as an argument!
* When reading or writing files, always specify the encoding using `StandardCharsets.UTF_8`!
* Do not declare multiple fields/variables on the same line! (e.g. Don't do this: `int x, y, z;`)
* Use a Logger, try to avoid `System.out.println(...)` and `Throwable#printStacktrace()`, use `Logger#log` instead!
* Do not use Exceptions to validate data, empty catch blocks are a very bad practice, use other means like a regular expression to validate data.
* If a parameter is annotated with `@Nonnull`, you should enforce this behaviour by doing `Validate.notNull(variable, "...");` and give a meaningful message about what went wrong
* Any `switch/case` should always have a `default:` case at the end.
* If you are working with a resource that must be closed, use a `try/with-resource`, this will automatically close the resource at the end. (e.g. `try (InputStream stream = ...) {`)
* Array designators should be placed behind the type, not the variable name. (e.g. `int[] myArray`)
* Enums must be compared using `==`, not with `.equals()`!
* Avoid direct string concatenation, use a `StringBuilder` instead!
* If you need both the key and the value from a Map, use `Map#entrySet()`!
#### 6. Naming conventions
* Classes should be in *PascalCase* (e.g. `MyAwesomeClass`)
* Enum constants should be in *SCREAMING_SNAKE_CASE* (e.g. `MY_ENUM_CONSTANT`)
* Constants (`static final` fields) should be in *SCREAMING_SNAKE_CASE* (e.g. `MY_CONSTANT_FIELD`)
* Variables, parameters and fields should be in *camelCase* (e.g. `myVariableOrField`)
* All methods should be in *camelCase* (e.g. `myMethod`)
* Packages must be all lowercase, consecutive words should generally be avoided. (e.g. `io.github.thebusybiscuit.slimefun4.core.something`)
#### 7. Style preferences
* Use **Spaces**, not Tabs!
* One class per file! Please don't put multiple classes into one file, this also applies to enums, make a seperate file for new classes or enums.
* Try to keep ternary operators to a minimum, only in return statements. (e.g. avoid doing this: `int y = x == null ? 1: 2`)
* Try to keep so-called "guard blocks" to a minimum. One guard block is fine but having multiple guard blocks before getting to the actual code... Well, you might wanna refactor your code there. Example:
```java
// guard block
if (something) {
  return;
}

// Actual code...
```
* if/else statements should always include a bracket, please avoid one-line statements. (e.g. Avoid doing: `if (x == 0) return;`)
* We do not enforce any particular width or column limit, just try to prevent your lines from becoming too long. But please avoid line-wrapping.
* Annotations that target the return type of the method should be inline. Annotations which target the method itself should be written in the line above:
```java
@Override // <- Describes the method itself. `@Nullable` describes only the return type.
public @Nullable String getString() {
  // [...]
}
```
* Comments should never go on the same line as code! Always above or below.
* When you deviate from this style, add formatter comments and explain why. Example:
```java
// @formatter:off - This array represents a 3x3 grid and should be shown as such.
String[] arrays = {
    "1", "2", "3",
    "4", "5", "6",
    "7", "8", "9",
};
// @formatter:on
```
* Make sure that empty lines are truly empty, they should not contain any whitespace characters.
* Empty blocks like constructors should not occupy more than one line. (e.g. `private MyClass() {}`)
* Modifiers for classes and fields must follow this order:<br>
`[public/protected/private] [abstract] [static] [final]`
* We recommend using horizontal whitespaces like this:
  * In variable assignments: `int x = 123;`
  * In a for-loop: `for (int i = 0; i < 10; i++) {`
  * Before and after statement parenthesis: `if (x != null) {`
  * Inbetween array initializers: `int[] array = { 1, 2, 3 };`
  * After the double slash of a comment: `// This is a comment`
* Slimefun follows the **1TBS / OTBS** Bracket-Style standard (One true brace style):
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
