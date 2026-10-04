package com.kevin233.omkmanager.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import com.kevin233.omkmanager.core.Omk
import com.kevin233.omkmanager.core.Str
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.BasicComponent
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.NavigationBar
import top.yukonga.miuix.kmp.basic.NavigationBarItem
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Home
import top.yukonga.miuix.kmp.icon.extended.Settings
import top.yukonga.miuix.kmp.icon.extended.Tasks

@Composable
fun MainBottomBar(selected: Int, onSelect: (Int) -> Unit) {
    NavigationBar {
        NavigationBarItem(
            selected = selected == 0,
            onClick = { onSelect(0) },
            icon = MiuixIcons.Home,
            label = Str.t("tab_home"),
        )
        NavigationBarItem(
            selected = selected == 1,
            onClick = { onSelect(1) },
            icon = MiuixIcons.Tasks,
            label = Str.t("tab_tools"),
        )
        NavigationBarItem(
            selected = selected == 2,
            onClick = { onSelect(2) },
            icon = MiuixIcons.Settings,
            label = Str.t("tab_settings"),
        )
    }
}

private data class HomeData(
    val moduleRunning: Boolean,
    val securityPatch: String,
    val keybox: Omk.KeyboxState,
    val tee: String,
    val pif: Omk.PifState,
    val scoopCount: Int,
    val activities: List<Omk.ActivityEntry>,
)

fun keyboxSourceText(source: String, bundled: Boolean): String = when (source) {
    "google_hardware" -> Str.t("home_keybox_hardware")
    "google_remote" -> Str.t("home_keybox_remote")
    else -> if (bundled) Str.t("home_keybox_bundled") else Str.t("home_keybox_custom")
}

fun keyboxLevelText(level: String): String = when (level) {
    "tee" -> Str.t("home_keybox_tee")
    "strongbox" -> Str.t("home_keybox_strongbox")
    else -> Str.t("home_keybox_level_unknown")
}

fun revocationText(revocation: String): String = when (revocation) {
    "not_checked" -> Str.t("home_keybox_revocation_not_checked")
    "not_listed" -> Str.t("home_keybox_revocation_not_revoked")
    "suspended" -> Str.t("home_keybox_revocation_suspended")
    "revoked" -> Str.t("home_keybox_revocation_revoked")
    else -> Str.t("home_keybox_status_unknown")
}

fun activityActionText(action: String): String = when (action) {
    "targets_saved" -> Str.t("activity_targets_saved")
    "keybox_changed" -> Str.t("activity_keybox_changed")
    "widevine_installed" -> Str.t("activity_widevine_installed")
    "security_patch_synced" -> Str.t("activity_security_patch_synced")
    "security_patch_restored" -> Str.t("activity_security_patch_restored")
    "pif_enabled" -> Str.t("activity_pif_enabled")
    "pif_disabled" -> Str.t("activity_pif_disabled")
    "adb_disabler_changed" -> Str.t("activity_adb_disabler_changed")
    else -> action
}

