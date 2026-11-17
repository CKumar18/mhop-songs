package com.example.domain.importer

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.provider.OpenableColumns
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.devanagari.DevanagariTextRecognizerOptions
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.IOException
import kotlin.coroutines.resume
import kotlin.math.min

class DocumentExtractor(private val context: Context) {

    @Volatile
    private var pdfBoxInitialized = false

    suspend fun extractFromUri(uri: Uri): ExtractedDocumentResult = withContext(Dispatchers.IO) {
        val fileName = resolveFileName(uri)
        val fileType = detectFileType(uri, fileName)

        when (fileType) {
            ImportedFileType.DOCX -> extractDocx(uri, fileName)
            ImportedFileType.PDF -> extractPdf(uri, fileName)
            ImportedFileType.UNKNOWN -> ExtractedDocumentResult(
                fileName = fileName,
                fileType = ImportedFileType.UNKNOWN,
                rawText = "",
                errorMessage = "Unsupported file format. Please select a .docx or .pdf document."
            )
        }
    }

    private fun extractDocx(uri: Uri, fileName: String): ExtractedDocumentResult {
        return try {
            val stream = context.contentResolver.openInputStream(uri)
                ?: throw IOException("Unable to open selected DOCX file.")
            val result = stream.use { DocxTextExtractor.extract(it) }
            if (result.text.isBlank()) {
                ExtractedDocumentResult(
                    fileName = fileName,
                    fileType = ImportedFileType.DOCX,
                    rawText = "",
                    extractedLinks = result.links,
                    warnings = result.warnings + "No readable text found inside this DOCX file.",
                    errorMessage = "The DOCX document appears to be empty or contains only images."
                )
            } else {
                ExtractedDocumentResult(
                    fileName = fileName,
                    fileType = ImportedFileType.DOCX,
                    rawText = result.text,
                    extractedLinks = result.links,
                    warnings = result.warnings,
                )
            }
        } catch (e: Exception) {
            ExtractedDocumentResult(
                fileName = fileName,
                fileType = ImportedFileType.DOCX,
                rawText = "",
                errorMessage = e.message ?: "Failed to read DOCX document."
            )
        }
    }

    private suspend fun extractPdf(uri: Uri, fileName: String): ExtractedDocumentResult {
        ensurePdfBoxInitialized()
        return try {
            val textBuilder = StringBuilder()
            val stream = context.contentResolver.openInputStream(uri)
                ?: throw IOException("Unable to open selected PDF file.")

            stream.use { input ->
                PDDocument.load(input).use { document ->
                    val stripper = PDFTextStripper().apply {
                        sortByPosition = true
                    }
                    val totalPages = document.numberOfPages
                    for (page in 1..totalPages) {
                        stripper.startPage = page
                        stripper.endPage = page
                        val pageText = stripper.getText(document)
                        textBuilder.append(pageText)
                        if (page < totalPages) {
                            textBuilder.append("\n\u000C\n")
                        }
                    }
                }
            }

            val extractedText = textBuilder.toString().trim()
            val nonWhitespaceCount = extractedText.count { !it.isWhitespace() }

            if (nonWhitespaceCount >= 20) {
                ExtractedDocumentResult(
                    fileName = fileName,
                    fileType = ImportedFileType.PDF,
                    rawText = extractedText,
                )
            } else {
                // Scanned or image-only PDF detected — attempt on-device OCR for Latin & Devanagari
                runScannedPdfOcrFallback(uri, fileName)
            }
        } catch (e: Exception) {
            ExtractedDocumentResult(
                fileName = fileName,
                fileType = ImportedFileType.PDF,
                rawText = "",
                errorMessage = e.message ?: "Failed to read PDF document."
            )
        }
    }

