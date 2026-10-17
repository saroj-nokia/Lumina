package com.example.ui.screens.preview

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.domain.model.Wallpaper
import com.example.ui.components.FloatingWallpaperToolbar
import com.example.ui.components.SetWallpaperBottomSheet
import com.example.ui.components.shimmerBackground
import com.example.util.ColorUtils

@Composable
fun PreviewScreen(
    wallpaper: Wallpaper,
    viewModel: PreviewViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    BackHandler {
        onNavigateBack()
    }

    LaunchedEffect(wallpaper) {
        viewModel.setWallpaper(wallpaper)
    }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is PreviewEvent.ShowSnackbar -> {
                    snackbarHostState.showSnackbar(message = event.message)
                }
            }
        }
    }

    val placeholderColor = remember(wallpaper.avgColor) {
        ColorUtils.parseHexColor(wallpaper.avgColor, defaultColor = Color(0xFF16181D))
    }

    var isHighResLoaded by remember { mutableStateOf(false) }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(placeholderColor)
                // Tapping anywhere toggles chrome visibility
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = { viewModel.toggleChrome() }
                )
        ) {
            // Placeholder / Shimmer while high-res loads
            if (!isHighResLoaded) {
                // Show thumbnail as immediate preview
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(wallpaper.thumbnailUrl)
                        .crossfade(true)
                        .build(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .shimmerBackground(baseColor = placeholderColor)
                )

                CircularProgressIndicator(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(40.dp)
                        .testTag("preview_loading_spinner"),
                    color = Color.White.copy(alpha = 0.8f),
                    strokeWidth = 3.dp
                )
            }

            // High resolution full-bleed image (pinch/zoom disabled)
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(wallpaper.fullUrl)
                    .crossfade(400)
                    .build(),
                contentDescription = "Wallpaper #${wallpaper.id}",
                contentScale = ContentScale.Crop,
                onSuccess = { isHighResLoaded = true },
                modifier = Modifier.fillMaxSize()
            )

            // Top Bar Chrome (Back Button and Photographer Attribution)
            AnimatedVisibility(
                visible = uiState.isChromeVisible,
                enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
                modifier = Modifier.align(Alignment.TopCenter)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Black.copy(alpha = 0.75f),
                                    Color.Transparent
                                )
                            )
                        )
                        .statusBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = onNavigateBack,
                            colors = IconButtonDefaults.iconButtonColors(
                                containerColor = Color.Black.copy(alpha = 0.4f),
                                contentColor = Color.White
                            ),
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .testTag("back_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back to Wallpapers",
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        // Wallhaven original page link attribution
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .clip(MaterialTheme.shapes.small)
                                .clickable {
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(wallpaper.pageUrl))
                                    context.startActivity(intent)
                                }
                                .padding(vertical = 4.dp)
                                .testTag("wallhaven_page_link")
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = if (wallpaper.resolution.isNotBlank()) "Wallpaper (${wallpaper.resolution})" else "Wallhaven #${wallpaper.id}",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.White
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Icon(
                                    imageVector = Icons.AutoMirrored.Rounded.OpenInNew,
                                    contentDescription = "Open wallpaper on Wallhaven",
                                    tint = Color.White.copy(alpha = 0.8f),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            Text(
                                text = "View on Wallhaven.cc",
                                style = MaterialTheme.typography.bodySmall,
                                textDecoration = TextDecoration.Underline,
                                color = Color.White.copy(alpha = 0.75f)
                            )
                        }
                    }
                }
            }

            // Floating Bottom Toolbar Chrome (Set, Favorite, Download)
            FloatingWallpaperToolbar(
                visible = uiState.isChromeVisible,
                isFavorite = uiState.isFavorite,
                onSetWallpaperClick = { viewModel.openSetSheet() },
                onToggleFavoriteClick = { viewModel.toggleFavorite() },
                onDownloadClick = { viewModel.downloadWallpaper(context) },
                isDownloading = uiState.isDownloading,
                modifier = Modifier.align(Alignment.BottomCenter)
            )

            // Set Wallpaper Bottom Sheet
            if (uiState.showSetSheet) {
                SetWallpaperBottomSheet(
                    isApplying = uiState.isApplyingWallpaper,
                    onDestinationSelected = { destination ->
                        viewModel.applyWallpaper(context, destination)
                    },
                    onDismissRequest = { viewModel.dismissSetSheet() }
                )
            }
        }
    }
}
