package com.txnrelay.util

import com.txnrelay.BuildConfig
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

object UrlValidator {
    /** Valid http(s) URL; plain http is only accepted in debug builds. */
    fun isValid(raw: String): Boolean {
        val url = raw.trim().toHttpUrlOrNull() ?: return false
        return url.host.isNotBlank() && (url.isHttps || BuildConfig.DEBUG)
    }
}
