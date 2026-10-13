package com.example.gallery.ui

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.gallery.GalleryViewModel
import com.example.gallery.R

private fun mediaPermission(): String =
    if (Build.VERSION.SDK_INT >= 33) {
        Manifest.permission.READ_MEDIA_IMAGES
    } else {
        Manifest.permission.READ_EXTERNAL_STORAGE
    }

private fun hasMediaPermission(context: Context): Boolean =
    ContextCompat.checkSelfPermission(context, mediaPermission()) == PackageManager.PERMISSION_GRANTED

/** ตัวสลับหน้าจอ: ขอสิทธิ์ -> โฟลเดอร์ (หน้าแรก) -> รูปในโฟลเดอร์ / รูปทั้งหมด -> ตัวดูรูป */
@Composable
fun GalleryApp(vm: GalleryViewModel, onToggleLanguage: () -> Unit) {
    val context = LocalContext.current
    var granted by remember { mutableStateOf(hasMediaPermission(context)) }

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { ok ->
        granted = ok
        if (ok) vm.refresh()
    }

    // กลับมาที่แอพทุกครั้ง: เช็กสิทธิ์ใหม่ และอ่านรูปในเครื่องใหม่
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        val now = hasMediaPermission(context)
        granted = now
        if (now) vm.refresh()
    }

    LaunchedEffect(Unit) {
        if (!hasMediaPermission(context)) launcher.launch(mediaPermission())
    }

    val openId by vm.openBucketId.collectAsStateWithLifecycle()
    val showAll by vm.showAll.collectAsStateWithLifecycle()
    val viewer by vm.viewer.collectAsStateWithLifecycle()

    Box(Modifier.fillMaxSize()) {
        when {
            !granted -> PermissionScreen(
                onGrant = { launcher.launch(mediaPermission()) },
                onOpenSettings = {
                    context.startActivity(
                        Intent(
                            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                            Uri.fromParts("package", context.packageName, null)
                        )
                    )
                }
            )
            openId != null -> FolderScreen(vm)
            showAll -> AllPhotosScreen(vm)
            else -> FoldersScreen(vm, onToggleLanguage)
        }
        viewer?.let { state -> ViewerScreen(vm, state) }
    }
}

@Composable
private fun PermissionScreen(onGrant: () -> Unit, onOpenSettings: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.PhotoLibrary,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(64.dp)
        )
        Spacer(Modifier.height(24.dp))
        Text(
            text = stringResource(R.string.permission_title),
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.permission_message),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(24.dp))
        Button(onClick = onGrant) { Text(stringResource(R.string.grant_permission)) }
        TextButton(onClick = onOpenSettings) { Text(stringResource(R.string.open_settings)) }
    }
}
