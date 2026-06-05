package com.example.ui

import android.app.Application
import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.DownloadItem
import com.example.data.DownloadRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.security.MessageDigest
import com.chaquo.python.Python
import com.chaquo.python.android.AndroidPlatform

class DownloadViewModel(application: Application) : AndroidViewModel(application) {

    init {
        val context = getApplication<Application>()
        if (!Python.isStarted()) {
            Python.start(AndroidPlatform(context))
        }
    }

    private val database = AppDatabase.getDatabase(application)
    private val repository = DownloadRepository(database.downloadDao())
    private val sharedPrefs = application.getSharedPreferences("dl_tool_prefs", Context.MODE_PRIVATE)

    // UI States
    val urlInput = MutableStateFlow("")
    val downloadStatus = MutableStateFlow("Готов к скачиванию")
    val statusMessage = MutableStateFlow("Вставьте ссылку и нажмите «Скачать»")
    val isLoading = MutableStateFlow(false)

    // Saved Download Folder Preferences
    val downloadFolder = MutableStateFlow(
        sharedPrefs.getString("download_folder", "/storage/emulated/0/Download") ?: "/storage/emulated/0/Download"
    )

    // Data List
    val allDownloads: StateFlow<List<DownloadItem>> = repository.allDownloads.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun onUrlChange(newUrl: String) {
        urlInput.value = newUrl
    }

    fun saveDownloadFolder(newFolder: String) {
        downloadFolder.value = newFolder
        sharedPrefs.edit().putString("download_folder", newFolder).apply()
        Toast.makeText(getApplication(), "Путь сохранен", Toast.LENGTH_SHORT).show()
    }

    fun restoreDefaultFolder() {
        val defaultPath = "/storage/emulated/0/Download"
        downloadFolder.value = defaultPath
        sharedPrefs.edit().putString("download_folder", defaultPath).apply()
        Toast.makeText(getApplication(), "Сброшено по умолчанию", Toast.LENGTH_SHORT).show()
    }



    fun clearHistory() {
        viewModelScope.launch(Dispatchers.IO) {
            allDownloads.value.forEach {
                repository.deleteDownload(it)
            }
        }
    }

