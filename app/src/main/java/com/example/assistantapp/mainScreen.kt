package com.example.assistantapp

import android.content.Context
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
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
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
    val vibrator = context.getSystemService(Vibrator::class.java)
    val prefs = context.getSharedPreferences("EcoVisualPrefs", Context.MODE_PRIVATE)
    val isPremium = prefs.getBoolean("isPremium", false)
    val selectedVoice = prefs.getString("selectedVoice", "es-MX-female-soft") ?: "es-MX-female-soft"

    // Initialize TextToSpeech
    DisposableEffect(Unit) {
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                val voices = tts?.getVoices()
                var voice: Voice? = null
                voices?.forEach { v ->
                    if (v.locale.language == "es" && v.name.contains("male")) {
                        voice = v
                        return@forEach
                    }
                }
                voice?.let { tts?.setVoice(it) }
                tts?.setLanguage(Locale("es", "MX"))
                applyVoice(tts, selectedVoice) // Aplicar la voz seleccionada al iniciar
                val welcomeMessage = if (isPremium) {
                    "Bienvenido a EcoVisual Premium. Da un click para instrucciones. " +
                            "Doble tap para modo detección. Presiona largo para configuración."
                } else {
                    "Bienvenido a EcoVisual Gratis. Da un click para instrucciones. " +
                            "Doble tap para modo detección."
                }
                tts?.speak(welcomeMessage, TextToSpeech.QUEUE_FLUSH, null, null)
            }
        }
        onDispose {
            tts?.stop()
            tts?.shutdown()
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
        val instructions = if (isPremium) {
            "Instrucciones de uso. Doble tap para modo detección. Presiona largo para configuración."
        } else {
            "Instrucciones de uso. Doble tap para modo detección."
        }
        tts?.speak(instructions, TextToSpeech.QUEUE_FLUSH, null, null)
    }

    fun handleDoubleTap() {
        playConfirmationTone(0.8f, 1.0f)
        vibrator?.vibrate(VibrationEffect.createWaveform(longArrayOf(100, 50), -1))
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        navController.navigate("blindMode")
    }

    fun handleLongPress() {
        playConfirmationTone(1.0f, 1.0f)
        vibrator?.vibrate(VibrationEffect.createWaveform(longArrayOf(100, 50, 100), -1))
        if (isPremium) {
            tts?.speak("Accediendo a configuración.", TextToSpeech.QUEUE_FLUSH, null, null)
            navController.navigate("settingsScreen")
        } else {
            tts?.speak("Configuración requiere Premium. Usa david@ecovisual.com.", TextToSpeech.QUEUE_FLUSH, null, null)
        }
    }

    LaunchedEffect(tapCount) {
        when (tapCount) {
            1 -> handleSingleTap()
            2 -> handleDoubleTap()
            3 -> {
                // Ignoramos el triple tap por ahora para evitar conflictos
                tapCount = 0
            }
        }
        if (tapCount > 0) {
            delay(300) // Reducimos el retraso para mejor detección
            tapCount = 0
        }
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.fillMaxSize()
            .background(Color(0xFFF4F4F4))
            .combinedClickable(
                onClick = { tapCount++ },
                onLongClick = { handleLongPress() }
            )
    ) {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(R.drawable.mainscreen)
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

        Text(
            text = "EcoVisual: Aplicación de detección de obstáculos",
            color = Color.Black,
            textAlign = TextAlign.Center,
            fontSize = 14.sp,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 16.dp)
        )
    }
}