package com.example.ui.screens.home

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.domain.model.Wallpaper
import com.example.ui.components.CategoryChips
import com.example.ui.components.EmptyStateView
import com.example.ui.components.ErrorStateView
import com.example.ui.components.LoadingStateView
import com.example.ui.components.SortingChips
import com.example.ui.components.WallpaperGrid

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onWallpaperClick: (Wallpaper) -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val focusManager = LocalFocusManager.current

    Column(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
    ) {
        // App Title & Tagline Header
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 6.dp)
        ) {
            Text(
                text = "Lumina",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "High-res phone wallpapers from Wallhaven",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Rounded Search Bar with 500ms debounce
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp)
        ) {
            OutlinedTextField(
                value = uiState.searchQuery,
                onValueChange = { viewModel.onSearchQueryChanged(it) },
                placeholder = {
                    Text(
                        text = "Search Wallhaven (e.g. AMOLED, cyberpunk, minimal)…",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Rounded.Search,
                        contentDescription = "Search",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp)
                    )
                },
                trailingIcon = {
                    if (uiState.searchQuery.isNotEmpty()) {
                        IconButton(
                            onClick = {
                                viewModel.clearSearch()
                                focusManager.clearFocus()
                            },
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .testTag("clear_search_button")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Clear,
                                contentDescription = "Clear search",
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                },
                singleLine = true,
                shape = CircleShape,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
                ),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("search_text_field")
            )
        }

        // Category Chips (Nature, Abstract, Dark/AMOLED, Minimal, Space, Cities, Animals, Cars, Architecture)
        CategoryChips(
            selectedCategory = uiState.selectedCategory,
            onCategorySelected = { category ->
                focusManager.clearFocus()
                viewModel.onCategorySelected(category)
            }
        )

        // Sorting Chips (Toplist, Latest, Views, Random)
        SortingChips(
            selectedSorting = uiState.selectedSorting,
            onSortingSelected = { sorting ->
                focusManager.clearFocus()
                viewModel.onSortingSelected(sorting)
            }
        )

        Spacer(modifier = Modifier.height(4.dp))

        // Main Content Area
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            when {
                uiState.isLoading && uiState.wallpapers.isEmpty() -> {
                    LoadingStateView(
                        message = if (uiState.searchQuery.isNotBlank()) {
                            "Searching Wallhaven for \"${uiState.searchQuery}\"…"
                        } else {
                            "Loading ${uiState.selectedCategory.title} wallpapers…"
                        }
                    )
                }

                uiState.error != null && uiState.wallpapers.isEmpty() -> {
                    ErrorStateView(
                        message = uiState.error ?: "Unable to fetch wallpapers.",
                        isRateLimited = uiState.isRateLimited,
                        isAuthError = false,
                        onRetry = { viewModel.retry() },
                        onOpenSettings = onOpenSettings
                    )
                }

                !uiState.isLoading && uiState.wallpapers.isEmpty() -> {
                    EmptyStateView(
                        title = "No wallpapers found",
                        subtitle = "We couldn't find any phone wallpapers for this keyword. Try another category or search query.",
                        actionText = "Show Top Wallpapers",
                        onAction = { viewModel.clearSearch() }
                    )
                }

                else -> {
                    WallpaperGrid(
                        wallpapers = uiState.wallpapers,
                        onWallpaperClick = onWallpaperClick,
                        onToggleFavorite = { viewModel.toggleFavorite(it) },
                        onLoadMore = { viewModel.loadMore() },
                        isLoadingMore = uiState.isLoadingMore,
                        contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 6.dp, bottom = 80.dp)
                    )
                }
            }
        }
    }
}
