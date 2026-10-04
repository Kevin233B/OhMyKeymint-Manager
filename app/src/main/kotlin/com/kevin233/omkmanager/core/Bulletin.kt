package com.kevin233.omkmanager.core

import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import java.net.URI

/**
 * Android 安全公告（ASB）页面解析。
 *
 * 逻辑与 WebUI 前端逐行对齐：
 *  - 锚点 `#bulletins`，向后续兄弟节点找第一张 table（途中遇 H1/H2 即止）；
 *  - 表头列匹配 “security patch level”（含中文变体）；
 *  - 行内链接 origin 必须属于 source.android.com / source.android.google.cn，
 *    路径匹配 ASB 公告格式；单元格内提取严格公历日期，取不晚于今天的最大值。
 */
object Bulletin {

    const val PRIMARY_URL = "https://source.android.com/docs/security/bulletin/asb-overview"
    const val FALLBACK_URL = "https://source.android.google.cn/docs/security/bulletin/asb-overview?hl=zh-cn"

    private const val HTML_LIMIT = 2097152

    private val ALLOWED_ORIGINS = setOf(
        "https://source.android.com",
        "https://source.android.google.cn",
    )

    private val PATH_RE = Regex(
        "^/docs/security/bulletin/(?:20\\d{2}/)?20\\d{2}-(0[1-9]|1[0-2])-(0[1-9]|[12]\\d|3[01])([/?#]|$)"
    )

    private val DATE_RE = Regex("\\b(\\d{4})-(\\d{2})-(\\d{2})\\b")

    private val HEADER_VARIANTS = listOf(
        "securitypatchlevel",
        "安全补丁级别",
        "安全補丁級別",
        "安全性修補程式等級",
    )

    private fun headerMatches(text: String): Boolean {
        val norm = text.replace(Regex("\\s+"), "").lowercase()
        return HEADER_VARIANTS.any { norm.contains(it) }
    }

    private fun linkQualifies(href: String): Boolean {
        if (href.isBlank()) return false
        val u = try {
            URI(href)
        } catch (_: Exception) {
            return false
        }
        if (u.rawUserInfo != null) return false
        val scheme = u.scheme?.lowercase() ?: return false
        val host = u.host?.lowercase() ?: return false
        if (scheme != "https") return false
        if (u.port != -1) return false
        val origin = "$scheme://$host"
        if (origin !in ALLOWED_ORIGINS) return false
        val path = u.rawPath ?: ""
        return PATH_RE.matches(path)
    }

    private fun todayStr(): String {
        val cal = java.util.Calendar.getInstance()
        return "%04d-%02d-%02d".format(
            cal.get(java.util.Calendar.YEAR),
            cal.get(java.util.Calendar.MONTH) + 1,
            cal.get(java.util.Calendar.DAY_OF_MONTH),
        )
    }

    /** 解析公告 HTML，返回最新安全补丁级别日期（yyyy-MM-dd），失败抛 [Su.SuException]。 */
    fun parse(html: String): String {
        if (html.length > HTML_LIMIT) throw Su.SuException("公告页面超过 2MB 限制")
        val doc = Jsoup.parse(html, "https://source.android.com")

        val anchor = doc.selectFirst("#bulletins")
            ?: throw Su.SuException("公告页面缺少 #bulletins 锚点")

        var table: Element? = null
        var sibling = anchor.nextElementSibling()
        while (sibling != null) {
            val tag = sibling.tagName().lowercase()
            if (tag == "table") {
                table = sibling
                break
            }
            if (tag == "h1" || tag == "h2") break
            sibling.selectFirst("table")?.let {
                table = it
                break
            }
            sibling = sibling.nextElementSibling()
        }
        val t = table ?: throw Su.SuException("公告页面未找到公告表格")

        val headerRow = t.selectFirst("thead tr") ?: t.selectFirst("tr")
            ?: throw Su.SuException("公告表格缺少表头行")
        val headers = headerRow.children()
        var col = -1
        for ((i, h) in headers.withIndex()) {
            if (headerMatches(h.text())) {
                col = i
                break
            }
        }
        if (col < 0) throw Su.SuException("公告表格缺少“安全补丁级别”列")

        val today = todayStr()
        val dates = sortedSetOf<String>()
        for (row in t.select("tr")) {
            val links = row.select("a")
            if (links.none { linkQualifies(it.absUrl("href")) }) continue
            val cells = row.children()
            if (cells.size <= col) continue
            val text = cells[col].text()
            for (m in DATE_RE.findAll(text)) {
                val d = "${m.groupValues[1]}-${m.groupValues[2]}-${m.groupValues[3]}"
                if (Omk.validCalendarDate(d) && d <= today) dates.add(d)
            }
        }
        return dates.lastOrNull() ?: throw Su.SuException("公告表格中没有找到有效的安全补丁日期")
    }
}
