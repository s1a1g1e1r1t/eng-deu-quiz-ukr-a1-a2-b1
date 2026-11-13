package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import com.example.ui.theme.AccentEmerald
import com.example.ui.theme.AccentGold
import com.example.ui.theme.AccentRose
import com.example.ui.theme.PrimaryIndigo
import com.example.ui.theme.PrimaryIndigoLight
import com.example.viewmodel.CefrLevel
import com.example.viewmodel.LearningLanguage
import com.example.viewmodel.MainViewModel
import com.example.viewmodel.QuizMode

@Composable
fun StatsScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val allWords by viewModel.allWords.collectAsState()
    val allSentences by viewModel.allSentences.collectAsState()
    val selectedGlobalLevel by viewModel.selectedLevel.collectAsState()
    val selectedGlobalLanguage by viewModel.selectedLanguage.collectAsState()
    val userProgress by viewModel.userProgress.collectAsState()
    val mistakes by viewModel.mistakeWords.collectAsState()
    val bookmarks by viewModel.bookmarkedWords.collectAsState()

    var selectedFilterLanguage by remember { mutableStateOf<LearningLanguage?>(selectedGlobalLanguage) }
    var selectedFilterLevel by remember { mutableStateOf<CefrLevel?>(selectedGlobalLevel) }
    var showResetDialog by remember { mutableStateOf(false) }

    val filteredWords = remember(allWords, selectedFilterLanguage, selectedFilterLevel) {
        allWords.filter { word ->
            val matchLang = selectedFilterLanguage == null || word.language.equals(selectedFilterLanguage!!.code, ignoreCase = true)
            val matchLevel = selectedFilterLevel == null || word.level.equals(selectedFilterLevel!!.code, ignoreCase = true)
            matchLang && matchLevel
        }
    }
    val filteredSentences = remember(allSentences, selectedFilterLanguage, selectedFilterLevel) {
        allSentences.filter { sentence ->
            val matchLang = selectedFilterLanguage == null || sentence.language.equals(selectedFilterLanguage!!.code, ignoreCase = true)
            val matchLevel = selectedFilterLevel == null || sentence.level.equals(selectedFilterLevel!!.code, ignoreCase = true)
            matchLang && matchLevel
        }
    }
    val filteredMistakes = remember(mistakes, selectedFilterLanguage) {
        if (selectedFilterLanguage == null) mistakes
        else mistakes.filter { it.language.equals(selectedFilterLanguage!!.code, ignoreCase = true) }
    }
    val filteredBookmarks = remember(bookmarks, selectedFilterLanguage) {
        if (selectedFilterLanguage == null) bookmarks
        else bookmarks.filter { it.language.equals(selectedFilterLanguage!!.code, ignoreCase = true) }
    }

    val totalWords = filteredWords.size.coerceAtLeast(1)
    val masteredCount = filteredWords.count { it.masteryLevel >= 3 }
    val familiarCount = filteredWords.count { it.masteryLevel == 2 }
    val learningCount = filteredWords.count { it.masteryLevel == 1 }
    val newCount = filteredWords.count { it.masteryLevel == 0 }
    val practicedCount = filteredWords.count { it.masteryLevel >= 1 || it.correctAttempts > 0 }

    val progressRatio = (practicedCount.toFloat() / totalWords.toFloat()).coerceIn(0f, 1f)

    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text("Скинути прогрес?") },
            text = { Text("Весь ваш прогрес слів, рівні вивчення та бали XP буде повернено до початкового стану.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.resetProgress()
                        showResetDialog = false
                    }
                ) {
                    Text("Скинути", color = AccentRose, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) {
                    Text("Скасувати")
                }
            }
        )
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("stats_screen"),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Column {
                Text(
                    text = "Твій навчальний прогрес",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                val langLabel = selectedFilterLanguage?.titleUa ?: "EN + DE"
                val levelLabel = selectedFilterLevel?.let { "${it.code} (${it.badge})" } ?: "усіх рівнях (A1–B1)"
                Text(
                    text = "Статистика: $langLabel • $levelLabel",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(10.dp))
                // Language filter chips
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val langOptions: List<Pair<LearningLanguage?, String>> = listOf(
                        null to "Усі мови",
                        LearningLanguage.EN to "🇬🇧 EN",
                        LearningLanguage.DE to "🇩🇪 DE"
                    )
                    langOptions.forEach { (lang, label) ->
                        val isSelected = selectedFilterLanguage == lang
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                selectedFilterLanguage = lang
                                if (lang != null) viewModel.selectLanguage(lang)
                            },
                            label = {
                                Text(
                                    text = label,
                                    fontSize = 13.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = PrimaryIndigo,
                                selectedLabelColor = Color.White
                            ),
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val options: List<Pair<CefrLevel?, String>> = listOf(
                        null to "Усі",
                        CefrLevel.A1 to "A1",
                        CefrLevel.A2 to "A2",
                        CefrLevel.B1 to "B1"
                    )
                    options.forEach { (lvl, label) ->
                        val isSelected = selectedFilterLevel == lvl
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                selectedFilterLevel = lvl
                                if (lvl != null) viewModel.selectLevel(lvl)
                            },
                            label = {
                                Text(
                                    text = label,
                                    fontSize = 13.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = PrimaryIndigo,
                                selectedLabelColor = Color.White
                            ),
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                }
            }
        }

        // Summary Score Tiles
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                StatTile(
                    title = "Дні поспіль",
                    value = "${userProgress?.currentStreak ?: 1}",
                    icon = Icons.Default.LocalFireDepartment,
                    color = AccentGold,
                    modifier = Modifier.weight(1f)
                )
                StatTile(
                    title = "Бали досвіду",
                    value = "${userProgress?.totalXp ?: 0}",
                    icon = Icons.Default.Bolt,
                    color = PrimaryIndigoLight,
                    modifier = Modifier.weight(1f)
                )
                StatTile(
                    title = "Вікторин",
                    value = "${userProgress?.quizzesCompleted ?: 0}",
                    icon = Icons.Default.CheckCircle,
                    color = AccentEmerald,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Mastery breakdown Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            val langPrefix = selectedFilterLanguage?.let { "${it.code} " } ?: ""
                            Text(
                                text = selectedFilterLevel?.let { "Опанування $langPrefix${it.code}" } ?: "Опанування ${langPrefix}усіх рівнів",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "$practicedCount з $totalWords слів опрацьовано",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            text = "${(progressRatio * 100).toInt()}%",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = PrimaryIndigo
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    LinearProgressIndicator(
                        progress = { progressRatio },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(10.dp)
                            .clip(RoundedCornerShape(5.dp)),
                        color = AccentEmerald,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    // 4 Category breakdown rows
                    MasteryRow(label = "Вивчено повністю", count = masteredCount, color = AccentEmerald)
                    Spacer(modifier = Modifier.height(10.dp))
                    MasteryRow(label = "Добре закріплено", count = familiarCount, color = PrimaryIndigoLight)
                    Spacer(modifier = Modifier.height(10.dp))
                    MasteryRow(label = "В процесі вивчення", count = learningCount, color = AccentGold)
                    Spacer(modifier = Modifier.height(10.dp))
                    MasteryRow(label = "Ще не вивчалось", count = newCount, color = Color.Gray)
                }
            }
        }

        // 100 Sentences Progress Card
        item {
            val totalSentences = filteredSentences.size.coerceAtLeast(1)
            val completedSentences = filteredSentences.count { it.isCompleted }
            val sentencesRatio = (completedSentences.toFloat() / totalSentences.toFloat()).coerceIn(0f, 1f)
            val targetLevelForSentence = selectedFilterLevel ?: selectedGlobalLevel
            val targetLangForSentence = selectedFilterLanguage ?: selectedGlobalLanguage

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("stats_sentences_card")
                    .clickable { viewModel.startSentenceBuilder(0, targetLevelForSentence, targetLangForSentence) },
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = AccentGold.copy(alpha = 0.12f)
                ),
                border = androidx.compose.foundation.BorderStroke(1.dp, AccentGold.copy(alpha = 0.4f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = androidx.compose.material.icons.Icons.Default.School,
                                contentDescription = null,
                                tint = AccentGold,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = selectedFilterLevel?.let { "Складання 100 речень (${targetLangForSentence.code} ${it.code})" }
                                    ?: "Складання речень (${selectedFilterLanguage?.code ?: "EN / DE"})",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Text(
                            text = "$completedSentences / $totalSentences",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = AccentGold
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    LinearProgressIndicator(
                        progress = { sentencesRatio },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = AccentGold,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = if (completedSentences >= totalSentences) "Вітаємо! Усі речення опановано! 🏆"
                        else "Натисніть для тренування ${targetLangForSentence.adjPluralUa} речень",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Action Study Cards: Mistakes & Bookmarks
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Індивідуальні тренування",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )

                val targetLangForPractice = selectedFilterLanguage ?: selectedGlobalLanguage
                val targetLevelForPractice = selectedFilterLevel ?: selectedGlobalLevel

                // Mistakes Card
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("stats_mistakes_card")
                        .clickable(enabled = filteredMistakes.isNotEmpty()) {
                            viewModel.startQuizSession(
                                mode = QuizMode.MULTIPLE_CHOICE,
                                onlyMistakes = true,
                                level = targetLevelForPractice,
                                language = targetLangForPractice
                            )
                        },
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (filteredMistakes.isNotEmpty()) AccentRose.copy(alpha = 0.12f)
                        else MaterialTheme.colorScheme.surface
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(AccentRose.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.ErrorOutline,
                                contentDescription = null,
                                tint = AccentRose,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Робота над помилками",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = if (filteredMistakes.isNotEmpty()) "${filteredMistakes.size} слів потребують повторення" else "Всі слова закріплені!",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Bookmarks Card
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("stats_bookmarks_card")
                        .clickable(enabled = filteredBookmarks.isNotEmpty()) {
                            viewModel.startQuizSession(
                                mode = QuizMode.FLASHCARDS,
                                onlyBookmarks = true,
                                level = targetLevelForPractice,
                                language = targetLangForPractice
                            )
                        },
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (filteredBookmarks.isNotEmpty()) AccentGold.copy(alpha = 0.12f)
                        else MaterialTheme.colorScheme.surface
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(AccentGold.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = null,
                                tint = AccentGold,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Збережені картки (⭐)",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "${filteredBookmarks.size} важливих слів у списку",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        // Reset Progress Action
        item {
            Spacer(modifier = Modifier.height(10.dp))
            OutlinedButton(
                onClick = { showResetDialog = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("reset_progress_button"),
                shape = RoundedCornerShape(14.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.DeleteSweep,
                    contentDescription = null,
                    tint = AccentRose,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Скинути весь прогрес",
                    color = AccentRose,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
fun MasteryRow(
    label: String,
    count: Int,
    color: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(color)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = label,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        Text(
            text = "$count слів",
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
fun StatTile(
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
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
                text = value,
                fontSize = 16.sp,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = title,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
