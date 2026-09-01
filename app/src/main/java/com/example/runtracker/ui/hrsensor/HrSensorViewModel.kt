package com.example.runtracker.ui.hrsensor

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.runtracker.data.health.BleHeartRateStore
import com.example.runtracker.domain.health.BleDevice
import com.example.runtracker.domain.health.LiveHeartRateSource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val SCAN_TIMEOUT_MS = 15_000L

@HiltViewModel
class HrSensorViewModel @Inject constructor(
    private val liveHeartRateSource: LiveHeartRateSource,
    private val store: BleHeartRateStore,
) : ViewModel() {

    val supported: Boolean = liveHeartRateSource.isSupported()
    val requiredPermissions: Array<String> = liveHeartRateSource.requiredPermissions()

    var savedName by mutableStateOf(store.savedName())
        private set
    var savedAddress by mutableStateOf(store.savedAddress())
        private set
    var scanning by mutableStateOf(false)
        private set
    var previewBpm by mutableStateOf<Int?>(null)
        private set
    var error by mutableStateOf<String?>(null)
        private set

    private val _devices = MutableStateFlow<List<BleDevice>>(emptyList())
    val devices: StateFlow<List<BleDevice>> = _devices.asStateFlow()

    private var scanJob: Job? = null
    private var connectJob: Job? = null

    fun startScan() {
        scanJob?.cancel()
        _devices.value = emptyList()
        error = null
        scanning = true
        scanJob = viewModelScope.launch {
            liveHeartRateSource.scan()
                .catch { error = it.message; scanning = false }
                .collect { device ->
                    _devices.update { (it + device).distinctBy { d -> d.address } }
                }
        }
        viewModelScope.launch {
            delay(SCAN_TIMEOUT_MS)
            stopScan()
        }
    }

    fun stopScan() {
        scanJob?.cancel()
        scanning = false
    }

    fun select(device: BleDevice) {
        stopScan()
        store.save(device.address, device.name)
        savedName = device.name
        savedAddress = device.address
        startPreview(address = device.address)
    }

    fun forget() {
        connectJob?.cancel()
        store.clear()
        savedName = null
        savedAddress = null
        previewBpm = null
    }

    fun startPreview(address: String? = null) {
        val target = address ?: savedAddress ?: return
        connectJob?.cancel()
        previewBpm = null
        connectJob = viewModelScope.launch {
            liveHeartRateSource.connect(target)
                .catch { error = it.message }
                .collect { previewBpm = it }
        }
    }

    override fun onCleared() {
        scanJob?.cancel()
        connectJob?.cancel()
    }
}
