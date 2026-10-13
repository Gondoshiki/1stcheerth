package com.example.gallery.data

import android.content.ContentUris
import android.content.Context
import android.os.Build
import android.provider.MediaStore

/** รูปหนึ่งรูปที่อ่านจาก MediaStore */
data class MediaItem(
    val id: Long,
    val uri: String,
    val name: String,
    val bucketId: Long,
    val bucketName: String,
    /** พาธของโฟลเดอร์ เช่น Pictures/Instagram/ (ว่างถ้าอ่านไม่ได้) */
    val path: String,
    /** เวลาถ่าย (มิลลิวินาที) ถ้าไม่มีใช้เวลาที่เพิ่มเข้าเครื่อง */
    val date: Long,
    /** เวลาแก้ไขล่าสุด (มิลลิวินาที) */
    val modified: Long,
    /** ขนาดไฟล์ (ไบต์) */
    val size: Long
)

/** อ่านรูปทั้งหมดในเครื่อง ต้องเรียกจาก background thread */
fun loadDeviceMedia(context: Context): List<MediaItem> {
    val result = ArrayList<MediaItem>()
    val collection = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
    val hasRelativePath = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q

    val projection = buildList {
        add(MediaStore.Images.Media._ID)
        add(MediaStore.Images.Media.DISPLAY_NAME)
        add(MediaStore.Images.Media.BUCKET_ID)
        add(MediaStore.Images.Media.BUCKET_DISPLAY_NAME)
        add(MediaStore.Images.Media.DATE_TAKEN)
        add(MediaStore.Images.Media.DATE_ADDED)
        add(MediaStore.Images.Media.DATE_MODIFIED)
        add(MediaStore.Images.Media.SIZE)
        if (hasRelativePath) add(MediaStore.Images.Media.RELATIVE_PATH)
    }.toTypedArray()

    context.contentResolver.query(collection, projection, null, null, null)?.use { cursor ->
        val idCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
        val nameCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
        val bucketIdCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.BUCKET_ID)
        val bucketNameCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.BUCKET_DISPLAY_NAME)
        val takenCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_TAKEN)
        val addedCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_ADDED)
        val modifiedCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_MODIFIED)
        val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.SIZE)
        val pathCol = if (hasRelativePath) {
            cursor.getColumnIndexOrThrow(MediaStore.Images.Media.RELATIVE_PATH)
        } else {
            -1
        }
        while (cursor.moveToNext()) {
            val id = cursor.getLong(idCol)
            val taken = cursor.getLong(takenCol)
            val added = cursor.getLong(addedCol)
            result.add(
                MediaItem(
                    id = id,
                    uri = ContentUris.withAppendedId(collection, id).toString(),
                    name = cursor.getString(nameCol) ?: "",
                    bucketId = cursor.getLong(bucketIdCol),
                    bucketName = cursor.getString(bucketNameCol)?.takeIf { it.isNotBlank() } ?: "-",
                    path = if (pathCol >= 0) cursor.getString(pathCol) ?: "" else "",
                    date = if (taken > 0) taken else added * 1000,
                    modified = cursor.getLong(modifiedCol) * 1000,
                    size = cursor.getLong(sizeCol)
                )
            )
        }
    }
    return result
}
