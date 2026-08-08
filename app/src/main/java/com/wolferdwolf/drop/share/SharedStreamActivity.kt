package com.wolferdwolf.drop.share

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import com.wolferdwolf.drop.MainActivity
import com.wolferdwolf.drop.ocr.ImageOcrProcessor
import com.wolferdwolf.drop.pdf.PdfImportActivity

/** Receives non-text ACTION_SEND streams and forwards them into Drop's universal intake flow. */
class SharedStreamActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) route(intent) else finish()
    }

    private fun route(incoming: Intent?) {
        val uri = incoming?.streamUri()
        val mimeType = incoming?.type.orEmpty()
        if (incoming?.action != Intent.ACTION_SEND || uri == null) {
            fail("Drop could not read the shared item.")
            return
        }

        when {
            mimeType.startsWith("image/") -> {
                ImageOcrProcessor.process(
                    this,
                    uri,
                    onSuccess = { extractedText ->
                        val clean = extractedText.trim()
                        if (clean.isBlank()) {
                            fail("No readable text was found in this image.")
                        } else {
                            startActivity(
                                Intent(this, MainActivity::class.java)
                                    .setAction(Intent.ACTION_SEND)
                                    .setType("text/plain")
                                    .putExtra(Intent.EXTRA_TEXT, clean)
                                    .putExtra(MainActivity.EXTRA_SOURCE_TYPE, com.wolferdwolf.drop.data.SavedSourceType.IMAGE.name)
                            )
                            finish()
                        }
                    },
                    onFailure = { message -> fail(message) }
                )
            }
            mimeType == "application/pdf" -> {
                startActivity(
                    Intent(this, PdfImportActivity::class.java)
                        .setData(uri)
                        .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                )
                finish()
            }
            else -> fail("This shared file type is not supported yet.")
        }
    }

    private fun fail(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }

    @Suppress("DEPRECATION")
    private fun Intent.streamUri(): Uri? = getParcelableExtra(Intent.EXTRA_STREAM)
}
