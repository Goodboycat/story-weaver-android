package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.credentials.CredentialManager
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.ai.AiParserService
import com.example.auth.signOut
import com.example.data.Localization
import com.example.data.PreferencesRepository
import com.example.data.StoryRepository
import com.example.data.UserSettings
import com.example.firebase.configureAppCheck
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.StoryViewScreen
import com.example.ui.theme.DarkBg
import com.example.ui.theme.StoryWeaverTheme
import com.example.viewmodel.DashboardViewModel
import com.example.viewmodel.StoryViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // STEP 1: Configure App Check with launch intent before any UI or Firebase operations
        configureAppCheck(this, intent)

        // STEP 2: Enable Edge-to-Edge and render Compose UI
        enableEdgeToEdge()
        setContent {
            StoryWeaverApp()
        }
    }
}

@Composable
fun StoryWeaverApp() {
    val context = LocalContext.current
    val prefsRepo = remember(context) { PreferencesRepository(context.applicationContext) }
    val settings by prefsRepo.settingsFlow.collectAsStateWithLifecycle(initialValue = UserSettings())
    val strings = remember(settings.languageCode) { Localization.stringsFor(settings.languageCode) }

    var authReady by remember { mutableStateOf(false) }
    var currentUser by remember { mutableStateOf<com.google.firebase.auth.FirebaseUser?>(null) }
    var firestoreDb by remember { mutableStateOf<FirebaseFirestore?>(null) }

    LaunchedEffect(Unit) {
        withFrameNanos { }
        delay(250L)
        val initialUser = withContext(Dispatchers.IO) {
            Firebase.auth.currentUser
        }
        currentUser = initialUser
        if (initialUser != null) {
            val dbId = context.applicationContext.getString(R.string.firestore_database_id)
            firestoreDb = withContext(Dispatchers.IO) {
                FirebaseFirestore.getInstance(dbId)
            }
        }
        authReady = true
    }

    LaunchedEffect(currentUser?.uid) {
        if (currentUser != null && firestoreDb == null) {
            val dbId = context.applicationContext.getString(R.string.firestore_database_id)
            firestoreDb = withContext(Dispatchers.IO) {
                FirebaseFirestore.getInstance(dbId)
            }
        }
    }

    DisposableEffect(authReady) {
        if (!authReady) {
            onDispose { }
        } else {
            val listener = FirebaseAuth.AuthStateListener { auth ->
                currentUser = auth.currentUser
            }
            Firebase.auth.addAuthStateListener(listener)
            onDispose { Firebase.auth.removeAuthStateListener(listener) }
        }
    }

    if (!authReady) {
        // Ultra-lightweight static frame so WMS/SurfaceSyncGroup completes immediately
        Box(modifier = Modifier.fillMaxSize().background(DarkBg))
        return
    }

    StoryWeaverTheme(settings = settings) {
        val user = currentUser
        val db = firestoreDb
        if (user == null) {
            AuthScreen(
                strings = strings,
                onAuthSuccess = { signedInUser ->
                    currentUser = signedInUser ?: Firebase.auth.currentUser
                }
            )
        } else if (db == null) {
            Surface(
                color = MaterialTheme.colorScheme.background,
                modifier = Modifier.fillMaxSize()
            ) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
        } else {
            val uid = user.uid
            key(uid) {
                AuthenticatedNavigation(
                    db = db,
                    prefsRepo = prefsRepo,
                    currentUserId = uid,
                    userDisplayName = user.displayName ?: user.email?.substringBefore("@") ?: "Adventurer",
                    userEmail = user.email,
                    strings = strings,
                    settings = settings,
                )
            }
        }
    }
}

@Composable
private fun AuthenticatedNavigation(
    db: FirebaseFirestore,
    prefsRepo: PreferencesRepository,
    currentUserId: String,
    userDisplayName: String,
    userEmail: String?,
    strings: com.example.data.AppStrings,
    settings: UserSettings,
) {
    val navController = rememberNavController()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    val dashboardViewModel: DashboardViewModel = viewModel(
        key = "dashboard_$currentUserId",
        factory = viewModelFactory {
            initializer {
                DashboardViewModel(
                    repository = StoryRepository(db),
                    preferencesRepository = prefsRepo,
                    currentUserId = currentUserId,
                )
            }
        }
    )

    NavHost(navController = navController, startDestination = "dashboard") {
        composable("dashboard") {
            DashboardScreen(
                viewModel = dashboardViewModel,
                strings = strings,
                settings = settings,
                currentUserId = currentUserId,
                userEmail = userEmail,
                onOpenStory = { storyId -> navController.navigate("story/$storyId") },
                onSignOut = {
                    signOut(credentialManager = credentialManager, scope = scope)
                }
            )
        }

        composable(
            route = "story/{storyId}",
            arguments = listOf(navArgument("storyId") { type = NavType.StringType })
        ) { backStackEntry ->
            val storyId = backStackEntry.arguments?.getString("storyId") ?: return@composable
            val storyViewModel: StoryViewModel = viewModel(
                key = "story_${currentUserId}_$storyId",
                factory = viewModelFactory {
                    initializer {
                        StoryViewModel(
                            repository = StoryRepository(db),
                            preferencesRepository = prefsRepo,
                            aiParser = AiParserService(),
                            storyId = storyId,
                            currentUserId = currentUserId,
                        )
                    }
                }
            )

            StoryViewScreen(
                viewModel = storyViewModel,
                strings = strings,
                userDisplayName = userDisplayName,
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
