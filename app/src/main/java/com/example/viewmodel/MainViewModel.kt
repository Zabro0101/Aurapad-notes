package com.example.viewmodel

import android.app.Application
import android.content.Context
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.os.Build
import android.widget.Toast
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asAndroidPath
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.backup.BackupManager
import com.example.database.AppDatabase
import com.example.models.Category
import com.example.models.Note
import com.example.repository.NoteRepository
import com.example.repository.SettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream

enum class SortOption {
    MODIFIED_DESC,
    MODIFIED_ASC,
    CREATED_DESC,
    TITLE_ASC,
    TITLE_DESC
}

data class StrokePath(
    val path: Path,
    val color: Color,
    val width: Float,
    val isEraser: Boolean = false
)

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val context = application.applicationContext
    
    // Repositories
    val noteRepository: NoteRepository
    val settingsRepository: SettingsRepository

    init {
        val db = AppDatabase.getDatabase(context)
        noteRepository = NoteRepository(db.noteDao, db.categoryDao)
        settingsRepository = SettingsRepository(context)

        // Seed default categories if database is empty
        viewModelScope.launch {
            noteRepository.allCategories.first().let { currentCats ->
                if (currentCats.isEmpty()) {
                    noteRepository.insertCategory(Category(name = "Personal", colorHex = "#FF4CAF50"))
                    noteRepository.insertCategory(Category(name = "Work", colorHex = "#FF1A73E8"))
                    noteRepository.insertCategory(Category(name = "Ideas", colorHex = "#FFFF9800"))
                    noteRepository.insertCategory(Category(name = "Finance", colorHex = "#FFE91E63"))
                }
            }
        }
    }

    // Settings States (read/write repository)
    private val _themeState = MutableStateFlow(settingsRepository.themeSetting)
    val themeState: StateFlow<String> = _themeState.asStateFlow()

    private val _onboardingCompleted = MutableStateFlow(settingsRepository.onboardingCompleted)
    val onboardingCompleted: StateFlow<Boolean> = _onboardingCompleted.asStateFlow()

    fun updateTheme(theme: String) {
        settingsRepository.themeSetting = theme
        _themeState.value = theme
    }

    fun completeOnboarding() {
        settingsRepository.onboardingCompleted = true
        _onboardingCompleted.value = true
    }

    // App Pin / Secure locks
    private val _masterPin = MutableStateFlow(settingsRepository.appLockPin)
    val masterPin: StateFlow<String?> = _masterPin.asStateFlow()
    
    fun setMasterPin(pin: String?) {
        settingsRepository.appLockPin = pin
        _masterPin.value = pin
    }

    // Active screen filtering states
    val searchQuery = MutableStateFlow("")
    val selectedCategoryFilter = MutableStateFlow<Int?>(null)
    val selectedTagFilter = MutableStateFlow<String?>(null)
    val sortOption = MutableStateFlow(SortOption.MODIFIED_DESC)

    // Notes Data Flow
    val notesFlow: StateFlow<List<Note>> = noteRepository.unarchivedNotes
        .combine(searchQuery) { list, query ->
            if (query.isBlank()) list else {
                list.filter { 
                    it.title.contains(query, ignoreCase = true) || 
                    it.content.contains(query, ignoreCase = true) ||
                    it.tags.contains(query, ignoreCase = true)
                }
            }
        }
        .combine(selectedCategoryFilter) { list, catId ->
            if (catId == null) list else list.filter { it.categoryId == catId }
        }
        .combine(selectedTagFilter) { list, tag ->
            if (tag.isNullOrBlank()) list else list.filter { it.tags.split(",").contains(tag) }
        }
        .combine(sortOption) { list, sort ->
            when (sort) {
                SortOption.MODIFIED_DESC -> list.sortedByDescending { it.modifiedAt }
                SortOption.MODIFIED_ASC -> list.sortedBy { it.modifiedAt }
                SortOption.CREATED_DESC -> list.sortedByDescending { it.createdAt }
                SortOption.TITLE_ASC -> list.sortedBy { it.title.lowercase() }
                SortOption.TITLE_DESC -> list.sortedByDescending { it.title.lowercase() }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Archive Flow
    val archivedNotesFlow: StateFlow<List<Note>> = noteRepository.archivedNotes
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Favorites Flow
    val favoriteNotesFlow: StateFlow<List<Note>> = noteRepository.favoriteNotes
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Categories Flow
    val categoriesFlow: StateFlow<List<Category>> = noteRepository.allCategories
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Extracted Unique Tags list
    val allTagsFlow: StateFlow<List<String>> = noteRepository.allNotes
        .map { notes ->
            notes.flatMap { note ->
                note.tags.split(",")
                    .map { it.trim() }
                    .filter { it.isNotEmpty() }
            }.distinct().sorted()
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Active editing note state
    private val _currentEditingNote = MutableStateFlow<Note?>(null)
    val currentEditingNote: StateFlow<Note?> = _currentEditingNote.asStateFlow()

    fun startEditingOrCreateNote(noteId: Int?, initialCategoryId: Int? = null) {
        viewModelScope.launch {
            if (noteId == null || noteId == 0) {
                _currentEditingNote.value = Note(
                    title = "",
                    content = "",
                    categoryId = initialCategoryId,
                    createdAt = System.currentTimeMillis(),
                    modifiedAt = System.currentTimeMillis()
                )
            } else {
                val dbNote = noteRepository.getNoteById(noteId)
                _currentEditingNote.value = dbNote
            }
        }
    }

    fun updateCurrentNoteState(updater: (Note) -> Note) {
        _currentEditingNote.value?.let { current ->
            _currentEditingNote.value = updater(current)
        }
    }

    fun saveCurrentNote(onDone: () -> Unit = {}) {
        viewModelScope.launch {
            _currentEditingNote.value?.let { current ->
                if (current.title.isNotBlank() || current.content.isNotBlank() || !current.checklistJson.isNullOrEmpty() || current.drawingPath != null || current.voicePath != null) {
                    val noteWithTime = current.copy(modifiedAt = System.currentTimeMillis())
                    val noteId = noteRepository.insertOrUpdateNote(noteWithTime)
                    // Update loaded state so we get newly generated ID if creating note
                    _currentEditingNote.value = noteWithTime.copy(id = noteId.toInt())
                }
            }
            onDone()
        }
    }

    fun deleteCurrentNote(onDone: () -> Unit) {
        viewModelScope.launch {
            _currentEditingNote.value?.let { current ->
                if (current.id != 0) {
                    noteRepository.deleteNote(current)
                }
            }
            _currentEditingNote.value = null
            onDone()
        }
    }

    fun duplicateCurrentNote(onDone: () -> Unit) {
        viewModelScope.launch {
            _currentEditingNote.value?.let { current ->
                if (current.id != 0) {
                    noteRepository.duplicateNote(current)
                }
            }
            onDone()
        }
    }

    // -------------------------------------------------------------
    // VOICE RECORDING & PLAYBACK SUPPORT
    // -------------------------------------------------------------
    private var mediaRecorder: MediaRecorder? = null
    private var mediaPlayer: MediaPlayer? = null
    
    val isRecording = mutableStateOf(false)
    val currentRecordPath = mutableStateOf<String?>(null)
    val isPlayingAudio = mutableStateOf(false)
    val activePlayingPath = mutableStateOf<String?>(null)

    fun startRecordingVoice() {
        try {
            val audioDir = File(context.filesDir, "audio").apply { mkdirs() }
            val audioFile = File(audioDir, "voice_note_${System.currentTimeMillis()}.3gp")
            val path = audioFile.absolutePath
            currentRecordPath.value = path

            mediaRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP)
                setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)
                setOutputFile(path)
                prepare()
                start()
            }
            isRecording.value = true
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Microphone recording initialization failed", Toast.LENGTH_SHORT).show()
        }
    }

    fun stopRecordingVoice(): Pair<String, Long>? {
        if (!isRecording.value) return null
        return try {
            mediaRecorder?.apply {
                stop()
                release()
            }
            mediaRecorder = null
            isRecording.value = false
            
            val path = currentRecordPath.value
            if (path != null) {
                val file = File(path)
                if (file.exists()) {
                    // Try to extract duration
                    val mp = MediaPlayer().apply {
                        setDataSource(path)
                        prepare()
                    }
                    val duration = mp.duration.toLong()
                    mp.release()

                    // Associate recording to current active note
                    updateCurrentNoteState { it.copy(voicePath = path, voiceDurationMs = duration) }
                    return Pair(path, duration)
                }
            }
            null
        } catch (e: Exception) {
            e.printStackTrace()
            mediaRecorder = null
            isRecording.value = false
            null
        }
    }

    fun playVoiceNote(path: String) {
        if (isPlayingAudio.value) {
            stopVoiceNote()
        }
        try {
            mediaPlayer = MediaPlayer().apply {
                setDataSource(path)
                prepare()
                start()
                setOnCompletionListener {
                    stopVoiceNote()
                }
            }
            isPlayingAudio.value = true
            activePlayingPath.value = path
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Audio playback configuration failed", Toast.LENGTH_SHORT).show()
        }
    }

    fun stopVoiceNote() {
        try {
            mediaPlayer?.apply {
                if (isPlaying) {
                    stop()
                }
                release()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            mediaPlayer = null
            isPlayingAudio.value = false
            activePlayingPath.value = null
        }
    }

    fun deleteVoiceNoteFromActiveNote() {
        _currentEditingNote.value?.voicePath?.let { path ->
            val file = File(path)
            if (file.exists()) {
                file.delete()
            }
        }
        updateCurrentNoteState { it.copy(voicePath = null, voiceDurationMs = 0) }
    }

    // -------------------------------------------------------------
    // DRAWING MODE PATH CANVAS STATE
    // -------------------------------------------------------------
    val drawingPaths = mutableStateListOf<StrokePath>()
    val drawingRedoStack = mutableStateListOf<StrokePath>()

    fun clearDrawingCanvas() {
        drawingPaths.clear()
        drawingRedoStack.clear()
    }

    fun undoDrawing() {
        if (drawingPaths.isNotEmpty()) {
            val last = drawingPaths.removeAt(drawingPaths.size - 1)
            drawingRedoStack.add(last)
        }
    }

    fun redoDrawing() {
        if (drawingRedoStack.isNotEmpty()) {
            val last = drawingRedoStack.removeAt(drawingRedoStack.size - 1)
            drawingPaths.add(last)
        }
    }

    fun saveCanvasDrawing(width: Int, height: Int): String? {
        if (drawingPaths.isEmpty()) return null
        return try {
            val drawingDir = File(context.filesDir, "drawings").apply { mkdirs() }
            val dFile = File(drawingDir, "drawing_${System.currentTimeMillis()}.png")
            
            val bitmap = android.graphics.Bitmap.createBitmap(
                if (width <= 0) 800 else width,
                if (height <= 0) 1000 else height,
                android.graphics.Bitmap.Config.ARGB_8888
            )
            val nativeCanvas = android.graphics.Canvas(bitmap)
            // Background White
            nativeCanvas.drawColor(android.graphics.Color.WHITE)

            val nativePaint = android.graphics.Paint().apply {
                isAntiAlias = true
                style = android.graphics.Paint.Style.STROKE
                strokeJoin = android.graphics.Paint.Join.ROUND
                strokeCap = android.graphics.Paint.Cap.ROUND
            }

            for (p in drawingPaths) {
                nativePaint.color = p.color.value.toInt()
                nativePaint.strokeWidth = p.width
                
                // Convert Jetpack Compose Path to Native Android Path
                val androidPath = p.path.asAndroidPath()
                nativeCanvas.drawPath(androidPath, nativePaint)
            }

            FileOutputStream(dFile).use { out ->
                bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, out)
            }
            bitmap.recycle()
            
            val absolutePath = dFile.absolutePath
            updateCurrentNoteState { it.copy(drawingPath = absolutePath) }
            absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun deleteDrawingFromActiveNote() {
        _currentEditingNote.value?.drawingPath?.let { path ->
            val file = File(path)
            if (file.exists()) {
                file.delete()
            }
        }
        updateCurrentNoteState { it.copy(drawingPath = null) }
    }

    // -------------------------------------------------------------
    // BACKUP & RESTORE WRAPPERS
    // -------------------------------------------------------------
    fun triggerLocalBackup(onDone: (File?) -> Unit) {
        viewModelScope.launch {
            try {
                val backupFile = BackupManager.exportZipBackup(context)
                onDone(backupFile)
            } catch (e: Exception) {
                e.printStackTrace()
                onDone(null)
            }
        }
    }

    fun triggerLocalRestore(zipFile: File, onDone: (Boolean) -> Unit) {
        viewModelScope.launch {
            try {
                val result = BackupManager.restoreZipBackup(context, zipFile)
                onDone(result)
            } catch (e: Exception) {
                e.printStackTrace()
                onDone(false)
            }
        }
    }
}
