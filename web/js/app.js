// Ponto de entrada: liga câmera, reconhecimento, tela e som
import { GESTOS, GESTO_DESCONHECIDO, TAMANHO_MINIMO_MAO } from "./config.js";
import { carregarDetector } from "./detector.js";
import { classificarGesto, criarEstabilizador, dedosEstendidos, tamanhoMao } from "./gestos.js";
import { desenharMao } from "./desenho.js";
import { tocarFala } from "./falas.js";
import { configurarInstalacao } from "./instalacao.js";

const video = document.getElementById("video");
const canvas = document.getElementById("canvas");
const ctx = canvas.getContext("2d");
const btnIniciar = document.getElementById("btnIniciar");
const btnTrocar = document.getElementById("btnTrocar");
const elGesto = document.getElementById("gesto");
const elNome = document.getElementById("nome");
const elDetalhe = document.getElementById("detalhe");
const elStatus = document.getElementById("status");

const estabilizador = criarEstabilizador();

let detector = null;
let stream = null;
let rodando = false;
let cameraFrontal = true;
let ultimoTempo = -1;
let gestoAnterior = null;
let quadros = 0, inicioFps = performance.now();

// ---------- Câmera ----------
async function abrirCamera() {
  if (stream) stream.getTracks().forEach(t => t.stop());
  stream = await navigator.mediaDevices.getUserMedia({
    video: { facingMode: cameraFrontal ? "user" : "environment", width: { ideal: 640 }, height: { ideal: 480 } },
    audio: false
  });
  video.srcObject = stream;
  await video.play();
  canvas.classList.toggle("espelhado", cameraFrontal);
}

async function iniciarOuParar() {
  if (rodando) {
    rodando = false;
    if (stream) stream.getTracks().forEach(t => t.stop());
    btnIniciar.textContent = "Iniciar";
    btnTrocar.disabled = true;
    elStatus.textContent = "Parado.";
    return;
  }
  try {
    await abrirCamera();
  } catch (e) {
    elStatus.textContent = "Erro ao abrir a câmera: " + e.message;
    return;
  }
  rodando = true;
  btnIniciar.textContent = "Parar";
  btnTrocar.disabled = false;
  elStatus.textContent = "Reconhecendo...";
  requestAnimationFrame(loop);
}

// ---------- Reconhecimento e tela ----------
function mostrarGesto(gesto) {
  if (gesto === null) {
    elGesto.textContent = "✋";
    elNome.textContent = "Aproxime a mão da câmera";
  } else if (gesto === GESTO_DESCONHECIDO) {
    elGesto.textContent = "❔";
    elNome.textContent = "Gesto não reconhecido";
  } else {
    elGesto.textContent = GESTOS[gesto].emoji;
    elNome.textContent = GESTOS[gesto].nome;
  }
}

function processarQuadro() {
  if (canvas.width !== video.videoWidth) canvas.width = video.videoWidth;
  if (canvas.height !== video.videoHeight) canvas.height = video.videoHeight;

  // Desenha o próprio quadro analisado, para os pontos ficarem sempre em cima da mão
  ctx.drawImage(video, 0, 0, canvas.width, canvas.height);
  const res = detector.detectForVideo(canvas, performance.now());

  // Escolhe a maior mão que passou no filtro de proximidade
  let escolhida = -1, maiorTamanho = 0;
  const tamanhos = res.landmarks.map(pontos => tamanhoMao(pontos, canvas.width, canvas.height));
  tamanhos.forEach((t, i) => { if (t >= TAMANHO_MINIMO_MAO && t > maiorTamanho) { maiorTamanho = t; escolhida = i; } });
  res.landmarks.forEach((pontos, i) => desenharMao(ctx, pontos, i === escolhida, cameraFrontal));

  let leitura = null, detalhe = "";
  if (escolhida >= 0) {
    const estendidos = dedosEstendidos(res.worldLandmarks[escolhida]);
    leitura = classificarGesto(estendidos);
    detalhe = `Dedos levantados: ${estendidos.filter(Boolean).length} · Tamanho da mão: ${maiorTamanho.toFixed(0)}%` +
      (tamanhos.length > 1 ? ` · ${tamanhos.length} mãos na imagem` : "");
  } else if (tamanhos.length) {
    detalhe = `Mão longe demais (${Math.max(...tamanhos).toFixed(0)}% < ${TAMANHO_MINIMO_MAO}%) — aproxime`;
  }

  const agora = performance.now();
  const gesto = estabilizador.atualizar(leitura, agora);
  const progresso = estabilizador.progresso(agora);
  if (progresso !== null) detalhe += ` · Segure: ${progresso.toFixed(0)}%`;

  // Toca a fala só quando o gesto muda, para não repetir enquanto a mão continua igual
  if (gesto !== gestoAnterior) {
    tocarFala(gesto);
    gestoAnterior = gesto;
  }
  mostrarGesto(gesto);
  elDetalhe.textContent = detalhe;

  quadros++;
  if (agora - inicioFps > 1000) {
    elStatus.textContent = `Reconhecendo... ${quadros} FPS`;
    quadros = 0;
    inicioFps = agora;
  }
}

function loop() {
  if (!rodando) return;
  if (video.readyState >= 2 && video.currentTime !== ultimoTempo) {
    ultimoTempo = video.currentTime;
    processarQuadro();
  }
  requestAnimationFrame(loop);
}

// ---------- Início ----------
configurarInstalacao(document.getElementById("btnInstalar"));

btnIniciar.addEventListener("click", iniciarOuParar);
btnTrocar.addEventListener("click", async () => {
  cameraFrontal = !cameraFrontal;
  try { await abrirCamera(); } catch (e) { elStatus.textContent = "Erro ao trocar câmera: " + e.message; }
});
// Solta a câmera quando o app sai da tela, senão outro app não consegue abri-la
document.addEventListener("visibilitychange", () => {
  if (document.hidden && rodando) iniciarOuParar();
});

const aviso = setTimeout(() => {
  elStatus.textContent += " (está demorando — recarregue a página)";
}, 30000);

carregarDetector(mensagem => { elStatus.textContent = mensagem; })
  .then(carregado => {
    detector = carregado.detector;
    elStatus.textContent = `Modelo pronto (${carregado.processador}). Toque em Iniciar.`;
    btnIniciar.disabled = false;
  })
  .catch(e => {
    elStatus.textContent = "Erro ao carregar o modelo: " + e.message;
    console.error(e);
  })
  .finally(() => clearTimeout(aviso));
