package com.gorai.myedenfocus.data.repository

import com.gorai.myedenfocus.data.local.SubjectDao
import com.gorai.myedenfocus.domain.model.Subject
import com.gorai.myedenfocus.domain.repository.SubjectRepository
import kotlinx.coroutines.flow.Flow

class SubjectRepositoryImpl(
    private val subjectDao: SubjectDao
): SubjectRepository {
    override suspend fun upsertSubject(subject: Subject) {
        subjectDao.upsertSubject(subject)
    }
    override fun getTotalSubjectCount(): Flow<Int> {
        TODO("Not yet implemented")
    }
    override suspend fun getSubjectById(subjectId: Int): Subject? {
        TODO("Not yet implemented")
    }
    override suspend fun deleteSubject(subjectInt: Int) {
        TODO("Not yet implemented")
    }
    override fun getTotalGoalHours(): Flow<Float> {
        TODO("Not yet implemented")
    }
    override fun getAllSubjects(): Flow<List<Subject>> {
        TODO("Not yet implemented")
    }

}