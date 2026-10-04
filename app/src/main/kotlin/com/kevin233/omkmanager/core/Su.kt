package com.kevin233.omkmanager.core

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import java.util.Base64
import java.util.concurrent.TimeUnit
import kotlin.math.min

/**
 * Root 命令执行层。
 *
 * 与 WebUI 传输语义完全对齐：
 *  - 每条命令通过 `su -c <cmd>` 执行，参数按 POSIX 单引号规则转义（等价 WebUI 的 oT）；
 *  - 输出为 UTF-8，调用侧负责 trim；
 *  - 退出码非 0 时抛错，stderr 优先作为错误信息。
 */
object Su {

    class SuException(message: String) : Exception(message)

    /** POSIX 单引号转义（与 WebUI 的 oT 一字不差）。 */
    fun shq(s: String): String = "'" + s.replace("'", "'\\''") + "'"

    /** 标准 base64（与 btoa 一致，含 padding）。 */
    fun b64(s: String): String = Base64.getEncoder().encodeToString(s.toByteArray(Charsets.UTF_8))

    fun utf8len(s: String): Int = s.toByteArray(Charsets.UTF_8).size

    /** 执行 root 命令，返回 trim 后的 stdout。失败抛 [SuException]。 */
    suspend fun exec(cmd: String, maxOut: Int = 1 shl 20, timeoutMs: Long = 120_000): String =
        withContext(Dispatchers.IO) {
            val proc = try {
                ProcessBuilder("su", "-c", cmd).start()
            } catch (_: IOException) {
                throw SuException("未获取 root 权限（请在 KernelSU / APatch 中授权本应用）")
            }
            try {
                val out = StringBuilder()
                val err = StringBuilder()
                val tOut = Thread {
                    val s = proc.inputStream.readCapped(maxOut)
                    synchronized(out) { out.append(s) }
                }
                val tErr = Thread {
                    val s = proc.errorStream.readCapped(64 * 1024)
                    synchronized(err) { err.append(s) }
                }
                tOut.start()
                tErr.start()
                val finished = proc.waitFor(timeoutMs, TimeUnit.MILLISECONDS)
                if (!finished) {
                    proc.destroyForcibly()
                    throw SuException("root 命令执行超时")
                }
                tOut.join(2000)
                tErr.join(2000)
                val code = proc.exitValue()
                if (code != 0) {
                    val msg = err.toString().trim()
                    throw SuException(msg.ifEmpty { "root 命令退出码 $code" })
                }
                out.toString().trim()
            } finally {
                proc.destroy()
            }
        }

    private fun InputStream.readCapped(cap: Int): String {
        val bos = ByteArrayOutputStream()
        try {
            val buf = ByteArray(8192)
            while (true) {
                val n = read(buf)
                if (n < 0) break
                if (bos.size() < cap) {
                    bos.write(buf, 0, min(n, cap - bos.size()))
                }
            }
        } catch (_: Exception) {
        }
        return bos.toString("UTF-8")
    }
}
