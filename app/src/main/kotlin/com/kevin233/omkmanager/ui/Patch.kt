package com.kevin233.omkmanager.ui

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.kevin233.omkmanager.core.Bulletin
import com.kevin233.omkmanager.core.Omk
import com.kevin233.omkmanager.core.Str
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.BasicComponent
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.Text

@Composable
fun PatchScreen(toast: (String) -> Unit) {
    val scope = rememberCoroutineScope()
    var busy by remember { mutableStateOf(false) }

    var attempt by remember { mutableStateOf(0) }
    val state = produceState<Result<String>?>(initialValue = null, attempt) {
        value = runCatching { Omk.currentSecurityPatch() }
    }

    fun sync() {
        if (busy) return
        busy = true
        scope.launch {
            try {
                val date = try {
                    val html = Omk.fetchSecurityBulletin(Bulletin.PRIMARY_URL)
                    Bulletin.parse(html)
                } catch (_: Exception) {
                    val html = Omk.fetchSecurityBulletin(Bulletin.FALLBACK_URL)
                    Bulletin.parse(html)
                }
                Omk.syncSecurityPatch(date)
                Omk.recordActivity("security_patch_synced", date)
                toast(Str.t("prompt_security_patch_sync_complete_fmt").format(date))
                attempt++
            } catch (e: Exception) {
                toast(Str.t("prompt_security_patch_sync_error_fmt").format(e.message ?: "?"))
            } finally {
                busy = false
            }
        }
    }

    fun restore() {
        if (busy) return
        busy = true
        scope.launch {
            try {
                Omk.syncSecurityPatch("auto")
                Omk.recordActivity("security_patch_restored", "")
                toast(Str.t("prompt_security_patch_restored_default"))
                attempt++
            } catch (e: Exception) {
                toast(Str.t("prompt_security_patch_restore_error_fmt").format(e.message ?: "?"))
            } finally {
                busy = false
            }
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 12.dp, end = 12.dp, bottom = 24.dp),
    ) {
        item { SmallTitle(text = Str.t("tools_sync_patch_desc"), modifier = Modifier.padding(top = 16.dp)) }
        item {
            Card(modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                val res = state.value
                when {
                    res == null -> BasicComponent(title = Str.t("loading"))
                    res.isSuccess -> BasicComponent(
                        title = Str.t("current_security_patch_fmt").format(res.getOrDefault("")),
                    )
                    else -> BasicComponent(
                        title = Str.t("error_load_fmt").format(res.exceptionOrNull()?.message ?: "?"),
                    )
                }
            }
        }
        item {
            Card(modifier = Modifier.fillMaxWidth().padding(top = 12.dp)) {
                Button(
                    onClick = { sync() },
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                ) {
                    Text(if (busy) Str.t("patch_syncing") else Str.t("patch_sync"))
                }
            }
        }
        item { SmallTitle(text = Str.t("tools_restore_patch_desc")) }
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Button(
                    onClick = { restore() },
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                ) {
                    Text(Str.t("patch_restore"))
                }
            }
        }
    }
}
