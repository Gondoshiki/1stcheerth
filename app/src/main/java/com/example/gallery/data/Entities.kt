package com.example.gallery.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "tags", indices = [Index(value = ["name"], unique = true)])
data class TagEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String
)

/** แท็กที่ติดกับโฟลเดอร์ในเครื่อง อ้างอิงด้วย bucketId ของ MediaStore */
@Entity(
    tableName = "folder_tags",
    primaryKeys = ["bucketId", "tagId"],
    foreignKeys = [
        ForeignKey(
            entity = TagEntity::class,
            parentColumns = ["id"],
            childColumns = ["tagId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["tagId"])]
)
data class FolderTagCrossRef(
    val bucketId: Long,
    val tagId: Long
)
