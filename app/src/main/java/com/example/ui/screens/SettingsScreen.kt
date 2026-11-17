package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.outlined.AdminPanelSettings
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.TextFields
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.example.domain.model.AdminSession
import com.example.domain.model.AppThemeMode
import com.example.domain.model.ReadingPreferences
import com.example.domain.model.SyncStatus
import com.example.ui.components.MhopBrandMark
import com.example.ui.components.ReadingSettingsBottomSheet

@Composable
fun SettingsScreen(
    readingPreferences: ReadingPreferences,
    adminSession: AdminSession,
    syncStatus: SyncStatus,
    onUpdatePreferences: ((ReadingPreferences) -> ReadingPreferences) -> Unit,
    onResetPreferences: () -> Unit,
    onSignInLocalAdmin: (displayName: String, email: String) -> Unit,
    onSignOutAdmin: () -> Unit,
    onOpenAdminDashboard: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var showReadingSheet by remember { mutableStateOf(false) }
    var showAdminSignInForm by remember { mutableStateOf(false) }
    var adminNameInput by remember { mutableStateOf("") }
    var adminEmailInput by remember { mutableStateOf("") }
    var showFirebaseGuideDialog by remember { mutableStateOf(false) }

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 760.dp)
                .align(Alignment.TopCenter)
                .testTag("settings_list"),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            item {
                Text(
                    text = "Settings",
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onBackground,
                )
            }

            // Reading & Typography
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Reading & Display",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.primary,
                    )

                    Surface(
                        color = MaterialTheme.colorScheme.surface,
                        shape = MaterialTheme.shapes.medium,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 56.dp)
                            .clickable { showReadingSheet = true }
                            .testTag("settings_reading_typography_row"),
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f),
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.TextFields,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = "Reading & Typography",
                                        style = MaterialTheme.typography.titleMedium,
                                    )
                                    Text(
                                        text = "${readingPreferences.fontFamilyOption.label}  |  Lyrics ${readingPreferences.lyricsFontSizeSp.toInt()} sp",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                            Icon(
                                imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                                contentDescription = "Open Reading Settings",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }

                    Text(
                        text = "App Theme",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        AppThemeMode.entries.forEach { mode ->
                            val selected = readingPreferences.appThemeMode == mode
                            Surface(
                                color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                                contentColor = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                                shape = MaterialTheme.shapes.small,
                                border = BorderStroke(
                                    1.dp,
                                    if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .heightIn(min = 48.dp)
                                    .clickable {
                                        onUpdatePreferences { it.copy(appThemeMode = mode) }
                                    }
                                    .testTag("theme_mode_${mode.name.lowercase()}"),
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 12.dp),
                                ) {
                                    Text(
                                        text = mode.label,
                                        style = MaterialTheme.typography.labelMedium,
                                    )
                                }
                            }
                        }
                    }
                }
            }

            item { HorizontalDivider(color = MaterialTheme.colorScheme.outline) }

            // Offline & Sync Status
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Library Storage & Sync",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = MaterialTheme.shapes.medium,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.Top,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.CloudOff,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                            )
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    text = "Offline Local Mode (Phase 1)",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                                )
                                Text(
                                    text = syncStatus.message,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                Text(
                                    text = "Published songs saved on this device: ${syncStatus.cachedSongCount}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                            }
                        }
                    }
                }
            }

            item { HorizontalDivider(color = MaterialTheme.colorScheme.outline) }

            // Administration Access
            item {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Administration",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.primary,
                    )

                    Surface(
                        color = MaterialTheme.colorScheme.surface,
                        shape = MaterialTheme.shapes.medium,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.AdminPanelSettings,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = if (adminSession.isAdmin) {
                                            adminSession.displayName ?: "Local Admin Preview"
                                        } else {
                                            "Community Member View"
                                        },
                                        style = MaterialTheme.typography.titleMedium,
                                    )
                                    Text(
                                        text = if (adminSession.isAdmin) {
                                            "Local Admin Preview active on this device"
                                        } else {
                                            "Sign in to manage songs and import DOCX/PDF files"
                                        },
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }

                            if (adminSession.isAdmin) {
                                Button(
                                    onClick = onOpenAdminDashboard,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(min = 48.dp)
                                        .testTag("settings_open_admin_dashboard"),
                                ) {
                                    Text("Open Admin Dashboard")
                                }
                                OutlinedButton(
                                    onClick = onSignOutAdmin,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(min = 48.dp)
                                        .testTag("settings_admin_sign_out"),
                                ) {
                                    Text("Sign Out of Admin Mode")
                                }
                            } else if (!showAdminSignInForm) {
                                Button(
                                    onClick = { showAdminSignInForm = true },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(min = 48.dp)
                                        .testTag("settings_admin_sign_in_button"),
                                ) {
                                    Text("Administrator Sign-In (Local Preview)")
                                }
                            } else {
                                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Text(
                                        text = "Phase 1 runs with local device storage. Signing in enables the Admin Dashboard, manual song editor, and DOCX/PDF song importer on this device.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                    OutlinedTextField(
                                        value = adminNameInput,
                                        onValueChange = { adminNameInput = it },
                                        label = { Text("Administrator Name") },
                                        placeholder = { Text("e.g., Worship Leader") },
                                        singleLine = true,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("admin_signin_name_input"),
                                    )
                                    OutlinedTextField(
                                        value = adminEmailInput,
                                        onValueChange = { adminEmailInput = it },
                                        label = { Text("Email (optional)") },
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth(),
                                    )
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    ) {
                                        OutlinedButton(
                                            onClick = { showAdminSignInForm = false },
                                            modifier = Modifier
                                                .weight(1f)
                                                .heightIn(min = 48.dp),
                                        ) {
                                            Text("Cancel")
                                        }
                                        Button(
                                            onClick = {
                                                onSignInLocalAdmin(adminNameInput, adminEmailInput)
                                                showAdminSignInForm = false
                                            },
                                            modifier = Modifier
                                                .weight(1f)
                                                .heightIn(min = 48.dp)
                                                .testTag("admin_signin_confirm_button"),
                                        ) {
                                            Text("Continue")
                                        }
                                    }
                                }
                            }

                            TextButton(
                                onClick = { showFirebaseGuideDialog = true },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 48.dp)
                                    .testTag("settings_firebase_setup_guide_button"),
                            ) {
                                Icon(Icons.Outlined.Info, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Firebase & First-Admin Setup Guide")
                            }
                        }
                    }
                }
            }

            item { HorizontalDivider(color = MaterialTheme.colorScheme.outline) }

            // About MHOP Songs
            item {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "About",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.primary,
                    )

                    Surface(
                        color = MaterialTheme.colorScheme.surface,
                        shape = MaterialTheme.shapes.medium,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(14.dp),
                            ) {
                                MhopBrandMark(size = 52.dp, cornerRadius = 10.dp)
                                Column {
                                    Text(
                                        text = "MHOP Songs",
                                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                    )
                                    Text(
                                        text = "Version 1.0  |  Multilingual Christian Songbook",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                            Text(
                                text = "Designed for the MHOP church family and Telugu, Hindi, and English Christian communities. Built for distraction-free worship reading, clear script typography, and reliable song management.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Outlined.MenuBook,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Supports Telugu (తెలుగు), Hindi (हिन्दी), and English",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary,
                                )
                            }
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }

    if (showReadingSheet) {
        ReadingSettingsBottomSheet(
            preferences = readingPreferences,
            onUpdate = onUpdatePreferences,
            onReset = onResetPreferences,
            onDismiss = { showReadingSheet = false },
        )
    }

    if (showFirebaseGuideDialog) {
        FirebaseSetupGuideDialog(onDismiss = { showFirebaseGuideDialog = false })
    }
}

@Composable
private fun FirebaseSetupGuideDialog(
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Firebase & First-Admin Setup") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = "1. Connect Firebase Authentication (Google Sign-In) and Cloud Firestore in Phase 2 (see FIREBASE_SETUP.md and firestore.rules in the project root).",
                    style = MaterialTheme.typography.bodySmall,
                )
                Text(
                    text = "2. Deploy firestore.rules so published songs are readable by all members, while drafts, song writes, and import_history are strictly restricted to users with role == 'admin'.",
                    style = MaterialTheme.typography.bodySmall,
                )
                Text(
                    text = "3. Create the first administrator securely in Firebase Console: sign in once with Google to create your Auth UID, open Firestore -> users collection, create a document with Document ID = <your Auth UID>, and set field role = 'admin'.",
                    style = MaterialTheme.typography.bodySmall,
                )
                Text(
                    text = "4. Public admin self-registration and hardcoded passwords are never permitted.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        },
    )
}