    fun deleteItem(id: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteDownloadById(id)
        }
    }

    fun downloadVideo() {
        val url = urlInput.value.trim()
        if (url.isEmpty()) {
            downloadStatus.value = "Ошибка"
            statusMessage.value = "Буфер обмена пуст или ссылка некорректна"
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            viewModelScope.launch(Dispatchers.Main) {
                isLoading.value = true
                downloadStatus.value = "Загрузка..."
                statusMessage.value = "Получение информации и скачивание с помощью yt-dlp..."
            }

            val hash = url.md5()
            val filename = "$hash.mp4"
            var targetFolder = downloadFolder.value
            if (targetFolder.isEmpty()) {
                targetFolder = "/storage/emulated/0/Download"
            }
            val fullFilePath = "$targetFolder/$filename"

            val dbItem = DownloadItem(
                url = url,
                title = "Видео $hash",
                filename = filename,
                filePath = fullFilePath,
                status = "DOWNLOADING"
            )
            val dbId = repository.insertDownload(dbItem).toInt()

            var success = false
            var extractedTitle = "Видео $hash"
            var errorMessage = "Неизвестная ошибка"

            try {
                // Get the Python instance and load our downloader script
                val py = Python.getInstance()
                val downloaderModule = py.getModule("downloader")
                
                // Call python function download_video(url, download_path, filename) -> returns dict
                val resultPyObject = downloaderModule.callAttr("download_video", url, targetFolder, filename)
                val resultMap = resultPyObject.asMap()
                
                success = resultMap[com.chaquo.python.PyObject.fromJava("success")]?.toBoolean() ?: false
                extractedTitle = resultMap[com.chaquo.python.PyObject.fromJava("title")]?.toString() ?: ""
                errorMessage = resultMap[com.chaquo.python.PyObject.fromJava("error")]?.toString() ?: "Ошибка yt-dlp"
            } catch (e: Exception) {
                success = false
                errorMessage = e.message ?: "Ошибка инициализации Python/Chaquopy"
            }

            viewModelScope.launch(Dispatchers.Main) {
                isLoading.value = false
                if (success) {
                    downloadStatus.value = "Готово к скачиванию"
                    statusMessage.value = "Скачивание успешно завершено"
                    urlInput.value = "" // Clear input on success
                    Toast.makeText(getApplication(), "Скачивание завершено", Toast.LENGTH_SHORT).show()
                } else {
                    downloadStatus.value = "Ошибка скачивания"
                    statusMessage.value = errorMessage
                    Toast.makeText(getApplication(), "Ошибка: $errorMessage", Toast.LENGTH_LONG).show()
                }
            }

            // Update DB item with results
            val updatedStatus = if (success) "COMPLETED" else "FAILED"
            val finalTitle = if (success && extractedTitle.isNotEmpty()) extractedTitle else "Видео $hash"
            val currentItem = allDownloads.value.find { it.id == dbId } ?: dbItem.copy(id = dbId)
            repository.updateDownload(currentItem.copy(
                status = updatedStatus,
                title = finalTitle
            ))
        }
    }

    private fun startSystemDownload(originalUrl: String, directUrl: String) {
        val context = getApplication<Application>()
        val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager

        // Create filenames matching screenshots
        val hash = originalUrl.md5()
        val filename = "$hash.mp4"
        val title = "Видео $hash"

        try {
            val request = DownloadManager.Request(Uri.parse(directUrl))
                .setTitle("Загрузка видео")
                .setDescription(filename)
                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                .setAllowedOverMetered(true)
                .setAllowedOverRoaming(true)

            // Calculate relative save path
            val relativeDir = getRelativeDownloadDir()
            if (relativeDir.isEmpty()) {
                request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, filename)
            } else {
                request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, "$relativeDir/$filename")
            }

            // Insert placeholder item inside Database
            viewModelScope.launch(Dispatchers.IO) {
                val dbId = repository.insertDownload(
                    DownloadItem(
                        url = originalUrl,
                        title = title,
                        filename = filename,
                        filePath = "${downloadFolder.value}/$filename",
                        status = "DOWNLOADING"
                    )
                ).toInt()

                // Register standard download status listener
                monitorDownloadStatus(downloadManager, dbId)
            }

            Toast.makeText(context, "Загрузка началась", Toast.LENGTH_SHORT).show()

        } catch (e: Exception) {
            isLoading.value = false
            downloadStatus.value = "Ошибка системы"
            statusMessage.value = e.localizedMessage ?: "Не удалось запустить DownloadManager"
        }
    }

    private fun getRelativeDownloadDir(): String {
        val folder = downloadFolder.value
        val prefix = "/storage/emulated/0/Download"
        return if (folder.startsWith(prefix)) {
            folder.removePrefix(prefix).trimStart('/')
        } else {
            ""
        }
    }

    private fun monitorDownloadStatus(downloadManager: DownloadManager, dbId: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            var downloading = true
            var checkCount = 0
            while (downloading && checkCount < 120) { // Timeout after 60 seconds
                delay(500)
                checkCount++
                
                val q = DownloadManager.Query()
                val cursor = downloadManager.query(q)
                if (cursor != null && cursor.moveToFirst()) {
                    var found = false
                    do {
                        val desc = cursor.getString(cursor.getColumnIndex(DownloadManager.COLUMN_DESCRIPTION) ?: 0)
                        val status = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_STATUS) ?: 0)
                        
                        // Compare filename or description
                        val dbItem = allDownloads.value.find { it.id == dbId }
                        if (dbItem != null && desc == dbItem.filename) {
                            found = true
                            if (status == DownloadManager.STATUS_SUCCESSFUL) {
                                repository.updateDownload(dbItem.copy(status = "COMPLETED"))
                                downloading = false
                                viewModelScope.launch(Dispatchers.Main) {
                                    isLoading.value = false
                                    downloadStatus.value = "Готово к скачиванию"
                                    statusMessage.value = "Скачивание успешно завершено"
                                }
                            } else if (status == DownloadManager.STATUS_FAILED) {
                                repository.updateDownload(dbItem.copy(status = "FAILED"))
                                downloading = false
                                viewModelScope.launch(Dispatchers.Main) {
                                    isLoading.value = false
                                    downloadStatus.value = "Сбой загрузки"
                                    statusMessage.value = "DownloadManager сообщил об ошибке"
                                }
                            }
                            break
                        }
                    } while (cursor.moveToNext())
                    cursor.close()
                    if (!found && !downloading) {
                        break
                    }
                } else {
                    cursor?.close()
                }
            }
            
            // Fallback: If timeout or loop ended, set to completed/checked
            if (downloading) {
                val dbItem = allDownloads.value.find { it.id == dbId }
                if (dbItem != null && dbItem.status == "DOWNLOADING") {
                    repository.updateDownload(dbItem.copy(status = "COMPLETED"))
                }
                viewModelScope.launch(Dispatchers.Main) {
                    isLoading.value = false
                    downloadStatus.value = "Готово к скачиванию"
                    statusMessage.value = "Вставьте ссылку и нажмите «Скачать»"
                }
            }
        }
    }

    private fun String.md5(): String {
        return try {
            val md = MessageDigest.getInstance("MD5")
            val bytes = md.digest(this.toByteArray())
            bytes.joinToString("") { "%02x".format(it) }
        } catch (e: Exception) {
            this.hashCode().toString()
        }
    }
}
