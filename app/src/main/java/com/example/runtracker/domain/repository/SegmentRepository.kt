package com.example.runtracker.domain.repository

import com.example.runtracker.domain.model.Segment
import com.example.runtracker.domain.model.SegmentEffort
import kotlinx.coroutines.flow.Flow

interface SegmentRepository {

    fun observeSegments(): Flow<List<Segment>>
    suspend fun getSegment(id: String): Segment?
    suspend fun getAllSegments(): List<Segment>
    suspend fun upsertSegment(segment: Segment)

    /** Leaderboard đầy đủ, đã sắp theo thời gian và gán `rank` (1 = nhanh nhất). */
    fun observeLeaderboard(segmentId: String): Flow<List<SegmentEffort>>

    suspend fun getEffortsForActivity(activityId: String): List<SegmentEffort>
    fun observeEffortsForActivity(activityId: String): Flow<List<SegmentEffort>>
    suspend fun addEffort(effort: SegmentEffort)
}
