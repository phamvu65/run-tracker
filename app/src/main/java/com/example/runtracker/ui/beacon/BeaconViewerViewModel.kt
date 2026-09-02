package com.example.runtracker.ui.beacon

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.runtracker.domain.beacon.BeaconCode
import com.example.runtracker.domain.beacon.LiveLocationTransport
import com.example.runtracker.domain.model.BeaconSnapshot
import com.example.runtracker.domain.model.BeaconStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import java.time.Duration
import java.time.Instant
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class BeaconViewerViewModel @Inject constructor(
    private val transport: LiveLocationTransport,
) : ViewModel() {

    private val _code = MutableStateFlow<String?>(null)
    val code: StateFlow<String?> = _code.asStateFlow()

    var input by mutableStateOf("")
        private set
    var error by mutableStateOf<String?>(null)
        private set

    fun onInputChange(value: String) {
        input = value
        error = null
    }

    val snapshot: StateFlow<BeaconSnapshot?> = _code
        .flatMapLatest { c -> if (c == null) flowOf(null) else transport.observe(c) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val status: StateFlow<BeaconStatus> = combine(snapshot, ticker()) { snap, _ ->
        statusOf(snap)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), BeaconStatus.WAITING)

    fun follow() {
        val normalized = BeaconCode.normalize(input)
        if (!BeaconCode.isValid(normalized)) {
            error = "Mã beacon gồm ${BeaconCode.LENGTH} ký tự."
            return
        }
        error = null
        _code.value = normalized
    }

    fun stopFollowing() {
        _code.value = null
        input = ""
    }

    private fun ticker() = flow {
        while (true) {
            emit(Unit)
            delay(3_000)
        }
    }

    private fun statusOf(snap: BeaconSnapshot?): BeaconStatus = when {
        snap?.latest == null -> if (snap?.ended == true) BeaconStatus.ENDED else BeaconStatus.WAITING
        snap.ended -> BeaconStatus.ENDED
        Duration.between(snap.latest.timestamp, Instant.now()).seconds > STALE_AFTER_SECONDS ->
            BeaconStatus.STALE
        else -> BeaconStatus.LIVE
    }

    private companion object {
        const val STALE_AFTER_SECONDS = 30L
    }
}
