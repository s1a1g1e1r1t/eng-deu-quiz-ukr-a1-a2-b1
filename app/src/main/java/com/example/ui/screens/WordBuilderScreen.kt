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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
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
import com.example.viewmodel.LetterChip
import com.example.viewmodel.MainViewModel
import com.example.viewmodel.QuizMode

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun WordBuilderScreen(
    viewModel: MainViewModel,
    onFinish: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.wordBuilderState.collectAsState()

    if (state == null) return
    val builder = state!!

    if (builder.isFinished) {
        WordBuilderResultView(
            score = builder.score,
            totalWords = builder.totalWordsCount,
            onRetry = {
                viewModel.startQuizSession(
                    mode = QuizMode.WORD_BUILDER,
                    level = builder.level,
                    language = builder.language
                )
            },
            onHome = onFinish
        )
        return
    }

    val target = builder.targetWord ?: return
    val progress = (builder.currentWordIndex + 1).toFloat() / builder.totalWordsCount.toFloat().coerceAtLeast(1f)

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("word_builder_screen"),
        contentPadding = PaddingValues(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        // Progress bar
        item {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            color = PrimaryIndigo,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = "${builder.language.flag} ${builder.language.code} • ${builder.level.code}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                        Text(
                            text = "Слово ${builder.currentWordIndex + 1} з ${builder.totalWordsCount}",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        text = "Бали: ${builder.score}",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = AccentGold
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = PrimaryIndigoLight,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
            }
        }

        // Clue Card (Ukrainian translation & category)
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("word_builder_clue_card"),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Surface(
                        color = PrimaryIndigoLight.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = target.category,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = PrimaryIndigo,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "Склади ${builder.language.adjSingleUa} слово для:",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = target.ukrainian,
                        fontSize = 26.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    IconButton(
                        onClick = { viewModel.speak(target.english, builder.language) },
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f))
                    ) {
                        Icon(
                            imageVector = Icons.Default.VolumeUp,
                            contentDescription = "Підказка вимови",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }

        // Entered Letters Slots
        item {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Твоя відповідь (натисни щоб прибрати літеру):",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(10.dp))

                FlowRow(
                    horizontalArrangement = Arrangement.Center,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        .padding(12.dp)
                ) {
                    if (builder.enteredLetters.isEmpty()) {
                        Text(
                            text = "Обирай літери знизу...",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            modifier = Modifier.padding(vertical = 12.dp)
                        )
                    } else {
                        builder.enteredLetters.forEach { chip ->
                            LetterSlotTile(
                                chip = chip,
                                isChecked = builder.isChecked,
                                isCorrect = builder.isCorrect,
                                onClick = { viewModel.removeEnteredLetter(chip) }
                            )
                        }
                    }
                }
            }
        }

        // Available Scrambled Letter Tiles
        item {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Доступні літери:",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(10.dp))

                FlowRow(
                    horizontalArrangement = Arrangement.Center,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    builder.availableLetters.forEach { chip ->
                        AvailableLetterTile(
                            chip = chip,
                            onClick = { viewModel.onLetterChipClicked(chip) }
                        )
                    }
                }
            }
        }

        // Verification & Next Action
        item {
            Column(modifier = Modifier.fillMaxWidth()) {
                if (!builder.isChecked) {
                    Button(
                        onClick = { viewModel.checkWordBuilder() },
                        enabled = builder.enteredLetters.isNotEmpty(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("word_builder_check_button"),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryIndigo)
                    ) {
                        Text(text = "Перевірити слово", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                } else {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (builder.isCorrect) AccentEmerald.copy(alpha = 0.15f)
                            else AccentRose.copy(alpha = 0.15f)
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = if (builder.isCorrect) "Вірно! Бездоганно! 🎯" else "Правильне написання:",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (builder.isCorrect) AccentEmerald else AccentRose
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "${target.english} ${target.transcription}",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = target.exampleEn,
                                fontSize = 12.sp,
                                fontStyle = FontStyle.Italic,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = { viewModel.nextWordBuilder() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("word_builder_next_button"),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryIndigo)
                    ) {
                        Text(
                            text = if (builder.remainingWords.isEmpty()) "Завершити" else "Наступне слово",
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
fun LetterSlotTile(
    chip: LetterChip,
    isChecked: Boolean,
    isCorrect: Boolean,
    onClick: () -> Unit
) {
    val borderColor = when {
        !isChecked -> PrimaryIndigo
        isCorrect -> AccentEmerald
        else -> AccentRose
    }

    Box(
        modifier = Modifier
            .size(46.dp)
            .padding(2.dp)
            .border(2.dp, borderColor, RoundedCornerShape(10.dp))
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.surface)
            .clickable(enabled = !isChecked) { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = chip.char.toString(),
            fontSize = 20.sp,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
fun AvailableLetterTile(
    chip: LetterChip,
    onClick: () -> Unit
) {
    val alpha = if (chip.isUsed) 0.3f else 1f

    Box(
        modifier = Modifier
            .size(46.dp)
            .padding(2.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(
                if (chip.isUsed) MaterialTheme.colorScheme.surfaceVariant
                else PrimaryIndigo.copy(alpha = 0.15f)
            )
            .clickable(enabled = !chip.isUsed) { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = chip.char.toString(),
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = if (chip.isUsed) Color.Gray else PrimaryIndigo
        )
    }
}

@Composable
fun WordBuilderResultView(
    score: Int,
    totalWords: Int,
    onRetry: () -> Unit,
    onHome: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp)
            .testTag("word_builder_result_view"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Чудова робота! 🧠",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = "Ти набрав $score балів у конструкторі слів!",
            fontSize = 15.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(30.dp))

        Button(
            onClick = onRetry,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .testTag("builder_retry_button"),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = PrimaryIndigo)
        ) {
            Icon(imageVector = Icons.Default.Replay, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = "Грати ще раз", fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedButton(
            onClick = onHome,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .testTag("builder_home_button"),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text(text = "На головну", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}
