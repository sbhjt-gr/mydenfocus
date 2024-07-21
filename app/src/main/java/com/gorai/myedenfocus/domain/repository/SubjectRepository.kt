package com.gorai.myedenfocus.domain.repository

import com.gorai.myedenfocus.domain.model.Subject
import kotlinx.coroutines.flow.Flow

interface SubjectRepository {
    suspend fun upsertSubject(subject: Subject)
    fun getTotalSubjectCount(): Flow<Int>
    suspend fun getSubjectById(subjectId: Int): Subject?
    suspend fun deleteSubject(subjectInt: Int)
    fun getTotalGoalHours(): Flow<Float>
    fun getAllSubjects(): Flow<List<Subject>>
}