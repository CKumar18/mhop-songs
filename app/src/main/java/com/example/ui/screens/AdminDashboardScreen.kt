package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.FileUpload
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material.icons.outlined.PublicOff
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.domain.model.AdminSession
import com.example.domain.model.ImportHistoryRecord
import com.example.domain.model.Song
import com.example.domain.model.SongLanguage
import com.example.ui.components.EmptyStateView
import com.example.ui.viewmodel.AdminViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private enum class AdminSongFilter(val label: String) {
    ALL("All"),
    PUBLISHED("Published"),
    DRAFTS("Drafts"),
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminDashboardScreen(
    adminSession: AdminSession,
    viewModel: AdminViewModel,
    onBack: () -> Unit,
    onAddSong: () -> Unit,
    onEditSong: (String) -> Unit,
    onPreviewSong: (String) -> Unit,
    onOpenDocumentImport: () -> Unit,
) {
    val allSongs by viewModel.allSongs.collectAsStateWithLifecycle()
    val importHistory by viewModel.importHistory.collectAsStateWithLifecycle()
    val adminMessage by viewModel.adminMessage.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(adminMessage) {
        if (adminMessage != null) {
            snackbarHostState.showSnackbar(adminMessage!!)
            viewModel.clearMessage()
        }
    }

    var selectedTab by remember { mutableIntStateOf(0) }
    var searchQuery by remember { mutableStateOf("") }
    var statusFilter by remember { mutableStateOf(AdminSongFilter.ALL) }
    var languageFilter by remember { mutableStateOf<SongLanguage?>(null) }
    var pendingDeleteSong by remember { mutableStateOf<Song?>(null) }

    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Admin Dashboard") },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.minimumInteractiveComponentSize(),
                    ) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { innerPadding ->
        if (!adminSession.isAdmin) {
            EmptyStateView(
                icon = Icons.Outlined.Lock,
                title = "Administrator Access Required",
                subtitle = "Sign in as an authorized administrator in Settings to manage songs or import documents.",
                actionLabel = "Go Back",
                onActionClick = onBack,
                modifier = Modifier.padding(innerPadding),
            )
            return@Scaffold
        }

        val teluguCount = allSongs.count { it.language == SongLanguage.TELUGU }
        val hindiCount = allSongs.count { it.language == SongLanguage.HINDI }
        val englishCount = allSongs.count { it.language == SongLanguage.ENGLISH }
        val draftCount = allSongs.count { !it.isPublished }
        val hasLocalTestSamples = allSongs.any { it.isLocalTestSample }

        val filteredSongs = allSongs.filter { song ->
            val matchesStatus = when (statusFilter) {
                AdminSongFilter.ALL -> true
                AdminSongFilter.PUBLISHED -> song.isPublished
                AdminSongFilter.DRAFTS -> !song.isPublished
            }
            val matchesLang = languageFilter == null || song.language == languageFilter
            val q = searchQuery.trim()
            val matchesQuery = q.isEmpty() ||
                song.title.contains(q, ignoreCase = true) ||
                song.alternateTitle.contains(q, ignoreCase = true) ||
                song.lyrics.contains(q, ignoreCase = true) ||
                song.category.contains(q, ignoreCase = true)
            matchesStatus && matchesLang && matchesQuery
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 820.dp)
                    .align(Alignment.TopCenter),
                contentPadding = PaddingValues(bottom = 32.dp),
            ) {
                // Summary Metrics Grid
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Text(
                            text = "Signed in: ${adminSession.displayName ?: "Local Admin"} (Local Device Storage)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            StatTile(label = "Total", value = allSongs.size.toString(), modifier = Modifier.weight(1f))
                            StatTile(label = "తెలుగు", value = teluguCount.toString(), modifier = Modifier.weight(1f))
                            StatTile(label = "हिन्दी", value = hindiCount.toString(), modifier = Modifier.weight(1f))
                            StatTile(label = "English", value = englishCount.toString(), modifier = Modifier.weight(1f))
                        }

                        // Quick Actions
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Button(
                                onClick = onAddSong,
                                modifier = Modifier
                                    .weight(1f)
                                    .heightIn(min = 48.dp)
                                    .testTag("admin_add_song_button"),
                            ) {
                                Icon(Icons.Outlined.Add, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Add Song")
                            }
                            Button(
                                onClick = onOpenDocumentImport,
                                modifier = Modifier
                                    .weight(1f)
                                    .heightIn(min = 48.dp)
                                    .testTag("admin_import_docs_button"),
                            ) {
                                Icon(Icons.Outlined.FileUpload, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Import DOCX / PDF")
                            }
                        }

                        // Optional clearly labeled Local Test Data utility
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            OutlinedButton(
                                onClick = { viewModel.loadLocalTestSongs() },
                                modifier = Modifier
                                    .weight(1f)
                                    .heightIn(min = 48.dp)
                                    .testTag("admin_seed_test_songs_button"),
                            ) {
                                Text("Load 3 Local Test Songs")
                            }
                            if (hasLocalTestSamples) {
                                OutlinedButton(
                                    onClick = { viewModel.clearLocalTestSongs() },
                                    modifier = Modifier
                                        .weight(1f)
                                        .heightIn(min = 48.dp)
                                        .testTag("admin_clear_test_songs_button"),
                                ) {
                                    Text("Clear Test Songs")
                                }
                            }
                        }
                    }
                }

                // Tabs
                item {
                    TabRow(
                        selectedTabIndex = selectedTab,
                        containerColor = MaterialTheme.colorScheme.surface,
                    ) {
                        Tab(
                            selected = selectedTab == 0,
                            onClick = { selectedTab = 0 },
                            text = { Text("Songs (${allSongs.size} | $draftCount drafts)") },
                        )
                        Tab(
                            selected = selectedTab == 1,
                            onClick = { selectedTab = 1 },
                            text = { Text("Import History (${importHistory.size})") },
                        )
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline)
                }

                if (selectedTab == 0) {
                    // Search & Filters inside Admin Library
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            OutlinedTextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                placeholder = { Text("Search admin library...") },
                                leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("admin_search_input"),
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                AdminSongFilter.entries.forEach { filter ->
                                    val selected = statusFilter == filter
                                    Surface(
                                        color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                                        contentColor = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                                        shape = MaterialTheme.shapes.small,
                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                                        modifier = Modifier
                                            .heightIn(min = 42.dp)
                                            .clickable { statusFilter = filter },
                                    ) {
                                        Box(
                                            contentAlignment = Alignment.Center,
                                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                                        ) {
                                            Text(filter.label, style = MaterialTheme.typography.labelMedium)
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.width(6.dp))
                                SongLanguage.entries.forEach { lang ->
                                    val selected = languageFilter == lang
                                    Surface(
                                        color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                                        contentColor = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                        shape = MaterialTheme.shapes.small,
                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                                        modifier = Modifier
                                            .heightIn(min = 42.dp)
                                            .clickable {
                                                languageFilter = if (selected) null else lang
                                            },
                                    ) {
                                        Box(
                                            contentAlignment = Alignment.Center,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                        ) {
                                            Text(lang.englishName, style = MaterialTheme.typography.labelMedium)
                                        }
                                    }
                                }
                            }
                        }
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline)
                    }

                    if (filteredSongs.isEmpty()) {
                        item {
                            EmptyStateView(
                                icon = Icons.Outlined.FileUpload,
                                title = "No Songs in This View",
                                subtitle = "Add a song manually or import DOCX/PDF files to build your church songbook.",
                            )
                        }
                    } else {
                        items(filteredSongs, key = { it.id }) { song ->
                            AdminSongRow(
                                song = song,
                                onPreview = { onPreviewSong(song.id) },
                                onEdit = { onEditSong(song.id) },
                                onTogglePublish = { viewModel.togglePublish(song.id, song.isPublished) },
                                onDeleteRequest = { pendingDeleteSong = song },
                            )
                        }
                    }
                } else {
                    if (importHistory.isEmpty()) {
                        item {
                            EmptyStateView(
                                icon = Icons.Outlined.History,
                                title = "No Document Imports Yet",
                                subtitle = "Completed DOCX and PDF import batches will be summarized here.",
                                actionLabel = "Import DOCX / PDF",
                                onActionClick = onOpenDocumentImport,
                            )
                        }
                    } else {
                        items(importHistory, key = { it.id }) { record ->
                            ImportHistoryRow(record)
                        }
                    }
                }
            }
        }
    }

    pendingDeleteSong?.let { target ->
        AlertDialog(
            onDismissRequest = { pendingDeleteSong = null },
            title = { Text("Delete Song Permanently?") },
            text = {
                Text("Are you sure you want to permanently delete \"${target.title}\"? This action cannot be undone.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteSong(target.id)
                        pendingDeleteSong = null
                    },
                    modifier = Modifier.testTag("confirm_delete_song_button"),
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDeleteSong = null }) {
                    Text("Cancel")
                }
            },
        )
    }
}

