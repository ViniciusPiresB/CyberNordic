package com.sinaisbuzz.app

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.SystemClock
import android.util.Size
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.core.resolutionselector.AspectRatioStrategy
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarkerResult
import com.sinaisbuzz.app.databinding.ActivityMainBinding
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

// Ponto de entrada: liga câmera, reconhecimento, tela e som
class MainActivity : AppCompatActivity() {

    private lateinit var tela: ActivityMainBinding
    private lateinit var falas: Falas

    // Toda a análise roda nesta linha de execução, fora da tela
    private val analise: ExecutorService = Executors.newSingleThreadExecutor()
    private val estabilizador = EstabilizadorDeGesto()

    private var detector: DetectorDeMaos? = null
    private var camera: ProcessCameraProvider? = null
    @Volatile private var rodando = false
    @Volatile private var cameraFrontal = true
    @Volatile private var gestoAnterior: Gesto? = null
    @Volatile private var falando = false
    private var quadros = 0
    private var inicioFps = 0L

    private val pedirCamera = registerForActivityResult(ActivityResultContracts.RequestPermission()) { permitido ->
        if (permitido) iniciar() else tela.txtStatus.setText(R.string.sem_permissao_camera)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        tela = ActivityMainBinding.inflate(layoutInflater)
        setContentView(tela.root)
        ajustarBordas()
        falas = Falas(this)

        tela.btnIniciar.setOnClickListener { if (rodando) parar() else iniciarComPermissao() }
        tela.btnTrocar.setOnClickListener {
            cameraFrontal = !cameraFrontal
            ligarCamera()
        }

        analise.execute {
            try {
                val carregado = DetectorDeMaos.criar(this)
                runOnUiThread {
                    detector = carregado
                    tela.txtStatus.text = "Modelo pronto (${carregado.processador}). Toque em Iniciar."
                    tela.btnIniciar.isEnabled = true
                }
            } catch (e: Exception) {
                runOnUiThread { tela.txtStatus.text = "Erro ao carregar o modelo: ${e.message}" }
            }
        }
    }

    // Solta a câmera quando o app sai da tela, senão outro app não consegue abri-la
    override fun onStop() {
        super.onStop()
        if (rodando) parar()
    }

    override fun onDestroy() {
        super.onDestroy()
        falas.parar()
        analise.execute { detector?.fechar() }
        analise.shutdown()
    }

