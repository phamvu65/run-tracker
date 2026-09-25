package com.example.runtracker.work

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.content.getSystemService
import androidx.hilt.work.HiltWorker
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.runtracker.MainActivity
import com.example.runtracker.core.LOCAL_USER_ID
import com.example.runtracker.core.hasNotificationPermission
import com.example.runtracker.domain.model.PlannedSession
import com.example.runtracker.domain.model.SessionType
import com.example.runtracker.domain.repository.ActivityRepository
import com.example.runtracker.domain.usecase.GenerateTrainingPlanUseCase
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.time.Duration
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime

/**
 * Job chạy mỗi ngày (buổi sáng): nếu hôm nay có buổi tập theo kế hoạch (khác NGHỈ) và chưa ghi
 * hoạt động nào hôm nay, bắn thông báo nhắc. Chỉ chạy khi bật trong Cài đặt (xem [AppSettingsStore]).
 */
@HiltWorker
class TrainingReminderWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val generateTrainingPlan: GenerateTrainingPlanUseCase,
    private val activityRepository: ActivityRepository,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result = try {
        val today = LocalDate.now()
        val session = generateTrainingPlan(today)?.sessionOn(today)
        if (session != null && session.type != SessionType.REST && !hasActivityToday(today)) {
            notify(session)
        }
        Result.success()
    } catch (t: Throwable) {
        if (runAttemptCount < MAX_ATTEMPTS) Result.retry() else Result.failure()
    }

    private suspend fun hasActivityToday(today: LocalDate): Boolean {
        val zoneId = ZoneId.systemDefault()
        val from = today.atStartOfDay(zoneId).toInstant()
        val to = today.plusDays(1).atStartOfDay(zoneId).toInstant()
        return activityRepository.getActivitiesBetween(LOCAL_USER_ID, from, to).isNotEmpty()
    }

    private fun notify(session: PlannedSession) {
        if (!applicationContext.hasNotificationPermission()) return
        val manager = applicationContext.getSystemService<NotificationManager>() ?: return
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, "Nhắc lịch tập", NotificationManager.IMPORTANCE_DEFAULT),
        )
        val contentIntent = PendingIntent.getActivity(
            applicationContext,
            0,
            Intent(applicationContext, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setContentTitle("Hôm nay có buổi ${session.type.label}")
            .setContentText(session.description)
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setContentIntent(contentIntent)
            .setAutoCancel(true)
            .build()
        manager.notify(NOTIF_ID, notification)
    }

    companion object {
        private const val UNIQUE_NAME = "training-reminder"
        private const val CHANNEL_ID = "training_reminder"
        private const val NOTIF_ID = 2001
        private const val MAX_ATTEMPTS = 3
        private val RUN_AT: LocalTime = LocalTime.of(6, 30)

        fun schedule(context: Context) {
            val now = ZonedDateTime.now()
            var next = now.with(RUN_AT)
            if (!next.isAfter(now)) next = next.plusDays(1)
            val initialDelay = Duration.between(now, next)

            val request = PeriodicWorkRequestBuilder<TrainingReminderWorker>(Duration.ofDays(1))
                .setInitialDelay(initialDelay)
                .setConstraints(
                    Constraints.Builder().setRequiredNetworkType(NetworkType.NOT_REQUIRED).build(),
                )
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                UNIQUE_NAME,
                ExistingPeriodicWorkPolicy.UPDATE,
                request,
            )
        }

        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(UNIQUE_NAME)
        }
    }
}
