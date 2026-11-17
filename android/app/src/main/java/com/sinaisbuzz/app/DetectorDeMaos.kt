package com.sinaisbuzz.app

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.core.Delegate
import com.google.mediapipe.tasks.vision.core.ImageProcessingOptions
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarker
import com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarkerResult

// Encontra os 21 pontos de cada mão usando o modelo do MediaPipe que vem dentro do app
class DetectorDeMaos private constructor(
    private val landmarker: HandLandmarker,
    val processador: Delegate
) {
    private var ultimoTempo = 0L

    // rotacao: quanto girar a imagem da câmera para ficar em pé (0, 90, 180 ou 270)
    fun detectar(imagem: Bitmap, rotacao: Int, tempoMs: Long): HandLandmarkerResult {
        // O modo vídeo exige tempos sempre crescentes
        ultimoTempo = maxOf(tempoMs, ultimoTempo + 1)
        val opcoes = ImageProcessingOptions.builder().setRotationDegrees(rotacao).build()
        return landmarker.detectForVideo(BitmapImageBuilder(imagem).build(), opcoes, ultimoTempo)
    }

    fun fechar() = landmarker.close()

    companion object {
        fun criar(context: Context): DetectorDeMaos {
            if (Config.PROCESSADOR == Delegate.GPU) {
                try {
                    return DetectorDeMaos(criarLandmarker(context, Delegate.GPU), Delegate.GPU)
                } catch (e: Exception) {
                    Log.w("DetectorDeMaos", "GPU indisponível, usando a CPU", e)
                }
            }
            return DetectorDeMaos(criarLandmarker(context, Delegate.CPU), Delegate.CPU)
        }

        private fun criarLandmarker(context: Context, processador: Delegate): HandLandmarker {
            val opcoes = HandLandmarker.HandLandmarkerOptions.builder()
                .setBaseOptions(
                    BaseOptions.builder()
                        .setModelAssetPath(Config.MODELO)
                        .setDelegate(processador)
                        .build()
                )
                .setRunningMode(RunningMode.VIDEO)
                .setNumHands(Config.MAXIMO_MAOS)
                .build()
            return HandLandmarker.createFromOptions(context, opcoes)
        }
    }
}
