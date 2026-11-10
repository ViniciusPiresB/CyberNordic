// Desenha os 21 pontos da mão sobre a imagem da câmera

// Cada dedo com sua cor: polegar, indicador, médio, anelar, mínimo
const DEDOS = [
  { cor: "#FF5252", pontos: [0, 1, 2, 3, 4] },
  { cor: "#FFD740", pontos: [0, 5, 6, 7, 8] },
  { cor: "#69F0AE", pontos: [9, 10, 11, 12] },
  { cor: "#40C4FF", pontos: [13, 14, 15, 16] },
  { cor: "#E040FB", pontos: [0, 17, 18, 19, 20] }
];
const PALMA = [5, 9, 13, 17];

export function desenharMao(ctx, pontos, ativa, espelhado) {
  const w = ctx.canvas.width, h = ctx.canvas.height;
  const escala = w / 640;
  const xy = i => [pontos[i].x * w, pontos[i].y * h];

  ctx.globalAlpha = ativa ? 1 : 0.35;
  ctx.lineCap = "round";
  ctx.lineWidth = 6 * escala;
  const linha = (lista, cor) => {
    ctx.strokeStyle = cor;
    ctx.beginPath();
    lista.forEach((i, n) => { const [x, y] = xy(i); n ? ctx.lineTo(x, y) : ctx.moveTo(x, y); });
    ctx.stroke();
  };
  linha(PALMA, ativa ? "#FFFFFF" : "#888");
  for (const d of DEDOS) linha(d.pontos, ativa ? d.cor : "#888");

  pontos.forEach((_, i) => {
    const [x, y] = xy(i);
    const dedo = DEDOS.find(d => d.pontos.includes(i) && i !== 0);
    ctx.fillStyle = ativa && dedo ? dedo.cor : ativa ? "#FFFFFF" : "#888";
    ctx.strokeStyle = "#000";
    ctx.lineWidth = 2 * escala;
    ctx.beginPath();
    ctx.arc(x, y, 9 * escala, 0, Math.PI * 2);
    ctx.fill();
    ctx.stroke();

    // Número do ponto (desfaz o espelhamento para o texto não ficar invertido)
    ctx.save();
    ctx.translate(x, y);
    if (espelhado) ctx.scale(-1, 1);
    ctx.fillStyle = "#000";
    ctx.font = `bold ${11 * escala}px Arial`;
    ctx.textAlign = "center";
    ctx.textBaseline = "middle";
    ctx.fillText(i, 0, 0);
    ctx.restore();
  });
  ctx.globalAlpha = 1;
}
