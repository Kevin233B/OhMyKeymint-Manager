package com.kevin233.omkmanager.ui

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.kevin233.omkmanager.core.Omk
import com.kevin233.omkmanager.core.Str
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.preference.ArrowPreference

@Composable
fun ToolsScreen(open: (Page) -> Unit) {
    val summaries = produceState<Pair<Result<Int>?, Result<Omk.PifState>?>?>(initialValue = null) {
        val scoop = runCatching { Omk.getScoop().size }
        val pif = runCatching { Omk.getPifFingerprintState() }
        value = scoop to pif
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 12.dp, end = 12.dp, bottom = 24.dp),
    ) {
        item { SmallTitle(text = Str.t("tools_title"), modifier = Modifier.padding(top = 16.dp)) }
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                ArrowPreference(
                    title = Str.t("tools_app_management"),
                    summary = summaries.value?.first?.getOrNull()?.let {
                        Str.t("home_selected_apps_fmt").format(it)
                    } ?: Str.t("loading"),
                    onClick = { open(Page.Targets) },
                )
                ArrowPreference(
                    title = Str.t("replace_keybox_title"),
                    summary = Str.t("tools_keybox_desc"),
                    onClick = { open(Page.Keybox) },
                )
                ArrowPreference(
                    title = Str.t("tools_security_patch"),
                    summary = Str.t("tools_sync_patch_desc"),
                    onClick = { open(Page.Patch) },
                )
                ArrowPreference(
                    title = Str.t("tools_fingerprint_spoofing"),
                    summary = summaries.value?.second?.getOrNull()?.let {
                        if (it.enabled) it.model ?: "" else Str.t("pif_disabled")
                    } ?: Str.t("loading"),
                    onClick = { open(Page.Pif) },
                )
                ArrowPreference(
                    title = Str.t("tools_soter_beta"),
                    summary = Str.t("tools_soter_beta_desc"),
                    onClick = { open(Page.SoterBeta) },
                )
                ArrowPreference(
                    title = Str.t("tools_wechat_soter"),
                    summary = Str.t("tools_wechat_soter_desc"),
                    onClick = { open(Page.SoterHal) },
                )
            }
        }
    }
}
