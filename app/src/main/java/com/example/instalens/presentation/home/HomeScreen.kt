package com.example.instalens.presentation.home

import android.graphics.RectF
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.zIndex
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.instalens.R
import com.example.instalens.data.manager.objectDetection.ObjectDetectionManagerImpl
import com.example.instalens.domain.model.Detection
import com.example.instalens.presentation.home.components.CameraOverlay
import com.example.instalens.presentation.home.components.CameraPreview
import com.example.instalens.presentation.home.components.ObjectCounter
import com.example.instalens.presentation.home.components.RequestPermissions
import com.example.instalens.presentation.utils.Dimens
import com.example.instalens.utils.CameraFrameAnalyzer
import com.example.instalens.utils.Constants
import com.example.instalens.utils.ImageScalingUtils

@Composable
fun HomeScreen() {
    val context = LocalContext.current
    val viewModel: HomeViewModel = hiltViewModel()

    RequestPermissions()

    val isImageSavedStateFlow by viewModel.isImageSavedStateFlow.collectAsState()
    val previewSizeState = remember { mutableStateOf(IntSize(0, 0)) }
    val boundingBoxCoordinatesState = remember { mutableStateListOf<RectF>() }
    val confidenceScoreState = remember { mutableFloatStateOf(Constants.INITIAL_CONFIDENCE_SCORE) }

    var scaleFactorX = 1f
    var scaleFactorY = 1f

    val screenWidth = LocalContext.current.resources.displayMetrics.widthPixels * 1f
    val screenHeight = LocalContext.current.resources.displayMetrics.heightPixels * 1f

    Box(modifier = Modifier.fillMaxSize()) {
        var detections by remember { mutableStateOf(emptyList<Detection>()) }

        LaunchedEffect(detections) {}

        val cameraFrameAnalyzer = remember {
            CameraFrameAnalyzer(
                objectDetectionManager = ObjectDetectionManagerImpl(context = context),
                onObjectDetectionResults = {
                    detections = it
                    boundingBoxCoordinatesState.clear()
                    detections.forEach { detection ->
                        boundingBoxCoordinatesState.add(detection.boundingBox)
                    }
                },
                confidenceScoreState = confidenceScoreState
            )
        }

        val cameraController = remember {
            viewModel.prepareCameraController(
                context,
                cameraFrameAnalyzer
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(color = colorResource(id = R.color.gray_900)),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                CameraPreview(
                    controller = remember { cameraController },
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
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .zIndex(1f)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                ObjectCounter(objectCount = detections.size)
            }
        }
    }
}