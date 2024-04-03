package org.gnucash.android.ui.syncable

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.executeAsync
import java.security.cert.X509Certificate
import javax.net.ssl.SSLContext
import javax.net.ssl.X509TrustManager

@Serializable
data class SyncableAccount(
    val guid: String,
    val name: String,
    val parent: String,
)

@Serializable
data class SyncableAccountTree(
    val accounts: List<SyncableAccount>,
    val root: String,
)

class SyncableConnection(val hostname: String, val port: Int) {
    private val trustManager = object: X509TrustManager {
        override fun checkClientTrusted(p0: Array<out X509Certificate>?, p1: String?) {
        }

        override fun checkServerTrusted(p0: Array<out X509Certificate>?, p1: String?) {
        }

        override fun getAcceptedIssuers() = arrayOf<X509Certificate>()
    }
    private val sslContext = SSLContext.getInstance("TLS").apply {
        init(null, arrayOf(trustManager), null)
    }
    private val client = with(OkHttpClient.Builder()) {
        sslSocketFactory(sslContext.socketFactory, trustManager)
        hostnameVerifier { _, _ -> true }
        build()
    }

    suspend fun getAccountTree(): SyncableAccountTree {
        val request = with(Request.Builder()) {
            url("https://${hostname}:${port}/accounts")
            build()
        }
        return client.newCall(request).executeAsync().use {
            Json.decodeFromString<SyncableAccountTree>(it.body?.string() ?: "{}")
        }
    }
}
