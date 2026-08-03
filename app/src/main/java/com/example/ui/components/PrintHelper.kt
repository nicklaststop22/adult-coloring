package com.example.ui.components

import android.content.Context
import android.graphics.BitmapFactory
import android.graphics.RectF
import android.graphics.pdf.PdfDocument
import android.os.Bundle
import android.os.CancellationSignal
import android.os.Environment
import android.os.ParcelFileDescriptor
import android.print.PageRange
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintDocumentInfo
import android.print.PrintManager
import android.widget.Toast
import com.example.data.models.ColoringPage
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

object PrintablePageExporter {

    fun printColoringPage(context: Context, page: ColoringPage) {
        try {
            val printManager = context.getSystemService(Context.PRINT_SERVICE) as? PrintManager
            if (printManager != null) {
                val jobName = "InkZen - ${page.title}"
                printManager.print(
                    jobName,
                    object : PrintDocumentAdapter() {
                        override fun onLayout(
                            oldAttributes: PrintAttributes?,
                            newAttributes: PrintAttributes?,
                            cancellationSignal: CancellationSignal?,
                            callback: LayoutResultCallback?,
                            extras: Bundle?
                        ) {
                            if (cancellationSignal?.isCanceled == true) {
                                callback?.onLayoutCancelled()
                                return
                            }
                            val info = PrintDocumentInfo.Builder("coloring_page.pdf")
                                .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
                                .setPageCount(1)
                                .build()
                            callback?.onLayoutFinished(info, true)
                        }

                        override fun onWrite(
                            pages: Array<out PageRange>?,
                            destination: ParcelFileDescriptor?,
                            cancellationSignal: CancellationSignal?,
                            callback: WriteResultCallback?
                        ) {
                            val bitmap = BitmapFactory.decodeFile(page.imagePath)
                            if (bitmap == null || destination == null) {
                                callback?.onWriteFailed("Cannot load image")
                                return
                            }

                            val document = PdfDocument()
                            val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
                            val pdfPage = document.startPage(pageInfo)
                            val canvas = pdfPage.canvas

                            val margin = 36f
                            val printableW = 595f - 2 * margin
                            val printableH = 842f - 2 * margin

                            val scale = minOf(printableW / bitmap.width, printableH / bitmap.height)
                            val destW = bitmap.width * scale
                            val destH = bitmap.height * scale

                            val left = margin + (printableW - destW) / 2f
                            val top = margin + (printableH - destH) / 2f

                            canvas.drawBitmap(bitmap, null, RectF(left, top, left + destW, top + destH), null)
                            document.finishPage(pdfPage)

                            try {
                                FileOutputStream(destination.fileDescriptor).use { out ->
                                    document.writeTo(out)
                                }
                                callback?.onWriteFinished(arrayOf(PageRange.ALL_PAGES))
                            } catch (e: Exception) {
                                callback?.onWriteFailed(e.localizedMessage)
                            } finally {
                                document.close()
                            }
                        }
                    },
                    null
                )
            } else {
                Toast.makeText(context, "Print service not available", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Toast.makeText(context, "Print error: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
        }
    }

    fun exportToPdf(context: Context, page: ColoringPage): String? {
        return try {
            val bitmap = BitmapFactory.decodeFile(page.imagePath) ?: return null

            val document = PdfDocument()
            val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
            val pdfPage = document.startPage(pageInfo)
            val canvas = pdfPage.canvas

            val margin = 36f
            val printableW = 595f - 2 * margin
            val printableH = 842f - 2 * margin

            val scale = minOf(printableW / bitmap.width, printableH / bitmap.height)
            val destW = bitmap.width * scale
            val destH = bitmap.height * scale

            val left = margin + (printableW - destW) / 2f
            val top = margin + (printableH - destH) / 2f

            val destRect = RectF(left, top, left + destW, top + destH)
            canvas.drawBitmap(bitmap, null, destRect, null)

            document.finishPage(pdfPage)

            val pdfFile = File(
                context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS),
                "InkZen_${page.title.replace(" ", "_")}_${page.id.take(6)}.pdf"
            )

            FileOutputStream(pdfFile).use { out ->
                document.writeTo(out)
            }
            document.close()

            pdfFile.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
