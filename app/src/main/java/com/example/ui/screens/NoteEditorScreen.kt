package com.example.ui.screens

import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.export.NoteExporter
import com.example.models.Category
import com.example.models.Note
import com.example.ui.theme.NoteColors
import com.example.viewmodel.MainViewModel
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteEditorScreen(
    viewModel: MainViewModel,
    noteId: Int?,
    initialCategoryId: Int?,
    onNavigateBack: () -> Unit,
    onNavigateToCanvasPaint: () -> Unit
) {
    val context = LocalContext.current
    val categories by viewModel.categoriesFlow.collectAsState()
    val rawNote by viewModel.currentEditingNote.collectAsState()

    // Initialize editing target
    LaunchedEffect(key1 = noteId) {
        viewModel.startEditingOrCreateNote(noteId, initialCategoryId)
    }

    if (rawNote == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    val note = rawNote!!

    // Local input caches for fast fluid editing
    var titleVal by remember(note.id) { mutableStateOf(note.title) }
    var contentVal by remember(note.id) { mutableStateOf(note.content) }
    var tagVal by remember(note.id) { mutableStateOf(note.tags) }
    
    // Checklist State
    val checklistItems = remember(note.id, note.checklistJson) {
        mutableStateListOf<ChecklistItem>().apply {
            if (!note.checklistJson.isNullOrEmpty()) {
                try {
                    val array = JSONArray(note.checklistJson)
                    for (i in 0 until array.length()) {
                        val obj = array.getJSONObject(i)
                        add(ChecklistItem(obj.optString("text"), obj.optBoolean("checked")))
                    }
                } catch (e: Exception) {
                    // Fail gracefully
                }
            }
        }
    }

    // Helper to serialize checklist back to JSON string
    fun saveChecklist() {
        if (checklistItems.isEmpty()) {
            viewModel.updateCurrentNoteState { it.copy(checklistJson = null) }
        } else {
            val array = JSONArray()
            for (item in checklistItems) {
                array.put(JSONObject().apply {
                    put("text", item.text)
                    put("checked", item.checked)
                })
            }
            viewModel.updateCurrentNoteState { it.copy(checklistJson = array.toString()) }
        }
    }

    // Attachment result receiver
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            try {
                val input = context.contentResolver.openInputStream(uri)
                if (input != null) {
                    val targetFile = File(context.filesDir, "attach_${System.currentTimeMillis()}_file.pdf")
                    FileOutputStream(targetFile).use { out ->
                        input.copyTo(out)
                    }
                    val name = uri.lastPathSegment ?: "Document.pdf"
                    viewModel.updateCurrentNoteState { 
                        it.copy(attachmentPath = targetFile.absolutePath, attachmentName = name)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(context, "Failed to load attachment file", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Synchronize local caches with parent state transitions (auto-save helper)
    val syncCachesAndSave = {
        viewModel.updateCurrentNoteState { 
            it.copy(
                title = titleVal,
                content = contentVal,
                tags = tagVal
            )
        }
        saveChecklist()
        viewModel.saveCurrentNote()
    }

    // Trigger auto-save whenever back or navigation occurs
    DisposableEffect(key1 = note.id) {
        onDispose {
            // Un-sync saves on dispose
        }
    }

    // Security Unlock dialog state if note is locked (and we are on locked view)
    var isPinLockedScreen by remember(note.id) { mutableStateOf(note.isLocked && note.lockPin != null) }
    var pinEntryText by remember { mutableStateOf("") }

    if (isPinLockedScreen) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(Icons.Default.Lock, contentDescription = "Locked Note", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(72.dp))
                Spacer(modifier = Modifier.height(24.dp))
                Text("Passcode Protected Note", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
                Spacer(modifier = Modifier.height(8.dp))
                Text("Enter the 4-digit security PIN to view this locked note.", fontSize = 14.sp, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f))
                Spacer(modifier = Modifier.height(24.dp))
                
                OutlinedTextField(
                    value = pinEntryText,
                    onValueChange = { if (it.length <= 4) pinEntryText = it },
                    label = { Text("4-digit PIN") },
                    singleLine = true,
                    modifier = Modifier.width(180.dp),
                    textStyle = TextStyle(fontSize = 18.sp, fontWeight = FontWeight.Bold)
                )

                Spacer(modifier = Modifier.height(24.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    TextButton(onClick = onNavigateBack) {
                        Text("Cancel")
                    }
                    Button(onClick = {
                        if (pinEntryText == note.lockPin) {
                            isPinLockedScreen = false
                        } else {
                            Toast.makeText(context, "Incorrect security passcode", Toast.LENGTH_SHORT).show()
                        }
                    }) {
                        Text("Unlock Note")
                    }
                }
            }
        }
        return
    }

    Scaffold(
        topBar = {
            Surface(
                color = Color(note.colorInt),
                tonalElevation = 1.dp
            ) {
                Row(
                    modifier = Modifier
                        .statusBarsPadding()
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    IconButton(onClick = {
                        syncCachesAndSave()
                        onNavigateBack()
                    }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        // Favorite Toggle Button
                        IconButton(onClick = {
                            viewModel.updateCurrentNoteState { it.copy(isFavorite = !it.isFavorite) }
                            syncCachesAndSave()
                        }) {
                            Icon(
                                imageVector = if (note.isFavorite) Icons.Default.Star else Icons.Outlined.StarBorder,
                                contentDescription = "Favorite",
                                tint = if (note.isFavorite) Color(0xFFFFB300) else MaterialTheme.colorScheme.onSurface
                            )
                        }

                        // Pin Toggle Button
                        IconButton(onClick = {
                            viewModel.updateCurrentNoteState { it.copy(isPinned = !it.isPinned) }
                            syncCachesAndSave()
                        }) {
                            Icon(
                                imageVector = if (note.isPinned) Icons.Default.PushPin else Icons.Outlined.PushPin,
                                contentDescription = "Pin Note",
                                tint = if (note.isPinned) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                            )
                        }

                        // Lock / Password dialog toggle
                        IconButton(onClick = {
                            if (note.isLocked) {
                                // Unlock completely
                                viewModel.updateCurrentNoteState { it.copy(isLocked = false, lockPin = null) }
                                Toast.makeText(context, "Password protection removed", Toast.LENGTH_SHORT).show()
                            } else {
                                // Launch standard modal dialog config for locking note
                                viewModel.updateCurrentNoteState { it.copy(isLocked = true, lockPin = "1234") }
                                Toast.makeText(context, "Note secured with default passcode (1234)", Toast.LENGTH_LONG).show()
                            }
                            syncCachesAndSave()
                        }) {
                            Icon(
                                imageVector = if (note.isLocked) Icons.Default.Lock else Icons.Outlined.LockOpen,
                                contentDescription = "Toggle Lock",
                                tint = if (note.isLocked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                            )
                        }

                        // Share selected note
                        IconButton(onClick = {
                            syncCachesAndSave()
                            val shareText = "TITLE: ${note.title}\n\n${note.content}"
                            val intent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_SUBJECT, note.title)
                                putExtra(Intent.EXTRA_TEXT, shareText)
                            }
                            context.startActivity(Intent.createChooser(intent, "Share Note Content"))
                        }) {
                            Icon(Icons.Default.Share, contentDescription = "Share")
                        }

                        // Archive Toggle
                        IconButton(onClick = {
                            viewModel.updateCurrentNoteState { it.copy(isArchived = !it.isArchived) }
                            syncCachesAndSave()
                            Toast.makeText(context, if (note.isArchived) "Note archived" else "Note unarchived", Toast.LENGTH_SHORT).show()
                            onNavigateBack()
                        }) {
                            Icon(
                                imageVector = if (note.isArchived) Icons.Default.Unarchive else Icons.Default.Archive,
                                contentDescription = "Archive Note"
                            )
                        }

                        // More / Export Options Button
                        var showExportOptions by remember { mutableStateOf(false) }
                        Box {
                            IconButton(onClick = { showExportOptions = true }) {
                                Icon(Icons.Default.MoreVert, contentDescription = "Export formatting Options")
                            }
                            DropdownMenu(
                                expanded = showExportOptions,
                                onDismissRequest = { showExportOptions = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Export as Plain TXT") },
                                    leadingIcon = { Icon(Icons.Default.Description, contentDescription = null) },
                                    onClick = {
                                        showExportOptions = false
                                        syncCachesAndSave()
                                        val cat = categories.find { it.id == note.categoryId }
                                        val file = NoteExporter.exportToTxt(context, note, cat?.name)
                                        Toast.makeText(context, "Exported: ${file.name}", Toast.LENGTH_LONG).show()
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Export as Beautiful HTML") },
                                    leadingIcon = { Icon(Icons.Default.Html, contentDescription = null) },
                                    onClick = {
                                        showExportOptions = false
                                        syncCachesAndSave()
                                        val cat = categories.find { it.id == note.categoryId }
                                        val file = NoteExporter.exportToHtml(context, note, cat?.name)
                                        Toast.makeText(context, "Exported Web document: ${file.name}", Toast.LENGTH_LONG).show()
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Export as Vector PDF") },
                                    leadingIcon = { Icon(Icons.Default.PictureAsPdf, contentDescription = null) },
                                    onClick = {
                                        showExportOptions = false
                                        syncCachesAndSave()
                                        val cat = categories.find { it.id == note.categoryId }
                                        val file = NoteExporter.exportToPdf(context, note, cat?.name)
                                        Toast.makeText(context, "Generated vector layout PDF: ${file.name}", Toast.LENGTH_LONG).show()
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Duplicate note") },
                                    leadingIcon = { Icon(Icons.Default.FileCopy, contentDescription = null) },
                                    onClick = {
                                        showExportOptions = false
                                        syncCachesAndSave()
                                        viewModel.duplicateCurrentNote {
                                            Toast.makeText(context, "Note Duplicated", Toast.LENGTH_SHORT).show()
                                            onNavigateBack()
                                        }
                                    }
                                )
                                Divider()
                                DropdownMenuItem(
                                    text = { Text("Delete note", color = Color.Red) },
                                    leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = Color.Red) },
                                    onClick = {
                                        showExportOptions = false
                                        viewModel.deleteCurrentNote {
                                            Toast.makeText(context, "Note deleted", Toast.LENGTH_SHORT).show()
                                            onNavigateBack()
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(note.colorInt))
                .padding(innerPadding)
        ) {
            // Main scrolling editor area
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Background & Category selector
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Category pill trigger
                        var showCatsDropdown by remember { mutableStateOf(false) }
                        val activeCategory = categories.find { it.id == note.categoryId }

                        Box {
                            FilterChip(
                                selected = activeCategory != null,
                                onClick = { showCatsDropdown = true },
                                label = { Text(activeCategory?.name ?: "No Category") },
                                trailingIcon = { Icon(Icons.Default.ArrowDropDown, contentDescription = null) },
                                shape = RoundedCornerShape(12.dp)
                            )
                            DropdownMenu(
                                expanded = showCatsDropdown,
                                onDismissRequest = { showCatsDropdown = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("None") },
                                    onClick = {
                                        viewModel.updateCurrentNoteState { it.copy(categoryId = null) }
                                        showCatsDropdown = false
                                    }
                                )
                                for (cat in categories) {
                                    DropdownMenuItem(
                                        text = { Text(cat.name) },
                                        onClick = {
                                            viewModel.updateCurrentNoteState { it.copy(categoryId = cat.id) }
                                            showCatsDropdown = false
                                        }
                                    )
                                }
                            }
                        }

                        // Auto-Save indicator text
                        Text(
                            text = "Auto-saved",
                            fontSize = 12.sp,
                            color = Color.DarkGray.copy(alpha = 0.6f),
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                // Note Background color picker row
                item {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        item {
                            Text("Color: ", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.DarkGray)
                        }
                        items(NoteColors) { col ->
                            val active = note.colorInt == col.value.toInt()
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .background(col)
                                    .border(
                                        width = if (active) 2.dp else 1.dp,
                                        color = if (active) MaterialTheme.colorScheme.primary else Color.Gray.copy(alpha = 0.5f),
                                        shape = CircleShape
                                    )
                                    .clickable {
                                        viewModel.updateCurrentNoteState { it.copy(colorInt = col.value.toInt()) }
                                    }
                            )
                        }
                    }
                }

                // Title Input field
                item {
                    BasicTextField(
                        value = titleVal,
                        onValueChange = { titleVal = it; syncCachesAndSave() },
                        textStyle = TextStyle(
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                            .testTag("note_title_input"),
                        decorationBox = { innerTextField ->
                            if (titleVal.isEmpty()) {
                                Text(
                                    "Title",
                                    style = TextStyle(
                                        fontSize = 24.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.Gray.copy(alpha = 0.6f)
                                    )
                                )
                            }
                            innerTextField()
                        }
                    )
                }

                // Attachment Previews Container (Drawing sketches)
                if (note.drawingPath != null) {
                    item {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color.LightGray.copy(alpha = 0.2f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp)
                        ) {
                            Box(modifier = Modifier.fillMaxSize()) {
                                AsyncImage(
                                    model = File(note.drawingPath),
                                    contentDescription = "Drawing Sketch attachment",
                                    modifier = Modifier.fillMaxSize()
                                )
                                IconButton(
                                    onClick = { viewModel.deleteDrawingFromActiveNote() },
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(8.dp)
                                        .background(Color.White, CircleShape)
                                ) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete Drawing", tint = Color.Red)
                                }
                            }
                        }
                    }
                }

                // File Attachment PDF / Doc container
                if (note.attachmentPath != null) {
                    item {
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Color.White)
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.AttachFile, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(note.attachmentName ?: "Attached File", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                        Text("Local Offline Document", fontSize = 11.sp, color = Color.Gray)
                                    }
                                }
                                IconButton(onClick = {
                                    viewModel.updateCurrentNoteState { it.copy(attachmentPath = null, attachmentName = null) }
                                }) {
                                    Icon(Icons.Default.Close, contentDescription = "Remove File", tint = Color.Red)
                                }
                            }
                        }
                    }
                }

                // Voice Recording Audio Playback panel inline
                if (note.voicePath != null) {
                    item {
                        val isPlaying = viewModel.isPlayingAudio.value && viewModel.activePlayingPath.value == note.voicePath
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Color.White)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    IconButton(
                                        onClick = {
                                            if (isPlaying) {
                                                viewModel.stopVoiceNote()
                                            } else {
                                                viewModel.playVoiceNote(note.voicePath)
                                            }
                                        },
                                        colors = IconButtonDefaults.iconButtonColors(
                                            containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                                        )
                                    ) {
                                        Icon(
                                            imageVector = if (isPlaying) Icons.Default.Stop else Icons.Default.PlayArrow,
                                            contentDescription = "Playback",
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text("Voice Memo", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                        Text("Duration: ${note.voiceDurationMs / 1000}s", fontSize = 12.sp, color = Color.Gray)
                                    }
                                }
                                IconButton(onClick = { viewModel.deleteVoiceNoteFromActiveNote() }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete Recording", tint = Color.Red)
                                }
                            }
                        }
                    }
                }

                // Main body Markdown content
                item {
                    BasicTextField(
                        value = contentVal,
                        onValueChange = { contentVal = it; syncCachesAndSave() },
                        textStyle = TextStyle(
                            fontSize = 16.sp,
                            lineHeight = 24.sp,
                            color = Color.Black
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .defaultMinSize(minHeight = 160.dp)
                            .testTag("note_content_input"),
                        decorationBox = { innerTextField ->
                            if (contentVal.isEmpty()) {
                                Text(
                                    "Note something offline...",
                                    style = TextStyle(
                                        fontSize = 16.sp,
                                        color = Color.Gray.copy(alpha = 0.6f)
                                    )
                                )
                            }
                            innerTextField()
                        }
                    )
                }

                // Checklists items container
                if (checklistItems.isNotEmpty()) {
                    item {
                        Text("Checklist", style = TextStyle(fontWeight = FontWeight.Bold, color = Color.DarkGray, fontSize = 14.sp))
                    }
                    itemsIndexed(checklistItems) { index, item ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Checkbox(
                                    checked = item.checked,
                                    onCheckedChange = { checked ->
                                        checklistItems[index] = item.copy(checked = checked)
                                        saveChecklist()
                                        viewModel.saveCurrentNote()
                                    }
                                )
                                BasicTextField(
                                    value = item.text,
                                    onValueChange = { text ->
                                        checklistItems[index] = item.copy(text = text)
                                        saveChecklist()
                                        viewModel.saveCurrentNote()
                                    },
                                    textStyle = TextStyle(
                                        fontSize = 15.sp,
                                        color = if (item.checked) Color.Gray else Color.Black,
                                        textDecoration = if (item.checked) androidx.compose.ui.text.style.TextDecoration.LineThrough else null
                                    ),
                                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)
                                )
                            }
                            IconButton(onClick = {
                                checklistItems.removeAt(index)
                                saveChecklist()
                                viewModel.saveCurrentNote()
                            }) {
                                Icon(Icons.Default.Close, contentDescription = "Delete item", tint = Color.Gray)
                            }
                        }
                    }
                }

                // Inline tags representation input
                item {
                    OutlinedTextField(
                        value = tagVal,
                        onValueChange = { tagVal = it; syncCachesAndSave() },
                        label = { Text("Tags (comma separated, e.g. work,ideas,todo)") },
                        modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            }

            // BOTTOM EDITING PANEL BAR (Rich editing toolbar for checklists, drawings, microphone voice records, lists)
            Surface(
                tonalElevation = 8.dp,
                color = Color.White,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    // Quick add toolbar
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Checklist additions
                            IconButton(onClick = {
                                checklistItems.add(ChecklistItem("", false))
                                saveChecklist()
                                viewModel.saveCurrentNote()
                            }) {
                                Icon(Icons.Default.LibraryAddCheck, contentDescription = "Add Checklist Item")
                            }

                            // Sketch Canvas launcher
                            IconButton(onClick = {
                                syncCachesAndSave()
                                onNavigateToCanvasPaint()
                            }) {
                                Icon(Icons.Default.Brush, contentDescription = "Draw painting")
                            }

                            // Direct Attachment SAF trigger
                            IconButton(onClick = { filePickerLauncher.launch("application/pdf") }) {
                                Icon(Icons.Default.AttachFile, contentDescription = "Attach PDF Document")
                            }

                            // Microphone recording toggle
                            val recordOn = viewModel.isRecording.value
                            IconButton(
                                onClick = {
                                    if (recordOn) {
                                        viewModel.stopRecordingVoice()
                                        Toast.makeText(context, "Recording completed", Toast.LENGTH_SHORT).show()
                                    } else {
                                        viewModel.startRecordingVoice()
                                        Toast.makeText(context, "Microphone recording started...", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                colors = IconButtonDefaults.iconButtonColors(
                                    containerColor = if (recordOn) Color.Red.copy(alpha = 0.15f) else Color.Transparent
                                )
                            ) {
                                Icon(
                                    imageVector = if (recordOn) Icons.Default.StopCircle else Icons.Default.Mic,
                                    contentDescription = "Record Voice memo",
                                    tint = if (recordOn) Color.Red else Color.Black
                                )
                            }
                        }

                        // Formatting macros (Markdown injections helper)
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            IconButton(onClick = { contentVal += "**" }) {
                                Icon(Icons.Default.FormatBold, contentDescription = "Bold notation")
                            }
                            IconButton(onClick = { contentVal += "_" }) {
                                Icon(Icons.Default.FormatItalic, contentDescription = "Italic notation")
                            }
                            IconButton(onClick = { contentVal += "\n• " }) {
                                Icon(Icons.Default.FormatListBulleted, contentDescription = "Bullet List")
                            }
                        }
                    }
                }
            }
        }
    }
}

data class ChecklistItem(
    val text: String,
    val checked: Boolean
)
