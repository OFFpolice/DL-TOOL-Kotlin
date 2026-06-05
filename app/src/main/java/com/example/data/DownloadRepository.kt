package com.example.data

import kotlinx.coroutines.flow.Flow

class DownloadRepository(private val downloadDao: DownloadDao) {
    val allDownloads: Flow<List<DownloadItem>> = downloadDao.getAllDownloads()

    suspend fun insertDownload(item: DownloadItem): Long {
        return downloadDao.insertDownloadItem(item)
    }

    suspend fun updateDownload(item: DownloadItem) {
        downloadDao.updateDownloadItem(item)
    }

    suspend fun deleteDownload(item: DownloadItem) {
        downloadDao.deleteDownloadItem(item)
    }

    suspend fun deleteDownloadById(id: Int) {
        downloadDao.deleteDownloadItemById(id)
    }
}
