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
import com.kevin233.omkmanager.core.Omk
import com.kevin233.omkmanager.core.Str
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.BasicComponent
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.preference.SwitchPreference

@Composable
fun SoterBetaScreen(toast: (String) -> Unit) {
    val scope = rememberCoroutineScope()
    var busy by remember { mutableStateOf(false) }

    var attempt by remember { mutableStateOf(0) }
    val state = produceState<Result<Boolean>?>(initialValue = null, attempt) {
        value = runCatching { Omk.getSoterBeta() }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 12.dp, end = 12.dp, bottom = 24.dp),
    ) {
        item { SmallTitle(text = Str.t("soter_beta_warning"), modifier = Modifier.padding(top = 16.dp)) }
        item { SmallTitle(text = Str.t("soter_beta_reboot")) }

        val res = state.value
        when {
            res == null -> item { SmallTitle(text = Str.t("loading")) }
            res.isFailure -> item {
                SmallTitle(text = Str.t("error_load_fmt").format(res.exceptionOrNull()?.message ?: "?"))
            }
            else -> {
                val enabled = res.getOrThrow()
                item {
                    Card(modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                        SwitchPreference(
                            title = Str.t("soter_beta_enabled"),
                            summary = Str.t("soter_beta_enabled_desc"),
                            checked = enabled,
                            onCheckedChange = {
                                if (busy) return@SwitchPreference
                                busy = true
                                scope.launch {
                                    try {
                                        Omk.setSoterBeta(it)
                                        toast(Str.t("soter_beta_saved"))
                                        attempt++
                                    } catch (e: Exception) {
                                        toast(Str.t("operation_failed_fmt").format(e.message ?: "?"))
                                    } finally {
                                        busy = false
                                    }
                                }
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SoterHalScreen(toast: (String) -> Unit) {
    val scope = rememberCoroutineScope()
    var busy by remember { mutableStateOf(false) }

    var attempt by remember { mutableStateOf(0) }
    val state = produceState<Result<Omk.SoterHal>?>(initialValue = null, attempt) {
        value = runCatching { Omk.getSoterHal() }
    }

    var enabled by remember { mutableStateOf(false) }
    var url by remember { mutableStateOf("") }
    var token by remember { mutableStateOf("") }
    var deviceId by remember { mutableStateOf("") }
    var tlsInsecure by remember { mutableStateOf(false) }
    var uidMap by remember { mutableStateOf("") }
    var initialized by remember { mutableStateOf(false) }

    androidx.compose.runtime.LaunchedEffect(state.value) {
        val cfg = state.value?.getOrNull()
        if (cfg != null && !initialized) {
            enabled = cfg.enabled
            url = cfg.url
            token = cfg.token
            deviceId = cfg.deviceId
            tlsInsecure = cfg.tlsInsecure
            uidMap = cfg.uidMap
            initialized = true
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 12.dp, end = 12.dp, bottom = 24.dp),
    ) {
        item { SmallTitle(text = Str.t("tools_wechat_soter_desc"), modifier = Modifier.padding(top = 16.dp)) }

        val res = state.value
        when {
            res == null -> item { SmallTitle(text = Str.t("loading")) }
            res.isFailure -> item {
                SmallTitle(text = Str.t("error_load_fmt").format(res.exceptionOrNull()?.message ?: "?"))
            }
            else -> {
                item {
                    Card(modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                        SwitchPreference(
                            title = Str.t("soter_hal_enabled"),
                            checked = enabled,
                            onCheckedChange = { enabled = it },
                        )
                    }
                }
                item { SmallTitle(text = Str.t("soter_hal_url")) }
                item {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        TextField(
                            value = url,
                            onValueChange = { url = it },
                            label = Str.t("soter_hal_url"),
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                        )
                    }
                }
                item { SmallTitle(text = Str.t("soter_hal_token")) }
                item {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        TextField(
                            value = token,
                            onValueChange = { token = it },
                            label = Str.t("soter_hal_token"),
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                        )
                    }
                }
                item { SmallTitle(text = Str.t("soter_hal_device_id")) }
                item {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        TextField(
                            value = deviceId,
                            onValueChange = { deviceId = it },
                            label = Str.t("soter_hal_device_id"),
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                        )
                    }
                }
                item { SmallTitle(text = Str.t("soter_hal_uid_map")) }
                item {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        TextField(
                            value = uidMap,
                            onValueChange = { uidMap = it },
                            label = Str.t("soter_hal_uid_map"),
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                        )
                    }
                }
                item {
                    Card(modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                        SwitchPreference(
                            title = Str.t("soter_hal_tls_insecure"),
                            checked = tlsInsecure,
                            onCheckedChange = { tlsInsecure = it },
                        )
                    }
                }
                item {
                    Card(modifier = Modifier.fillMaxWidth().padding(top = 12.dp)) {
                        Button(
                            onClick = {
                                if (busy) return@Button
                                busy = true
                                scope.launch {
                                    try {
                                        Omk.setSoterHal(
                                            Omk.SoterHal(enabled, url, token, deviceId, tlsInsecure, uidMap)
                                        )
                                        toast(Str.t("soter_hal_saved"))
                                        attempt++
                                    } catch (e: Exception) {
                                        val msg = e.message ?: "?"
                                        val shown = when {
                                            msg.contains("超出字节") -> Str.t("soter_hal_too_large")
                                            msg.contains("回读") -> Str.t("soter_hal_mismatch")
                                            else -> msg
                                        }
                                        toast(Str.t("operation_failed_fmt").format(shown))
                                    } finally {
                                        busy = false
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                        ) {
                            Text(if (busy) Str.t("loading") else Str.t("save"))
                        }
                    }
                }
            }
        }
    }
}
