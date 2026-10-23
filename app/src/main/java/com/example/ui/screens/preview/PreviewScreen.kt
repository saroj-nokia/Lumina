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
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.outlined.RestartAlt
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
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.example.domain.model.Wallpaper
import com.example.ui.components.FloatingWallpaperToolbar
import com.example.ui.components.SetWallpaperBottomSheet
import com.example.ui.components.shimmerBackground
import com.example.util.ColorUtils
import com.example.util.CropState
import com.example.util.MAX_CROP_ZOOM
import com.example.util.SystemWallpaperPicker
import com.example.util.WallpaperManagerHelper
import com.example.util.baseWindow
import com.example.util.computeCropRect
import com.example.util.freeSpaceFraction
import com.example.util.panCrop
import com.example.util.zoomCrop
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

private const val MAX_DECODE_SIDE = 4096f

private val CropStateSaver = listSaver<CropState, Float>(
    save = { listOf(it.zoom, it.centerX, it.centerY) },
    restore = { CropState(it[0], it[1], it[2]) }
)

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

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is PreviewEvent.ShowSnackbar -> {
                    snackbarHostState.showSnackbar(message = event.message)
                }
                is PreviewEvent.OpenSystemPicker -> {
                    if (!SystemWallpaperPicker.launch(context, event.uri)) {
                        snackbarHostState.showSnackbar(
                            message = "Your device has no system wallpaper picker for this. Use Lumina's crop instead."
                        )
                    }
                }
            }
        }
    }

    val placeholderColor = remember(wallpaper.avgColor) {
        ColorUtils.parseHexColor(wallpaper.avgColor, defaultColor = Color(0xFF16181D))
    }

    var isHighResLoaded by remember(wallpaper.id) { mutableStateOf(false) }

    // The crop lives here (not in the ViewModel) so dragging only redraws the image layer.
    // The same CropState goes to computeCropRect for the preview and for applying, so they always match.
    val (targetW, targetH) = remember { WallpaperManagerHelper.targetSize(context) }
    val targetAspect = targetW.toFloat() / targetH
    val srcW = wallpaper.width.coerceAtLeast(1)
    val srcH = wallpaper.height.coerceAtLeast(1)
    var crop by rememberSaveable(wallpaper.id, stateSaver = CropStateSaver) { mutableStateOf(CropState()) }
    val isCropped by remember { derivedStateOf { crop != CropState() } }
    var boxSize by remember { mutableStateOf(IntSize.Zero) }
    var showHint by remember(wallpaper.id) {
        mutableStateOf(freeSpaceFraction(srcW, srcH, targetAspect) > 0.1f)
    }
    LaunchedEffect(wallpaper.id) {
        if (showHint) {
            delay(3000)
            showHint = false
        }
    }

    Scaffold(
        snackbarHost = {
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier
                    .navigationBarsPadding()
                    .padding(bottom = 88.dp)
            )
        },
        contentWindowInsets = WindowInsets(0, 0, 0, 0), // full-bleed: the chrome handles the system bars itself
        modifier = modifier.fillMaxSize()
    ) { _ ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(placeholderColor)
                .pointerInput(Unit) {
                    detectTapGestures(
                        onDoubleTap = { crop = CropState() },
                        onTap = { viewModel.toggleChrome() }
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            // Same shape as the screen, so what is visible here is exactly what gets applied.
            Box(
                modifier = Modifier
                    .aspectRatio(targetAspect)
                    .clipToBounds()
                    .onSizeChanged { boxSize = it }
                    .pointerInput(srcW, srcH, targetAspect) {
                        detectTransformGestures { _, pan, zoom, _ ->
                            if (size.width == 0 || size.height == 0) return@detectTransformGestures
                            showHint = false
                            var next = crop
                            if (zoom != 1f) next = zoomCrop(next, srcW, srcH, targetAspect, zoom)
                            crop = panCrop(
                                next, srcW, srcH, targetAspect,
                                dx = pan.x / size.width,
                                dy = pan.y / size.height
                            )
                        }
                    }
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

                if (boxSize.width > 0) {
                    // Lay the whole image out at the scale where the zoom-1 window fills the box, then
                    // zoom and slide that layer to show the chosen window.
                    val baseWidth = remember(srcW, srcH, targetAspect) { baseWindow(srcW, srcH, targetAspect).first }
                    val scale = boxSize.width / baseWidth
                    val density = LocalDensity.current

                    // Decode at most MAX_DECODE_SIDE px on the long side, and keep this big bitmap out of the
                    // memory cache so it does not push the grid thumbnails out.
                    val fullImageRequest = remember(wallpaper.fullUrl, srcW, srcH, scale) {
                        val w = srcW * scale
                        val h = srcH * scale
                        val shrink = minOf(1f, MAX_DECODE_SIDE / maxOf(w, h))
                        ImageRequest.Builder(context)
                            .data(wallpaper.fullUrl)
                            .size((w * shrink).roundToInt(), (h * shrink).roundToInt())
                            .memoryCachePolicy(CachePolicy.DISABLED)
                            .crossfade(400)
                            .build()
                    }

                    AsyncImage(
                        model = fullImageRequest,
                        contentDescription = "Wallpaper #${wallpaper.id}",
                        contentScale = ContentScale.FillBounds,
                        onSuccess = { isHighResLoaded = true },
                        modifier = Modifier
                            .wrapContentSize(Alignment.TopStart, unbounded = true)
                            .size(
                                with(density) { (srcW * scale).toDp() },
                                with(density) { (srcH * scale).toDp() }
                            )
                            .graphicsLayer {
                                val rect = computeCropRect(srcW, srcH, targetAspect, crop)
                                val zoom = crop.zoom.coerceIn(1f, MAX_CROP_ZOOM)
                                transformOrigin = TransformOrigin(0f, 0f)
                                scaleX = zoom
                                scaleY = zoom
                                translationX = -rect.left * scale * zoom
                                translationY = -rect.top * scale * zoom
                            }
                    )
                }
            }

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

                        if (isCropped) {
                            IconButton(
                                onClick = { crop = CropState() },
                                colors = IconButtonDefaults.iconButtonColors(
                                    containerColor = Color.Black.copy(alpha = 0.4f),
                                    contentColor = Color.White
                                ),
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .testTag("reset_crop_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.RestartAlt,
                                    contentDescription = "Reset crop",
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                    }
                }
            }

            // One-time hint when the image is bigger than the screen shape
            AnimatedVisibility(
                visible = showHint,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(bottom = 112.dp)
            ) {
                Surface(shape = CircleShape, color = Color.Black.copy(alpha = 0.6f)) {
                    Text(
                        text = "Drag to choose the part you want",
                        style = MaterialTheme.typography.labelLarge,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
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
                        viewModel.applyWallpaper(context, destination, crop)
                    },
                    onSystemPickerSelected = { viewModel.openSystemPicker(context) },
                    onDismissRequest = { viewModel.dismissSetSheet() }
                )
            }
        }
    }
}
