package org.gnucash.android.ui.syncable

import android.util.Base64
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.fzakaria.ascii85.Ascii85
import com.google.crypto.tink.BinaryKeysetReader
import com.google.crypto.tink.CleartextKeysetHandle
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import timber.log.Timber

sealed class SyncableLinkStage {
    object WaitingForCameraPermission : SyncableLinkStage()
    object Scanning : SyncableLinkStage()
    class Confirming: SyncableLinkStage()
    class Failed(val message: String): SyncableLinkStage()
}

data class SyncableLinkUiState (
    val stage: SyncableLinkStage,
)

class SyncableLinkViewModel() : ViewModel() {
    private val _uiState = MutableStateFlow(SyncableLinkUiState(SyncableLinkStage.WaitingForCameraPermission))
    val uiState: StateFlow<SyncableLinkUiState> = _uiState

    fun cameraPermissionRequestComplete(granted: Boolean) {
        if (granted) {
            _uiState.value = SyncableLinkUiState(SyncableLinkStage.Scanning)
        } else {
            _uiState.value = SyncableLinkUiState(SyncableLinkStage.Failed("Camera permission was not granted"))
        }
    }

    fun qrCodeScanned(encodedData: String) {
        val data = encodedData.split(';', ignoreCase = false, limit = 3)
        if (data.size < 3) {
            Timber.e("malformed data: %s", data)
            return
        }
        val keyEncoded = data[2]
        val keyBinary = Base64.decode(keyEncoded, Base64.DEFAULT)
        val key = CleartextKeysetHandle.read(BinaryKeysetReader.withBytes(keyBinary))
        Timber.d("key info: %s", key.keysetInfo)
    }
}