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

/** Mọi migration của DB, theo thứ tự. */
val ALL_MIGRATIONS = arrayOf(MIGRATION_1_2, MIGRATION_2_3)
