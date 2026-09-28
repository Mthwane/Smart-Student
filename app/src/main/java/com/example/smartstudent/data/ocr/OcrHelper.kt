package com.example.smartstudent.data.ocr

import android.content.Context
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

/**
 * Extracts raw text from an uploaded bank statement (image or PDF), entirely on-device
 * via ML Kit's Latin text recognizer — no API key, no upload to any server, free.
 *
 * The extracted text is what later gets sent to Gemini for categorization/analysis,
 * so OCR quality here directly affects how well transactions get parsed.
 */
object OcrHelper {

    // Reused across calls — ML Kit clients are meant to be long-lived rather than
    // recreated per scan, and re-using one also avoids re-triggering model checks.
    private val recognizer by lazy { TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS) }

    // Cap how large a rendered PDF page bitmap gets. Full-resolution statement scans
    // can be huge; 1800px on the long edge is still plenty sharp for OCR but avoids
    // multi-second-per-page bitmap allocation/rendering on large source PDFs.
    private const val MAX_RENDER_DIMENSION = 1800

    /**
     * Touches the recognizer once, off the main thread, as early as possible (called
     * from Application.onCreate). ML Kit's unbundled text-recognition model downloads
     * via Google Play Services on first use — doing that download now, in the
     * background, means it's usually already cached by the time a user scans anything,
     * instead of that download happening (and blocking) during their first real scan.
     */
    fun warmUp() {
        runCatching {
            // Just referencing `recognizer` starts the client/model init.
            recognizer
        }
    }

    suspend fun extractText(context: Context, uri: Uri): String = withContext(Dispatchers.IO) {
        val mimeType = context.contentResolver.getType(uri) ?: ""
        if (mimeType == "application/pdf" || uri.toString().endsWith(".pdf", ignoreCase = true)) {
            extractTextFromPdf(context, uri)
        } else {
            extractTextFromImageUri(context, uri)
        }
    }

    private suspend fun extractTextFromImageUri(context: Context, uri: Uri): String {
        val image = InputImage.fromFilePath(context, uri)
        val result = recognizer.process(image).await()
        return result.text
    }

    private suspend fun extractTextFromPdf(context: Context, uri: Uri): String = withContext(Dispatchers.IO) {
        val bitmaps = mutableListOf<Bitmap>()
        context.contentResolver.openFileDescriptor(uri, "r")?.use { pfd: ParcelFileDescriptor ->
            PdfRenderer(pfd).use { renderer ->
                for (pageIndex in 0 until renderer.pageCount) {
                    renderer.openPage(pageIndex).use { page ->
                        val scale = (MAX_RENDER_DIMENSION.toFloat() / maxOf(page.width, page.height)).coerceAtMost(2f)
                        val width = (page.width * scale).toInt().coerceAtLeast(1)
                        val height = (page.height * scale).toInt().coerceAtLeast(1)
                        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                        bitmap.eraseColor(android.graphics.Color.WHITE)
                        page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                        bitmaps.add(bitmap)
                    }
                }
            }
        }

        // Recognize every page concurrently instead of one-at-a-time — ML Kit's
        // recognizer handles concurrent calls fine, and for a multi-page statement
        // this turns "N pages x ~1-2s each, back to back" into roughly one page's
        // worth of wall-clock time.
        val pageTexts = bitmaps.map { bitmap ->
            async {
                val text = recognizer.process(InputImage.fromBitmap(bitmap, 0)).await().text
                bitmap.recycle()
                text
            }
        }.awaitAll()

        pageTexts.joinToString("\n")
    }
}
