package com.example.ui.screens.favorites

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.domain.model.Wallpaper
import com.example.ui.components.EmptyStateView
import com.example.ui.components.WallpaperGrid

@Composable
fun FavoritesScreen(
    viewModel: FavoritesViewModel,
    onWallpaperClick: (Wallpaper) -> Unit,
    onExploreClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val favorites by viewModel.favorites.collectAsStateWithLifecycle()

    Column(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            Text(
                text = "Favorites",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = if (favorites.isNotEmpty()) {
                    "${favorites.size} saved wallpapers • Available offline"
                } else {
                    "Your personal gallery of curated wallpapers"
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            if (favorites.isEmpty()) {
                EmptyStateView(
                    title = "No favorites saved",
                    subtitle = "Tap the heart icon on any wallpaper to save it here for fast offline access anytime.",
                    actionText = "Explore Wallpapers",
                    onAction = onExploreClick
                )
            } else {
                WallpaperGrid(
                    wallpapers = favorites,
                    onWallpaperClick = onWallpaperClick,
                    onToggleFavorite = { viewModel.removeFavorite(it) },
                    contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 8.dp, bottom = 80.dp)
                )
            }
        }
    }
}
