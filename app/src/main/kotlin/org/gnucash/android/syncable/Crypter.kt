package org.gnucash.android.syncable

import com.google.crypto.tink.HybridDecrypt
import com.google.crypto.tink.HybridEncrypt
import com.google.crypto.tink.KeysetHandle
import com.google.crypto.tink.subtle.Base64
import java.nio.charset.StandardCharsets

class Crypter(rxKey: KeysetHandle, txKey: KeysetHandle) {
    private val encrypt = txKey.getPrimitive(HybridEncrypt::class.java)
    fun encryptToSend(message: String) = String(
        Base64.encode(
            encrypt.encrypt(message.toByteArray(StandardCharsets.UTF_8), byteArrayOf()),
            Base64.DEFAULT),
        StandardCharsets.US_ASCII
    )

    private val decrypt = rxKey.getPrimitive(HybridDecrypt::class.java)
    fun decryptReceived(ciphertext: String) = String(
        decrypt.decrypt(Base64.decode(ciphertext, Base64.DEFAULT), byteArrayOf()),
        StandardCharsets.UTF_8
    )
}
