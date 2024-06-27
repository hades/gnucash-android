package org.gnucash.android.syncable

import android.util.Base64
import com.google.crypto.tink.BinaryKeysetReader
import com.google.crypto.tink.BinaryKeysetWriter
import com.google.crypto.tink.CleartextKeysetHandle
import com.google.crypto.tink.KeysetHandle
import com.google.crypto.tink.hybrid.PredefinedHybridParameters
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream

data class Connection(val remoteId: Int)

sealed class ConnectionState {
    data object Pending : ConnectionState()
    data object Established : ConnectionState()
    data class Failed(val message: String) : ConnectionState()
}

class ConnectionManager(private val scope: CoroutineScope) {
    private val states: MutableMap<Int, MutableStateFlow<ConnectionState>> = mutableMapOf()

    fun getState(connection: Connection): StateFlow<ConnectionState> = when (val state = states[connection.remoteId]) {
        null -> throw IllegalArgumentException("requested connection state not available")
        else -> state.asStateFlow()
    }

    fun acceptInvite(invite: InviteData): Connection {
        val keyBinary = Base64.decode(invite.publicKeyBase64, Base64.DEFAULT)
        val key = CleartextKeysetHandle.read(BinaryKeysetReader.withBytes(keyBinary))
        val remoteId = key.keysetInfo.primaryKeyId
        states[remoteId] = MutableStateFlow(ConnectionState.Pending)
        scope.launch {
            createConnection(key, invite.transportOptions[0])
            states[remoteId]?.update { ConnectionState.Established }
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
