package com.txnrelay.network

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Url

/** Retrofit definition. The URL is user-configurable, so it is passed per call via @Url. */
interface EventApi {
    @POST
    suspend fun postEvent(
        @Url url: String,
        @Header("Idempotency-Key") idempotencyKey: String,
        // Change the header name/scheme here if your backend uses e.g. "X-API-Key". Null = header omitted.
        @Header("Authorization") authorization: String?,
        @Body payload: EventPayload,
    ): Response<Unit>
}
