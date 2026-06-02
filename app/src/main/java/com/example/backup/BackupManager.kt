package com.example.backup

import android.content.Context
import android.os.Environment
import android.util.Base64
import com.example.database.AppDatabase
import com.example.models.Category
import com.example.models.Note
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.w3c.dom.Element
import org.w3c.dom.Node
import java.io.ByteArrayInputStream
import java.io.File
import java.io.FileOutputStream
import javax.xml.parsers.DocumentBuilderFactory

object BackupManager {

    // 1. Local Storage Directory access (Public Documents or Private fallback)
    fun getLocalBackupDirectory(context: Context): File {
        val publicDocDir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS), "AuraPadBackups")
        val backupsDir = File(publicDocDir, "Backups")
        try {
            if (!backupsDir.exists()) {
                val created = backupsDir.mkdirs()
                if (!created && !backupsDir.exists()) {
                    throw Exception("Could not create public directory")
                }
            }
            // Test writing access to detect scoped storage write blocks
            val testFile = File(backupsDir, ".write_test")
            testFile.writeText("test")
            testFile.delete()
            return backupsDir
        } catch (e: Exception) {
            // Safe fallback to app-specific external file directory (fully user accessible under files/AuraPadBackups)
            val fallbackDir = File(context.getExternalFilesDir(null), "AuraPadBackups/Backups")
            if (!fallbackDir.exists()) {
                fallbackDir.mkdirs()
            }
            return fallbackDir
        }
    }

    fun getLocalExportDirectory(context: Context): File {
        val publicDocDir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS), "AuraPadBackups")
        val exportsDir = File(publicDocDir, "Exports")
        try {
            if (!exportsDir.exists()) {
                val created = exportsDir.mkdirs()
                if (!created && !exportsDir.exists()) {
                    throw Exception("Could not create public directory")
                }
            }
            // Test writing access
            val testFile = File(exportsDir, ".write_test")
            testFile.writeText("test")
            testFile.delete()
            return exportsDir
        } catch (e: Exception) {
            val fallbackDir = File(context.getExternalFilesDir(null), "AuraPadBackups/Exports")
            if (!fallbackDir.exists()) {
                fallbackDir.mkdirs()
            }
            return fallbackDir
        }
    }

    // Helper to escape characters in XML
    fun escapeXml(value: String?): String {
        if (value == null) return ""
        return value.replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&apos;")
    }

    // Helper to parse Base64 and write to local app folders
    private fun fileToBase64(file: File?): String {
        if (file == null || !file.exists()) return ""
        return try {
            val bytes = file.readBytes()
            Base64.encodeToString(bytes, Base64.NO_WRAP)
        } catch (e: Exception) {
            ""
        }
    }

    private fun base64ToFile(base64Str: String, destFile: File) {
        if (base64Str.isEmpty()) return
        try {
            val bytes = Base64.decode(base64Str, Base64.NO_WRAP)
            destFile.parentFile?.mkdirs()
            destFile.writeBytes(bytes)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // 2. Generate detailed XML representation for database entries and associated attachments
    suspend fun createBackupXml(context: Context): String = withContext(Dispatchers.IO) {
        val database = AppDatabase.getDatabase(context)
        val notes = database.noteDao.getUnarchivedNotesFlow().first() + database.noteDao.getArchivedNotesFlow().first()
        val categories = database.categoryDao.getAllCategories().first()

        val sb = java.lang.StringBuilder()
        sb.append("<?xml version=\"1.0\" encoding=\"utf-8\" standalone=\"yes\"?>\n")
        sb.append("<aurapad_backup>\n")
        sb.append("  <version>1</version>\n")

        // Serialize Categories
        sb.append("  <categories>\n")
        for (cat in categories) {
            sb.append("    <category>\n")
            sb.append("      <id>${cat.id}</id>\n")
            sb.append("      <name>${escapeXml(cat.name)}</name>\n")
            sb.append("      <colorHex>${escapeXml(cat.colorHex)}</colorHex>\n")
            sb.append("    </category>\n")
        }
        sb.append("  </categories>\n")

        // Serialize Notes
        sb.append("  <notes>\n")
        for (note in notes) {
            sb.append("    <note>\n")
            sb.append("      <id>${note.id}</id>\n")
            sb.append("      <title>${escapeXml(note.title)}</title>\n")
            sb.append("      <content>${escapeXml(note.content)}</content>\n")
            sb.append("      <checklistJson>${escapeXml(note.checklistJson)}</checklistJson>\n")
            sb.append("      <categoryId>${note.categoryId ?: ""}</categoryId>\n")
            sb.append("      <colorInt>${note.colorInt}</colorInt>\n")
            sb.append("      <isPinned>${note.isPinned}</isPinned>\n")
            sb.append("      <isArchived>${note.isArchived}</isArchived>\n")
            sb.append("      <isFavorite>${note.isFavorite}</isFavorite>\n")
            sb.append("      <isLocked>${note.isLocked}</isLocked>\n")
            sb.append("      <lockPin>${escapeXml(note.lockPin)}</lockPin>\n")
            sb.append("      <createdAt>${note.createdAt}</createdAt>\n")
            sb.append("      <modifiedAt>${note.modifiedAt}</modifiedAt>\n")
            sb.append("      <voiceDurationMs>${note.voiceDurationMs}</voiceDurationMs>\n")
            sb.append("      <attachmentName>${escapeXml(note.attachmentName)}</attachmentName>\n")
            sb.append("      <tags>${escapeXml(note.tags)}</tags>\n")

            // Media attachment files processing encoded in Base64
            if (note.voicePath != null) {
                val voiceFile = File(note.voicePath)
                if (voiceFile.exists()) {
                    sb.append("      <voiceFileName>${escapeXml(voiceFile.name)}</voiceFileName>\n")
                    sb.append("      <voiceFileBase64>${fileToBase64(voiceFile)}</voiceFileBase64>\n")
                }
            }
            if (note.drawingPath != null) {
                val drawingFile = File(note.drawingPath)
                if (drawingFile.exists()) {
                    sb.append("      <drawingFileName>${escapeXml(drawingFile.name)}</drawingFileName>\n")
                    sb.append("      <drawingFileBase64>${fileToBase64(drawingFile)}</drawingFileBase64>\n")
                }
            }
            if (note.attachmentPath != null) {
                val attachFile = File(note.attachmentPath)
                if (attachFile.exists()) {
                    sb.append("      <attachmentFileName>${escapeXml(attachFile.name)}</attachmentFileName>\n")
                    sb.append("      <attachmentFileBase64>${fileToBase64(attachFile)}</attachmentFileBase64>\n")
                }
            }

            sb.append("    </note>\n")
        }
        sb.append("  </notes>\n")
        sb.append("</aurapad_backup>")
        sb.toString()
    }

    // 3. Process XML backup parse and load entities & decoded media attachments safely
    suspend fun restoreBackupXml(context: Context, xmlContent: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val database = AppDatabase.getDatabase(context)
            
            val dbFactory = DocumentBuilderFactory.newInstance()
            val dBuilder = dbFactory.newDocumentBuilder()
            val doc = dBuilder.parse(ByteArrayInputStream(xmlContent.toByteArray(Charsets.UTF_8)))
            doc.documentElement.normalize()

            // 1. Parsing and storing Categories
            val categoryNodes = doc.getElementsByTagName("category")
            for (i in 0 until categoryNodes.length) {
                val node = categoryNodes.item(i)
                if (node.nodeType == Node.ELEMENT_NODE) {
                    val element = node as Element
                    val id = getElementValueOrNull(element, "id")?.toIntOrNull() ?: 0
                    val name = getElementValue(element, "name")
                    val colorHex = getElementValue(element, "colorHex")
                    if (id != 0 && name.isNotEmpty()) {
                        database.categoryDao.insertCategory(Category(id, name, colorHex))
                    }
                }
            }

            // 2. Parsing and storing Notes
            val noteNodes = doc.getElementsByTagName("note")
            for (i in 0 until noteNodes.length) {
                val node = noteNodes.item(i)
                if (node.nodeType == Node.ELEMENT_NODE) {
                    val element = node as Element
                    val id = getElementValue(element, "id").toIntOrNull() ?: 0
                    val title = getElementValue(element, "title")
                    val content = getElementValue(element, "content")
                    val checklistJsonVal = getElementValueOrNull(element, "checklistJson")
                    val categoryIdVal = getElementValueOrNull(element, "categoryId")?.toIntOrNull()
                    val colorInt = getElementValue(element, "colorInt").toIntOrNull() ?: 0xFFF8F9FA.toInt()
                    val isPinned = getElementValue(element, "isPinned").toBoolean()
                    val isArchived = getElementValue(element, "isArchived").toBoolean()
                    val isFavorite = getElementValue(element, "isFavorite").toBoolean()
                    val isLocked = getElementValue(element, "isLocked").toBoolean()
                    val lockPinVal = getElementValueOrNull(element, "lockPin")
                    val createdAt = getElementValue(element, "createdAt").toLongOrNull() ?: System.currentTimeMillis()
                    val modifiedAt = getElementValue(element, "modifiedAt").toLongOrNull() ?: System.currentTimeMillis()
                    val voiceDurationMs = getElementValue(element, "voiceDurationMs").toLongOrNull() ?: 0L
                    val attachmentNameVal = getElementValueOrNull(element, "attachmentName")
                    val tags = getElementValue(element, "tags")

                    // Decode files
                    var restoredVoicePath: String? = null
                    val voiceFileName = getElementValueOrNull(element, "voiceFileName")
                    val voiceFileBase64 = getElementValueOrNull(element, "voiceFileBase64")
                    if (!voiceFileName.isNullOrEmpty() && !voiceFileBase64.isNullOrEmpty()) {
                        val audioDir = File(context.filesDir, "audio").apply { mkdirs() }
                        val file = File(audioDir, voiceFileName)
                        base64ToFile(voiceFileBase64, file)
                        restoredVoicePath = file.absolutePath
                    }

                    var restoredDrawingPath: String? = null
                    val drawingFileName = getElementValueOrNull(element, "drawingFileName")
                    val drawingFileBase64 = getElementValueOrNull(element, "drawingFileBase64")
                    if (!drawingFileName.isNullOrEmpty() && !drawingFileBase64.isNullOrEmpty()) {
                        val drawingDir = File(context.filesDir, "drawings").apply { mkdirs() }
                        val file = File(drawingDir, drawingFileName)
                        base64ToFile(drawingFileBase64, file)
                        restoredDrawingPath = file.absolutePath
                    }

                    var restoredAttachmentPath: String? = null
                    val attachmentFileName = getElementValueOrNull(element, "attachmentFileName")
                    val attachmentFileBase64 = getElementValueOrNull(element, "attachmentFileBase64")
                    if (!attachmentFileName.isNullOrEmpty() && !attachmentFileBase64.isNullOrEmpty()) {
                        val file = File(context.filesDir, attachmentFileName)
                        base64ToFile(attachmentFileBase64, file)
                        restoredAttachmentPath = file.absolutePath
                    }

                    val note = Note(
                        id = id,
                        title = title,
                        content = content,
                        checklistJson = checklistJsonVal.takeIf { !it.isNullOrEmpty() },
                        categoryId = categoryIdVal,
                        colorInt = colorInt,
                        isPinned = isPinned,
                        isArchived = isArchived,
                        isFavorite = isFavorite,
                        isLocked = isLocked,
                        lockPin = lockPinVal.takeIf { !it.isNullOrEmpty() },
                        createdAt = createdAt,
                        modifiedAt = modifiedAt,
                        voicePath = restoredVoicePath,
                        voiceDurationMs = voiceDurationMs,
                        drawingPath = restoredDrawingPath,
                        attachmentPath = restoredAttachmentPath,
                        attachmentName = attachmentNameVal.takeIf { !it.isNullOrEmpty() },
                        tags = tags
                    )
                    database.noteDao.insertNote(note)
                }
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    private fun getElementValue(parent: Element, tagName: String): String {
        val nodeList = parent.getElementsByTagName(tagName)
        if (nodeList.length > 0) {
            return nodeList.item(0).textContent ?: ""
        }
        return ""
    }

    private fun getElementValueOrNull(parent: Element, tagName: String): String? {
        val nodeList = parent.getElementsByTagName(tagName)
        if (nodeList.length > 0) {
            val content = nodeList.item(0).textContent
            if (content != null && content.isNotEmpty()) return content
        }
        return null
    }

    // 4. Export complete backup as XML to local directory folder
    suspend fun exportLocalXmlBackup(context: Context): File? = withContext(Dispatchers.IO) {
        try {
            val backupDirectory = getLocalBackupDirectory(context)
            val xmlFile = File(backupDirectory, "aurapad_backup_${System.currentTimeMillis()}.xml")
            val xmlContent = createBackupXml(context)
            FileOutputStream(xmlFile).use { out ->
                out.write(xmlContent.toByteArray())
            }
            xmlFile
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun restoreLocalXmlBackup(context: Context, xmlFile: File): Boolean = withContext(Dispatchers.IO) {
        try {
            val xmlContent = xmlFile.readText(Charsets.UTF_8)
            restoreBackupXml(context, xmlContent)
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
