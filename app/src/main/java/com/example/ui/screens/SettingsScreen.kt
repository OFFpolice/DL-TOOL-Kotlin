package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.DownloadViewModel
import com.example.ui.theme.*

@Composable
fun SettingsScreen(
    viewModel: DownloadViewModel,
    modifier: Modifier = Modifier
) {
    val savedFolder by viewModel.downloadFolder.collectAsState()
    var folderInput by remember { mutableStateOf(savedFolder) }

    // Sync input if saved value changes (e.g. on Default reset click)
    LaunchedEffect(savedFolder) {
        folderInput = savedFolder
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBg)
            .padding(16.dp)
    ) {
        // App Title text
        AppHeaderTitle()

        Spacer(modifier = Modifier.height(20.dp))

        // Card: Folder Config Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = DarkCard)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Text(
                    text = "Папка для загрузки видео",
                    color = TextWhite,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "Укажите путь, куда будут сохраняться видео",
                    color = TextGray,
                    fontSize = 13.sp
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Custom Styled Folder Path Input
                OutlinedTextField(
                    value = folderInput,
                    onValueChange = { folderInput = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = DarkBg,
                        unfocusedContainerColor = DarkBg,
                        disabledContainerColor = DarkBg,
                        focusedTextColor = TextWhite,
                        unfocusedTextColor = TextWhite,
                        focusedIndicatorColor = StatusBlue,
                        unfocusedIndicatorColor = DarkBorder,
                        cursorColor = StatusBlue
                    ),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Action Buttons Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Save Button
                    Button(
                        onClick = { viewModel.saveDownloadFolder(folderInput) },
                        modifier = Modifier
                            .height(48.dp)
                            .padding(end = 12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = AccentBlue),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Save,
                            contentDescription = "Save",
                            tint = TextWhite,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Сохранить",
                            color = TextWhite,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Reset to Default Text Button
                    TextButton(
                        onClick = { viewModel.restoreDefaultFolder() },
                        colors = ButtonDefaults.textButtonColors(contentColor = LightBlue)
                    ) {
                        Text(
                            text = "По умолчанию",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}
