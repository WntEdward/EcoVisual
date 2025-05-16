package com.example.instalens.presentation.mainActivity

import android.content.Context
import android.os.Bundle
import android.os.Vibrator
import android.speech.tts.TextToSpeech
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import com.example.instalens.presentation.navgraph.NavGraph
import com.example.instalens.ui.theme.InstaLensTheme
import dagger.hilt.android.AndroidEntryPoint
import java.util.Locale

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    companion object {
        private val TAG: String? = MainActivity::class.simpleName
    }

    private val viewModel by viewModels<MainViewModel>()
    private lateinit var textToSpeech: TextToSpeech
    private lateinit var vibrator: Vibrator

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)

        // Inicializar TextToSpeech
        textToSpeech = TextToSpeech(this) { status ->
            if (status == TextToSpeech.SUCCESS) {
                textToSpeech.language = Locale("es_MX")
                textToSpeech.speak(
                    "EcoVisual iniciado.",
                    TextToSpeech.QUEUE_FLUSH,
                    null,
                    null
                )
            }
        }

        // Inicializar Vibrator
        vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator

        // Splash Screen
        installSplashScreen().apply {
            setKeepOnScreenCondition {
                viewModel.redirectFlagState
            }
        }

        setContent {
            InstaLensTheme {
                Box(modifier = Modifier.background(color = MaterialTheme.colorScheme.background)) {
                    val startDestination = viewModel.startDestination
                    Log.d(TAG, "setContent() called with startDestination = $startDestination ")
                    NavGraph(startDestination = startDestination)
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // Liberar recursos
        textToSpeech.shutdown()
    }

    // Métodos para acceder a TextToSpeech y Vibrator
    fun getTextToSpeech(): TextToSpeech = textToSpeech
    fun getVibrator(): Vibrator = vibrator
}