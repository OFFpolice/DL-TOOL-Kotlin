package com.example.ui.screens

import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.DownloadItem
import com.example.ui.DownloadViewModel
import com.example.ui.theme.*

@Composable
fun DownloadScreen(
    viewModel: DownloadViewModel,
    modifier: Modifier = Modifier,
    onNavigateToHistory: () -> Unit = {}
) {
    val context = LocalContext.current
    val urlVal by viewModel.urlInput.collectAsState()
    val statusTitle by viewModel.downloadStatus.collectAsState()
    val statusDesc by viewModel.statusMessage.collectAsState()
    val loadingState by viewModel.isLoading.collectAsState()
    val dlsList by viewModel.allDownloads.collectAsState()

    val keyboardController = LocalSoftwareKeyboardController.current

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBg)
            .padding(16.dp)
    ) {
        // App Title text
        AppHeaderTitle()

        Spacer(modifier = Modifier.height(20.dp))

        // Card 1: Main download interface
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = DarkCard)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                // Link Input Textfield container
                OutlinedTextField(
                    value = urlVal,
                    onValueChange = { viewModel.onUrlChange(it) },
                    placeholder = {
                        Text(
                            text = "Введите ссылку",
                            color = TextGray,
                            fontSize = 15.sp
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp),
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
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = {
                        keyboardController?.hide()
                        viewModel.downloadVideo()
                    })
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Input action buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Paste Button
                    Button(
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clip = clipboard.primaryClip
                            if (clip != null && clip.itemCount > 0) {
                                val text = clip.getItemAt(0).text?.toString() ?: ""
                                viewModel.onUrlChange(text)
                                Toast.makeText(context, "Вставлено из буфера", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "Буфер обмена пуст", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .padding(end = 8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = DarkButton),
                        shape = RoundedCornerShape(30.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentPaste,
                            contentDescription = "Paste",
                            tint = TextWhite,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = "Вставить", color = TextWhite, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                    }

                    // Download Button
                    Button(
                        onClick = {
                            keyboardController?.hide()
                            viewModel.downloadVideo()
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .padding(start = 8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = AccentBlue),
                        shape = RoundedCornerShape(30.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp),
                        enabled = !loadingState
                    ) {
                        if (loadingState) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                color = TextWhite,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Download,
                                contentDescription = "Download",
                                tint = TextWhite,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(text = "Скачать", color = TextWhite, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Section header "Статус"
        Text(
            text = "Статус",
            color = TextWhite,
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 4.dp, bottom = 12.dp)
        )

        // Card 2: Status Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = DarkCard)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = "Status",
                    tint = StatusBlue,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(
                        text = statusTitle,
                        color = TextWhite,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = statusDesc,
                        color = TextGray,
                        fontSize = 13.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Section header "Последние загрузки"
        Text(
            text = "Последние загрузки",
            color = TextWhite,
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 4.dp, bottom = 12.dp)
        )

        // Card 3: Recent downloads container
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = DarkCard)
        ) {
            if (dlsList.isEmpty()) {
                // Empty state centered view matching screenshots
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Folder,
                        contentDescription = "Empty",
                        tint = StatusBlue,
                        modifier = Modifier
                            .size(56.dp)
                            .padding(bottom = 12.dp)
                    )
                    Text(
                        text = "Здесь пока пусто",
                        color = TextWhite,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Ваши скачанные видео появятся здесь",
                        color = TextGray,
                        fontSize = 13.sp,
                        lineHeight = 18.sp
                    )
                }
            } else {
                // Show the single most recent item
                val recentItem = dlsList.first()
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "Последний скачанный файл:",
                        color = TextGray,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    RecentDownloadItemView(item = recentItem, onDelete = {
                        viewModel.deleteItem(recentItem.id)
                    })
                    Spacer(modifier = Modifier.height(20.dp))
                    Button(
                        onClick = onNavigateToHistory,
                        colors = ButtonDefaults.buttonColors(containerColor = DarkButton),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(text = "Вся история", color = LightBlue, fontSize = 14.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun AppHeaderTitle() {
    val annotatedTitle = buildAnnotatedString {
        withStyle(style = SpanStyle(color = StatusBlue, fontWeight = FontWeight.Bold)) {
            append("DL ")
        }
        withStyle(style = SpanStyle(color = TextWhite, fontWeight = FontWeight.Bold)) {
            append("TOOL")
        }
    }
    Text(
        text = annotatedTitle,
        fontSize = 26.sp,
        fontWeight = FontWeight.Black,
        modifier = Modifier.padding(vertical = 12.dp)
    )
}

@Composable
fun RecentDownloadItemView(
    item: DownloadItem,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = DarkBg)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.PlayArrow,
                contentDescription = "Video file",
                tint = StatusBlue,
                modifier = Modifier
                    .size(36.dp)
                    .background(DarkButton, RoundedCornerShape(8.dp))
                    .padding(6.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.filename,
                    color = TextWhite,
                    fontSize = 13.sp,
                    maxLines = 1,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = item.status,
                    color = if (item.status == "COMPLETED") Color.Green else if (item.status == "FAILED") Color.Red else StatusBlue,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete",
                    tint = TextGray,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
