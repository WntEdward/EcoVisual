package com.example.instalens.presentation.home

import android.content.Context
import android.graphics.RectF
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.instalens.data.manager.objectDetection.ObjectDetectionManagerImpl
import com.example.instalens.domain.model.Detection
import com.example.instalens.presentation.home.components.CameraOverlay
import com.example.instalens.presentation.home.components.CameraPreview
import com.example.instalens.presentation.home.components.RequestPermissions
import com.example.instalens.utils.CameraFrameAnalyzer
import com.example.instalens.utils.Constants
import com.example.instalens.utils.ImageScalingUtils

@Composable
fun HomeScreen() {
    val context = LocalContext.current
    val viewModel: HomeViewModel = hiltViewModel()

    RequestPermissions()

    val previewSizeState = remember { mutableStateOf(androidx.compose.ui.unit.IntSize(0, 0)) }
    val confidenceScoreState = remember { mutableFloatStateOf(Constants.INITIAL_CONFIDENCE_SCORE) }

    var scaleFactorX = 1f
    var scaleFactorY = 1f

    var detections by remember { mutableStateOf(emptyList<Detection>()) }

    val cameraFrameAnalyzer = remember {
        CameraFrameAnalyzer(
            objectDetectionManager = ObjectDetectionManagerImpl(context = context),
            onObjectDetectionResults = { newDetections ->
                detections = newDetections
                viewModel.processDetections(newDetections)
            },
            confidenceScoreState = confidenceScoreState,
            context = context
        )
    }

    val cameraController = remember {
        viewModel.prepareCameraController(
            context,
            cameraFrameAnalyzer
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        CameraPreview(
            controller = cameraController,
            modifier = Modifier.fillMaxSize(),
            onPreviewSizeChanged = { newSize ->
                previewSizeState.value = newSize
                val scaleFactors = ImageScalingUtils.getScaleFactors(
                    newSize.width,
                    newSize.height
                )
                scaleFactorX = scaleFactors[0]
                scaleFactorY = scaleFactors[1]
                Log.d("HomeViewModel", "Scale factors: X=$scaleFactorX, Y=$scaleFactorY")
            }
        )
        CameraOverlay(detections = detections)

        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(16.dp)
                .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                .padding(8.dp)
        ) {
            Text(
                text = "Modo asistencia activo",
                color = Color.White,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}