package com.apppair.ui.selection

import android.content.Context
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.apppair.data.model.AppInfo
import com.apppair.data.model.SelectedAppPair
import com.apppair.data.repository.AppRepository
import com.apppair.service.AppPairForegroundService
import com.apppair.utils.NotificationHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AppSelectionViewModel @Inject constructor(
    private val repository: AppRepository
) : ViewModel() {

    private val _allApps = MutableStateFlow<List<AppInfo>>(emptyList())
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    val selectedAppPair: StateFlow<SelectedAppPair> = repository.selectedAppPairFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = SelectedAppPair()
        )

    val filteredApps: StateFlow<List<AppInfo>> = combine(_allApps, _searchQuery, selectedAppPair) { apps, query, selection ->
        val q = query.trim().lowercase()
        apps.map { app ->
            app.copy(
                isSelectedAsA = app.packageName == selection.packageA,
                isSelectedAsB = app.packageName == selection.packageB
            )
        }.filter { app ->
            if (q.isEmpty()) true
            else app.label.lowercase().contains(q) || app.packageName.lowercase().contains(q)
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    init {
        loadApps()
    }

    fun loadApps() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val apps = repository.getInstalledApps()
                _allApps.value = apps
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun onAppClicked(app: AppInfo) {
        val current = selectedAppPair.value
        val pkg = app.packageName

        viewModelScope.launch {
            when {
                current.packageA == pkg -> {
                    // Deselect A
                    repository.selectApps("", current.packageB ?: "")
                }
                current.packageB == pkg -> {
                    // Deselect B
                    repository.selectApps(current.packageA ?: "", "")
                }
                current.packageA.isNullOrBlank() -> {
                    // Assign to A
                    repository.selectApps(pkg, current.packageB ?: "")
                }
                current.packageB.isNullOrBlank() -> {
                    // Assign to B
                    repository.selectApps(current.packageA ?: "", pkg)
                }
                else -> {
                    // Both already assigned -> Replace B with new selection
                    repository.selectApps(current.packageA, pkg)
                }
            }
        }
    }

    fun selectAsSlotA(app: AppInfo) {
        val current = selectedAppPair.value
        val pkg = app.packageName
        viewModelScope.launch {
            if (current.packageB == pkg) {
                // Swap if already B
                repository.selectApps(pkg, current.packageA ?: "")
            } else {
                repository.selectApps(pkg, current.packageB ?: "")
            }
        }
    }

    fun selectAsSlotB(app: AppInfo) {
        val current = selectedAppPair.value
        val pkg = app.packageName
        viewModelScope.launch {
            if (current.packageA == pkg) {
                // Swap if already A
                repository.selectApps(current.packageB ?: "", pkg)
            } else {
                repository.selectApps(current.packageA ?: "", pkg)
            }
        }
    }

    fun startSwitcherService(context: Context) {
        val intent = Intent(context, AppPairForegroundService::class.java)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            context.startForegroundService(intent)
        } else {
            context.startService(intent)
        }
    }

    fun stopSwitcherService(context: Context) {
        val intent = Intent(context, AppPairForegroundService::class.java).apply {
            action = NotificationHelper.ACTION_STOP_SERVICE
        }
        context.startService(intent)
    }
}
