package org.gnucash.android.ui.syncable

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.gnucash.android.syncable.ConnectionManager
import org.gnucash.android.syncable.InviteData
import org.gnucash.android.syncable.TransportOption
import timber.log.Timber

sealed class SyncableLinkStage {
    data object WaitingForCameraPermission : SyncableLinkStage()
    data object Scanning : SyncableLinkStage()
    data object Confirming: SyncableLinkStage()
    data class Failed(val message: String): SyncableLinkStage()
}

data class SyncableLinkUiState (
    val stage: SyncableLinkStage,
)

class SyncableLinkViewModel() : ViewModel() {
    private val _uiState = MutableStateFlow(SyncableLinkUiState(SyncableLinkStage.WaitingForCameraPermission))
    val uiState: StateFlow<SyncableLinkUiState> = _uiState

    private val manager = ConnectionManager(viewModelScope)

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
            _uiState.value = SyncableLinkUiState(SyncableLinkStage.Failed("malformed QR code data"))
            return
        }
        if (data[0] != "0") {
            _uiState.value = SyncableLinkUiState(SyncableLinkStage.Failed("malformed QR code data"))
            return
        }
        val invite = InviteData(data[0].toInt(), data[1].split(',').map { TransportOption.Http(it) }, data[2])
        manager.acceptInvite(invite)
    }
}