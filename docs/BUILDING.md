# 从源码构建

当前发布版使用 `scripts/build.sh` 构建。工程保留了从 APK 恢复源码后的 Gradle 配置，但没有 Gradle Wrapper，也未把它作为当前发布版的可复现构建承诺。

下面的步骤以 Linux 和 Bash 为例。仓库包含运行依赖 jar，不包含 Android SDK、R8、Robolectric 或发布签名私钥。

## 1. 准备工具

需要：

- JDK 17，`java`、`javac`、`jar`、`keytool` 在 PATH 中，或设置 `JAVA_HOME`。
- Python 3。
- Android SDK Platform 35 的 `android.jar`。
- 可在当前机器上运行的 `aapt2`、`zipalign`、`apksigner`，加入 PATH。
- D8/R8 jar；已有发布构建使用 R8 8.3.37。

工具路径可以通过环境变量指定：

- `ANDROID_JAR`：编译使用的 API 35 jar。未设置时，先查 `ANDROID_SDK_ROOT` 或 `ANDROID_HOME` 下的 `platforms/android-35/android.jar`，否则使用 `tools/android-35/android.jar`。
- `RESOURCE_JAR`：`aapt2` 使用、包含框架资源的 `android.jar`，默认同 `ANDROID_JAR`。若另用资源平台 jar，可以单独指定。
- `R8_JAR`：默认 `tools/r8-8.3.37.jar`。
- `SIGN_DIR`：本地签名文件目录，默认位于用户数据目录的 `pelican/signing`，不在源码目录中。

这里的 `tools/` 为本机依赖目录，已被 Git 忽略。脚本不会自动下载数百 MB 的工具链，也不会自动调用真实模型接口。

## 2. 执行构建

在仓库根目录运行。以下路径是示例，请换成实际位置：

```bash
export JAVA_HOME="$HOME/jdk-17"
export ANDROID_SDK_ROOT="$HOME/Android/Sdk"
export PATH="$JAVA_HOME/bin:$ANDROID_SDK_ROOT/build-tools/35.0.0:$PATH"
export R8_JAR="$HOME/android-tools/r8-8.3.37.jar"

bash scripts/build.sh
```

构建会编译资源和 Java、运行本地回归检查、生成 DEX、打包并签名。输出在：

```text
build/Pelican.apk
build/Pelican.apk.sha256
```

校验自己的构建产物：

```bash
cd build
sha256sum --check Pelican.apk.sha256
```

构建日志写到 `verification/` 下的 `.log` 文件，不会提交到 Git。`scripts/patch-*.py` 和 `scripts/update-editor24.py` 是历史开发补丁，不是构建依赖，不要重复执行。

## 3. 签名与覆盖安装

如果指定的 `SIGN_DIR` 中没有签名，脚本会创建一套新的本地签名，并在后续构建中复用。已有签名使用 `pelican.p12`、`store-password.txt` 和别名 `pelican`；口令文件及私钥不得提交。

**自行构建的签名与 GitHub Release 不同，不能保证覆盖已安装的发布版。** 同一包名签名不一致时，Android 会拒绝覆盖安装。不要为了安装测试包先卸载现有应用；请先备份数据，必要时使用独立测试设备。

要继续使用自己的旧签名，应把 `SIGN_DIR` 指向原有签名目录。脚本不包含也不会下载项目的发布私钥。

## 4. 本地 Android JVM 检查

如果 `tools/robolectric/` 中准备了相应的依赖 jar，构建脚本会额外运行 `scripts/test-runtime.sh`。也可在 Java 编译完成后手动运行：

```bash
bash scripts/test-runtime.sh
```

历史环境为 Robolectric 4.16.1、Android API 28 legacy graphics、JDK 17。依赖包括 Robolectric 及其传递依赖、JUnit；SQLite 测试还需对应 JDBC 依赖。仓库中的 `verification/android-runtime/pom.xml` 只列出部分顶层依赖，不代表工具链已随源码完整提供。

运行器通过 `pelican.root` 系统属性把仓库路径传给回放测试，不要求源码放在某个固定目录。Robolectric 仍可能按需从 Maven 下载 Android 运行依赖，因此“本地测试”不等于“完全离线”。这一步不调用你的中转站，也不消耗模型额度。

Android 16 框架测试有额外的原生库限制，尚未计为通过。JVM 测试和真实手机安装、画面渲染、长期后台运行应分别验收，不能混为一谈。

## 5. 发布版校验值

`verification/Pelican.apk.sha256` 保存的是 **2.4 发布附件** 的 SHA256，不是自行重新构建后的哈希。下载 `Pelican-2.4.apk` 后，在包含该文件的目录中对照此校验文件检查。

改动文档或脱敏测试站点，不会自动更换 Release 中的 APK。本次仓库整理没有重新打包，也没有重新发布安装包。
