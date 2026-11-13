package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Style
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.theme.AccentEmerald
import com.example.ui.theme.AccentGold
import com.example.ui.theme.AccentRose
import com.example.ui.theme.PrimaryIndigo
import com.example.ui.theme.PrimaryIndigoLight
import com.example.viewmodel.CefrLevel
import com.example.viewmodel.LearningLanguage
import com.example.viewmodel.MainViewModel
import com.example.viewmodel.QuizMode
import com.example.viewmodel.Screen

@Composable
fun HomeScreen(
    viewModel: MainViewModel,
    onNavigateToModes: () -> Unit,
    modifier: Modifier = Modifier
) {
    val allWords by viewModel.allWords.collectAsState()
    val allSentences by viewModel.allSentences.collectAsState()
    val selectedLevel by viewModel.selectedLevel.collectAsState()
    val selectedLanguage by viewModel.selectedLanguage.collectAsState()
    val mistakes by viewModel.mistakeWords.collectAsState()
    val bookmarks by viewModel.bookmarkedWords.collectAsState()
    val userProgress by viewModel.userProgress.collectAsState()

    val levelWords = allWords.filter {
        it.language.equals(selectedLanguage.code, ignoreCase = true) &&
                it.level.equals(selectedLevel.code, ignoreCase = true)
    }
    val totalWords = levelWords.size.coerceAtLeast(1)
    val practicedWords = levelWords.count { it.masteryLevel >= 1 || it.correctAttempts > 0 }
    val masteredWords = levelWords.count { it.masteryLevel >= 3 }
    val levelSentences = allSentences.filter {
        it.language.equals(selectedLanguage.code, ignoreCase = true) &&
                it.level.equals(selectedLevel.code, ignoreCase = true)
    }
    val completedSentences = levelSentences.count { it.isCompleted }
    val langMistakes = mistakes.filter { it.language.equals(selectedLanguage.code, ignoreCase = true) }
    val langBookmarks = bookmarks.filter { it.language.equals(selectedLanguage.code, ignoreCase = true) }
    val progressPercent = (practicedWords.toFloat() / totalWords.toFloat()).coerceIn(0f, 1f)

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("home_screen"),
        contentPadding = PaddingValues(bottom = 96.dp)
    ) {
        // CEFR Level Selector Row (A1, A2, B1)
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 4.dp),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    // Language Switcher Row (EN / DE)
                    Text(
                        text = "Мова вивчення:",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        LearningLanguage.values().forEach { lang ->
                            val isLangSelected = selectedLanguage == lang
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (isLangSelected) PrimaryIndigo else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("language_selector_${lang.code.lowercase()}")
                                    .clickable { viewModel.selectLanguage(lang) }
                            ) {
                                Row(
                                    modifier = Modifier.padding(vertical = 9.dp, horizontal = 8.dp),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "${lang.flag} ${lang.titleUa}",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isLangSelected) Color.White else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Оберіть рівень навчання:",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = selectedLevel.descriptionUa,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = PrimaryIndigo
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CefrLevel.values().forEach { lvl ->
                            val isSelected = selectedLevel == lvl
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (isSelected) PrimaryIndigo else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { viewModel.selectLevel(lvl) }
                            ) {
                                Column(
                                    modifier = Modifier.padding(vertical = 10.dp, horizontal = 4.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = lvl.code,
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = lvl.badge,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = if (isSelected) Color.White.copy(alpha = 0.9f) else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Hero Banner
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                shape = RoundedCornerShape(24.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.vocab_banner_1794152095087),
                        contentDescription = "${selectedLanguage.titleUa} Банер",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color.Transparent,
                                        Color.Black.copy(alpha = 0.75f)
                                    )
                                )
                            )
                    )
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(16.dp)
                    ) {
                        Surface(
                            color = AccentGold,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = "${selectedLanguage.flag} ${selectedLanguage.titleUa.uppercase()} • ${selectedLevel.code}",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.Black,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Прокачай свій словниковий запас!",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "${selectedLevel.titleUa}: ${levelWords.size} слів • 100 речень • вимова",
                            fontSize = 12.sp,
                            color = Color.White.copy(alpha = 0.85f)
                        )
                    }
                }
            }
        }

        // Progress Overview Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Прогрес (${selectedLanguage.code} ${selectedLevel.code})",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "$practicedWords з $totalWords слів опрацьовано",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Bolt,
                                    contentDescription = null,
                                    tint = PrimaryIndigoLight,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(2.dp))
                                Text(
                                    text = "${userProgress?.totalXp ?: 0} XP",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = AccentEmerald,
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(2.dp))
                                Text(
                                    text = "$completedSentences/${levelSentences.size.coerceAtLeast(100)} речень",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Text(
                                text = "• $masteredWords вивчено",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Circular Progress
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.size(64.dp)
                    ) {
                        CircularProgressIndicator(
                            progress = { progressPercent },
                            modifier = Modifier.fillMaxSize(),
                            strokeWidth = 6.dp,
                            color = PrimaryIndigo,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                        Text(
                            text = "${(progressPercent * 100).toInt()}%",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }

        // Quick Training Action Buttons
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
            ) {
                Button(
                    onClick = {
                        viewModel.startQuizSession(
                            mode = QuizMode.MULTIPLE_CHOICE,
                            level = selectedLevel,
                            language = selectedLanguage
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .testTag("quick_start_quiz_button"),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PrimaryIndigo
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Вікторина ${selectedLanguage.code} ${selectedLevel.code} (100 питань • 5 варіантів)",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Dedicated Sentence Builder Card (100 sentences per level)
                Card(
                    onClick = { viewModel.startSentenceBuilder(0, selectedLevel, selectedLanguage) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("sentence_builder_entry_card"),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = AccentGold.copy(alpha = 0.15f)
                    ),
                    border = androidx.compose.foundation.BorderStroke(1.5.dp, AccentGold.copy(alpha = 0.5f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(AccentGold),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = androidx.compose.material.icons.Icons.Default.School,
                                contentDescription = null,
                                tint = Color.Black,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Складання 100 речень (${selectedLanguage.code} ${selectedLevel.code}) • $completedSentences/100",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Тренуй порядок слів у 100 ${selectedLanguage.adjPluralUa} реченнях ${selectedLevel.code}",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = "Перейти",
                            tint = AccentGold,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Gemini AI Tutor Entry Card
                Card(
                    onClick = { viewModel.navigateTo(Screen.GeminiChat) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("home_gemini_chat_card"),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = PrimaryIndigo.copy(alpha = 0.12f)
                    ),
                    border = androidx.compose.foundation.BorderStroke(1.5.dp, PrimaryIndigo.copy(alpha = 0.45f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    Brush.linearGradient(
                                        colors = listOf(PrimaryIndigo, PrimaryIndigoLight)
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = "Gemini",
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Чат-бот Gemini (AI-репетитор)",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Пояснення граматики, перевірка речень та живі діалоги ${selectedLanguage.adjectiveUa}",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = "Відкрити чат",
                            tint = PrimaryIndigo,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Mistakes & Bookmarks row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Mistakes Review Button
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .height(60.dp)
                            .testTag("review_mistakes_card")
                            .clickable(enabled = langMistakes.isNotEmpty()) {
                                viewModel.startQuizSession(
                                    mode = QuizMode.MULTIPLE_CHOICE,
                                    onlyMistakes = true,
                                    level = selectedLevel,
                                    language = selectedLanguage
                                )
                            },
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (langMistakes.isNotEmpty()) AccentRose.copy(alpha = 0.12f)
                            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.ErrorOutline,
                                contentDescription = null,
                                tint = if (langMistakes.isNotEmpty()) AccentRose else Color.Gray,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "Помилки",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (langMistakes.isNotEmpty()) AccentRose else Color.Gray
                                )
                                Text(
                                    text = if (langMistakes.isNotEmpty()) "${langMistakes.size} слів" else "Немає",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    // Bookmarked Words Button
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .height(60.dp)
                            .testTag("review_bookmarks_card")
                            .clickable(enabled = langBookmarks.isNotEmpty()) {
                                viewModel.startQuizSession(
                                    mode = QuizMode.FLASHCARDS,
                                    onlyBookmarks = true,
                                    level = selectedLevel,
                                    language = selectedLanguage
                                )
                            },
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (langBookmarks.isNotEmpty()) AccentGold.copy(alpha = 0.15f)
                            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Bookmark,
                                contentDescription = null,
                                tint = if (langBookmarks.isNotEmpty()) AccentGold else Color.Gray,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "Збережені",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (langBookmarks.isNotEmpty()) AccentGold else Color.Gray
                                )
                                Text(
                                    text = if (langBookmarks.isNotEmpty()) "${langBookmarks.size} слів" else "0 слів",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }

        // Training Modes Carousel/Row
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp, bottom = 8.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Режими навчання",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "Всі режими",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = PrimaryIndigo,
                        modifier = Modifier.clickable { onNavigateToModes() }
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    QuickModeTile(
                        title = "Флеш-картки",
                        icon = Icons.Default.Style,
                        color = AccentGold,
                        onClick = { viewModel.startQuizSession(QuizMode.FLASHCARDS, level = selectedLevel, language = selectedLanguage) },
                        modifier = Modifier.weight(1f)
                    )
                    QuickModeTile(
                        title = "Конструктор",
                        icon = Icons.Default.Psychology,
                        color = PrimaryIndigoLight,
                        onClick = { viewModel.startQuizSession(QuizMode.WORD_BUILDER, level = selectedLevel, language = selectedLanguage) },
                        modifier = Modifier.weight(1f)
                    )
                    QuickModeTile(
                        title = "Бліц-раунд",
                        icon = Icons.Default.Speed,
                        color = AccentEmerald,
                        onClick = { viewModel.startQuizSession(QuizMode.SPEED_MATCH, level = selectedLevel, language = selectedLanguage) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // Categories Section Header
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 8.dp)
            ) {
                Text(
                    text = "Теми (${selectedLanguage.code} ${selectedLevel.code})",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "Обирай тему для тренування ${selectedLanguage.adjPluralUa} слів ${selectedLevel.code}",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Category Cards List with dynamic counts from selected level dictionary
        val emojiMap = mapOf(
            "Сім'я та люди" to "👨‍👩‍👧",
            "Їжа та напої" to "🍏",
            "Їжа" to "🍽️",
            "Дім та побут" to "🏠",
            "Дім" to "🏠",
            "Одяг та аксесуари" to "👗",
            "Тварини та природа" to "🐾",
            "Тварини" to "🐾",
            "Природа" to "🌿",
            "Кольори та опис" to "🎨",
            "Опис" to "🎨",
            "Дії та дієслова" to "🚶",
            "Дії" to "⚡",
            "Час та числа" to "🕒",
            "Час" to "🕒",
            "Числа" to "🔢",
            "Місто та подорожі" to "🏙️",
            "Місто" to "🏙️",
            "Подорожі" to "✈️",
            "Базові фрази" to "💬",
            "Робота та кар'єра" to "💼",
            "Робота" to "💼",
            "Освіта" to "🎓",
            "Здоров'я та спорт" to "🏥",
            "Здоров'я" to "🏥",
            "Емоції та характер" to "😊",
            "Емоції" to "😊",
            "Характер" to "🌟",
            "Особистість" to "🧠",
            "Мислення" to "💡",
            "Спілкування" to "🗣️",
            "Суспільство" to "🏛️",
            "Економіка" to "📈",
            "Техніка" to "💻",
            "Культура" to "🎭",
            "Покупки" to "🛍️",
            "Події" to "📅",
            "Люди" to "👥",
            "Екологія та побут" to "🌍"
        )

        val categories = levelWords
            .groupBy { it.category }
            .entries
            .sortedByDescending { it.value.size }
            .take(12)
            .map { (name, wordsInCat) ->
                val emoji = emojiMap[name] ?: "📘"
                CategoryMeta(name, emoji, "${wordsInCat.size} слів")
            }

        items(categories.chunked(2).size) { rowIndex ->
            val rowItems = categories.chunked(2)[rowIndex]
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                rowItems.forEach { cat ->
                    CategoryCard(
                        category = cat,
                        onClick = {
                            viewModel.startQuizSession(
                                mode = QuizMode.MULTIPLE_CHOICE,
                                category = cat.name,
                                level = selectedLevel,
                                language = selectedLanguage
                            )
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
                if (rowItems.size == 1) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

data class CategoryMeta(val name: String, val emoji: String, val countLabel: String)

@Composable
fun QuickModeTile(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .height(86.dp)
            .testTag("mode_tile_${title.lowercase()}")
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(10.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = title,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
fun CategoryCard(
    category: CategoryMeta,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .testTag("category_card_${category.name.lowercase()}")
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = category.emoji,
                    fontSize = 20.sp
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    text = category.name,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1
                )
                Text(
                    text = category.countLabel,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
