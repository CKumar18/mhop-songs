package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.AdminPanelSettings
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.LibraryMusic
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.domain.model.AdminSession
import com.example.domain.model.FontFamilyOption
import com.example.domain.model.Song
import com.example.domain.model.SongLanguage
import com.example.ui.components.EmptyStateView
import com.example.ui.components.MhopBrandMark
import com.example.ui.components.SectionHeaderRow
import com.example.ui.components.SongListRow
import com.example.ui.theme.resolveSongFontFamily
import com.example.ui.viewmodel.LanguageCounts

@Composable
fun HomeScreen(
    languageCounts: LanguageCounts,
    favouriteCount: Int,
    recentlyViewedSongs: List<Song>,
    recentlyAddedSongs: List<Song>,
    adminSession: AdminSession,
    onOpenSearch: () -> Unit,
    onSelectLanguage: (SongLanguage) -> Unit,
    onOpenFavourites: () -> Unit,
    onOpenSong: (String) -> Unit,
    onToggleFavourite: (String) -> Unit,
    onOpenAdminDashboard: () -> Unit,
    onOpenAdminSignIn: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val isCompactWidth = maxWidth < 420.dp

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 760.dp)
                .align(Alignment.TopCenter),
            contentPadding = PaddingValues(bottom = 32.dp),
        ) {
            // Top Brand Header
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(14.dp),
                        ) {
                            MhopBrandMark(size = 48.dp, cornerRadius = 10.dp)
                            Column {
                                Text(
                                    text = "MHOP Songs",
                                    style = MaterialTheme.typography.headlineSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                    ),
                                    color = MaterialTheme.colorScheme.onBackground,
                                )
                                Text(
                                    text = "Church Worship Songbook",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }

                        if (adminSession.isAdmin) {
                            TextButton(
                                onClick = onOpenAdminDashboard,
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .testTag("home_admin_button"),
                            ) {
                                Icon(Icons.Outlined.AdminPanelSettings, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Admin")
                            }
                        }
                    }

                    // Prominent Search Bar Trigger
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = MaterialTheme.shapes.medium,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 54.dp)
                            .clickable(onClick = onOpenSearch)
                            .testTag("home_search_button"),
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Search,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "Search songs by title or lyrics...",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }

            // Multilingual Language Selection
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    SectionHeaderRow(title = "Browse by Language")

                    val languages = listOf(
                        Triple(SongLanguage.TELUGU, languageCounts.telugu, "Telugu"),
                        Triple(SongLanguage.HINDI, languageCounts.hindi, "Hindi"),
                        Triple(SongLanguage.ENGLISH, languageCounts.english, "English"),
                    )

                    if (isCompactWidth) {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            languages.forEach { (lang, count, subtitle) ->
                                LanguageCard(
                                    language = lang,
                                    count = count,
                                    subtitle = subtitle,
                                    onClick = { onSelectLanguage(lang) },
                                    modifier = Modifier.fillMaxWidth(),
                                )
                            }
                        }
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            languages.forEach { (lang, count, subtitle) ->
                                LanguageCard(
                                    language = lang,
                                    count = count,
                                    subtitle = subtitle,
                                    onClick = { onSelectLanguage(lang) },
                                    modifier = Modifier.weight(1f),
                                )
                            }
                        }
                    }
                }
            }

            // Favourite Songs Shortcut
            item {
                Spacer(modifier = Modifier.height(16.dp))
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f),
                    shape = MaterialTheme.shapes.medium,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .heightIn(min = 56.dp)
                        .clickable(onClick = onOpenFavourites)
                        .testTag("home_favourites_shortcut"),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Outlined.FavoriteBorder,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Favourite Songs",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                                Text(
                                    text = if (favouriteCount == 0) "No saved songs yet" else "$favouriteCount saved songs",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                            contentDescription = "Open Favourites",
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            }

            // Empty Library Guidance
            if (languageCounts.total == 0) {
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.6f),
                        modifier = Modifier.padding(horizontal = 20.dp),
                    )
                    EmptyStateView(
                        icon = Icons.Outlined.LibraryMusic,
                        title = "Your Church Songbook Is Ready",
                        subtitle = if (adminSession.isAdmin) {
                            "Import songs directly from Word (.docx) or PDF files, or add your first song manually in the Admin Dashboard."
                        } else {
                            "No songs have been added to this device yet. Administrators can sign in via Settings to add songs or import DOCX/PDF files."
                        },
                        actionLabel = if (adminSession.isAdmin) "Open Admin Dashboard" else "Administrator Sign-In",
                        onActionClick = if (adminSession.isAdmin) onOpenAdminDashboard else onOpenAdminSignIn,
                    )
                }
            } else {
                // Recently Viewed Songs
                if (recentlyViewedSongs.isNotEmpty()) {
                    item {
                        Spacer(modifier = Modifier.height(22.dp))
                        SectionHeaderRow(
                            title = "Recently Viewed",
                            modifier = Modifier.padding(horizontal = 20.dp),
                        )
                    }
                    items(recentlyViewedSongs, key = { "recent_viewed_${it.id}" }) { song ->
                        SongListRow(
                            song = song,
                            onClick = { onOpenSong(song.id) },
                            onToggleFavourite = { onToggleFavourite(song.id) },
                        )
                    }
                }

                // Recently Added Songs
                if (recentlyAddedSongs.isNotEmpty()) {
                    item {
                        Spacer(modifier = Modifier.height(22.dp))
                        SectionHeaderRow(
                            title = "Recently Added",
                            actionLabel = "View All",
                            onActionClick = onOpenSearch,
                            modifier = Modifier.padding(horizontal = 20.dp),
                        )
                    }
                    items(recentlyAddedSongs, key = { "recent_added_${it.id}" }) { song ->
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
private fun LanguageCard(
    language: SongLanguage,
    count: Int,
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scriptFont = resolveSongFontFamily(FontFamilyOption.NOTO_SANS, language)
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = MaterialTheme.shapes.medium,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = modifier
            .heightIn(min = 76.dp)
            .clickable(onClick = onClick)
            .testTag("language_card_${language.code}"),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = language.nativeLabel,
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontFamily = scriptFont,
                        fontWeight = FontWeight.SemiBold,
                    ),
                    color = MaterialTheme.colorScheme.primary,
                )
                val countLabel = when (count) {
                    0 -> "$subtitle  |  Empty"
                    1 -> "$subtitle  |  1 song"
                    else -> "$subtitle  |  $count songs"
                }
                Text(
                    text = countLabel,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                contentDescription = "Open $subtitle songs",
                tint = MaterialTheme.colorScheme.primary,
            )
        }
    }
}
