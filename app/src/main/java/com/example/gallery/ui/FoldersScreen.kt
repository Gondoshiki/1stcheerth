@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)

package com.example.gallery.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Label
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.gallery.FolderItem
import com.example.gallery.GalleryViewModel
import com.example.gallery.R
import kotlinx.coroutines.flow.update

/** หน้าแรก: โฟลเดอร์จริงในเครื่อง + ช่องค้นหา (ชื่อหรือแท็ก) + ชิปแท็ก + เรียงลำดับ แบบ Right Gallery */
@Composable
fun FoldersScreen(vm: GalleryViewModel, onToggleLanguage: () -> Unit) {
    val folders by vm.folders.collectAsStateWithLifecycle()
    val tags by vm.tags.collectAsStateWithLifecycle()
    val filter by vm.folderFilter.collectAsStateWithLifecycle()
    val media by vm.media.collectAsStateWithLifecycle()
    val columns by vm.columns.collectAsStateWithLifecycle()
    val listView by vm.listView.collectAsStateWithLifecycle()

    var editing by remember { mutableStateOf<FolderItem?>(null) }
    var showSort by remember { mutableStateOf(false) }
    var showViewType by remember { mutableStateOf(false) }
    var showColumns by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.app_name)) },
                actions = {
                    IconButton(onClick = { vm.showAll.value = true }) {
                        Icon(
                            Icons.Default.PhotoLibrary,
                            contentDescription = stringResource(R.string.all_photos)
                        )
                    }
                    IconButton(onClick = { showSort = true }) {
                        Icon(
                            Icons.AutoMirrored.Filled.Sort,
                            contentDescription = stringResource(R.string.sort_title)
                        )
                    }
                    AppMenu(
                        tags = tags,
                        onCreateTag = { vm.createTag(it) },
                        onDeleteTag = { vm.deleteTag(it) },
                        onToggleLanguage = onToggleLanguage,
                        onChangeViewType = { showViewType = true },
                        onColumnCount = { showColumns = true }
                    )
                }
            )
        }
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            SearchField(
                value = filter.query,
                onValueChange = { q -> vm.folderFilter.update { it.copy(query = q) } },
                hint = stringResource(R.string.search_hint_folders),
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 8.dp)
            )

            TagFilterRow(tags = tags, selected = filter.tagIds, onToggle = { vm.toggleFolderTag(it) })

            Box(
                Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                when {
                    media == null -> CircularProgressIndicator(Modifier.align(Alignment.Center))
                    folders.isEmpty() -> EmptyState(
                        text = stringResource(
                            if (filter.query.isNotBlank() || filter.tagIds.isNotEmpty()) {
                                R.string.no_results
                            } else {
                                R.string.no_folders
                            }
                        )
                    )
                    listView -> LazyColumn(
                        contentPadding = PaddingValues(vertical = 8.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(folders, key = { it.bucketId }) { item ->
                            FolderRow(
                                item = item,
                                onClick = { vm.openFolder(item.bucketId) },
                                onEditTags = { editing = item }
                            )
                        }
                    }
                    else -> LazyVerticalGrid(
                        columns = GridCells.Fixed(columns),
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalArrangement = Arrangement.spacedBy(20.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(folders, key = { it.bucketId }) { item ->
                            FolderCard(
                                item = item,
                                onClick = { vm.openFolder(item.bucketId) },
                                onEditTags = { editing = item }
                            )
                        }
                    }
                }
            }
        }
    }

    if (showSort) {
        SortDialog(
            options = FOLDER_SORT_OPTIONS,
            selected = filter.sort,
            ascending = filter.ascending,
            natural = filter.natural,
            onConfirm = { sort, asc, natural, _ ->
                vm.setFolderSort(sort, asc, natural)
                showSort = false
            },
            onDismiss = { showSort = false }
        )
    }

    if (showViewType) {
        ViewTypeDialog(
            listView = listView,
            onSelect = {
                vm.setListView(it)
                showViewType = false
            },
            onDismiss = { showViewType = false }
        )
    }

    if (showColumns) {
        ColumnCountDialog(
            current = columns,
            onSelect = {
                vm.setColumns(it)
                showColumns = false
            },
            onDismiss = { showColumns = false }
        )
    }

    editing?.let { folder ->
        FolderTagsDialog(
            folderName = folder.name,
            initialTagIds = folder.tags.map { it.id }.toSet(),
            allTags = tags,
            onCreateTag = { name, done -> vm.createTag(name, done) },
            onSave = { ids ->
                vm.saveFolderTags(folder.bucketId, ids)
                editing = null
            },
            onDismiss = { editing = null }
        )
    }
}

@Composable
private fun TagButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    IconButton(
        onClick = onClick,
        modifier = modifier
            .size(36.dp)
            .background(Color(0x88000000), CircleShape)
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Filled.Label,
            contentDescription = stringResource(R.string.edit_tags),
            tint = Color.White,
            modifier = Modifier.size(20.dp)
        )
    }
}

/** การ์ดโฟลเดอร์แบบกริด: รูปปกมุมมน ชื่อ (หลายบรรทัดได้) จำนวนรายการ และแท็ก */
@Composable
private fun FolderCard(item: FolderItem, onClick: () -> Unit, onEditTags: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onClick, onLongClick = onEditTags)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(20.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            AsyncImage(
                model = item.coverUri,
                contentDescription = item.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            TagButton(
                onClick = onEditTags,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
            )
        }
        Spacer(Modifier.height(8.dp))
        Text(
            text = item.name,
            style = MaterialTheme.typography.bodyLarge,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 4.dp)
        )
        Text(
            text = stringResource(R.string.items_count, item.count),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 4.dp)
        )
        if (item.tags.isNotEmpty()) {
            Text(
                text = item.tags.joinToString("  ") { "#${it.name}" },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
            )
        }
    }
}

/** แถวโฟลเดอร์แบบรายการ */
@Composable
private fun FolderRow(item: FolderItem, onClick: () -> Unit, onEditTags: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onClick, onLongClick = onEditTags)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = item.coverUri,
            contentDescription = item.name,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(72.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
        )
        Column(
            Modifier
                .weight(1f)
                .padding(horizontal = 16.dp)
        ) {
            Text(
                text = item.name,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = stringResource(R.string.items_count, item.count),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (item.tags.isNotEmpty()) {
                Text(
                    text = item.tags.joinToString("  ") { "#${it.name}" },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        TagButton(onClick = onEditTags)
    }
}
