package com.apppair.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.apppair.data.repository.AppRepository
import com.apppair.data.repository.InstalledApp
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AppPairUiState(
    val isLoading: Boolean = true,
    val installedApps: List<InstalledApp> = emptyList(),
    val selectedApp1: InstalledApp? = null,
    val selectedApp2: InstalledApp? = null,
    val isServiceRunning: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class MainViewModel @Inject constructor(
    private val repository: AppRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AppPairUiState())
    val uiState: StateFlow<AppPairUiState> = _uiState.asStateFlow()

    init {
        loadInstalledApps()
    }

    private fun loadInstalledApps() {
        viewModelScope.launch {
            try {
                val apps = repository.getInstalledApps()
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    installedApps = apps,
                    error = null
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Failed to load apps: ${e.message}"
                )
            }
        }
    }

    fun selectApp1(app: InstalledApp) {
        _uiState.value = _uiState.value.copy(selectedApp1 = app)
        checkAndSave()
    }

    fun selectApp2(app: InstalledApp) {
        _uiState.value = _uiState.value.copy(selectedApp2 = app)
        checkAndSave()
    }

    private fun checkAndSave() {
        val state = _uiState.value
        if (state.selectedApp1 != null && state.selectedApp2 != null) {
            viewModelScope.launch {
                repository.saveSelection(
                    state.selectedApp1.packageName,
                    state.selectedApp2.packageName
                )
            }
        }
    }

    fun clearSelection() {
        _uiState.value = _uiState.value.copy(
            selectedApp1 = null,
            selectedApp2 = null
        )
        viewModelScope.launch {
            repository.clearSelection()
        }
    }

    fun setServiceRunning(running: Boolean) {
        _uiState.value = _uiState.value.copy(isServiceRunning = running)
    }
}
