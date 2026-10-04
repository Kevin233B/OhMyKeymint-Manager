package com.kevin233.omkmanager.core

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager

data class AppEntry(val pkg: String, val label: String, val isSystem: Boolean)

/**
 * 已安装应用枚举与“推荐选择”启发式。
 *
 * 与 WebUI 前端语义对齐：
 *  - 候选 = 用户安装的应用（排除系统应用）+ 谷歌服务三件套（gsf / gms / vending，需已安装）；
 *  - 跳过 = Root/Shizuku/Xposed 管理类应用（硬编码清单）
 *          + 声明 Root/Shizuku 敏感权限的应用
 *          + 提供 Xposed 模块设置入口（category MODULE_SETTINGS）的应用；
 *  - 推荐选择只追加勾选，不动已有勾选。
 */
object AppList {

    private val SKIP_PKGS = setOf(
        "me.weishu.kernelsu",
        "com.rifsxd.ksunext",
        "me.bmax.apatch",
        "com.topjohnwu.magisk",
        "io.github.huskydg.magisk",
        "eu.chainfire.supersu",
        "com.noshufou.android.su",
        "org.lsposed.manager",
        "de.robv.android.xposed.installer",
        "org.meowcat.edxposed.manager",
        "moe.shizuku.privileged.api",
        "rikka.sui",
        "com.tsng.hidemyapplist",
        "org.frknkrc44.hma_oss",
        "bin.mt.plus",
    )

    private val GOOGLE_PKGS = listOf(
        "com.google.android.gsf",
        "com.google.android.gms",
        "com.android.vending",
    )

    private val ROOT_PERMS = setOf(
        "moe.shizuku.manager.permission.API_V23",
        "moe.shizuku.manager.permission.API",
        "android.permission.ACCESS_SUPERUSER",
        "com.topjohnwu.magisk.permission.REQUEST_SU",
    )

    const val XPOSED_CATEGORY = "de.robv.android.xposed.category.MODULE_SETTINGS"

    fun installed(context: Context): List<AppEntry> {
        val pm = context.packageManager
        val res = ArrayList<AppEntry>()
        for (pi in pm.getInstalledPackages(0)) {
            val ai: ApplicationInfo = pi.applicationInfo ?: continue
            val label = pm.getApplicationLabel(ai)?.toString()?.takeIf { it.isNotBlank() } ?: pi.packageName
            val isSystem = (ai.flags and ApplicationInfo.FLAG_SYSTEM) != 0
            res.add(AppEntry(pi.packageName, label, isSystem))
        }
        return res.sortedWith(compareBy<AppEntry> { it.label.lowercase() }.thenBy { it.pkg })
    }

    /** 返回推荐勾选的包名集合（在 [apps] 全量列表基础上计算）。 */
    fun recommend(context: Context, apps: List<AppEntry>): Set<String> {
        val pm = context.packageManager
        val skip = HashSet(SKIP_PKGS)

        for (pi in pm.getInstalledPackages(PackageManager.GET_PERMISSIONS)) {
            val req = pi.requestedPermissions ?: continue
            if (req.any { it in ROOT_PERMS }) skip.add(pi.packageName)
        }

        val intent = Intent(Intent.ACTION_MAIN).addCategory(XPOSED_CATEGORY)
        for (ri in pm.queryIntentActivities(intent, 0)) {
            val pkg = ri.activityInfo?.packageName ?: continue
            skip.add(pkg)
        }

        val out = LinkedHashSet<String>()
        for (a in apps) {
            if (!a.isSystem && a.pkg !in skip) out.add(a.pkg)
        }
        for (g in GOOGLE_PKGS) {
            if (apps.any { it.pkg == g }) out.add(g)
        }
        return out
    }
}
