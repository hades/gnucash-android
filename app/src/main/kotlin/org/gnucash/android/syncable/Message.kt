package org.gnucash.android.syncable

sealed class Message {
    data class AcceptInvite(
        val userAgent: String,
        val publicKeyBase64: String,
    ): Message()
}
