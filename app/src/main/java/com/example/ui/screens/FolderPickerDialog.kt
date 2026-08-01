package com.example.ui.screens

import android.os.Environment
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ui.DownloadViewModel
import com.example.ui.theme.*
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FolderPickerDialog(
    initialPath: String,
    onDismiss: () -> Unit,
    onFolderSelected: (String) -> Unit
) {
    val context = LocalContext.current
    val rootStoragePath = remember { Environment.getExternalStorageDirectory().absolutePath }
    val defaultDownloadPath = remember { DownloadViewModel.DEFAULT_DOWNLOAD_PATH }

    var currentPath by remember {
        mutableStateOf(if (initialPath.isNotBlank() && File(initialPath).exists()) initialPath else defaultDownloadPath)
    }

    var showNewFolderDialog by remember { mutableStateOf(false) }
    var newFolderName by remember { mutableStateOf("") }
    var refreshTrigger by remember { mutableStateOf(0) }

    // Resolve list of subfolders in current path
    val folderList = remember(currentPath, refreshTrigger) {
        val dir = File(currentPath)
        if (!dir.exists()) {
            try {
                dir.mkdirs()
            } catch (e: Exception) {
                // ignore
            }
        }

        val result = mutableListOf<File>()
        val files = dir.listFiles()
        if (files != null) {
            files.filter { it.isDirectory && !it.name.startsWith(".") }
                .sortedBy { it.name.lowercase() }
                .forEach { result.add(it) }
        }
        result
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .fillMaxHeight(0.82f),
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                // Header Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.FolderSpecial,
                            contentDescription = "Folder Picker",
                            tint = AccentBlue,
                            modifier = Modifier.size(26.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Выберите папку",
                            color = TextWhite,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = TextGray
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Quick Navigation Chips
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = currentPath == defaultDownloadPath,
                        onClick = { currentPath = defaultDownloadPath },
                        label = { Text("DL-TOOL/video", fontSize = 12.sp) },
                        leadingIcon = {
                            Icon(
                                Icons.Default.Folder,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = AccentBlue,
                            selectedLabelColor = TextWhite,
                            containerColor = MaterialTheme.colorScheme.background,
                            labelColor = TextGray
                        )
                    )

                    FilterChip(
                        selected = currentPath == "$rootStoragePath/Download",
                        onClick = { currentPath = "$rootStoragePath/Download" },
                        label = { Text("Загрузки", fontSize = 12.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = AccentBlue,
                            selectedLabelColor = TextWhite,
                            containerColor = MaterialTheme.colorScheme.background,
                            labelColor = TextGray
                        )
                    )

                    FilterChip(
                        selected = currentPath == rootStoragePath,
                        onClick = { currentPath = rootStoragePath },
                        label = { Text("Корень", fontSize = 12.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = AccentBlue,
                            selectedLabelColor = TextWhite,
                            containerColor = MaterialTheme.colorScheme.background,
                            labelColor = TextGray
                        )
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Current Path Banner
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.background)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Place,
                            contentDescription = "Path",
                            tint = LightBlue,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = currentPath.replace(rootStoragePath, "Внутренняя память"),
                            color = TextWhite,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        
                        // New Folder Button inside path bar
                        IconButton(
                            onClick = {
                                newFolderName = ""
                                showNewFolderDialog = true
                            },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CreateNewFolder,
                                contentDescription = "Создать папку",
                                tint = AccentBlue,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Folder List
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.background, shape = RoundedCornerShape(12.dp))
                        .padding(8.dp)
                ) {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        // "Up" level item if not at root
                        if (currentPath != rootStoragePath && File(currentPath).parent != null) {
                            item {
                                Surface(
                                    onClick = {
                                        val parent = File(currentPath).parent
                                        if (parent != null && parent.startsWith(rootStoragePath)) {
                                            currentPath = parent
                                        } else {
                                            currentPath = rootStoragePath
                                        }
                                    },
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color.Transparent,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.ArrowBack,
                                            contentDescription = "Up",
                                            tint = LightBlue,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Text(
                                            text = ".. [На уровень выше]",
                                            color = LightBlue,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }

                        if (folderList.isEmpty()) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(24.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Icon(
                                            imageVector = Icons.Default.FolderOpen,
                                            contentDescription = null,
                                            tint = TextGray,
                                            modifier = Modifier.size(36.dp)
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = "В этой папке нет подпапок",
                                            color = TextGray,
                                            fontSize = 13.sp
                                        )
                                    }
                                }
                            }
                        } else {
                            items(folderList) { folder ->
                                val folderChildCount = remember(folder) {
                                    folder.listFiles { f -> f.isDirectory }?.size ?: 0
                                }

                                Surface(
                                    onClick = { currentPath = folder.absolutePath },
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 12.dp, vertical = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Folder,
                                            contentDescription = "Folder",
                                            tint = AccentBlue,
                                            modifier = Modifier.size(22.dp)
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = folder.name,
                                                color = TextWhite,
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Medium,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = "Подпапок: $folderChildCount",
                                                color = TextGray,
                                                fontSize = 11.sp
                                            )
                                        }

                                        Icon(
                                            imageVector = Icons.Default.ChevronRight,
                                            contentDescription = "Enter",
                                            tint = TextGray,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Bottom confirmation bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Отмена", color = TextGray, fontSize = 14.sp)
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Button(
                        onClick = {
                            // Ensure folder exists
                            val targetFile = File(currentPath)
                            if (!targetFile.exists()) {
                                targetFile.mkdirs()
                            }
                            onFolderSelected(currentPath)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = AccentBlue),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Confirm",
                            tint = TextWhite,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Выбрать эту папку",
                            color = TextWhite,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }
    }

    // New Folder Dialog
    if (showNewFolderDialog) {
        AlertDialog(
            onDismissRequest = { showNewFolderDialog = false },
            containerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = TextWhite,
            title = {
                Text(
                    text = "Создать папку",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column {
                    Text(
                        text = "Введите имя новой папки:",
                        color = TextGray,
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = newFolderName,
                        onValueChange = { newFolderName = it },
                        modifier = Modifier.fillMaxWidth(),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.background,
                            unfocusedContainerColor = MaterialTheme.colorScheme.background,
                            focusedTextColor = TextWhite,
                            unfocusedTextColor = TextWhite,
                            focusedIndicatorColor = StatusBlue,
                            unfocusedIndicatorColor = MaterialTheme.colorScheme.outline
                        ),
                        shape = RoundedCornerShape(10.dp),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val name = newFolderName.trim()
                        if (name.isNotEmpty()) {
                            val newDir = File(currentPath, name)
                            if (newDir.mkdirs() || newDir.exists()) {
                                currentPath = newDir.absolutePath
                                refreshTrigger++
                                Toast.makeText(context, "Папка «$name» создана", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "Не удалось создать папку", Toast.LENGTH_SHORT).show()
                            }
                        }
                        showNewFolderDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AccentBlue)
                ) {
                    Text("Создать", color = TextWhite)
                }
            },
            dismissButton = {
                TextButton(onClick = { showNewFolderDialog = false }) {
                    Text("Отмена", color = TextGray)
                }
            }
        )
    }
}
