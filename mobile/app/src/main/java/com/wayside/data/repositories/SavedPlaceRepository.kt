package com.wayside.data.repositories

import com.wayside.data.Place
import com.wayside.data.entities.toPlace
import com.wayside.data.entities.toSavedEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/** Saved places, as the rest of the app wants them — no Room types past this line. */
interface SavedPlaceRepository {
    fun observeSaved(): Flow<List<Place>>

    suspend fun save(place: Place)

    suspend fun remove(placeId: String)

    suspend fun clear()
}

@Singleton
class RoomSavedPlaceRepository @Inject constructor(
    private val dao: SavedPlaceDao,
) : SavedPlaceRepository {

    override fun observeSaved(): Flow<List<Place>> =
        dao.observeAll().map { saved -> saved.map { it.toPlace() } }

    override suspend fun save(place: Place) = dao.save(place.toSavedEntity())

    override suspend fun remove(placeId: String) = dao.delete(placeId)

    override suspend fun clear() = dao.clear()
}
