package com.example.instalens.di

import android.app.Application
import android.content.Context
import androidx.camera.core.ImageAnalysis
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableFloatStateOf
import com.example.instalens.data.manager.datastore.LocalUserConfigManagerImpl
import com.example.instalens.data.manager.objectDetection.ObjectDetectionManagerImpl
import com.example.instalens.domain.manager.datastore.LocalUserConfigManager
import com.example.instalens.domain.manager.objectDetection.ObjectDetectionManager
import com.example.instalens.domain.model.Detection
import com.example.instalens.domain.usecases.detection.DetectObjectUseCase
import com.example.instalens.domain.usecases.userconfig.ReadUserConfig
import com.example.instalens.domain.usecases.userconfig.UserConfigUseCases
import com.example.instalens.domain.usecases.userconfig.WriteUserConfig
import com.example.instalens.utils.CameraFrameAnalyzer
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
class AppModule {
    @Provides
    @Singleton
    fun provideLocalUserConfigManager(
        application: Application
    ): LocalUserConfigManager = LocalUserConfigManagerImpl(application)

    @Provides
    @Singleton
    fun provideObjectDetectionManager(
        @ApplicationContext context: Context
    ): ObjectDetectionManager = ObjectDetectionManagerImpl(context)

    @Provides
    @Singleton
    fun provideCameraFrameAnalyzer(
        objectDetectionManager: ObjectDetectionManager,
        @ApplicationContext context: Context
    ): ImageAnalysis.Analyzer {
        return CameraFrameAnalyzer(
            objectDetectionManager = objectDetectionManager,
            onObjectDetectionResults = { detections: List<Detection> ->
                // Implementar lógica si es necesario, por ahora vacía
            },
            confidenceScoreState = mutableFloatStateOf(0.5f), // Valor por defecto, ajustar según necesidad
            context = context
        )
    }

    @Provides
    @Singleton
    fun provideUserConfigUseCases(
        localUserConfigManager: LocalUserConfigManager
    ): UserConfigUseCases = UserConfigUseCases(
        readUserConfig = ReadUserConfig(localUserConfigManager),
        writeUserConfig = WriteUserConfig(localUserConfigManager)
    )

    @Provides
    @Singleton
    fun provideDetectObjectUseCase(
        objectDetectionManager: ObjectDetectionManager
    ): DetectObjectUseCase = DetectObjectUseCase(
        objectDetectionManager = objectDetectionManager
    )
}