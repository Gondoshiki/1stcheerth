@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)

package com.example.gallery.ui

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Sort
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.gallery.GalleryViewModel
import com.example.gallery.PhotoSort
import com.example.gallery.R
import com.example.gallery.data.MediaItem
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import kotlinx.coroutines.flow.update

/** หน้ารูปทั้งหมด (กดไอคอนรูปที่หน้าแรก): แบ่งหัวข้อตามวัน ค้นหาด้วยชื่อรูป/ชื่อโฟลเดอร์/แท็ก */
@Composable
fun AllPhotosScreen(vm: GalleryViewModel) {
    val context = LocalContext.current
    val photos by vm.photos.collectAsStateWithLifecycle()
    val tags by vm.tags.collectAsStateWithLifecycle()
    val filter by vm.photoFilter.collectAsStateWithLifecycle()
    val media by vm.media.collectAsStateWithLifecycle()

    var selection by remember { mutableStateOf(setOf<Long>()) }
    var confirmDelete by remember { mutableStateOf(false) }
    var showSort by remember { mutableStateOf(false) }

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
        if (selection.isNotEmpty()) selection = emptySet() else vm.showAll.value = false
    }

    Scaffold(
        topBar = {
            if (selection.isEmpty()) {
                TopAppBar(
                    title = { Text(stringResource(R.string.all_photos)) },
                    navigationIcon = {
                        IconButton(onClick = { vm.showAll.value = false }) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(R.string.back)
                            )
                        }
                    },
                    actions = {
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
            SearchField(
                value = filter.query,
                onValueChange = { q -> vm.photoFilter.update { it.copy(query = q) } },
                hint = stringResource(R.string.search_hint_photos),
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 8.dp)
            )

            TagFilterRow(tags = tags, selected = filter.tagIds, onToggle = { vm.togglePhotoTag(it) })

            Box(
                Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                when {
                    media == null -> CircularProgressIndicator(Modifier.align(Alignment.Center))
                    photos.isEmpty() -> EmptyState(
                        text = stringResource(
                            if (filter.query.isNotBlank() || filter.tagIds.isNotEmpty()) {
                                R.string.no_results
                            } else {
                                R.string.no_folders
                            }
                        )
                    )
                    else -> PhotoGrid(
                        photos = photos,
                        group = when (filter.sort) {
                            PhotoSort.TAKEN -> DateGroup.TAKEN
                            PhotoSort.MODIFIED -> DateGroup.MODIFIED
                            else -> DateGroup.NONE
                        },
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

    if (showSort) {
        SortDialog(
            options = PHOTO_SORT_OPTIONS,
            selected = filter.sort,
            ascending = filter.ascending,
            natural = filter.natural,
            onConfirm = { sort, asc, natural, _ ->
                vm.setPhotoSort(sort, asc, natural)
                showSort = false
            },
            onDismiss = { showSort = false }
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

/** แบ่งหัวข้อตามวันที่แบบไหน (NONE = ไม่แบ่ง) */
enum class DateGroup { NONE, TAKEN, MODIFIED }

private sealed interface GridEntry {
    data class Header(val key: String, val label: String) : GridEntry
    data class Photo(val item: MediaItem, val index: Int) : GridEntry
}

private fun buildEntries(
    photos: List<MediaItem>,
    group: DateGroup,
    formatter: DateTimeFormatter
): List<GridEntry> {
    if (group == DateGroup.NONE) {
        return photos.mapIndexed { index, item -> GridEntry.Photo(item, index) }
    }
    val zone = ZoneId.systemDefault()
    val out = ArrayList<GridEntry>(photos.size + 32)
    var lastDay: LocalDate? = null
    photos.forEachIndexed { index, item ->
        val millis = if (group == DateGroup.TAKEN) item.date else item.modified
        val day = Instant.ofEpochMilli(millis).atZone(zone).toLocalDate()
        if (day != lastDay) {
            out.add(GridEntry.Header(key = "h-$day", label = formatter.format(day)))
            lastDay = day
        }
        out.add(GridEntry.Photo(item, index))
    }
    return out
}

/** ตารางรูป (ใช้ทั้งหน้ารูปทั้งหมดและหน้าโฟลเดอร์) onClick ส่งตำแหน่งรูปในรายการ photos */
@Composable
fun PhotoGrid(
    photos: List<MediaItem>,
    group: DateGroup,
    selection: Set<Long>,
    onClick: (Int) -> Unit,
    onLongClick: (MediaItem) -> Unit,
    modifier: Modifier = Modifier,
    bottomPadding: Dp = 16.dp
) {
    val locale = LocalConfiguration.current.locales[0]
    val formatter = remember(locale) {
        DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(locale)
    }
    val entries = remember(photos, group, formatter) { buildEntries(photos, group, formatter) }

    LazyVerticalGrid(
        columns = GridCells.Adaptive(104.dp),
        modifier = modifier,
        contentPadding = PaddingValues(start = 2.dp, end = 2.dp, bottom = bottomPadding),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        items(
            entries,
            key = { entry ->
                when (entry) {
                    is GridEntry.Header -> entry.key
                    is GridEntry.Photo -> "p-${entry.item.id}"
                }
            },
            span = { entry ->
                if (entry is GridEntry.Header) GridItemSpan(maxLineSpan) else GridItemSpan(1)
            },
            contentType = { entry -> if (entry is GridEntry.Header) 0 else 1 }
        ) { entry ->
            when (entry) {
                is GridEntry.Header -> Text(
                    text = entry.label,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 14.dp, end = 14.dp, top = 16.dp, bottom = 8.dp)
                )
                is GridEntry.Photo -> Box(
                    Modifier
                        .aspectRatio(1f)
                        .combinedClickable(
                            onClick = { onClick(entry.index) },
                            onLongClick = { onLongClick(entry.item) }
                        )
                ) {
                    AsyncImage(
                        model = entry.item.uri,
                        contentDescription = entry.item.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    SelectionOverlay(entry.item.id in selection)
                }
            }
        }
    }
}
