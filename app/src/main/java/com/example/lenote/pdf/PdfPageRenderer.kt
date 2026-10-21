package com.example.lenote.pdf

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import android.util.LruCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.max

/**
 * Utilitário e gerenciador de renderização em segundo plano de páginas PDF ([PdfRenderer]).
 *
 * Características:
 * 1. Renderização assíncrona em [Dispatchers.IO] gerando [Bitmap] de alta definição (escala de 2x ou 3x);
 * 2. Cache em memória ([LruCache]) por chave `pdfPath:pageIndex:densityScale`;
 * 3. Gerenciamento seguro de [ParcelFileDescriptor] e [PdfRenderer], com fechamento e limpeza de recursos.
 */
object PdfPageRenderer {

    // Cache em memória (máximo ~48MB ou ~1/8 da memória livre)
    private val maxMemory = (Runtime.getRuntime().maxMemory() / 1024).toInt()
    private val cacheSize = max(8192, maxMemory / 8)

    private val bitmapCache = object : LruCache<String, Bitmap>(cacheSize) {
        override fun sizeOf(key: String, bitmap: Bitmap): Int {
            return bitmap.byteCount / 1024
        }
    }

    // Mapa de Renderers ativos indexados pelo caminho do arquivo PDF
    private val openRenderers = mutableMapOf<String, Pair<ParcelFileDescriptor, PdfRenderer>>()
    private val lock = Any()

    /**
     * Retorna o número total de páginas de um arquivo PDF.
     */
    fun getPageCount(file: File): Int {
        if (!file.exists() || file.length() == 0L) return 0
        synchronized(lock) {
            val pfd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY) ?: return 0
            return try {
                val renderer = PdfRenderer(pfd)
                val count = renderer.pageCount
                renderer.close()
                count
            } catch (_: Throwable) {
                0
            } finally {
                try { pfd.close() } catch (_: Throwable) {}
            }
        }
    }

    /**
     * Retorna um [Bitmap] em cache ou síncrono da página solicitada.
     */
    fun getCachedBitmap(pdfPath: String, pageIndex: Int, scale: Float = 2.0f): Bitmap? {
        val key = "$pdfPath:$pageIndex:$scale"
        return synchronized(lock) {
            bitmapCache.get(key)
        }
    }

    /**
     * Renderiza assincronamente a página solicitada do PDF com a escala especificada.
     */
    suspend fun renderPage(
        pdfPath: String,
        pageIndex: Int,
        scale: Float = 2.0f
    ): Bitmap? = withContext(Dispatchers.IO) {
        val file = File(pdfPath)
        if (!file.exists() || file.length() == 0L) return@withContext null

        val cacheKey = "$pdfPath:$pageIndex:$scale"
        synchronized(lock) {
            val cached = bitmapCache.get(cacheKey)
            if (cached != null && !cached.isRecycled) {
                return@withContext cached
            }
        }

        synchronized(lock) {
            // Re-check after lock
            val cached = bitmapCache.get(cacheKey)
            if (cached != null && !cached.isRecycled) {
                return@withContext cached
            }

            try {
                val (pfd, renderer) = getOrCreateRendererLocked(file)
                if (pageIndex < 0 || pageIndex >= renderer.pageCount) {
                    return@withContext null
                }

                val page = renderer.openPage(pageIndex)
                try {
                    val targetW = max(1, (page.width * scale).toInt())
                    val targetH = max(1, (page.height * scale).toInt())

                    val bitmap = Bitmap.createBitmap(targetW, targetH, Bitmap.Config.ARGB_8888)
                    // Fundo branco sólido por padrão (padrão de documento)
                    bitmap.eraseColor(Color.WHITE)

                    // Renderização de alta fidelidade
                    page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)

                    bitmapCache.put(cacheKey, bitmap)
                    bitmap
                } finally {
                    page.close()
                }
            } catch (e: Throwable) {
                null
            }
        }
    }

    private fun getOrCreateRendererLocked(file: File): Pair<ParcelFileDescriptor, PdfRenderer> {
        val path = file.absolutePath
        val existing = openRenderers[path]
        if (existing != null) {
            return existing
        }

        val pfd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
            ?: throw IllegalStateException("Não foi possível abrir o arquivo PDF: ${file.path}")
        val renderer = PdfRenderer(pfd)
        val pair = Pair(pfd, renderer)
        openRenderers[path] = pair
        return pair
    }

    /**
     * Fecha e descarta renderers associados a um arquivo PDF específico.
     */
    fun closeRenderer(pdfPath: String) {
        synchronized(lock) {
            val pair = openRenderers.remove(pdfPath)
            if (pair != null) {
                try { pair.second.close() } catch (_: Throwable) {}
                try { pair.first.close() } catch (_: Throwable) {}
            }
        }
    }

    /**
     * Limpa todo o cache e fecha todos os renderers abertos.
     */
    fun clearAll() {
        synchronized(lock) {
            bitmapCache.evictAll()
            for ((_, pair) in openRenderers) {
                try { pair.second.close() } catch (_: Throwable) {}
                try { pair.first.close() } catch (_: Throwable) {}
            }
            openRenderers.clear()
        }
    }
}
