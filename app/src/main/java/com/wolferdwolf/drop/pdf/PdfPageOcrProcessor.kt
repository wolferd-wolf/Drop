package com.wolferdwolf.drop.pdf

import android.content.Context
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions

object PdfPageOcrProcessor {
    const val MAX_OCR_PAGES = 3
    const val MAX_RENDER_DIMENSION = 1600

    data class Result(
        val text: String,
        val attemptedPages: Int,
        val totalPages: Int,
        val failedPages: Int
    )

    fun boundedPageCount(totalPages: Int): Int = totalPages.coerceAtLeast(0).coerceAtMost(MAX_OCR_PAGES)

    fun process(
        context: Context,
        uri: Uri,
        onSuccess: (Result) -> Unit,
        onFailure: (String) -> Unit
    ) {
        val descriptor = runCatching {
            context.contentResolver.openFileDescriptor(uri, "r")
        }.getOrNull()
        if (descriptor == null) {
            onFailure("Drop could not reopen this scanned PDF for offline text recognition.")
            return
        }

        val renderer = runCatching { PdfRenderer(descriptor) }
            .getOrElse {
                descriptor.closeQuietly()
                onFailure("Drop could not render this scanned PDF safely.")
                return
            }
        val totalPages = renderer.pageCount
        val pageLimit = boundedPageCount(totalPages)
        if (pageLimit == 0) {
            renderer.closeQuietly()
            descriptor.closeQuietly()
            onFailure("This PDF has no readable pages.")
            return
        }

        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        val pageTexts = mutableListOf<String>()
        var failedPages = 0

        fun finish() {
            val text = pageTexts.filter(String::isNotBlank)
                .joinToString("\n\n")
                .take(PdfTextExtractor.MAX_TEXT_LENGTH)
            recognizer.close()
            renderer.closeQuietly()
            descriptor.closeQuietly()
            onSuccess(
                Result(
                    text = text,
                    attemptedPages = pageLimit,
                    totalPages = totalPages,
                    failedPages = failedPages
                )
            )
        }

        fun processPage(index: Int) {
            if (index >= pageLimit) {
                finish()
                return
            }
            val rendered = runCatching { renderPage(renderer, index) }.getOrNull()
            if (rendered == null) {
                failedPages++
                processPage(index + 1)
                return
            }
            recognizer.process(InputImage.fromBitmap(rendered, 0))
                .addOnSuccessListener { pageTexts += it.text.trim() }
                .addOnFailureListener { failedPages++ }
                .addOnCompleteListener {
                    rendered.recycle()
                    processPage(index + 1)
                }
        }

        processPage(0)
    }

    private fun renderPage(renderer: PdfRenderer, index: Int): Bitmap {
        renderer.openPage(index).use { page ->
            val scale = minOf(
                1f,
                MAX_RENDER_DIMENSION.toFloat() / page.width,
                MAX_RENDER_DIMENSION.toFloat() / page.height
            )
            val width = (page.width * scale).toInt().coerceAtLeast(1)
            val height = (page.height * scale).toInt().coerceAtLeast(1)
            return Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).also { bitmap ->
                bitmap.eraseColor(android.graphics.Color.WHITE)
                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
            }
        }
    }

    private fun AutoCloseable.closeQuietly() {
        runCatching { close() }
    }
}
