package com.gorai.myedenfocus.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.gorai.myedenfocus.domain.model.Session
import com.gorai.myedenfocus.domain. model.Subject
import com.gorai.myedenfocus.domain.model.Task

@Database(
    entities = [Subject::class, Task::class, Session::class],
    version = 1
)
@TypeConverters(ColorListConverter::class)
abstract class AppDatabase: RoomDatabase() {
    abstract fun subjectDao(): SubjectDao
    abstract fun taskDao(): TaskDao
    abstract fun sessionDao(): SessionDao
}
