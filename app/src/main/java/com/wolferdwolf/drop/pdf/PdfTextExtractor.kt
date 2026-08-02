package com.wolferdwolf.drop.pdf

import android.content.Context
import android.net.Uri
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper

data class PdfExtraction(
    val text: String,
    val pageCount: Int,
    val wasTruncated: Boolean,
    val hasEmbeddedText: Boolean
)

object PdfTextExtractor {
    const val MAX_PDF_BYTES = 20L * 1024L * 1024L
    const val MAX_PAGES = 30
    const val MAX_TEXT_LENGTH = 50_000

    fun extract(context: Context, uri: Uri, declaredSize: Long?): Result<PdfExtraction> = runCatching {
        require(declaredSize == null || declaredSize <= MAX_PDF_BYTES) {
            "This PDF is larger than the 20 MB offline-processing limit."
        }
        PDFBoxResourceLoader.init(context.applicationContext)
        context.contentResolver.openInputStream(uri)?.use { input ->
            PDDocument.load(input).use { document ->
                require(!document.isEncrypted) { "Password-protected PDFs are not supported yet." }
                val totalPages = document.numberOfPages
                require(totalPages > 0) { "This PDF has no readable pages." }
                val processedPages = minOf(totalPages, MAX_PAGES)
                val raw = PDFTextStripper().apply {
                    startPage = 1
                    endPage = processedPages
                    sortByPosition = true
                }.getText(document)
                val clean = normalize(raw)
                PdfExtraction(
                    text = clean.take(MAX_TEXT_LENGTH),
                    pageCount = totalPages,
                    wasTruncated = totalPages > MAX_PAGES || clean.length > MAX_TEXT_LENGTH,
                    hasEmbeddedText = clean.isNotBlank()
                )
            }
        } ?: error("Drop could not open this PDF. Try selecting it again from Files.")
    }

    internal fun normalize(value: String): String = value
        .replace("\u0000", "")
        .lineSequence()
        .map { it.trimEnd() }
        .joinToString("\n")
        .replace(Regex("\n{3,}"), "\n\n")
        .trim()
}
