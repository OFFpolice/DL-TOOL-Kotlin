package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Environment
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.example.data.DownloadItem
import com.example.ui.DownloadViewModel
import com.example.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

@Composable
fun HistoryScreen(
    viewModel: DownloadViewModel,
    modifier: Modifier = Modifier
) {
    val downloads by viewModel.allDownloads.collectAsState()
    val context = LocalContext.current

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
    ) {
        // App Title & Header text
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                AppHeaderTitle()
            }

            AnimatedVisibility(
                visible = downloads.isNotEmpty(),
                enter = fadeIn() + expandHorizontally(),
                exit = fadeOut() + shrinkHorizontally()
            ) {
                TextButton(
                    onClick = { viewModel.clearHistory() },
                    colors = ButtonDefaults.textButtonColors(contentColor = LightBlue)
                ) {
                    Text(text = "Очистить все", fontSize = 13.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Crossfade(
            targetState = downloads.isEmpty(),
            animationSpec = tween(durationMillis = 300),
            label = "downloads_crossfade",
            modifier = Modifier.weight(1f)
        ) { isEmpty ->
            if (isEmpty) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Bookmark,
                        contentDescription = "Сохранено",
                        tint = StatusBlue,
                        modifier = Modifier.size(52.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Сохраненных файлов пока нет",
                        color = TextWhite,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Скачанные видео и аудио появятся здесь",
                        color = TextGray,
                        fontSize = 13.sp
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(downloads, key = { it.id }) { item ->
                        SavedItemCard(
                            item = item,
                            onClick = {
                                openDownloadedFile(context, item)
                            },
                            onDelete = { viewModel.deleteItem(item.id) },
                            onShare = {
                                shareDownloadedVideo(context, item)
                            },
                            modifier = Modifier.animateItem()
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun VideoThumbnailView(
    item: DownloadItem,
    modifier: Modifier = Modifier
) {
    val file = remember(item.filePath, item.filename) {
        findExistingFile(item)
    }

    val bitmapState = produceState<Bitmap?>(initialValue = null, key1 = file?.absolutePath) {
        if (file != null && file.exists()) {
            withContext(Dispatchers.IO) {
                var retriever: MediaMetadataRetriever? = null
                try {
                    retriever = MediaMetadataRetriever()
                    retriever.setDataSource(file.absolutePath)
                    val frame = retriever.getFrameAtTime(1000000, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                        ?: retriever.frameAtTime
                    value = frame
                } catch (e: Exception) {
                    value = null
                } finally {
                    try {
                        retriever?.release()
                    } catch (_: Exception) {}
                }
            }
        } else {
            value = null
        }
    }

    val bitmap = bitmapState.value

    Surface(
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = modifier
    ) {
        Box(contentAlignment = Alignment.Center) {
            if (bitmap != null) {
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = "Превью видео",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.35f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Play",
                        tint = TextWhite,
                        modifier = Modifier.size(20.dp)
                    )
                }
            } else {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "Play",
                    tint = StatusBlue,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}

@Composable
fun SavedItemCard(
    item: DownloadItem,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    onShare: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            VideoThumbnailView(
                item = item,
                modifier = Modifier.size(width = 64.dp, height = 46.dp)
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.title.ifEmpty { item.filename },
                    color = TextWhite,
                    fontSize = 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    fontWeight = FontWeight.SemiBold
                )

                Spacer(modifier = Modifier.height(4.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = if (item.status == "COMPLETED") "Загружено" else if (item.status == "FAILED") "Ошибка" else "Загрузка...",
                        color = if (item.status == "COMPLETED") Color(0xFF4CAF50) else if (item.status == "FAILED") Color(0xFFFF5252) else StatusBlue,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )

                    if (item.status == "COMPLETED") {
                        Text(text = " • ", color = TextGray, fontSize = 11.sp)
                        val fileSizeMb = try {
                            val f = File(item.filePath)
                            if (f.exists()) "%.1f МБ".format(f.length().toDouble() / (1024 * 1024)) else "МБ н/д"
                        } catch (e: Exception) {
                            "МБ н/д"
                        }
                        Text(
                            text = fileSizeMb,
                            color = TextGray,
                            fontSize = 11.sp
                        )
                    }
                }
            }

            if (item.status == "COMPLETED") {
                IconButton(onClick = onShare) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = "Share",
                        tint = LightBlue,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete",
                    tint = TextGray,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

fun findExistingFile(item: DownloadItem): File? {
    val f1 = File(item.filePath)
    if (f1.exists()) return f1

    val publicDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
    val f2 = File(publicDir, item.filename)
    if (f2.exists()) return f2

    val dlToolDir = File(publicDir, "DL-TOOL/video/${item.filename}")
    if (dlToolDir.exists()) return dlToolDir

    return null
}

fun openDownloadedFile(context: Context, item: DownloadItem) {
    try {
        val file = findExistingFile(item)
        if (file == null || !file.exists()) {
            Toast.makeText(context, "Файл не найден на устройстве", Toast.LENGTH_SHORT).show()
            return
        }

        val uri: Uri = try {
            FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
        } catch (e: Exception) {
            Uri.fromFile(file)
        }

        val ext = file.extension.lowercase()
        val mimeType = when (ext) {
            "m4a", "mp3", "aac", "wav", "flac" -> "audio/*"
            else -> "video/*"
        }

        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, mimeType)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(Intent.createChooser(intent, "Открыть файл"))
    } catch (e: Exception) {
        Toast.makeText(context, "Не удалось открыть файл: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
    }
}

fun shareDownloadedVideo(context: Context, item: DownloadItem) {
    try {
        val file = findExistingFile(item)
        if (file == null || !file.exists()) {
            Toast.makeText(context, "Файл не найден или еще загружается", Toast.LENGTH_SHORT).show()
            return
        }

        val uri: Uri = try {
            FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
        } catch (e: Exception) {
            Uri.fromFile(file)
        }

        val ext = file.extension.lowercase()
        val mimeType = when (ext) {
            "m4a", "mp3", "aac", "wav", "flac" -> "audio/*"
            else -> "video/*"
        }

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Поделиться файлом"))
    } catch (e: Exception) {
        Toast.makeText(context, "Не удалось поделиться: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
    }
}
