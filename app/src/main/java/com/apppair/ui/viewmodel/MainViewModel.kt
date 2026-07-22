package com.apppair.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.apppair.data.model.PermissionStatus
import com.apppair.data.model.SelectedAppPair
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
    val permissions: PermissionStatus = PermissionStatus(),
    val error: String? = null
)

@HiltViewModel
class MainViewModel @Inject constructor(
    private val repository: AppRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AppPairUiState())
    val uiState: StateFlow<AppPairUiState> = _uiState.asStateFlow()

    init {
        loadApps()
        observeServiceState()
        checkPermissions()
    }

    private fun loadApps() {
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

    private fun observeServiceState() {
        viewModelScope.launch {
            repository.serviceActive.collect { active ->
                _uiState.value = _uiState.value.copy(isServiceRunning = active)
            }
        }
    }

    fun checkPermissions() {
        val status = repository.checkPermissions()
        _uiState.value = _uiState.value.copy(permissions = status)
    }

    fun selectApp1(app: InstalledApp) {
        _uiState.value = _uiState.value.copy(selectedApp1 = app)
        saveIfComplete()
    }

    fun selectApp2(app: InstalledApp) {
        _uiState.value = _uiState.value.copy(selectedApp2 = app)
        saveIfComplete()
    }

    private fun saveIfComplete() {
        val s = _uiState.value
        if (s.selectedApp1 != null && s.selectedApp2 != null) {
            viewModelScope.launch {
                repository.saveSelectedApps(
                    s.selectedApp1!!.packageName,
                    s.selectedApp2!!.packageName
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

    fun setServiceRunning(active: Boolean) {
        viewModelScope.launch {
            repository.setServiceActive(active)
        }
    }
}
