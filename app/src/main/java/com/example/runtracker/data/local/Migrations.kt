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

/** Mọi migration của DB, theo thứ tự. */
val ALL_MIGRATIONS = arrayOf(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