@Composable
private fun StatTile(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = MaterialTheme.shapes.small,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun AdminSongRow(
    song: Song,
    onPreview: () -> Unit,
    onEdit: () -> Unit,
    onTogglePublish: () -> Unit,
    onDeleteRequest: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = song.title,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface,
            )

            val meta = buildList {
                add(song.language.nativeLabel)
                add(if (song.isPublished) "Published" else "Draft")
                if (song.category.isNotBlank()) add(song.category)
                if (song.originalKey.isNotBlank()) add("Key ${song.originalKey}")
                if (song.isLocalTestSample) add("Local Test Data")
            }.joinToString("  |  ")

            Text(
                text = meta,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(
                    onClick = onPreview,
                    modifier = Modifier.minimumInteractiveComponentSize(),
                ) {
                    Icon(Icons.Outlined.Visibility, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Preview")
                }
                TextButton(
                    onClick = onEdit,
                    modifier = Modifier.minimumInteractiveComponentSize(),
                ) {
                    Icon(Icons.Outlined.Edit, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Edit")
                }
                TextButton(
                    onClick = onTogglePublish,
                    modifier = Modifier.minimumInteractiveComponentSize(),
                ) {
                    Icon(
                        if (song.isPublished) Icons.Outlined.PublicOff else Icons.Outlined.Public,
                        contentDescription = null,
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(if (song.isPublished) "Unpublish" else "Publish")
                }
                Spacer(modifier = Modifier.weight(1f))
                IconButton(
                    onClick = onDeleteRequest,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("admin_delete_${song.id}"),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.DeleteOutline,
                        contentDescription = "Delete ${song.title}",
                        tint = MaterialTheme.colorScheme.error,
                    )
                }
            }
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outline)
    }
}

@Composable
private fun ImportHistoryRow(record: ImportHistoryRecord) {
    val formatter = remember { SimpleDateFormat("MMM d, yyyy  HH:mm", Locale.getDefault()) }
    Column(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = record.fileNames.joinToString(", ").ifBlank { "Document Batch" },
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
            )
            Text(
                text = "${formatter.format(Date(record.importedAt))}  |  ${record.adminDisplayName.ifBlank { "Admin" }}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = "Files: ${record.documentsProcessed}  |  Detected: ${record.songsDetected}  |  Imported: ${record.songsImported}  |  Review: ${record.songsRequiringReview}  |  Skipped: ${record.duplicatesSkipped}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
            )
            if (record.errorsSummary.isNotBlank()) {
                Text(
                    text = record.errorsSummary,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outline)
    }
}
