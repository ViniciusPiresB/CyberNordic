// Carrega o motor de visão e o modelo de mãos, que ficam dentro do próprio app
import { HandLandmarker, FilesetResolver } from "../vendor/mediapipe/vision_bundle.mjs";

export async function carregarDetector(aoAvancar) {
  aoAvancar("Carregando motor de visão (1/2)...");
  const vision = await FilesetResolver.forVisionTasks("vendor/mediapipe/wasm");

  aoAvancar("Carregando modelo de mãos (2/2)...");
  return HandLandmarker.createFromOptions(vision, {
    baseOptions: {
      modelAssetPath: "vendor/mediapipe/hand_landmarker.task",
      // GPU (WebGL) não detecta mãos em alguns Androids (ex.: Galaxy S25); CPU/XNNPACK funciona e é rápida
      delegate: "CPU"
    },
    runningMode: "VIDEO",
    // No modo vídeo o modelo só procura mãos novas quando há vaga sobrando;
    // com poucas vagas, mãos distantes "ocupam" o lugar da mão próxima
    numHands: 4
  });
}
