@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)

package com.example.gallery.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.gallery.R
import com.example.gallery.data.TagEntity

/** กล่องเลือกแท็กของโฟลเดอร์ + สร้างแท็กใหม่ได้ทันที */
@Composable
fun FolderTagsDialog(
    folderName: String,
    initialTagIds: Set<Long>,
    allTags: List<TagEntity>,
    onCreateTag: (String, (Long) -> Unit) -> Unit,
    onSave: (Set<Long>) -> Unit,
    onDismiss: () -> Unit
) {
    var selected by remember { mutableStateOf(initialTagIds) }
    var newTag by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.folder_tags_title, folderName)) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                if (allTags.isEmpty()) {
                    Text(
                        text = stringResource(R.string.no_tags_yet),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    allTags.forEach { tag ->
                        FilterChip(
                            selected = tag.id in selected,
                            onClick = {
                                selected = if (tag.id in selected) selected - tag.id else selected + tag.id
                            },
                            label = { Text(tag.name) }
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = newTag,
                        onValueChange = { newTag = it },
                        label = { Text(stringResource(R.string.new_tag)) },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(
                        enabled = newTag.isNotBlank(),
                        onClick = {
                            onCreateTag(newTag) { id -> selected = selected + id }
                            newTag = ""
                        }
                    ) { Text(stringResource(R.string.add)) }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(selected) }) { Text(stringResource(R.string.save)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        }
    )
}

/** กล่องจัดการแท็กทั้งหมด: เพิ่ม/ลบ */
@Composable
fun TagManagerDialog(
    tags: List<TagEntity>,
    onCreate: (String) -> Unit,
    onDelete: (Long) -> Unit,
    onDismiss: () -> Unit
) {
    var newTag by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.manage_tags)) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = newTag,
                        onValueChange = { newTag = it },
                        label = { Text(stringResource(R.string.new_tag)) },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(
                        enabled = newTag.isNotBlank(),
                        onClick = {
                            onCreate(newTag)
                            newTag = ""
                        }
                    ) { Text(stringResource(R.string.add)) }
                }
                Spacer(Modifier.height(8.dp))
                if (tags.isEmpty()) {
                    Text(
                        text = stringResource(R.string.no_tags_yet),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                tags.forEach { tag ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "#${tag.name}",
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = { onDelete(tag.id) }) {
                            Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.delete))
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
                Text(
                    text = stringResource(R.string.delete_tag_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.close)) }
        }
    )
}

@Composable
fun ConfirmDialog(
    title: String,
    message: String,
    confirmLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(message) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(confirmLabel, color = MaterialTheme.colorScheme.error)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        }
    )
}
