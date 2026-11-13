package com.example.ui.screens

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.WordCard
import com.example.ui.theme.AccentGold
import com.example.ui.theme.PrimaryIndigo
import com.example.viewmodel.LearningLanguage
import com.example.viewmodel.MainViewModel

@Composable
fun DictionaryScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val allWords by viewModel.allWords.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var selectedLanguageFilter by remember { mutableStateOf<String?>(null) }
    var selectedLevelFilter by remember { mutableStateOf<String?>(null) }
    var selectedCategory by remember { mutableStateOf<String?>(null) }
    var onlyBookmarks by remember { mutableStateOf(false) }

    val categories = remember(allWords, selectedLanguageFilter) {
        allWords.filter { selectedLanguageFilter == null || it.language.equals(selectedLanguageFilter, ignoreCase = true) }
            .map { it.category }
            .distinct()
            .sorted()
    }

    val filteredWords = allWords.filter { word ->
        val matchesQuery = searchQuery.isBlank() ||
                word.english.contains(searchQuery, ignoreCase = true) ||
                word.ukrainian.contains(searchQuery, ignoreCase = true)
        val matchesLanguage = selectedLanguageFilter == null || word.language.equals(selectedLanguageFilter, ignoreCase = true)
        val matchesLevel = selectedLevelFilter == null || word.level.equals(selectedLevelFilter, ignoreCase = true)
        val matchesCategory = selectedCategory == null || word.category == selectedCategory
        val matchesBookmarks = !onlyBookmarks || word.isBookmarked

        matchesQuery && matchesLanguage && matchesLevel && matchesCategory && matchesBookmarks
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("dictionary_screen"),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Search bar
        item {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("dictionary_search_input"),
                placeholder = { Text("Пошук слова (англ., нім. або укр.)...") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Пошук",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "Очистити",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = PrimaryIndigo,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface
                )
            )
        }

        // Language, level and category filter chips row
        item {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                // Language filter chips
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(vertical = 2.dp)
                ) {
                    val languages = listOf(null to "Всі мови", "EN" to "🇬🇧 Англійська", "DE" to "🇩🇪 Німецька")
                    items(languages) { (code, label) ->
                        val isSelected = selectedLanguageFilter == code
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                selectedLanguageFilter = code
                                selectedCategory = null
                            },
                            label = {
                                Text(
                                    text = label,
                                    fontSize = 12.sp,
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

                // CEFR Level filter chips
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(vertical = 2.dp)
                ) {
                    val levels = listOf(null to "Всі рівні", "A1" to "A1 Початковий", "A2" to "A2 Базовий", "B1" to "B1 Середній")
                    items(levels) { (code, label) ->
                        val isSelected = selectedLevelFilter == code
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedLevelFilter = code },
                            label = {
                                Text(
                                    text = label,
                                    fontSize = 12.sp,
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

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(vertical = 2.dp)
                ) {
                    // Bookmark toggle chip
                    item {
                        FilterChip(
                            selected = onlyBookmarks,
                            onClick = { onlyBookmarks = !onlyBookmarks },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Star,
                                    contentDescription = null,
                                    tint = if (onlyBookmarks) Color.White else AccentGold,
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            label = { Text("Збережені ⭐") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = AccentGold,
                                selectedLabelColor = Color.White
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.testTag("filter_bookmarks_chip")
                        )
                    }

                    // All categories
                    item {
                        val isAllSelected = selectedCategory == null
                        FilterChip(
                            selected = isAllSelected,
                            onClick = { selectedCategory = null },
                            label = { Text("Всі теми") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = PrimaryIndigo,
                                selectedLabelColor = Color.White
                            ),
                            shape = RoundedCornerShape(12.dp)
                        )
                    }

                    items(categories) { cat ->
                        val isSelected = selectedCategory == cat
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedCategory = if (isSelected) null else cat },
                            label = { Text(cat) },
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
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Знайдено: ${filteredWords.size} слів",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Натисніть на слово для прикладу",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                    )
                }
            }
        }

        // Empty state
        if (filteredWords.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Слів не знайдено 🔍",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Спробуй змінити параметри пошуку або скинути фільтри",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            items(filteredWords, key = { it.id }) { word ->
                WordCard(
                    word = word,
                    onSpeak = { text, langCode ->
                        val lang = if (langCode.equals("DE", ignoreCase = true)) LearningLanguage.DE else LearningLanguage.EN
                        viewModel.speak(text, lang)
                    },
                    onToggleBookmark = { id, isBookmarked -> viewModel.toggleBookmark(id, isBookmarked) }
                )
            }
        }
    }
}
