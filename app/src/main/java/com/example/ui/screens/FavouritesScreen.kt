package com.example.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.domain.model.Song
import com.example.ui.components.EmptyStateView
import com.example.ui.components.SongListRow

@Composable
fun FavouritesScreen(
    favouriteSongs: List<Song>,
    recentlyViewedSongs: List<Song>,
    onOpenSong: (String) -> Unit,
    onToggleFavourite: (String) -> Unit,
    onBrowseSongs: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var selectedTab by remember { mutableIntStateOf(0) }

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 760.dp)
                .align(Alignment.TopCenter),
        ) {
            Text(
                text = "Saved & Recent",
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            )

            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.surface,
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Favourites (${favouriteSongs.size})") },
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Recently Viewed (${recentlyViewedSongs.size})") },
                )
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outline)

            val listToShow = if (selectedTab == 0) favouriteSongs else recentlyViewedSongs
            if (listToShow.isEmpty()) {
                EmptyStateView(
                    icon = Icons.Outlined.FavoriteBorder,
                    title = if (selectedTab == 0) "No Favourite Songs Yet" else "No Recently Viewed Songs",
                    subtitle = if (selectedTab == 0) {
                        "Tap the heart icon on any song to keep it ready for quick access during worship."
                    } else {
                        "Songs you open in the reader will appear here for quick return."
                    },
                    actionLabel = "Browse Songs",
                    onActionClick = onBrowseSongs,
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 24.dp),
                ) {
                    item { Spacer(modifier = Modifier.height(4.dp)) }
                    items(listToShow, key = { "${selectedTab}_${it.id}" }) { song ->
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
