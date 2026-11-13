package com.example

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.database.AppDatabase
import com.example.data.repository.WordRepository
import com.example.ui.components.AppBottomNav
import com.example.ui.components.AppTopBar
import com.example.ui.screens.DictionaryScreen
import com.example.ui.screens.FlashcardsScreen
import com.example.ui.screens.GeminiChatScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.ModeSelectionScreen
import com.example.ui.screens.QuizPlayScreen
import com.example.ui.screens.SentenceBuilderScreen
import com.example.ui.screens.SpeedMatchScreen
import com.example.ui.screens.StatsScreen
import com.example.ui.screens.WordBuilderScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.PrimaryIndigo
import com.example.util.TtsHelper
import com.example.util.configureAppCheck
import com.example.viewmodel.GeminiChatViewModel
import com.example.viewmodel.MainViewModel
import com.example.viewmodel.QuizMode
import com.example.viewmodel.Screen

class MainActivity : ComponentActivity() {

    private lateinit var ttsHelper: TtsHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Configure Firebase App Check with launch intent before any UI or ViewModels
        configureAppCheck(this, intent)

        enableEdgeToEdge()

        val database = AppDatabase.getDatabase(this)
        val repository = WordRepository(
            database.wordDao(),
            database.userProgressDao(),
            database.sentenceDao()
        )
        ttsHelper = TtsHelper(this)

        val factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return when {
                    modelClass.isAssignableFrom(MainViewModel::class.java) -> {
                        MainViewModel(repository, ttsHelper) as T
                    }
                    modelClass.isAssignableFrom(GeminiChatViewModel::class.java) -> {
                        GeminiChatViewModel(ttsHelper) as T
                    }
                    else -> throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
                }
            }
        }

        setContent {
            MyApplicationTheme {
                val viewModel: MainViewModel = viewModel(factory = factory)
                val geminiChatViewModel: GeminiChatViewModel = viewModel(factory = factory)
                VocabAppContent(
                    viewModel = viewModel,
                    geminiChatViewModel = geminiChatViewModel
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        configureAppCheck(this, intent)
    }

    override fun onDestroy() {
        super.onDestroy()
        ttsHelper.shutdown()
    }
}

@Composable
fun VocabAppContent(
    viewModel: MainViewModel,
    geminiChatViewModel: GeminiChatViewModel
) {
    val currentScreen by viewModel.currentScreen.collectAsState()
    val userProgress by viewModel.userProgress.collectAsState()
    val selectedLevel by viewModel.selectedLevel.collectAsState()
    val selectedLanguage by viewModel.selectedLanguage.collectAsState()

    // Handle back button on sub-screens
    BackHandler(enabled = currentScreen !is Screen.Home) {
        viewModel.navigateBack()
    }

    val topBarTitle = when (val screen = currentScreen) {
        is Screen.Home -> "Vocab ${selectedLanguage.flag} ${selectedLanguage.code} • ${selectedLevel.code}"
        is Screen.ModeSelection -> "Вікторини ${selectedLanguage.code} • ${selectedLevel.code}"
        is Screen.SentenceBuilder -> "100 речень (${selectedLanguage.code} ${selectedLevel.code})"
        is Screen.Dictionary -> "Словник EN / DE"
        is Screen.Stats -> "Мій прогрес"
        is Screen.GeminiChat -> "Gemini AI-чат"
        is Screen.Quiz -> screen.category?.let { "Тема: $it" } ?: screen.mode.titleUa
    }

    val hideBottomNav = currentScreen is Screen.Quiz || currentScreen is Screen.GeminiChat
    val showGeminiFab = currentScreen is Screen.Home ||
            currentScreen is Screen.ModeSelection ||
            currentScreen is Screen.Dictionary ||
            currentScreen is Screen.Stats

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            AppTopBar(
                title = topBarTitle,
                canNavigateBack = currentScreen !is Screen.Home,
                onNavigateBack = { viewModel.navigateBack() },
                streak = userProgress?.currentStreak ?: 1,
                xp = userProgress?.totalXp ?: 0
            )
        },
        bottomBar = {
            if (!hideBottomNav) {
                AppBottomNav(
                    currentScreen = currentScreen,
                    onTabSelected = { targetScreen ->
                        if (targetScreen is Screen.SentenceBuilder) {
                            viewModel.startSentenceBuilder(0, selectedLevel, selectedLanguage)
                        } else {
                            viewModel.navigateTo(targetScreen)
                        }
                    }
                )
            }
        },
        floatingActionButton = {
            if (showGeminiFab) {
                FloatingActionButton(
                    onClick = { viewModel.navigateTo(Screen.GeminiChat) },
                    containerColor = PrimaryIndigo,
                    contentColor = Color.White,
                    modifier = Modifier.testTag("fab_gemini_chat")
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = "Чат-бот Gemini"
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
            when (val screen = currentScreen) {
                is Screen.Home -> {
                    HomeScreen(
                        viewModel = viewModel,
                        onNavigateToModes = { viewModel.navigateTo(Screen.ModeSelection) }
                    )
                }
                is Screen.ModeSelection -> {
                    ModeSelectionScreen(viewModel = viewModel)
                }
                is Screen.SentenceBuilder -> {
                    SentenceBuilderScreen(
                        viewModel = viewModel,
                        onHome = { viewModel.navigateTo(Screen.Home) }
                    )
                }
                is Screen.Quiz -> {
                    when (screen.mode) {
                        QuizMode.MULTIPLE_CHOICE, QuizMode.LISTENING -> {
                            QuizPlayScreen(
                                viewModel = viewModel,
                                onFinish = { viewModel.navigateTo(Screen.Home) }
                            )
                        }
                        QuizMode.FLASHCARDS -> {
                            FlashcardsScreen(
                                viewModel = viewModel,
                                onFinish = { viewModel.navigateTo(Screen.Home) }
                            )
                        }
                        QuizMode.WORD_BUILDER -> {
                            WordBuilderScreen(
                                viewModel = viewModel,
                                onFinish = { viewModel.navigateTo(Screen.Home) }
                            )
                        }
                        QuizMode.SPEED_MATCH -> {
                            SpeedMatchScreen(
                                viewModel = viewModel,
                                onFinish = { viewModel.navigateTo(Screen.Home) }
                            )
                        }
                    }
                }
                is Screen.Dictionary -> {
                    DictionaryScreen(viewModel = viewModel)
                }
                is Screen.Stats -> {
                    StatsScreen(viewModel = viewModel)
                }
                is Screen.GeminiChat -> {
                    GeminiChatScreen(
                        chatViewModel = geminiChatViewModel,
                        selectedLanguage = selectedLanguage,
                        selectedLevel = selectedLevel,
                        onSelectLanguage = { viewModel.selectLanguage(it) },
                        onSelectLevel = { viewModel.selectLevel(it) }
                    )
                }
            }
        }
    }
}
