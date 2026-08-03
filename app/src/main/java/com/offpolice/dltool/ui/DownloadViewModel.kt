package com.offpolice.dltool.ui

import android.app.Application
import android.app.DownloadManager
import android.content.Context
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Environment
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.offpolice.dltool.data.AppDatabase
import com.offpolice.dltool.data.DownloadItem
import com.offpolice.dltool.data.DownloadRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.security.MessageDigest
import com.chaquo.python.Python
import com.chaquo.python.android.AndroidPlatform

data class VideoFormatOption(
    val formatId: String,
    val label: String,
    val ext: String,
    val size: String,
    val isAudio: Boolean
)

data class VideoInfo(
    val url: String,
    val title: String,
    val thumbnail: String,
    val uploader: String,
    val duration: String,
    val formats: List<VideoFormatOption>
)

data class YtDlpVersionInfo(
    val currentVersion: String,
    val latestVersion: String,
    val hasUpdate: Boolean
)

open class PyDownloadProgressListener {
    @Volatile
    var cancelledFlag: Boolean = false

    open fun onProgress(downloadedBytes: Long, totalBytes: Long, percent: Float, speedBytesPerSec: Long) {}
    open fun isCancelled(): Boolean = cancelledFlag
}

class DownloadViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getDatabase(application)
    private val repository = DownloadRepository(database.downloadDao())
    private val sharedPrefs = application.getSharedPreferences("dl_tool_prefs", Context.MODE_PRIVATE)

    val urlInput = MutableStateFlow("")
    val downloadStatus = MutableStateFlow("Готов к скачиванию")
    val statusMessage = MutableStateFlow("Вставьте ссылку и нажмите «Скачать»")
    val isLoading = MutableStateFlow(false)
    val videoInfoState = MutableStateFlow<VideoInfo?>(null)

    val isDownloading = MutableStateFlow(false)
    val downloadProgress = MutableStateFlow(0f)
    val activeDownloadTitle = MutableStateFlow("")
    val activeDownloadProgressText = MutableStateFlow("")
    private var activeDownloadJob: Job? = null
    private var currentFilePath: String? = null
    private var currentDbId: Int? = null
    private var currentHash: String? = null
    private var currentProgressListener: PyDownloadProgressListener? = null

    fun cancelDownload() {
        currentProgressListener?.cancelledFlag = true
        activeDownloadJob?.cancel()
        activeDownloadJob = null

        val filePath = currentFilePath
        val folder = downloadFolder.value
        val hash = currentHash
        if (filePath != null) {
            deletePartialFiles(folder, hash ?: "", filePath)
        }

        val dbId = currentDbId
        if (dbId != null) {
            viewModelScope.launch(Dispatchers.IO) {
                repository.deleteDownloadById(dbId)
            }
        }

        isDownloading.value = false
        downloadProgress.value = 0f
        isLoading.value = false
        downloadStatus.value = "Загрузка отменена"
        statusMessage.value = "Вставьте ссылку и нажмите «Скачать»"
        Toast.makeText(getApplication(), "Загрузка отменена", Toast.LENGTH_SHORT).show()
    }

    private fun deletePartialFiles(targetFolder: String, baseName: String, fullFilePath: String) {
        try {
            val mainFile = java.io.File(fullFilePath)
            if (mainFile.exists()) mainFile.delete()

            val partFile = java.io.File("$fullFilePath.part")
            if (partFile.exists()) partFile.delete()

            val ytdlFile = java.io.File("$fullFilePath.ytdl")
            if (ytdlFile.exists()) ytdlFile.delete()

            if (targetFolder.isNotEmpty()) {
                val dir = java.io.File(targetFolder)
                if (dir.exists() && dir.isDirectory) {
                    val searchPrefix = if (baseName.isNotEmpty()) baseName.substringBeforeLast(".") else ""
                    dir.listFiles()?.forEach { f ->
                        if (searchPrefix.isNotEmpty() && f.name.contains(searchPrefix)) {
                            try { f.delete() } catch (_: Exception) {}
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    companion object {
        val DEFAULT_DOWNLOAD_PATH: String =
            "${Environment.getExternalStorageDirectory().absolutePath}/Download/DL-TOOL/video"
    }

    val downloadFolder = MutableStateFlow(
        sharedPrefs.getString("download_folder", DEFAULT_DOWNLOAD_PATH) ?: DEFAULT_DOWNLOAD_PATH
    )

    val isBlackThemeEnabled = MutableStateFlow(
        sharedPrefs.getBoolean("black_theme_enabled", false)
    )

    fun setBlackThemeEnabled(enabled: Boolean) {
        isBlackThemeEnabled.value = enabled
        sharedPrefs.edit().putBoolean("black_theme_enabled", enabled).apply()
    }

    val isYtDlpAutoCheckEnabled = MutableStateFlow(
        sharedPrefs.getBoolean("yt_dlp_auto_check", true)
    )

    val ytDlpVersionInfo = MutableStateFlow<YtDlpVersionInfo?>(null)
    val isCheckingYtDlp = MutableStateFlow(false)
    val isUpdatingYtDlp = MutableStateFlow(false)
    val showYtDlpUpdateDialog = MutableStateFlow(false)

    fun setYtDlpAutoCheckEnabled(enabled: Boolean) {
        isYtDlpAutoCheckEnabled.value = enabled
        sharedPrefs.edit().putBoolean("yt_dlp_auto_check", enabled).apply()
    }

    fun dismissYtDlpUpdateDialog() {
        showYtDlpUpdateDialog.value = false
    }

    fun checkYtDlpVersion(isManual: Boolean = false) {
        viewModelScope.launch(Dispatchers.IO) {
            isCheckingYtDlp.value = true
            try {
                val py = Python.getInstance()
                val downloader = py.getModule("downloader")
                val res = downloader.callAttr("check_ytdlp_version")
                val isSuccess = res.get("success")?.toBoolean() ?: false
                val currentVer = res.get("current_version")?.toString() ?: "Неизвестно"
                val latestVer = res.get("latest_version")?.toString() ?: currentVer
                val hasUpdate = res.get("has_update")?.toBoolean() ?: false

                val info = YtDlpVersionInfo(currentVer, latestVer, hasUpdate)
                ytDlpVersionInfo.value = info

                if (hasUpdate) {
                    if (isManual || isYtDlpAutoCheckEnabled.value) {
                        showYtDlpUpdateDialog.value = true
                    }
                } else if (isManual) {
                    viewModelScope.launch(Dispatchers.Main) {
                        Toast.makeText(getApplication(), "У вас установлена последняя версия yt-dlp ($currentVer)", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                if (isManual) {
                    viewModelScope.launch(Dispatchers.Main) {
                        Toast.makeText(getApplication(), "Ошибка проверки версии: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            } finally {
                isCheckingYtDlp.value = false
            }
        }
    }

    fun updateYtDlp() {
        viewModelScope.launch(Dispatchers.IO) {
            isUpdatingYtDlp.value = true
            try {
                val py = Python.getInstance()
                val downloader = py.getModule("downloader")
                val res = downloader.callAttr("update_ytdlp_package")
                val isSuccess = res.get("success")?.toBoolean() ?: false
                val newVer = res.get("new_version")?.toString() ?: ""
                val err = res.get("error")?.toString() ?: ""

                if (isSuccess && newVer.isNotEmpty()) {
                    ytDlpVersionInfo.value = YtDlpVersionInfo(newVer, newVer, false)
                    showYtDlpUpdateDialog.value = false
                    viewModelScope.launch(Dispatchers.Main) {
                        Toast.makeText(getApplication(), "yt-dlp успешно обновлен до версии $newVer!", Toast.LENGTH_LONG).show()
                    }
                } else {
                    viewModelScope.launch(Dispatchers.Main) {
                        Toast.makeText(getApplication(), "Ошибка обновления yt-dlp: $err", Toast.LENGTH_LONG).show()
                    }
                }
            } catch (e: Exception) {
                viewModelScope.launch(Dispatchers.Main) {
                    Toast.makeText(getApplication(), "Ошибка обновления: ${e.message}", Toast.LENGTH_LONG).show()
                }
            } finally {
                isUpdatingYtDlp.value = false
            }
        }
    }

    val allDownloads: StateFlow<List<DownloadItem>> = repository.allDownloads.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    init {
        val context = getApplication<Application>()
        if (!Python.isStarted()) {
            Python.start(AndroidPlatform(context))
        }
        if (sharedPrefs.getBoolean("yt_dlp_auto_check", true)) {
            checkYtDlpVersion(isManual = false)
        }
    }

    fun onUrlChange(newUrl: String) {
        val trimmed = newUrl.trim()
        if (trimmed.isEmpty()) {
            urlInput.value = ""
            return
        }

        // If whole string was pasted with surrounding spaces, handle trimmed value
        val targetUrl = if (newUrl.startsWith("http://") || newUrl.startsWith("https://") || newUrl.startsWith("HTTP://") || newUrl.startsWith("HTTPS://")) {
            trimmed
        } else {
            newUrl
        }

        // Forbid spaces, control characters, emojis, or non-ASCII characters
        for (ch in targetUrl) {
            if (Character.isWhitespace(ch) || Character.isISOControl(ch)) return
            val type = Character.getType(ch)
            if (type == Character.SURROGATE.toInt() ||
                type == Character.OTHER_SYMBOL.toInt() ||
                type == Character.NON_SPACING_MARK.toInt() ||
                ch.code > 127
            ) {
                return
            }
        }

        val lower = targetUrl.lowercase()
        val isBuildingHttp = "http://".startsWith(lower)
        val isBuildingHttps = "https://".startsWith(lower)
        val startsWithHttp = lower.startsWith("http://") || lower.startsWith("https://")

        if (isBuildingHttp || isBuildingHttps || startsWithHttp) {
            urlInput.value = targetUrl
        }
    }

    fun saveDownloadFolder(newFolder: String) {
        downloadFolder.value = newFolder
        sharedPrefs.edit().putString("download_folder", newFolder).apply()
        Toast.makeText(getApplication(), "Папка сохранена", Toast.LENGTH_SHORT).show()
    }

    fun restoreDefaultFolder() {
        downloadFolder.value = DEFAULT_DOWNLOAD_PATH
        sharedPrefs.edit().putString("download_folder", DEFAULT_DOWNLOAD_PATH).apply()
        Toast.makeText(getApplication(), "Сброшено: Download/DL-TOOL/video", Toast.LENGTH_SHORT).show()
    }

    fun clearHistory() {
        viewModelScope.launch(Dispatchers.IO) {
            allDownloads.value.forEach { item ->
                try {
                    val f = java.io.File(item.filePath)
                    if (f.exists()) f.delete()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                try {
                    MediaScannerConnection.scanFile(
                        getApplication(),
                        arrayOf(item.filePath),
                        null,
                        null
                    )
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                repository.deleteDownload(item)
            }
        }
    }

    fun deleteItem(id: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            val item = allDownloads.value.find { it.id == id }
            if (item != null) {
                try {
                    val f1 = java.io.File(item.filePath)
                    if (f1.exists()) {
                        f1.delete()
                    }
                    val publicDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                    val f2 = java.io.File(publicDir, item.filename)
                    if (f2.exists()) {
                        f2.delete()
                    }
                    val f3 = java.io.File(publicDir, "DL-TOOL/video/${item.filename}")
                    if (f3.exists()) {
                        f3.delete()
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }

                try {
                    MediaScannerConnection.scanFile(
                        getApplication(),
                        arrayOf(item.filePath),
                        null,
                        null
                    )
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            repository.deleteDownloadById(id)
        }
    }

    fun dismissVideoInfo() {
        videoInfoState.value = null
    }

    fun downloadVideo() {
        fetchVideoInfo()
    }

    fun fetchVideoInfo() {
        val url = urlInput.value.trim()
        if (url.isEmpty()) {
            downloadStatus.value = "Ошибка"
            statusMessage.value = "Поле ввода ссылки пустое"
            Toast.makeText(getApplication(), "Введите ссылку на видео", Toast.LENGTH_SHORT).show()
            return
        }

        if (!url.startsWith("http://") && !url.startsWith("https://") && !url.startsWith("HTTP://") && !url.startsWith("HTTPS://")) {
            downloadStatus.value = "Ошибка"
            statusMessage.value = "Ссылка должна начинаться с http:// или https://"
            Toast.makeText(getApplication(), "Ссылка должна начинаться с http:// или https://", Toast.LENGTH_SHORT).show()
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            viewModelScope.launch(Dispatchers.Main) {
                isLoading.value = true
                downloadStatus.value = "Получение информации..."
                statusMessage.value = "Извлечение превью и доступных форматов..."
            }

            try {
                val py = Python.getInstance()
                val downloaderModule = py.getModule("downloader")
                val resultPyObject = downloaderModule.callAttr("get_video_info", url)
                val resultMap = resultPyObject.asMap()

                val success = resultMap[com.chaquo.python.PyObject.fromJava("success")]?.toBoolean() ?: false
                val title = resultMap[com.chaquo.python.PyObject.fromJava("title")]?.toString() ?: "Видео"
                val thumbnail = resultMap[com.chaquo.python.PyObject.fromJava("thumbnail")]?.toString() ?: ""
                val uploader = resultMap[com.chaquo.python.PyObject.fromJava("uploader")]?.toString() ?: ""
                val duration = resultMap[com.chaquo.python.PyObject.fromJava("duration")]?.toString() ?: ""
                val errorMsg = resultMap[com.chaquo.python.PyObject.fromJava("error")]?.toString() ?: "Ошибка получения данных"

                if (success) {
                    val formatsPyList = resultMap[com.chaquo.python.PyObject.fromJava("formats")]?.asList()
                    val options = mutableListOf<VideoFormatOption>()

                    formatsPyList?.forEach { item ->
                        val itemMap = item.asMap()
                        options.add(
                            VideoFormatOption(
                                formatId = itemMap[com.chaquo.python.PyObject.fromJava("format_id")]?.toString() ?: "best",
                                label = itemMap[com.chaquo.python.PyObject.fromJava("label")]?.toString() ?: "Качество",
                                ext = itemMap[com.chaquo.python.PyObject.fromJava("ext")]?.toString() ?: "mp4",
                                size = itemMap[com.chaquo.python.PyObject.fromJava("size")]?.toString() ?: "Размер н/д",
                                isAudio = itemMap[com.chaquo.python.PyObject.fromJava("is_audio")]?.toBoolean() ?: false
                            )
                        )
                    }

                    val info = VideoInfo(
                        url = url,
                        title = title,
                        thumbnail = thumbnail,
                        uploader = uploader,
                        duration = duration,
                        formats = options
                    )

                    viewModelScope.launch(Dispatchers.Main) {
                        isLoading.value = false
                        videoInfoState.value = info
                    }
                } else {
                    viewModelScope.launch(Dispatchers.Main) {
                        isLoading.value = false
                        downloadStatus.value = "Ошибка"
                        statusMessage.value = errorMsg
                        Toast.makeText(getApplication(), "Ошибка: $errorMsg", Toast.LENGTH_LONG).show()
                    }
                }
            } catch (e: Exception) {
                viewModelScope.launch(Dispatchers.Main) {
                    isLoading.value = false
                    downloadStatus.value = "Ошибка"
                    statusMessage.value = e.message ?: "Ошибка получения данных"
                    Toast.makeText(getApplication(), "Ошибка: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    fun confirmDownload(option: VideoFormatOption) {
        val info = videoInfoState.value ?: return
        val url = info.url
        videoInfoState.value = null

        val listener = object : PyDownloadProgressListener() {
            override fun onProgress(downloadedBytes: Long, totalBytes: Long, percent: Float, speedBytesPerSec: Long) {
                val frac = (percent / 100f).coerceIn(0f, 1f)
                downloadProgress.value = frac

                val downloadedMb = downloadedBytes / (1024.0 * 1024.0)
                val totalMb = totalBytes / (1024.0 * 1024.0)

                val speedStr = if (speedBytesPerSec < 1024 * 1024) {
                    "%.0f КБ/с".format(speedBytesPerSec / 1024.0)
                } else {
                    "%.1f МБ/с".format(speedBytesPerSec / (1024.0 * 1024.0))
                }

                val text = if (totalBytes > 0) {
                    "%.1f МБ из %.1f МБ (%.0f%%) • %s".format(downloadedMb, totalMb, percent, speedStr)
                } else {
                    "%.1f МБ • %s".format(downloadedMb, speedStr)
                }

                activeDownloadProgressText.value = text
            }

            override fun isCancelled(): Boolean = cancelledFlag
        }
        currentProgressListener = listener

        activeDownloadJob = viewModelScope.launch(Dispatchers.IO) {
            viewModelScope.launch(Dispatchers.Main) {
                isLoading.value = true
                isDownloading.value = true
                downloadProgress.value = 0f
                activeDownloadTitle.value = info.title.ifEmpty { "Загрузка видео..." }
                activeDownloadProgressText.value = "Подготовка к скачиванию..."
                downloadStatus.value = "Загрузка..."
                statusMessage.value = "Скачивание [${option.label}]..."
            }

            val hash = url.md5()
            currentHash = hash
            val filename = "$hash.${option.ext}"
            var targetFolder = downloadFolder.value
            if (targetFolder.isEmpty()) {
                targetFolder = DEFAULT_DOWNLOAD_PATH
            }
            val fullFilePath = "$targetFolder/$filename"
            currentFilePath = fullFilePath

            val dbItem = DownloadItem(
                url = url,
                title = info.title.ifEmpty { "Видео $hash" },
                filename = filename,
                filePath = fullFilePath,
                status = "DOWNLOADING"
            )
            val dbId = repository.insertDownload(dbItem).toInt()
            currentDbId = dbId

            var success = false
            var extractedTitle = info.title
            var errorMessage = "Неизвестная ошибка"

            try {
                val py = Python.getInstance()
                val downloaderModule = py.getModule("downloader")
                val resultPyObject = downloaderModule.callAttr("download_video", url, targetFolder, filename, option.formatId, listener)
                val resultMap = resultPyObject.asMap()

                success = resultMap[com.chaquo.python.PyObject.fromJava("success")]?.toBoolean() ?: false
                val pyTitle = resultMap[com.chaquo.python.PyObject.fromJava("title")]?.toString() ?: ""
                if (pyTitle.isNotEmpty()) extractedTitle = pyTitle
                errorMessage = resultMap[com.chaquo.python.PyObject.fromJava("error")]?.toString() ?: "Ошибка yt-dlp"
            } catch (e: Exception) {
                success = false
                errorMessage = e.message ?: "Ошибка загрузки"
            }

            if (listener.cancelledFlag || errorMessage == "DOWNLOAD_CANCELLED") {
                deletePartialFiles(targetFolder, hash, fullFilePath)
                repository.deleteDownloadById(dbId)
                return@launch
            }

            if (success) {
                try {
                    MediaScannerConnection.scanFile(
                        getApplication(),
                        arrayOf(fullFilePath),
                        arrayOf("video/*", "audio/*"),
                        null
                    )
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            viewModelScope.launch(Dispatchers.Main) {
                isLoading.value = false
                isDownloading.value = false
                downloadProgress.value = 0f
                if (success) {
                    downloadStatus.value = "Готово к скачиванию"
                    statusMessage.value = "Скачивание успешно завершено"
                    urlInput.value = ""
                    Toast.makeText(getApplication(), "Скачивание завершено!", Toast.LENGTH_SHORT).show()
                } else {
                    downloadStatus.value = "Ошибка скачивания"
                    statusMessage.value = errorMessage
                    Toast.makeText(getApplication(), "Ошибка: $errorMessage", Toast.LENGTH_LONG).show()
                }
            }

            val updatedStatus = if (success) "COMPLETED" else "FAILED"
            val finalTitle = if (extractedTitle.isNotEmpty()) extractedTitle else "Видео $hash"
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
