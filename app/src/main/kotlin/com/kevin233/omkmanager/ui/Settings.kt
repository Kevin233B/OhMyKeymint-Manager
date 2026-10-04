package com.kevin233.omkmanager.ui

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.kevin233.omkmanager.core.Prefs
import com.kevin233.omkmanager.core.Str
import top.yukonga.miuix.kmp.basic.BasicComponent
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.DropdownItem
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.preference.WindowSpinnerPreference

@Composable
fun SettingsScreen() {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 12.dp, end = 12.dp, bottom = 24.dp),
    ) {
        item { SmallTitle(text = Str.t("settings_appearance"), modifier = Modifier.padding(top = 16.dp)) }
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                var themeIdx by remember { mutableIntStateOf(Prefs.theme.ordinal) }
                WindowSpinnerPreference(
                    title = Str.t("settings_mode"),
                    items = listOf(
                        DropdownItem(text = Str.t("settings_mode_system")),
                        DropdownItem(text = Str.t("settings_mode_light")),
                        DropdownItem(text = Str.t("settings_mode_dark")),
                    ),
                    selectedIndex = themeIdx,
                    onSelectedIndexChange = { idx ->
                        themeIdx = idx
                        Prefs.setTheme(Prefs.Theme.entries[idx])
                    },
                )
            }
        }

        item { SmallTitle(text = Str.t("settings_language")) }
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                var langIdx by remember { mutableIntStateOf(Prefs.lang.ordinal) }
                WindowSpinnerPreference(
                    title = Str.t("settings_language"),
                    items = listOf(
                        DropdownItem(text = Str.t("settings_language_auto")),
                        DropdownItem(text = Str.t("settings_language_zh")),
                        DropdownItem(text = Str.t("settings_language_en")),
                    ),
                    selectedIndex = langIdx,
                    onSelectedIndexChange = { idx ->
                        langIdx = idx
                        Prefs.setLang(Prefs.Lang.entries[idx])
                    },
                )
            }
        }

        item { SmallTitle(text = Str.t("settings_about")) }
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                BasicComponent(
                    title = Str.t("settings_version"),
                    summary = "1.0.0",
                )
                BasicComponent(
                    title = Str.t("settings_author"),
                    summary = Str.t("author_name"),
                )
                BasicComponent(
                    title = Str.t("settings_description"),
                    summary = Str.t("app_desc"),
                )
                BasicComponent(
                    title = Str.t("settings_upstream"),
                    summary = Str.t("upstream_value"),
                )
            }
        }
    }
}
