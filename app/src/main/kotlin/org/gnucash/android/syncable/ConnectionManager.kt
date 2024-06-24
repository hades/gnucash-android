package org.gnucash.android.syncable

import android.util.Base64
import com.google.crypto.tink.BinaryKeysetReader
import com.google.crypto.tink.CleartextKeysetHandle
import com.google.crypto.tink.integration.android.AndroidKeysetManager
import timber.log.Timber

class Connection(private val transport: Transport) {

}

class ConnectionManager {

    fun pair(invite: InviteData): Connection {
        val keyBinary = Base64.decode(invite.publicKeyBase64, Base64.DEFAULT)
        val key = CleartextKeysetHandle.read(BinaryKeysetReader.withBytes(keyBinary))
        Timber.d("key info: %s", key.keysetInfo)
    }
}
