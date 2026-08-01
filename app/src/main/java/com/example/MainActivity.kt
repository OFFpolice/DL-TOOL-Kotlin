package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.example.ui.DownloadViewModel
import com.example.ui.screens.*
import com.example.ui.theme.*

class MainActivity : ComponentActivity() {

    private val viewModel: DownloadViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val isBlackTheme by viewModel.isBlackThemeEnabled.collectAsState()
            MyApplicationTheme(isBlackTheme = isBlackTheme) {
                var showSplash by remember { mutableStateOf(true) }

                Crossfade(
                    targetState = showSplash,
                    animationSpec = tween(durationMillis = 400),
                    label = "splash_crossfade"
                ) { isSplash ->
                    if (isSplash) {
                        SplashScreen(onSplashFinished = { showSplash = false })
                    } else {
                        MainAppLayout(viewModel = viewModel)
                    }
                }
            }
        }
    }
}

@Composable
fun MainAppLayout(viewModel: DownloadViewModel) {
    val tabStack = remember { mutableStateListOf(0) }
    val selectedTab = tabStack.lastOrNull() ?: 0

    fun selectTab(tab: Int) {
        if (selectedTab != tab) {
            tabStack.add(tab)
        }
    }

    BackHandler(enabled = tabStack.size > 1) {
        tabStack.removeAt(tabStack.size - 1)
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surfaceContainer,
                tonalElevation = NavigationBarDefaults.Elevation
            ) {
                // Tab 0: Скачать (Download)
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectTab(0) },
                    icon = {
                        Icon(
                            imageVector = Icons.Filled.Download,
                            contentDescription = "Скачать"
                        )
                    },
                    label = { Text("Скачать") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = TextWhite,
                        selectedTextColor = StatusBlue,
                        indicatorColor = MaterialTheme.colorScheme.surfaceVariant,
                        unselectedIconColor = TextGray,
                        unselectedTextColor = TextGray
                    )
                )

                // Tab 1: Сохранено (Saved)
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectTab(1) },
                    icon = {
                        Icon(
                            imageVector = Icons.Filled.Bookmark,
                            contentDescription = "Сохранено"
                        )
                    },
                    label = { Text("Сохранено") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = TextWhite,
                        selectedTextColor = StatusBlue,
                        indicatorColor = MaterialTheme.colorScheme.surfaceVariant,
                        unselectedIconColor = TextGray,
                        unselectedTextColor = TextGray
                    )
                )

                // Tab 2: О нас (About us)
                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = { selectTab(2) },
                    icon = {
                        Icon(
                            imageVector = Icons.Filled.Info,
                            contentDescription = "О нас"
                        )
                    },
                    label = { Text("О нас") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = TextWhite,
                        selectedTextColor = StatusBlue,
                        indicatorColor = MaterialTheme.colorScheme.surfaceVariant,
                        unselectedIconColor = TextGray,
                        unselectedTextColor = TextGray
                    )
                )

                // Tab 3: Настройки (Settings)
                NavigationBarItem(
                    selected = selectedTab == 3,
                    onClick = { selectTab(3) },
                    icon = {
                        Icon(
                            imageVector = Icons.Filled.Settings,
                            contentDescription = "Настройки"
                        )
                    },
                    label = { Text("Настройки") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = TextWhite,
                        selectedTextColor = StatusBlue,
                        indicatorColor = MaterialTheme.colorScheme.surfaceVariant,
                        unselectedIconColor = TextGray,
                        unselectedTextColor = TextGray
                    )
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            AnimatedContent(
                targetState = selectedTab,
                transitionSpec = {
                    if (targetState > initialState) {
                        (slideInHorizontally { width -> width / 3 } + fadeIn(animationSpec = tween(250)))
                            .togetherWith(slideOutHorizontally { width -> -width / 3 } + fadeOut(animationSpec = tween(200)))
                    } else {
                        (slideInHorizontally { width -> -width / 3 } + fadeIn(animationSpec = tween(250)))
                            .togetherWith(slideOutHorizontally { width -> width / 3 } + fadeOut(animationSpec = tween(200)))
                    }
                },
                label = "tab_content_transition"
            ) { tab ->
                when (tab) {
                    0 -> DownloadScreen(
                        viewModel = viewModel,
                        onNavigateToHistory = { selectTab(1) }
                    )
                    1 -> HistoryScreen(viewModel = viewModel)
                    2 -> AboutScreen()
                    3 -> SettingsScreen(viewModel = viewModel)
                }
            }
        }
    }
}
