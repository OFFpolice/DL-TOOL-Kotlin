package com.example.ui

import android.app.Application
import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.api.CobaltApiService
import com.example.api.CobaltRequest
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
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.security.MessageDigest
import java.util.concurrent.TimeUnit

class DownloadViewModel(application: Application) : AndroidViewModel(application) {

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

    // Retrofit service setup
    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .addInterceptor(HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        })
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl("https://api.cobalt.tools/") // Base fallback, request allows raw @Url
        .client(okHttpClient)
        .addConverterFactory(MoshiConverterFactory.create())
        .build()

    private val apiService = retrofit.create(CobaltApiService::class.java)

    // List of reliable public cobalt instances
    private val apiEndpoints = listOf(
        "https://api.cobalt.tools/api/json",
        "https://cobalt.api.ryb.icu/api/json",
        "https://co.wuk.sh/api/json"
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

        viewModelScope.launch {
            isLoading.value = true
            downloadStatus.value = "Анализ ссылки..."
            statusMessage.value = "Отправка запроса на сервер yt-dlp..."

            var resolvedUrl: String? = null
            var errorMsg: String? = null

            // Try each endpoint sequentially in case of rate limit or block
            for (endpoint in apiEndpoints) {
                try {
                    val request = CobaltRequest(url = url, videoQuality = "1080", isAudioOnly = false)
                    val response = apiService.getDownloadUrl(endpoint, request)
                    if (response.isSuccessful && response.body() != null) {
                        val body = response.body()!!
                        if (body.status == "redirect" || body.status == "stream") {
                            resolvedUrl = body.url
                            break
                        } else if (body.status == "error") {
                            errorMsg = body.text ?: "Неизвестная ошибка API"
                        }
                    }
                } catch (e: Exception) {
                    errorMsg = e.localizedMessage ?: "Ошибка сети"
                }
            }

            if (resolvedUrl != null) {
                downloadStatus.value = "Начало загрузки"
                statusMessage.value = "Передано в DownloadManager"
                startSystemDownload(url, resolvedUrl)
                urlInput.value = "" // Clear input on success
            } else {
                isLoading.value = false
                downloadStatus.value = "Ошибка скачивания"
                statusMessage.value = errorMsg ?: "Не удалось разрешить видео-ссылку. Попробуйте еще раз."
            }
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
