package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
            val showYtDlpUpdateDialog by viewModel.showYtDlpUpdateDialog.collectAsState()
            val ytDlpVersionInfo by viewModel.ytDlpVersionInfo.collectAsState()
            val isUpdatingYtDlp by viewModel.isUpdatingYtDlp.collectAsState()

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

                if (showYtDlpUpdateDialog && ytDlpVersionInfo != null) {
                    YtDlpUpdateDialog(
                        currentVersion = ytDlpVersionInfo?.currentVersion ?: "",
                        latestVersion = ytDlpVersionInfo?.latestVersion ?: "",
                        isUpdating = isUpdatingYtDlp,
                        onDismiss = { viewModel.dismissYtDlpUpdateDialog() },
                        onConfirmUpdate = { viewModel.updateYtDlp() }
                    )
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
            TelegramStyleBottomBar(
                selectedTab = selectedTab,
                onTabSelected = { selectTab(it) }
            )
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

@Composable
fun TelegramStyleBottomBar(
    selectedTab: Int,
    onTabSelected: (Int) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 14.dp, vertical = 8.dp),
        contentAlignment = Alignment.BottomCenter
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(elevation = 12.dp, shape = RoundedCornerShape(32.dp), clip = false),
            shape = RoundedCornerShape(32.dp),
            color = Color(0xFF20262E),
            tonalElevation = 6.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp)
                    .padding(horizontal = 6.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TelegramNavItem(
                    label = "Скачать",
                    selected = selectedTab == 0,
                    selectedIcon = Icons.Filled.Download,
                    unselectedIcon = Icons.Outlined.Download,
                    onClick = { onTabSelected(0) }
                )
                TelegramNavItem(
                    label = "Сохранено",
                    selected = selectedTab == 1,
                    selectedIcon = Icons.Filled.Bookmark,
                    unselectedIcon = Icons.Outlined.BookmarkBorder,
                    onClick = { onTabSelected(1) }
                )
                TelegramNavItem(
                    label = "О нас",
                    selected = selectedTab == 2,
                    selectedIcon = Icons.Filled.Info,
                    unselectedIcon = Icons.Outlined.Info,
                    onClick = { onTabSelected(2) }
                )
                TelegramNavItem(
                    label = "Настройки",
                    selected = selectedTab == 3,
                    selectedIcon = Icons.Filled.Settings,
                    unselectedIcon = Icons.Outlined.Settings,
                    onClick = { onTabSelected(3) }
                )
            }
        }
    }
}

@Composable
private fun RowScope.TelegramNavItem(
    label: String,
    selected: Boolean,
    selectedIcon: ImageVector,
    unselectedIcon: ImageVector,
    onClick: () -> Unit
) {
    val activeCyanBlue = Color(0xFF33A9EE)       // Telegram cyan blue accent
    val activePillBgColor = Color(0xFF253B50)     // Telegram capsule background color (covers icon + label)
    val unselectedWhite = Color(0xFFFFFFFF)       // Telegram unselected white

    Box(
        modifier = Modifier
            .weight(1f)
            .fillMaxHeight()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .background(
                    color = if (selected) activePillBgColor else Color.Transparent,
                    shape = RoundedCornerShape(20.dp)
                )
                .padding(horizontal = 12.dp, vertical = 5.dp)
        ) {
            Icon(
                imageVector = if (selected) selectedIcon else unselectedIcon,
                contentDescription = label,
                tint = if (selected) activeCyanBlue else unselectedWhite,
                modifier = Modifier.size(23.dp)
            )
            Spacer(modifier = Modifier.height(1.dp))
            Text(
                text = label,
                fontSize = 11.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                color = if (selected) activeCyanBlue else unselectedWhite,
                maxLines = 1
            )
        }
    }
}
