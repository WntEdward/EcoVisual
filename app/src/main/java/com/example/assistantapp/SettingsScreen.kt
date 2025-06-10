package com.example.assistantapp

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.speech.tts.TextToSpeech
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Divider
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import kotlinx.coroutines.delay
import java.util.Locale

@Composable
fun SettingsScreen(navController: NavHostController) {
    val context = LocalContext.current
    val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
    val prefs = context.getSharedPreferences("EcoVisualPrefs", Context.MODE_PRIVATE)
    val isPremium = prefs.getBoolean("isPremium", false)

    // Lista de voces disponibles
    val voiceOptions = listOf(
        Pair("Voz Masculina - Tono Neutro", "es-MX-male-neutral"),
        Pair("Voz Femenina - Tono Suave", "es-MX-female-soft"),
        Pair("Voz Masculina - Tono Grave", "es-MX-male-deep"),
        Pair("Voz Femenina - Tono Claro", "es-MX-female-clear")
    )

    var selectedVoice by remember { mutableStateOf(prefs.getString("selectedVoice", "es-MX-female-soft") ?: "es-MX-female-soft") }
    var tts by remember { mutableStateOf<TextToSpeech?>(null) }
    var currentOptionIndex by remember { mutableStateOf(voiceOptions.indexOfFirst { it.second == selectedVoice }) }
    var tapCount by remember { mutableStateOf(0) }

    // Listas para hasta 5 contactos de emergencia
    val emergencyContactNames = remember {
        mutableStateListOf(
            prefs.getString("emergencyContactName1", "") ?: "",
            prefs.getString("emergencyContactName2", "") ?: "",
            prefs.getString("emergencyContactName3", "") ?: "",
            prefs.getString("emergencyContactName4", "") ?: "",
            prefs.getString("emergencyContactName5", "") ?: ""
        )
    }
    val emergencyContactNumbers = remember {
        mutableStateListOf(
            prefs.getString("emergencyContactNumber1", "") ?: "",
            prefs.getString("emergencyContactNumber2", "") ?: "",
            prefs.getString("emergencyContactNumber3", "") ?: "",
            prefs.getString("emergencyContactNumber4", "") ?: "",
            prefs.getString("emergencyContactNumber5", "") ?: ""
        )
    }
    var editingIndex by remember { mutableStateOf<Int?>(null) }

    // Modo ahorro de datos
    var isDataSavingMode by remember { mutableStateOf(prefs.getBoolean("isDataSavingMode", false)) }

    LaunchedEffect(Unit) {
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = Locale("es", "MX")
                if (isPremium) {
                    tts?.speak(
                        "Pantalla de configuración. Toca una vez para escuchar la voz actual. " +
                                "Doble toque para agregar o editar contacto de emergencia. Triple toque para guardar y salir.",
                        TextToSpeech.QUEUE_FLUSH,
                        null,
                        null
                    )
                    applyVoice(tts, selectedVoice, isDataSavingMode)
                } else {
                    tts?.speak(
                        "Acceso denegado. Esta funcionalidad requiere un plan Premium.",
                        TextToSpeech.QUEUE_FLUSH,
                        null,
                        null
                    )
                    navController.navigate("mainPage")
                }
            }
        }
    }

    fun applyVoice(tts: TextToSpeech?, voiceId: String?, isSavingMode: Boolean) {
        when (voiceId) {
            "es-MX-male-neutral" -> tts?.setPitch(if (isSavingMode) 0.8f else 1.0f)
            "es-MX-female-soft" -> tts?.setPitch(if (isSavingMode) 1.0f else 1.2f)
            "es-MX-male-deep" -> tts?.setPitch(if (isSavingMode) 0.6f else 0.8f)
            "es-MX-female-clear" -> tts?.setPitch(if (isSavingMode) 1.2f else 1.4f)
        }
        if (isSavingMode) {
            tts?.setSpeechRate(0.8f) // Reducir velocidad en modo ahorro
        } else {
            tts?.setSpeechRate(1.0f)
        }
    }

    fun triggerVibration() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            vibrator.vibrate(50)
        }
    }

    fun handleSingleTap() {
        if (isPremium) {
            triggerVibration()
            tts?.stop()
            val voiceName = voiceOptions[currentOptionIndex].first
            applyVoice(tts, selectedVoice, isDataSavingMode)
            tts?.speak("Esta es la voz $voiceName. Doble toque para agregar o editar contacto.", TextToSpeech.QUEUE_FLUSH, null, null)
        }
    }

    fun handleDoubleTap() {
        if (isPremium) {
            triggerVibration()
            tts?.stop()
            val emptySlots = emergencyContactNames.count { it.isEmpty() }
            if (emptySlots == 0) {
                tts?.speak("Límite de 5 contactos alcanzado. Edita un contacto existente.", TextToSpeech.QUEUE_FLUSH, null, null)
                return
            }
            val nextIndex = emergencyContactNames.indexOfFirst { it.isEmpty() }
            editingIndex = if (nextIndex != -1) nextIndex else 0
            tts?.speak("Ingresa el nombre y número para el contacto ${editingIndex!! + 1}. Toca de nuevo para confirmar.", TextToSpeech.QUEUE_FLUSH, null, null)
        }
    }

    fun handleTripleTap() {
        if (isPremium) {
            triggerVibration()
            prefs.edit().putString("selectedVoice", selectedVoice).apply()
            prefs.edit().putBoolean("isDataSavingMode", isDataSavingMode).apply() // Guardar estado del modo ahorro
            emergencyContactNames.forEachIndexed { index, name ->
                prefs.edit().putString("emergencyContactName${index + 1}", name).apply()
                prefs.edit().putString("emergencyContactNumber${index + 1}", emergencyContactNumbers[index]).apply()
            }
            tts?.stop()
            tts?.speak("Configuración guardada. Regresando al menú principal.", TextToSpeech.QUEUE_FLUSH, null, null)
            navController.navigate("mainPage") {
                popUpTo(navController.graph.startDestinationId) { inclusive = false }
            }
        }
    }

    LaunchedEffect(tapCount) {
        when (tapCount) {
            1 -> handleSingleTap()
            2 -> handleDoubleTap()
            3 -> handleTripleTap()
        }
        if (tapCount > 0) {
            delay(500)
            tapCount = 0
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            tts?.stop()
            tts?.shutdown()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF4F4F4))
            .pointerInput(Unit) {
                detectTapGestures { tapCount++ }
            },
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Configuración",
                color = Color.Black,
                textAlign = TextAlign.Center,
                fontSize = 20.sp,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            if (isPremium) {
                // Sección de Voces (arriba)
                Text(
                    text = "Selección de Voz",
                    color = Color.Black,
                    textAlign = TextAlign.Center,
                    fontSize = 18.sp,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Text(
                    text = "Voz actual: ${voiceOptions[currentOptionIndex].first}",
                    color = Color.Black,
                    textAlign = TextAlign.Center,
                    fontSize = 16.sp,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                // Línea separadora
                Divider(
                    color = Color.Gray,
                    thickness = 1.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp)
                )

                // Sección de Contactos de Emergencia
                Text(
                    text = "Contactos de Emergencia (máximo 5)",
                    color = Color.Black,
                    textAlign = TextAlign.Center,
                    fontSize = 18.sp,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                emergencyContactNames.forEachIndexed { index, name ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        TextField(
                            value = name,
                            onValueChange = { emergencyContactNames[index] = it },
                            label = { Text("Nombre ${index + 1}") },
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        TextField(
                            value = emergencyContactNumbers[index],
                            onValueChange = { emergencyContactNumbers[index] = it },
                            label = { Text("Número ${index + 1}") },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                val emptySlots = emergencyContactNames.count { it.isEmpty() }
                if (emptySlots < 5) {
                    Text(
                        text = "Doble toque para agregar un nuevo contacto.",
                        color = Color.Black,
                        textAlign = TextAlign.Center,
                        fontSize = 14.sp,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                } else {
                    Text(
                        text = "Límite de 5 contactos alcanzado.",
                        color = Color.Black,
                        textAlign = TextAlign.Center,
                        fontSize = 14.sp,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }


            }
        }
    }
}

fun applyVoice(tts: TextToSpeech?, selectedVoice: String, dataSavingMode: Boolean) {

}
