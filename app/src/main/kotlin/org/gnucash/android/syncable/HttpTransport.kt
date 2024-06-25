package org.gnucash.android.syncable

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.executeAsync
import java.security.cert.X509Certificate
import javax.net.ssl.SSLContext
import javax.net.ssl.X509TrustManager

@Serializable
private class AcceptInviteMessage(val type: String, val userAgent: String, val publicKeyBase64: String)

class HttpTransport(private val hostport: String, private val myId: String, private val remoteId: String, private val crypter: Crypter): Transport {
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

    override suspend fun sendMessage(msg: Message) = withContext(Dispatchers.IO) {
        val request = with(Request.Builder()) {
            url("https://${hostport}/syncable/${myId}:${remoteId}")
            post(crypter.encryptToSend(messageToString(msg)).toRequestBody("application/x.syncable".toMediaType()))
            build()
        }
        client.newCall(request).executeAsync().use {  }
    }

    private fun messageToString(msg: Message): String = when (msg) {
        is Message.AcceptInvite -> Json.encodeToString(AcceptInviteMessage("AcceptInvite", userAgent = msg.userAgent, publicKeyBase64 = msg.publicKeyBase64))
    }
}
