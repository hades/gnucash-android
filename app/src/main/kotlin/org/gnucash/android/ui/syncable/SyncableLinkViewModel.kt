package org.gnucash.android.ui.syncable

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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

    fun qrCodeScanned(data: String) {
        Timber.d("scanned message: %s", data)
    }
}