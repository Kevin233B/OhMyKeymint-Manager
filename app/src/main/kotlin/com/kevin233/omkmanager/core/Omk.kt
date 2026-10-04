package com.kevin233.omkmanager.core

import org.json.JSONArray
import org.json.JSONObject
import org.json.JSONTokener

/**
 * OhMyKeymint helper CLI 客户端。
 *
 * 与模块 WebUI 后端协议逐字节对齐：
 *  - 发现：`/data/adb/omk` 或 `/data/adb/modules/oh_my_keymint/libs/<abi>` 下可执行的 inject / keymint；
 *  - 所有校验均在 helper 内完成，本层只传参并按 WebUI 前端的规则解析输出。
 */
object Omk {

    // ===== 数据模型 =====

    data class Helpers(val inject: String, val keymint: String)

    data class KeyboxState(
        val valid: Boolean,
        val bundled: Boolean,
        val source: String,          // google_hardware | google_remote | unknown
        val level: String,           // tee | strongbox | unknown
        val playIntegrity: String,   // not_checked
        val revocation: String,      // not_checked | not_listed | suspended | revoked | unknown
    )

    data class PifState(
        val enabled: Boolean,
        val model: String? = null,
        val product: String? = null,
        val fingerprint: String? = null,
        val securityPatch: String? = null,
    )

    data class PifDevice(val model: String, val product: String)

    data class ActivityEntry(val action: String, val detail: String, val timestamp: Long) {
        /** timestamp 为秒级 epoch。 */
        val timestampMillis: Long get() = timestamp * 1000L
    }

    data class SoterHal(
        val enabled: Boolean,
        val url: String,
        val token: String,
        val deviceId: String,
        val tlsInsecure: Boolean,
        val uidMap: String,
    )

    // ===== 常量（与 WebUI 前端一致） =====

    private const val KEYBOX_MAX_BYTES = 65536       // Jw
    private const val KEYBOX_CHUNK = 49152          // 单 argv 分块
    private const val ACTIVITY_MAX = 30             // Uw
    private const val DETAIL_MAX_BYTES = 256       // Ww
    private const val TIMESTAMP_MAX = 253402300799L // Gw（秒）
    private const val SOTER_HAL_JSON_MAX = 16384    // Rw

    private val PACKAGE_RE = Regex("^[A-Za-z0-9_]+(?:\\.[A-Za-z0-9_]+)*$")
    private val PIF_PRODUCT_RE = Regex("^[a-z0-9][a-z0-9_]*$")

    val ACTIVITY_ACTIONS = setOf(
        "targets_saved", "keybox_changed", "widevine_installed",
        "security_patch_synced", "security_patch_restored",
        "pif_enabled", "pif_disabled", "adb_disabler_changed",
    )

    // ===== 发现 =====

    private var cachedHelpers: Helpers? = null

    suspend fun helpers(force: Boolean = false): Helpers {
        cachedHelpers?.let { if (!force) return it }
        val out = try {
            Su.exec(
                "/system/bin/getprop ro.product.cpu.abilist; " +
                    "/system/bin/getprop ro.product.cpu.abi; " +
                    "/system/bin/uname -m 2>/dev/null || :",
                maxOut = 4096,
            )
        } catch (e: Exception) {
            throw Su.SuException(e.message ?: "无法探测 Android ABI")
        }
        val abi = out.split(Regex("[\\s,]+")).asSequence()
            .filter { it.isNotBlank() }
            .mapNotNull { t ->
                when (t.trim()) {
                    "arm64-v8a", "aarch64" -> "arm64-v8a"
                    "x86_64", "amd64" -> "x86_64"
                    else -> null
                }
            }
            .firstOrNull()
            ?: throw Su.SuException("无法识别 Android ABI：OMK 仅提供 arm64-v8a 和 x86_64 二进制")

        val bases = listOf("/data/adb/omk", "/data/adb/modules/oh_my_keymint/libs/$abi")
        for (base in bases) {
            val test = "[ -x ${Su.shq("$base/inject")} ] && [ -x ${Su.shq("$base/keymint")} ]"
            val ok = try {
                Su.exec(test, maxOut = 64)
                true
            } catch (_: Exception) {
                false
            }
            if (ok) {
                val h = Helpers("$base/inject", "$base/keymint")
                cachedHelpers = h
                return h
            }
        }
        throw Su.SuException("未找到 OhMyKeymint 模块（inject / keymint 不可执行或未安装）")
    }

    // ===== 执行封装 =====

    private suspend fun runInject(vararg args: String, maxOut: Int = 1 shl 20): String {
        val h = helpers()
        val cmdline = (listOf(Su.shq(h.inject)) + args.map { Su.shq(it) }).joinToString(" ")
        return Su.exec(cmdline, maxOut = maxOut)
    }

