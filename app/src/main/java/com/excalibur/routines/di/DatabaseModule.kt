package com.excalibur.routines.di

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.excalibur.routines.data.local.RoutinesDatabase
import com.excalibur.routines.data.local.dao.RoutineDao
import com.excalibur.routines.data.local.dao.ScheduleDao
import com.excalibur.routines.data.local.dao.UserStatsDao
import com.excalibur.routines.data.repository.RoutineRepositoryImpl
import com.excalibur.routines.data.repository.ScheduleRepositoryImpl
import com.excalibur.routines.data.repository.UserStatsRepositoryImpl
import com.excalibur.routines.domain.repository.RoutineRepository
import com.excalibur.routines.domain.repository.ScheduleRepository
import com.excalibur.routines.domain.repository.UserStatsRepository
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

    private val MIGRATION_3_4 = object : Migration(3, 4) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `user_stats` (
                    `id` INTEGER NOT NULL PRIMARY KEY,
                    `totalPoints` INTEGER NOT NULL,
                    `currentStreak` INTEGER NOT NULL,
                    `bestStreak` INTEGER NOT NULL,
                    `lastCompletedDate` TEXT,
                    `routinesCompletedCount` INTEGER NOT NULL
                )
                """.trimIndent()
            )
        }
    }

    @Provides
    @Singleton
    fun provideRoutinesDatabase(
        @ApplicationContext context: Context
    ): RoutinesDatabase {
        return Room.databaseBuilder(
            context,
            RoutinesDatabase::class.java,
            "routines.db"
        )
            .addMigrations(MIGRATION_3_4)
            .build()
    }

    @Provides
    fun provideRoutineDao(database: RoutinesDatabase): RoutineDao {
        return database.routineDao()
    }

    @Provides
    fun provideScheduleDao(database: RoutinesDatabase): ScheduleDao {
        return database.scheduleDao()
    }

    @Provides
    fun provideUserStatsDao(database: RoutinesDatabase): UserStatsDao {
        return database.userStatsDao()
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

    @Binds
    @Singleton
    abstract fun bindUserStatsRepository(
        impl: UserStatsRepositoryImpl
    ): UserStatsRepository
}
