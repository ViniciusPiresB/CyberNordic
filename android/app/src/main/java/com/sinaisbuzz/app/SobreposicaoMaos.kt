package com.sinaisbuzz.app

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import com.google.mediapipe.tasks.components.containers.NormalizedLandmark
import kotlin.math.min

// Desenha os 21 pontos da mão por cima da imagem da câmera
class SobreposicaoMaos(context: Context, attrs: AttributeSet?) : View(context, attrs) {

    private var maos: List<List<NormalizedLandmark>> = emptyList()
    private var maoAtiva = -1
    private var larguraImagem = 1
    private var alturaImagem = 1
    private var rotacao = 0
    private var espelhar = false

    private val linha = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeCap = Paint.Cap.ROUND }
    private val ponto = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val contorno = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; color = Color.BLACK }
    private val numero = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.BLACK
        textAlign = Paint.Align.CENTER
        isFakeBoldText = true
    }

    // Os pontos vêm na orientação do sensor (larguraSensor x alturaSensor); rotacao os põe em pé.
    // espelhar: a prévia da câmera frontal aparece espelhada, então os pontos também.
    fun mostrar(
        maos: List<List<NormalizedLandmark>>, maoAtiva: Int,
        larguraSensor: Int, alturaSensor: Int, rotacao: Int, espelhar: Boolean
    ) {
        this.maos = maos
        this.maoAtiva = maoAtiva
        val emPe = rotacao % 180 == 0
        this.larguraImagem = if (emPe) larguraSensor else alturaSensor
        this.alturaImagem = if (emPe) alturaSensor else larguraSensor
        this.rotacao = rotacao
        this.espelhar = espelhar
        invalidate()
    }

    fun limpar() = mostrar(emptyList(), -1, 1, 1, 0, false)

    // Posição normalizada (0 a 1) do ponto na imagem como aparece na tela
    private fun naTela(p: NormalizedLandmark): Pair<Float, Float> {
        val (x, y) = when (rotacao) {
            90 -> (1 - p.y()) to p.x()
            180 -> (1 - p.x()) to (1 - p.y())
            270 -> p.y() to (1 - p.x())
            else -> p.x() to p.y()
        }
        return (if (espelhar) 1 - x else x) to y
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (maos.isEmpty()) return

        // Mesmo enquadramento da câmera (fitCenter): imagem inteira, centralizada
        val escalaTela = min(width.toFloat() / larguraImagem, height.toFloat() / alturaImagem)
        val inicioX = (width - larguraImagem * escalaTela) / 2
        val inicioY = (height - alturaImagem * escalaTela) / 2
        val escala = larguraImagem * escalaTela / 640f
        linha.strokeWidth = 6 * escala
        contorno.strokeWidth = 2 * escala
        numero.textSize = 11 * escala * 1.4f

        maos.forEachIndexed { indice, originais ->
            val ativa = indice == maoAtiva
            val alfa = if (ativa) 255 else 90
            val pontos = originais.map(::naTela)
            fun x(i: Int) = inicioX + pontos[i].first * larguraImagem * escalaTela
            fun y(i: Int) = inicioY + pontos[i].second * alturaImagem * escalaTela

            fun tracar(lista: IntArray, cor: Int) {
                linha.color = if (ativa) cor else CINZA
                linha.alpha = alfa
                for (n in 1 until lista.size) canvas.drawLine(x(lista[n - 1]), y(lista[n - 1]), x(lista[n]), y(lista[n]), linha)
            }
            tracar(PALMA, Color.WHITE)
            DEDOS.forEach { (cor, lista) -> tracar(lista, cor) }

            for (i in pontos.indices) {
                val cor = DEDOS.firstOrNull { (_, lista) -> i != 0 && i in lista }?.first ?: Color.WHITE
                ponto.color = if (ativa) cor else CINZA
                ponto.alpha = alfa
                contorno.alpha = alfa
                numero.alpha = alfa
                val raio = 9 * escala * 1.4f
                canvas.drawCircle(x(i), y(i), raio, ponto)
                canvas.drawCircle(x(i), y(i), raio, contorno)
                canvas.drawText(i.toString(), x(i), y(i) - (numero.ascent() + numero.descent()) / 2, numero)
            }
        }
    }

    private companion object {
        val CINZA = Color.parseColor("#888888")
        val PALMA = intArrayOf(5, 9, 13, 17)

        // Cada dedo com sua cor: polegar, indicador, médio, anelar, mínimo
        val DEDOS = listOf(
            Color.parseColor("#FF5252") to intArrayOf(0, 1, 2, 3, 4),
            Color.parseColor("#FFD740") to intArrayOf(0, 5, 6, 7, 8),
            Color.parseColor("#69F0AE") to intArrayOf(9, 10, 11, 12),
            Color.parseColor("#40C4FF") to intArrayOf(13, 14, 15, 16),
            Color.parseColor("#E040FB") to intArrayOf(0, 17, 18, 19, 20)
        )
    }
}
