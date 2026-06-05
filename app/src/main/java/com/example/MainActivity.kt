package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.History
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
            MyApplicationTheme {
                var showSplash by remember { mutableStateOf(true) }

                if (showSplash) {
                    SplashScreen(onSplashFinished = { showSplash = false })
                } else {
                    MainAppLayout(viewModel = viewModel)
                }
            }
        }
    }
}

@Composable
fun MainAppLayout(viewModel: DownloadViewModel) {
    var selectedTab by remember { mutableIntStateOf(0) }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = DarkBg,
        bottomBar = {
            NavigationBar(
                containerColor = BottomNavBg,
                tonalElevation = NavigationBarDefaults.Elevation
            ) {
                // Tab 0: Скачать (Download)
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
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
                        indicatorColor = DarkButton,
                        unselectedIconColor = TextGray,
                        unselectedTextColor = TextGray
                    )
                )

                // Tab 1: История (History)
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = {
                        Icon(
                            imageVector = Icons.Filled.History,
                            contentDescription = "История"
                        )
                    },
                    label = { Text("История") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = TextWhite,
                        selectedTextColor = StatusBlue,
                        indicatorColor = DarkButton,
                        unselectedIconColor = TextGray,
                        unselectedTextColor = TextGray
                    )
                )

                // Tab 2: О нас (About us)
                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
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
                        indicatorColor = DarkButton,
                        unselectedIconColor = TextGray,
                        unselectedTextColor = TextGray
                    )
                )

                // Tab 3: Настройки (Settings)
                NavigationBarItem(
                    selected = selectedTab == 3,
                    onClick = { selectedTab = 3 },
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
                        indicatorColor = DarkButton,
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
            when (selectedTab) {
                0 -> DownloadScreen(
                    viewModel = viewModel,
                    onNavigateToHistory = { selectedTab = 1 }
                )
                1 -> HistoryScreen(viewModel = viewModel)
                2 -> AboutScreen()
                3 -> SettingsScreen(viewModel = viewModel)
            }
        }
    }
}
