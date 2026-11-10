// Atualiza o projeto Android com a pasta web/ e gera dist/sinais-com-a-mao.apk
import { spawnSync } from "node:child_process";
import { copyFileSync, mkdirSync, readdirSync } from "node:fs";
import { homedir } from "node:os";
import { dirname, join } from "node:path";
import { fileURLToPath } from "node:url";

const raiz = join(dirname(fileURLToPath(import.meta.url)), "..");
const ferramentas = join(homedir(), "android-tools");

function javaHome() {
  if (process.env.JAVA_HOME) return process.env.JAVA_HOME;
  const jdk = readdirSync(ferramentas).find(nome => nome.startsWith("jdk-21"));
  if (!jdk) throw new Error(`JDK 21 não encontrado em ${ferramentas}. Defina JAVA_HOME.`);
  return join(ferramentas, jdk);
}

function executar(comando, pasta) {
  const { status } = spawnSync(comando, {
    cwd: pasta,
    shell: true,
    stdio: "inherit",
    env: { ...process.env, JAVA_HOME: javaHome() }
  });
  if (status !== 0) process.exit(status ?? 1);
}

executar("npx cap sync android", raiz);
const gradle = process.platform === "win32" ? ".\\gradlew.bat" : "./gradlew";
executar(`${gradle} assembleDebug --no-daemon --console=plain`, join(raiz, "android"));

mkdirSync(join(raiz, "dist"), { recursive: true });
const destino = join(raiz, "dist", "sinais-com-a-mao.apk");
copyFileSync(join(raiz, "android", "app", "build", "outputs", "apk", "debug", "app-debug.apk"), destino);
console.log(`APK gerado em ${destino}`);
