package org.example.project

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

private enum class CameraPermissionState { Idle, Requesting, Granted, Denied }
private enum class ScannerState { Permission, Starting, Scanning, Result, CameraUnavailable, InvalidQr, CameraError }

private data class ValidatedQr(val rawValue: String, val displayValue: String)

@Composable
internal actual fun ABAScanner(onClose: () -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var permissionState by remember { mutableStateOf(CameraPermissionState.Idle) }
    var scannerState by remember { mutableStateOf(ScannerState.Permission) }
    var result by remember { mutableStateOf<ValidatedQr?>(null) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        permissionState = if (granted) CameraPermissionState.Granted else CameraPermissionState.Denied
        scannerState = if (granted) ScannerState.Starting else ScannerState.Permission
    }

    LaunchedEffect(Unit) {
        permissionState = CameraPermissionState.Requesting
        if (ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            permissionState = CameraPermissionState.Granted
            scannerState = ScannerState.Starting
        } else {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    when {
        permissionState == CameraPermissionState.Denied -> PermissionDeniedState(
            onTryAgain = {
                permissionState = CameraPermissionState.Requesting
                permissionLauncher.launch(Manifest.permission.CAMERA)
            },
            onSettings = {
                context.startActivity(
                    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = Uri.parse("package:${context.packageName}")
                    }
                )
            },
            onCancel = onClose
        )

        permissionState == CameraPermissionState.Requesting -> ScannerMessage("Requesting camera access…", onClose)
        permissionState == CameraPermissionState.Granted && scannerState == ScannerState.Result && result != null -> ScanResultView(
            result!!,
            onClose
        )

        permissionState == CameraPermissionState.Granted -> AndroidCameraView(
            context = context,
            lifecycleOwner = lifecycleOwner,
            scannerState = scannerState,
            onStateChange = { scannerState = it },
            onResult = { result = it; scannerState = ScannerState.Result },
            onClose = onClose
        )

        else -> ScannerMessage("Camera access is required to scan a QR code.", onClose)
    }
}

@Composable
private fun AndroidCameraView(
    context: Context,
    lifecycleOwner: androidx.lifecycle.LifecycleOwner,
    scannerState: ScannerState,
    onStateChange: (ScannerState) -> Unit,
    onResult: (ValidatedQr) -> Unit,
    onClose: () -> Unit
) {
    val previewView = remember { PreviewView(context).apply { scaleType = PreviewView.ScaleType.FILL_CENTER } }
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }
    var camera by remember { mutableStateOf<Camera?>(null) }
    var torchOn by remember { mutableStateOf(false) }

    DisposableEffect(previewView) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        val scanner = BarcodeScanning.getClient(
            BarcodeScannerOptions.Builder().setBarcodeFormats(Barcode.FORMAT_QR_CODE).build()
        )
        val hasDetected = AtomicBoolean(false)
        val mainExecutor = ContextCompat.getMainExecutor(context)

        val listener = Runnable {
            try {
                val provider = cameraProviderFuture.get()
                if (!provider.hasCamera(CameraSelector.DEFAULT_BACK_CAMERA)) {
                    onStateChange(ScannerState.CameraUnavailable)
                    return@Runnable
                }
                val preview = Preview.Builder().build().also { it.surfaceProvider = previewView.surfaceProvider }
                val analysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()
                analysis.setAnalyzer(cameraExecutor, QrAnalyzer(scanner, hasDetected) { value ->
                    val validated = validateQr(value)
                    mainExecutor.execute {
                        if (validated != null) onResult(validated) else onStateChange(ScannerState.InvalidQr)
                    }
                })
                provider.unbindAll()
                camera = provider.bindToLifecycle(lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, preview, analysis)
                onStateChange(ScannerState.Scanning)
            } catch (_: Exception) {
                onStateChange(ScannerState.CameraError)
            }
        }
        cameraProviderFuture.addListener(listener, mainExecutor)

        onDispose {
            cameraProviderFuture.addListener({
                runCatching { cameraProviderFuture.get().unbindAll() }
            }, mainExecutor)
            scanner.close()
            cameraExecutor.shutdown()
        }
    }

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        AndroidView(factory = { previewView }, modifier = Modifier.fillMaxSize())
        ScanOverlay(scannerState, torchOn, onTorch = {
            camera?.cameraControl?.enableTorch(!torchOn)
            torchOn = !torchOn
        }, onClose = onClose)
    }
}

