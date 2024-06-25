package org.gnucash.android.syncable

import android.util.Base64
import com.google.crypto.tink.BinaryKeysetReader
import com.google.crypto.tink.BinaryKeysetWriter
import com.google.crypto.tink.CleartextKeysetHandle
import com.google.crypto.tink.KeysetHandle
import com.google.crypto.tink.hybrid.PredefinedHybridParameters
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream

data class Connection(private val remoteId: Int)

class ConnectionManager(private val scope: CoroutineScope) {

    fun acceptInvite(invite: InviteData): Connection {
        val keyBinary = Base64.decode(invite.publicKeyBase64, Base64.DEFAULT)
        val key = CleartextKeysetHandle.read(BinaryKeysetReader.withBytes(keyBinary))
        val remoteId = key.keysetInfo.primaryKeyId
        scope.launch {
            createConnection(key, invite.transportOptions[0])
        }
        return Connection(remoteId)
    }

    private suspend fun createConnection(remoteKey: KeysetHandle, transportOption: TransportOption) = withContext(Dispatchers.Default) {
        val myKey = KeysetHandle.generateNew(PredefinedHybridParameters.ECIES_P256_HKDF_HMAC_SHA256_AES128_GCM)
        val myPublicKey = myKey.publicKeysetHandle
        val myId = String.format("%08x", myPublicKey.keysetInfo.primaryKeyId)
        val myPublicKeyData = ByteArrayOutputStream()
        myPublicKey.writeNoSecret(BinaryKeysetWriter.withOutputStream(myPublicKeyData))
        val myPublicKeyBase64 = Base64.encode(myPublicKeyData.toByteArray(), Base64.DEFAULT)
        val crypter = Crypter(rxKey = myKey, txKey = remoteKey)
        val transport = makeTransport(transportOption, myId = myId, remoteId = String.format("%08x", remoteKey.keysetInfo.primaryKeyId), crypter)
        transport.sendMessage(Message.AcceptInvite("android", String(myPublicKeyBase64)))
    }

    private fun makeTransport(
        transportOption: TransportOption,
        myId: String,
        remoteId: String,
        crypter: Crypter
    ) = when(transportOption) {
        is TransportOption.Http -> HttpTransport(transportOption.hostport, myId = myId, remoteId = remoteId, crypter)
    }
}
