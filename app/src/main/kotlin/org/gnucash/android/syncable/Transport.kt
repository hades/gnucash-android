package org.gnucash.android.syncable

interface Transport {
    suspend fun sendMessage(msg: Message)
}
