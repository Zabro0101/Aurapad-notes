package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.SettingsBackupRestore
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.example.backup.BackupManager
import com.example.viewmodel.MainViewModel
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackupRestoreScreen(
    viewModel: MainViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    var isBackingUp by remember { mutableStateOf(false) }
    var isRestoring by remember { mutableStateOf(false) }

    // Backup file state holders
    var generatedBackupFile by remember { mutableStateOf<File?>(null) }

    // Resolve directories
    val backupDirectory = remember { BackupManager.getLocalBackupDirectory(context) }
    val exportsDirectory = remember { BackupManager.getLocalExportDirectory(context) }

    var localBackupFiles by remember { mutableStateOf<List<File>>(emptyList()) }
    var fileToConfirmRestore by remember { mutableStateOf<File?>(null) }
    var fileToConfirmDelete by remember { mutableStateOf<File?>(null) }

    val refreshLocalBackups = {
        try {
            val files = backupDirectory.listFiles { file -> file.isFile && file.name.endsWith(".xml") }
            localBackupFiles = files?.sortedByDescending { it.lastModified() } ?: emptyList()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    LaunchedEffect(Unit) {
        refreshLocalBackups()
    }

    // Restore pick file selector of XML backup files
    val xmlPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            isRestoring = true
            try {
                val input = context.contentResolver.openInputStream(uri)
                if (input != null) {
                    val tempBackupFile = File(context.cacheDir, "temp_restore_backup.xml")
                    FileOutputStream(tempBackupFile).use { out ->
                        input.copyTo(out)
                    }

                    viewModel.triggerLocalRestore(tempBackupFile) { success ->
                        isRestoring = false
                        if (success) {
                            Toast.makeText(context, "XML Backup parsed successfully! All notes restored.", Toast.LENGTH_LONG).show()
                            refreshLocalBackups()
                        } else {
                            Toast.makeText(context, "Invalid XML backup document format", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                isRestoring = false
                Toast.makeText(context, "Failed to parse XML backup", Toast.LENGTH_SHORT).show()
            }
        }
    }

    Scaffold(
        topBar = {
            MediumTopAppBar(
                title = { Text("Backup & Diagnostics") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
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
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Explanation Info Card
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                ),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info, 
                            contentDescription = null, 
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Local Storage Backup & Restore",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Everything is saved 100% locally on your device. You can create complete XML backups containing your database (notes, categories, checklists) and attachments (drawings and voice recordings) serialized directly inside a .xml package.",
                        fontSize = 13.sp,
                        lineHeight = 18.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Divider(color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "📁 Local Backups Folder:\n${backupDirectory.absolutePath}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "📂 Local Exports Folder:\n${exportsDirectory.absolutePath}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // BACKUP GENERATION TRIGGERS
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.SettingsBackupRestore, 
                        contentDescription = null, 
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(48.dp)
                    )
                    
                    Text("Manage Local XML Backups", fontWeight = FontWeight.Bold, fontSize = 16.sp)

                    Button(
                        onClick = {
                            isBackingUp = true
                            viewModel.triggerLocalBackup { file ->
                                isBackingUp = false
                                if (file != null) {
                                    generatedBackupFile = file
                                    Toast.makeText(context, "XML Backup stored in Backups folder!", Toast.LENGTH_SHORT).show()
                                    refreshLocalBackups()
                                } else {
                                    Toast.makeText(context, "Failed to compile XML", Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        enabled = !isBackingUp && !isRestoring,
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (isBackingUp) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp))
                        } else {
                            Icon(Icons.Default.CloudDone, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Create XML Backup")
                        }
                    }

                    // Share button triggers if file generated
                    AnimatedVisibility(visible = generatedBackupFile != null) {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Saved as: ${generatedBackupFile?.name}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Button(
                                onClick = {
                                    val file = generatedBackupFile
                                    if (file != null) {
                                        try {
                                            val fileUri = FileProvider.getUriForFile(
                                                context,
                                                "${context.packageName}.provider",
                                                file
                                            )
                                            val intent = Intent(Intent.ACTION_SEND).apply {
                                                type = "text/xml"
                                                putExtra(Intent.EXTRA_STREAM, fileUri)
                                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                            }
                                            context.startActivity(Intent.createChooser(intent, "Share backup XML file"))
                                        } catch (e: Exception) {
                                            e.printStackTrace()
                                            Toast.makeText(context, "Sharing failed", Toast.LENGTH_LONG).show()
                                        }
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                                shape = RoundedCornerShape(20.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.Share, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Share XML Backup File")
                            }
                        }
                    }
                }
            }

            // RESTORE METADATA TRIGGERS
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.UploadFile, 
                        contentDescription = null, 
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(48.dp)
                    )
                    
                    Text("Recover from local XML backups", fontWeight = FontWeight.Bold, fontSize = 16.sp)

                    OutlinedButton(
                        onClick = { xmlPickerLauncher.launch("*/*") },
                        enabled = !isBackingUp && !isRestoring,
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (isRestoring) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp))
                        } else {
                            Text("Select and Restore XML Backup")
                        }
                    }
                }
            }

            // LOCAL BACKUPS IN FOLDER LISTING
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Default.FolderOpen,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Backups folder (${localBackupFiles.size})",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }

                    if (localBackupFiles.isEmpty()) {
                        Text(
                            text = "No backup files found in Backups folders. Tap 'Create XML Backup' to save a local snapshot.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(vertical = 12.dp)
                        )
                    } else {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            localBackupFiles.forEach { file ->
                                val dateStr = remember(file) {
                                    try {
                                        val sdf = SimpleDateFormat("MMM d, yyyy - hh:mm a", Locale.getDefault())
                                        sdf.format(Date(file.lastModified()))
                                    } catch (e: Exception) {
                                        ""
                                    }
                                }
                                val sizeStr = remember(file) {
                                    val sizeKb = file.length() / 1024
                                    if (sizeKb > 1024) {
                                        String.format(Locale.getDefault(), "%.1f MB", sizeKb / 1024.0)
                                    } else {
                                        "$sizeKb KB"
                                    }
                                }

                                Card(
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.surface
                                    ),
                                    shape = RoundedCornerShape(16.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = file.name,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp,
                                                maxLines = 1,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = "$dateStr • $sizeStr",
                                                fontSize = 11.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }

                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            // Restore Button
                                            IconButton(
                                                onClick = { fileToConfirmRestore = file },
                                                colors = IconButtonDefaults.iconButtonColors(
                                                    contentColor = MaterialTheme.colorScheme.primary
                                                )
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Restore,
                                                    contentDescription = "Restore this backup"
                                                )
                                            }

                                            // Delete Button
                                            IconButton(
                                                onClick = { fileToConfirmDelete = file },
                                                colors = IconButtonDefaults.iconButtonColors(
                                                    contentColor = MaterialTheme.colorScheme.error
                                                )
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Delete,
                                                    contentDescription = "Delete this backup"
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
        }
    }

    // Confirmation dialogs
    if (fileToConfirmRestore != null) {
        val file = fileToConfirmRestore!!
        AlertDialog(
            onDismissRequest = { fileToConfirmRestore = null },
            title = { Text("Restore backup?") },
            text = {
                Text("Are you sure you want to restore all entries from '${file.name}'? Existing notes or categories with the same IDs will be replaced. This will restore checkboxes, drawings, attachments, and voice notes accurately.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val targetFile = fileToConfirmRestore
                        fileToConfirmRestore = null
                        if (targetFile != null) {
                            isRestoring = true
                            viewModel.triggerLocalRestore(targetFile) { success ->
                                isRestoring = false
                                if (success) {
                                    Toast.makeText(context, "Notes restored successfully! App is synchronized.", Toast.LENGTH_LONG).show()
                                } else {
                                    Toast.makeText(context, "Failed to restore backup", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    }
                ) {
                    Text("Restore Now")
                }
            },
            dismissButton = {
                TextButton(onClick = { fileToConfirmRestore = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (fileToConfirmDelete != null) {
        val file = fileToConfirmDelete!!
        AlertDialog(
            onDismissRequest = { fileToConfirmDelete = null },
            title = { Text("Delete Backup File?") },
            text = {
                Text("This permanently deletes '${file.name}' from your device's Backups folder. This action cannot be reversed.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val targetFile = fileToConfirmDelete
                        fileToConfirmDelete = null
                        if (targetFile != null) {
                            try {
                                if (targetFile.delete()) {
                                    Toast.makeText(context, "Backup file deleted permanently", Toast.LENGTH_SHORT).show()
                                    refreshLocalBackups()
                                } else {
                                    Toast.makeText(context, "Could not delete backup file", Toast.LENGTH_SHORT).show()
                                }
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { fileToConfirmDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}