@Composable
fun HomeScreen(
    openTargets: () -> Unit,
    openPif: () -> Unit,
    toast: (String) -> Unit,
) {
    var attempt by remember { mutableStateOf(0) }
    val scope = rememberCoroutineScope()
    val state = produceState<Result<HomeData>?>(initialValue = null, attempt) {
        value = runCatching {
            HomeData(
                moduleRunning = Omk.moduleRunning(),
                securityPatch = Omk.currentSecurityPatch(),
                keybox = Omk.getKeyboxState(),
                tee = Omk.getTeeStatus(),
                pif = Omk.getPifFingerprintState(),
                scoopCount = Omk.getScoop().size,
                activities = Omk.getActivityLog(),
            )
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(
            start = 12.dp, end = 12.dp, bottom = 24.dp,
        ),
    ) {
        val result = state.value
        when {
            result == null -> item {
                SmallTitle(text = Str.t("loading"), modifier = Modifier.padding(top = 16.dp))
            }
            result.isFailure -> item {
                val err = result.exceptionOrNull()?.message ?: "?"
                SmallTitle(text = Str.t("error_load_fmt").format(err), modifier = Modifier.padding(top = 16.dp))
                Button(onClick = { attempt++ }, modifier = Modifier.padding(top = 8.dp)) {
                    Text(Str.t("retry"))
                }
            }
            else -> {
                val data = result.getOrThrow()
                item { SmallTitle(text = Str.t("home_status"), modifier = Modifier.padding(top = 16.dp)) }
                item {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        BasicComponent(
                            title = Str.t("home_module_status"),
                            summary = if (data.moduleRunning) Str.t("home_status_running") else Str.t("home_status_error"),
                        )
                        BasicComponent(
                            title = Str.t("home_security_patch"),
                            summary = data.securityPatch,
                        )
                        BasicComponent(
                            title = Str.t("home_tee_status"),
                            summary = if (data.tee == "normal") Str.t("home_tee_normal") else data.tee,
                        )
                        BasicComponent(
                            title = Str.t("home_keybox"),
                            summary = if (data.keybox.valid) {
                                "${keyboxSourceText(data.keybox.source, data.keybox.bundled)} · ${keyboxLevelText(data.keybox.level)}"
                            } else {
                                Str.t("home_keybox_invalid")
                            },
                        )
                        BasicComponent(
                            title = Str.t("home_keybox_revocation"),
                            summary = revocationText(data.keybox.revocation),
                            onClick = {
                                scope.launch {
                                    try {
                                        val r = Omk.checkKeyboxRevocation()
                                        toast(revocationText(r))
                                    } catch (e: Exception) {
                                        toast(Str.t("operation_failed_fmt").format(e.message ?: "?"))
                                    }
                                }
                            },
                        )
                        BasicComponent(
                            title = Str.t("home_spoofed_device"),
                            summary = data.pif.model ?: Str.t("pif_disabled"),
                            onClick = openPif,
                        )
                        BasicComponent(
                            title = Str.t("home_selected_apps_fmt").format(data.scoopCount),
                            onClick = openTargets,
                        )
                    }
                }

                item { SmallTitle(text = Str.t("home_recent_activity")) }
                item {
                    var showAll by remember { mutableStateOf(false) }
                    val clipboard = LocalClipboardManager.current
                    val fmt = remember {
                        java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault())
                    }
                    val shown = if (showAll) data.activities else data.activities.take(3)
                    Card(modifier = Modifier.fillMaxWidth()) {
                        if (data.activities.isEmpty()) {
                            BasicComponent(title = Str.t("home_activity_empty"))
                        } else {
                            for (a in shown) {
                                BasicComponent(
                                    title = activityActionText(a.action),
                                    summary = listOf(
                                        a.detail,
                                        fmt.format(java.util.Date(a.timestampMillis)),
                                    ).filter { it.isNotBlank() }.joinToString(" · "),
                                )
                            }
                        }
                        if (data.activities.size > 3) {
                            BasicComponent(
                                title = if (showAll) Str.t("home_activity_show_less")
                                else Str.t("home_activity_show_all_fmt").format(data.activities.size),
                                onClick = { showAll = !showAll },
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Button(
                                onClick = {
                                    val text = data.activities.joinToString("\n") { a ->
                                        "${activityActionText(a.action)}\t${a.detail}\t${fmt.format(java.util.Date(a.timestampMillis))}"
                                    }
                                    clipboard.setText(AnnotatedString(text))
                                    toast(Str.t("home_activity_copied"))
                                },
                                modifier = Modifier.weight(1f),
                            ) {
                                Text(Str.t("home_copy_activity"))
                            }
                            Button(
                                onClick = {
                                    scope.launch {
                                        try {
                                            Omk.clearActivityLog()
                                            toast(Str.t("home_activity_cleared"))
                                            attempt++
                                        } catch (e: Exception) {
                                            toast(Str.t("operation_failed_fmt").format(e.message ?: "?"))
                                        }
                                    }
                                },
                                modifier = Modifier.weight(1f),
                            ) {
                                Text(Str.t("home_activity_clear"))
                            }
                        }
                    }
                }
            }
        }
    }
}
