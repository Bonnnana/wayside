package com.wayside.data.repositories

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.wayside.data.entities.SavedPlaceEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SavedPlaceDao {

    /** Newest first — the order the Saved tab shows them in. */
    @Query("SELECT * FROM saved_places ORDER BY savedAtUtc DESC")
    fun observeAll(): Flow<List<SavedPlaceEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun save(place: SavedPlaceEntity)

    @Query("DELETE FROM saved_places WHERE id = :placeId")
    suspend fun delete(placeId: String)

    @Query("DELETE FROM saved_places")
    suspend fun clear()
}
