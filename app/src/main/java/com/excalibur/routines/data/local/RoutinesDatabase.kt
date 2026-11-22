package com.excalibur.routines.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.excalibur.routines.data.local.dao.RoutineDao
import com.excalibur.routines.data.local.dao.ScheduleDao
import com.excalibur.routines.data.local.entity.RoutineEntity
import com.excalibur.routines.data.local.entity.RoutineScheduleEntity
import com.excalibur.routines.data.local.entity.RoutineStepEntity

@Database(
    entities = [
        RoutineEntity::class,
        RoutineStepEntity::class,
        RoutineScheduleEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class RoutinesDatabase : RoomDatabase() {
    abstract fun routineDao(): RoutineDao
    abstract fun scheduleDao(): ScheduleDao
}
