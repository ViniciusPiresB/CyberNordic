// Guarda tudo no aparelho para o app abrir sem PC e sem internet.
// Ao mudar a lista abaixo ou qualquer arquivo dela, aumente a versão para o celular baixar de novo.
const CACHE = "sinais-v2";

const ARQUIVOS = [
  "index.html",
  "manifest.json",
  "css/estilos.css",
  "js/app.js",
  "js/config.js",
  "js/desenho.js",
  "js/detector.js",
  "js/falas.js",
  "js/gestos.js",
  "js/instalacao.js",
  "assets/icones/icone-192.png",
  "assets/icones/icone-512.png",
  "assets/audios/pronto-para-a-missao.mp3",
  "assets/audios/comando-estelar.mp3",
  "assets/audios/caipira.mp3",
  "assets/audios/missao-incompleta.mp3",
  "assets/audios/sem-imaginacao.mp3",
  "assets/audios/brincar-com-a-crianca.mp3",
  "assets/audios/ao-infinito.mp3",
  "vendor/mediapipe/vision_bundle.mjs",
  "vendor/mediapipe/wasm/vision_wasm_internal.js",
  "vendor/mediapipe/wasm/vision_wasm_internal.wasm",
  "vendor/mediapipe/hand_landmarker.task"
];

self.addEventListener("install", evento => {
  evento.waitUntil(
    caches.open(CACHE)
      .then(cache => cache.addAll(ARQUIVOS.map(url => new Request(url, { cache: "reload" }))))
      .then(() => self.skipWaiting())
  );
});

self.addEventListener("activate", evento => {
  evento.waitUntil(
    caches.keys()
      .then(nomes => Promise.all(nomes.filter(n => n.startsWith("sinais-") && n !== CACHE).map(n => caches.delete(n))))
      .then(() => self.clients.claim())
  );
});

self.addEventListener("fetch", evento => {
  if (evento.request.method !== "GET") return;

  // A página tenta a rede para receber atualizações, mas com sinal ruim abre logo a cópia guardada
  if (evento.request.mode === "navigate") {
    const limite = new Promise((_, rejeitar) => setTimeout(rejeitar, 2000));
    evento.respondWith(
      Promise.race([fetch(evento.request), limite])
        .catch(() => caches.match("index.html", { ignoreSearch: true }))
    );
    return;
  }

  // Áudios, código, motor e modelo saem do aparelho; o que não estiver guardado vem da rede
  evento.respondWith(
    caches.match(evento.request).then(guardado => guardado || fetch(evento.request))
  );
});
