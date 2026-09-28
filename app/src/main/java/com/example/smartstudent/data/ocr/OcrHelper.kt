package com.example.smartstudent.data.ocr

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

/**
 * On-device OCR using ML Kit's bundled Latin text recognizer (no model download, nothing
 * is uploaded by this step). PDFs are rendered and recognized one page at a time so a
 * long statement can't exhaust memory.
 */
object OcrHelper {
    private const val MAX_RENDER_DIMENSION = 1800
    private const val MAX_PDF_PAGES = 12

    private val recognizer by lazy { TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS) }

    suspend fun extractText(context: Context, uri: Uri): String = withContext(Dispatchers.IO) {
        val app = context.applicationContext
        val mime = app.contentResolver.getType(uri).orEmpty()
        if (mime == "application/pdf" || uri.toString().endsWith(".pdf", ignoreCase = true)) {
            fromPdf(app, uri)
        } else {
            recognizer.process(InputImage.fromFilePath(app, uri)).await().text
        }
    }

    private suspend fun fromPdf(context: Context, uri: Uri): String {
        val pfd = context.contentResolver.openFileDescriptor(uri, "r") ?: return ""
        val out = StringBuilder()
        pfd.use {
            PdfRenderer(it).use { renderer ->
                for (i in 0 until minOf(renderer.pageCount, MAX_PDF_PAGES)) {
                    renderer.openPage(i).use { page ->
                        val scale = (MAX_RENDER_DIMENSION.toFloat() / maxOf(page.width, page.height)).coerceAtMost(2f)
                        val bitmap = Bitmap.createBitmap(
                            (page.width * scale).toInt().coerceAtLeast(1),
                            (page.height * scale).toInt().coerceAtLeast(1),
                            Bitmap.Config.ARGB_8888
                        )
                        try {
                            bitmap.eraseColor(Color.WHITE)
                            page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                            out.appendLine(recognizer.process(InputImage.fromBitmap(bitmap, 0)).await().text)
                        } finally {
                            bitmap.recycle()
                        }
                    }
                }
            }
        }
        return out.toString()
    }
}
