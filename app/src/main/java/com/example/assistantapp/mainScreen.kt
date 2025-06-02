package com.example.assistantapp

import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.speech.tts.TextToSpeech
import android.speech.tts.Voice
import android.util.Log
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.offset
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import coil.compose.AsyncImage
import coil.decode.GifDecoder
import coil.request.ImageRequest
import kotlinx.coroutines.delay
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MainPage(navController: NavHostController) {
    val haptic = LocalHapticFeedback.current
    val context = LocalContext.current
    var tts by remember { mutableStateOf<TextToSpeech?>(null) }
    var tapCount by remember { mutableStateOf(0) }
    var currentSpeechRate by remember { mutableStateOf(1.0f) }
    val vibrator = context.getSystemService(Vibrator::class.java)
    val timeFormatter = DateTimeFormatter.ofPattern("h:mm a")

    // Initialize TextToSpeech
    DisposableEffect(Unit) {
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                val textToSpeech = tts!!
                // Get the list of available voices
                val voices = textToSpeech.getVoices()

                // Print all available voices to log for debugging
                for (voice in voices) {
                    Log.d("TTS", "Voice: ${voice.name}, Language: ${voice.locale.language}, Country: ${voice.locale.country}")
                }

                // Find a Spanish male voice
                var selectedVoice: Voice? = null
                for (voice in voices) {
                    if (voice.locale.language == "es" && voice.name.contains("male")) {
                        selectedVoice = voice
                        break
                    }
                }

                // If a male Spanish voice is found, set it
                if (selectedVoice != null) {
                    textToSpeech.setVoice(selectedVoice)
                    Log.d("TTS", "Selected voice: ${selectedVoice.name}")
                } else {
                    Log.d("TTS", "No male Spanish voice found, using default")
                }

                // Set language to Spanish Mexico
                textToSpeech.setLanguage(Locale("es", "MX"))

                // Speak initial message
                textToSpeech.speak(
                    "Da un click en cualquier parte de la pantalla para las instrucciones. Manten presionado para la hora",
                    TextToSpeech.QUEUE_FLUSH,
                    null,
                    null
                )
            }
        }

        onDispose {
            tts?.stop()
            tts?.shutdown()
        }
    }

    fun playConfirmationTone(pitch: Float, speed: Float) {
        val params = Bundle().apply {
            putFloat(TextToSpeech.Engine.KEY_PARAM_PAN, 0.0f)
            putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, 1.0f)
        }
        tts?.setPitch(pitch)
        tts?.setSpeechRate(speed)
        tts?.speak(" ", TextToSpeech.QUEUE_ADD, params, "tone_id")
    }

    fun handleSingleTap() {
        playConfirmationTone(1.2f, 1.0f)
        vibrator?.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE))
        tts?.speak(
            "instrucciones de uso " +
                    "Doble tap a la pantalla para cambiar de modo " +
                    "en el segundo modo puedes preguntar sobre detalles de lo que hay enfrente " +
                    "presiona largo la pantalla para saber la hora" +
                    "Doble click a la pantalla para continuar",
            TextToSpeech.QUEUE_FLUSH,
            null,
            null
        )
    }

    fun handleDoubleTap() {
        playConfirmationTone(0.8f, 1.0f)
        vibrator?.vibrate(VibrationEffect.createWaveform(longArrayOf(100, 50), -1))
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        navController.navigate("blindMode")
    }

    LaunchedEffect(tapCount) {
        when (tapCount) {
            1 -> handleSingleTap()
            2 -> handleDoubleTap()
        }
        if (tapCount > 0) {
            delay(500)
            tapCount = 0
        }
    }

    fun announceTime() {
        val currentTime = LocalDateTime.now().format(timeFormatter)
        vibrator?.vibrate(VibrationEffect.createWaveform(longArrayOf(100, 50, 100), -1))
        tts?.speak("La hora actual es $currentTime", TextToSpeech.QUEUE_FLUSH, null, null)
    }

    LaunchedEffect(Unit) {
        while (true) {
            val currentTime = LocalTime.now()
            val minute = currentTime.minute
            val hour = currentTime.hour

            if (minute == 0) {
                tts?.speak("Son las $hour en punto", TextToSpeech.QUEUE_ADD, null, "chime")
            } else if (minute == 30) {
                tts?.speak(" $hour y media", TextToSpeech.QUEUE_ADD, null, "chime")
            }

            delay(60000)
        }
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.fillMaxSize()
            .background(Color(0xFFF4F4F4))
    ) {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(R.drawable.mainscreen)
                .decoderFactory(GifDecoder.Factory())
                .crossfade(true)
                .build(),
            contentDescription = "Animated blind man",
            modifier = Modifier
                .fillMaxHeight(0.8f),
            contentScale = ContentScale.Crop
        )

        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(R.drawable.nav)
                .crossfade(true)
                .build(),
            contentDescription = "Application logo",
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 0.dp)
                .offset(y = (-80).dp)
                .fillMaxWidth()
                .size(400.dp)
                .alpha(animateFloatAsState(targetValue = 1f, animationSpec = tween(1000)).value),
            contentScale = ContentScale.Fit
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .combinedClickable(
                    onClick = { tapCount++ },
                    onLongClick = {
                        announceTime()
                        tapCount = 0
                    }
                )
        )

        Text(
            text = "EcoVisual: Aplicacion de deteccion de obstaculos",
            color = Color.Black,
            textAlign = TextAlign.Center,
            fontSize = 14.sp,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 16.dp)
        )
    }
}