package com.example.kameraku

import android.Manifest
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.ui.unit.dp
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import coil.compose.AsyncImage
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            CameraScreen()
        }
    }
}

@Composable
fun CameraScreen() {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var previewView by remember { mutableStateOf<PreviewView?>(null) }
    var camera by remember { mutableStateOf<Camera?>(null) }
    var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }
    var isTorchOn by remember { mutableStateOf(false) }
    var lastPhotoUri by remember { mutableStateOf<Uri?>(null) }

    var cameraSelector by remember { mutableStateOf(CameraSelector.DEFAULT_BACK_CAMERA) }

    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)
                    == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> hasPermission = granted }

    LaunchedEffect(Unit) {
        if (!hasPermission) permissionLauncher.launch(Manifest.permission.CAMERA)
    }

    LaunchedEffect(hasPermission, previewView, cameraSelector) {
        if (!hasPermission) return@LaunchedEffect
        val pv = previewView ?: return@LaunchedEffect

        val provider = suspendCancellableCoroutine<ProcessCameraProvider> { cont ->
            val f = ProcessCameraProvider.getInstance(context)
            f.addListener({ cont.resume(f.get()) }, ContextCompat.getMainExecutor(context))
        }

        provider.unbindAll()

        val preview = Preview.Builder().build().also {
            it.setSurfaceProvider(pv.surfaceProvider)
        }

        val cam = provider.bindToLifecycle(lifecycleOwner, cameraSelector, preview)
        camera = cam

        val ic = ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
            .build()

        provider.unbindAll()
        provider.bindToLifecycle(lifecycleOwner, cameraSelector, preview, ic)
        imageCapture = ic

        isTorchOn = false
    }

    Box(modifier = Modifier.fillMaxSize()) {
        if (hasPermission) {
            CameraPreview { view ->
                previewView = view
            }
        }

        if (lastPhotoUri != null) {
            AsyncImage(
                model = lastPhotoUri,
                contentDescription = null,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(20.dp)
                    .size(80.dp)
                    .clip(CircleShape)
                    .border(2.dp, androidx.compose.ui.graphics.Color.White, CircleShape)
            )
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Button(onClick = {
                imageCapture?.let { ic ->
                    takePhoto(context, ic) { uri ->
                        lastPhotoUri = uri
                        Log.d("CameraScreen", "Saved: $uri")
                        Toast.makeText(context, "Foto Disimpan!", Toast.LENGTH_SHORT).show()
                    }
                }
            }) {
                Text("Ambil Foto")
            }

            Spacer(modifier = Modifier.height(12.dp))

            Button(onClick = {
                camera?.cameraControl?.enableTorch(!isTorchOn)
                isTorchOn = !isTorchOn
            }) {
                Text(if (isTorchOn) "Flash OFF" else "Flash ON")
            }

            Spacer(modifier = Modifier.height(12.dp))

            Button(onClick = {
                cameraSelector =
                    if (cameraSelector == CameraSelector.DEFAULT_BACK_CAMERA)
                        CameraSelector.DEFAULT_FRONT_CAMERA
                    else
                        CameraSelector.DEFAULT_BACK_CAMERA
            }) {
                Text("Ganti Kamera")
            }
        }
    }
}

@Composable
fun CameraPreview(onPreviewReady: (PreviewView) -> Unit) {
    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { context ->
            PreviewView(context).apply {
                scaleType = PreviewView.ScaleType.FILL_CENTER
                post { onPreviewReady(this) }
            }
        }
    )
}

fun outputOptions(ctx: Context, name: String): ImageCapture.OutputFileOptions {
    val v = ContentValues().apply {
        put(MediaStore.MediaColumns.DISPLAY_NAME, name)
        put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
        put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/KameraKu")
    }
    val r = ctx.contentResolver
    return ImageCapture.OutputFileOptions.Builder(
        r,
        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
        v
    ).build()
}

fun takePhoto(
    ctx: Context,
    ic: ImageCapture,
    onSaved: (Uri) -> Unit
) {
    val opt = outputOptions(ctx, "IMG_${System.currentTimeMillis()}")
    ic.takePicture(opt, ContextCompat.getMainExecutor(ctx), object :
        ImageCapture.OnImageSavedCallback {
        override fun onImageSaved(res: ImageCapture.OutputFileResults) {
            res.savedUri?.let(onSaved)
        }

        override fun onError(e: ImageCaptureException) {
            Log.e("Camera", "Gagal mengambil foto", e)
            Toast.makeText(ctx, "Gagal mengambil foto: ${e.message}", Toast.LENGTH_LONG).show()
        }
    })
}
