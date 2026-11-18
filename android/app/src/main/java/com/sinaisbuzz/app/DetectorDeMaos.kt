package com.sinaisbuzz.app

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.Log
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.tasks.components.containers.NormalizedLandmark
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

    // Regiões de mãos distantes que ficam pintadas de preto, para o detector procurar outra mão
    private class Escondida(val regiao: RectF, val ate: Long)
    private val escondidas = mutableListOf<Escondida>()
    private val tinta = Paint().apply { color = Color.BLACK }

    // rotacao: quanto girar a imagem da câmera para ficar em pé (0, 90, 180 ou 270)
    fun detectar(imagem: Bitmap, rotacao: Int, tempoMs: Long): HandLandmarkerResult {
        escondidas.removeAll { it.ate <= tempoMs }
        val entrada = if (escondidas.isEmpty()) imagem else esconderRegioes(imagem)

        // O modo vídeo exige tempos sempre crescentes
        ultimoTempo = maxOf(tempoMs, ultimoTempo + 1)
        val opcoes = ImageProcessingOptions.builder().setRotationDegrees(rotacao).build()
        return landmarker.detectForVideo(BitmapImageBuilder(entrada).build(), opcoes, ultimoTempo)
    }

    // Esconde esta mão por um tempo. Com a região preta, o modelo perde a mão e volta a procurar.
    // pontos: na orientação da imagem do sensor, como saem do detector
    fun esconder(pontos: List<NormalizedLandmark>, tempoMs: Long) {
        val esquerda = pontos.minOf { it.x() }
        val direita = pontos.maxOf { it.x() }
        val topo = pontos.minOf { it.y() }
        val base = pontos.maxOf { it.y() }
        val folgaX = (direita - esquerda) * Config.MARGEM_ESCONDER
        val folgaY = (base - topo) * Config.MARGEM_ESCONDER
        val regiao = RectF(esquerda - folgaX, topo - folgaY, direita + folgaX, base + folgaY)
        escondidas += Escondida(regiao, tempoMs + Config.TEMPO_ESCONDIDA_MS)
    }

    fun fechar() = landmarker.close()

    private fun esconderRegioes(imagem: Bitmap): Bitmap {
        val copia = if (imagem.isMutable) imagem else imagem.copy(Bitmap.Config.ARGB_8888, true)
        val tela = Canvas(copia)
        for (e in escondidas) {
            val r = e.regiao
            tela.drawRect(r.left * copia.width, r.top * copia.height, r.right * copia.width, r.bottom * copia.height, tinta)
        }
        return copia
    }

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
