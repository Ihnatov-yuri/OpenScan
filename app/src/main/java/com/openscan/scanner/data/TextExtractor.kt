package com.openscan.scanner.data

import android.content.Context
import android.net.Uri
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.tasks.await
import java.io.File

/** On-device OCR over a document's page images using ML Kit Text Recognition. */
object TextExtractor {

    suspend fun extract(context: Context, pages: List<File>): String {
        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        try {
            val builder = StringBuilder()
            pages.forEachIndexed { index, file ->
                val image = InputImage.fromFilePath(context, Uri.fromFile(file))
                val text = recognizer.process(image).await().text
                if (pages.size > 1) builder.append("──── Page ${index + 1} ────\n")
                builder.append(text.ifBlank { "(no text detected)" }).append("\n\n")
            }
            return builder.toString().trim()
        } finally {
            recognizer.close()
        }
    }
}
