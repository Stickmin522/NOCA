# 构建交接记录

此文档记录已有配置，**不是已验证的构建教程**。用户要求暂停开发，本次不继续编译。

| 组件 | 快照中的版本/设置 |
| --- | --- |
| Android Gradle Plugin | 9.3.0 |
| Kotlin / Compose 编译插件 | 2.4.0 |
| 本地尝试的 Gradle | 9.5.0 |
| 本地 JDK | 21 |
| Android API | compile/target 37，min 29 |
| NDK | 28.2.13676358 |
| CMake | 3.22.1 |
| Rust 已验证工具链 | 1.99.1 |
| Rust target | aarch64-linux-android |
| ABI | arm64-v8a |
| Compose BOM | 2026.09.00 |
| Material 3 | 1.5.0-beta01 |

`app/build.gradle.kts` 包含 Rust 编译任务，CMake 导入其静态库并生成 JNI 动态库。已加入 16 KB 页相关链接设置，但尚未对最终 APK 验证。

仓库没有 Gradle Wrapper，也不含 Android SDK、NDK、Rust 安装目录和 Maven 缓存。`local.properties` 是本机 SDK 路径，已被忽略。`NOCA_CARGO` 可指定 Cargo 路径，`NOCA_MAVEN_MIRROR` 是为本机网络限制预留的可选本地 Maven 仓库。

已有两个排障脚本存档在 `archive/tooling/`：`noca-build.ps1` 和 `maven-fetch.mjs`。保留原样用于了解前期尝试；它们包含原工作区结构与本机路径，不应当作跨机器的自动安装或构建方案。脚本所引用的工具链和缓存不随仓库分发。

最近一次 Android 构建阻断在 Compose 插件/依赖解析阶段。后来准备了本地依赖镜像，但没有完成后续 Android 编译验收。Rust 测试及 ARM64 静态库的成功不能替代 JNI、Compose、APK 和实卡验收。
