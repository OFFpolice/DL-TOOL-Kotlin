package com.offpolice.dltool.ui.screens

import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.offpolice.dltool.data.DownloadItem
import com.offpolice.dltool.ui.DownloadViewModel
import com.offpolice.dltool.ui.theme.*

@Composable
fun DownloadScreen(
    viewModel: DownloadViewModel,
    modifier: Modifier = Modifier,
    onNavigateToHistory: () -> Unit = {}
) {
    val context = LocalContext.current
    val urlVal by viewModel.urlInput.collectAsState()
    val loadingState by viewModel.isLoading.collectAsState()
    val videoInfo by viewModel.videoInfoState.collectAsState()
    val isDownloading by viewModel.isDownloading.collectAsState()
    val downloadProgress by viewModel.downloadProgress.collectAsState()
    val activeTitle by viewModel.activeDownloadTitle.collectAsState()
    val activeProgressText by viewModel.activeDownloadProgressText.collectAsState()

    val animatedProgress by animateFloatAsState(
        targetValue = downloadProgress,
        animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing),
        label = "smooth_download_progress"
    )

    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current

    if (urlVal.isNotEmpty()) {
        BackHandler {
            viewModel.onUrlChange("")
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .pointerInput(Unit) {
                detectTapGestures(onTap = {
                    focusManager.clearFocus()
                })
            }
            .padding(16.dp)
    ) {
        AppHeaderTitle()

        Spacer(modifier = Modifier.height(20.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
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
                        focusedContainerColor = MaterialTheme.colorScheme.background,
                        unfocusedContainerColor = MaterialTheme.colorScheme.background,
                        disabledContainerColor = MaterialTheme.colorScheme.background,
                        focusedTextColor = TextWhite,
                        unfocusedTextColor = TextWhite,
                        focusedIndicatorColor = StatusBlue,
                        unfocusedIndicatorColor = MaterialTheme.colorScheme.outline,
                        cursorColor = StatusBlue
                    ),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                    trailingIcon = if (urlVal.isNotEmpty()) {
                        {
                            IconButton(onClick = { viewModel.onUrlChange("") }) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Очистить",
                                    tint = TextGray
                                )
                            }
                        }
                    } else null,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = {
                        keyboardController?.hide()
                        viewModel.downloadVideo()
                    })
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clip = clipboard.primaryClip
                            if (clip != null && clip.itemCount > 0) {
                                val text = clip.getItemAt(0).text?.toString()?.trim() ?: ""
                                if (text.startsWith("http://") || text.startsWith("https://") || text.startsWith("HTTP://") || text.startsWith("HTTPS://")) {
                                    viewModel.onUrlChange(text)
                                    if (viewModel.urlInput.value == text) {
                                        Toast.makeText(context, "Вставлено из буфера", Toast.LENGTH_SHORT).show()
                                    } else {
                                        Toast.makeText(context, "Ссылка содержит недопустимые символы или эмодзи", Toast.LENGTH_SHORT).show()
                                    }
                                } else {
                                    Toast.makeText(context, "Ссылка должна начинаться с http:// или https://", Toast.LENGTH_SHORT).show()
                                }
                            } else {
                                Toast.makeText(context, "Буфер обмена пуст", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .padding(end = 8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
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
                        enabled = !loadingState && !isDownloading
                    ) {
                        if (loadingState && !isDownloading) {
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

        AnimatedVisibility(
            visible = isDownloading,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Column {
                Spacer(modifier = Modifier.height(16.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, StatusBlue.copy(alpha = 0.5f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    color = StatusBlue,
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = activeTitle.ifEmpty { "Загрузка..." },
                                        color = TextWhite,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = activeProgressText.ifEmpty { "Скачивание видео..." },
                                        color = TextGray,
                                        fontSize = 12.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }

                            IconButton(
                                onClick = { viewModel.cancelDownload() },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Отмена загрузки",
                                    tint = TextGray
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        if (animatedProgress > 0f) {
                            LinearProgressIndicator(
                                progress = { animatedProgress },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(3.dp)),
                                color = AccentBlue,
                                trackColor = MaterialTheme.colorScheme.background
                            )
                        } else {
                            LinearProgressIndicator(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(3.dp)),
                                color = AccentBlue,
                                trackColor = MaterialTheme.colorScheme.background
                            )
                        }
                    }
                }
            }
        }

        videoInfo?.let { info ->
            VideoInfoDialog(
                videoInfo = info,
                onDismiss = { viewModel.dismissVideoInfo() },
                onConfirmDownload = { selectedOption ->
                    viewModel.confirmDownload(selectedOption)
                }
            )
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
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.background)
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
                    .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp))
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
