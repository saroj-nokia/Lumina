package com.example.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.repository.WallpaperRepository
import com.example.data.repository.WallpaperResult
import com.example.domain.model.Wallpaper
import com.example.domain.model.WallpaperCategory
import com.example.domain.model.WallpaperSorting
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

data class HomeUiState(
    val isLoading: Boolean = true,
    val isLoadingMore: Boolean = false,
    val wallpapers: List<Wallpaper> = emptyList(),
    val selectedCategory: WallpaperCategory = WallpaperCategory.FEATURED,
    val selectedSorting: WallpaperSorting = WallpaperSorting.TOPLIST,
    val searchQuery: String = "",
    val hasMore: Boolean = true,
    val currentPage: Int = 1,
    val lastPage: Int = 1,
    val randomSeed: String = UUID.randomUUID().toString().take(6),
    val error: String? = null,
    val isRateLimited: Boolean = false
)

class HomeViewModel(
    private val repository: WallpaperRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private var searchJob: Job? = null

    init {
        loadWallpapers(isInitial = true)
        observeFavorites()
    }

    private fun observeFavorites() {
        viewModelScope.launch {
            repository.getFavorites().collect { favorites ->
                val favIds = favorites.map { it.id }.toSet()
                _uiState.update { state ->
                    state.copy(
                        wallpapers = state.wallpapers.map { wp ->
                            wp.copy(isFavorite = favIds.contains(wp.id))
                        }
                    )
                }
            }
        }
    }

    fun onCategorySelected(category: WallpaperCategory) {
        if (_uiState.value.selectedCategory == category && _uiState.value.searchQuery.isEmpty()) return
        searchJob?.cancel()
        _uiState.update {
            it.copy(
                selectedCategory = category,
                searchQuery = "",
                currentPage = 1,
                hasMore = true,
                randomSeed = UUID.randomUUID().toString().take(6)
            )
        }
        loadWallpapers(isInitial = true)
    }

    fun onSortingSelected(sorting: WallpaperSorting) {
        if (_uiState.value.selectedSorting == sorting) return
        _uiState.update {
            it.copy(
                selectedSorting = sorting,
                currentPage = 1,
                hasMore = true,
                randomSeed = UUID.randomUUID().toString().take(6)
            )
        }
        loadWallpapers(isInitial = true)
    }

    fun onSearchQueryChanged(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            delay(500) // 500 ms debounce as specified
            _uiState.update { it.copy(currentPage = 1, hasMore = true) }
            loadWallpapers(isInitial = true)
        }
    }

    fun clearSearch() {
        _uiState.update { it.copy(searchQuery = "") }
        searchJob?.cancel()
        _uiState.update { it.copy(currentPage = 1, hasMore = true) }
        loadWallpapers(isInitial = true)
    }

    fun retry() {
        loadWallpapers(isInitial = true)
    }

    fun loadMore() {
        val state = _uiState.value
        if (state.isLoading || state.isLoadingMore || !state.hasMore) return
        loadWallpapers(isInitial = false)
    }

    fun toggleFavorite(wallpaper: Wallpaper) {
        viewModelScope.launch {
            repository.toggleFavorite(wallpaper)
        }
    }

    private fun loadWallpapers(isInitial: Boolean) {
        viewModelScope.launch {
            val state = _uiState.value
            val targetPage = if (isInitial) 1 else state.currentPage + 1

            _uiState.update {
                if (isInitial) {
                    it.copy(isLoading = true, error = null, isRateLimited = false)
                } else {
                    it.copy(isLoadingMore = true)
                }
            }

            val result = repository.getWallpapers(
                category = state.selectedCategory,
                customQuery = state.searchQuery.ifBlank { null },
                sorting = state.selectedSorting,
                page = targetPage,
                seed = if (state.selectedSorting == WallpaperSorting.RANDOM) state.randomSeed else null
            )

            when (result) {
                is WallpaperResult.Success -> {
                    _uiState.update { current ->
                        val newItems = if (isInitial) {
                            result.data
                        } else {
                            (current.wallpapers + result.data).distinctBy { it.id }
                        }
                        current.copy(
                            isLoading = false,
                            isLoadingMore = false,
                            wallpapers = newItems,
                            currentPage = result.currentPage,
                            lastPage = result.lastPage,
                            hasMore = result.hasMore,
                            error = null,
                            isRateLimited = false
                        )
                    }
                }
                is WallpaperResult.Error -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isLoadingMore = false,
                            error = result.message,
                            isRateLimited = result.isRateLimited
                        )
                    }
                }
            }
        }
    }

    class Factory(private val repository: WallpaperRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return HomeViewModel(repository) as T
        }
    }
}
