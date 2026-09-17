package com.wayside.di

import android.content.Context
import androidx.room.Room
import com.wayside.data.WaysideDatabase
import com.wayside.data.repositories.RoomSavedPlaceRepository
import com.wayside.data.repositories.SavedPlaceDao
import com.wayside.data.repositories.SavedPlaceRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): WaysideDatabase =
        Room.databaseBuilder(context, WaysideDatabase::class.java, WaysideDatabase.NAME)
            // Saved places can be re-saved; losing them on a schema change beats shipping a
            // migration for a table that is a cache of the user's taps.
            .fallbackToDestructiveMigration(dropAllTables = true)
            .build()

    @Provides
    fun provideSavedPlaceDao(database: WaysideDatabase): SavedPlaceDao = database.savedPlaceDao()
}

@Module
@InstallIn(SingletonComponent::class)
@Suppress("unused")
abstract class DatabaseBindingsModule {

    @Binds
    @Singleton
    abstract fun bindSavedPlaceRepository(
        impl: RoomSavedPlaceRepository,
    ): SavedPlaceRepository
}
