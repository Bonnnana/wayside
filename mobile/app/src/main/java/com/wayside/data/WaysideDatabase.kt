package com.wayside.data

import androidx.room.Database
import androidx.room.RoomDatabase
import com.wayside.data.entities.SavedPlaceEntity
import com.wayside.data.repositories.SavedPlaceDao

@Database(
    entities = [SavedPlaceEntity::class],
    version = 2,
    exportSchema = false,
)
abstract class WaysideDatabase : RoomDatabase() {
    abstract fun savedPlaceDao(): SavedPlaceDao

    companion object {
        const val NAME = "wayside.db"
    }
}
