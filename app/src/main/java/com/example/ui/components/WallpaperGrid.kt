package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridState
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.domain.model.Wallpaper
import com.example.util.ColorUtils

@Composable
fun WallpaperGrid(
    wallpapers: List<Wallpaper>,
    onWallpaperClick: (Wallpaper) -> Unit,
    onToggleFavorite: ((Wallpaper) -> Unit)? = null,
    onLoadMore: () -> Unit = {},
    isLoadingMore: Boolean = false,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(12.dp),
    gridState: LazyStaggeredGridState = rememberLazyStaggeredGridState()
) {
    // Infinite scroll detection: trigger when within 6 items of end
    val shouldLoadMore by remember {
        derivedStateOf {
            val totalItems = gridState.layoutInfo.totalItemsCount
            val lastVisibleItem = gridState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            totalItems > 0 && lastVisibleItem >= totalItems - 6
        }
    }

    LaunchedEffect(shouldLoadMore, isLoadingMore) {
        if (shouldLoadMore && !isLoadingMore) {
            onLoadMore()
        }
    }

    LazyVerticalStaggeredGrid(
        columns = StaggeredGridCells.Adaptive(minSize = 160.dp),
        state = gridState,
        contentPadding = contentPadding,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalItemSpacing = 10.dp,
        modifier = modifier.fillMaxSize()
    ) {
        items(
            items = wallpapers,
            key = { it.id }
        ) { wallpaper ->
            WallpaperCard(
                wallpaper = wallpaper,
                onClick = { onWallpaperClick(wallpaper) },
                onToggleFavorite = onToggleFavorite?.let { action -> { action(wallpaper) } }
            )
        }

        if (isLoadingMore) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .size(32.dp)
                            .testTag("load_more_indicator"),
                        strokeWidth = 3.dp,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

// Same gradient on every card, so create it once.
private val CardScrim = Brush.verticalGradient(
    colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.65f))
)

@Composable
fun WallpaperCard(
    wallpaper: Wallpaper,
    onClick: () -> Unit,
    onToggleFavorite: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var isImageLoaded by remember { mutableStateOf(false) }
    val placeholderColor = remember(wallpaper.avgColor) {
        ColorUtils.parseHexColor(wallpaper.avgColor, defaultColor = Color(0xFF1E222B))
    }

    // Aspect ratio: bound between 0.55 (tall portrait) and 0.85 to maintain aesthetic staggered look
    val cardAspectRatio = remember(wallpaper.aspectRatio) {
        wallpaper.aspectRatio.coerceIn(0.58f, 0.85f)
    }

    val context = LocalContext.current
    // Built once per thumbnail instead of on every recomposition.
    val thumbRequest = remember(wallpaper.thumbnailUrl) {
        ImageRequest.Builder(context)
            .data(wallpaper.thumbnailUrl)
            .crossfade(300)
            .build()
    }

    Surface(
        shape = MaterialTheme.shapes.large,
        color = placeholderColor,
        shadowElevation = 2.dp,
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(cardAspectRatio)
            .clip(MaterialTheme.shapes.large)
            .clickable(onClick = onClick)
            .testTag("wallpaper_card_${wallpaper.id}")
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Skeleton shimmer placeholder tinted with avg_color
            if (!isImageLoaded) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .shimmerBackground(baseColor = placeholderColor)
                )
            }

            // High performance AsyncImage with crossfade
            AsyncImage(
                model = thumbRequest,
                contentDescription = "Wallhaven wallpaper #${wallpaper.id}",
                contentScale = ContentScale.Crop,
                onSuccess = { isImageLoaded = true },
                modifier = Modifier.fillMaxSize()
            )

            // Bottom gradient overlay for resolution and favorite button
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .background(CardScrim)
                    .padding(horizontal = 10.dp, vertical = 8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f, fill = false)) {
                        Text(
                            text = if (wallpaper.resolution.isNotBlank()) wallpaper.resolution else "#${wallpaper.id}",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    if (onToggleFavorite != null) {
                        IconButton(
                            onClick = onToggleFavorite,
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .testTag("favorite_button_${wallpaper.id}")
                        ) {
                            Icon(
                                imageVector = if (wallpaper.isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                                contentDescription = if (wallpaper.isFavorite) "Remove from favorites" else "Add to favorites",
                                tint = if (wallpaper.isFavorite) Color(0xFFF43F5E) else Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
