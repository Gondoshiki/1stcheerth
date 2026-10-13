package com.example.gallery.ui

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.gallery.GalleryViewModel
import com.example.gallery.R
import com.example.gallery.ViewerState

/** ดูรูปเต็มจอ: ปัดซ้าย/ขวาเปลี่ยนรูป ถ่างนิ้วหรือแตะสองครั้งเพื่อซูม แตะครั้งเดียวเพื่อซ่อน/แสดงแถบ */
@Composable
fun ViewerScreen(vm: GalleryViewModel, state: ViewerState) {
    val context = LocalContext.current
    val items = state.items

    BackHandler { vm.closeViewer() }

    if (items.isEmpty()) {
        LaunchedEffect(Unit) { vm.closeViewer() }
        return
    }

    val pagerState = rememberPagerState(
        initialPage = state.index.coerceIn(0, items.lastIndex)
    ) { items.size }
    var zoomed by remember { mutableStateOf(false) }
    var showBars by remember { mutableStateOf(true) }
    var confirmDelete by remember { mutableStateOf(false) }

    val deleter = rememberMediaDeleter {
        vm.closeViewer()
        vm.refresh()
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
            userScrollEnabled = !zoomed,
            key = { items[it].id }
        ) { page ->
            ZoomableImage(
                model = items[page].uri,
                description = items[page].name,
                onZoomChanged = { zoomed = it },
                onTap = { showBars = !showBars }
            )
        }

        if (showBars) {
            val current = items[pagerState.currentPage.coerceIn(0, items.lastIndex)]
            Row(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .background(Color(0x99000000))
                    .statusBarsPadding()
                    .padding(horizontal = 4.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { vm.closeViewer() }) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = stringResource(R.string.close),
                        tint = Color.White
                    )
                }
                Text(
                    text = "${pagerState.currentPage + 1} / ${items.size}   ${current.name}",
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = { shareImages(context, listOf(Uri.parse(current.uri))) }) {
                    Icon(
                        Icons.Default.Share,
                        contentDescription = stringResource(R.string.share),
                        tint = Color.White
                    )
                }
                if (deleter != null) {
                    IconButton(onClick = { confirmDelete = true }) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = stringResource(R.string.delete),
                            tint = Color.White
                        )
                    }
                }
            }
        }
    }

    if (confirmDelete) {
        ConfirmDialog(
            title = stringResource(R.string.delete_photos_title, 1),
            message = stringResource(R.string.delete_photos_message),
            confirmLabel = stringResource(R.string.delete),
            onConfirm = {
                confirmDelete = false
                val current = items[pagerState.currentPage.coerceIn(0, items.lastIndex)]
                deleter?.invoke(listOf(Uri.parse(current.uri)))
            },
            onDismiss = { confirmDelete = false }
        )
    }
}

@Composable
private fun ZoomableImage(
    model: String,
    description: String,
    onZoomChanged: (Boolean) -> Unit,
    onTap: () -> Unit
) {
    val context = LocalContext.current
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    var boxSize by remember { mutableStateOf(IntSize.Zero) }
    val currentOnZoom by rememberUpdatedState(onZoomChanged)
    val currentOnTap by rememberUpdatedState(onTap)

    fun clampOffset(value: Offset, s: Float): Offset {
        val maxX = boxSize.width * (s - 1f) / 2f
        val maxY = boxSize.height * (s - 1f) / 2f
        return Offset(value.x.coerceIn(-maxX, maxX), value.y.coerceIn(-maxY, maxY))
    }

    Box(
        Modifier
            .fillMaxSize()
            .onSizeChanged { boxSize = it }
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { currentOnTap() },
                    onDoubleTap = {
                        if (scale > 1f) {
                            scale = 1f
                            offset = Offset.Zero
                        } else {
                            scale = 2.5f
                        }
                        currentOnZoom(scale > 1f)
                    }
                )
            }
            .pointerInput(Unit) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)
                    do {
                        val event = awaitPointerEvent()
                        val multiTouch = event.changes.count { it.pressed } >= 2
                        // นิ้วเดียวตอนยังไม่ซูม ปล่อยให้ pager ปัดเปลี่ยนรูปเอง
                        if (multiTouch || scale > 1f) {
                            val zoom = event.calculateZoom()
                            val pan = event.calculatePan()
                            val newScale = (scale * zoom).coerceIn(1f, 5f)
                            scale = newScale
                            offset = if (newScale <= 1f) Offset.Zero else clampOffset(offset + pan, newScale)
                            currentOnZoom(newScale > 1f)
                            event.changes.forEach { if (it.positionChanged()) it.consume() }
                        }
                    } while (event.changes.any { it.pressed })
                }
            }
    ) {
        AsyncImage(
            model = ImageRequest.Builder(context).data(model).size(2400).build(),
            contentDescription = description,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    translationX = offset.x
                    translationY = offset.y
                }
        )
    }
}
