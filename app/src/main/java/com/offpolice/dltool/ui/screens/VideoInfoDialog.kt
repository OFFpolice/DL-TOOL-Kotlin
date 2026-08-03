package com.offpolice.dltool.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.offpolice.dltool.ui.VideoFormatOption
import com.offpolice.dltool.ui.VideoInfo
import com.offpolice.dltool.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideoInfoDialog(
    videoInfo: VideoInfo,
    onDismiss: () -> Unit,
    onConfirmDownload: (VideoFormatOption) -> Unit
) {
    var selectedOption by remember {
        mutableStateOf(videoInfo.formats.firstOrNull() ?: VideoFormatOption("best", "Авто", "mp4", "best", false))
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .wrapContentHeight(),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 10.dp,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Выберите качество",
                        color = TextWhite,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Video Info Compact Preview Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.background),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Thumbnail Box
                        Box(
                            modifier = Modifier
                                .width(100.dp)
                                .height(64.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color.Black),
                            contentAlignment = Alignment.Center
                        ) {
                            if (videoInfo.thumbnail.isNotEmpty()) {
                                AsyncImage(
                                    model = videoInfo.thumbnail,
                                    contentDescription = "Preview",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.PlayCircle,
                                    contentDescription = null,
                                    tint = AccentBlue,
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = videoInfo.title,
                                color = TextWhite,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                lineHeight = 16.sp
                            )

                            if (videoInfo.uploader.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = videoInfo.uploader,
                                    color = TextGray,
                                    fontSize = 11.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            if (videoInfo.duration.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(6.dp))

                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = MaterialTheme.colorScheme.surface,
                                    border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f))
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Schedule,
                                            contentDescription = null,
                                            tint = TextGray,
                                            modifier = Modifier.size(12.dp)
                                        )
                                        Text(
                                            text = videoInfo.duration,
                                            color = TextGray,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "Доступные форматы:",
                    color = TextGray,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )

                Spacer(modifier = Modifier.height(8.dp))

                // 2-Column Grid of Quality Cards
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 300.dp)
                ) {
                    itemsIndexed(
                        items = videoInfo.formats,
                        span = { index, _ ->
                            if (index == videoInfo.formats.lastIndex && videoInfo.formats.size % 2 != 0) {
                                GridItemSpan(2)
                            } else {
                                GridItemSpan(1)
                            }
                        }
                    ) { index, option ->
                        val isSelected = option.formatId == selectedOption.formatId

                        val cardBg by animateColorAsState(
                            targetValue = if (isSelected) AccentBlue.copy(alpha = 0.18f) else MaterialTheme.colorScheme.background,
                            animationSpec = tween(150), label = "bg"
                        )

                        val borderColor by animateColorAsState(
                            targetValue = if (isSelected) AccentBlue else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                            animationSpec = tween(150), label = "border"
                        )

                        Surface(
                            onClick = { selectedOption = option },
                            shape = RoundedCornerShape(14.dp),
                            color = cardBg,
                            border = BorderStroke(if (isSelected) 1.8.dp else 1.dp, borderColor),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 10.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    // Icon indicator
                                    Box(
                                        modifier = Modifier
                                            .size(30.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(
                                                if (option.isAudio) StatusGreen.copy(alpha = 0.15f)
                                                else if (isSelected) AccentBlue.copy(alpha = 0.25f)
                                                else MaterialTheme.colorScheme.surface
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = if (option.isAudio) Icons.Default.Audiotrack else Icons.Default.Movie,
                                            contentDescription = null,
                                            tint = if (option.isAudio) StatusGreen else if (isSelected) AccentBlue else LightBlue,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }

                                    Column {
                                        Text(
                                            text = option.label,
                                            color = TextWhite,
                                            fontSize = 12.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = if (option.size.isNotEmpty()) "${option.ext.uppercase()} • ${option.size}" else option.ext.uppercase(),
                                            color = if (isSelected) LightBlue else TextGray,
                                            fontSize = 10.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }

                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = "Selected",
                                        tint = AccentBlue,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Bottom Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = TextGray),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                    ) {
                        Text("Отмена", fontSize = 13.sp)
                    }

                    Button(
                        onClick = { onConfirmDownload(selectedOption) },
                        colors = ButtonDefaults.buttonColors(containerColor = AccentBlue),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1.5f)
                            .height(44.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Download,
                            contentDescription = "Download",
                            tint = TextWhite,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Скачать",
                            color = TextWhite,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }
    }
}
