package com.wolferdwolf.drop.ocr

import android.content.Context
import android.net.Uri
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions

object ImageOcrProcessor {
    fun process(
        context: Context,
        uri: Uri,
        onSuccess: (String) -> Unit,
        onFailure: (String) -> Unit
    ) {
        val image = runCatching { InputImage.fromFilePath(context, uri) }
            .getOrElse {
                onFailure("Drop could not open this image. Try another file or share it again.")
                return
            }

        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        recognizer.process(image)
            .addOnSuccessListener { result ->
                val text = result.text.trim()
                if (text.isBlank()) onFailure("No readable text was found in this image.")
                else onSuccess(text)
            }
            .addOnFailureListener {
                onFailure("Drop could not read this image offline. Try a clearer or less cropped photo.")
            }
            .addOnCompleteListener { recognizer.close() }
    }
}
