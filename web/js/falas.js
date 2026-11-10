// Falas do Buzz Lightyear tocadas quando um gesto ativa
import { GESTOS } from "./config.js";

const audios = {};
for (const [gesto, { audio }] of Object.entries(GESTOS)) {
  audios[gesto] = new Audio(audio);
  audios[gesto].preload = "auto";
}

let audioTocando = null;

export function tocarFala(gesto) {
  const audio = audios[gesto];
  if (!audio) return;
  if (audioTocando && audioTocando !== audio) audioTocando.pause();
  audio.currentTime = 0;
  audio.play().catch(e => console.warn("Não foi possível tocar o áudio", e));
  audioTocando = audio;
}
