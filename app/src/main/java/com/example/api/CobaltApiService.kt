package com.example.api

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Headers
import retrofit2.http.POST
import retrofit2.http.Url

data class CobaltRequest(
    val url: String,
    val videoQuality: String = "1080",
    val isAudioOnly: Boolean = false,
    val downloadMode: String = "auto"
)

data class PickerItem(
    val url: String,
    val type: String?,
    val quality: String?
)

data class CobaltResponse(
    val status: String,
    val url: String? = null,
    val text: String? = null,
    val picker: List<PickerItem>? = null
)

interface CobaltApiService {
    @Headers(
        "Accept: application/json",
        "Content-Type: application/json"
    )
    @POST
    suspend fun getDownloadUrl(
        @Url endpoint: String,
        @Body request: CobaltRequest
    ): Response<CobaltResponse>
}
