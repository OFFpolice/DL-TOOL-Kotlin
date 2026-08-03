package com.offpolice.dltool.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.offpolice.dltool.ui.theme.AccentBlue
import com.offpolice.dltool.ui.theme.StatusGreen
import com.offpolice.dltool.ui.theme.TextGray
import com.offpolice.dltool.ui.theme.TextWhite

@Composable
fun YtDlpUpdateDialog(
    currentVersion: String,
    latestVersion: String,
    isUpdating: Boolean,
    onDismiss: () -> Unit,
    onConfirmUpdate: () -> Unit
) {
    AlertDialog(
        onDismissRequest = { if (!isUpdating) onDismiss() },
        containerColor = MaterialTheme.colorScheme.surface,
        titleContentColor = TextWhite,
        textContentColor = TextGray,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.SystemUpdate,
                    contentDescription = null,
                    tint = AccentBlue,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "Обновление yt-dlp",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Column {
                Text(
                    text = "Доступна новая версия компонента yt-dlp!",
                    color = TextWhite,
                    fontSize = 14.sp
                )
                Spacer(modifier = Modifier.height(14.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.background, RoundedCornerShape(12.dp))
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Текущая:", fontSize = 11.sp, color = TextGray)
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(currentVersion, fontSize = 13.sp, color = TextWhite, fontWeight = FontWeight.SemiBold)
                    }
                    Icon(
                        imageVector = Icons.Default.ArrowForward,
                        contentDescription = null,
                        tint = AccentBlue,
                        modifier = Modifier.size(18.dp)
                    )
                    Column {
                        Text("Новая:", fontSize = 11.sp, color = TextGray)
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(latestVersion, fontSize = 13.sp, color = StatusGreen, fontWeight = FontWeight.Bold)
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Обновление обеспечит поддержку новых сервисов и улучшит скорость работы.",
                    fontSize = 12.sp,
                    color = TextGray
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirmUpdate,
                enabled = !isUpdating,
                colors = ButtonDefaults.buttonColors(containerColor = AccentBlue),
                shape = RoundedCornerShape(10.dp)
            ) {
                if (isUpdating) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        color = TextWhite,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Обновление...", color = TextWhite, fontSize = 13.sp)
                } else {
                    Icon(imageVector = Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Обновить", color = TextWhite, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            }
        },
        dismissButton = {
            if (!isUpdating) {
                TextButton(onClick = onDismiss) {
                    Text("Позже", color = TextGray, fontSize = 13.sp)
                }
            }
        }
    )
}
