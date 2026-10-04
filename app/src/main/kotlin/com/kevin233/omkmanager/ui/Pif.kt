package com.kevin233.omkmanager.ui

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import top.yukonga.miuix.kmp.basic.DropdownItem
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.preference.WindowSpinnerPreference

@Composable
fun PifScreen(toast: (String) -> Unit) {
    val scope = rememberCoroutineScope()
    var busy by remember { mutableStateOf(false) }
    var selectedIdx by remember { mutableIntStateOf(0) }

    var attempt by remember { mutableStateOf(0) }
    val state = produceState<Pair<Result<Omk.PifState>, Result<List<Omk.PifDevice>>>?>(null, attempt) {
        value = runCatching { Omk.getPifFingerprintState() } to
            runCatching { Omk.listPifDevices() }
    }

    fun applyProduct(product: String) {
        if (busy) return
        busy = true
        scope.launch {
            try {
                val st = Omk.applyPifFingerprint(product)
                toast(Str.t("prompt_pif_applied_fmt").format(st.model ?: product))
                attempt++
            } catch (e: Exception) {
                toast(Str.t("prompt_pif_apply_error_fmt").format(e.message ?: "?"))
            } finally {
                busy = false
            }
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 12.dp, end = 12.dp, bottom = 24.dp),
    ) {
        item { SmallTitle(text = Str.t("pif_zygisk_next_required"), modifier = Modifier.padding(top = 16.dp)) }

        val res = state.value
        when {
            res == null -> item { SmallTitle(text = Str.t("loading")) }
            res.first.isFailure -> item {
                SmallTitle(text = Str.t("error_load_fmt").format(res.first.exceptionOrNull()?.message ?: "?"))
            }
            else -> {
                val pif = res.first.getOrThrow()
                item { SmallTitle(text = Str.t("pif_current_config")) }
                item {
                    Card(modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                        if (pif.enabled) {
                            BasicComponent(title = pif.model ?: "", summary = pif.product ?: "")
                            BasicComponent(
                                title = Str.t("pif_security_patch_fmt").format(pif.securityPatch ?: ""),
                                summary = pif.fingerprint ?: "",
                            )
                        } else {
                            BasicComponent(title = Str.t("pif_disabled"))
                        }
                    }
                }

                val devicesRes = res.second
                if (devicesRes != null && devicesRes.isSuccess) {
                    val devices = devicesRes.getOrThrow()
                    if (devices.isNotEmpty()) {
                        item { SmallTitle(text = Str.t("pif_choose_device")) }
                        item {
                            Card(modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                                WindowSpinnerPreference(
                                    title = Str.t("pif_choose_device"),
                                    items = devices.map { d ->
                                        DropdownItem(text = d.model, summary = d.product)
                                    },
                                    selectedIndex = selectedIdx,
                                    onSelectedIndexChange = { selectedIdx = it },
                                )
                            }
                        }
                        item {
                            Card(modifier = Modifier.fillMaxWidth().padding(top = 12.dp)) {
                                Button(
                                    onClick = {
                                        val d = devices.getOrNull(selectedIdx) ?: devices.first()
                                        applyProduct(d.product)
                                    },
                                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                                ) {
                                    Text(if (busy) Str.t("loading") else Str.t("pif_apply"))
                                }
                            }
                        }
                        item {
                            Card(modifier = Modifier.fillMaxWidth()) {
                                Button(
                                    onClick = {
                                        val d = devices.random()
                                        applyProduct(d.product)
                                    },
                                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                                ) {
                                    Text(Str.t("pif_random_device"))
                                }
                            }
                        }
                    }
                }

                if (pif.enabled) {
                    item { SmallTitle(text = Str.t("tools_pif_desc")) }
                    item {
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Button(
                                onClick = {
                                    if (busy) return@Button
                                    busy = true
                                    scope.launch {
                                        try {
                                            Omk.disablePifFingerprint()
                                            toast(Str.t("prompt_pif_disabled"))
                                            attempt++
                                        } catch (e: Exception) {
                                            toast(Str.t("prompt_pif_apply_error_fmt").format(e.message ?: "?"))
                                        } finally {
                                            busy = false
                                        }
                                    }
                                },
                                modifier = Modifier.fillMaxWidth().padding(16.dp),
                            ) {
                                Text(Str.t("pif_disable"))
                            }
                        }
                    }
                }
            }
        }
    }
}
