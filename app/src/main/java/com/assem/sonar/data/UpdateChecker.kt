package com.assem.sonar.data

import android.content.Context
import com.assem.sonar.R
import com.assem.sonar.model.UpdateState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

class UpdateChecker(private val context: Context) {

    private val ua = "Mozilla/5.0 (Linux; Android 13; SM-S911B) AppleWebKit/537.36 " +
        "(KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36"

    suspend fun check(
        packageName: String,
        localVersionName: String,
        localVersionCode: Long,
    ): UpdateState = withContext(Dispatchers.IO) {
        if (!NetPolicy.isAllowed(context)) {
            return@withContext UpdateState.Failed(
                context.getString(R.string.update_internet_disabled),
            )
        }

        val play = runCatching { playStore(packageName) }.getOrNull()
        if (play?.first != null) {
            return@withContext decide(
                localVersionName, localVersionCode,
                play.first!!, play.second, context.getString(R.string.source_play),
            )
        }
        val apto = runCatching { aptoide(packageName) }.getOrNull()
        if (apto?.first != null) {
            return@withContext decide(
                localVersionName, localVersionCode,
                apto.first!!, apto.second, context.getString(R.string.source_aptoide),
            )
        }
        UpdateState.Failed(context.getString(R.string.update_source_unreachable))
    }

    private fun decide(
        localName: String,
        localCode: Long,
        storeName: String,
        storeCode: Long?,
        source: String,
    ): UpdateState {
        val newer = if (storeCode != null && storeCode > 0L) {
            storeCode > localCode
        } else {
            isNewer(storeName, localName)
        }
        return if (newer) UpdateState.Available(storeName, storeCode, source)
        else UpdateState.UpToDate
    }

    /** Returns (versionName, versionCode?) or (null, null) when not found. */
    private fun playStore(packageName: String): Pair<String?, Long?> {
        val url = "https://play.google.com/store/apps/details?id=$packageName&hl=en&gl=us"
        val body = httpGet(url) ?: return null to null
        if (body.contains("requested URL was not found", ignoreCase = true) ||
            body.contains("We're sorry", ignoreCase = true)
        ) {
            return null to null
        }
        val m = Regex("\\[\\[\\[\"([0-9][0-9.]*)\"\\]\\]\\]").find(body)
        return (m?.groupValues?.get(1)) to null
    }

    /** Returns (versionName, versionCode?) or (null, null) when not found. */
    private fun aptoide(packageName: String): Pair<String?, Long?> {
        val url = "https://ws75.aptoide.com/api/7/apps/search?query=$packageName&limit=1"
        val body = httpGet(url) ?: return null to null
        if (!body.contains("\"package\":\"$packageName\"")) return null to null
        val name = Regex("\"vername\":\"([^\"]+)\"").find(body)?.groupValues?.get(1)
        val code = Regex("\"vercode\":(\\d+)").find(body)?.groupValues?.get(1)?.toLongOrNull()
        return name to code
    }

    private fun httpGet(url: String): String? {
        var conn: HttpURLConnection? = null
        return try {
            conn = (URL(url).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 12_000
                readTimeout = 12_000
                instanceFollowRedirects = true
                setRequestProperty("User-Agent", ua)
                setRequestProperty("Accept-Language", "en-US,en;q=0.9")
            }
            val code = conn.responseCode
            if (code !in 200..299) return null
            conn.inputStream.bufferedReader().use { it.readText() }
        } catch (_: Throwable) {
            null
        } finally {
            conn?.disconnect()
        }
    }

    private fun isNewer(store: String, local: String): Boolean {
        val a = store.split('.', '-', ' ').mapNotNull { it.toIntOrNull() }
        val b = local.split('.', '-', ' ').mapNotNull { it.toIntOrNull() }
        if (a.isEmpty() || b.isEmpty()) return false
        for (i in 0 until maxOf(a.size, b.size)) {
            val x = a.getOrElse(i) { 0 }
            val y = b.getOrElse(i) { 0 }
            if (x != y) return x > y
        }
        return false
    }
}