    private suspend fun runScannedPdfOcrFallback(uri: Uri, fileName: String): ExtractedDocumentResult {
        val warnings = mutableListOf(
            "Scanned PDF detected (no selectable text). Running on-device OCR for English and Hindi/Devanagari; Telugu scanned images require manual review."
        )
        val ocrTextBuilder = StringBuilder()

        return try {
            val descriptor = context.contentResolver.openFileDescriptor(uri, "r")
                ?: return ExtractedDocumentResult(
                    fileName = fileName,
                    fileType = ImportedFileType.PDF,
                    rawText = "",
                    isScannedPdf = true,
                    ocrAttempted = false,
                    warnings = warnings,
                    errorMessage = "Could not open scanned PDF for OCR."
                )

            descriptor.use { pfd ->
                PdfRenderer(pfd).use { renderer ->
                    val maxPages = min(renderer.pageCount, 12)
                    if (renderer.pageCount > maxPages) {
                        warnings.add("OCR processed the first $maxPages of ${renderer.pageCount} pages.")
                    }
                    for (pageIndex in 0 until maxPages) {
                        renderer.openPage(pageIndex).use { page ->
                            val scale = 2
                            val bitmap = Bitmap.createBitmap(
                                (page.width * scale).coerceAtLeast(1),
                                (page.height * scale).coerceAtLeast(1),
                                Bitmap.Config.ARGB_8888
                            )
                            try {
                                Canvas(bitmap).drawColor(Color.WHITE)
                                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                                val pageOcr = recognizeBitmapText(bitmap)
                                if (pageOcr.isNotBlank()) {
                                    ocrTextBuilder.append(pageOcr).append("\n\u000C\n")
                                }
                            } finally {
                                bitmap.recycle()
                            }
                        }
                    }
                }
            }

            val ocrText = ocrTextBuilder.toString().trim()
            if (ocrText.isBlank()) {
                ExtractedDocumentResult(
                    fileName = fileName,
                    fileType = ImportedFileType.PDF,
                    rawText = "",
                    isScannedPdf = true,
                    ocrAttempted = true,
                    warnings = warnings,
                    errorMessage = "Scanned PDF could not be recognized automatically. Please convert to selectable text or enter the song manually."
                )
            } else {
                ExtractedDocumentResult(
                    fileName = fileName,
                    fileType = ImportedFileType.PDF,
                    rawText = ocrText,
                    isScannedPdf = true,
                    ocrAttempted = true,
                    warnings = warnings,
                )
            }
        } catch (e: Exception) {
            ExtractedDocumentResult(
                fileName = fileName,
                fileType = ImportedFileType.PDF,
                rawText = "",
                isScannedPdf = true,
                ocrAttempted = true,
                warnings = warnings,
                errorMessage = "Scanned PDF OCR failed: ${e.message ?: "unknown error"}. Please enter manually."
            )
        }
    }

    private suspend fun recognizeBitmapText(bitmap: Bitmap): String {
        val image = InputImage.fromBitmap(bitmap, 0)
        val devanagariText = runRecognizer(
            image = image,
            client = TextRecognition.getClient(DevanagariTextRecognizerOptions.Builder().build())
        )
        // Devanagari recognizer also reads Latin, but Latin recognizer can be cleaner for English-only pages
        val latinText = runRecognizer(
            image = image,
            client = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        )
        val hasDevanagari = devanagariText.any { it in '\u0900'..'\u097F' }
        return if (hasDevanagari) devanagariText else latinText.ifBlank { devanagariText }
    }

    private suspend fun runRecognizer(
        image: InputImage,
        client: com.google.mlkit.vision.text.TextRecognizer
    ): String = suspendCancellableCoroutine { cont ->
        client.process(image)
            .addOnSuccessListener { result ->
                client.close()
                if (cont.isActive) cont.resume(result.text.trim())
            }
            .addOnFailureListener {
                client.close()
                if (cont.isActive) cont.resume("")
            }
    }

    private fun ensurePdfBoxInitialized() {
        if (!pdfBoxInitialized) {
            synchronized(this) {
                if (!pdfBoxInitialized) {
                    PDFBoxResourceLoader.init(context.applicationContext)
                    pdfBoxInitialized = true
                }
            }
        }
    }

    private fun resolveFileName(uri: Uri): String {
        context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (idx >= 0) {
                    val name = cursor.getString(idx)
                    if (!name.isNullOrBlank()) return name
                }
            }
        }
        return uri.lastPathSegment?.substringAfterLast("/") ?: "document"
    }

    private fun detectFileType(uri: Uri, fileName: String): ImportedFileType {
        val mime = context.contentResolver.getType(uri)?.lowercase().orEmpty()
        val lowerName = fileName.lowercase()
        return when {
            lowerName.endsWith(".docx") || mime.contains("wordprocessingml") -> ImportedFileType.DOCX
            lowerName.endsWith(".pdf") || mime == "application/pdf" -> ImportedFileType.PDF
            else -> ImportedFileType.UNKNOWN
        }
    }
}
