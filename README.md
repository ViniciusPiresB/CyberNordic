# Sinais com a Mão

App que reconhece gestos da mão pela câmera do celular e toca falas do Buzz Lightyear.
Depois de instalado, funciona sem internet e sem computador.

## Gestos

Para ativar, a mão precisa estar perto da câmera e segurar o gesto por 1 segundo.

| Gesto | Fala |
|---|---|
| 🖐️ Mão aberta | "Patrulheiro espacial Buzz Lightyear, pronto para a missão" |
| ☝️ 1 dedo | "Eu sou Buzz Lightyear, do comando estelar" |
| ✌️ 2 dedos | "Não sou um brinquedo, sou um patrulheiro. Caipira" |
| 3️⃣ 3 dedos | "Atenção, missão incompleta" |
| 🤘 Rock | "Sem imaginação não tem diversão" |
| 🤙 Hang loose | "Missão atualizada, brincar com a criança" |
| ✊ Mão fechada | "Ao infinito..." |

Há duas versões com o mesmo comportamento:

- **App Android nativo** (Kotlin), em `android/`: a versão principal, mais rápida.
- **Versão para navegador**, em `web/`: roda em qualquer navegador moderno e pode ser instalada pelo link.

Os áudios, ícones e o modelo de mãos ficam em `web/` e são usados pelas duas.

## Estrutura

```text
sinais-com-a-mao/
├── android/                         App Android nativo (Kotlin)
│   └── app/src/main/java/com/sinaisbuzz/app/
│       ├── Config.kt                Tempos, distância, processador, gestos e falas
│       ├── MainActivity.kt          Ponto de entrada: câmera, tela e reconhecimento
│       ├── RegrasDeGestos.kt        Regras de reconhecimento e tempo de segurar
│       ├── DetectorDeMaos.kt        Carrega o MediaPipe
│       ├── SobreposicaoMaos.kt      Desenha os pontos da mão
│       └── Falas.kt                 Toca os áudios
├── web/                             Versão para navegador
│   ├── index.html                   Tela
│   ├── css/estilos.css      Aparência
│   ├── js/
│   │   ├── config.js        Tempos, distância, gestos e falas
│   │   ├── app.js           Ponto de entrada: câmera, tela e laço principal
│   │   ├── gestos.js        Regras de reconhecimento
│   │   ├── detector.js      Carrega o MediaPipe
│   │   ├── desenho.js       Desenha os pontos da mão
│   │   ├── falas.js         Toca os áudios
│   │   └── instalacao.js    Instalação pelo navegador
│   ├── assets/                      Áudios e ícones (usados também pelo APK)
│   ├── vendor/mediapipe/            Motor de visão e modelo (não editar)
│   ├── manifest.json                Dados do app para instalação pelo navegador
│   └── service-worker.js            Guarda os arquivos para uso sem internet
├── scripts/gerar-apk.mjs            Gera o APK
└── dist/                            APK pronto (gerado, não vai para o git)
```

## Ajustes comuns

No app Android, em `android/app/src/main/java/com/sinaisbuzz/app/Config.kt`. Na versão para navegador, em `web/js/config.js`.

- Tempo segurando o gesto até ativar (`TEMPO_SEGURAR_GESTO_MS`).
- Quão perto a mão precisa estar (`TAMANHO_MINIMO_MAO`).
- Emoji, nome e áudio de cada gesto.
- No Android: `PROCESSADOR` escolhe GPU ou CPU, e `MAXIMO_MAOS` quantas mãos procurar.

## Gerar o APK

Requer Node.js, JDK 21 e Android SDK 36. O SDK é indicado em `android/local.properties`.

```bash
npm run apk
```

O arquivo sai em `dist/sinais-com-a-mao.apk`. Para instalar pelo cabo, com a depuração USB ativa:

```bash
adb install -r dist/sinais-com-a-mao.apk
```

## Versão pelo navegador

A pasta `web/` pode ser publicada em qualquer hospedagem HTTPS. Ao adicionar ou trocar arquivos,
atualize a lista e aumente a versão de `CACHE` em `web/service-worker.js`.
