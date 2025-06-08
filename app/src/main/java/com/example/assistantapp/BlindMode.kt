package com.example.assistantapp

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.media.AudioAttributes
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import kotlinx.coroutines.launch
import java.util.Locale
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.io.File
import android.content.SharedPreferences
import androidx.compose.ui.Alignment
import androidx.navigation.NavHostController

// Función auxiliar para crear un archivo temporal
fun createTempFile(context: Context): File {
    val tempFileName = "temp_image_${System.currentTimeMillis()}.jpg"
    return File(context.cacheDir, tempFileName).apply {
        if (exists()) delete()
        createNewFile()
    }
}

@Composable
fun ReadingModeCamera(
    onImageCaptured: (Bitmap) -> Unit,
    cameraExecutor: ExecutorService
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val preview = Preview.Builder().build()
    val previewView = remember { PreviewView(context) }
    val imageCapture = remember {
        ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
            .build()
    }

    LaunchedEffect(Unit) {
        val cameraProvider = ProcessCameraProvider.getInstance(context).get()
        cameraProvider.unbindAll()
        cameraProvider.bindToLifecycle(
            lifecycleOwner,
            CameraSelector.DEFAULT_BACK_CAMERA,
            preview,
            imageCapture
        )
        preview.setSurfaceProvider(previewView.surfaceProvider)

        val file = createTempFile(context)
        val outputOptions = ImageCapture.OutputFileOptions.Builder(file).build()
        imageCapture.takePicture(
            outputOptions,
            cameraExecutor,
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                    val bitmap = BitmapFactory.decodeFile(file.absolutePath)
                    onImageCaptured(bitmap)
                }
                override fun onError(exception: ImageCaptureException) {
                    exception.printStackTrace()
                }
            }
        )
    }
    AndroidView({ previewView }, modifier = Modifier.fillMaxSize())
}

