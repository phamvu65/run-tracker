package com.example.runtracker

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.example.runtracker.work.FitnessFreshnessWorker
import dagger.hilt.android.HiltAndroidApp
import org.osmdroid.config.Configuration as OsmConfiguration
import javax.inject.Inject

@HiltAndroidApp
class RunTrackerApp : Application(), Configuration.Provider {

    @Inject lateinit var workerFactory: HiltWorkerFactory

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder().setWorkerFactory(workerFactory).build()

    override fun onCreate() {
        super.onCreate()
        // osmdroid cần User-Agent riêng để tuân thủ chính sách tile. KHÔNG để mặc định
        // theo applicationId "com.example.*" (một số máy chủ tile chặn tiền tố mẫu).
        // Nguồn tile mặc định xem [DefaultTileSource] — không dùng openstreetmap.org
        // vì nhiều ISP ở VN đầu độc DNS domain đó về 127.0.0.1.
        OsmConfiguration.getInstance().apply {
            userAgentValue = "RunTracker-Android/${BuildConfig.VERSION_NAME}"
            osmdroidBasePath = cacheDir
            osmdroidTileCache = cacheDir.resolve("osmdroid-tiles").apply { mkdirs() }
        }
        FitnessFreshnessWorker.schedule(this)
    }
}
