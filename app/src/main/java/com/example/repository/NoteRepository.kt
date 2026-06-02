package com.example.repository

import com.example.database.CategoryDao
import com.example.database.NoteDao
import com.example.models.Category
import com.example.models.Note
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class NoteRepository(
    private val noteDao: NoteDao,
    private val categoryDao: CategoryDao
) {
    // Notes streams
    val allNotes: Flow<List<Note>> = noteDao.getAllNotesFlow()
    val unarchivedNotes: Flow<List<Note>> = noteDao.getUnarchivedNotesFlow()
    val archivedNotes: Flow<List<Note>> = noteDao.getArchivedNotesFlow()
    val favoriteNotes: Flow<List<Note>> = noteDao.getFavoriteNotesFlow()

    // Categories stream
    val allCategories: Flow<List<Category>> = categoryDao.getAllCategories()

    // Notes Crud
    suspend fun getNoteById(id: Int): Note? = withContext(Dispatchers.IO) {
        noteDao.getNoteById(id)
    }

    suspend fun insertOrUpdateNote(note: Note): Long = withContext(Dispatchers.IO) {
        val modifiedNote = note.copy(modifiedAt = System.currentTimeMillis())
        noteDao.insertNote(modifiedNote)
    }

    suspend fun updateNote(note: Note) = withContext(Dispatchers.IO) {
        noteDao.updateNote(note.copy(modifiedAt = System.currentTimeMillis()))
    }

    suspend fun deleteNote(note: Note) = withContext(Dispatchers.IO) {
        noteDao.deleteNote(note)
    }

    suspend fun deleteNoteById(id: Int) = withContext(Dispatchers.IO) {
        noteDao.deleteNoteById(id)
    }

    suspend fun duplicateNote(note: Note): Long = withContext(Dispatchers.IO) {
        val dup = note.copy(
            id = 0,
            title = if (note.title.isNotEmpty()) "${note.title} (Copy)" else "Copy",
            createdAt = System.currentTimeMillis(),
            modifiedAt = System.currentTimeMillis(),
            isPinned = false,
            isLocked = false,
            lockPin = null
        )
        noteDao.insertNote(dup)
    }

    // Categories Crud
    suspend fun getCategoryById(id: Int): Category? = withContext(Dispatchers.IO) {
        categoryDao.getCategoryById(id)
    }

    suspend fun insertCategory(category: Category): Long = withContext(Dispatchers.IO) {
        categoryDao.insertCategory(category)
    }

    suspend fun updateCategory(category: Category) = withContext(Dispatchers.IO) {
        categoryDao.updateCategory(category)
    }

    suspend fun deleteCategory(category: Category) = withContext(Dispatchers.IO) {
        categoryDao.deleteCategory(category)
    }
}
