// Instalação pelo navegador. No APK (Capacitor) os arquivos já vêm dentro do app e nada disto é usado.
export function configurarInstalacao(botao) {
  if ("serviceWorker" in navigator && !window.Capacitor) {
    navigator.serviceWorker.register("service-worker.js")
      .catch(e => console.warn("Não foi possível registrar o service worker", e));
    // Pede para o aparelho não apagar os arquivos guardados quando faltar espaço
    navigator.storage?.persist?.();
  }

  let pedidoInstalacao = null;
  window.addEventListener("beforeinstallprompt", e => {
    e.preventDefault();
    pedidoInstalacao = e;
    botao.hidden = false;
  });
  botao.addEventListener("click", async () => {
    if (!pedidoInstalacao) return;
    botao.hidden = true;
    pedidoInstalacao.prompt();
    await pedidoInstalacao.userChoice;
    pedidoInstalacao = null;
  });
}
