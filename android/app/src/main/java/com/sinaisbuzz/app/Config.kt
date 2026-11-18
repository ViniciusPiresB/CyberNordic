package com.sinaisbuzz.app

import com.google.mediapipe.tasks.core.Delegate

// Ajustes do app: é aqui que se muda tempos, distância, gestos e falas.
object Config {
    // Tamanho mínimo da mão, em % do menor lado da imagem. Mãos menores são ignoradas por estarem longe.
    const val TAMANHO_MINIMO_MAO = 25f

    // Tempo segurando o mesmo gesto até ele ativar
    const val TEMPO_SEGURAR_GESTO_MS = 1000L

    // Falhas rápidas de leitura até este tempo não zeram a contagem
    const val TOLERANCIA_LEITURA_MS = 250L

    // Quantas mãos acompanhar ao mesmo tempo. Com todas as vagas ocupadas o detector de palma
    // deixa de rodar, o que deixa a análise bem mais rápida; por isso, 1 vaga.
    const val MAXIMO_MAOS = 1

    // Se a mão acompanhada ficar distante por este tempo, a região dela é escondida (pintada de preto)
    // para que o detector encontre uma mão próxima no lugar
    const val TEMPO_ATE_ESCONDER_MS = 300L

    // Por quanto tempo uma mão distante fica escondida
    const val TEMPO_ESCONDIDA_MS = 1500L

    // Folga ao redor da mão escondida, em fração do tamanho dela
    const val MARGEM_ESCONDER = 0.3f

    // Tamanho da imagem analisada. Como só mãos próximas contam, uma imagem pequena basta.
    const val LARGURA_ANALISE = 320
    const val ALTURA_ANALISE = 240

    // Onde a análise roda. No Galaxy Tab A7 a GPU é cerca de 3x mais rápida que a CPU.
    // Se a GPU falhar ao iniciar, o app volta para a CPU.
    val PROCESSADOR = Delegate.GPU

    const val MODELO = "hand_landmarker.task"
}

enum class Gesto(val emoji: String, val nome: String, val audio: String?) {
    ABERTA("🖐️", "Mão aberta", "audios/pronto-para-a-missao.mp3"),           // "Patrulheiro espacial Buzz Lightyear, pronto para a missão"
    UM("☝️", "1 dedo levantado", "audios/comando-estelar.mp3"),               // "Eu sou Buzz Lightyear, do comando estelar"
    DOIS("✌️", "2 dedos levantados", "audios/caipira.mp3"),                   // "Não sou um brinquedo, sou um patrulheiro. Caipira"
    TRES("3️⃣", "3 dedos levantados", "audios/missao-incompleta.mp3"),         // "Atenção, missão incompleta"
    ROCK("🤘", "Rock", "audios/sem-imaginacao.mp3"),                          // "Sem imaginação não tem diversão"
    HANG_LOOSE("🤙", "Hang loose", "audios/brincar-com-a-crianca.mp3"),       // "Missão atualizada, brincar com a criança"
    FECHADA("✊", "Mão fechada", "audios/ao-infinito.mp3"),                   // "Ao infinito..."

    // Mão próxima, mas sem formar nenhum gesto conhecido
    DESCONHECIDO("❔", "Gesto não reconhecido", null)
}
