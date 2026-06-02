package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Redo
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.viewmodel.MainViewModel
import com.example.viewmodel.StrokePath

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DrawingScreen(
    viewModel: MainViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    var selectedColor by remember { mutableStateOf(Color.Black) }
    var strokeWidth by remember { mutableStateOf(8f) }
    var isEraserMode by remember { mutableStateOf(false) }

    var canvasWidth by remember { mutableStateOf(0) }
    var canvasHeight by remember { mutableStateOf(0) }

    val presetInkColors = listOf(
        Color.Black,
        Color.DarkGray,
        Color(0xFFE53935), // Red
        Color(0xFF3949AB), // Indigo Blue
        Color(0xFF43A047), // Green
        Color(0xFFFFB300), // Orange/Gold
        Color(0xFF8E24AA)  // Purple
    )

    // Clear canvas when first opening Drawing mode
    LaunchedEffect(key1 = true) {
        viewModel.clearDrawingCanvas()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Drawing Canvas", fontSize = 18.sp, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.undoDrawing() }) {
                        Icon(Icons.Default.Undo, contentDescription = "Undo")
                    }
                    IconButton(onClick = { viewModel.redoDrawing() }) {
                        Icon(Icons.Default.Redo, contentDescription = "Redo")
                    }
                    IconButton(onClick = { viewModel.clearDrawingCanvas() }) {
                        Icon(Icons.Default.Delete, contentDescription = "Clear All")
                    }
                    IconButton(onClick = {
                        val path = viewModel.saveCanvasDrawing(canvasWidth, canvasHeight)
                        if (path != null) {
                            Toast.makeText(context, "Drawing saved onto note", Toast.LENGTH_SHORT).show()
                            onNavigateBack()
                        } else {
                            Toast.makeText(context, "Draw some lines first", Toast.LENGTH_SHORT).show()
                        }
                    }) {
                        Icon(Icons.Default.Done, contentDescription = "Done Save", tint = MaterialTheme.colorScheme.primary)
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(innerPadding)
        ) {
            // Interactive drawing area
            var activePath by remember { mutableStateOf(Path()) }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(16.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .border(2.dp, MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(16.dp))
                    .background(Color.White)
                    .onSizeChanged {
                        canvasWidth = it.width
                        canvasHeight = it.height
                    }
                    .pointerInput(selectedColor, strokeWidth, isEraserMode) {
                        detectDragGestures(
                            onDragStart = { offset ->
                                activePath = Path().apply { moveTo(offset.x, offset.y) }
                                val stroke = StrokePath(
                                    path = activePath,
                                    color = if (isEraserMode) Color.White else selectedColor,
                                    width = strokeWidth,
                                    isEraser = isEraserMode
                                )
                                viewModel.drawingPaths.add(stroke)
                                viewModel.drawingRedoStack.clear()
                            },
                            onDrag = { change, _ ->
                                change.consume()
                                activePath.lineTo(change.position.x, change.position.y)
                                
                                val idx = viewModel.drawingPaths.lastIndex
                                if (idx >= 0) {
                                    val currentStroke = viewModel.drawingPaths[idx]
                                    viewModel.drawingPaths[idx] = currentStroke.copy(path = activePath)
                                }
                            }
                        )
                    }
            ) {
                // Vector drawn strokes
                Canvas(modifier = Modifier.fillMaxSize()) {
                    for (stroke in viewModel.drawingPaths) {
                        drawPath(
                            path = stroke.path,
                            color = stroke.color,
                            style = Stroke(
                                width = stroke.width,
                                miter = 4f,
                                join = androidx.compose.ui.graphics.StrokeJoin.Round,
                                cap = androidx.compose.ui.graphics.StrokeCap.Round
                            )
                        )
                    }
                }

                if (viewModel.drawingPaths.isEmpty()) {
                    Text(
                        text = "Touch screen to start sketching",
                        fontSize = 14.sp,
                        color = Color.LightGray,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
            }

            // TOOLBAR SETTINGS CONTROLLER (Brush / Eraser, size and color settings)
            Surface(
                tonalElevation = 8.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Size slider, tool trigger
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            // Pen toggle
                            FilterChip(
                                selected = !isEraserMode,
                                onClick = { isEraserMode = false },
                                label = { Text("Brush Pen") },
                                leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp)) }
                            )

                            // Eraser toggle
                            FilterChip(
                                selected = isEraserMode,
                                onClick = { isEraserMode = true },
                                label = { Text("Eraser") },
                                leadingIcon = { Icon(Icons.Default.Clear, contentDescription = null, modifier = Modifier.size(16.dp)) }
                            )
                        }

                        // Pen size indicator
                        Text("Size: ${strokeWidth.toInt()}dp", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    }

                    Slider(
                        value = strokeWidth,
                        onValueChange = { strokeWidth = it },
                        valueRange = 2f..48f,
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Color row selectors (visible when not eraser)
                    if (!isEraserMode) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text("Ink: ", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                items(presetInkColors) { col ->
                                    val active = selectedColor == col
                                    Box(
                                        modifier = Modifier
                                            .size(34.dp)
                                            .clip(CircleShape)
                                            .background(col)
                                            .border(
                                                width = if (active) 3.dp else 1.dp,
                                                color = if (active) MaterialTheme.colorScheme.primary else Color.LightGray,
                                                shape = CircleShape
                                            )
                                            .clickable { selectedColor = col }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
