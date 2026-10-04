package com.kevin233.omkmanager.core

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * 界面文案（zh-CN 为主，与模块 WebUI locale 措辞保持一致；en 为精简对照）。
 * 通过 [Prefs.lang] 的可读状态驱动重组。
 */
object Str {

    private val ZH = mapOf(
        // 通用
        "app_name" to "OMK 管理器",
        "tab_home" to "首页",
        "tab_tools" to "工具",
        "tab_settings" to "设置",
        "retry" to "重试",
        "loading" to "正在加载…",
        "save" to "保存",
        "refresh" to "刷新",
        "cancel" to "取消",
        // 首页
        "home_status" to "状态",
        "home_status_running" to "运行中",
        "home_status_error" to "需要注意",
        "home_status_loading" to "正在检查",
        "home_keybox" to "Keybox",
        "home_keybox_bundled" to "内置 Keybox",
        "home_keybox_custom" to "自定义 Keybox",
        "home_keybox_hardware" to "谷歌硬件根证书",
        "home_keybox_remote" to "谷歌远程密钥",
        "home_keybox_unknown" to "未知密钥",
        "home_keybox_invalid" to "无效 Keybox",
        "home_keybox_security_level" to "安全级别",
        "home_keybox_tee" to "TEE",
        "home_keybox_strongbox" to "StrongBox",
        "home_keybox_level_unknown" to "未知",
        "home_keybox_revocation" to "证书状态",
        "home_keybox_revocation_not_checked" to "未检查",
        "home_keybox_revocation_not_revoked" to "未撤销",
        "home_keybox_revocation_revoked" to "已撤销",
        "home_keybox_revocation_suspended" to "已暂停",
        "home_keybox_revocation_check_failed" to "检查失败",
        "home_keybox_local_invalid" to "本地验证失败",
        "home_keybox_status_unknown" to "未知",
        "home_security_patch" to "安全补丁",
        "home_tee_status" to "TEE 状态",
        "home_tee_normal" to "正常",
        "home_spoofed_device" to "伪装设备指纹",
        "home_module_status" to "模块状态",
        "home_selected_apps_fmt" to "已选择 %s 个应用",
        "home_recent_activity" to "最近操作",
        "home_activity_empty" to "暂无操作记录",
        "home_activity_events_fmt" to "%s 条记录",
        "home_activity_show_all_fmt" to "显示全部（%s）",
        "home_activity_show_less" to "收起",
        "home_activity_clear" to "清除操作记录",
        "home_activity_copied" to "已复制操作记录",
        "home_activity_cleared" to "操作记录已清除",
        "home_copy_activity" to "复制操作记录",
        // 活动类型
        "activity_targets_saved" to "保存应用选择",
        "activity_keybox_changed" to "更换密钥箱",
        "activity_widevine_installed" to "Widevine 密钥配置",
        "activity_security_patch_synced" to "同步安全补丁",
        "activity_security_patch_restored" to "恢复默认安全补丁",
        "activity_pif_enabled" to "启用 PIF 指纹",
        "activity_pif_disabled" to "停用 PIF 指纹",
        "activity_adb_disabler_changed" to "ADB 禁用器状态变更",
        // 工具
        "tools_title" to "工具",
        "tools_app_management" to "应用管理",
        "tools_app_targets_desc" to "选择需要添加包名的应用",
        "tools_key_management" to "密钥管理",
        "tools_keybox_desc" to "选择 XML 文件替换当前 Keybox",
        "tools_security_patch" to "设置安全补丁",
        "tools_sync_patch_desc" to "同步至 Google 最新安全补丁日期",
        "tools_restore_patch_desc" to "恢复设备默认安全补丁",
        "tools_fingerprint_spoofing" to "指纹伪装",
        "tools_pif_desc" to "获取并应用 Pixel 指纹以伪装 PIF",
        "tools_soter_beta" to "修复腾讯Soter Server (Beta)",
        "tools_soter_beta_desc" to "腾讯 SoterServer 的实验性兼容功能，需要 Zygisk Next。",
        "tools_wechat_soter" to "修复微信支付指纹",
        "tools_wechat_soter_desc" to "配置Qualcomm Soter服务",
        "tools_soter_hal" to "Soter HAL 配置",
        "on" to "已启用",
        "off" to "已停用",
        // 应用管理
        "targets_title" to "应用管理",
        "targets_search_hint" to "搜索",
        "filter_all" to "全部",
        "filter_selected" to "已选择",
        "filter_unselected" to "未选择",
        "app_targets_empty" to "没有匹配的应用",
        "menu_select_all" to "推荐选择",
        "menu_select_recommended_desc" to "选择用户应用和谷歌服务，跳过已识别的 Root、Shizuku 和 Xposed 工具。保留已有勾选。",
        "prompt_saved_target" to "配置已保存",
        "prompt_save_error_fmt" to "保存配置失败：%s",
        "app_system_tag" to "（系统应用）",
        // 更换 Keybox
        "replace_keybox_title" to "更换Keybox",
        "replace_keybox_pick" to "选择 XML 文件",
        "replace_keybox_selected_file_fmt" to "已选择：%s",
        "prompt_keybox_replaced" to "Keybox已更换，将自动加载。",
        "prompt_keybox_too_large" to "文件不能超过 64 KiB。",
        "prompt_keybox_xml_required" to "请选择 XML 文件。",
        "prompt_keybox_utf8" to "文件不是有效的 UTF-8 文本。",
        "prompt_keybox_replace_error_fmt" to "更换Keybox失败：%s",
        // 安全补丁
        "patch_sync" to "同步最新安全补丁",
        "patch_restore" to "恢复默认安全补丁",
        "patch_syncing" to "正在获取公告…",
        "prompt_security_patch_sync_complete_fmt" to "同步安全补丁完成（%s），请重启设备",
        "prompt_security_patch_restored_default" to "已恢复默认安全补丁，请重启设备",
        "prompt_security_patch_sync_error_fmt" to "同步安全补丁失败：%s",
        "prompt_security_patch_restore_error_fmt" to "恢复默认安全补丁失败：%s",
        "current_security_patch_fmt" to "当前安全补丁：%s",
        // PIF
        "pif_fingerprint_title" to "伪装 PIF 指纹",
        "pif_zygisk_next_required" to "需自行安装并启用 Zygisk Next；OMK 不内置、下载或安装该模块。",
        "pif_current_config" to "当前配置",
        "pif_choose_device" to "选择 Pixel 设备",
        "pif_random_device" to "随机",
        "pif_apply" to "应用",
        "pif_disable" to "停用",
        "pif_disabled" to "已停用",
        "pif_security_patch_fmt" to "安全补丁：%s",
        "prompt_pif_applied_fmt" to "已应用 %s 的 PIF 指纹。",
        "prompt_pif_disabled" to "已停用 PIF 指纹伪装。",
        "prompt_pif_apply_error_fmt" to "更新 PIF 指纹伪装失败：%s",
        // Soter Beta
        "soter_beta_enabled" to "启用功能",
        "soter_beta_enabled_desc" to "默认关闭。点击应用后才会保存更改。",
        "soter_beta_reboot" to "请自行安装并启用 Zygisk Next。开启或关闭后均需重启设备；兼容效果尚未验证。",
        "soter_beta_warning" to "仅限 Beta 模拟：返回固定公钥与全零签名，并非真实 TEE 认证，也不能修复支付或加密签名。仅作用于腾讯 SoterServer，不影响 KeyMint。",
        "soter_beta_saved" to "Soter Beta 设置已保存，请重启设备以应用；兼容效果尚未验证。",
        // Soter HAL
        "soter_hal_enabled" to "启用功能",
        "soter_hal_url" to "服务地址 (URL)",
        "soter_hal_token" to "令牌 (Token)",
        "soter_hal_device_id" to "设备 ID",
        "soter_hal_tls_insecure" to "允许不安全 TLS",
        "soter_hal_uid_map" to "UID 映射",
        "soter_hal_saved" to "Soter HAL 配置已保存",
        "soter_hal_too_large" to "配置超出字节限制",
        "soter_hal_mismatch" to "保存后读取不一致",
        // 设置
        "settings_title" to "设置",
        "settings_appearance" to "外观",
        "settings_mode" to "模式",
        "settings_mode_system" to "跟随系统",
        "settings_mode_light" to "浅色",
        "settings_mode_dark" to "深色",
        "settings_language" to "语言",
        "settings_language_auto" to "跟随系统",
        "settings_language_zh" to "简体中文",
        "settings_language_en" to "English",
        "settings_about" to "关于",
        "settings_version" to "版本",
        "settings_author" to "作者",
        "author_name" to "Kevin233",
        "settings_description" to "描述",
        "app_desc" to "OhMyKeymint 模块的第三方管理器（非官方，与上游作者无关）",
        "settings_upstream" to "上游模块",
        "upstream_value" to "ITxiao6666/OhMyKeymint（AGPL-3.0 + 附加条款，禁止商用）",
        // 错误
        "error_load_fmt" to "加载失败：%s",
        "error_root" to "未获取 root 权限（请在 KernelSU / APatch 中授权本应用后重试）",
        "operation_failed_fmt" to "操作失败：%s",
    )

