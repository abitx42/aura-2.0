package com.example.ui

import androidx.compose.animation.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.Note
import com.example.data.NoteVersion
import com.example.audio.PlaybackState
import androidx.compose.foundation.BorderStroke
import com.example.ui.theme.*
import com.example.ui.anim.auraSpringPress
import com.example.ui.anim.ShimmerNoteCard
import com.example.ui.anim.ShimmerNoteListItem
import com.example.ui.components.*
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun NotesScreen(
    viewModel: AppViewModel,
    onOpenNoteEditor: (Note?) -> Unit,
    onOpenDrawingWorkspace: (String?) -> Unit
) {
    val searchVal by viewModel.notesSearchQuery.collectAsState()
    val selectedCategory by viewModel.selectedNoteCategory.collectAsState()
    val sortVal by viewModel.notesSortOrder.collectAsState()
    val isGridView by viewModel.isNotesGridView.collectAsState()
    val notesList by viewModel.filteredNotes.collectAsState()
    val isNotesLoading by viewModel.isNotesLoading.collectAsState()

    val availableCategories by viewModel.allUniqueCategories.collectAsState()

    var showSortMenu by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AuraTheme.colors.screenBackground)
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // App bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = "Personal Notes",
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = AuraTheme.colors.textPrimary
                    )
                    Text(
                        text = "${notesList.size} secure notes stored offline",
                        style = MaterialTheme.typography.bodySmall,
                        color = AuraTheme.colors.textSecondary
                    )
                }

                AuraHeaderActions(
                    onProClick = { viewModel.navigateTo(Section.SecuritySettings) },
                    onProfileClick = { viewModel.navigateTo(Section.SecuritySettings) }
                )
            }

            // Action Toolbar (Layout toggle + Sort menu + Clock)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                LiveClockWidget(
                    modifier = Modifier.weight(1f),
                    initialIsAnalog = false
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { viewModel.toggleNotesLayout() }) {
                        Icon(
                            imageVector = if (isGridView) Icons.Default.ViewList else Icons.Default.GridView,
                            contentDescription = "Toggle Layout",
                            tint = AuraTheme.colors.textSecondary
                        )
                    }

                    Box {
                        IconButton(onClick = { showSortMenu = true }) {
                            Icon(Icons.Default.Sort, contentDescription = "Sort Notes", tint = AuraTheme.colors.textSecondary)
                        }
                        DropdownMenu(
                            expanded = showSortMenu,
                            onDismissRequest = { showSortMenu = false },
                            modifier = Modifier.background(AuraTheme.colors.cardBackground)
                        ) {
                            DropdownMenuItem(
                                text = { Text("Recent Modified", color = AuraTheme.colors.textPrimary) },
                                onClick = {
                                    viewModel.setNotesSortOrder(SortOrder.ModifiedRecent)
                                    showSortMenu = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Oldest Modified", color = AuraTheme.colors.textPrimary) },
                                onClick = {
                                    viewModel.setNotesSortOrder(SortOrder.ModifiedOldest)
                                    showSortMenu = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Recent Created", color = AuraTheme.colors.textPrimary) },
                                onClick = {
                                    viewModel.setNotesSortOrder(SortOrder.CreatedRecent)
                                    showSortMenu = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Title Alphabetical", color = AuraTheme.colors.textPrimary) },
                                onClick = {
                                    viewModel.setNotesSortOrder(SortOrder.TitleAscending)
                                    showSortMenu = false
                                }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Search Bar
            OutlinedTextField(
                value = searchVal,
                onValueChange = { viewModel.setNotesSearchQuery(it) },
                placeholder = { Text("Search by title, tags, contents...", color = AuraTheme.colors.textMuted, fontSize = 13.sp) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search", tint = AuraTheme.colors.textMuted) },
                trailingIcon = {
                    if (searchVal.isNotEmpty()) {
                        IconButton(onClick = { viewModel.setNotesSearchQuery("") }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear", tint = AuraTheme.colors.textPrimary)
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = AuraTheme.colors.cardBackground,
                    unfocusedContainerColor = AuraTheme.colors.cardBackground.copy(alpha = 0.5f),
                    focusedBorderColor = AuraTheme.colors.accentBrand,
                    unfocusedBorderColor = AuraTheme.colors.cardBorder,
                    focusedTextColor = AuraTheme.colors.textPrimary,
                    unfocusedTextColor = AuraTheme.colors.textPrimary
                ),
                shape = RoundedCornerShape(16.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Filter chips list
            val fullCategoriesList = listOf("All") + availableCategories
            AuraPeriodSelector(
                items = fullCategoriesList,
                selectedIndex = fullCategoriesList.indexOf(selectedCategory).coerceAtLeast(0),
                onItemSelected = { idx -> viewModel.setSelectedCategory(fullCategoriesList[idx]) },
                modifier = Modifier.padding(horizontal = 16.dp),
                accentColor = AuraTheme.colors.accentBrand
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Document List
            if (isNotesLoading) {
                if (isGridView) {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(6) {
                            ShimmerNoteCard()
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(8) {
                            ShimmerNoteListItem()
                        }
                    }
                }
            } else if (notesList.isEmpty()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    AuraEmptyState(
                        title = "No Local Notes Found",
                        description = "Create a new digital notebook or vector canvas using the action button.",
                        icon = Icons.Default.DriveFileRenameOutline,
                        iconTint = AuraTheme.colors.accentBrand
                    )
                }
            } else {
                if (isGridView) {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(notesList, key = { it.id }) { note ->
                            NoteCardItem(
                                note = note,
                                onClicked = { onOpenNoteEditor(note) },
                                onTogglePinned = { viewModel.toggleNotePinned(note) },
                                onToggleFavorite = { viewModel.toggleNoteFavorite(note) }
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(notesList, key = { it.id }) { note ->
                            NoteListItem(
                                note = note,
                                onClicked = { onOpenNoteEditor(note) },
                                onTogglePinned = { viewModel.toggleNotePinned(note) },
                                onToggleFavorite = { viewModel.toggleNoteFavorite(note) }
                            )
                        }
                    }
                }
            }
        }

        // Radiant Orange Floating Action Button (FAB)
        AuraFloatingActionButton(
            onClick = { onOpenNoteEditor(null) },
            contentDescription = "New Note",
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 80.dp, end = 20.dp)
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun NoteCardItem(
    note: Note,
    onClicked: () -> Unit,
    onTogglePinned: () -> Unit,
    onToggleFavorite: () -> Unit
) {
    val sdf = remember { SimpleDateFormat("MM-dd HH:mm", Locale.getDefault()) }
    val shape = RoundedCornerShape(16.dp)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(156.dp)
            .clip(shape)
            .border(
                1.dp,
                if (note.isPinned) AuraTheme.colors.accentBrand.copy(alpha = 0.5f) else AuraTheme.colors.cardBorder,
                shape
            )
            .auraSpringPress(
                cornerRadius = 16.dp,
                onClick = onClicked,
                onLongClick = onTogglePinned
            )
            .testTag("note_item_card_${note.id}"),
        colors = CardDefaults.cardColors(
            containerColor = if (note.isPinned) AuraTheme.colors.cardBackground.copy(alpha = 0.95f) else AuraTheme.colors.cardBackground
        ),
        shape = shape
    ) {
        Column(
            modifier = Modifier
                .padding(14.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                // Top Row: Category Squircle Badge + Pin / Bookmark
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(AuraTheme.colors.accentBrand.copy(alpha = 0.14f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = note.category.uppercase(),
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            color = AuraTheme.colors.accentBrand,
                            letterSpacing = 0.8.sp
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        if (note.isPinned) {
                            Icon(
                                Icons.Default.PushPin,
                                contentDescription = "Pinned",
                                tint = AuraTheme.colors.badgeGold,
                                modifier = Modifier.size(13.dp)
                            )
                        }
                        if (note.isBookmarked) {
                            Icon(
                                Icons.Default.Bookmark,
                                contentDescription = "Bookmarked",
                                tint = AuraTheme.colors.accentBrand,
                                modifier = Modifier.size(13.dp)
                            )
                        }
                    }
                }

                // Title & Content
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = note.title.ifBlank { "Untitled" },
                            style = MaterialTheme.typography.titleMedium,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = AuraTheme.colors.textPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        Spacer(modifier = Modifier.height(2.dp))

                        Text(
                            text = note.content.ifBlank { "Empty details..." },
                            style = MaterialTheme.typography.bodySmall,
                            fontSize = 11.sp,
                            color = AuraTheme.colors.textSecondary,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            lineHeight = 15.sp
                        )
                    }

                    if (note.photoPath != null) {
                        androidx.compose.foundation.Image(
                            painter = coil.compose.rememberAsyncImagePainter(
                                model = coil.request.ImageRequest.Builder(LocalContext.current)
                                    .data(note.photoPath)
                                    .size(coil.size.Size(120, 120))
                                    .crossfade(true)
                                    .memoryCacheKey("note_photo_thumb_${note.id}")
                                    .build()
                            ),
                            contentDescription = "Attachment preview",
                            modifier = Modifier
                                .size(42.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .border(1.dp, AuraTheme.colors.cardBorder, RoundedCornerShape(8.dp)),
                            contentScale = androidx.compose.ui.layout.ContentScale.Crop
                        )
                    }
                }
            }

            // Bottom metadata row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = sdf.format(Date(note.lastModified)),
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 9.sp,
                    color = AuraTheme.colors.textMuted
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (note.photoPath != null) {
                        Icon(Icons.Default.PhotoCamera, contentDescription = "Media attached", tint = AuraTheme.colors.accentBrand, modifier = Modifier.size(12.dp))
                    }
                    if (note.voicePath != null) {
                        Icon(Icons.Default.VolumeUp, contentDescription = "Audio track", tint = AuraTheme.colors.positiveGreen, modifier = Modifier.size(12.dp))
                    }
                    if (note.drawingData != null) {
                        Icon(Icons.Default.Edit, contentDescription = "Sketch included", tint = AuraTheme.colors.accentBrand, modifier = Modifier.size(12.dp))
                    }
                    Icon(
                        imageVector = if (note.isFavorite) Icons.Default.Favorite else Icons.Outlined.FavoriteBorder,
                        contentDescription = "Favorite Toggle",
                        tint = if (note.isFavorite) AuraTheme.colors.negativeRed else AuraTheme.colors.textMuted,
                        modifier = Modifier
                            .size(15.dp)
                            .clickable { onToggleFavorite() }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun NoteListItem(
    note: Note,
    onClicked: () -> Unit,
    onTogglePinned: () -> Unit,
    onToggleFavorite: () -> Unit
) {
    val sdf = remember { SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()) }
    val shape = RoundedCornerShape(14.dp)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .border(
                1.dp,
                if (note.isPinned) AuraTheme.colors.accentBrand.copy(alpha = 0.4f) else AuraTheme.colors.cardBorder,
                shape
            )
            .auraSpringPress(
                cornerRadius = 14.dp,
                onClick = onClicked,
                onLongClick = onTogglePinned
            ),
        colors = CardDefaults.cardColors(containerColor = AuraTheme.colors.cardBackground),
        shape = shape
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (note.isPinned) {
                        Icon(Icons.Default.PushPin, contentDescription = "Pinned", tint = AuraTheme.colors.badgeGold, modifier = Modifier.size(12.dp))
                    }
                    Text(
                        text = note.title.ifBlank { "Untitled" },
                        style = MaterialTheme.typography.titleMedium,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = AuraTheme.colors.textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = note.content.ifBlank { "Empty details..." },
                    style = MaterialTheme.typography.bodySmall,
                    fontSize = 11.sp,
                    color = AuraTheme.colors.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (note.voicePath != null) {
                    Icon(Icons.Default.Mic, contentDescription = "Audio track", tint = AuraTheme.colors.positiveGreen, modifier = Modifier.size(14.dp))
                }
                if (note.drawingData != null) {
                    Icon(Icons.Default.Gesture, contentDescription = "Sketch included", tint = AuraTheme.colors.accentBrand, modifier = Modifier.size(14.dp))
                }
                IconButton(onClick = onToggleFavorite, modifier = Modifier.size(24.dp)) {
                    Icon(
                        imageVector = if (note.isFavorite) Icons.Default.Favorite else Icons.Outlined.FavoriteBorder,
                        contentDescription = "Favorite Toggle",
                        tint = if (note.isFavorite) AuraTheme.colors.negativeRed else AuraTheme.colors.textMuted,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

// ==========================================
// RICH MARKDOWN EDITOR SCREEN
// ==========================================
@Composable
fun NoteEditorScreen(
    note: Note?,
    viewModel: AppViewModel,
    onBack: () -> Unit,
    onOpenDrawingBoard: (String?) -> Unit
) {
    val isGlobalRecording by viewModel.isRecording.collectAsState()
    val recordedMicSecs by viewModel.recordedDuration.collectAsState()

    var title by remember { mutableStateOf(note?.title ?: "") }
    var content by remember { mutableStateOf(note?.content ?: "") }
    var category by remember { mutableStateOf(note?.category ?: "Personal") }
    var tags by remember { mutableStateOf(note?.tags ?: "") }
    var isBookmarked by remember { mutableStateOf(note?.isBookmarked ?: false) }

    var isPreviewMode by remember { mutableStateOf(false) }
    var showVersionsSheet by remember { mutableStateOf(false) }

    // Recover drawing and voice attachments
    var currentVoicePath by remember { mutableStateOf(note?.voicePath) }
    var currentDrawingData by remember { mutableStateOf(note?.drawingData) }
    var currentPhotoPath by remember { mutableStateOf(note?.photoPath) }

    val noteVersions by viewModel.noteVersionsFlow.collectAsState()

    // Calculate dynamic counts
    val charCount = content.length
    val wordCount = if (content.trim().isBlank()) 0 else content.trim().split("\\s+".toRegex()).size
    val readTimeMinutes = (wordCount / 200).coerceAtLeast(1)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AuraTheme.colors.screenBackground)
    ) {
        // Toolbar
        Surface(color = AuraTheme.colors.cardBackground, modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 12.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = {
                        // Auto save on back click if note has content
                        val finalTitle = if (title.isBlank() && content.isNotBlank()) {
                            content.lines().firstOrNull { it.isNotBlank() }?.take(30) ?: "Untitled Note"
                        } else title
                        if (finalTitle.isNotBlank() || content.isNotBlank()) {
                            viewModel.saveDraftNote(finalTitle, content, category, tags, currentVoicePath, currentDrawingData, isBookmarked, currentPhotoPath)
                        }
                        onBack()
                    }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = AuraTheme.colors.textPrimary)
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (note == null) "NEW NOTE" else "EDIT NOTE",
                        fontSize = 14.sp,
                        style = MaterialTheme.typography.titleMedium,
                        color = AuraTheme.colors.textPrimary,
                        fontWeight = FontWeight.Bold
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Preview Switch Button
                    IconButton(onClick = { isPreviewMode = !isPreviewMode }) {
                        Icon(
                            imageVector = if (isPreviewMode) Icons.Default.EditNote else Icons.Default.Visibility,
                            contentDescription = "Toggle Preview",
                            tint = if (isPreviewMode) AuraTheme.colors.accentBrand else AuraTheme.colors.textSecondary
                        )
                    }

                    // Smart Bookmark Highlight Toggle
                    IconButton(onClick = { isBookmarked = !isBookmarked }) {
                        Icon(
                            imageVector = if (isBookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                            contentDescription = "Smart Bookmarking Highlight",
                            tint = if (isBookmarked) AuraTheme.colors.gold else AuraTheme.colors.textSecondary
                        )
                    }

                    // Version history trigger
                    if (note != null) {
                        IconButton(onClick = { showVersionsSheet = true }) {
                            Icon(Icons.Default.History, contentDescription = "Version Snapshots", tint = AuraTheme.colors.textSecondary)
                        }
                    }

                    Button(
                        onClick = {
                            val finalTitle = if (title.isBlank() && content.isNotBlank()) {
                                content.lines().firstOrNull { it.isNotBlank() }?.take(30) ?: "Untitled Note"
                            } else title
                            viewModel.saveDraftNote(finalTitle, content, category, tags, currentVoicePath, currentDrawingData, isBookmarked, currentPhotoPath)
                            onBack()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = AuraTheme.colors.accentBrand),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp)
                    ) {
                        Text("Save", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Sub Bar: Configuration tags
        Surface(color = AuraTheme.colors.cardBackground, modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Category Tag Configuration
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Label:", fontSize = 11.sp, color = AuraTheme.colors.textMuted)
                    Spacer(modifier = Modifier.width(8.dp))
                    Box {
                        var expandedCat by remember { mutableStateOf(false) }
                        AssistChip(
                            onClick = { expandedCat = true },
                            label = { Text(category, fontSize = 11.sp, color = AuraTheme.colors.textPrimary) },
                            colors = AssistChipDefaults.assistChipColors(containerColor = AuraTheme.colors.bottomNavBackground),
                            border = BorderStroke(1.dp, AuraTheme.colors.cardBorder)
                        )
                        DropdownMenu(
                            expanded = expandedCat,
                            onDismissRequest = { expandedCat = false },
                            modifier = Modifier.background(AuraTheme.colors.cardBackground)
                        ) {
                            listOf("Personal", "Work", "Study", "Ideas", "Journal").forEach { itemCat ->
                                DropdownMenuItem(
                                    text = { Text(itemCat, color = AuraTheme.colors.textPrimary) },
                                    onClick = {
                                        category = itemCat
                                        expandedCat = false
                                    }
                                )
                            }
                        }
                    }
                }

                // Tags CSV
                OutlinedTextField(
                    value = tags,
                    onValueChange = { tags = it },
                    placeholder = { Text("tags separated by comma", fontSize = 11.sp, color = AuraTheme.colors.textMuted) },
                    modifier = Modifier
                        .width(180.dp)
                        .height(46.dp),
                    textStyle = MaterialTheme.typography.bodySmall.copy(color = AuraTheme.colors.textPrimary),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = AuraTheme.colors.bottomNavBackground,
                        unfocusedContainerColor = AuraTheme.colors.bottomNavBackground,
                        focusedBorderColor = AuraTheme.colors.accentBrand,
                        unfocusedBorderColor = AuraTheme.colors.cardBorder
                    ),
                    shape = RoundedCornerShape(10.dp)
                )
            }
        }

        // Media attachments layout row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Drawing attachment pill
            AssistChip(
                onClick = { onOpenDrawingBoard(currentDrawingData) },
                label = {
                    Text(
                        if (currentDrawingData == null) "Attach Drawing Workspace" else "Modify Sketchpad drawing",
                        color = AuraTheme.colors.textPrimary,
                        fontSize = 11.sp
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Gesture,
                        contentDescription = "Drawing",
                        tint = if (currentDrawingData == null) AuraTheme.colors.textMuted else AuraTheme.colors.accentBrand,
                        modifier = Modifier.size(14.dp)
                    )
                },
                colors = AssistChipDefaults.assistChipColors(
                    containerColor = if (currentDrawingData == null) AuraTheme.colors.cardBackground else AuraTheme.colors.accentBrand.copy(alpha = 0.2f)
                ),
                border = null
            )

            // Voice recorder controller pill
            AssistChip(
                onClick = {
                    if (isGlobalRecording) {
                        viewModel.stopAudioNoteRecording()
                        currentVoicePath = viewModel.getVoiceNoteFile()
                    } else {
                        val path = viewModel.startAudioNoteRecording()
                        if (path != null) {
                            currentVoicePath = path
                        }
                    }
                },
                label = {
                    Text(
                        if (isGlobalRecording) "Recording... (${recordedMicSecs / 1000}s)"
                        else if (currentVoicePath != null) "Playback attachment"
                        else "Record Voice Note",
                        color = AuraTheme.colors.textPrimary,
                        fontSize = 11.sp
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = if (isGlobalRecording) Icons.Default.FiberManualRecord else Icons.Default.Mic,
                        contentDescription = "Voice Recorder",
                        tint = if (isGlobalRecording) AuraTheme.colors.negativeRed else if (currentVoicePath != null) AuraTheme.colors.accentBrand else AuraTheme.colors.textMuted,
                        modifier = Modifier.size(14.dp).animateContentSize()
                    )
                },
                colors = AssistChipDefaults.assistChipColors(
                    containerColor = if (isGlobalRecording) AuraTheme.colors.negativeRed.copy(alpha = 0.2f) else AuraTheme.colors.cardBackground
                ),
                border = null
            )
        }

        // Play attached voice button if file registered
        if (!isGlobalRecording && currentVoicePath != null) {
            val audioState by viewModel.playbackState.collectAsState()
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = {
                    if (audioState is PlaybackState.Playing) {
                        viewModel.pauseVoiceNote()
                    } else if (audioState is PlaybackState.Paused) {
                        viewModel.resumeVoiceNote()
                    } else {
                        viewModel.startPlayingVoiceNote(currentVoicePath!!, title.ifBlank { "Voice Note attachment" })
                    }
                }) {
                    Icon(
                        imageVector = if (audioState is PlaybackState.Playing) Icons.Default.PauseCircle else Icons.Default.PlayCircle,
                        contentDescription = "Play/Pause attachment",
                        tint = AuraTheme.colors.accentBrand,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Text("Voice attachment loaded", fontSize = 11.sp, color = AuraTheme.colors.accentBrand)

                Spacer(modifier = Modifier.weight(1f))

                IconButton(onClick = { currentVoicePath = null }) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete voice file", tint = AuraTheme.colors.negativeRed, modifier = Modifier.size(16.dp))
                }
            }
        }

        // Image/Video Attachment Card (Render with Coil)
        if (currentPhotoPath != null) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .border(1.dp, AuraTheme.colors.cardBorder, RoundedCornerShape(16.dp)),
                colors = CardDefaults.cardColors(containerColor = AuraTheme.colors.cardBackground)
            ) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    androidx.compose.foundation.Image(
                        painter = coil.compose.rememberAsyncImagePainter(
                            model = coil.request.ImageRequest.Builder(LocalContext.current)
                                .data(currentPhotoPath)
                                .size(coil.size.Size(600, 400)) // Downscale full image preview
                                .crossfade(true)
                                .memoryCacheKey("note_full_preview_${currentPhotoPath.hashCode()}")
                                .build()
                        ),
                        contentDescription = "Note visual attachment",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = androidx.compose.ui.layout.ContentScale.Crop
                    )
                    
                    // Small overlay play icon if it's a video
                    if (currentPhotoPath!!.endsWith(".mp4") || currentPhotoPath!!.contains("video")) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .background(Color.Black.copy(alpha = 0.5f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = "Video file", tint = Color.White, modifier = Modifier.size(28.dp))
                        }
                    }

                    // Remove icon in premium glass circle button
                    IconButton(
                        onClick = { currentPhotoPath = null },
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp)
                            .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete photo asset", tint = Color.Red, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }

        // Main Markdown Text Editor Area
        if (isPreviewMode) {
            // Text rendering block with simulated markdown structures
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(16.dp)
                    .border(1.dp, AuraTheme.colors.cardBorder, RoundedCornerShape(12.dp))
                    .background(AuraTheme.colors.cardBackground, RoundedCornerShape(12.dp))
                    .padding(16.dp)
            ) {
                item {
                    Text(
                        text = title.ifBlank { "Untitled Document" },
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Black,
                        color = AuraTheme.colors.textPrimary
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider(color = AuraTheme.colors.cardBorder.copy(alpha = 0.5f))
                    Spacer(modifier = Modifier.height(16.dp))
                }

                val paragraphs = content.split("\n")
                items(paragraphs) { pText ->
                    MarkdownParagraphRenderer(text = pText)
                }
            }
        } else {
            // Standard Text Fields
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(16.dp)
                    .border(1.dp, AuraTheme.colors.cardBorder, RoundedCornerShape(12.dp))
                    .background(AuraTheme.colors.cardBackground, RoundedCornerShape(12.dp))
            ) {
                // Title Field
                TextField(
                    value = title,
                    onValueChange = { title = it },
                    placeholder = { Text("Title", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = AuraTheme.colors.textMuted) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("note_title_input"),
                    textStyle = MaterialTheme.typography.titleMedium.copy(color = AuraTheme.colors.textPrimary, fontWeight = FontWeight.Bold, fontSize = 18.sp),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        focusedTextColor = AuraTheme.colors.textPrimary,
                        unfocusedTextColor = AuraTheme.colors.textPrimary
                    )
                )

                HorizontalDivider(color = AuraTheme.colors.cardBorder.copy(alpha = 0.5f), thickness = 1.dp)

                // Description Contents
                TextField(
                    value = content,
                    onValueChange = { content = it },
                    placeholder = { Text("Write your thoughts or markdown headings...", fontSize = 14.sp, color = AuraTheme.colors.textMuted) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f).testTag("note_content_input"),
                    textStyle = MaterialTheme.typography.bodyMedium.copy(color = AuraTheme.colors.textPrimary, fontSize = 14.sp),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        focusedTextColor = AuraTheme.colors.textPrimary,
                        unfocusedTextColor = AuraTheme.colors.textPrimary
                    )
                )
            }
        }

        // Sub footer showing live statistics
        Surface(
            color = AuraTheme.colors.bottomNavBackground,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Char: $charCount   |   Words: $wordCount",
                    fontSize = 11.sp,
                    color = AuraTheme.colors.textMuted
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.HourglassEmpty, contentDescription = "Read Time", tint = AuraTheme.colors.textMuted, modifier = Modifier.size(12.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Read: ~$readTimeMinutes min",
                        fontSize = 11.sp,
                        color = AuraTheme.colors.textMuted
                    )
                }
            }
        }
    }

    // Version history restoration bottom sheet details
    if (showVersionsSheet) {
        AlertDialog(
            onDismissRequest = { showVersionsSheet = false },
            title = { Text("VERSION HISTORY LOGS", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold) },
            text = {
                Column(modifier = Modifier.fillMaxWidth().heightIn(max = 300.dp)) {
                    if (noteVersions.isEmpty()) {
                        Text("No snapshot history saved yet.", color = AuraTheme.colors.textMuted)
                    } else {
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(noteVersions) { version ->
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                             viewModel.restoreNoteVersion(version)
                                             title = version.title
                                             content = version.content
                                             showVersionsSheet = false
                                         },
                                    colors = CardDefaults.cardColors(containerColor = AuraTheme.colors.bottomNavBackground)
                                ) {
                                    Column(modifier = Modifier.padding(10.dp)) {
                                        Text(version.changeDescription, fontSize = 12.sp, color = AuraTheme.colors.accentBrand, fontWeight = FontWeight.SemiBold)
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(version.title, fontSize = 11.sp, color = AuraTheme.colors.textPrimary, fontWeight = FontWeight.Bold)
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            SimpleDateFormat("MMM dd, yyyy HH:mm:ss", Locale.getDefault()).format(Date(version.modifiedAt)),
                                            fontSize = 9.sp,
                                            color = AuraTheme.colors.textMuted
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showVersionsSheet = false }) {
                    Text("Close", color = AuraTheme.colors.accentBrand)
                }
            },
            containerColor = AuraTheme.colors.cardBackground
        )
    }
}

@Composable
fun MarkdownParagraphRenderer(text: String, modifier: Modifier = Modifier) {
    val trimmed = text.trim()
    when {
        // H1 Heading
        trimmed.startsWith("# ") -> {
            Text(
                text = trimmed.substring(2),
                fontSize = 20.sp,
                fontWeight = FontWeight.Black,
                color = AuraTheme.colors.accentBrand,
                modifier = modifier.padding(vertical = 8.dp)
            )
        }
        // H2 Heading
        trimmed.startsWith("## ") -> {
            Text(
                text = trimmed.substring(3),
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = AuraTheme.colors.gold,
                modifier = modifier.padding(vertical = 6.dp)
            )
        }
        // Bold Block
        trimmed.startsWith("**") && trimmed.endsWith("**") && trimmed.length > 4 -> {
            Text(
                text = trimmed.substring(2, trimmed.length - 2),
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = AuraTheme.colors.textPrimary,
                modifier = modifier.padding(vertical = 2.dp)
            )
        }
        // Horizontal divider
        trimmed == "---" -> {
            HorizontalDivider(color = AuraTheme.colors.cardBorder, thickness = 1.dp, modifier = modifier.padding(vertical = 12.dp))
        }
        // Bullet points
        trimmed.startsWith("- ") -> {
            Row(modifier = modifier.padding(start = 8.dp, top = 2.dp, bottom = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(4.dp).background(AuraTheme.colors.accentBrand, CircleShape))
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = trimmed.substring(2), fontSize = 13.sp, color = AuraTheme.colors.textPrimary)
            }
        }
        // Standard bullet lists / checklists
        trimmed.startsWith("[ ] ") -> {
            Row(modifier = modifier.padding(start = 8.dp, top = 2.dp, bottom = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.CheckBoxOutlineBlank, contentDescription = "Todo blank", tint = AuraTheme.colors.textMuted, modifier = Modifier.size(12.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = trimmed.substring(4), fontSize = 13.sp, color = AuraTheme.colors.textPrimary)
            }
        }
        trimmed.startsWith("[x] ") -> {
            Row(modifier = modifier.padding(start = 8.dp, top = 2.dp, bottom = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.CheckBox, contentDescription = "Todo completed", tint = AuraTheme.colors.accentBrand, modifier = Modifier.size(12.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = trimmed.substring(4),
                    fontSize = 13.sp,
                    color = AuraTheme.colors.textMuted,
                    textDecoration = TextDecoration.LineThrough
                )
            }
        }
        else -> {
            Text(
                text = text,
                fontSize = 13.sp,
                color = AuraTheme.colors.textPrimary,
                lineHeight = 20.sp,
                modifier = modifier.padding(vertical = 2.dp)
            )
        }
    }
}
