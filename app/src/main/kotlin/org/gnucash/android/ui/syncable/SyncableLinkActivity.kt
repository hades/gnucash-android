package org.gnucash.android.ui.syncable

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.budiyev.android.codescanner.CodeScanner
import com.budiyev.android.codescanner.CodeScannerView
import com.budiyev.android.codescanner.DecodeCallback

private class CodeScannerHolder {
    var scanner: CodeScanner? = null
}

class SyncableLinkActivity : ComponentActivity() {

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val viewModel: SyncableLinkViewModel by viewModels()
        val requestPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            viewModel.cameraPermissionRequestComplete(isGranted)
        }
        ensureCameraPermissions(requestPermissionLauncher, viewModel)
        setContent {
            val state = viewModel.uiState.collectAsState()
            Scaffold(
                topBar = { TopAppBar(title = { Text("Pairing") }) }
            ) { innerPadding ->
                Column(modifier = Modifier.padding(innerPadding)) {
                    when (val stage = state.value.stage) {
                        SyncableLinkStage.WaitingForCameraPermission -> WaitingForCameraUi()
                        SyncableLinkStage.Scanning -> ScanningUi { viewModel.qrCodeScanned(it) }
                        is SyncableLinkStage.Confirming -> ConfirmingUi()
                        is SyncableLinkStage.Failed -> ErrorUi(stage.message)
                    }
                }
            }
        }
    }

    private fun ensureCameraPermissions(
        requestPermissionLauncher: ActivityResultLauncher<String>,
        viewModel: SyncableLinkViewModel
    ) {
        when {
            ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED -> viewModel.cameraPermissionRequestComplete(true)
            else -> requestPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }
}

@Composable
fun WaitingForCameraUi() {
    Text("waiting for camera")
}

@Composable
fun ScanningUi(scannedCallback: (String) -> Unit) {
    val scannerHolder = remember { CodeScannerHolder() }
    AndroidView(
        factory = { context ->
            CodeScannerView(context).apply {
                scannerHolder.scanner = CodeScanner(context, this).apply {
                    decodeCallback = DecodeCallback { scannedCallback(it.text) }
                    startPreview()
                }
            }
        },
    )
}

@Composable
fun ConfirmingUi() {
    Text ( "confirming" )
}

@Composable
fun ErrorUi(msg: String) {
   Text ( "error: $msg")
}