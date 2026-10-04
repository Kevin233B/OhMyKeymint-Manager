package com.kevin233.omkmanager.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.kevin233.omkmanager.core.Omk
import com.kevin233.omkmanager.core.Str
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import top.yukonga.miuix.kmp.basic.BasicComponent
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.Text

@Composable
fun KeyboxScreen(toast: (String) -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var busy by remember { mutableStateOf(false) }

    var attempt by remember { mutableStateOf(0) }
    val state = produceState<Result<Omk.KeyboxState>?>(initialValue = null, attempt) {
        value = runCatching { Omk.getKeyboxState() }
    }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        if (busy) return@rememberLauncherForActivityResult
        busy = true
        scope.launch {
            try {
                val name = uri.lastPathSegment?.substringAfterLast('/') ?: uri.toString()
                val bytes = withContext(Dispatchers.IO) {
                    context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                        ?: throw IllegalStateException("无法读取文件")
                }
                if (!name.endsWith(".xml", ignoreCase = true)) {
                    toast(Str.t("prompt_keybox_xml_required"))
                    return@launch
                }
                Omk.installKeybox(bytes)
                toast(Str.t("prompt_keybox_replaced"))
                attempt++
            } catch (e: Exception) {
                toast(Str.t("prompt_keybox_replace_error_fmt").format(e.message ?: "?"))
            } finally {
                busy = false
            }
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 12.dp, end = 12.dp, bottom = 24.dp),
    ) {
        item { SmallTitle(text = Str.t("tools_keybox_desc"), modifier = Modifier.padding(top = 16.dp)) }
        item {
            Card(modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                Button(
                    onClick = { if (!busy) picker.launch(arrayOf("*/*")) },
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                ) {
                    Text(Str.t("replace_keybox_pick"))
                }
            }
        }
        val res = state.value
        if (res != null && res.isSuccess) {
            val kb = res.getOrThrow()
            item { SmallTitle(text = Str.t("pif_current_config")) }
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    BasicComponent(
                        title = Str.t("home_keybox"),
                        summary = if (kb.valid) {
                            "${keyboxSourceText(kb.source, kb.bundled)} · ${keyboxLevelText(kb.level)}"
                        } else {
                            Str.t("home_keybox_invalid")
                        },
                    )
                    BasicComponent(
                        title = Str.t("home_keybox_revocation"),
                        summary = revocationText(kb.revocation),
                    )
                }
            }
        }
    }
}
