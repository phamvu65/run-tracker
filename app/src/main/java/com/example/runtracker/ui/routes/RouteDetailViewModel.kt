package com.example.runtracker.ui.routes

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.runtracker.domain.model.Route
import com.example.runtracker.domain.repository.RouteRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

const val ARG_ROUTE_ID = "routeId"

@HiltViewModel
class RouteDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val routeRepository: RouteRepository,
) : ViewModel() {

    private val routeId: String = checkNotNull(savedStateHandle[ARG_ROUTE_ID])

    private val _route = MutableStateFlow<Route?>(null)
    val route: StateFlow<Route?> = _route.asStateFlow()

    var deleted by mutableStateOf(false)
        private set

    init {
        viewModelScope.launch { _route.value = routeRepository.getRoute(routeId) }
    }

    fun delete() {
        viewModelScope.launch {
            routeRepository.deleteRoute(routeId)
            deleted = true
        }
    }
}
