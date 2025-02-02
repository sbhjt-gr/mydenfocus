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
    version = 6,
    exportSchema = true
)
@TypeConverters(ColorListConverter::class, Converters::class)
abstract class AppDatabase: RoomDatabase() {
    abstract fun subjectDao(): SubjectDao
    abstract fun taskDao(): TaskDao
    abstract fun sessionDao(): SessionDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    "ALTER TABLE Session ADD COLUMN topicName TEXT NOT NULL DEFAULT ''"
                )
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(database: SupportSQLiteDatabase) {
            }
        }

        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS session_new (
                        sessionId INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        sessionSubjectId INTEGER NOT NULL,
                        relatedToSubject TEXT NOT NULL,
                        topicId INTEGER,
                        topicName TEXT NOT NULL,
                        startTime INTEGER NOT NULL,
                        endTime INTEGER NOT NULL,
                        duration INTEGER NOT NULL,
                        plannedDuration INTEGER NOT NULL,
                        wasCompleted INTEGER NOT NULL DEFAULT 0,
                        focusScore INTEGER NOT NULL DEFAULT 0,
                        pauseCount INTEGER NOT NULL DEFAULT 0,
                        totalPauseDuration INTEGER NOT NULL DEFAULT 0,
                        notes TEXT NOT NULL DEFAULT '',
                        mood TEXT NOT NULL DEFAULT 'NEUTRAL',
                        productivityRating INTEGER NOT NULL DEFAULT 0,
                        tags TEXT NOT NULL DEFAULT '[]'
                    )
                """)

                database.execSQL("""
                    INSERT INTO session_new (
                        sessionId,
                        sessionSubjectId,
                        relatedToSubject,
                        topicName,
                        startTime,
                        duration
                    )
                    SELECT 
                        sessionId,
                        sessionSubjectId,
                        relatedToSubject,
                        topicName,
                        date,
                        duration
                    FROM Session
                """)

                database.execSQL("DROP TABLE Session")
                database.execSQL("ALTER TABLE session_new RENAME TO Session")
            }
        }

        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE Task ADD COLUMN completedAt INTEGER")
            }
        }
    }
}
