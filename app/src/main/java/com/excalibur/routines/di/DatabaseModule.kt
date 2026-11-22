package com.excalibur.routines.di

import android.content.Context
import androidx.room.Room
import com.excalibur.routines.data.local.RoutinesDatabase
import com.excalibur.routines.data.local.dao.RoutineDao
import com.excalibur.routines.data.local.dao.ScheduleDao
import com.excalibur.routines.data.repository.RoutineRepositoryImpl
import com.excalibur.routines.data.repository.ScheduleRepositoryImpl
import com.excalibur.routines.domain.repository.RoutineRepository
import com.excalibur.routines.domain.repository.ScheduleRepository
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
    fun provideRoutinesDatabase(
        @ApplicationContext context: Context
    ): RoutinesDatabase {
        return Room.databaseBuilder(
            context,
            RoutinesDatabase::class.java,
            "routines.db"
        ).fallbackToDestructiveMigration().build()
    }

    @Provides
    fun provideRoutineDao(database: RoutinesDatabase): RoutineDao {
        return database.routineDao()
    }

    @Provides
    fun provideScheduleDao(database: RoutinesDatabase): ScheduleDao {
        return database.scheduleDao()
    }
}

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindRoutineRepository(
        impl: RoutineRepositoryImpl
    ): RoutineRepository

    @Binds
    @Singleton
    abstract fun bindScheduleRepository(
        impl: ScheduleRepositoryImpl
    ): ScheduleRepository
}
