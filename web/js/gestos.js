// Regras de reconhecimento: não acessa tela, câmera nem som.
import { GESTO_DESCONHECIDO, TEMPO_SEGURAR_GESTO_MS, TOLERANCIA_LEITURA_MS } from "./config.js";

const distancia = (a, b) => Math.hypot(a.x - b.x, a.y - b.y, a.z - b.z);

// Tamanho da mão = distância punho (0) → base do dedo médio (9), em % do menor lado da imagem.
// Quanto mais perto da câmera, maior a mão.
export function tamanhoMao(pontos, largura, altura) {
  const dx = (pontos[0].x - pontos[9].x) * largura;
  const dy = (pontos[0].y - pontos[9].y) * altura;
  return Math.hypot(dx, dy) / Math.min(largura, altura) * 100;
}

// Usa as coordenadas 3D em metros (worldLandmarks), que não dependem da rotação da mão.
// Retorna [polegar, indicador, médio, anelar, mínimo]
export function dedosEstendidos(m) {
  const estendidos = [];
  // Polegar: ponta longe da base do mínimo, comparado à largura da palma
  const larguraPalma = distancia(m[5], m[17]);
  estendidos.push(distancia(m[4], m[17]) > larguraPalma * 1.35 && distancia(m[4], m[9]) > distancia(m[3], m[9]));
  // Demais dedos: ponta bem mais longe do punho do que a articulação do meio
  for (const [ponta, meio] of [[8, 6], [12, 10], [16, 14], [20, 18]]) {
    estendidos.push(distancia(m[ponta], m[0]) > distancia(m[meio], m[0]) * 1.1);
  }
  return estendidos;
}

export function classificarGesto([polegar, indicador, medio, anelar, minimo]) {
  // Combinações específicas vêm antes da simples contagem de dedos
  if (polegar && minimo && !indicador && !medio && !anelar) return "hangLoose";
  if (indicador && minimo && !medio && !anelar) return "rock"; // com ou sem polegar
  const n = [polegar, indicador, medio, anelar, minimo].filter(Boolean).length;
  return { 0: "fechada", 1: "um", 2: "dois", 3: "tres", 5: "aberta" }[n] ?? GESTO_DESCONHECIDO;
}

// Só ativa o gesto depois de segurá-lo pelo tempo definido em config.js
export function criarEstabilizador() {
  let candidato = null, inicioCandidato = 0, ultimaLeitura = 0, gestoAtivo = null;

  return {
    atualizar(valor, agora) {
      if (valor === candidato) ultimaLeitura = agora;
      else if (candidato === null || agora - ultimaLeitura > TOLERANCIA_LEITURA_MS) {
        candidato = valor;
        inicioCandidato = ultimaLeitura = agora;
      }
      if (candidato === null) gestoAtivo = null;
      else if (agora - inicioCandidato >= TEMPO_SEGURAR_GESTO_MS) gestoAtivo = candidato;
      return gestoAtivo;
    },

    // Percentual até ativar o gesto em espera, ou null se não há nenhum
    progresso(agora) {
      if (candidato === null || candidato === GESTO_DESCONHECIDO || candidato === gestoAtivo) return null;
      return Math.min(99, (agora - inicioCandidato) / TEMPO_SEGURAR_GESTO_MS * 100);
    }
  };
}
