package com.anodyne.app.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.anodyne.app.data.MediaRepository
import com.anodyne.app.data.AudioFolder
import com.anodyne.app.data.AudioFile

sealed interface HomeUiState {
    data object Loading : HomeUiState
    data class Success(
        val folders: List<AudioFolder>,
        val allAudios: List<AudioFile>,
        val isRefreshing: Boolean = false
    ) : HomeUiState
    data object Empty : HomeUiState
    data class Error(val message: String) : HomeUiState
}

class HomeViewModel(application: Application) : AndroidViewModel(application) {
    private val _uiState = MutableStateFlow<HomeUiState>(HomeUiState.Loading)
    val uiState: StateFlow<HomeUiState> = _uiState
    private val repo = MediaRepository(application)

    init {
        viewModelScope.launch {
            val cached = repo.getFullAudioData(forceRefresh = false)
            if (cached.all.isNotEmpty()) {
                _uiState.value = HomeUiState.Success(
                    folders = cached.folders,
                    allAudios = cached.all,
                )
                refreshCounts(forceRefresh = true)
            } else {
                refreshCounts(forceRefresh = false)
            }
        }
    }

    fun invalidateCache() {
        MediaRepository.invalidateCache()
        repo.invalidateDiskCache()
    }

    fun getApplicationContext() = getApplication<Application>().applicationContext

    fun refreshCounts(forceRefresh: Boolean = false) = viewModelScope.launch {
        val startMs = System.currentTimeMillis()
        val current = _uiState.value
        when (current) {
            is HomeUiState.Success -> {
                _uiState.value = current.copy(isRefreshing = true)
            }
            else -> {
                _uiState.value = HomeUiState.Loading
            }
        }
        val result = repo.getFullAudioData(forceRefresh = forceRefresh)

        val elapsed = System.currentTimeMillis() - startMs
        val minDuration = 400L
        if (elapsed < minDuration) delay(minDuration - elapsed)

        _uiState.value = when {
            result.all.isEmpty() && result.folders.isEmpty() -> HomeUiState.Empty
            else -> HomeUiState.Success(
                folders = result.folders,
                allAudios = result.all,
            )
        }
    }
}
