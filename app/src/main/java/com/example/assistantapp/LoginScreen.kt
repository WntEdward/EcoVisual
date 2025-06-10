package com.example.assistantapp

import android.content.Context
import android.os.Build
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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import androidx.compose.ui.layout.ContentScale
import java.util.Locale

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun LoginScreen(navController: NavHostController) {
    val haptic = LocalHapticFeedback.current
    val context = LocalContext.current
    var tts by remember { mutableStateOf<TextToSpeech?>(null) }
    var tapCount by remember { mutableStateOf(0) }
    var isRegisterMode by remember { mutableStateOf(false) }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    val vibrator = context.getSystemService(Vibrator::class.java)
    val prefs = context.getSharedPreferences("EcoVisualPrefs", Context.MODE_PRIVATE)
    val selectedVoice = prefs.getString("selectedVoice", "es-MX-female-soft") ?: "es-MX-female-soft"

    // Initialize TextToSpeech
    DisposableEffect(Unit) {
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.setLanguage(Locale("es", "MX"))
                val voices = tts?.getVoices()
                var voice: Voice? = null
                voices?.forEach { v ->
                    if (v.locale.language == "es" && v.name.contains("male", ignoreCase = true)) {
                        voice = v
                        return@forEach
                    }
                }
                voice?.let { tts?.setVoice(it) }
                // Aplicar la voz seleccionada
                when (selectedVoice) {
                    "es-MX-male-neutral" -> tts?.setPitch(1.0f)
                    "es-MX-female-soft" -> tts?.setPitch(1.2f)
                    "es-MX-male-deep" -> tts?.setPitch(0.8f)
                    "es-MX-female-clear" -> tts?.setPitch(1.4f)
                }
                tts?.speak(
                    "Bienvenido a EcoVisual. Toca la pantalla para iniciar sesión o mantén presionado para registrarte.",
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
            if (isRegisterMode) {
                "Modo registro. Ingresa tu nombre, correo y contraseña. Doble toque para enviar."
            } else {
                "Modo inicio de sesión. Ingresa tu correo y contraseña. Doble toque para enviar."
            },
            TextToSpeech.QUEUE_FLUSH,
            null,
            null
        )
    }

    fun handleDoubleTap() {
        playConfirmationTone(0.8f, 1.0f)
        vibrator?.vibrate(VibrationEffect.createWaveform(longArrayOf(100, 50), -1))
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)

        if (isRegisterMode) {
            // Registro
            if (email.isNotEmpty() && password.isNotEmpty() && name.isNotEmpty()) {
                prefs.edit().putString("password_$email", password).apply()
                prefs.edit().putString("name_$email", name).apply()
                prefs.edit().putBoolean("isPremium_$email", false).apply() // Por defecto gratis
                tts?.speak("Registro exitoso. Bienvenido, $name.", TextToSpeech.QUEUE_FLUSH, null, null)
                isRegisterMode = false // Vuelve a modo login
            } else {
                tts?.speak("Ingresa nombre, correo y contraseña válidos.", TextToSpeech.QUEUE_FLUSH, null, null)
            }
        } else {
            // Login
            if (email.isEmpty() || password.isEmpty()) {
                tts?.speak("Ingresa correo y contraseña válidos.", TextToSpeech.QUEUE_FLUSH, null, null)
                return
            }
            val storedPassword = prefs.getString("password_$email", "")
            val isValidLogin = when (email) {
                "david@ecovisual.com" -> password == "David123"
                "lalo@ecovisual.com" -> password == "Lalo123"
                else -> storedPassword == password
            }
            if (isValidLogin) {
                val isPremium = when (email) {
                    "david@ecovisual.com" -> true
                    "lalo@ecovisual.com" -> false
                    else -> prefs.getBoolean("isPremium_$email", false)
                }
                prefs.edit().putBoolean("isLoggedIn", true).apply()
                prefs.edit().putBoolean("isPremium", isPremium).apply()
                prefs.edit().putString("userEmail", email).apply()
                tts?.speak("Inicio de sesión exitoso. Bienvenido.", TextToSpeech.QUEUE_FLUSH, null, null)
                navController.navigate("mainPage") {
                    popUpTo(navController.graph.startDestinationId) { inclusive = true }
                }
            } else {
                tts?.speak("Credenciales incorrectas. Intenta de nuevo.", TextToSpeech.QUEUE_FLUSH, null, null)
            }
        }
    }

    fun handleLongPress() {
        isRegisterMode = !isRegisterMode
        playConfirmationTone(1.0f, 1.0f)
        vibrator?.vibrate(VibrationEffect.createWaveform(longArrayOf(100, 50, 100), -1))
        tts?.speak(
            if (isRegisterMode) "Modo registro activado. Ingresa tu nombre, correo y contraseña." else "Modo inicio de sesión activado. Ingresa tu correo y contraseña.",
            TextToSpeech.QUEUE_FLUSH,
            null,
            null
        )
    }

    LaunchedEffect(tapCount) {
        when (tapCount) {
            1 -> handleSingleTap()
            2 -> handleDoubleTap()
        }
        if (tapCount > 0) {
            kotlinx.coroutines.delay(500)
            tapCount = 0
        }
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF4F4F4))
            .combinedClickable(
                onClick = { tapCount++ },
                onLongClick = { handleLongPress() }
            )
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(R.drawable.nav)
                    .crossfade(true)
                    .build(),
                contentDescription = "Application logo",
                modifier = Modifier
                    .size(200.dp)
                    .alpha(animateFloatAsState(targetValue = 1f, animationSpec = tween(1000)).value),
                contentScale = ContentScale.Fit
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = if (isRegisterMode) "Registrarse en EcoVisual" else "Iniciar Sesión en EcoVisual",
                color = Color.Black,
                textAlign = TextAlign.Center,
                fontSize = 14.sp,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            if (isRegisterMode) {
                BasicTextField(
                    value = name,
                    onValueChange = { name = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp)
                        .background(Color.White)
                        .padding(16.dp),
                    textStyle = TextStyle(color = Color.Black, fontSize = 14.sp),
                    decorationBox = { innerTextField ->
                        if (name.isEmpty()) {
                            Text("Nombre", color = Color.Gray, fontSize = 14.sp)
                        }
                        innerTextField()
                    }
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            BasicTextField(
                value = email,
                onValueChange = { email = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
                    .background(Color.White)
                    .padding(16.dp),
                textStyle = TextStyle(color = Color.Black, fontSize = 14.sp),
                decorationBox = { innerTextField ->
                    if (email.isEmpty()) {
                        Text("Correo electrónico", color = Color.Gray, fontSize = 14.sp)
                    }
                    innerTextField()
                }
            )

            Spacer(modifier = Modifier.height(8.dp))

            BasicTextField(
                value = password,
                onValueChange = { password = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
                    .background(Color.White)
                    .padding(16.dp),
                textStyle = TextStyle(color = Color.Black, fontSize = 14.sp),
                visualTransformation = PasswordVisualTransformation(),
                decorationBox = { innerTextField ->
                    if (password.isEmpty()) {
                        Text("Contraseña", color = Color.Gray, fontSize = 14.sp)
                    }
                    innerTextField()
                }
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Doble toque para enviar. Mantén presionado para cambiar de modo.",
                color = Color.Black,
                textAlign = TextAlign.Center,
                fontSize = 14.sp
            )
        }
    }
}