@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.gallery.ui

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Label
import androidx.compose.material.icons.automirrored.filled.Sort
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.gallery.GalleryViewModel
import com.example.gallery.R
import kotlinx.coroutines.flow.update

/** หน้ารูปในโฟลเดอร์เดียว: แท็กของโฟลเดอร์ ค้นหา เรียงลำดับ เลือกหลายรูป แชร์ ลบ */
@Composable
fun FolderScreen(vm: GalleryViewModel) {
    val context = LocalContext.current
    val detail by vm.detail.collectAsStateWithLifecycle()
    val filter by vm.inFolderFilter.collectAsStateWithLifecycle()
    val tags by vm.tags.collectAsStateWithLifecycle()
    val customSort by vm.inFolderCustom.collectAsStateWithLifecycle()

    var selection by remember { mutableStateOf(setOf<Long>()) }
    var editingTags by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    var showSort by remember { mutableStateOf(false) }

    val d = detail
    val photos = d?.photos.orEmpty()

    val deleter = rememberMediaDeleter {
        selection = emptySet()
        vm.refresh()
    }

    fun toggle(id: Long) {
        selection = if (id in selection) selection - id else selection + id
    }

    fun selectedUris(): List<Uri> =
        photos.filter { it.id in selection }.map { Uri.parse(it.uri) }

    BackHandler {
        if (selection.isNotEmpty()) selection = emptySet() else vm.closeFolder()
    }

    Scaffold(
        topBar = {
            if (selection.isEmpty()) {
                TopAppBar(
                    title = {
                        Text(
                            text = d?.folder?.name.orEmpty(),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = { vm.closeFolder() }) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(R.string.back)
                            )
                        }
                    },
                    actions = {
                        IconButton(enabled = d != null, onClick = { editingTags = true }) {
                            Icon(
                                Icons.AutoMirrored.Filled.Label,
                                contentDescription = stringResource(R.string.edit_tags)
                            )
                        }
                        IconButton(onClick = { showSort = true }) {
                            Icon(
                                Icons.AutoMirrored.Filled.Sort,
                                contentDescription = stringResource(R.string.sort_title)
                            )
                        }
                    }
                )
            } else {
                val onDelete: (() -> Unit)? = if (deleter != null) ({ confirmDelete = true }) else null
                SelectionTopBar(
                    count = selection.size,
                    onClose = { selection = emptySet() },
                    onSelectAll = { selection = photos.map { it.id }.toSet() },
                    onShare = { shareImages(context, selectedUris()) },
                    onDelete = onDelete
                )
            }
        }
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (d != null) {
                Text(
                    text = stringResource(R.string.items_count, d.folder.count) +
                        d.folder.tags.joinToString("") { "   #${it.name}" },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                )
            }

            SearchField(
                value = filter.query,
                onValueChange = { q -> vm.inFolderFilter.update { it.copy(query = q) } },
                hint = stringResource(R.string.search_hint_in_folder),
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 8.dp)
            )

            Box(
                Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                if (d != null) {
                    if (photos.isEmpty()) {
                        EmptyState(
                            text = stringResource(
                                if (filter.query.isNotBlank()) R.string.no_results else R.string.no_photos
                            )
                        )
                    } else {
                        PhotoGrid(
                            photos = photos,
                            group = DateGroup.NONE,
                            selection = selection,
                            onClick = { index ->
                                if (selection.isNotEmpty()) toggle(photos[index].id) else vm.openViewer(photos, index)
                            },
                            onLongClick = { item -> toggle(item.id) },
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }
        }
    }

    if (showSort) {
        SortDialog(
            options = PHOTO_SORT_OPTIONS,
            selected = filter.sort,
            ascending = filter.ascending,
            natural = filter.natural,
            showFolderOnly = true,
            initialFolderOnly = customSort,
            onConfirm = { sort, asc, natural, folderOnly ->
                vm.setInFolderSort(sort, asc, natural, folderOnly)
                showSort = false
            },
            onDismiss = { showSort = false }
        )
    }

    if (editingTags && d != null) {
        FolderTagsDialog(
            folderName = d.folder.name,
            initialTagIds = d.folder.tags.map { it.id }.toSet(),
            allTags = tags,
            onCreateTag = { name, done -> vm.createTag(name, done) },
            onSave = { ids ->
                vm.saveFolderTags(d.folder.bucketId, ids)
                editingTags = false
            },
            onDismiss = { editingTags = false }
        )
    }

    if (confirmDelete) {
        ConfirmDialog(
            title = stringResource(R.string.delete_photos_title, selection.size),
            message = stringResource(R.string.delete_photos_message),
            confirmLabel = stringResource(R.string.delete),
            onConfirm = {
                confirmDelete = false
                deleter?.invoke(selectedUris())
            },
            onDismiss = { confirmDelete = false }
        )
    }
}