    private suspend fun runKeymint(vararg args: String, maxOut: Int = 1 shl 20): String {
        val h = helpers()
        val cmdline = (listOf(Su.shq(h.keymint)) + args.map { Su.shq(it) }).joinToString(" ")
        return Su.exec(cmdline, maxOut = maxOut)
    }

    // ===== 通用校验（移植自 WebUI 前端） =====

    private fun parseValue(raw: String, what: String): Any = try {
        JSONTokener(raw).nextValue()
    } catch (_: Exception) {
        throw Su.SuException("OMK 返回了无效的$what")
    }

    private fun parseObject(raw: String, what: String): JSONObject {
        val v = parseValue(raw, what)
        if (v !is JSONObject) throw Su.SuException("OMK 返回了无效的$what")
        return v
    }

    private fun parseArray(raw: String, what: String): JSONArray {
        val v = parseValue(raw, what)
        if (v !is JSONArray) throw Su.SuException("OMK 返回了无效的$what")
        return v
    }

    private fun exactKeys(o: JSONObject, expected: Set<String>, what: String) {
        val keys = HashSet<String>()
        val it = o.keys()
        while (it.hasNext()) keys.add(it.next())
        if (keys != expected) throw Su.SuException("OMK 返回了无效的$what")
    }

    /** Qw：非空、长度 ≤max、无首尾空白、无控制字符。 */
    private fun cleanString(s: String, max: Int): Boolean =
        s.isNotEmpty() && s.length <= max && s.trim() == s && !s.any { it.code < 0x20 || it.code == 0x7f }

    fun validPackage(s: String): Boolean =
        s.isNotEmpty() && s.length <= 255 && PACKAGE_RE.matches(s)

    private fun validPifProduct(s: String): Boolean = cleanString(s, 128) && PIF_PRODUCT_RE.matches(s)

    /** Cw：严格公历日期校验。 */
    fun validCalendarDate(s: String): Boolean {
        val m = Regex("^(\\d{4})-(\\d{2})-(\\d{2})$").find(s) ?: return false
        val y = m.groupValues[1].toInt()
        val mo = m.groupValues[2].toInt()
        val d = m.groupValues[3].toInt()
        if (y < 2000 || mo < 1 || mo > 12 || d < 1) return false
        val leap = (y % 400 == 0) || (y % 4 == 0 && y % 100 != 0)
        val dim = intArrayOf(31, if (leap) 29 else 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31)
        return d <= dim[mo - 1]
    }

    // ===== inject 子命令 =====

    suspend fun getScoop(): List<String> {
        val raw = runInject("--webui-get-scoop", maxOut = 1 shl 20)
        val arr = parseArray(raw, "应用选择")
        val out = LinkedHashSet<String>()
        for (i in 0 until arr.length()) {
            val p = arr.optString(i)
            if (!validPackage(p)) throw Su.SuException("OMK 返回了无效的应用选择")
            out.add(p)
        }
        return out.toList()
    }

    suspend fun setScoop(pkgs: List<String>) {
        val checked = LinkedHashSet<String>()
        for (p in pkgs) {
            if (!validPackage(p)) throw Su.SuException("无效的应用包名：$p")
            checked.add(p)
        }
        val json = JSONArray().apply { checked.forEach { put(it) } }.toString()
        runInject("--webui-set-scoop", Su.b64(json), maxOut = 1 shl 16)
    }

    suspend fun getTeeStatus(): String = runInject("--webui-get-tee-status", maxOut = 256)

    // ===== keymint 子命令 =====

    suspend fun getKeyboxState(): KeyboxState {
        val raw = runKeymint("--webui-get-keybox-state", maxOut = 256)
        val o = parseObject(raw, "密钥箱状态")
        exactKeys(o, setOf("valid", "bundled", "source", "level", "play_integrity", "revocation"), "密钥箱状态")
        val valid = o.opt("valid")
        val bundled = o.opt("bundled")
        if (valid !is Boolean || bundled !is Boolean) throw Su.SuException("OMK 返回了无效的密钥箱状态")
        val source = o.getString("source")
        val level = o.getString("level")
        val playIntegrity = o.getString("play_integrity")
        val revocation = o.getString("revocation")
        if (source !in setOf("google_hardware", "google_remote", "unknown")) throw Su.SuException("OMK 返回了无效的密钥箱状态")
        if (level !in setOf("tee", "strongbox", "unknown")) throw Su.SuException("OMK 返回了无效的密钥箱状态")
        if (playIntegrity != "not_checked") throw Su.SuException("OMK 返回了无效的密钥箱状态")
        if (revocation !in setOf("not_checked", "not_listed", "suspended", "revoked", "unknown"))
            throw Su.SuException("OMK 返回了无效的密钥箱状态")
        return KeyboxState(valid, bundled, source, level, playIntegrity, revocation)
    }

