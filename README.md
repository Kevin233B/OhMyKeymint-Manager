# OhMyKeymint-Manager

**OMK 管理器** —— [OhMyKeymint](https://github.com/ITxiao6666/OhMyKeymint) 模块的独立 Android 管理器（APK），不依赖 KernelSU WebUI。

- 作者：Kevin233
- UI：Kotlin + Jetpack Compose + [Miuix](https://github.com/compose-miuix-ui/miuix)（MIUI / HyperOS 风格）
- Root 执行：`su -c` 直调模块自带 native helper（`inject` / `keymint` 的 `--webui-*` 子命令），协议与模块 WebUI 后端逐字节对齐

## 功能

- **首页**：模块状态 / 安全补丁 / TEE 状态 / Keybox（源与安全级别）/ 证书撤销检查 / 伪装设备指纹 / 已选应用数 / 最近操作（显示全部、复制、清除）
- **工具**
  - 应用管理：选择需要注入包名的应用（全部 / 已选 / 未选、搜索、推荐选择——用户应用 + 谷歌服务，跳过 Root / Shizuku / Xposed 工具，保留已有勾选）
  - 更换密钥箱：选择 XML 文件（≤64 KiB、UTF-8）替换当前 Keybox
  - 设置安全补丁：同步 Google 最新安全公告日期（source.android.com → source.android.google.cn 回退），或恢复设备默认
  - 指纹伪装：Pixel 设备目录 + 随机 + 应用 / 停用 PIF 指纹
  - 修复腾讯 Soter Server（Beta）
  - 修复微信支付指纹（Soter HAL 配置）
- **设置**：外观（跟随系统 / 浅色 / 深色）、语言（跟随系统 / 简体中文 / English）、关于

## 使用前提

1. 设备已安装并启用 OhMyKeymint 模块（KernelSU / APatch 正常安装即可）
2. 在 KernelSU / APatch 中授予本应用 root 权限

## 构建

GitHub Actions（`.github/workflows/build.yml`）：JDK 21 + Android SDK + Gradle 9.6.0，`./gradlew :app:assembleRelease`，签名密钥随仓库提交（个人自用仓库）。

## 许可

本项目为原创第三方管理器，与上游 OhMyKeymint 及其作者无隶属关系。协议与上游一致：**AGPL-3.0 + 上游附加条款（禁止商用等）**，界面文案沿用上游 WebUI zh-CN 措辞。

上游：[ITxiao6666/OhMyKeymint](https://github.com/ITxiao6666/OhMyKeymint)（其 fork 自 [qwq233/OhMyKeymint](https://github.com/qwq233/OhMyKeymint)）
