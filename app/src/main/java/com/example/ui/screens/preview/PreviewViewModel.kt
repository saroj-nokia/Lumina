package com.example.ui.screens.preview

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.repository.WallpaperRepository
import com.example.domain.model.Wallpaper
import com.example.domain.model.WallpaperDestination
import com.example.util.ImageDownloader
import com.example.util.WallpaperManagerHelper
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class PreviewUiState(
    val wallpaper: Wallpaper? = null,
    val isFavorite: Boolean = false,
    val isApplyingWallpaper: Boolean = false,
    val isDownloading: Boolean = false,
    val isChromeVisible: Boolean = true,
    val showSetSheet: Boolean = false
)

sealed interface PreviewEvent {
    data class ShowSnackbar(val message: String, val isError: Boolean = false) : PreviewEvent
}

class PreviewViewModel(
    private val repository: WallpaperRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(PreviewUiState())
    val uiState: StateFlow<PreviewUiState> = _uiState.asStateFlow()

    private val _events = Channel<PreviewEvent>(Channel.BUFFERED)
    val events: Flow<PreviewEvent> = _events.receiveAsFlow()

    private var favoriteJob: Job? = null

    fun setWallpaper(wallpaper: Wallpaper) {
        if (_uiState.value.wallpaper?.id == wallpaper.id) return

        favoriteJob?.cancel()

        _uiState.update {
            it.copy(
                wallpaper = wallpaper,
                isFavorite = wallpaper.isFavorite,
                isChromeVisible = true,
                showSetSheet = false
            )
        }

        favoriteJob = viewModelScope.launch {
            repository.isFavorite(wallpaper.id).collect { isFav ->
                _uiState.update { it.copy(isFavorite = isFav) }
            }
        }
    }

    fun toggleChrome() {
        _uiState.update { it.copy(isChromeVisible = !it.isChromeVisible) }
    }

    fun openSetSheet() {
        _uiState.update { it.copy(showSetSheet = true) }
    }

    fun dismissSetSheet() {
        _uiState.update { it.copy(showSetSheet = false) }
    }

    fun toggleFavorite() {
        val wp = _uiState.value.wallpaper ?: return
        viewModelScope.launch {
            val nowFav = repository.toggleFavorite(wp)
            _uiState.update { it.copy(isFavorite = nowFav) }
            _events.send(
                PreviewEvent.ShowSnackbar(
                    if (nowFav) "Added to Favorites ❤️" else "Removed from Favorites"
                )
            )
        }
    }

    fun applyWallpaper(context: Context, destination: WallpaperDestination) {
        val wp = _uiState.value.wallpaper ?: return
        val appContext = context.applicationContext
        viewModelScope.launch {
            _uiState.update { it.copy(isApplyingWallpaper = true) }
            val urlToApply = wp.fullUrl
            val result = WallpaperManagerHelper.applyWallpaper(
                context = appContext,
                imageUrl = urlToApply,
                destination = destination
            )
            _uiState.update { it.copy(isApplyingWallpaper = false, showSetSheet = false) }

            if (result.isSuccess) {
                _events.send(PreviewEvent.ShowSnackbar("Wallpaper set successfully! 🎉"))
            } else {
                _events.send(
                    PreviewEvent.ShowSnackbar(
                        "Failed to set wallpaper: ${result.exceptionOrNull()?.localizedMessage ?: "Unknown error"}",
                        isError = true
                    )
                )
            }
        }
    }

    fun downloadWallpaper(context: Context) {
        val wp = _uiState.value.wallpaper ?: return
        val appContext = context.applicationContext
        viewModelScope.launch {
            _uiState.update { it.copy(isDownloading = true) }
            val result = ImageDownloader.saveToGallery(
                context = appContext,
                imageUrl = wp.fullUrl,
                photoId = wp.id
            )
            _uiState.update { it.copy(isDownloading = false) }

            if (result.isSuccess) {
                _events.send(PreviewEvent.ShowSnackbar("Saved to Pictures/Wallpapers! 📸"))
            } else {
                _events.send(
                    PreviewEvent.ShowSnackbar(
                        "Download failed: ${result.exceptionOrNull()?.localizedMessage ?: "Unknown error"}",
                        isError = true
                    )
                )
            }
        }
    }

    class Factory(private val repository: WallpaperRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return PreviewViewModel(repository) as T
        }
    }
}
