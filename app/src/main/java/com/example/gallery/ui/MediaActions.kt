package com.example.gallery.ui

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext

/** แชร์รูปหนึ่งหรือหลายรูปไปแอพอื่น */
fun shareImages(context: Context, uris: List<Uri>) {
    if (uris.isEmpty()) return
    val send = if (uris.size == 1) {
        Intent(Intent.ACTION_SEND).apply {
            type = "image/*"
            putExtra(Intent.EXTRA_STREAM, uris[0])
        }
    } else {
        Intent(Intent.ACTION_SEND_MULTIPLE).apply {
            type = "image/*"
            putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(uris))
        }
    }
    send.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    context.startActivity(Intent.createChooser(send, null))
}

/**
 * ลบรูปออกจากเครื่อง (ระบบ Android จะถามยืนยันเอง) ใช้ได้บน Android 11 ขึ้นไป
 * คืนค่า null บนเวอร์ชันที่เก่ากว่า เพื่อให้ซ่อนปุ่มลบ
 */
@Composable
fun rememberMediaDeleter(onDeleted: () -> Unit): ((List<Uri>) -> Unit)? {
    val context = LocalContext.current
    val currentOnDeleted by rememberUpdatedState(onDeleted)
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) currentOnDeleted()
    }
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return null
    return remember(launcher) {
        { uris: List<Uri> ->
            val pending = MediaStore.createDeleteRequest(context.contentResolver, uris)
            launcher.launch(IntentSenderRequest.Builder(pending.intentSender).build())
        }
    }
}
