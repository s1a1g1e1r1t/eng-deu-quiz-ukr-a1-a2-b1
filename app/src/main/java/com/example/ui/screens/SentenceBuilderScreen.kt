package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.AccentEmerald
import com.example.ui.theme.AccentGold
import com.example.ui.theme.AccentRose
import com.example.ui.theme.PrimaryIndigo
import com.example.ui.theme.PrimaryIndigoLight
import com.example.viewmodel.CefrLevel
import com.example.viewmodel.LearningLanguage
import com.example.viewmodel.MainViewModel
import com.example.viewmodel.SentenceChip

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SentenceBuilderScreen(
    viewModel: MainViewModel,
    onHome: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.sentenceBuilderState.collectAsState()
    val selectedLevel by viewModel.selectedLevel.collectAsState()
    val selectedLanguage by viewModel.selectedLanguage.collectAsState()

    androidx.compose.runtime.LaunchedEffect(state) {
        if (state == null) {
            viewModel.startSentenceBuilder(0, selectedLevel, selectedLanguage)
        }
    }

    if (state == null) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            androidx.compose.material3.CircularProgressIndicator(color = PrimaryIndigo)
        }
        return
    }
    val session = state!!

    if (session.isFinished) {
        SentenceBuilderResultView(
            language = session.language,
            levelCode = session.level.code,
            completedCount = session.completedCount,
            totalCount = session.sentences.size,
            score = session.score,
            onRestart = { viewModel.startSentenceBuilder(0, session.level, session.language) },
            onHome = onHome
        )
        return
    }

    val target = session.targetSentence ?: return
    val progress = (session.currentIndex + 1).toFloat() / session.sentences.size.toFloat()

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("sentence_builder_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Language Switcher Tabs (EN / DE)
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                LearningLanguage.values().forEach { lang ->
                    val isSelected = session.language == lang
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSelected) PrimaryIndigo else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier
                            .weight(1f)
                            .clickable {
                                viewModel.selectLanguage(lang)
                                viewModel.startSentenceBuilder(0, session.level, lang)
                            }
                    ) {
                        Row(
                            modifier = Modifier.padding(vertical = 8.dp, horizontal = 6.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${lang.flag} ${lang.titleUa}",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }

        // Level Switcher Tabs
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CefrLevel.values().forEach { lvl ->
                    val isSelected = session.level == lvl
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSelected) PrimaryIndigo else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier
                            .weight(1f)
                            .clickable {
                                viewModel.selectLevel(lvl)
                                viewModel.startSentenceBuilder(0, lvl, session.language)
                            }
                    ) {
                        Column(
                            modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "${lvl.code} (100)",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = lvl.badge,
                                fontSize = 10.sp,
                                color = if (isSelected) Color.White.copy(alpha = 0.85f) else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        // Header navigation bar: index, previous/next, progress
        item {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = { viewModel.jumpToSentence(session.currentIndex - 1) },
                            enabled = session.currentIndex > 0,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Попереднє речення",
                                tint = if (session.currentIndex > 0) PrimaryIndigo else Color.Gray
                            )
                        }

                        Text(
                            text = "Речення ${session.currentIndex + 1} з ${session.sentences.size}",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        IconButton(
                            onClick = { viewModel.jumpToSentence(session.currentIndex + 1) },
                            enabled = session.currentIndex < session.sentences.size - 1,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = "Наступне речення",
                                tint = if (session.currentIndex < session.sentences.size - 1) PrimaryIndigo else Color.Gray
                            )
                        }
                    }

                    Surface(
                        color = AccentEmerald.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = "Засвоєно: ${session.completedCount}/${session.sentences.size}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = AccentEmerald,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = PrimaryIndigo,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
            }
        }

        // Ukrainian Sentence Prompt Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("sentence_prompt_card"),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            color = PrimaryIndigoLight.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = target.category,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = PrimaryIndigo,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }

                        IconButton(
                            onClick = { viewModel.speak(target.english, session.language) },
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f))
                        ) {
                            Icon(
                                imageVector = Icons.Default.VolumeUp,
                                contentDescription = "Прослухати вимову",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "Склади речення ${session.language.adjectiveUa}:",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = target.ukrainian,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        // Assembled Sentence Drop Zone
        item {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Твоє ${session.language.adjSingleUa} речення:",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (session.enteredChips.isNotEmpty() && !session.isChecked) {
                        Text(
                            text = "Очистити",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = AccentRose,
                            modifier = Modifier.clickable { viewModel.clearEnteredSentence() }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                val borderModifier = if (session.isChecked) {
                    Modifier.border(
                        2.dp,
                        if (session.isCorrect) AccentEmerald else AccentRose,
                        RoundedCornerShape(16.dp)
                    )
                } else {
                    Modifier.border(
                        1.dp,
                        MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                        RoundedCornerShape(16.dp)
                    )
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .then(borderModifier)
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                        .padding(14.dp)
                ) {
                    if (session.enteredChips.isEmpty()) {
                        Text(
                            text = "Торкніться слів знизу, щоб зібрати речення...",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            modifier = Modifier.padding(vertical = 12.dp)
                        )
                    } else {
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            session.enteredChips.forEach { chip ->
                                AssembledWordChip(
                                    chip = chip,
                                    isChecked = session.isChecked,
                                    isCorrect = session.isCorrect,
                                    onClick = { viewModel.removeSentenceChip(chip) }
                                )
                            }
                        }
                    }
                }
            }
        }

        // Available Scrambled Words Pool
        item {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Банк слів (натискайте для додавання):",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(10.dp))

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    session.availableChips.forEach { chip ->
                        AvailableWordChip(
                            chip = chip,
                            onClick = { viewModel.onSentenceChipClicked(chip) }
                        )
                    }
                }
            }
        }

        // Check & Feedback & Next
        item {
            Column(modifier = Modifier.fillMaxWidth()) {
                if (!session.isChecked) {
                    Button(
                        onClick = { viewModel.checkSentence() },
                        enabled = session.enteredChips.isNotEmpty(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("sentence_check_button"),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryIndigo)
                    ) {
                        Icon(imageVector = Icons.Default.Check, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Перевірити речення",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                } else {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (session.isCorrect) AccentEmerald.copy(alpha = 0.15f)
                            else AccentRose.copy(alpha = 0.15f)
                        )
                    ) {
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
                                Text(
                                    text = if (session.isCorrect) "Правильно! Бездоганний порядок слів! 👏" else "Зверни увагу на правильний варіант:",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (session.isCorrect) AccentEmerald else AccentRose
                                )
                                IconButton(
                                    onClick = { viewModel.speak(target.english, session.language) },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.VolumeUp,
                                        contentDescription = "Озвучити",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            Text(
                                text = target.english,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = { viewModel.nextSentence() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("sentence_next_button"),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryIndigo)
                    ) {
                        Text(
                            text = if (session.currentIndex + 1 >= session.sentences.size) "Завершити тренажер" else "Наступне речення (${session.currentIndex + 2}/${session.sentences.size})",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AssembledWordChip(
    chip: SentenceChip,
    isChecked: Boolean,
    isCorrect: Boolean,
    onClick: () -> Unit
) {
    val borderColor = when {
        !isChecked -> PrimaryIndigo
        isCorrect -> AccentEmerald
        else -> AccentRose
    }

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier
            .border(1.5.dp, borderColor, RoundedCornerShape(12.dp))
            .clickable(enabled = !isChecked) { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = chip.word,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (!isChecked) {
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    imageVector = Icons.Default.Clear,
                    contentDescription = "Видалити",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}

@Composable
fun AvailableWordChip(
    chip: SentenceChip,
    onClick: () -> Unit
) {
    val alpha = if (chip.isUsed) 0.3f else 1f

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (chip.isUsed) MaterialTheme.colorScheme.surfaceVariant
        else PrimaryIndigo.copy(alpha = 0.15f),
        modifier = Modifier.clickable(enabled = !chip.isUsed) { onClick() }
    ) {
        Text(
            text = chip.word,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (chip.isUsed) Color.Gray else PrimaryIndigo,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp)
        )
    }
}

@Composable
fun SentenceBuilderResultView(
    language: LearningLanguage = LearningLanguage.EN,
    levelCode: String = "A1",
    completedCount: Int,
    totalCount: Int,
    score: Int,
    onRestart: () -> Unit,
    onHome: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp)
            .testTag("sentence_builder_result_view"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(AccentEmerald.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                tint = AccentEmerald,
                modifier = Modifier.size(40.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Тренажер 100 речень (${language.code} • $levelCode) завершено! 🏆",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Ти успішно зібрав $completedCount з $totalCount ${language.adjPluralUa} речень рівня $levelCode!",
            fontSize = 15.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(30.dp))

        Button(
            onClick = onRestart,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .testTag("sentence_restart_button"),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = PrimaryIndigo)
        ) {
            Icon(imageVector = Icons.Default.Replay, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = "Почати спочатку з 1-го речення", fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedButton(
            onClick = onHome,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .testTag("sentence_home_button"),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text(text = "На головну", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}
