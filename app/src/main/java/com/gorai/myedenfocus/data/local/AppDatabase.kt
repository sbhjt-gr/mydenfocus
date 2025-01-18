package com.gorai.myedenfocus.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.gorai.myedenfocus.domain.model.Session
import com.gorai.myedenfocus.domain.model.Subject
import com.gorai.myedenfocus.domain.model.Task

@Database(
    entities = [Subject::class, Task::class, Session::class],
    version = 2,
    exportSchema = true
)
@TypeConverters(ColorListConverter::class)
abstract class AppDatabase: RoomDatabase() {
    abstract fun subjectDao(): SubjectDao
    abstract fun taskDao(): TaskDao
    abstract fun sessionDao(): SessionDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Add topicName column to Session table with default empty string
                database.execSQL(
                    "ALTER TABLE Session ADD COLUMN topicName TEXT NOT NULL DEFAULT ''"
                )
            }
        }
    }
}
