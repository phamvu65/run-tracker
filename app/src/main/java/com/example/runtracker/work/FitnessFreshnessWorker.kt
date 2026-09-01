package com.example.runtracker.work

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.runtracker.domain.usecase.RecalculateFitnessFreshnessUseCase
import com.example.runtracker.domain.usecase.UpdatePerformancePredictionsUseCase
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.time.Duration
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZonedDateTime

/**
 * Job chạy mỗi đêm: cuộn CTL/ATL/TSB tới hôm nay (điền cả ngày nghỉ để fitness suy giảm).
 */
@HiltWorker
class FitnessFreshnessWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val recalculate: RecalculateFitnessFreshnessUseCase,
    private val updatePredictions: UpdatePerformancePredictionsUseCase,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result = try {
        recalculate(LocalDate.now())
        updatePredictions()
        Result.success()
    } catch (t: Throwable) {
        if (runAttemptCount < MAX_ATTEMPTS) Result.retry() else Result.failure()
    }

    companion object {
        private const val UNIQUE_NAME = "fitness-freshness"
        private const val MAX_ATTEMPTS = 3
        private val RUN_AT: LocalTime = LocalTime.of(3, 0)

        fun schedule(context: Context) {
            val now = ZonedDateTime.now()
            var next = now.with(RUN_AT)
            if (!next.isAfter(now)) next = next.plusDays(1)
            val initialDelay = Duration.between(now, next)

            val request = PeriodicWorkRequestBuilder<FitnessFreshnessWorker>(Duration.ofDays(1))
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
    }
}
