package com.example.instalens.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import android.graphics.RectF
import android.os.VibrationEffect
import android.os.Vibrator
import android.speech.tts.TextToSpeech
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.compose.runtime.State
import com.example.instalens.domain.manager.objectDetection.ObjectDetectionManager
import com.example.instalens.domain.model.Detection
import com.example.instalens.presentation.mainActivity.MainActivity
import javax.inject.Inject

class CameraFrameAnalyzer @Inject constructor(
    private val objectDetectionManager: ObjectDetectionManager,
    private val onObjectDetectionResults: (List<Detection>) -> Unit,
    private val confidenceScoreState: State<Float>,
    private val context: Context
) : ImageAnalysis.Analyzer {
    private var frameSkipCounter = 0
    private var lastSpokenObstacle: String? = null

    override fun analyze(image: ImageProxy) {
        if (frameSkipCounter % 60 == 0) {
            val rotatedImageMatrix: Matrix =
                Matrix().apply {
                    postRotate(image.imageInfo.rotationDegrees.toFloat())
                }

            val rotatedBitmap: Bitmap = Bitmap.createBitmap(
                image.toBitmap(),
                0,
                0,
                image.width,
                image.height,
                rotatedImageMatrix,
                true
            )

            val objectDetectionResults = objectDetectionManager.detectObjectsInCurrentFrame(
                bitmap = rotatedBitmap,
                image.imageInfo.rotationDegrees,
                confidenceThreshold = confidenceScoreState.value
            )

            val obstacles = objectDetectionResults.filter { detection ->
                val obstacleCategories = listOf("person", "car", "dog", "cat", "truck", "bus", "chair")
                detection.detectedObjectName in obstacleCategories && isInLowerHalf(detection.boundingBox, image.height)
            }

            if (obstacles.isNotEmpty()) {
                val obstacle = obstacles.first()
                val obstacleName = obstacle.detectedObjectName

                val mainActivity = context as? MainActivity
                mainActivity?.getVibrator()?.vibrate(VibrationEffect.createOneShot(500, VibrationEffect.DEFAULT_AMPLITUDE))

                if (obstacleName != lastSpokenObstacle) {
                    val message = "Obstáculo: $obstacleName adelante"
                    mainActivity?.getTextToSpeech()?.speak(
                        message,
                        TextToSpeech.QUEUE_FLUSH,
                        null,
                        null
                    )
                    lastSpokenObstacle = obstacleName
                }
            } else {
                lastSpokenObstacle = null
            }

            onObjectDetectionResults(objectDetectionResults)
        }
        frameSkipCounter++

        image.close()
    }

    private fun isInLowerHalf(boundingBox: RectF, imageHeight: Int): Boolean {
        val centerY = (boundingBox.top + boundingBox.bottom) / 2
        return centerY > imageHeight / 2
    }
}