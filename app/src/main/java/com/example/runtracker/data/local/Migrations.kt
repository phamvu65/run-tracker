package com.example.runtracker.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * v1 → v2: thêm các cột weather overlay vào `activities` (ngoài `weatherTempC` đã có sẵn từ v1).
 */
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE activities ADD COLUMN weatherApparentTempC REAL")
        db.execSQL("ALTER TABLE activities ADD COLUMN weatherHumidityPct INTEGER")
        db.execSQL("ALTER TABLE activities ADD COLUMN weatherWindMps REAL")
        db.execSQL("ALTER TABLE activities ADD COLUMN weatherWindDirDeg INTEGER")
        db.execSQL("ALTER TABLE activities ADD COLUMN weatherCode INTEGER")
    }
}

/** v2 → v3: thêm cột số bước chân (`stepCount`) vào `activities`, đếm bằng cảm biến phần cứng. */
val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE activities ADD COLUMN stepCount INTEGER")
    }
}

/**
 * v3 → v4: thêm cột `locationName` (reverse-geocode) vào `activities` + bảng `best_efforts` mới
 * (thành tích chạy nhanh nhất theo cự ly chuẩn, xem `BestEffortEntity`/`EffortDistance`).
 */
val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE activities ADD COLUMN locationName TEXT")
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `best_efforts` (
                `id` TEXT NOT NULL,
                `userId` TEXT NOT NULL,
                `activityId` TEXT NOT NULL,
                `distance` TEXT NOT NULL,
                `elapsedSeconds` INTEGER NOT NULL,
                `achievedAt` INTEGER NOT NULL,
                `rankAtAchievement` INTEGER NOT NULL,
                `improvedBySecondsAtAchievement` INTEGER,
                PRIMARY KEY(`id`)
            )
            """.trimIndent(),
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_best_efforts_activityId` ON `best_efforts` (`activityId`)")
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_best_efforts_userId_distance` ON `best_efforts` (`userId`, `distance`)",
        )
    }
}

/**
 * v4 → v5: bảng `activity_records` mới — kỷ lục toàn-buổi-tập (pace nhanh nhất/quãng đường dài
 * nhất/độ cao lên nhiều nhất), xem `ActivityRecordEntity`/`ActivityRecordType`.
 */
val MIGRATION_4_5 = object : Migration(4, 5) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `activity_records` (
                `id` TEXT NOT NULL,
                `userId` TEXT NOT NULL,
                `activityId` TEXT NOT NULL,
                `recordType` TEXT NOT NULL,
                `value` REAL NOT NULL,
                `achievedAt` INTEGER NOT NULL,
                `rankAtAchievement` INTEGER NOT NULL,
                `improvedByAtAchievement` REAL,
                PRIMARY KEY(`id`)
            )
            """.trimIndent(),
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_activity_records_activityId` ON `activity_records` (`activityId`)")
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_activity_records_userId_recordType` ON `activity_records` (`userId`, `recordType`)",
        )
    }
}

/**
 * v5 → v6: bảng `unlocked_badges` mới — huy hiệu đã mở khoá (mốc quãng đường, chuỗi ngày, marathon
 * đầu tiên...), xem `UnlockedBadgeEntity`/`BadgeType`.
 */
val MIGRATION_5_6 = object : Migration(5, 6) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `unlocked_badges` (
                `id` TEXT NOT NULL,
                `userId` TEXT NOT NULL,
                `badgeType` TEXT NOT NULL,
                `activityId` TEXT NOT NULL,
                `unlockedAt` INTEGER NOT NULL,
                PRIMARY KEY(`id`)
            )
            """.trimIndent(),
        )
        db.execSQL(
            "CREATE UNIQUE INDEX IF NOT EXISTS `index_unlocked_badges_userId_badgeType` " +
                "ON `unlocked_badges` (`userId`, `badgeType`)",
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_unlocked_badges_activityId` ON `unlocked_badges` (`activityId`)")
    }
}

/** Mọi migration của DB, theo thứ tự. */
val ALL_MIGRATIONS = arrayOf(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6)
