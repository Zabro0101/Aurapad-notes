package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import com.example.ui.screens.*
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.MainViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val viewModel: MainViewModel = viewModel()
            
            // Collect theme configurations
            val themeSetting by viewModel.themeState.collectAsState()
            val onboardingCompleted by viewModel.onboardingCompleted.collectAsState()
            val masterPin by viewModel.masterPin.collectAsState()

            val isDarkTheme = when (themeSetting) {
                "dark" -> true
                "light" -> false
                else -> androidx.compose.foundation.isSystemInDarkTheme()
            }

            MyApplicationTheme(darkTheme = isDarkTheme) {
                val navController = rememberNavController()
                val context = LocalContext.current
                val focusManager = androidx.compose.ui.platform.LocalFocusManager.current
                val keyboardController = androidx.compose.ui.platform.LocalSoftwareKeyboardController.current

                // Request microphone record permissions for audio support
                val permissionLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestPermission()
                ) { isGranted ->
                    if (!isGranted) {
                        Toast.makeText(context, "Microphone permissions are required for voice memo features", Toast.LENGTH_LONG).show()
                    }
                }

                LaunchedEffect(key1 = true) {
                    if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                        permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                    }
                }

                // Global Entry Master passcode screen
                var isAppLocked by remember(masterPin) { mutableStateOf(masterPin != null) }
                var entryPinText by remember { mutableStateOf("") }

                if (isAppLocked && masterPin != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.background)
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lock, 
                                contentDescription = "Security Screen Lock", 
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(80.dp)
                            )
                            Spacer(modifier = Modifier.height(24.dp))
                            Text(
                                "Locked by Aurapad Safeguard", 
                                fontSize = 22.sp, 
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "Enter your 4-digit Master PIN to unlock the app.", 
                                fontSize = 14.sp, 
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 24.dp)
                            )
                            Spacer(modifier = Modifier.height(32.dp))

                            OutlinedTextField(
                                value = entryPinText,
                                onValueChange = { if (it.length <= 4) entryPinText = it },
                                label = { Text("4-digit entry PIN") },
                                singleLine = true,
                                modifier = Modifier.width(180.dp),
                                textStyle = TextStyle(fontSize = 20.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                            )

                            Spacer(modifier = Modifier.height(32.dp))
                            Button(
                                onClick = {
                                    if (entryPinText == masterPin) {
                                        keyboardController?.hide()
                                        focusManager.clearFocus()
                                        isAppLocked = false
                                    } else {
                                        Toast.makeText(context, "Invalid Master PIN", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.width(180.dp)
                            ) {
                                Text("Unlock App", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                } else {
                    // Start navigation Graph
                    NavHost(
                        navController = navController,
                        startDestination = "splash",
                        enterTransition = {
                            slideIntoContainer(
                                AnimatedContentTransitionScope.SlideDirection.Left,
                                animationSpec = tween(350)
                            ) + fadeIn(animationSpec = tween(350))
                        },
                        exitTransition = {
                            slideOutOfContainer(
                                AnimatedContentTransitionScope.SlideDirection.Left,
                                animationSpec = tween(350)
                            ) + fadeOut(animationSpec = tween(350))
                        },
                        popEnterTransition = {
                            slideIntoContainer(
                                AnimatedContentTransitionScope.SlideDirection.Right,
                                animationSpec = tween(350)
                            ) + fadeIn(animationSpec = tween(350))
                        },
                        popExitTransition = {
                            slideOutOfContainer(
                                AnimatedContentTransitionScope.SlideDirection.Right,
                                animationSpec = tween(350)
                            ) + fadeOut(animationSpec = tween(350))
                        }
                    ) {
                        composable(
                            "splash",
                            enterTransition = { EnterTransition.None },
                            exitTransition = { fadeOut(animationSpec = tween(350)) }
                        ) {
                            SplashScreen(
                                onOnboardingCheck = {
                                    if (onboardingCompleted) {
                                        navController.navigate("home") {
                                            popUpTo("splash") { inclusive = true }
                                        }
                                    } else {
                                        navController.navigate("onboarding") {
                                            popUpTo("splash") { inclusive = true }
                                        }
                                    }
                                }
                            )
                        }

                        composable(
                            "onboarding",
                            enterTransition = { fadeIn(animationSpec = tween(350)) },
                            exitTransition = { fadeOut(animationSpec = tween(350)) }
                        ) {
                            OnboardingScreen(
                                onFinish = {
                                    viewModel.completeOnboarding()
                                    navController.navigate("home") {
                                        popUpTo("onboarding") { inclusive = true }
                                    }
                                }
                            )
                        }

                        composable("home") {
                            HomeScreen(
                                viewModel = viewModel,
                                onNavigateToEditor = { noteId, catId, autoRecord ->
                                    val safeId = noteId ?: 0
                                    var queryPart = if (catId != null) "?catId=$catId" else ""
                                    if (autoRecord) {
                                        queryPart += if (queryPart.isEmpty()) "?autoRecord=true" else "&autoRecord=true"
                                    }
                                    navController.navigate("editor/$safeId$queryPart")
                                },
                                onNavigateToArchive = { navController.navigate("archive") },
                                onNavigateToFavorites = { navController.navigate("favorites") },
                                onNavigateToDrawing = { navController.navigate("drawing") },
                                onNavigateToVoiceNotes = { navController.navigate("voice_notes") },
                                onNavigateToAttachments = { navController.navigate("attachments") },
                                onNavigateToBackupRestore = { navController.navigate("backup_restore") },
                                onNavigateToSettings = { navController.navigate("settings") },
                                onNavigateToCategories = { navController.navigate("categories") },
                                onNavigateToSearch = { navController.navigate("search") }
                            )
                        }

                        composable(
                            route = "editor/{noteId}?catId={catId}&autoRecord={autoRecord}",
                            arguments = listOf(
                                navArgument("noteId") { type = NavType.IntType },
                                navArgument("catId") { 
                                    type = NavType.IntType
                                    defaultValue = 0 
                                },
                                navArgument("autoRecord") {
                                    type = NavType.BoolType
                                    defaultValue = false
                                }
                            )
                        ) { backStackEntry ->
                            val noteId = backStackEntry.arguments?.getInt("noteId") ?: 0
                            val catIdArg = backStackEntry.arguments?.getInt("catId") ?: 0
                            val catId = if (catIdArg == 0) null else catIdArg
                            val autoRecord = backStackEntry.arguments?.getBoolean("autoRecord") ?: false

                            NoteEditorScreen(
                                viewModel = viewModel,
                                noteId = noteId,
                                initialCategoryId = catId,
                                autoRecord = autoRecord,
                                onNavigateBack = { navController.popBackStack() },
                                onNavigateToCanvasPaint = { navController.navigate("drawing") }
                            )
                        }

                        composable("categories") {
                            CategoriesScreen(
                                viewModel = viewModel,
                                onNavigateBack = { navController.popBackStack() }
                            )
                        }

                        composable("archive") {
                            ArchiveScreen(
                                viewModel = viewModel,
                                onNavigateBack = { navController.popBackStack() }
                            )
                        }

                        composable("favorites") {
                            FavoritesScreen(
                                viewModel = viewModel,
                                onNavigateToEditor = { noteId -> navController.navigate("editor/$noteId") },
                                onNavigateBack = { navController.popBackStack() }
                            )
                        }

                        composable("drawing") {
                            DrawingScreen(
                                viewModel = viewModel,
                                onNavigateBack = { navController.popBackStack() }
                            )
                        }

                        composable("voice_notes") {
                            VoiceNotesScreen(
                                viewModel = viewModel,
                                onNavigateToEditor = { noteId -> navController.navigate("editor/$noteId") },
                                onNavigateBack = { navController.popBackStack() }
                            )
                        }

                        composable("attachments") {
                            AttachmentsScreen(
                                viewModel = viewModel,
                                onNavigateToEditor = { noteId -> navController.navigate("editor/$noteId") },
                                onNavigateBack = { navController.popBackStack() }
                            )
                        }

                        composable("search") {
                            SearchScreen(
                                viewModel = viewModel,
                                onNavigateToEditor = { noteId -> navController.navigate("editor/$noteId") },
                                onNavigateBack = { navController.popBackStack() }
                            )
                        }

                        composable("backup_restore") {
                            BackupRestoreScreen(
                                viewModel = viewModel,
                                onNavigateBack = { navController.popBackStack() }
                            )
                        }

                        composable("settings") {
                            SettingsScreen(
                                viewModel = viewModel,
                                onNavigateBack = { navController.popBackStack() }
                            )
                        }
                    }
                }
            }
        }
    }
}