    private val EN = mapOf(
        "app_name" to "OMK Manager",
        "tab_home" to "Home",
        "tab_tools" to "Tools",
        "tab_settings" to "Settings",
        "retry" to "Retry",
        "loading" to "Loading…",
        "save" to "Save",
        "refresh" to "Refresh",
        "cancel" to "Cancel",
        "home_status" to "Status",
        "home_status_running" to "Running",
        "home_status_error" to "Attention",
        "home_status_loading" to "Checking",
        "home_keybox" to "Keybox",
        "home_keybox_bundled" to "Bundled keybox",
        "home_keybox_custom" to "Custom keybox",
        "home_keybox_hardware" to "Google hardware root",
        "home_keybox_remote" to "Google remote key",
        "home_keybox_unknown" to "Unknown key",
        "home_keybox_invalid" to "Invalid keybox",
        "home_keybox_security_level" to "Security level",
        "home_keybox_tee" to "TEE",
        "home_keybox_strongbox" to "StrongBox",
        "home_keybox_level_unknown" to "Unknown",
        "home_keybox_revocation" to "Certificate status",
        "home_keybox_revocation_not_checked" to "Not checked",
        "home_keybox_revocation_not_revoked" to "Not revoked",
        "home_keybox_revocation_revoked" to "Revoked",
        "home_keybox_revocation_suspended" to "Suspended",
        "home_keybox_revocation_check_failed" to "Check failed",
        "home_keybox_local_invalid" to "Local verification failed",
        "home_keybox_status_unknown" to "Unknown",
        "home_security_patch" to "Security patch",
        "home_tee_status" to "TEE status",
        "home_tee_normal" to "Normal",
        "home_spoofed_device" to "Spoofed device fingerprint",
        "home_module_status" to "Module status",
        "home_selected_apps_fmt" to "%s apps selected",
        "home_recent_activity" to "Recent activity",
        "home_activity_empty" to "No activity yet",
        "home_activity_events_fmt" to "%s entries",
        "home_activity_show_all_fmt" to "Show all (%s)",
        "home_activity_show_less" to "Show less",
        "home_activity_clear" to "Clear activity",
        "home_activity_copied" to "Activity copied",
        "home_activity_cleared" to "Activity cleared",
        "home_copy_activity" to "Copy activity",
        "activity_targets_saved" to "Saved app selection",
        "activity_keybox_changed" to "Keybox replaced",
        "activity_widevine_installed" to "Widevine key provisioning",
        "activity_security_patch_synced" to "Security patch synced",
        "activity_security_patch_restored" to "Security patch restored",
        "activity_pif_enabled" to "PIF enabled",
        "activity_pif_disabled" to "PIF disabled",
        "activity_adb_disabler_changed" to "ADB disabler changed",
        "tools_title" to "Tools",
        "tools_app_management" to "App management",
        "tools_app_targets_desc" to "Choose apps to receive keybox packages",
        "tools_key_management" to "Key management",
        "tools_keybox_desc" to "Pick an XML file to replace the current keybox",
        "tools_security_patch" to "Set security patch",
        "tools_sync_patch_desc" to "Sync to Google's latest security patch date",
        "tools_restore_patch_desc" to "Restore device default security patch",
        "tools_fingerprint_spoofing" to "Fingerprint spoofing",
        "tools_pif_desc" to "Fetch and apply a Pixel fingerprint for PIF",
        "tools_soter_beta" to "Fix Tencent Soter Server (Beta)",
        "tools_soter_beta_desc" to "Experimental compatibility for Tencent SoterServer, requires Zygisk Next.",
        "tools_wechat_soter" to "Fix WeChat pay fingerprint",
        "tools_wechat_soter_desc" to "Configure the Qualcomm Soter service",
        "tools_soter_hal" to "Soter HAL config",
        "on" to "Enabled",
        "off" to "Disabled",
        "targets_title" to "App management",
        "targets_search_hint" to "Search",
        "filter_all" to "All",
        "filter_selected" to "Selected",
        "filter_unselected" to "Unselected",
        "app_targets_empty" to "No matching apps",
        "menu_select_all" to "Recommended",
        "menu_select_recommended_desc" to "Select user apps and Google services, skipping detected Root, Shizuku and Xposed tools. Existing checks are kept.",
        "prompt_saved_target" to "Configuration saved",
        "prompt_save_error_fmt" to "Failed to save: %s",
        "app_system_tag" to " (system)",
        "replace_keybox_title" to "Replace keybox",
        "replace_keybox_pick" to "Pick XML file",
        "replace_keybox_selected_file_fmt" to "Selected: %s",
        "prompt_keybox_replaced" to "Keybox replaced, it will be loaded automatically.",
        "prompt_keybox_too_large" to "File must not exceed 64 KiB.",
        "prompt_keybox_xml_required" to "Please select an XML file.",
        "prompt_keybox_utf8" to "File is not valid UTF-8 text.",
        "prompt_keybox_replace_error_fmt" to "Failed to replace keybox: %s",
        "patch_sync" to "Sync latest patch",
        "patch_restore" to "Restore default",
        "patch_syncing" to "Fetching bulletin…",
        "prompt_security_patch_sync_complete_fmt" to "Security patch synced (%s), please reboot",
        "prompt_security_patch_restored_default" to "Default security patch restored, please reboot",
        "prompt_security_patch_sync_error_fmt" to "Failed to sync security patch: %s",
        "prompt_security_patch_restore_error_fmt" to "Failed to restore security patch: %s",
        "current_security_patch_fmt" to "Current security patch: %s",
        "pif_fingerprint_title" to "Spoof PIF fingerprint",
        "pif_zygisk_next_required" to "Zygisk Next must be installed and enabled separately; OMK does not bundle it.",
        "pif_current_config" to "Current config",
        "pif_choose_device" to "Choose a Pixel device",
        "pif_random_device" to "Random",
        "pif_apply" to "Apply",
        "pif_disable" to "Disable",
        "pif_disabled" to "Disabled",
        "pif_security_patch_fmt" to "Security patch: %s",
        "prompt_pif_applied_fmt" to "Applied PIF fingerprint of %s.",
        "prompt_pif_disabled" to "PIF fingerprint spoofing disabled.",
        "prompt_pif_apply_error_fmt" to "Failed to update PIF spoofing: %s",
        "soter_beta_enabled" to "Enable",
        "soter_beta_enabled_desc" to "Off by default. Changes are saved on apply.",
        "soter_beta_reboot" to "Install and enable Zygisk Next yourself. A reboot is required after toggling; compatibility is unverified.",
        "soter_beta_warning" to "Beta simulation only: returns a fixed public key and all-zero signatures, not real TEE attestation, and cannot fix payment or crypto signatures. Only affects Tencent SoterServer, not KeyMint.",
        "soter_beta_saved" to "Soter Beta settings saved, reboot to apply; compatibility is unverified.",
        "soter_hal_enabled" to "Enable",
        "soter_hal_url" to "Service URL",
        "soter_hal_token" to "Token",
        "soter_hal_device_id" to "Device ID",
        "soter_hal_tls_insecure" to "Allow insecure TLS",
        "soter_hal_uid_map" to "UID map",
        "soter_hal_saved" to "Soter HAL config saved",
        "soter_hal_too_large" to "Config exceeds byte limit",
        "soter_hal_mismatch" to "Read-back mismatch after save",
        "settings_title" to "Settings",
        "settings_appearance" to "Appearance",
        "settings_mode" to "Mode",
        "settings_mode_system" to "Follow system",
        "settings_mode_light" to "Light",
        "settings_mode_dark" to "Dark",
        "settings_language" to "Language",
        "settings_language_auto" to "Follow system",
        "settings_language_zh" to "简体中文",
        "settings_language_en" to "English",
        "settings_about" to "About",
        "settings_version" to "Version",
        "settings_author" to "Author",
        "author_name" to "Kevin233",
        "settings_description" to "Description",
        "app_desc" to "Unofficial third-party manager for the OhMyKeymint module (not affiliated with upstream)",
        "settings_upstream" to "Upstream module",
        "upstream_value" to "ITxiao6666/OhMyKeymint (AGPL-3.0 + additional terms, non-commercial)",
        "error_load_fmt" to "Failed to load: %s",
        "error_root" to "Root not available (grant this app in KernelSU / APatch and retry)",
        "operation_failed_fmt" to "Operation failed: %s",
    )

