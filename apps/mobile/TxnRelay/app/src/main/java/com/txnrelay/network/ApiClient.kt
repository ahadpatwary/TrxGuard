package com.txnrelay.network

import com.txnrelay.BuildConfig
import com.txnrelay.config.AppConfig
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.concurrent.TimeUnit

/** Builds the HTTP stack. Swap this file (and RetrofitEventUploader) to change networking library. */
fun createEventApi(): EventApi {
    val json = Json {
        encodeDefaults = true
        ignoreUnknownKeys = true
    }

    val client = OkHttpClient.Builder()
        .connectTimeout(AppConfig.CONNECT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .readTimeout(AppConfig.READ_WRITE_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .writeTimeout(AppConfig.READ_WRITE_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .callTimeout(AppConfig.CALL_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .apply {
            if (BuildConfig.DEBUG) {
                // BASIC = method/url/status only. Never log bodies: they contain payment messages.
                addInterceptor(
                    HttpLoggingInterceptor().apply {
                        level = HttpLoggingInterceptor.Level.BASIC
                        redactHeader("Authorization")
                    },
                )
            }
        }
        .build()

    return Retrofit.Builder()
        .baseUrl("http://localhost/") // placeholder, real URL is passed with @Url
        .client(client)
        .addConverterFactory(json.asConverterFactory("application/json; charset=UTF-8".toMediaType()))
        .build()
        .create(EventApi::class.java)
}
