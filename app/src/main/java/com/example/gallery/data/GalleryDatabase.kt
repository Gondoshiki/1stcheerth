package com.example.gallery.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [TagEntity::class, FolderTagCrossRef::class],
    version = 2,
    exportSchema = false
)
abstract class GalleryDatabase : RoomDatabase() {

    abstract fun dao(): GalleryDao

    companion object {
        @Volatile
        private var instance: GalleryDatabase? = null

        fun get(context: Context): GalleryDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    GalleryDatabase::class.java,
                    "gallery.db"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { instance = it }
            }
    }
}
