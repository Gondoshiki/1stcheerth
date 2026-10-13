@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.gallery.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.gallery.FolderSort
import com.example.gallery.PhotoSort
import com.example.gallery.R
import com.example.gallery.data.TagEntity

/** ตัวเลือกในกล่อง "เรียงตาม" ของโฟลเดอร์ (ลำดับเดียวกับ Right Gallery) */
val FOLDER_SORT_OPTIONS: List<Pair<FolderSort, Int>> = listOf(
    FolderSort.NAME to R.string.sort_name,
    FolderSort.PATH to R.string.sort_path,
    FolderSort.SIZE to R.string.sort_size,
    FolderSort.COUNT to R.string.sort_count,
    FolderSort.MODIFIED to R.string.sort_modified,
    FolderSort.TAKEN to R.string.sort_taken,
    FolderSort.RANDOM to R.string.sort_random
)

val PHOTO_SORT_OPTIONS: List<Pair<PhotoSort, Int>> = listOf(
    PhotoSort.NAME to R.string.sort_name,
    PhotoSort.SIZE to R.string.sort_size,
    PhotoSort.MODIFIED to R.string.sort_modified,
    PhotoSort.TAKEN to R.string.sort_taken,
    PhotoSort.RANDOM to R.string.sort_random
)

/** ช่องค้นหาทรงแคปซูลสีเทาเข้ม ไม่มีเส้นขอบ เหมือน Right Gallery */
@Composable
fun SearchField(
    value: String,
    onValueChange: (String) -> Unit,
    hint: String,
    modifier: Modifier = Modifier
) {
    TextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.fillMaxWidth(),
        placeholder = { Text(hint) },
        singleLine = true,
        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
        trailingIcon = {
            if (value.isNotEmpty()) {
                IconButton(onClick = { onValueChange("") }) {
                    Icon(Icons.Default.Close, contentDescription = stringResource(R.string.close))
                }
            }
        },
        shape = RoundedCornerShape(28.dp),
        colors = TextFieldDefaults.colors(
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
            disabledIndicatorColor = Color.Transparent,
            errorIndicatorColor = Color.Transparent,
            focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        )
    )
}

/** แถบชิปแท็กสำหรับกรอง เลือกได้หลายแท็ก */
@Composable
fun TagFilterRow(
    tags: List<TagEntity>,
    selected: Set<Long>,
    onToggle: (Long) -> Unit
) {
    if (tags.isEmpty()) return
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(tags, key = { it.id }) { tag ->
            FilterChip(
                selected = tag.id in selected,
                onClick = { onToggle(tag.id) },
                label = { Text(tag.name) }
            )
        }
    }
}

@Composable
fun EmptyState(text: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/** ม่านดำจางๆ พร้อมเครื่องหมายถูก แสดงทับรูปที่ถูกเลือก */
@Composable
fun SelectionOverlay(selected: Boolean) {
    if (!selected) return
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0x66000000))
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(6.dp)
                .size(24.dp)
                .background(MaterialTheme.colorScheme.primary, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

/** แถบบนตอนเลือกรูปหลายรูป */
@Composable
fun SelectionTopBar(
    count: Int,
    onClose: () -> Unit,
    onSelectAll: () -> Unit,
    onShare: () -> Unit,
    onDelete: (() -> Unit)?
) {
    TopAppBar(
        title = { Text(stringResource(R.string.selected_count, count)) },
        navigationIcon = {
            IconButton(onClick = onClose) {
                Icon(Icons.Default.Close, contentDescription = stringResource(R.string.close))
            }
        },
        actions = {
            IconButton(onClick = onSelectAll) {
                Icon(Icons.Default.SelectAll, contentDescription = stringResource(R.string.select_all))
            }
            IconButton(onClick = onShare) {
                Icon(Icons.Default.Share, contentDescription = stringResource(R.string.share))
            }
            if (onDelete != null) {
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.delete))
                }
            }
        }
    )
}

