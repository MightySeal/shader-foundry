package io.easyshaders.legacy

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Matrix
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.video.Recording
import androidx.camera.view.CameraController
import androidx.camera.view.LifecycleCameraController
import android.Manifest
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.offset
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.ContextCompat.startActivity
import androidx.core.net.toUri
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun LegacyCameraScreen(
    modifier: Modifier,
    viewModel: LegacyCameraViewModel = hiltViewModel(),
) {
    val context = LocalContext.current

    val controller = remember {
        LifecycleCameraController(context).apply {
            CameraController.IMAGE_CAPTURE
            CameraController.VIDEO_CAPTURE
        }
    }
    val scope = rememberCoroutineScope()
    val scaffoldState = rememberBottomSheetScaffoldState()
    val gallery by viewModel.gallery.collectAsState()
    val isVideo = remember { mutableStateOf(false) }
    val recording by remember { mutableStateOf<Recording?>(null) }
    val isRecording by remember { mutableStateOf(false) }
    val zoom = remember { mutableFloatStateOf(1.0f) }

    val cameraPermissionState = rememberPermissionState(Manifest.permission.CAMERA)

    if (cameraPermissionState.status.isGranted) {
        val state = viewModel.uiState.collectAsState(LegacyCameraViewState.Loading).value

        when (state) {
            is LegacyCameraViewState.Loading -> {
                // Show loading state
            }

            is LegacyCameraViewState.Ready -> {
                val lifecycleOwner = LocalLifecycleOwner.current
                val context = LocalContext.current
                val previewView = remember { PreviewView(context) }

                BottomSheetScaffold(
                    scaffoldState = scaffoldState,
                    sheetPeekHeight = 0.dp,
                    sheetContent = {
                        PhotoBottomSheetContent(
                            gallery = gallery,
                            modifier = Modifier
                                .fillMaxWidth(),
                            onClick = {
                                val intent = Intent()
                                intent.setAction(Intent.ACTION_VIEW)
                                intent.setDataAndType(
                                    it,
                                    "image/*"
                                )
                                startActivity(context, intent, null)
                            }
                        )
                    }
                ) { padding ->
                    Box(
                        modifier = modifier
                            .fillMaxSize()
                            .padding(padding),
                        contentAlignment = Alignment.Center
                    ) {
                        Column {
                            Spacer(
                                modifier = Modifier
                                    .height(10.dp)
                            )
                            Box {
                                CameraPreview(
                                    modifier = Modifier
                                        .aspectRatio(0.75f)
                                        .fillMaxSize(),
                                    controller = controller
                                )
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .align(Alignment.BottomCenter),
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Button(
                                        modifier = Modifier.padding(end = 8.dp),
                                        shape = CircleShape,
                                        onClick = {
                                            controller.setZoomRatio(1.0f)
                                            zoom.value = 1.0f
                                        },
                                        elevation = null,
                                        colors =
                                        ButtonDefaults.buttonColors(
                                            containerColor = if (zoom.value == 1.0f) Color.White else Color.Transparent,
                                            contentColor = if (zoom.value == 1.0f) Color.Black else Color.White,
                                        )
                                    ) {
                                        Text("1.0")
                                    }
                                    Button(
                                        shape = CircleShape,
                                        onClick = {
                                            controller.setZoomRatio(2.0f)
                                            zoom.value = 2.0f
                                        },
                                        elevation = null,
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (zoom.value == 2.0f) Color.White else Color.Transparent,
                                            contentColor = if (zoom.value == 2.0f) Color.Black else Color.White,
                                        )
                                    ) {
                                        Text("2.0")
                                    }
                                }
                            }
                            Spacer(
                                modifier = Modifier
                                    .height(10.dp)
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                Button(onClick = { isVideo.value = false }) {
                                    Text("Photo")
                                }
                                Button(
                                    onClick = {
                                        // isVideo.value = true
                                    },
                                    enabled = false
                                ) {
                                    Text("Video")
                                }
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceAround
                            ) {
                                IconButton(
                                    onClick = {
                                        scope.launch {
                                            scaffoldState.bottomSheetState.expand()
                                        }
                                    },
                                    modifier = Modifier
                                        .offset(16.dp, 16.dp)
                                        .padding(bottom = 16.dp, end = 16.dp),
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.PhotoLibrary,
                                        contentDescription = "Open Library"
                                    )
                                }
                                Button(
                                    onClick = {
                                        if (isVideo.value) {

                                        }
                                        else {
                                            takePicture(
                                                controller = controller,
                                                context = context,
                                                onSave = {
                                                    viewModel.onTakePhoto(it)
                                                }
                                            )
                                        }
                                    },
                                    shape = CircleShape,
                                    colors = ButtonDefaults.buttonColors(containerColor = LocalContentColor.current),
                                    modifier = Modifier
                                        .height(75.dp)
                                        .width(75.dp),
                                ) {}
                                IconButton(
                                    onClick = {
                                        switchCamera(controller)
                                    },
                                    modifier = Modifier
                                        .offset(16.dp, 16.dp)
                                        .padding(bottom = 16.dp, end = 16.dp),
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Cameraswitch,
                                        contentDescription = "Switch camera"
                                    )
                                }
                            }
                        }
                    }
                }

                OverlayControls(
                    controls = state.controls,
                    modifier = Modifier.padding(top = 48.dp, start = 16.dp, end = 16.dp, bottom = 16.dp),
                ) {
                    viewModel.onControlChange(it)
                }

                LaunchedEffect(previewView) {
                    viewModel.startPreview(
                        lifecycleOwner = lifecycleOwner,
                        surfaceProvider = previewView.surfaceProvider
                    )
                }
            }
        }

    } else {
        CameraPermission(cameraPermissionState)
    }
}

