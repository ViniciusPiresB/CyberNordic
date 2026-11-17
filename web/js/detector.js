// Carrega o motor de visão e o modelo de mãos, que ficam dentro do próprio app
import { HandLandmarker, FilesetResolver } from "../vendor/mediapipe/vision_bundle.mjs";
import { LIMITE_CPU_RAPIDA_MS, PROCESSADOR } from "./config.js";

// Retorna { detector, processador }, com processador "CPU" ou "GPU"
export async function carregarDetector(aoAvancar) {
  aoAvancar("Carregando motor de visão (1/2)...");
  const vision = await FilesetResolver.forVisionTasks("vendor/mediapipe/wasm");

  aoAvancar("Carregando modelo de mãos (2/2)...");
  const criar = delegate => HandLandmarker.createFromOptions(vision, {
    baseOptions: { modelAssetPath: "vendor/mediapipe/hand_landmarker.task", delegate },
    runningMode: "VIDEO",
    // No modo vídeo o modelo só procura mãos novas quando há vaga sobrando;
    // com poucas vagas, mãos distantes "ocupam" o lugar da mão próxima
    numHands: 4
  });

  if (PROCESSADOR !== "auto") return { detector: await criar(PROCESSADOR), processador: PROCESSADOR };

  // A CPU funciona em todos os aparelhos testados; a GPU só entra quando a CPU é lenta demais
  const cpu = await criar("CPU");
  aoAvancar("Medindo a velocidade do aparelho...");
  if (tempoPorQuadro(cpu) <= LIMITE_CPU_RAPIDA_MS) return { detector: cpu, processador: "CPU" };

  try {
    const gpu = await criar("GPU");
    cpu.close();
    return { detector: gpu, processador: "GPU" };
  } catch (e) {
    console.warn("GPU indisponível, mantendo a CPU", e);
    return { detector: cpu, processador: "CPU" };
  }
}

// Tempo médio de análise de um quadro vazio, em ms (o primeiro é descartado por ser mais lento)
function tempoPorQuadro(detector) {
  const quadro = document.createElement("canvas");
  quadro.width = 480;
  quadro.height = 640;
  quadro.getContext("2d").fillRect(0, 0, quadro.width, quadro.height);

  detector.detectForVideo(quadro, performance.now());
  const inicio = performance.now();
  for (let i = 0; i < 3; i++) detector.detectForVideo(quadro, performance.now());
  return (performance.now() - inicio) / 3;
}