@Composable
fun BlindModeScreen(navController: NavHostController) {
    val context = LocalContext.current
    val vibrator = remember {
        context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
    }
    val scope = rememberCoroutineScope()
    val prefs = context.getSharedPreferences("EcoVisualPrefs", Context.MODE_PRIVATE)
    val isPremium = prefs.getBoolean("isPremium", false)
    val selectedVoice = prefs.getString("selectedVoice", "es-MX-female-soft") ?: "es-MX-female-soft"
    val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    var fallDetected by remember { mutableStateOf(false) }

    // --- MOVER estas declaraciones ANTES del listener ---
    val tts = remember { mutableStateOf<TextToSpeech?>(null) }

    fun cleanTextForTTS(text: String): String {
        return text.replace("[!.,:;]".toRegex(), "").trim()
    }
    // --------------------------------------------------------

    // Sensor Listener para detectar caídas
    val sensorListener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent) {
            if (event.sensor.type == Sensor.TYPE_ACCELEROMETER) {
                val x = event.values[0]
                val y = event.values[1]
                val z = event.values[2]
                val acceleration = Math.sqrt((x * x + y * y + z * z).toDouble()).toFloat()
                if (acceleration > 15) {
                    fallDetected = true
                    tts.value?.speak(
                        cleanTextForTTS("Caída detectada Enviando alerta"),
                        TextToSpeech.QUEUE_FLUSH,
                        null,
                        null
                    )
                    simulateAlert(context, prefs)
                }
            }
        }
        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
    }

    DisposableEffect(Unit) {
        sensorManager.registerListener(sensorListener, accelerometer, SensorManager.SENSOR_DELAY_NORMAL)
        onDispose {
            sensorManager.unregisterListener(sensorListener)
        }
    }

    fun triggerVibration(pattern: LongArray) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createWaveform(pattern, -1))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(pattern, -1)
        }
    }

    LaunchedEffect(context) {
        tts.value = TextToSpeech(context) { status ->
            if (status != TextToSpeech.ERROR) {
                tts.value?.language = Locale("es", "MX")
                tts.value?.setSpeechRate(1.5f)
                applyVoice(tts.value, selectedVoice)
                tts.value?.speak(cleanTextForTTS("Modo navegación activo"), TextToSpeech.QUEUE_FLUSH, null, null)
            }
        }
    }

    fun applyVoice(tts: TextToSpeech?, voiceId: String?) {
        when (voiceId) {
            "es-MX-male-neutral" -> tts?.setPitch(1.0f)
            "es-MX-female-soft" -> tts?.setPitch(1.2f)
            "es-MX-male-deep" -> tts?.setPitch(0.8f)
            "es-MX-female-clear" -> tts?.setPitch(1.4f)
        }
    }

    var capturedImage by remember { mutableStateOf<Bitmap?>(null) }
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }
    val hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED &&
                    ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        )
    }

    var currentMode by remember { mutableStateOf("navigation") }
    var isAssistantMode by remember { mutableStateOf(false) }
    var isReadingMode by remember { mutableStateOf(false) }
    var navigationPaused by remember { mutableStateOf(false) }
    var isMicActive by remember { mutableStateOf(false) }
    var chatResponse by remember { mutableStateOf("") }
    var readingModeResult by remember { mutableStateOf("") }
    var analysisResult by remember { mutableStateOf("") }
    var lastSpokenIndex by remember { mutableStateOf(0) }
    var lastProcessedTimestamp by remember { mutableStateOf(0L) }
    val frameInterval = 12000

    val speechRecognizer = remember { SpeechRecognizer.createSpeechRecognizer(context) }
    val speechIntent = remember {
        Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
        }
    }

    LaunchedEffect(Unit) {
        speechRecognizer.setRecognitionListener(object : RecognitionListener {
            override fun onResults(results: Bundle?) {
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (!matches.isNullOrEmpty() && isAssistantMode) {
                    val spokenText = matches[0]
                    scope.launch {
                        chatResponse = sendMessageToGeminiAI(spokenText, "")
                        tts.value?.speak(cleanTextForTTS(chatResponse), TextToSpeech.QUEUE_FLUSH, null, null)
                    }
                }
            }
            override fun onReadyForSpeech(params: Bundle?) {}
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {}
            override fun onError(error: Int) {}
            override fun onPartialResults(partialResults: Bundle?) {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })
    }

    LaunchedEffect(isAssistantMode) {
        if (isAssistantMode) {
            isMicActive = true
            navigationPaused = true
            speechRecognizer.startListening(speechIntent)
            tts.value?.speak(cleanTextForTTS("Modo asistencia activo Habla para interactuar"), TextToSpeech.QUEUE_FLUSH, null, null)
        } else {
            isMicActive = false
            navigationPaused = false
            speechRecognizer.stopListening()
            tts.value?.stop()
            chatResponse = ""
            if (isReadingMode) {
                tts.value?.speak(cleanTextForTTS("Modo lectura"), TextToSpeech.QUEUE_FLUSH, null, null)
            } else {
                tts.value?.speak(cleanTextForTTS("Modo navegación activo"), TextToSpeech.QUEUE_FLUSH, null, null)
            }
        }
    }

    LaunchedEffect(isReadingMode) {
        if (isReadingMode && !isAssistantMode) {
            navigationPaused = true
            tts.value?.speak(cleanTextForTTS("Modo lectura"), TextToSpeech.QUEUE_FLUSH, null, null)
        } else if (!isReadingMode && !isAssistantMode) {
            navigationPaused = false
            tts.value?.speak(cleanTextForTTS("Modo navegación activo"), TextToSpeech.QUEUE_FLUSH, null, null)
        }
    }

    if (!hasPermission) {
        ActivityCompat.requestPermissions(
            context as Activity,
            arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO),
            1
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures(
                    onDoubleTap = {
                        triggerVibration(longArrayOf(0,200,100,200))
                        if (!isReadingMode) {
                            if (isPremium) {
                                isAssistantMode = !isAssistantMode
                            } else {
                                tts.value?.speak(
                                    cleanTextForTTS("Modo asistencia requiere Premium Usa david@ecovisual.com"),
                                    TextToSpeech.QUEUE_FLUSH,
                                    null,
                                    null
                                )
                            }
                        }
                    },
                    onLongPress = {
                        triggerVibration(longArrayOf(0,500))
                        if (!isAssistantMode) {
                            if (isPremium) {
                                isReadingMode = !isReadingMode
                            } else {
                                tts.value?.speak(
                                    cleanTextForTTS("Modo lectura requiere Premium Usa david@ecovisual.com"),
                                    TextToSpeech.QUEUE_FLUSH,
                                    null,
                                    null
                                )
                            }
                        } else {
                            isAssistantMode = false
                            tts.value?.stop()
                            chatResponse = ""
                            tts.value?.speak(cleanTextForTTS("Modo navegación activo"), TextToSpeech.QUEUE_FLUSH, null, null)
                        }
                    }
                )
            }
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (!isAssistantMode) {
                when {
                    isReadingMode -> {
                        ReadingModeCamera(
                            onImageCaptured = { bitmap ->
                                capturedImage = bitmap
                                scope.launch {
                                    readingModeResult = ""
                                    sendFrameToGemini2AI(bitmap, { partialResult ->
                                        readingModeResult += partialResult
                                        tts.value?.speak(cleanTextForTTS(partialResult), TextToSpeech.QUEUE_ADD, null, null)
                                    }, { /* error */ })
                                }
                            },
                            cameraExecutor = cameraExecutor
                        )
                    }
                    !navigationPaused -> {
                        CameraPreviewWithAnalysis { imageProxy ->
                            val currentTimestamp = System.currentTimeMillis()
                            if (currentTimestamp - lastProcessedTimestamp >= frameInterval) {
                                scope.launch {
                                    val bitmap = imageProxy.toBitmap()
                                    if (bitmap != null) {
                                        sendFrameToGeminiAI(bitmap, { partialResult ->
                                            analysisResult += " $partialResult"
                                            val newText = analysisResult.substring(lastSpokenIndex)
                                            tts.value?.speak(cleanTextForTTS(newText), TextToSpeech.QUEUE_ADD, null, null)
                                            lastSpokenIndex = analysisResult.length
                                        }, { /* error */ })
                                        lastProcessedTimestamp = currentTimestamp
                                    }
                                    imageProxy.close()
                                }
                            } else {
                                imageProxy.close()
                            }
                        }
                    }
                }
            } else {
                Box(modifier = Modifier.fillMaxSize()) {
                    Text("Modo Asistencia: Habla para interactuar", modifier = Modifier.align(Alignment.Center))
                }
            }
            AIResponseOverlay(
                currentMode = currentMode,
                navigationResponse = analysisResult,
                response = analysisResult,
                chatResponse = chatResponse,
                readingModeResult = readingModeResult,
                tts = tts.value,
                lastSpokenIndex = lastSpokenIndex
            )
        }
    }
}