private fun switchCamera(
    controller: LifecycleCameraController,
) {
    controller.cameraSelector =
        if (controller.cameraSelector == CameraSelector.DEFAULT_BACK_CAMERA) {
            CameraSelector.DEFAULT_FRONT_CAMERA
        } else {
            CameraSelector.DEFAULT_BACK_CAMERA
        }
}

private fun takePicture(
    controller: LifecycleCameraController,
    context: Context,
    onSave: (picture: LegacyCameraViewModel.LocalPicture) -> Unit = {}
) {
    controller.takePicture(
        ContextCompat.getMainExecutor(context),
        object : ImageCapture.OnImageCapturedCallback() {
            override fun onCaptureSuccess(image: ImageProxy) {
                super.onCaptureSuccess(image)
                Log.e("Camera", "onCaptureSuccess")

                val matrix = Matrix().apply {
                    postRotate(image.imageInfo.rotationDegrees.toFloat())
                }
                val rotatedBitmap = Bitmap.createBitmap(
                    image.toBitmap(),
                    0,
                    0,
                    image.width,
                    image.height,
                    matrix,
                    true
                )

                val filename = "${System.currentTimeMillis()}.jpg"

                var outputStream: OutputStream? = null

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    context.contentResolver?.also { resolver ->
                        val contentValues = ContentValues().apply {
                            //putting file information in content values
                            put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
                            put(MediaStore.MediaColumns.MIME_TYPE, "image/jpg")
                            put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES)
                        }

                        //Inserting the contentValues to contentResolver and getting the Uri
                        val imageUri: Uri? = resolver.insert(
                            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                            contentValues
                        )

                        //Opening an output stream with the Uri that we got
                        outputStream = imageUri?.let { resolver.openOutputStream(it) }
                        onSave(
                            LegacyCameraViewModel.LocalPicture(
                                rotatedBitmap, imageUri
                            )
                        )
                    }
                } else {
                    val imagesDir =
                        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
                    val file = File(imagesDir, filename)

                    onSave(
                        LegacyCameraViewModel.LocalPicture(
                            rotatedBitmap, file.toUri()
                        )
                    )
                    outputStream = FileOutputStream(file)
                }

                outputStream?.use {
                    rotatedBitmap.compress(Bitmap.CompressFormat.JPEG, 100, it)
                }
            }

            override fun onError(exception: ImageCaptureException) {
                super.onError(exception)
                Log.e("Camera", "Couldn't take photo: ", exception)
            }
        }
    )
}

private fun recordVideo(
    controller: LifecycleCameraController,
    context: Context,
) { }

@Composable
private fun OverlayControls(
    controls: List<Control>,
    modifier: Modifier = Modifier,
    onChange: (ControlValue) -> Unit
) {
    Box(
        modifier = modifier.fillMaxWidth(),
    ) {
        LazyColumn {
            items(controls) { control ->
                when (control) {
                    is Control.FloatSeek -> {
                        FloatSeekControl(control) {
                            onChange(ControlValue.FloatValue(control.id, it))
                        }
                    }

                    is Control.CheckBox -> {
                        CheckBoxControl(control) {
                            onChange(ControlValue.BooleanValue(control.id, it))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FloatSeekControl(control: Control.FloatSeek, onChange: (Float) -> Unit) {
    var sliderPosition by remember { mutableFloatStateOf(control.initial) }

    Column {
        Text(
            text = "${control.title}: ${"%.2f".format(sliderPosition)}",
            modifier = Modifier.align(Alignment.Start),
        )
        Slider(
            modifier = Modifier.width(300.dp),
            value = sliderPosition,
            valueRange = control.range,
            onValueChange = {
                sliderPosition = it
                onChange(it)
            }
        )
    }
}

@Composable
private fun CheckBoxControl(control: Control.CheckBox, onChange: (Boolean) -> Unit) {
    var checkboxState by remember { mutableStateOf(false) }
    Row {
        Checkbox(
            checked = checkboxState,
            onCheckedChange = { enabled ->
                checkboxState = enabled
                onChange(enabled)
            },
            modifier = Modifier.align(Alignment.CenterVertically),
        )
        Text(
            text = control.title,
            modifier = Modifier.align(Alignment.CenterVertically),
        )
    }
}

@Composable
private fun CaptureButton(
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(modifier = modifier) {
        Button(
            onClick = onClick,
            shape = CircleShape,
            colors = ButtonDefaults.buttonColors(containerColor = Color.White),
            modifier = Modifier
                .height(75.dp)
                .width(75.dp),
        ) {}
    }
}