    suspend fun checkKeyboxRevocation(): String =
        runKeymint("--webui-check-keybox-revocation", maxOut = 256)

    suspend fun installKeybox(bytes: ByteArray) {
        if (bytes.size > KEYBOX_MAX_BYTES) throw Su.SuException("密钥箱文件超过 64KB 上限")
        val content = try {
            Charsets.UTF_8.newDecoder()
                .onMalformedInput(java.nio.charset.CodingErrorAction.REPORT)
                .onUnmappableCharacter(java.nio.charset.CodingErrorAction.REPORT)
                .decode(java.nio.ByteBuffer.wrap(bytes)).toString()
        } catch (_: Exception) {
            throw Su.SuException("密钥箱文件不是有效的 UTF-8 文本")
        }
        val args = mutableListOf<String>()
        args.add("--webui-install-keybox")
        content.chunked(KEYBOX_CHUNK).forEach { args.add(it) }
        runKeymint(*args.toTypedArray(), maxOut = 256)
        recordActivity("keybox_changed", "")
    }

    suspend fun getActivityLog(): List<ActivityEntry> {
        val raw = runKeymint("--webui-get-activity-log", maxOut = 16384)
        val arr = parseArray(raw, "活动日志")
        if (arr.length() > ACTIVITY_MAX) throw Su.SuException("OMK 返回了无效的活动日志")
        val out = ArrayList<ActivityEntry>(arr.length())
        for (i in 0 until arr.length()) {
            val e = arr.optJSONObject(i) ?: throw Su.SuException("OMK 返回了无效的活动日志条目")
            exactKeys(e, setOf("action", "detail", "timestamp"), "活动日志条目")
            val action = e.getString("action")
            val detail = e.getString("detail")
            val ts = e.opt("timestamp")
            if (action !in ACTIVITY_ACTIONS) throw Su.SuException("OMK 返回了无效的活动日志条目")
            if (Su.utf8len(detail) > DETAIL_MAX_BYTES || detail.any { it.code < 0x20 || it.code == 0x7f })
                throw Su.SuException("OMK 返回了无效的活动日志条目")
            if (ts !is Number || ts.toLong() <= 0 || ts.toLong() > TIMESTAMP_MAX)
                throw Su.SuException("OMK 返回了无效的活动日志条目")
            out.add(ActivityEntry(action, detail, ts.toLong()))
        }
        return out
    }

    suspend fun clearActivityLog() {
        runKeymint("--webui-clear-activity-log", maxOut = 64)
    }

    suspend fun recordActivity(action: String, detail: String) {
        if (action !in ACTIVITY_ACTIONS) throw Su.SuException("无效的活动类型：$action")
        if (Su.utf8len(detail) > DETAIL_MAX_BYTES || detail.any { it.code < 0x20 || it.code == 0x7f })
            throw Su.SuException("活动详情超出限制")
        val out = runKeymint("--webui-record-activity", action, Su.b64(detail), maxOut = 64)
        if (out != "ok") throw Su.SuException("OMK 返回了异常的活动记录结果")
    }

    suspend fun syncSecurityPatch(date: String): String {
        if (date != "auto" && !validCalendarDate(date)) throw Su.SuException("无效的安全补丁日期：$date")
        val out = runKeymint("--webui-sync-security-patch", date, maxOut = 256)
        if (out != date) throw Su.SuException("OMK 返回了异常的安全补丁结果")
        return out
    }

    suspend fun fetchSecurityBulletin(url: String): String =
        runKeymint("--webui-fetch-security-bulletin", url, maxOut = 2098176)

    // ===== PIF 指纹伪装 =====

    private fun parsePifState(raw: String): PifState {
        val o = parseObject(raw, "PIF 指纹状态")
        val enabled = o.opt("enabled")
        if (enabled !is Boolean) throw Su.SuException("OMK 返回了无效的 PIF 指纹状态")
        if (!enabled) {
            exactKeys(o, setOf("enabled"), "已停用的 PIF 指纹状态")
            return PifState(enabled = false)
        }
        exactKeys(o, setOf("enabled", "model", "product", "fingerprint", "security_patch"), "已启用的 PIF 指纹状态")
        val model = o.getString("model")
        val product = o.getString("product")
        val fingerprint = o.getString("fingerprint")
        val patch = o.getString("security_patch")
        if (!cleanString(model, 128) || !validPifProduct(product) ||
            !cleanString(fingerprint, 1024) || !cleanString(patch, 10) || !validCalendarDate(patch)
        ) throw Su.SuException("OMK 返回了无效的已启用 PIF 指纹状态")
        return PifState(true, model, product, fingerprint, patch)
    }

    suspend fun getPifFingerprintState(): PifState =
        parsePifState(runKeymint("--webui-get-pif-fingerprint-state", maxOut = 2048))

