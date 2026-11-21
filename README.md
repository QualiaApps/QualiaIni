# QualiaIni

[![Github Releases](https://icon.anvilcraft.dev/github/downloads/QiuShui1012/QualiaIni)][Github Releases]
[![License](https://cdn.jsdelivr.net/npm/@gugle/devins-badges-plus/assets/cozy/license/mit_vector.svg)][License]

[License]: https://github.com/QiuShui1012/QualiaIni/blob/master/LICENSE
[Github Releases]: https://github.com/QiuShui1012/QualiaIni/releases

面向 Qualia 系列项目的 INI 解析库：解析后得到类型化的文档模型，序列化时保持原文一致，文本形式按需构建并缓存。

## 坐标

```groovy
implementation "com.qiushui1012.lib:qualia-ini:1.0.0"
```

产物面向 Java 8 字节码，包含 sources 与 javadoc。

## 快速开始

```java
Ini ini = Ini.parse(path);
IniSection section = ini.section("server");
String host = section.getString("host", "localhost");
int port = section.getInt("port", 25565);
boolean secure = section.getBoolean("secure", false);
```

## 文档模型

`Ini` 是文档，`IniSection` 是节，`IniValue` 是条目值。节与条目都保持写入顺序。

值有四种类型，解析时按字面量推断：

| 类           | 触发条件                      | 示例            |
|--------------|-------------------------------|-----------------|
| `IniString`  | 双引号包裹，或不匹配其它类型  | `hello`、`"42"` |
| `IniInteger` | 纯数字                        | `68`、`-12`     |
| `IniDecimal` | 含小数点或指数                | `3.10`、`1e5`   |
| `IniBoolean` | `true` 或 `false`，大小写不限 | `TRUE`          |

每个值都保留原始字面量，因此解析后直接序列化得到与原文一致的结果，`+007`、`2.500`、`True` 这类写法不会被改写。

### 读取

```java
IniValue value = section.require("port");     // 缺失时抛 IniException
long port = value.asLong();                    // 类型不符时抛 IniException

section.has("port");                           // 是否存在
section.get("port");                           // 缺失时返回 null
section.keys();                                // 按键序返回不可变列表
```

`getString`、`getLong`、`getInt`、`getDecimal`、`getBoolean` 在键缺失时返回给定的默认值，键存在但无法转换时抛 `IniException`。

### 写入

```java
Ini ini = Ini.empty();
ini.section("server")
    .put("host", "localhost")
    .put("port", 25565)
    .put("ratio", new BigDecimal("1.50"))
    .put("secure", true);
```

`put` 有 `String`、`int`、`long`、`double`、`BigDecimal`、`boolean` 与 `IniValue` 七个重载。传入字符串时，如果裸写会让再次解析得到不同的值（含首尾空白、`#`、`;`、`"`、反斜杠、换行，或形如数字、布尔值），会自动加双引号并转义，例如 `put("code", "007")` 输出 `code = "007"`。

## 序列化

三种形态，均为惰性构建：

```java
String text = ini.serialize();          // 首次构建后缓存，文档变更前重复调用不重新拼接
ini.serialize(writer);                  // 直接写入 Writer，不生成完整字符串
ini.serialize(outputStream);            // 以 UTF-8 写入流，流由调用方负责关闭
ini.writeTo(path);                      // 写入文件，自动创建父目录
```

`serialize()` 的返回值在文档被修改后失效，节内 `put`、`remove` 与文档的 `section`、`removeSection` 都会触发失效。流式方法不经过缓存，每次都按当前内容写出。

## 解析规则

- 节名写作 `[name]`，独占一行。
- 条目写作 `key = value`，等号左侧为键，右侧为值，两侧空白会被去除。
- 以 `#` 开头的行是注释。
- 值中的 `#` 或 `;` 开启行内注释，反斜杠转义的双引号包裹的部分除外。
- 双引号包裹的值支持 `\\`、`\"`、`\n`、`\r`、`\t` 转义。
- 空行、注释行以及既不含 `[` 开头也不含等号的行会被跳过。
- 重复节名与未闭合的节头抛 `IniException`。
- 空节合法，序列化输出仅含节头。

`IniException` 继承 `IllegalArgumentException`。

## 构建

```bash
./gradlew build
./gradlew publishToMavenLocal
```

## 发布

发布到 Maven Central 由手动触发的 `Publish to Maven Central` 工作流完成，入口在 Actions 页面。

可填参数：`version` 留空则取 `gradle.properties` 的 `lib.version`；`publishing_type` 取 `automatic` 时上传后自动发布，取 `user_managed` 时只上传，需到 Portal 手动点发布；`dry_run` 只构建与校验；`create_release` 发布成功后创建 GitHub Release。

需要的仓库密钥为 `MAVEN_CENTRAL_USERNAME`、`MAVEN_CENTRAL_PASSWORD`（central.sonatype.com 生成的用户令牌，旧的 OSSRH 令牌会返回 401）、`MAVEN_CENTRAL_SIGNING_KEY`、`MAVEN_CENTRAL_SIGNING_PASSWORD`。签名密钥用 ASCII 装甲私钥，其 Base64 编码同样可用；两者的行尾差异与转义换行由构建脚本统一处理。

工作流先用 `maven-publish` 把产物上传到 OSSRH Staging API，再调用 `POST /manual/upload/defaultRepository/com.qiushui1012.lib` 把部署转入 Portal，缺少后一步时产物只会停留在 Staging API 一侧。上传前会查询 Central 上是否已存在同名版本，存在则直接失败，因为 Central 不允许覆盖已发布版本。签名单独作为一步执行，签名失败与上传失败会在不同的步骤上报出。
