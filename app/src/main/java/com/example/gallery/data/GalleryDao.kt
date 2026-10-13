package com.example.gallery.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface GalleryDao {

    @Query("SELECT * FROM tags")
    fun observeTags(): Flow<List<TagEntity>>

    @Query("SELECT * FROM folder_tags")
    fun observeFolderTags(): Flow<List<FolderTagCrossRef>>

    @Insert
    suspend fun insertTag(tag: TagEntity): Long

    @Query("SELECT * FROM tags WHERE name = :name COLLATE NOCASE LIMIT 1")
    suspend fun findTag(name: String): TagEntity?

    @Query("DELETE FROM tags WHERE id = :id")
    suspend fun deleteTag(id: Long)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertFolderTags(refs: List<FolderTagCrossRef>)

    @Query("DELETE FROM folder_tags WHERE bucketId = :bucketId")
    suspend fun clearFolderTags(bucketId: Long)
}