    suspend fun listPifDevices(): List<PifDevice> {
        val raw = runKeymint("--webui-list-pif-devices", maxOut = 65536)
        val arr = parseArray(raw, "PIF 设备目录")
        if (arr.length() == 0 || arr.length() > 64) throw Su.SuException("OMK 返回了无效的 PIF 设备目录")
        val out = ArrayList<PifDevice>(arr.length())
        val products = HashSet<String>()
        for (i in 0 until arr.length()) {
            val e = arr.optJSONObject(i) ?: throw Su.SuException("OMK 返回了无效的 PIF 设备")
            exactKeys(e, setOf("model", "product"), "PIF 设备")
            val model = e.getString("model")
            val product = e.getString("product")
            if (!cleanString(model, 128) || !validPifProduct(product))
                throw Su.SuException("OMK 返回了无效的 PIF 设备")
            if (!products.add(product)) throw Su.SuException("OMK 返回了重复的 PIF 设备 product")
            out.add(PifDevice(model, product))
        }
        return out
    }

    suspend fun applyPifFingerprint(product: String): PifState {
        if (!validPifProduct(product)) throw Su.SuException("无效的 PIF product")
        val st = parsePifState(runKeymint("--webui-apply-pif-fingerprint", product, maxOut = 2048))
        if (!st.enabled || st.product != product) throw Su.SuException("OMK 返回了异常的 PIF 指纹结果")
        recordActivity("pif_enabled", st.model ?: "")
        return st
    }

    suspend fun disablePifFingerprint(): PifState {
        val st = parsePifState(runKeymint("--webui-disable-pif-fingerprint", maxOut = 2048))
        if (st.enabled) throw Su.SuException("OMK 返回了异常的 PIF 指纹结果")
        recordActivity("pif_disabled", "")
        return st
    }

    // ===== Soter =====

    suspend fun getSoterBeta(): Boolean {
        val raw = runKeymint("--webui-get-soter-beta", maxOut = 256)
        val o = parseObject(raw, "Soter Beta 状态")
        exactKeys(o, setOf("enabled"), "Soter Beta 状态")
        val b = o.opt("enabled")
        if (b !is Boolean) throw Su.SuException("OMK 返回了无效的 Soter Beta 状态")
        return b
    }

    suspend fun setSoterBeta(enabled: Boolean) {
        val out = runKeymint("--webui-set-soter-beta", if (enabled) "1" else "0", maxOut = 256)
        if (out != "soter_beta_saved") throw Su.SuException("OMK 返回了异常的 Soter Beta 结果")
    }

    suspend fun getSoterHal(): SoterHal {
        val raw = runKeymint("--webui-get-soter-hal", maxOut = 16385)
        val o = parseObject(raw, "Soter HAL 配置")
        exactKeys(o, setOf("enabled", "url", "token", "device_id", "tls_insecure", "uid_map"), "Soter HAL 配置")
        val enabled = o.opt("enabled")
        val tlsInsecure = o.opt("tls_insecure")
        if (enabled !is Boolean || tlsInsecure !is Boolean) throw Su.SuException("OMK 返回了无效的 Soter HAL 配置")
        return SoterHal(enabled, o.getString("url"), o.getString("token"), o.getString("device_id"), tlsInsecure, o.getString("uid_map"))
    }

    suspend fun setSoterHal(cfg: SoterHal) {
        val o = JSONObject()
        o.put("enabled", cfg.enabled)
        o.put("url", cfg.url)
        o.put("token", cfg.token)
        o.put("device_id", cfg.deviceId)
        o.put("tls_insecure", cfg.tlsInsecure)
        o.put("uid_map", cfg.uidMap)
        val json = o.toString()
        if (Su.utf8len(json) > SOTER_HAL_JSON_MAX) throw Su.SuException("Soter HAL 配置超出字节限制")
        val out = runKeymint("--webui-set-soter-hal-base64", Su.b64(json), maxOut = 256)
        if (out != "soter_hal_saved") throw Su.SuException("OMK 返回了异常的 Soter HAL 结果")
        val rb = getSoterHal()
        if (rb != cfg) throw Su.SuException("Soter HAL 配置保存后回读不一致")
    }

    // ===== 首页附加状态 =====

    suspend fun currentSecurityPatch(): String =
        Su.exec("/system/bin/getprop ro.build.version.security_patch", maxOut = 64)

    suspend fun moduleRunning(): Boolean = try {
        Su.exec(
            "[ -d /data/adb/modules/oh_my_keymint ] && [ ! -f /data/adb/modules/oh_my_keymint/disable ] && [ ! -f /data/adb/modules/oh_my_keymint/remove ]",
            maxOut = 64,
        )
        true
    } catch (_: Exception) {
        false
    }
}
