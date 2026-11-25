package com.example.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.PreferencesRepository
import com.example.data.Story
import com.example.data.StoryRepository
import com.example.data.TextScalePreset
import com.example.data.ThemePreset
import com.example.firebase.unwrapFirestoreErrorMessage
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

class DashboardViewModel(
    private val repository: StoryRepository,
    private val preferencesRepository: PreferencesRepository,
    private val currentUserId: String,
) : ViewModel() {

    val storiesState: StateFlow<UiState<List<Story>>> = repository.observeStories(currentUserId)
        .map<List<Story>, UiState<List<Story>>> { UiState.Success(it) }
        .catch { error ->
            val firestoreError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (firestoreError == null && error is CancellationException) throw error
            Log.w(TAG, "Error observing stories", firestoreError ?: error)
            emit(UiState.Error(unwrapFirestoreErrorMessage(error, "Failed to load campaigns.")))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = UiState.Loading
        )

    private val _actionMessage = MutableStateFlow<String?>(null)
    val actionMessage: StateFlow<String?> = _actionMessage.asStateFlow()

    private val _isBusy = MutableStateFlow(false)
    val isBusy: StateFlow<Boolean> = _isBusy.asStateFlow()

    init {
        viewModelScope.launch {
            repository.upsertCurrentUserProfile()
        }
    }

    fun clearMessage() {
        _actionMessage.value = null
    }

    fun createStory(
        title: String,
        description: String,
        systemName: String,
        onCreated: (String) -> Unit,
    ) {
        viewModelScope.launch {
            _isBusy.value = true
            val result = repository.createStory(title, description, systemName)
            _isBusy.value = false
            result.onSuccess { storyId ->
                onCreated(storyId)
            }.onFailure { err ->
                _actionMessage.value = unwrapFirestoreErrorMessage(err, "Could not create campaign.")
            }
        }
    }

    fun joinStory(code: String, onJoined: (String) -> Unit) {
        viewModelScope.launch {
            _isBusy.value = true
            val result = repository.joinStoryByCode(code)
            _isBusy.value = false
            result.onSuccess { storyId ->
                onJoined(storyId)
            }.onFailure { err ->
                _actionMessage.value = unwrapFirestoreErrorMessage(err, "Could not join campaign.")
            }
        }
    }

    fun deleteStory(story: Story) {
        viewModelScope.launch {
            _isBusy.value = true
            val result = repository.deleteStory(story)
            _isBusy.value = false
            result.onFailure { err ->
                _actionMessage.value = unwrapFirestoreErrorMessage(err, "Failed to delete campaign.")
            }
        }
    }

    fun setThemePreset(preset: ThemePreset) {
        viewModelScope.launch { preferencesRepository.setThemePreset(preset) }
    }

    fun setCustomAccent(argb: Long?) {
        viewModelScope.launch { preferencesRepository.setCustomAccent(argb) }
    }

    fun setTextScale(scale: TextScalePreset) {
        viewModelScope.launch { preferencesRepository.setTextScale(scale) }
    }

    fun setLanguage(code: String) {
        viewModelScope.launch { preferencesRepository.setLanguage(code) }
    }

    private companion object {
        const val TAG = "DashboardVM"
    }
}
