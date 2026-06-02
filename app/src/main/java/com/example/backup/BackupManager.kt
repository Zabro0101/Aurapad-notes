package com.example.backup

import android.content.Context
import com.example.database.AppDatabase
import com.example.models.Category
import com.example.models.Note
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

object BackupManager {

    // Serialize database data to a backup JSON structure
    suspend fun createBackupJson(context: Context): String = withContext(Dispatchers.IO) {
        val database = AppDatabase.getDatabase(context)
        val notes = database.noteDao.getUnarchivedNotesFlow().first() + database.noteDao.getArchivedNotesFlow().first()
        val categories = database.categoryDao.getAllCategories().first()

        val backupObj = JSONObject()

        // Serialize Categories
        val categoriesArray = JSONArray()
        for (cat in categories) {
            val catObj = JSONObject().apply {
                put("id", cat.id)
                put("name", cat.name)
                put("colorHex", cat.colorHex)
            }
            categoriesArray.put(catObj)
        }
        backupObj.put("categories", categoriesArray)

        // Serialize Notes
        val notesArray = JSONArray()
        for (note in notes) {
            val noteObj = JSONObject().apply {
                put("id", note.id)
                put("title", note.title)
                put("content", note.content)
                put("checklistJson", note.checklistJson)
                put("categoryId", note.categoryId)
                put("colorInt", note.colorInt)
                put("isPinned", note.isPinned)
                put("isArchived", note.isArchived)
                put("isFavorite", note.isFavorite)
                put("isLocked", note.isLocked)
                put("lockPin", note.lockPin)
                put("createdAt", note.createdAt)
                put("modifiedAt", note.modifiedAt)
                put("voicePath", note.voicePath)
                put("voiceDurationMs", note.voiceDurationMs)
                put("drawingPath", note.drawingPath)
                put("attachmentPath", note.attachmentPath)
                put("attachmentName", note.attachmentName)
                put("tags", note.tags)
            }
            notesArray.put(noteObj)
        }
        backupObj.put("notes", notesArray)

        backupObj.toString(4)
    }

    // Restore database tables from parsed JSON backup String
    suspend fun restoreBackupJson(context: Context, backupJson: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val database = AppDatabase.getDatabase(context)
            val rootObj = JSONObject(backupJson)

            // Parse Categories
            val categoriesArray = rootObj.optJSONArray("categories")
            if (categoriesArray != null) {
                for (i in 0 until categoriesArray.length()) {
                    val catObj = categoriesArray.getJSONObject(i)
                    val cat = Category(
                        id = catObj.optInt("id", 0),
                        name = catObj.optString("name", "Category"),
                        colorHex = catObj.optString("colorHex", "#FF1A73E8")
                    )
                    database.categoryDao.insertCategory(cat)
                }
            }

            // Parse Notes
            val notesArray = rootObj.optJSONArray("notes")
            if (notesArray != null) {
                for (i in 0 until notesArray.length()) {
                    val noteObj = notesArray.getJSONObject(i)
                    val note = Note(
                        id = noteObj.optInt("id", 0),
                        title = noteObj.optString("title"),
                        content = noteObj.optString("content"),
                        checklistJson = if (noteObj.isNull("checklistJson")) null else noteObj.optString("checklistJson"),
                        categoryId = if (noteObj.isNull("categoryId")) null else noteObj.optInt("categoryId"),
                        colorInt = noteObj.optInt("colorInt", 0xFFF8F9FA.toInt()),
                        isPinned = noteObj.optBoolean("isPinned", false),
                        isArchived = noteObj.optBoolean("isArchived", false),
                        isFavorite = noteObj.optBoolean("isFavorite", false),
                        isLocked = noteObj.optBoolean("isLocked", false),
                        lockPin = if (noteObj.isNull("lockPin")) null else noteObj.optString("lockPin"),
                        createdAt = noteObj.optLong("createdAt", System.currentTimeMillis()),
                        modifiedAt = noteObj.optLong("modifiedAt", System.currentTimeMillis()),
                        voicePath = if (noteObj.isNull("voicePath")) null else noteObj.optString("voicePath"),
                        voiceDurationMs = noteObj.optLong("voiceDurationMs", 0),
                        drawingPath = if (noteObj.isNull("drawingPath")) null else noteObj.optString("drawingPath"),
                        attachmentPath = if (noteObj.isNull("attachmentPath")) null else noteObj.optString("attachmentPath"),
                        attachmentName = if (noteObj.isNull("attachmentName")) null else noteObj.optString("attachmentName"),
                        tags = noteObj.optString("tags", "")
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

    // 2. Export full backup package to ZIP including notes metadata and attachments files
    suspend fun exportZipBackup(context: Context): File = withContext(Dispatchers.IO) {
        val backupDir = File(context.cacheDir, "backups").apply { mkdirs() }
        val zipFile = File(backupDir, "aurapad_backup_${System.currentTimeMillis()}.zip")

        val jsonString = createBackupJson(context)
        
        ZipOutputStream(BufferedOutputStream(FileOutputStream(zipFile))).use { zos ->
            // Add metadata JSON
            val dataEntry = ZipEntry("backup_data.json")
            zos.putNextEntry(dataEntry)
            zos.write(jsonString.toByteArray())
            zos.closeEntry()

            // Compress recorded voice attachments and visual drawing files
            val storageDir = context.filesDir
            val filesList = storageDir.listFiles()
            if (filesList != null) {
                for (file in filesList) {
                    if (file.isFile && (file.name.endsWith(".3gp") || file.name.endsWith(".png") || file.name.endsWith(".pdf"))) {
                        val fileEntry = ZipEntry("files/${file.name}")
                        zos.putNextEntry(fileEntry)
                        FileInputStream(file).use { input ->
                            input.copyTo(zos)
                        }
                        zos.closeEntry()
                    }
                }
            }
        }
        zipFile
    }

    // 3. Restore complete ZIP backup and unpack associated files
    suspend fun restoreZipBackup(context: Context, zipFile: File): Boolean = withContext(Dispatchers.IO) {
        try {
            var isDbRestored = false
            ZipInputStream(FileInputStream(zipFile)).use { zis ->
                var entry = zis.nextEntry
                while (entry != null) {
                    if (entry.name == "backup_data.json") {
                        val jsonContent = zis.readBytes().toString(Charsets.UTF_8)
                        isDbRestored = restoreBackupJson(context, jsonContent)
                    } else if (entry.name.startsWith("files/")) {
                        val targetFileName = entry.name.substringAfter("files/")
                        val targetFile = File(context.filesDir, targetFileName)
                        FileOutputStream(targetFile).use { out ->
                            zis.copyTo(out)
                        }
                    }
                    zis.closeEntry()
                    entry = zis.nextEntry
                }
            }
            isDbRestored
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
