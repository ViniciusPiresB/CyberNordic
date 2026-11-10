// Ajustes do app: é aqui que se muda tempos, distância, gestos e falas.

// Tamanho mínimo da mão, em % do menor lado da imagem. Mãos menores são ignoradas por estarem longe.
export const TAMANHO_MINIMO_MAO = 25;

// Tempo segurando o mesmo gesto até ele ativar
export const TEMPO_SEGURAR_GESTO_MS = 1000;

// Falhas rápidas de leitura até este tempo não zeram a contagem
export const TOLERANCIA_LEITURA_MS = 250;

export const GESTOS = {
  aberta:    { emoji: "🖐️", nome: "Mão aberta",         audio: "assets/audios/pronto-para-a-missao.mp3" },  // "Patrulheiro espacial Buzz Lightyear, pronto para a missão"
  um:        { emoji: "☝️", nome: "1 dedo levantado",   audio: "assets/audios/comando-estelar.mp3" },       // "Eu sou Buzz Lightyear, do comando estelar"
  dois:      { emoji: "✌️", nome: "2 dedos levantados", audio: "assets/audios/caipira.mp3" },               // "Não sou um brinquedo, sou um patrulheiro. Caipira"
  tres:      { emoji: "3️⃣", nome: "3 dedos levantados", audio: "assets/audios/missao-incompleta.mp3" },     // "Atenção, missão incompleta"
  rock:      { emoji: "🤘", nome: "Rock",               audio: "assets/audios/sem-imaginacao.mp3" },        // "Sem imaginação não tem diversão"
  hangLoose: { emoji: "🤙", nome: "Hang loose",         audio: "assets/audios/brincar-com-a-crianca.mp3" }, // "Missão atualizada, brincar com a criança"
  fechada:   { emoji: "✊", nome: "Mão fechada",        audio: "assets/audios/ao-infinito.mp3" }            // "Ao infinito..."
};

// Valor usado quando a mão está próxima, mas não forma nenhum gesto conhecido
export const GESTO_DESCONHECIDO = "?";
