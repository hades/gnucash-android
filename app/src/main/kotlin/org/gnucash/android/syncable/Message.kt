package org.gnucash.android.syncable

sealed class Message {
    data class AcceptInvite(
        val deviceName: String,
        val publicKeyBase64: String,
        val transportOption: TransportOption,
    )
}
