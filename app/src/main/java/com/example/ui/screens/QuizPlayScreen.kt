package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Hearing
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Star
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
import com.example.viewmodel.LearningLanguage
import com.example.viewmodel.MainViewModel
import com.example.viewmodel.PromptType
import com.example.viewmodel.QuizMode

@Composable
fun QuizPlayScreen(
    viewModel: MainViewModel,
    onFinish: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.quizState.collectAsState()

    if (state == null) return
    val quiz = state!!

    if (quiz.isFinished) {
        QuizResultView(
            correctCount = quiz.correctCount,
            totalCount = quiz.questions.size,
            xpEarned = quiz.sessionXpEarned,
            onRetry = {
                viewModel.startQuizSession(
                    mode = quiz.mode,
                    level = quiz.level,
                    language = quiz.language
                )
            },
            onHome = onFinish
        )
        return
    }

    val currentQuestion = quiz.questions.getOrNull(quiz.currentIndex) ?: return
    val progress = (quiz.currentIndex + 1).toFloat() / quiz.questions.size.toFloat()
    val animatedProgress by animateFloatAsState(targetValue = progress, label = "quiz_progress")

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("quiz_play_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Top Header: Question index & Progress bar
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
                                text = "${quiz.language.flag} ${quiz.language.code} • ${quiz.level.code}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                        Text(
                            text = "Питання ${quiz.currentIndex + 1} з ${quiz.questions.size}",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        text = "Правильно: ${quiz.correctCount}",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = AccentEmerald
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                LinearProgressIndicator(
                    progress = { animatedProgress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = PrimaryIndigo,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
            }
        }

        // Question Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("question_card"),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Category & Part of Speech Tag
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            color = PrimaryIndigoLight.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = currentQuestion.targetWord.category,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = PrimaryIndigo,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }

                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = currentQuestion.targetWord.partOfSpeech,
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    when (currentQuestion.promptType) {
                        PromptType.AUDIO -> {
                            Text(
                                text = "Послухай та обери слово:",
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            IconButton(
                                onClick = { viewModel.speak(currentQuestion.targetWord.english, quiz.language) },
                                modifier = Modifier
                                    .testTag("listening_audio_button")
                                    .size(72.dp)
                                    .clip(CircleShape)
                                    .background(PrimaryIndigo)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Hearing,
                                    contentDescription = "Прослухати знову",
                                    tint = Color.White,
                                    modifier = Modifier.size(36.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Торкніться, щоб повторити",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        PromptType.EN_TO_UA -> {
                            Text(
                                text = "Як перекладається це слово?",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = currentQuestion.targetWord.english,
                                fontSize = 32.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onSurface,
                                textAlign = TextAlign.Center
                            )
                            Text(
                                text = currentQuestion.targetWord.transcription,
                                fontSize = 14.sp,
                                fontStyle = FontStyle.Italic,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            IconButton(
                                onClick = { viewModel.speak(currentQuestion.targetWord.english, quiz.language) },
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f))
                            ) {
                                Icon(
                                    imageVector = Icons.Default.VolumeUp,
                                    contentDescription = "Вимова",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        PromptType.UA_TO_EN -> {
                            val adjMasc = if (quiz.language == LearningLanguage.DE) "німецький" else "англійський"
                            Text(
                                text = "Обери $adjMasc переклад:",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = currentQuestion.targetWord.ukrainian,
                                fontSize = 28.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onSurface,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        }

        // 5 Options (5 buttons)
        items(currentQuestion.options.size) { index ->
            val option = currentQuestion.options[index]
            val isSelected = quiz.selectedAnswer == option
            val isCorrect = option.trim().equals(currentQuestion.correctAnswer.trim(), ignoreCase = true)

            val (backgroundColor, borderColor, textColor) = when {
                !quiz.isAnswered -> {
                    Triple(
                        MaterialTheme.colorScheme.surface,
                        MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                        MaterialTheme.colorScheme.onSurface
                    )
                }
                isCorrect -> {
                    Triple(
                        AccentEmerald.copy(alpha = 0.15f),
                        AccentEmerald,
                        AccentEmerald
                    )
                }
                isSelected && !isCorrect -> {
                    Triple(
                        AccentRose.copy(alpha = 0.15f),
                        AccentRose,
                        AccentRose
                    )
                }
                else -> {
                    Triple(
                        MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
                        Color.Transparent,
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                }
            }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("option_button_$index")
                    .border(1.5.dp, borderColor, RoundedCornerShape(16.dp))
                    .clickable(enabled = !quiz.isAnswered) {
                        viewModel.submitQuizAnswer(option)
                    },
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = backgroundColor)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = option,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = textColor,
                        modifier = Modifier.weight(1f)
                    )

                    if (quiz.isAnswered) {
                        if (isCorrect) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Вірно",
                                tint = AccentEmerald,
                                modifier = Modifier.size(24.dp)
                            )
                        } else if (isSelected) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Невірно",
                                tint = AccentRose,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }
            }
        }

        // Post-Answer Explanation Card & Next Button
        item {
            AnimatedVisibility(visible = quiz.isAnswered) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
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
                                    text = if (quiz.isCorrect) "Чудово! Правильна відповідь 🎉" else "Зверни увагу на переклад:",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (quiz.isCorrect) AccentEmerald else AccentRose
                                )
                                IconButton(
                                    onClick = { viewModel.speak(currentQuestion.targetWord.english, quiz.language) },
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
                                text = "${currentQuestion.targetWord.english} — ${currentQuestion.targetWord.ukrainian}",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = currentQuestion.targetWord.transcription,
                                fontSize = 12.sp,
                                fontStyle = FontStyle.Italic,
                                color = MaterialTheme.colorScheme.primary
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = "Приклад: \"${currentQuestion.targetWord.exampleEn}\"",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "(${currentQuestion.targetWord.exampleUa})",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Button(
                        onClick = { viewModel.nextQuizQuestion() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("quiz_next_button"),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryIndigo)
                    ) {
                        Text(
                            text = if (quiz.currentIndex + 1 >= quiz.questions.size) "Завершити вікторину" else "Наступне слово",
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
fun QuizResultView(
    correctCount: Int,
    totalCount: Int,
    xpEarned: Int,
    onRetry: () -> Unit,
    onHome: () -> Unit,
    modifier: Modifier = Modifier
) {
    val percentage = if (totalCount > 0) (correctCount * 100) / totalCount else 0
    val stars = when {
        percentage >= 90 -> 3
        percentage >= 60 -> 2
        percentage >= 30 -> 1
        else -> 0
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp)
            .testTag("quiz_result_view"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Star rating
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            repeat(3) { index ->
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = null,
                    tint = if (index < stars) AccentGold else MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.size(44.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = when {
                stars == 3 -> "Чудовий результат! 🌟"
                stars == 2 -> "Добре попрацював! 👍"
                else -> "Тренуйся ще, і все вийде! 💪"
            },
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Ти правильно відповів на $correctCount з $totalCount питань ($percentage%)",
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(20.dp))

        // XP Gained Card
        Surface(
            color = AccentGold.copy(alpha = 0.15f),
            shape = RoundedCornerShape(16.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "+$xpEarned XP",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = AccentGold
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "досвіду отримано",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = onRetry,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .testTag("result_retry_button"),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = PrimaryIndigo)
        ) {
            Icon(
                imageVector = Icons.Default.Replay,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Спробувати ще раз",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedButton(
            onClick = onHome,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .testTag("result_home_button"),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text(
                text = "Повернутися на головну",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}