    /** 当前语言是否为英文（跟随系统时按系统 locale 判断）。 */
    val useEn: Boolean
        get() = when (Prefs.lang) {
            Prefs.Lang.SYSTEM -> {
                val loc = java.util.Locale.getDefault()
                !(loc.language == "zh")
            }
            Prefs.Lang.ZH -> false
            Prefs.Lang.EN -> true
        }

    fun t(key: String): String =
        (if (useEn) EN[key] else ZH[key]) ?: key
}

/**
 * 偏好设置（主题 / 语言），SharedPreferences 持久化。
 * 顶层 [mutableStateOf] 保证 Compose 读取时可感知变更。
 */
object Prefs {

    enum class Theme { SYSTEM, LIGHT, DARK }
    enum class Lang { SYSTEM, ZH, EN }

    var theme by mutableStateOf(Theme.SYSTEM)
        private set
    var lang by mutableStateOf(Lang.SYSTEM)
        private set

    private var sp: android.content.SharedPreferences? = null

    fun init(context: android.content.Context) {
        if (sp != null) return
        sp = context.getSharedPreferences("omk_manager", android.content.Context.MODE_PRIVATE)
        theme = sp!!.getString("theme", null)?.let { runCatching { Theme.valueOf(it) }.getOrNull() } ?: Theme.SYSTEM
        lang = sp!!.getString("lang", null)?.let { runCatching { Lang.valueOf(it) }.getOrNull() } ?: Lang.SYSTEM
    }

    fun updateTheme(t: Theme) {
        theme = t
        sp?.edit()?.putString("theme", t.name)?.apply()
    }

    fun updateLang(l: Lang) {
        lang = l
        sp?.edit()?.putString("lang", l.name)?.apply()
    }
}