    // ---------- Câmera ----------
    private fun iniciarComPermissao() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) iniciar()
        else pedirCamera.launch(Manifest.permission.CAMERA)
    }

    private fun iniciar() {
        rodando = true
        tela.btnIniciar.setText(R.string.parar)
        tela.btnTrocar.isEnabled = true
        tela.txtStatus.text = "Reconhecendo..."
        ligarCamera()
    }

    private fun parar() {
        rodando = false
        camera?.unbindAll()
        tela.sobreposicao.limpar()
        tela.btnIniciar.setText(R.string.iniciar)
        tela.btnTrocar.isEnabled = false
        tela.txtStatus.text = "Parado."
    }

    private fun ligarCamera() {
        val futuro = ProcessCameraProvider.getInstance(this)
        futuro.addListener({
            val provedor = futuro.get()
            camera = provedor
            if (!rodando) return@addListener

            // Prévia e análise no mesmo formato 4:3, para os pontos caírem em cima da mão.
            // A prévia fica nítida; a análise usa uma imagem pequena, que é mais rápida.
            val formatoPrevia = ResolutionSelector.Builder()
                .setAspectRatioStrategy(AspectRatioStrategy.RATIO_4_3_FALLBACK_AUTO_STRATEGY)
                .build()
            val formatoAnalise = ResolutionSelector.Builder()
                .setAspectRatioStrategy(AspectRatioStrategy.RATIO_4_3_FALLBACK_AUTO_STRATEGY)
                .setResolutionStrategy(
                    ResolutionStrategy(
                        Size(Config.LARGURA_ANALISE, Config.ALTURA_ANALISE),
                        ResolutionStrategy.FALLBACK_RULE_CLOSEST_HIGHER_THEN_LOWER
                    )
                )
                .build()
            val previa = Preview.Builder().setResolutionSelector(formatoPrevia).build()
            previa.surfaceProvider = tela.camera.surfaceProvider
            val quadros = ImageAnalysis.Builder()
                .setResolutionSelector(formatoAnalise)
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
                .build()
            quadros.setAnalyzer(analise, ::analisar)

            val lado = if (cameraFrontal) CameraSelector.DEFAULT_FRONT_CAMERA else CameraSelector.DEFAULT_BACK_CAMERA
            try {
                provedor.unbindAll()
                provedor.bindToLifecycle(this, lado, previa, quadros)
            } catch (e: Exception) {
                tela.txtStatus.text = "Erro ao abrir a câmera: ${e.message}"
            }
        }, ContextCompat.getMainExecutor(this))
    }

    // ---------- Reconhecimento ----------
    private fun analisar(quadro: ImageProxy) {
        val det = detector
        if (det == null || !rodando) {
            quadro.close()
            return
        }
        val (imagem, rotacao) = quadro.use { it.toBitmap() to it.imageInfo.rotationDegrees }
        // O MediaPipe gira a imagem por conta própria, o que é mais rápido que girar o bitmap
        val resultado = det.detectar(imagem, rotacao, SystemClock.uptimeMillis())
        processar(resultado, imagem.width, imagem.height, rotacao)
        esconderMaosDistantes(det, resultado, imagem.width, imagem.height)
    }

    // Se as vagas estão ocupadas só por mãos distantes há algum tempo, esconde essas mãos
    // para que o detector encontre uma mão próxima
    private var inicioSoDistantes = 0L
    private fun esconderMaosDistantes(det: DetectorDeMaos, resultado: HandLandmarkerResult, largura: Int, altura: Int) {
        val maos = resultado.landmarks()
        val soDistantes = maos.size >= Config.MAXIMO_MAOS &&
            maos.all { RegrasDeGestos.tamanhoMao(it, largura, altura) < Config.TAMANHO_MINIMO_MAO }
        val agora = SystemClock.uptimeMillis()
        if (!soDistantes) inicioSoDistantes = 0L
        else if (inicioSoDistantes == 0L) inicioSoDistantes = agora
        else if (agora - inicioSoDistantes >= Config.TEMPO_ATE_ESCONDER_MS) {
            maos.forEach { det.esconder(it, agora) }
            inicioSoDistantes = 0L
        }
    }

    // largura e altura são da imagem como sai do sensor; os pontos vêm nessa mesma orientação
    private fun processar(resultado: HandLandmarkerResult, largura: Int, altura: Int, rotacao: Int) {
        val maos = resultado.landmarks()

        // Escolhe a maior mão que passou no filtro de proximidade
        val tamanhos = maos.map { RegrasDeGestos.tamanhoMao(it, largura, altura) }
        val escolhida = tamanhos.indices
            .filter { tamanhos[it] >= Config.TAMANHO_MINIMO_MAO }
            .maxByOrNull { tamanhos[it] } ?: -1

        var leitura: Gesto? = null
        var detalhe = ""
        if (escolhida >= 0) {
            val dedos = RegrasDeGestos.dedosEstendidos(resultado.worldLandmarks()[escolhida])
            leitura = RegrasDeGestos.classificar(dedos)
            detalhe = "Dedos levantados: ${dedos.count { it }} · Tamanho da mão: ${"%.0f".format(tamanhos[escolhida])}%" +
                if (maos.size > 1) " · ${maos.size} mãos na imagem" else ""
        } else if (tamanhos.isNotEmpty()) {
            detalhe = "Mão longe demais (${"%.0f".format(tamanhos.max())}% < ${Config.TAMANHO_MINIMO_MAO.toInt()}%) — aproxime"
        }

        val agora = SystemClock.uptimeMillis()
        // Enquanto uma fala toca, os gestos não são verificados; a contagem recomeça quando ela acaba
        val pausado = falando
        val gesto = if (pausado) gestoAnterior else estabilizador.atualizar(leitura, agora)
        if (pausado) detalhe = "🔊 Falando... os gestos voltam a ser lidos quando a fala acabar"
        else estabilizador.progresso(agora)?.let { detalhe += " · Segure: $it%" }

        quadros++
        val fps = if (agora - inicioFps >= 1000) quadros.also { quadros = 0; inicioFps = agora } else null

        runOnUiThread {
            if (!rodando) return@runOnUiThread
            tela.sobreposicao.mostrar(maos, escolhida, largura, altura, rotacao, espelhar = cameraFrontal)
            // Toca a fala só quando o gesto muda, para não repetir enquanto a mão continua igual
            if (!falando && gesto != gestoAnterior) {
                gestoAnterior = gesto
                if (gesto != null) falando = falas.tocar(gesto) { falando = false }
            }
            mostrarGesto(gesto)
            tela.txtDetalhe.text = detalhe
            fps?.let { tela.txtStatus.text = "Reconhecendo... $it FPS" }
        }
    }

    private fun mostrarGesto(gesto: Gesto?) {
        if (gesto == null) {
            tela.txtEmoji.text = "✋"
            tela.txtNome.setText(R.string.aproxime_a_mao)
        } else {
            tela.txtEmoji.text = gesto.emoji
            tela.txtNome.text = gesto.nome
        }
    }

    // Mantém o conteúdo fora das barras do sistema (Android 15+ desenha atrás delas)
    private fun ajustarBordas() {
        val paddingCabecalho = tela.cabecalho.paddingTop
        ViewCompat.setOnApplyWindowInsetsListener(tela.raiz) { _, insets ->
            val barras = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            tela.cabecalho.updatePadding(top = paddingCabecalho + barras.top)
            tela.raiz.updatePadding(left = barras.left, right = barras.right, bottom = barras.bottom)
            insets
        }
    }
}
