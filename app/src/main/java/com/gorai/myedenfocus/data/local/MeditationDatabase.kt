package com.gorai.myedenfocus.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.gorai.myedenfocus.data.local.dao.MeditationSessionDao
import com.gorai.myedenfocus.data.local.entity.MeditationSessionEntity
import com.gorai.myedenfocus.data.local.converter.DateTimeConverter

@Database(
    entities = [MeditationSessionEntity::class],
    version = 1
)
@TypeConverters(DateTimeConverter::class)
abstract class MeditationDatabase : RoomDatabase() {
    abstract val meditationSessionDao: MeditationSessionDao
} 