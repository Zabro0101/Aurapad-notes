package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Unarchive
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.models.Note
import com.example.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArchiveScreen(
    viewModel: MainViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val archivedNotes by viewModel.archivedNotesFlow.collectAsState()
    val categories by viewModel.categoriesFlow.collectAsState()

    var activeNoteToHandle by remember { mutableStateOf<Note?>(null) }

    Scaffold(
        topBar = {
            MediumTopAppBar(
                title = { Text("Archive Vault") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(innerPadding)
        ) {
            if (archivedNotes.isEmpty()) {
                Column(
                    modifier = Modifier.fillMaxSize().padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Archive,
                        contentDescription = null,
                        modifier = Modifier.size(72.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Archive empty", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Notes you archive will appear here", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f), textAlign = TextAlign.Center)
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    contentPadding = PaddingValues(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(archivedNotes) { note ->
                        NoteCard(
                            note = note,
                            categories = categories,
                            onClick = { activeNoteToHandle = note },
                            onLongClick = { activeNoteToHandle = note }
                        )
                    }
                }
            }
        }
    }

    activeNoteToHandle?.let { note ->
        AlertDialog(
            onDismissRequest = { activeNoteToHandle = null },
            title = { Text("Archived Note Options") },
            text = { Text("Restore this note to active workspace, or delete permanently?") },
            confirmButton = {
                Button(onClick = {
                    viewModel.updateCurrentNoteState { note.copy(isArchived = false) }
                    viewModel.saveCurrentNote()
                    activeNoteToHandle = null
                    Toast.makeText(context, "Note restored successfully", Toast.LENGTH_SHORT).show()
                }) {
                    Icon(Icons.Default.Unarchive, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Restore")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    viewModel.updateCurrentNoteState { note }
                    viewModel.deleteCurrentNote {
                        activeNoteToHandle = null
                        Toast.makeText(context, "Note permanently deleted", Toast.LENGTH_SHORT).show()
                    }
                }) {
                    Icon(Icons.Default.DeleteForever, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.Red)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Delete Permanently", color = Color.Red)
                }
            }
        )
    }
}
