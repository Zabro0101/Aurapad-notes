package com.example.export

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import com.example.models.Note
import com.example.models.Category
import org.json.JSONArray
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object NoteExporter {

    private val dateFormatter = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())

    // Convert Note checklist into readable text format
    fun formatChecklist(checklistJson: String?): String {
        if (checklistJson.isNullOrEmpty()) return ""
        return try {
            val sb = StringBuilder()
            val array = JSONArray(checklistJson)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val text = obj.optString("text")
                val checked = obj.optBoolean("checked")
                val marker = if (checked) "[x]" else "[ ]"
                sb.append("$marker $text\n")
            }
            sb.toString()
        } catch (e: Exception) {
            ""
        }
    }

    // 1. Export Note to TXT format
    fun exportToTxt(context: Context, note: Note, categoryName: String?): File {
        val dir = File(context.cacheDir, "exports").apply { mkdirs() }
        val sanitizedTitle = note.title.ifEmpty { "Untitled" }.replace(Regex("[^a-zA-Z0-9_-]"), "_")
        val file = File(dir, "$sanitizedTitle.txt")

        val contentBuilder = StringBuilder().apply {
            append("TITLE: ${note.title.ifEmpty { "Untitled" }}\n")
            append("DATE: ${dateFormatter.format(Date(note.modifiedAt))}\n")
            if (!categoryName.isNullOrEmpty()) {
                append("CATEGORY: $categoryName\n")
            }
            if (note.tags.isNotEmpty()) {
                append("TAGS: ${note.tags}\n")
            }
            append("------------------------------------------\n\n")
            append(note.content)
            
            val checklist = formatChecklist(note.checklistJson)
            if (checklist.isNotEmpty()) {
                append("\n\n--- CHECKLIST ---\n")
                append(checklist)
            }
        }

        FileOutputStream(file).use { out ->
            out.write(contentBuilder.toString().toByteArray())
        }
        return file
    }

    // 2. Export Note to HTML format
    fun exportToHtml(context: Context, note: Note, categoryName: String?): File {
        val dir = File(context.cacheDir, "exports").apply { mkdirs() }
        val sanitizedTitle = note.title.ifEmpty { "Untitled" }.replace(Regex("[^a-zA-Z0-9_-]"), "_")
        val file = File(dir, "$sanitizedTitle.html")

        val checklistHtml = StringBuilder()
        if (!note.checklistJson.isNullOrEmpty()) {
            try {
                val array = JSONArray(note.checklistJson)
                if (array.length() > 0) {
                    checklistHtml.append("<h3 style='margin-top: 30px;'>Checklist</h3>")
                    checklistHtml.append("<ul style='list-style-type: none; padding-left: 0;'>")
                    for (i in 0 until array.length()) {
                        val obj = array.getJSONObject(i)
                        val text = obj.optString("text")
                        val checked = obj.optBoolean("checked")
                        val checkedAttr = if (checked) "checked disabled" else "disabled"
                        checklistHtml.append("""
                            <li style="margin-bottom: 8px; display: flex; align-items: center;">
                                <input type="checkbox" $checkedAttr style="margin-right: 10px; transform: scale(1.2);">
                                <span style="font-size: 16px; ${if (checked) "text-decoration: line-through; color: #888;" else ""}">$text</span>
                            </li>
                        """.trimIndent())
                    }
                    checklistHtml.append("</ul>")
                }
            } catch (e: Exception) {
                // Ignore parsing errors
            }
        }

        // Clean content representation with breaklines converted to paragraph/breaks
        val formattedContent = note.content
            .replace("\n", "<br>")
            .replace("<br><br>", "</p><p>")

        val htmlContent = """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="utf-8">
                <meta name="viewport" content="width=device-width, initial-scale=1">
                <title>${note.title.ifEmpty { "Untitled" }}</title>
                <style>
                    body {
                        font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, Helvetica, Arial, sans-serif;
                        color: #1a1a1a;
                        background-color: #fcfcfc;
                        line-height: 1.6;
                        max-width: 700px;
                        margin: 0 auto;
                        padding: 40px 20px;
                    }
                    .note-card {
                        background: #ffffff;
                        border: 1px solid #e1e3e6;
                        border-radius: 12px;
                        padding: 30px;
                        box-shadow: 0 4px 12px rgba(0,0,0,0.03);
                    }
                    h1 {
                        font-size: 32px;
                        margin-top: 0;
                        margin-bottom: 10px;
                        color: #111111;
                    }
                    .meta {
                        font-size: 14px;
                        color: #666666;
                        margin-bottom: 25px;
                        border-bottom: 1px solid #eee;
                        padding-bottom: 15px;
                    }
                    .meta span {
                        margin-right: 15px;
                    }
                    .badge {
                        background-color: #e8f0fe;
                        color: #1a73e8;
                        padding: 3px 8px;
                        border-radius: 4px;
                        font-weight: 500;
                    }
                    .content-area {
                        font-size: 18px;
                        color: #333;
                    }
                    .tag {
                        display: inline-block;
                        background: #f1f3f4;
                        color: #5f6368;
                        padding: 2px 8px;
                        border-radius: 12px;
                        font-size: 13px;
                        margin-right: 6px;
                        margin-top: 4px;
                    }
                </style>
            </head>
            <body>
                <div class="note-card">
                    <h1>${note.title.ifEmpty { "Untitled" }}</h1>
                    <div class="meta">
                        <span>📅 ${dateFormatter.format(Date(note.modifiedAt))}</span>
                        ${if (!categoryName.isNullOrEmpty()) "<span>📂 Category: <span class='badge'>$categoryName</span></span>" else ""}
                    </div>
                    <div class="content-area">
                        <p>$formattedContent</p>
                    </div>
                    $checklistHtml
                    
                    ${if (note.tags.isNotEmpty()) {
                        "<div style='margin-top: 30px; border-top: 1px solid #eee; padding-top: 15px;'>" +
                        note.tags.split(",").joinToString("") { "<span class='tag'>#$it</span>" } +
                        "</div>"
                    } else ""}
                </div>
            </body>
            </html>
        """.trimIndent()

        FileOutputStream(file).use { out ->
            out.write(htmlContent.toByteArray())
        }
        return file
    }

    // 3. Export Note to genuine PDF document using local Android PdfDocument API
    fun exportToPdf(context: Context, note: Note, categoryName: String?): File {
        val dir = File(context.cacheDir, "exports").apply { mkdirs() }
        val sanitizedTitle = note.title.ifEmpty { "Untitled" }.replace(Regex("[^a-zA-Z0-9_-]"), "_")
        val file = File(dir, "$sanitizedTitle.pdf")

        val pdfDocument = PdfDocument()
        
        // A4 Paper definition: Width = 595pt, Height = 842pt (at 72 dpi)
        val pageWidth = 595
        val pageHeight = 842
        val margin = 45f
        
        val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create()
        val page = pdfDocument.startPage(pageInfo)
        val canvas: Canvas = page.canvas

        // Setup paints
        val textPaint = TextPaint().apply {
            isAntiAlias = true
            color = 0xFF202124.toInt()
            textSize = 14f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
        }

        val titlePaint = TextPaint().apply {
            isAntiAlias = true
            color = 0xFF1A1B1E.toInt()
            textSize = 24f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        }

        val metaPaint = Paint().apply {
            isAntiAlias = true
            color = 0xFF5F6368.toInt()
            textSize = 11f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.ITALIC)
        }

        val linePaint = Paint().apply {
            color = 0xFFDADCE0.toInt()
            strokeWidth = 1f
        }

        var currentY = margin

        // Draw note title
        val titleText = note.title.ifEmpty { "Untitled Note" }
        val titleWidth = pageWidth - (margin * 2).toInt()
        val titleLayout = StaticLayout.Builder.obtain(titleText, 0, titleText.length, titlePaint, titleWidth)
            .setAlignment(Layout.Alignment.ALIGN_NORMAL)
            .setLineSpacing(1f, 1f)
            .setIncludePad(false)
            .build()

        canvas.save()
        canvas.translate(margin, currentY)
        titleLayout.draw(canvas)
        canvas.restore()
        currentY += titleLayout.height + 10f

        // Draw date & metadata
        val dateText = "Modified: ${dateFormatter.format(Date(note.modifiedAt))}" +
                (if (!categoryName.isNullOrEmpty()) "  |  Category: $categoryName" else "")
        canvas.drawText(dateText, margin, currentY, metaPaint)
        currentY += 15f

        // Draw divider line
        canvas.drawLine(margin, currentY, pageWidth - margin, currentY, linePaint)
        currentY += 20f

        // Draw main body content
        val bodyContentText = note.content
        if (bodyContentText.isNotEmpty()) {
            val contentWidth = pageWidth - (margin * 2).toInt()
            val contentLayout = StaticLayout.Builder.obtain(bodyContentText, 0, bodyContentText.length, textPaint, contentWidth)
                .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                .setLineSpacing(1.1f, 1.1f)
                .setIncludePad(false)
                .build()

            canvas.save()
            canvas.translate(margin, currentY)
            contentLayout.draw(canvas)
            canvas.restore()
            currentY += contentLayout.height + 25f
        }

        // Draw Checklist if it's there
        val checklist = formatChecklist(note.checklistJson)
        if (checklist.isNotEmpty()) {
            val checkPaint = TextPaint().apply {
                isAntiAlias = true
                color = 0xFF1A73E8.toInt()
                textSize = 14f
                typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            }
            canvas.drawText("Checklist:", margin, currentY, checkPaint)
            currentY += 20f

            val listWidth = pageWidth - (margin * 2).toInt()
            val checklistLayout = StaticLayout.Builder.obtain(checklist, 0, checklist.length, textPaint, listWidth)
                .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                .setLineSpacing(1.1f, 1.1f)
                .setIncludePad(false)
                .build()

            canvas.save()
            canvas.translate(margin, currentY)
            checklistLayout.draw(canvas)
            canvas.restore()
            currentY += checklistLayout.height + 20f
        }

        // Draw Tags if existing
        if (note.tags.isNotEmpty()) {
            val tagsPaint = TextPaint().apply {
                isAntiAlias = true
                color = 0xFF5F6368.toInt()
                textSize = 12f
                typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.ITALIC)
            }
            val formattedTags = "Tags: " + note.tags.split(",").joinToString("   ") { "#$it" }
            canvas.drawText(formattedTags, margin, currentY, tagsPaint)
        }

        pdfDocument.finishPage(page)

        FileOutputStream(file).use { out ->
            pdfDocument.writeTo(out)
        }
        pdfDocument.close()
        return file
    }
}
