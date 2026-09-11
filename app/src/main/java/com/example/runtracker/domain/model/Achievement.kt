package com.example.runtracker.domain.model

/** Loại kỷ lục cá nhân đơn giản, so trong cùng [ActivityType]. */
enum class PersonalRecordKind { LONGEST_DISTANCE, FASTEST_PACE, LONGEST_DURATION }

/** Một kỷ lục cá nhân vừa đạt được sau khi chốt số liệu một buổi tập. */
data class PersonalRecord(
    val kind: PersonalRecordKind,
    val label: String,
    val valueText: String,
)
