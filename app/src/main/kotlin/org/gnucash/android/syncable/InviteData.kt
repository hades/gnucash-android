package org.gnucash.android.syncable

sealed class TransportOption {
    class Http(val hostport: String): TransportOption()
}

data class InviteData(
    val version: Int,
    val transportOptions: List<TransportOption>,
    val publicKeyBase64: String,
)
