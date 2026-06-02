package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.models.Category
import com.example.models.Note
import com.example.viewmodel.MainViewModel
import com.example.viewmodel.SortOption
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(
    viewModel: MainViewModel,
    onNavigateToEditor: (Int?, Int?, Boolean) -> Unit,
    onNavigateToArchive: () -> Unit,
    onNavigateToFavorites: () -> Unit,
    onNavigateToDrawing: () -> Unit,
    onNavigateToVoiceNotes: () -> Unit,
    onNavigateToAttachments: () -> Unit,
    onNavigateToBackupRestore: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToCategories: () -> Unit,
    onNavigateToSearch: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val context = LocalContext.current

    // Observe data from ViewModel
    val notes by viewModel.notesFlow.collectAsState()
    val categories by viewModel.categoriesFlow.collectAsState()
    val selectedFilterCatId by viewModel.selectedCategoryFilter.collectAsState()

    var isGridView by remember { mutableStateOf(true) }
    var showSortMenu by remember { mutableStateOf(false) }

    // Quick Note Action Options Dialog/Sheet
    var activeLongClickedNote by remember { mutableStateOf<Note?>(null) }
    var isFabExpanded by remember { mutableStateOf(false) }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Notes,
                        contentDescription = "Aurapad Logo",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(36.dp)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        text = "AURAPAD",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Divider(modifier = Modifier.padding(vertical = 12.dp))

                // Navigation Items
                NavigationDrawerItem(
                    label = { Text("All Notes") },
                    selected = selectedFilterCatId == null,
                    onClick = {
                        viewModel.selectedCategoryFilter.value = null
                        coroutineScope.launch { drawerState.close() }
                    },
                    icon = { Icon(Icons.Outlined.Description, contentDescription = "Notes") },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )

                NavigationDrawerItem(
                    label = { Text("Favorites") },
                    selected = false,
                    onClick = {
                        coroutineScope.launch { drawerState.close() }
                        onNavigateToFavorites()
                    },
                    icon = { Icon(Icons.Outlined.StarBorder, contentDescription = "Favorites") },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )

                NavigationDrawerItem(
                    label = { Text("Archived") },
                    selected = false,
                    onClick = {
                        coroutineScope.launch { drawerState.close() }
                        onNavigateToArchive()
                    },
                    icon = { Icon(Icons.Outlined.Archive, contentDescription = "Archive") },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )

                NavigationDrawerItem(
                    label = { Text("Drawing Canvas") },
                    selected = false,
                    onClick = {
                        coroutineScope.launch { drawerState.close() }
                        onNavigateToDrawing()
                    },
                    icon = { Icon(Icons.Outlined.Brush, contentDescription = "Drawing") },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )

                NavigationDrawerItem(
                    label = { Text("Voice Recordings") },
                    selected = false,
                    onClick = {
                        coroutineScope.launch { drawerState.close() }
                        onNavigateToVoiceNotes()
                    },
                    icon = { Icon(Icons.Outlined.Mic, contentDescription = "Recordings") },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )

                NavigationDrawerItem(
                    label = { Text("Files & Attachments") },
                    selected = false,
                    onClick = {
                        coroutineScope.launch { drawerState.close() }
                        onNavigateToAttachments()
                    },
                    icon = { Icon(Icons.Outlined.AttachFile, contentDescription = "Files") },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )

                Divider(modifier = Modifier.padding(vertical = 12.dp))

                NavigationDrawerItem(
                    label = { Text("Manage Folders") },
                    selected = false,
                    onClick = {
                        coroutineScope.launch { drawerState.close() }
                        onNavigateToCategories()
                    },
                    icon = { Icon(Icons.Outlined.Folder, contentDescription = "Folders") },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )

                NavigationDrawerItem(
                    label = { Text("Backup & Restore") },
                    selected = false,
                    onClick = {
                        coroutineScope.launch { drawerState.close() }
                        onNavigateToBackupRestore()
                    },
                    icon = { Icon(Icons.Outlined.SettingsBackupRestore, contentDescription = "Backup") },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )

                NavigationDrawerItem(
                    label = { Text("Settings") },
                    selected = false,
                    onClick = {
                        coroutineScope.launch { drawerState.close() }
                        onNavigateToSettings()
                    },
                    icon = { Icon(Icons.Outlined.Settings, contentDescription = "Settings") },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )
            }
        }
    ) {
        Scaffold(
            topBar = {
                Surface(
                    color = MaterialTheme.colorScheme.background,
                    tonalElevation = 2.dp
                ) {
                    Column {
                        // Custom Header toolbar (Bento Grid Style)
                        Row(
                            modifier = Modifier
                                .statusBarsPadding()
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = { coroutineScope.launch { drawerState.open() } },
                                modifier = Modifier.size(44.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Menu,
                                    contentDescription = "Menu",
                                    tint = MaterialTheme.colorScheme.onBackground
                                )
                            }

                            // Interactive search bar layout styled as an elegant Bento pill
                            Card(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .clickable { onNavigateToSearch() }
                                    .border(
                                        width = 1.dp,
                                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.8f),
                                        shape = RoundedCornerShape(24.dp)
                                    ),
                                shape = RoundedCornerShape(24.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surface
                                ),
                                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(horizontal = 16.dp, vertical = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Search, 
                                        contentDescription = "Search Notes",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = "Search your notes...",
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                        fontSize = 14.sp,
                                        modifier = Modifier.weight(1f)
                                    )
                                    // Beautiful Avatar Badge from Bento Mockup
                                    Box(
                                        modifier = Modifier
                                            .size(28.dp)
                                            .background(MaterialTheme.colorScheme.primary, CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "A",
                                            color = MaterialTheme.colorScheme.onPrimary,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.width(4.dp))

                            // View config controls
                            IconButton(
                                onClick = { isGridView = !isGridView },
                                modifier = Modifier.size(44.dp)
                            ) {
                                Icon(
                                    imageVector = if (isGridView) Icons.Default.ViewList else Icons.Default.GridView,
                                    contentDescription = "Change Layout",
                                    tint = MaterialTheme.colorScheme.onBackground
                                )
                            }
                            
                            Box {
                                IconButton(
                                    onClick = { showSortMenu = true },
                                    modifier = Modifier.size(44.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Sort, 
                                        contentDescription = "Sort Options",
                                        tint = MaterialTheme.colorScheme.onBackground
                                    )
                                }
                                DropdownMenu(
                                    expanded = showSortMenu,
                                    onDismissRequest = { showSortMenu = false }
                                ) {
                                    DropdownMenuItem(
                                        text = { Text("Modified (Newest)") },
                                        onClick = { viewModel.sortOption.value = SortOption.MODIFIED_DESC; showSortMenu = false }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Modified (Oldest)") },
                                        onClick = { viewModel.sortOption.value = SortOption.MODIFIED_ASC; showSortMenu = false }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Created (Newest)") },
                                        onClick = { viewModel.sortOption.value = SortOption.CREATED_DESC; showSortMenu = false }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Title (A-Z)") },
                                        onClick = { viewModel.sortOption.value = SortOption.TITLE_ASC; showSortMenu = false }
                                    )
                                }
                            }
                        }

                        // Horizontal Category quick-toggle pills
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            item {
                                FilterChip(
                                    selected = selectedFilterCatId == null,
                                    onClick = { viewModel.selectedCategoryFilter.value = null },
                                    label = { Text("All") },
                                    shape = RoundedCornerShape(12.dp)
                                )
                            }
                            items(categories) { category ->
                                val active = selectedFilterCatId == category.id
                                val catColor = try {
                                    Color(android.graphics.Color.parseColor(category.colorHex))
                                } catch (e: Exception) {
                                    MaterialTheme.colorScheme.primary
                                }
                                FilterChip(
                                    selected = active,
                                    onClick = { viewModel.selectedCategoryFilter.value = category.id },
                                    label = { 
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Box(
                                                modifier = Modifier
                                                    .size(8.dp)
                                                    .clip(CircleShape)
                                                    .background(catColor)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(category.name)
                                        }
                                    },
                                    shape = RoundedCornerShape(12.dp)
                                )
                            }
                        }
                    }
                }
            },
            floatingActionButton = {
                Column(
                    horizontalAlignment = Alignment.End, 
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    AnimatedVisibility(
                        visible = isFabExpanded,
                        enter = fadeIn(animationSpec = tween(200)) + slideInVertically(animationSpec = tween(250)) { it / 2 },
                        exit = fadeOut(animationSpec = tween(150)) + slideOutVertically(animationSpec = tween(180)) { it / 2 }
                    ) {
                        Column(
                            horizontalAlignment = Alignment.End,
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // Sub-action: Take Text Note
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.padding(end = 4.dp)
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                                    shadowElevation = 2.dp
                                ) {
                                    Text(
                                        text = "Text Note",
                                        color = MaterialTheme.colorScheme.surface,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                    )
                                }
                                SmallFloatingActionButton(
                                    onClick = {
                                        isFabExpanded = false
                                        onNavigateToEditor(null, selectedFilterCatId, false)
                                    },
                                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.EditNote,
                                        contentDescription = "New Text Note"
                                    )
                                }
                            }

                            // Sub-action: Audio Record
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.padding(end = 4.dp)
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                                    shadowElevation = 2.dp
                                ) {
                                    Text(
                                        text = "Voice Record",
                                        color = MaterialTheme.colorScheme.surface,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                    )
                                }
                                SmallFloatingActionButton(
                                    onClick = {
                                        isFabExpanded = false
                                        onNavigateToEditor(null, selectedFilterCatId, true)
                                    },
                                    containerColor = MaterialTheme.colorScheme.errorContainer,
                                    contentColor = MaterialTheme.colorScheme.onErrorContainer
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Mic,
                                        contentDescription = "New Voice Record"
                                    )
                                }
                            }
                        }
                    }

                    // Main FAB
                    FloatingActionButton(
                        onClick = { isFabExpanded = !isFabExpanded },
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.testTag("add_note_fab")
                    ) {
                        Icon(
                            imageVector = if (isFabExpanded) Icons.Default.Close else Icons.Default.Add,
                            contentDescription = if (isFabExpanded) "Close Menu" else "Add Note"
                        )
                    }
                }
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
                    .padding(innerPadding)
            ) {
                // Dimmed backdrop overlay when FAB is expanded, mimicking Samsung Notes overlay style
                AnimatedVisibility(
                    visible = isFabExpanded,
                    enter = fadeIn(animationSpec = tween(200)),
                    exit = fadeOut(animationSpec = tween(150))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.45f))
                            .clickable { isFabExpanded = false }
                    )
                }

                if (notes.isEmpty()) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Notes,
                            contentDescription = "No Notes",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
                            modifier = Modifier.size(80.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "No notes created yet",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Press the + button to capture your offline thoughts or sketch drawings",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            textAlign = TextAlign.Center
                        )
                    }
                } else {
                    Crossfade(
                        targetState = isGridView,
                        animationSpec = tween(350),
                        label = "GridListSwitch"
                    ) { gridMode ->
                        if (gridMode) {
                            LazyVerticalGrid(
                                columns = GridCells.Fixed(2),
                                contentPadding = PaddingValues(12.dp),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                items(notes, key = { it.id }) { note ->
                                    NoteCard(
                                        note = note,
                                        categories = categories,
                                        onClick = { 
                                            if (note.isLocked) {
                                                activeLongClickedNote = note
                                            } else {
                                                onNavigateToEditor(note.id, null, false)
                                            }
                                        },
                                        onLongClick = { activeLongClickedNote = note }
                                    )
                                }
                            }
                        } else {
                            LazyVerticalGrid(
                                columns = GridCells.Fixed(1),
                                contentPadding = PaddingValues(12.dp),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                items(notes, key = { it.id }) { note ->
                                    NoteCard(
                                        note = note,
                                        categories = categories,
                                        onClick = { 
                                            if (note.isLocked) {
                                                activeLongClickedNote = note
                                            } else {
                                                onNavigateToEditor(note.id, null, false)
                                            }
                                        },
                                        onLongClick = { activeLongClickedNote = note }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Direct Options Sheet/Dialog for long click activations
    activeLongClickedNote?.let { note ->
        AlertDialog(
            onDismissRequest = { activeLongClickedNote = null },
            title = { Text(note.title.ifEmpty { "Empty Note" }, maxLines = 1, overflow = TextOverflow.Ellipsis) },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (note.isLocked) {
                        Text("This note is password protected. Enter PIN code to open.")
                    } else {
                        Text("Organize or change settings for this selected local note:")
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        activeLongClickedNote = null
                        if (!note.isLocked) {
                            onNavigateToEditor(note.id, null, false)
                        } else {
                            // Manual editor entry
                            onNavigateToEditor(note.id, null, false)
                        }
                    }
                ) {
                    Text(if (note.isLocked) "Unlock" else "Open Editor")
                }
            },
            dismissButton = {
                TextButton(onClick = { activeLongClickedNote = null }) {
                    Text("Close")
                }
            }
        )
    }
}

// Visual layout of individual notes represented in cards
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun NoteCard(
    note: Note,
    categories: List<Category>,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val category = categories.find { it.id == note.categoryId }
    val formattedDate = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date(note.modifiedAt))
    val darkTheme = androidx.compose.foundation.isSystemInDarkTheme()

    // Smart color computation for Bento Grid theme
    val isBgDark = if (note.colorInt != 0) {
        val col = Color(note.colorInt)
        (col.red * 0.299f + col.green * 0.587f + col.blue * 0.114f) < 0.5f
    } else {
        darkTheme
    }

    val titleColor = if (isBgDark) Color.White else Color(0xFF0F172A)
    val bodyColor = if (isBgDark) Color.White.copy(alpha = 0.75f) else Color(0xFF475569)

    val cardBgColor = when {
        note.voicePath != null -> {
            if (darkTheme) MaterialTheme.colorScheme.secondaryContainer else Color(0xFFFEF7FF)
        }
        note.colorInt != 0 -> Color(note.colorInt)
        else -> MaterialTheme.colorScheme.surface
    }

    val cardBorderColor = when {
        note.isPinned -> MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
        note.voicePath != null -> {
            if (darkTheme) MaterialTheme.colorScheme.outlineVariant else Color(0xFFEADDFF)
        }
        else -> MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
    }

    val scale = remember { androidx.compose.animation.core.Animatable(0.92f) }
    val alphaAnim = remember { androidx.compose.animation.core.Animatable(0.3f) }
    LaunchedEffect(note.id) {
        launch {
            scale.animateTo(
                1f,
                animationSpec = androidx.compose.animation.core.spring(
                    dampingRatio = androidx.compose.animation.core.Spring.DampingRatioMediumBouncy,
                    stiffness = androidx.compose.animation.core.Spring.StiffnessLow
                )
            )
        }
        launch {
            alphaAnim.animateTo(
                1f,
                animationSpec = androidx.compose.animation.core.tween(durationMillis = 250)
            )
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = scale.value
                scaleY = scale.value
                this.alpha = alphaAnim.value
            }
            .clip(RoundedCornerShape(24.dp))
            .border(
                1.5.dp,
                cardBorderColor,
                RoundedCornerShape(24.dp)
            )
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
            .testTag("note_item_${note.id}"),
        colors = CardDefaults.cardColors(
            containerColor = cardBgColor
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
            // No hardcoded padding so drawings can go full-width if needed
        ) {
            // Graphical top banner for Drawings to look like custom mock cards
            if (note.drawingPath != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(72.dp)
                        .background(if (darkTheme) Color(0xFF1E2530) else Color(0xFFF1F5F9))
                        .padding(bottom = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
                        val p = androidx.compose.ui.graphics.Path().apply {
                            moveTo(size.width * 0.15f, size.height * 0.65f)
                            quadraticTo(
                                size.width * 0.35f, size.height * 0.25f,
                                size.width * 0.55f, size.height * 0.65f
                            )
                            quadraticTo(
                                size.width * 0.75f, size.height * 0.95f,
                                size.width * 0.9f, size.height * 0.45f
                            )
                        }
                        drawPath(
                            path = p,
                            color = if (darkTheme) Color(0xFF9ECAFF).copy(alpha = 0.4f) else Color(0xFF0061A4).copy(alpha = 0.4f),
                            style = androidx.compose.ui.graphics.drawscope.Stroke(
                                width = 3.dp.toPx(),
                                cap = androidx.compose.ui.graphics.StrokeCap.Round
                            )
                        )
                        drawCircle(
                            color = if (darkTheme) Color(0xFF9ECAFF).copy(alpha = 0.4f) else Color(0xFF0061A4).copy(alpha = 0.4f),
                            radius = 4.dp.toPx(),
                            center = androidx.compose.ui.geometry.Offset(size.width * 0.8f, size.height * 0.25f)
                        )
                    }
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(6.dp)
                            .background(Color.Black.copy(alpha = 0.2f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 4.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "SKETCH",
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }

            // Recording Equalizer display for voice memo representation from bento mockup
            if (note.voicePath != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(if (darkTheme) Color(0xFF281C4E) else Color(0xFFFEF7FF))
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(3.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(if (darkTheme) Color(0xFFD0BCFF) else Color(0xFF6750A4), CircleShape)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        listOf(8.dp, 16.dp, 12.dp, 20.dp, 14.dp, 8.dp).forEach { height ->
                            Box(
                                modifier = Modifier
                                    .width(3.dp)
                                    .height(height)
                                    .background(
                                        if (darkTheme) Color(0xFFD0BCFF) else Color(0xFF6750A4),
                                        RoundedCornerShape(1.dp)
                                    )
                            )
                        }
                    }
                    
                    Text(
                        text = "02:45",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (darkTheme) Color(0xFFD0BCFF) else Color(0xFF6750A4)
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Category Tag/Badge
                    if (category != null) {
                        val catColor = try {
                            Color(android.graphics.Color.parseColor(category.colorHex))
                        } catch (e: Exception) {
                            MaterialTheme.colorScheme.primary
                        }
                        Box(
                            modifier = Modifier
                                .background(catColor.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = category.name,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = catColor
                            )
                        }
                    } else {
                        Spacer(modifier = Modifier.width(1.dp))
                    }

                    // Metadata tags: Pin / Lock / Favorite
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (note.isPinned) {
                            Icon(
                                imageVector = Icons.Default.PushPin,
                                contentDescription = "Pinned",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(13.dp)
                            )
                        }
                        if (note.isLocked) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = "Locked",
                                tint = if (isBgDark) Color.LightGray else Color.DarkGray,
                                modifier = Modifier.size(13.dp)
                            )
                        }
                        if (note.isFavorite) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = "Favorite",
                                tint = Color(0xFFFFB300),
                                modifier = Modifier.size(13.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Text Title
                Text(
                    text = note.title.ifEmpty { "Untitled Plan" },
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = titleColor,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Body preview / checklists / locked text representation
                if (note.isLocked) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(vertical = 4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            tint = bodyColor.copy(alpha = 0.6f),
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Confidential Folder",
                            fontSize = 12.sp,
                            color = bodyColor.copy(alpha = 0.8f),
                            fontWeight = FontWeight.Medium
                        )
                    }
                } else {
                    val parsedLines = note.content.lines().filter { it.isNotBlank() }.take(3)
                    if (parsedLines.any { it.trim().startsWith("-") || it.trim().startsWith("*") || it.trim().startsWith("[") || it.trim().startsWith("•") }) {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(5.dp),
                            modifier = Modifier.padding(vertical = 4.dp)
                        ) {
                            parsedLines.forEach { line ->
                                val cleanLine = line.trim().trimStart('-', '*', '•', '[', ']', 'x', ' ').trim()
                                val isChecked = line.contains("[x]") || line.contains("[X]")
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(12.dp)
                                            .border(
                                                width = 1.dp,
                                                color = if (isChecked) titleColor else titleColor.copy(alpha = 0.5f),
                                                shape = CircleShape
                                            )
                                            .background(
                                                color = if (isChecked) titleColor.copy(alpha = 0.15f) else Color.Transparent,
                                                shape = CircleShape
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (isChecked) {
                                            Box(
                                                modifier = Modifier
                                                    .size(5.dp)
                                                    .background(titleColor, CircleShape)
                                            )
                                        }
                                    }
                                    Text(
                                        text = cleanLine,
                                        fontSize = 12.sp,
                                        color = if (isChecked) bodyColor.copy(alpha = 0.45f) else bodyColor,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        style = if (isChecked) androidx.compose.ui.text.TextStyle(
                                            textDecoration = androidx.compose.ui.text.style.TextDecoration.LineThrough
                                        ) else androidx.compose.ui.text.TextStyle.Default
                                    )
                                }
                            }
                        }
                    } else {
                        Text(
                            text = note.content,
                            fontSize = 12.sp,
                            color = bodyColor,
                            maxLines = 4,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Media references & Date Bottom alignment
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = formattedDate,
                        fontSize = 10.sp,
                        color = bodyColor.copy(alpha = 0.6f),
                        fontWeight = FontWeight.Medium
                    )

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (note.drawingPath != null) {
                            Icon(
                                imageVector = Icons.Default.Brush,
                                contentDescription = "Has Drawing",
                                tint = bodyColor.copy(alpha = 0.6f),
                                modifier = Modifier.size(13.dp)
                            )
                        }
                        if (note.voicePath != null) {
                            Icon(
                                imageVector = Icons.Default.Mic,
                                contentDescription = "Has Recording",
                                tint = bodyColor.copy(alpha = 0.6f),
                                modifier = Modifier.size(13.dp)
                            )
                        }
                        if (note.attachmentPath != null) {
                            Icon(
                                imageVector = Icons.Default.AttachFile,
                                contentDescription = "Has Attachment",
                                tint = bodyColor.copy(alpha = 0.6f),
                                modifier = Modifier.size(13.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

