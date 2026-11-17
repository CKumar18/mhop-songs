package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Sort
import androidx.compose.material.icons.outlined.Clear
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.SearchOff
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.domain.model.Song
import com.example.domain.model.SongLanguage
import com.example.domain.model.SortOrder
import com.example.ui.components.EmptyStateView
import com.example.ui.components.SongListRow
import com.example.ui.viewmodel.SearchFilterState

@Composable
fun SearchScreen(
    songs: List<Song>,
    filterState: SearchFilterState,
    categories: List<String>,
    onQueryChange: (String) -> Unit,
    onSelectLanguage: (SongLanguage?) -> Unit,
    onSelectCategory: (String?) -> Unit,
    onSortOrderChange: (SortOrder) -> Unit,
    onClearFilters: () -> Unit,
    onOpenSong: (String) -> Unit,
    onToggleFavourite: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var sortMenuExpanded by remember { mutableStateOf(false) }
    var categoryMenuExpanded by remember { mutableStateOf(false) }

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 760.dp)
                .align(Alignment.TopCenter),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = "Song Library",
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onBackground,
                )

                OutlinedTextField(
                    value = filterState.query,
                    onValueChange = onQueryChange,
                    placeholder = { Text("Search title, lyrics, category, or tags") },
                    leadingIcon = {
                        Icon(Icons.Outlined.Search, contentDescription = null)
                    },
                    trailingIcon = {
                        if (filterState.query.isNotEmpty()) {
                            IconButton(
                                onClick = { onQueryChange("") },
                                modifier = Modifier.minimumInteractiveComponentSize(),
                            ) {
                                Icon(Icons.Outlined.Clear, contentDescription = "Clear search")
                            }
                        }
                    },
                    singleLine = true,
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("search_input"),
                )

                // Clean Language Tabs (no badge clutter)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    LanguageTabItem(
                        label = "All",
                        selected = filterState.selectedLanguage == null,
                        onClick = { onSelectLanguage(null) },
                        testTag = "filter_lang_all",
                    )
                    SongLanguage.entries.forEach { lang ->
                        LanguageTabItem(
                            label = lang.nativeLabel,
                            selected = filterState.selectedLanguage == lang,
                            onClick = { onSelectLanguage(lang) },
                            testTag = "filter_lang_${lang.code}",
                        )
                    }
                }

                // Sort and Category controls
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = if (songs.size == 1) "1 song" else "${songs.size} songs",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        if (categories.isNotEmpty()) {
                            Box {
                                TextButton(
                                    onClick = { categoryMenuExpanded = true },
                                    modifier = Modifier.minimumInteractiveComponentSize(),
                                ) {
                                    Text(filterState.selectedCategory ?: "All Categories")
                                }
                                DropdownMenu(
                                    expanded = categoryMenuExpanded,
                                    onDismissRequest = { categoryMenuExpanded = false },
                                ) {
                                    DropdownMenuItem(
                                        text = { Text("All Categories") },
                                        onClick = {
                                            onSelectCategory(null)
                                            categoryMenuExpanded = false
                                        },
                                    )
                                    categories.forEach { cat ->
                                        DropdownMenuItem(
                                            text = { Text(cat) },
                                            onClick = {
                                                onSelectCategory(cat)
                                                categoryMenuExpanded = false
                                            },
                                        )
                                    }
                                }
                            }
                        }

                        Box {
                            TextButton(
                                onClick = { sortMenuExpanded = true },
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .testTag("sort_order_button"),
                            ) {
                                Icon(Icons.AutoMirrored.Outlined.Sort, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(filterState.sortOrder.label)
                            }
                            DropdownMenu(
                                expanded = sortMenuExpanded,
                                onDismissRequest = { sortMenuExpanded = false },
                            ) {
                                SortOrder.entries.forEach { order ->
                                    DropdownMenuItem(
                                        text = { Text(order.label) },
                                        onClick = {
                                            onSortOrderChange(order)
                                            sortMenuExpanded = false
                                        },
                                    )
                                }
                            }
                        }
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outline)

            if (songs.isEmpty()) {
                val langName = filterState.selectedLanguage?.englishName
                val title = when {
                    filterState.query.isNotBlank() -> "No Matching Songs"
                    langName != null -> "No $langName Songs Yet"
                    else -> "Song Library Is Empty"
                }
                val subtitle = when {
                    filterState.query.isNotBlank() ->
                        "Try another title, lyric phrase, or clear your active filters."
                    langName != null ->
                        "No published songs in $langName are currently in the library."
                    else ->
                        "Published songs in Telugu, Hindi, and English will appear here."
                }
                EmptyStateView(
                    icon = Icons.Outlined.SearchOff,
                    title = title,
                    subtitle = subtitle,
                    actionLabel = if (filterState.query.isNotBlank() ||
                        filterState.selectedLanguage != null ||
                        filterState.selectedCategory != null
                    ) {
                        "Clear Filters"
                    } else {
                        null
                    },
                    onActionClick = onClearFilters,
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 24.dp),
                ) {
                    items(songs, key = { it.id }) { song ->
                        SongListRow(
                            song = song,
                            onClick = { onOpenSong(song.id) },
                            onToggleFavourite = { onToggleFavourite(song.id) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LanguageTabItem(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    testTag: String,
) {
    Surface(
        color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
        contentColor = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
        shape = MaterialTheme.shapes.small,
        border = BorderStroke(
            width = 1.dp,
            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
        ),
        modifier = Modifier
            .heightIn(min = 48.dp)
            .clickable(onClick = onClick)
            .testTag(testTag),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                ),
            )
        }
    }
}
