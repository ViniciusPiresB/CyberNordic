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

## Estrutura

```text
sinais-com-a-mao/
├── web/                     Código do app (é aqui que se edita)
│   ├── index.html           Tela
│   ├── css/estilos.css      Aparência
│   ├── js/
│   │   ├── config.js        Tempos, distância, gestos e falas
│   │   ├── app.js           Ponto de entrada: câmera, tela e laço principal
│   │   ├── gestos.js        Regras de reconhecimento
│   │   ├── detector.js      Carrega o MediaPipe
│   │   ├── desenho.js       Desenha os pontos da mão
│   │   ├── falas.js         Toca os áudios
│   │   └── instalacao.js    Instalação pelo navegador
│   ├── assets/              Áudios e ícones
│   ├── vendor/mediapipe/    Motor de visão e modelo (não editar)
│   ├── manifest.json        Dados do app para instalação pelo navegador
│   └── service-worker.js    Guarda os arquivos para uso sem internet
├── android/                 Projeto Android (permissões e ícones do APK)
├── scripts/gerar-apk.mjs    Gera o APK
└── dist/                    APK pronto (gerado, não vai para o git)
```

## Ajustes comuns

Tudo fica em `web/js/config.js`:

- `TEMPO_SEGURAR_GESTO_MS`: tempo segurando o gesto até ativar.
- `TAMANHO_MINIMO_MAO`: quão perto a mão precisa estar.
- `GESTOS`: emoji, nome e áudio de cada gesto.

## Gerar o APK

Requer Node.js, JDK 21 e Android SDK 36. O SDK é indicado em `android/local.properties`.

```bash
npm install
npm run apk
```

O arquivo sai em `dist/sinais-com-a-mao.apk`. Para instalar pelo cabo, com a depuração USB ativa:

```bash
adb install -r dist/sinais-com-a-mao.apk
```

## Versão pelo navegador

A pasta `web/` pode ser publicada em qualquer hospedagem HTTPS. Ao adicionar ou trocar arquivos,
atualize a lista e aumente a versão de `CACHE` em `web/service-worker.js`.
