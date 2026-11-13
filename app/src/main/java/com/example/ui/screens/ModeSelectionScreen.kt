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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Hearing
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Style
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import com.example.ui.theme.AccentCyan
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
fun ModeSelectionScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val allWords by viewModel.allWords.collectAsState()
    val selectedLevel by viewModel.selectedLevel.collectAsState()
    val selectedLanguage by viewModel.selectedLanguage.collectAsState()
    var selectedCategory by remember { mutableStateOf<String?>(null) }

    val categories = remember(allWords, selectedLanguage) {
        allWords.filter { it.language.equals(selectedLanguage.code, ignoreCase = true) }
            .map { it.category }
            .distinct()
            .sorted()
    }
    val allCategoryOptions = listOf("Всі теми") + categories

    val modes = listOf(
        ModeItem(
            mode = QuizMode.MULTIPLE_CHOICE,
            title = "Вікторина 100 питань (${selectedLanguage.code} ${selectedLevel.code})",
            subtitle = "Обирай правильний переклад серед 5 варіантів (100 питань з повної бази ${selectedLevel.code})",
            icon = Icons.Default.Quiz,
            badge = "100 питань • 5 кнопок",
            accentColor = PrimaryIndigo
        ),
        ModeItem(
            mode = QuizMode.FLASHCARDS,
            title = "Флеш-картки (${selectedLanguage.code} ${selectedLevel.code})",
            subtitle = "Перевертай картки з транскрипцією та прикладами рівня ${selectedLevel.code}",
            icon = Icons.Default.Style,
            badge = "Для пам'яті",
            accentColor = AccentGold
        ),
        ModeItem(
            mode = QuizMode.WORD_BUILDER,
            title = "Конструктор слова (${selectedLanguage.code} ${selectedLevel.code})",
            subtitle = "Збирай правильне ${selectedLanguage.adjSingleUa} написання слів ${selectedLevel.code} з літер",
            icon = Icons.Default.Psychology,
            badge = "Правопис",
            accentColor = PrimaryIndigoLight
        ),
        ModeItem(
            mode = QuizMode.LISTENING,
            title = "Аудіо-вікторина 100 питань (${selectedLanguage.code} ${selectedLevel.code})",
            subtitle = "Слухай нативну вимову та знаходь переклад (100 питань • 5 варіантів)",
            icon = Icons.Default.Hearing,
            badge = "100 питань • 5 кнопок",
            accentColor = AccentCyan
        ),
        ModeItem(
            mode = QuizMode.SPEED_MATCH,
            title = "Бліц-спринт (30 сек)",
            subtitle = "Швидко відповідай: вірний переклад чи ні (${selectedLanguage.code} ${selectedLevel.code})",
            icon = Icons.Default.Speed,
            badge = "Драйв",
            accentColor = AccentEmerald
        )
    )

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("mode_selection_screen"),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Column {
                Text(
                    text = "Обери формат тренування",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "Різні способи закріплення слів для стійкої пам'яті",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Language Selector Chips
        item {
            Column {
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
                        val isSelected = selectedLanguage == lang
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                viewModel.selectLanguage(lang)
                                selectedCategory = null
                            },
                            label = {
                                Text(
                                    text = "${lang.flag} ${lang.titleUa}",
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

        // CEFR Level Selector Chips
        item {
            Column {
                Text(
                    text = "Оберіть рівень складності:",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CefrLevel.values().forEach { lvl ->
                        val isSelected = selectedLevel == lvl
                        FilterChip(
                            selected = isSelected,
                            onClick = { viewModel.selectLevel(lvl) },
                            label = {
                                Text(
                                    text = "${lvl.code} ${lvl.badge}",
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

        // Category Filter Chips
        item {
            Column {
                Text(
                    text = "Фільтр за темою:",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(6.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(vertical = 4.dp)
                ) {
                    items(allCategoryOptions) { cat ->
                        val isSelected = (cat == "Всі теми" && selectedCategory == null) || (cat == selectedCategory)
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                selectedCategory = if (cat == "Всі теми") null else cat
                            },
                            label = {
                                Text(
                                    text = cat,
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = PrimaryIndigo,
                                selectedLabelColor = Color.White
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.testTag("filter_chip_${cat.lowercase()}")
                        )
                    }
                }
            }
        }

        // Featured 100 Sentences Builder Mode
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("mode_card_sentence_builder")
                    .clickable { viewModel.startSentenceBuilder(0, selectedLevel, selectedLanguage) },
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = AccentGold.copy(alpha = 0.15f)
                ),
                border = androidx.compose.foundation.BorderStroke(1.5.dp, AccentGold.copy(alpha = 0.6f)),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(AccentGold),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Style,
                            contentDescription = "Складання речень",
                            tint = Color.Black,
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "Складання 100 речень (${selectedLanguage.code} ${selectedLevel.code})",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(AccentGold)
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "100 речень",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.Black
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = "Окремий режим: складай 100 ${selectedLanguage.adjPluralUa} речень ${selectedLevel.code} зі слів у правильному порядку",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 16.sp
                        )
                    }

                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = "Почати",
                        tint = AccentGold,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        // Featured Gemini AI Chat Mode
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("mode_card_gemini_chat")
                    .clickable { viewModel.navigateTo(Screen.GeminiChat) },
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = PrimaryIndigo.copy(alpha = 0.12f)
                ),
                border = androidx.compose.foundation.BorderStroke(1.5.dp, PrimaryIndigo.copy(alpha = 0.45f)),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(PrimaryIndigo),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = "Чат-бот Gemini",
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "Чат-бот Gemini",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(PrimaryIndigo)
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "AI Репетитор",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = "Спілкуйся ${selectedLanguage.adjectiveUa}, став запитання з граматики рівня ${selectedLevel.code} та перевіряй речення",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 16.sp
                        )
                    }

                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = "Відкрити",
                        tint = PrimaryIndigo,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        // Modes list
        items(modes) { item ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("mode_card_${item.mode.name.lowercase()}")
                    .clickable {
                        viewModel.startQuizSession(
                            mode = item.mode,
                            category = selectedCategory,
                            level = selectedLevel,
                            language = selectedLanguage
                        )
                    },
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
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(item.accentColor.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = item.icon,
                            contentDescription = item.title,
                            tint = item.accentColor,
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = item.title,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(item.accentColor.copy(alpha = 0.15f))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = item.badge,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = item.accentColor
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = item.subtitle,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 16.sp
                        )
                    }

                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = "Почати",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

data class ModeItem(
    val mode: QuizMode,
    val title: String,
    val subtitle: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val badge: String,
    val accentColor: Color
)
