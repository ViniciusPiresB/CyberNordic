package com.sinaisbuzz.app

import com.google.mediapipe.tasks.components.containers.Landmark
import com.google.mediapipe.tasks.components.containers.NormalizedLandmark
import kotlin.math.hypot
import kotlin.math.min
import kotlin.math.sqrt

// Regras de reconhecimento: não acessa tela, câmera nem som.
object RegrasDeGestos {

    // Tamanho da mão = distância punho (0) → base do dedo médio (9), em % do menor lado da imagem.
    // Quanto mais perto da câmera, maior a mão.
    fun tamanhoMao(pontos: List<NormalizedLandmark>, largura: Int, altura: Int): Float {
        val dx = (pontos[0].x() - pontos[9].x()) * largura
        val dy = (pontos[0].y() - pontos[9].y()) * altura
        return hypot(dx, dy) / min(largura, altura) * 100
    }

    // Usa as coordenadas 3D em metros (worldLandmarks), que não dependem da rotação da mão.
    // Retorna [polegar, indicador, médio, anelar, mínimo]
    fun dedosEstendidos(m: List<Landmark>): BooleanArray {
        // Polegar: ponta longe da base do mínimo, comparado à largura da palma
        val larguraPalma = distancia(m[5], m[17])
        val polegar = distancia(m[4], m[17]) > larguraPalma * 1.35f && distancia(m[4], m[9]) > distancia(m[3], m[9])
        // Demais dedos: ponta bem mais longe do punho do que a articulação do meio
        val outros = listOf(8 to 6, 12 to 10, 16 to 14, 20 to 18).map { (ponta, meio) ->
            distancia(m[ponta], m[0]) > distancia(m[meio], m[0]) * 1.1f
        }
        return booleanArrayOf(polegar, *outros.toBooleanArray())
    }

    fun classificar(dedos: BooleanArray): Gesto {
        val (polegar, indicador, medio, anelar, minimo) = dedos
        // Combinações específicas vêm antes da simples contagem de dedos
        if (polegar && minimo && !indicador && !medio && !anelar) return Gesto.HANG_LOOSE
        if (indicador && minimo && !medio && !anelar) return Gesto.ROCK // com ou sem polegar
        return when (dedos.count { it }) {
            0 -> Gesto.FECHADA
            1 -> Gesto.UM
            2 -> Gesto.DOIS
            3 -> Gesto.TRES
            5 -> Gesto.ABERTA
            else -> Gesto.DESCONHECIDO
        }
    }

    private fun distancia(a: Landmark, b: Landmark): Float {
        val dx = a.x() - b.x()
        val dy = a.y() - b.y()
        val dz = a.z() - b.z()
        return sqrt(dx * dx + dy * dy + dz * dz)
    }
}

// Só ativa o gesto depois de segurá-lo pelo tempo definido em Config
class EstabilizadorDeGesto {
    private var candidato: Gesto? = null
    private var inicioCandidato = 0L
    private var ultimaLeitura = 0L
    var gestoAtivo: Gesto? = null
        private set

    fun atualizar(leitura: Gesto?, agora: Long): Gesto? {
        if (leitura == candidato) {
            ultimaLeitura = agora
        } else if (candidato == null || agora - ultimaLeitura > Config.TOLERANCIA_LEITURA_MS) {
            candidato = leitura
            inicioCandidato = agora
            ultimaLeitura = agora
        }
        val atual = candidato
        if (atual == null) gestoAtivo = null
        else if (agora - inicioCandidato >= Config.TEMPO_SEGURAR_GESTO_MS) gestoAtivo = atual
        return gestoAtivo
    }

    // Percentual até ativar o gesto em espera, ou null se não há nenhum
    fun progresso(agora: Long): Int? {
        val atual = candidato ?: return null
        if (atual == Gesto.DESCONHECIDO || atual == gestoAtivo) return null
        return min(99L, (agora - inicioCandidato) * 100 / Config.TEMPO_SEGURAR_GESTO_MS).toInt()
    }
}