private class QrAnalyzer(
    private val scanner: com.google.mlkit.vision.barcode.BarcodeScanner,
    private val hasDetected: AtomicBoolean,
    private val onValue: (String) -> Unit
) : ImageAnalysis.Analyzer {
    override fun analyze(imageProxy: ImageProxy) {
        if (hasDetected.get()) {
            imageProxy.close()
            return
        }
        val image = imageProxy.image
        if (image == null) {
            imageProxy.close()
            return
        }
        scanner.process(InputImage.fromMediaImage(image, imageProxy.imageInfo.rotationDegrees))
            .addOnSuccessListener { barcodes ->
                val value = barcodes.firstOrNull()?.rawValue
                if (!value.isNullOrBlank() && hasDetected.compareAndSet(false, true)) onValue(value)
            }
            .addOnCompleteListener { imageProxy.close() }
    }
}

private fun validateQr(value: String): ValidatedQr? {
    val normalized = value.trim()
    if (normalized.isEmpty() || normalized.length > 4096 || normalized.any { it.code < 0x20 && it != '\n' && it != '\r' && it != '\t' }) return null
    return ValidatedQr(rawValue = normalized, displayValue = normalized.take(180))
}

@Composable
private fun ScanOverlay(state: ScannerState, torchOn: Boolean, onTorch: () -> Unit, onClose: () -> Unit) {
    Box(Modifier.fillMaxSize().padding(20.dp)) {
        Text(
            "×",
            modifier = Modifier.align(Alignment.TopStart).size(48.dp).clickable { onClose() },
            color = Color.White,
            fontSize = 36.sp,
            textAlign = TextAlign.Center
        )
        Text(
            "Scan QR Code",
            modifier = Modifier.align(Alignment.TopCenter).padding(top = 12.dp),
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 21.sp
        )
        Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(Modifier.size(250.dp).border(3.dp, HomeColors.gold, RoundedCornerShape(22.dp)))
            Spacer(Modifier.height(18.dp))
            Text(
                when (state) {
                    ScannerState.InvalidQr -> "This QR code is not supported"
                    ScannerState.CameraError -> "Camera error. Please try again."
                    ScannerState.CameraUnavailable -> "Camera unavailable on this device"
                    ScannerState.Starting -> "Starting camera…"
                    else -> "Place a QR code inside the frame"
                },
                color = Color.White,
                fontSize = 14.sp,
                textAlign = TextAlign.Center
            )
        }
        Button(
            onClick = onTorch,
            modifier = Modifier.align(Alignment.BottomCenter),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xCC8E1238))
        ) {
            Text(if (torchOn) "Turn flashlight off" else "Turn flashlight on")
        }
    }
}

@Composable
private fun PermissionDeniedState(onTryAgain: () -> Unit, onSettings: () -> Unit, onCancel: () -> Unit) {
    ScannerPanel(
        "Camera access is required",
        "Allow camera access in your device settings to scan a QR code.",
        onCancel
    ) {
        Button(
            onClick = onTryAgain,
            colors = ButtonDefaults.buttonColors(
                containerColor = HomeColors.gold,
                contentColor = HomeColors.deepCrimson
            )
        ) { Text("Try Again") }
        Button(
            onClick = onSettings,
            colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = HomeColors.deepCrimson)
        ) { Text("Open Settings") }
    }
}

@Composable
private fun ScannerMessage(message: String, onClose: () -> Unit) {
    ScannerPanel("ABA Scan", message, onClose) {}
}

@Composable
private fun ScanResultView(result: ValidatedQr, onClose: () -> Unit) {
    ScannerPanel("Review QR Code", "Verify this information before continuing.", onClose) {
        Box(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(Color(0x33FFFFFF)).padding(16.dp)
        ) { Text(result.displayValue, color = Color.White, fontSize = 14.sp) }
        Button(
            onClick = onClose,
            colors = ButtonDefaults.buttonColors(
                containerColor = HomeColors.gold,
                contentColor = HomeColors.deepCrimson
            )
        ) { Text("Done") }
    }
}

@Composable
private fun ScannerPanel(
    title: String,
    message: String,
    onClose: () -> Unit,
    actions: @Composable ColumnScope.() -> Unit
) {
    Box(Modifier.fillMaxSize().background(HomeColors.deepCrimson).padding(24.dp)) {
        Text(
            "×",
            modifier = Modifier.align(Alignment.TopStart).size(48.dp).clickable { onClose() },
            color = Color.White,
            fontSize = 36.sp,
            textAlign = TextAlign.Center
        )
        Column(
            Modifier.align(Alignment.Center).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                title,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 24.sp,
                textAlign = TextAlign.Center
            )
            Text(message, color = Color(0xFFFFD7DD), fontSize = 15.sp, textAlign = TextAlign.Center)
            actions()
            Button(
                onClick = onClose,
                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent, contentColor = Color.White)
            ) { Text("Cancel") }
        }
    }
}
