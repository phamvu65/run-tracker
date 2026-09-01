package com.example.runtracker.data.mapper

import com.example.runtracker.data.local.entity.SegmentEffortEntity
import com.example.runtracker.data.local.entity.SegmentEntity
import com.example.runtracker.domain.geo.PolylineCodec
import com.example.runtracker.domain.model.GeoPoint
import com.example.runtracker.domain.model.Segment
import com.example.runtracker.domain.model.SegmentEffort
import java.time.Instant

fun SegmentEntity.toDomain(): Segment = Segment(
    id = id,
    name = name,
    start = GeoPoint(startLat, startLng),
    end = GeoPoint(endLat, endLng),
    distanceMeters = distanceMeters,
    avgGrade = avgGrade,
    points = PolylineCodec.decode(polyline),
    createdByUserId = createdByUserId,
    isPublic = isPublic,
)

fun Segment.toEntity(): SegmentEntity = SegmentEntity(
    id = id,
    name = name,
    startLat = start.latitude, startLng = start.longitude,
    endLat = end.latitude, endLng = end.longitude,
    distanceMeters = distanceMeters,
    avgGrade = avgGrade,
    polyline = PolylineCodec.encode(points),
    createdByUserId = createdByUserId,
    isPublic = isPublic,
)

fun SegmentEffortEntity.toDomain(rank: Int? = null): SegmentEffort = SegmentEffort(
    id = id,
    segmentId = segmentId,
    activityId = activityId,
    userId = userId,
    elapsedSeconds = elapsedSeconds,
    startTime = Instant.ofEpochMilli(startTime),
    avgHeartRate = avgHeartRate,
    rank = rank ?: this.rank,
)

fun SegmentEffort.toEntity(): SegmentEffortEntity = SegmentEffortEntity(
    id = id,
    segmentId = segmentId,
    activityId = activityId,
    userId = userId,
    elapsedSeconds = elapsedSeconds,
    startTime = startTime.toEpochMilli(),
    avgHeartRate = avgHeartRate,
    rank = rank,
)
