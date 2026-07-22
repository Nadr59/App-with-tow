package com.apppair.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.apppair.data.model.PermissionStatus
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
class MainViewModel @Inject constructor(private val repository: AppRepository) : ViewModel() {
    private val _uiState = MutableStateFlow(AppPairUiState())
    val uiState: StateFlow<AppPairUiState> = _uiState.asStateFlow()

    init { try { loadApps(); observeServiceState(); refreshPermissions() } catch (e: Exception) { _uiState.value = _uiState.value.copy(isLoading = false, error = e.message) } }

    private fun loadApps() { viewModelScope.launch { try { _uiState.value = _uiState.value.copy(isLoading = false, installedApps = repository.getInstalledApps()) } catch (e: Exception) { _uiState.value = _uiState.value.copy(isLoading = false, error = e.message) } } }
    private fun observeServiceState() { viewModelScope.launch { try { repository.serviceActive.collect { _uiState.value = _uiState.value.copy(isServiceRunning = it) } } catch (_: Exception) {} } }
    fun refreshPermissions() { try { _uiState.value = _uiState.value.copy(permissions = repository.checkPermissions()) } catch (_: Exception) {} }
    fun selectApp1(app: InstalledApp) { _uiState.value = _uiState.value.copy(selectedApp1 = app); saveIfComplete() }
    fun selectApp2(app: InstalledApp) { _uiState.value = _uiState.value.copy(selectedApp2 = app); saveIfComplete() }
    private fun saveIfComplete() { val s = _uiState.value; if (s.selectedApp1 != null && s.selectedApp2 != null) { viewModelScope.launch { try { repository.saveSelectedApps(s.selectedApp1!!.packageName, s.selectedApp2!!.packageName) } catch (_: Exception) {} } } }
    fun clearSelection() { _uiState.value = _uiState.value.copy(selectedApp1 = null, selectedApp2 = null); viewModelScope.launch { try { repository.clearSelection() } catch (_: Exception) {} } }
    fun setServiceRunning(active: Boolean) { viewModelScope.launch { try { repository.setServiceActive(active) } catch (_: Exception) {} } }
}
