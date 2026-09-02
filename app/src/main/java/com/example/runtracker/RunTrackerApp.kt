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
        // osmdroid cần User-Agent riêng để tuân thủ chính sách tile của OSM.
        OsmConfiguration.getInstance().apply {
            userAgentValue = BuildConfig.APPLICATION_ID
            osmdroidBasePath = cacheDir
            osmdroidTileCache = cacheDir.resolve("osmdroid-tiles")
        }
        FitnessFreshnessWorker.schedule(this)
    }
}
