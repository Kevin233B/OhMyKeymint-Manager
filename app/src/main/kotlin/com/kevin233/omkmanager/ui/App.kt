package com.kevin233.omkmanager.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.kevin233.omkmanager.core.Prefs
import com.kevin233.omkmanager.core.Str
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.SmallTopAppBar
import top.yukonga.miuix.kmp.basic.SnackbarHost
import top.yukonga.miuix.kmp.basic.SnackbarHostState
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Back
import top.yukonga.miuix.kmp.theme.ColorSchemeMode
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.ThemeController

/** 子页面路由 */
sealed interface Page {
    data object Targets : Page
    data object Keybox : Page
    data object Patch : Page
    data object Pif : Page
    data object SoterBeta : Page
    data object SoterHal : Page
}

private fun pageTitle(page: Page): String = when (page) {
    Page.Targets -> Str.t("targets_title")
    Page.Keybox -> Str.t("replace_keybox_title")
    Page.Patch -> Str.t("tools_security_patch")
    Page.Pif -> Str.t("pif_fingerprint_title")
    Page.SoterBeta -> Str.t("tools_soter_beta")
    Page.SoterHal -> Str.t("tools_soter_hal")
}

@Composable
fun App() {
    val controller = remember(Prefs.theme) {
        ThemeController(
            when (Prefs.theme) {
                Prefs.Theme.SYSTEM -> ColorSchemeMode.System
                Prefs.Theme.LIGHT -> ColorSchemeMode.Light
                Prefs.Theme.DARK -> ColorSchemeMode.Dark
            }
        )
    }
    MiuixTheme(controller = controller) {
        var tab by rememberSaveable { mutableStateOf(0) }
        var page by remember { mutableStateOf<Page?>(null) }

        val snackbar = remember { SnackbarHostState() }
        val scope = rememberCoroutineScope()
        val toast: (String) -> Unit = { msg -> scope.launch { snackbar.showSnackbar(msg) } }

        BackHandler(enabled = page != null) { page = null }

        Scaffold(
            topBar = {
                val p = page
                if (p == null) {
                    SmallTopAppBar(
                        title = Str.t("app_name"),
                    )
                } else {
                    SmallTopAppBar(
                        title = pageTitle(p),
                        navigationIcon = {
                            IconButton(onClick = { page = null }) {
                                Icon(imageVector = MiuixIcons.Back, contentDescription = "Back")
                            }
                        },
                    )
                }
            },
            bottomBar = {
                if (page == null) {
                    MainBottomBar(selected = tab) { tab = it }
                }
            },
            snackbarHost = { SnackbarHost(snackbar) },
        ) { paddingValues ->
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                when (val p = page) {
                    null -> when (tab) {
                        0 -> HomeScreen(
                            openTargets = { page = Page.Targets },
                            openPif = { page = Page.Pif },
                            toast = toast,
                        )
                        1 -> ToolsScreen(open = { page = it })
                        else -> SettingsScreen()
                    }
                    Page.Targets -> TargetsScreen(toast = toast, onDone = { page = null })
                    Page.Keybox -> KeyboxScreen(toast = toast)
                    Page.Patch -> PatchScreen(toast = toast)
                    Page.Pif -> PifScreen(toast = toast)
                    Page.SoterBeta -> SoterBetaScreen(toast = toast)
                    Page.SoterHal -> SoterHalScreen(toast = toast)
                }
            }
        }
    }
}
