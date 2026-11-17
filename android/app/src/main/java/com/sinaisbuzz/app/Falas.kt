package com.sinaisbuzz.app

import android.content.Context
import android.media.MediaPlayer
import android.util.Log

// Falas do Buzz Lightyear tocadas quando um gesto ativa.
// Uma fala nunca é interrompida por outro gesto: só termina sozinha (ou quando o app fecha).
class Falas(private val context: Context) {
    private var tocando: MediaPlayer? = null

    val falando get() = tocando != null

    // Retorna true se a fala começou; aoTerminar é chamado (na linha principal) quando ela acaba
    fun tocar(gesto: Gesto, aoTerminar: () -> Unit): Boolean {
        val arquivo = gesto.audio ?: return false
        if (falando) return false
        return try {
            val player = MediaPlayer()
            context.assets.openFd(arquivo).use { player.setDataSource(it.fileDescriptor, it.startOffset, it.length) }
            player.setOnCompletionListener {
                it.release()
                tocando = null
                aoTerminar()
            }
            player.prepare()
            player.start()
            tocando = player
            true
        } catch (e: Exception) {
            Log.w("Falas", "Não foi possível tocar $arquivo", e)
            false
        }
    }

    fun parar() {
        tocando?.release()
        tocando = null
    }
}
