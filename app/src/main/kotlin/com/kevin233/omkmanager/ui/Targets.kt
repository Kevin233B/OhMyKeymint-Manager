package com.kevin233.omkmanager.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.kevin233.omkmanager.core.AppList
import com.kevin233.omkmanager.core.AppEntry
import com.kevin233.omkmanager.core.Omk
import com.kevin233.omkmanager.core.Str
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.preference.CheckboxPreference

@Composable
fun TargetsScreen(
    toast: (String) -> Unit,
    onDone: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // 全量应用列表（无需 root）
    val apps = remember { AppList.installed(context) }

    // 当前 scoop（root）
    var attempt by remember { mutableStateOf(0) }
    val scoopState = produceState<Result<Set<String>>?>(initialValue = null, attempt) {
        value = runCatching { Omk.getScoop().toSet() }
    }

    var selected by remember { mutableStateOf<Set<String>?>(null) }
    var search by remember { mutableStateOf("") }
    var filter by remember { mutableStateOf(0) }
    var saving by remember { mutableStateOf(false) }
    var recommending by remember { mutableStateOf(false) }

    LaunchedEffect(scoopState.value) {
        scoopState.value?.getOrNull()?.let { selected = it }
    }

    val sel = selected
    if (sel == null) {
        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        ) {
            when {
                scoopState.value == null -> Text(Str.t("loading"))
                scoopState.value!!.isFailure -> {
                    Text(Str.t("error_load_fmt").format(scoopState.value!!.exceptionOrNull()?.message ?: "?"))
                    Button(onClick = { attempt++ }, modifier = Modifier.padding(top = 8.dp)) {
                        Text(Str.t("retry"))
                    }
                }
            }
        }
        return
    }

    val filtered = remember(sel, filter, search) {
        apps.filter { a ->
            val matchesSearch = search.isBlank() ||
                a.label.contains(search, ignoreCase = true) ||
                a.pkg.contains(search, ignoreCase = true)
            val matchesFilter = when (filter) {
                1 -> a.pkg in sel
                2 -> a.pkg !in sel
                else -> true
            }
            matchesSearch && matchesFilter
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 12.dp, end = 12.dp, bottom = 24.dp),
    ) {
        item {
            TextField(
                value = search,
                onValueChange = { search = it },
                label = Str.t("targets_search_hint"),
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            )
        }
        item {
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                TextButton(text = Str.t("filter_all"), onClick = { filter = 0 })
                TextButton(text = Str.t("filter_selected"), onClick = { filter = 1 })
                TextButton(text = Str.t("filter_unselected"), onClick = { filter = 2 })
            }
        }
        item { SmallTitle(text = Str.t("menu_select_recommended_desc")) }
        item {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Button(
                    onClick = {
                        if (recommending) return@Button
                        recommending = true
                        scope.launch {
                            try {
                                val rec = AppList.recommend(context, apps)
                                selected = sel + rec
                                toast(Str.t("menu_select_all"))
                            } catch (e: Exception) {
                                toast(Str.t("operation_failed_fmt").format(e.message ?: "?"))
                            } finally {
                                recommending = false
                            }
                        }
                    },
                    modifier = Modifier.weight(1f),
                ) {
                    Text(Str.t("menu_select_all"))
                }
                Button(
                    onClick = {
                        if (saving) return@Button
                        saving = true
                        scope.launch {
                            try {
                                Omk.setScoop(sel.toList())
                                Omk.recordActivity("targets_saved", sel.size.toString())
                                toast(Str.t("prompt_saved_target"))
                                onDone()
                            } catch (e: Exception) {
                                toast(Str.t("prompt_save_error_fmt").format(e.message ?: "?"))
                            } finally {
                                saving = false
                            }
                        }
                    },
                    modifier = Modifier.weight(1f),
                ) {
                    Text(Str.t("save"))
                }
            }
        }
        item { SmallTitle(text = "${filtered.size}") }
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                if (filtered.isEmpty()) {
                    top.yukonga.miuix.kmp.basic.BasicComponent(
                        title = Str.t("app_targets_empty"),
                        summary = Str.t("home_activity_events_fmt").format(sel.size),
                    )
                }
            }
        }
        items(filtered, key = { it.pkg }) { a: AppEntry ->
            Card(modifier = Modifier.fillMaxWidth().padding(top = 6.dp)) {
                CheckboxPreference(
                    title = a.label,
                    summary = a.pkg + if (a.isSystem) Str.t("app_system_tag") else "",
                    checked = a.pkg in sel,
                    onCheckedChange = { checked ->
                        selected = if (checked) sel + a.pkg else sel - a.pkg
                    },
                )
            }
        }
    }
}
