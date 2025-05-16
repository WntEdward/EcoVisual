package com.example.instalens.presentation.home.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import com.example.instalens.domain.model.Detection

@Composable
fun CameraOverlay(detections: List<Detection>) {
    Box(modifier = Modifier.fillMaxSize()) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            detections.forEach { detection ->
                val box = detection.boundingBox
                drawRect(
                    color = Color.Red,
                    topLeft = Offset(box.left, box.top),
                    size = Size(box.width(), box.height()),
                    style = Stroke(width = 2f)
                )
            }
        }
    }
}