/** เมนู ⋮ ของหน้าโฟลเดอร์: เปลี่ยนมุมมอง / จำนวนคอลัมน์ / จัดการแท็ก / สลับภาษา */
@Composable
fun AppMenu(
    tags: List<TagEntity>,
    onCreateTag: (String) -> Unit,
    onDeleteTag: (Long) -> Unit,
    onToggleLanguage: () -> Unit,
    onChangeViewType: () -> Unit,
    onColumnCount: () -> Unit
) {
    var open by remember { mutableStateOf(false) }
    var showTags by remember { mutableStateOf(false) }

    Box {
        IconButton(onClick = { open = true }) {
            Icon(Icons.Default.MoreVert, contentDescription = stringResource(R.string.more))
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.change_view_type)) },
                onClick = {
                    open = false
                    onChangeViewType()
                }
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.column_count)) },
                onClick = {
                    open = false
                    onColumnCount()
                }
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.manage_tags)) },
                onClick = {
                    open = false
                    showTags = true
                }
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.switch_language)) },
                onClick = {
                    open = false
                    onToggleLanguage()
                }
            )
        }
    }

    if (showTags) {
        TagManagerDialog(
            tags = tags,
            onCreate = onCreateTag,
            onDelete = onDeleteTag,
            onDismiss = { showTags = false }
        )
    }
}

@Composable
fun RadioRow(text: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 4.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(selected = selected, onClick = null)
        Spacer(Modifier.width(16.dp))
        Text(text = text, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
fun CheckRow(text: String, checked: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 4.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(checked = checked, onCheckedChange = null)
        Spacer(Modifier.width(16.dp))
        Text(text = text, style = MaterialTheme.typography.bodyLarge)
    }
}

/**
 * กล่อง "เรียงตาม": เลือกวิธีเรียง + จากน้อยไปมาก/มากไปน้อย + ติ๊กเรียงเลขตามค่าจริง แล้วกดตกลง
 * showFolderOnly = true จะมีช่อง "ใช้กับโฟลเดอร์นี้เท่านั้น" (ใช้ในหน้าโฟลเดอร์)
 */
@Composable
fun <T> SortDialog(
    options: List<Pair<T, Int>>,
    selected: T,
    ascending: Boolean,
    natural: Boolean,
    onConfirm: (sort: T, ascending: Boolean, natural: Boolean, folderOnly: Boolean) -> Unit,
    onDismiss: () -> Unit,
    showFolderOnly: Boolean = false,
    initialFolderOnly: Boolean = false
) {
    var sort by remember { mutableStateOf(selected) }
    var asc by remember { mutableStateOf(ascending) }
    var numeric by remember { mutableStateOf(natural) }
    var folderOnly by remember { mutableStateOf(initialFolderOnly) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.sort_title)) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                options.forEach { (value, label) ->
                    RadioRow(
                        text = stringResource(label),
                        selected = sort == value,
                        onClick = { sort = value }
                    )
                }
                HorizontalDivider(Modifier.padding(vertical = 8.dp))
                RadioRow(
                    text = stringResource(R.string.ascending),
                    selected = asc,
                    onClick = { asc = true }
                )
                RadioRow(
                    text = stringResource(R.string.descending),
                    selected = !asc,
                    onClick = { asc = false }
                )
                HorizontalDivider(Modifier.padding(vertical = 8.dp))
                CheckRow(
                    text = stringResource(R.string.sort_numeric),
                    checked = numeric,
                    onClick = { numeric = !numeric }
                )
                if (showFolderOnly) {
                    CheckRow(
                        text = stringResource(R.string.use_for_this_folder_only),
                        checked = folderOnly,
                        onClick = { folderOnly = !folderOnly }
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(sort, asc, numeric, folderOnly) }) { Text(stringResource(R.string.ok)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        }
    )
}

/** กล่องเลือกมุมมอง: ตาราง / รายการ */
@Composable
fun ViewTypeDialog(listView: Boolean, onSelect: (Boolean) -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.change_view_type)) },
        text = {
            Column {
                RadioRow(stringResource(R.string.view_grid), !listView) { onSelect(false) }
                RadioRow(stringResource(R.string.view_list), listView) { onSelect(true) }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        }
    )
}

/** กล่องเลือกจำนวนคอลัมน์ของกริดโฟลเดอร์ (1-5) */
@Composable
fun ColumnCountDialog(current: Int, onSelect: (Int) -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.column_count)) },
        text = {
            Column {
                (1..5).forEach { count ->
                    RadioRow(text = count.toString(), selected = current == count) { onSelect(count) }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        }
    )
}
