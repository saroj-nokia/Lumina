package com.example.util

import kotlin.math.max
import kotlin.math.roundToInt

// Pure Kotlin (no Android imports) so it can be unit tested on the JVM.
// The preview and the wallpaper applier both use this file, so they always agree on the crop.

const val MAX_CROP_ZOOM = 3f

/** zoom 1..3; centerX/centerY are 0..1 over the free space (0.5 = centered). */
data class CropState(val zoom: Float = 1f, val centerX: Float = 0.5f, val centerY: Float = 0.5f)

data class CropRect(val left: Int, val top: Int, val width: Int, val height: Int)

private class Window(val left: Float, val top: Float, val width: Float, val height: Float)

/** Largest window inside the source that has the target aspect (width / height). */
fun baseWindow(srcW: Int, srcH: Int, targetAspect: Float): Pair<Float, Float> =
    if (srcW.toFloat() / srcH > targetAspect) {
        (srcH * targetAspect) to srcH.toFloat()
    } else {
        srcW.toFloat() to (srcW / targetAspect)
    }

private fun window(srcW: Int, srcH: Int, targetAspect: Float, crop: CropState): Window {
    require(srcW > 0 && srcH > 0 && targetAspect > 0f) { "Invalid size" }
    val z = crop.zoom.coerceIn(1f, MAX_CROP_ZOOM)
    val (bw, bh) = baseWindow(srcW, srcH, targetAspect)
    val w = bw / z
    val h = bh / z
    return Window(
        left = crop.centerX.coerceIn(0f, 1f) * max(0f, srcW - w),
        top = crop.centerY.coerceIn(0f, 1f) * max(0f, srcH - h),
        width = w,
        height = h
    )
}

/** Source pixels to use for this crop. Always fully inside the image. */
fun computeCropRect(srcW: Int, srcH: Int, targetAspect: Float, crop: CropState): CropRect {
    val win = window(srcW, srcH, targetAspect, crop)
    val w = win.width.roundToInt().coerceIn(1, srcW)
    val h = win.height.roundToInt().coerceIn(1, srcH)
    return CropRect(
        left = win.left.roundToInt().coerceIn(0, srcW - w),
        top = win.top.roundToInt().coerceIn(0, srcH - h),
        width = w,
        height = h
    )
}

private fun normalize(pos: Float, free: Float): Float =
    if (free > 0.5f) (pos / free).coerceIn(0f, 1f) else 0.5f

/**
 * Move the window. dx/dy are the finger movement as a fraction of the visible window,
 * so dragging right (dx > 0) shows the part of the image that is further left.
 * Works from the unrounded position so slow drags do not get stuck.
 */
fun panCrop(crop: CropState, srcW: Int, srcH: Int, targetAspect: Float, dx: Float, dy: Float): CropState {
    val win = window(srcW, srcH, targetAspect, crop)
    return crop.copy(
        centerX = normalize(win.left - dx * win.width, srcW - win.width),
        centerY = normalize(win.top - dy * win.height, srcH - win.height)
    )
}

/** Multiply the zoom by [factor] around the center of the current window. */
fun zoomCrop(crop: CropState, srcW: Int, srcH: Int, targetAspect: Float, factor: Float): CropState {
    val old = window(srcW, srcH, targetAspect, crop)
    val zoom = (crop.zoom * factor).coerceIn(1f, MAX_CROP_ZOOM)
    val (bw, bh) = baseWindow(srcW, srcH, targetAspect)
    val w = bw / zoom
    val h = bh / zoom
    return CropState(
        zoom = zoom,
        centerX = normalize(old.left + old.width / 2 - w / 2, srcW - w),
        centerY = normalize(old.top + old.height / 2 - h / 2, srcH - h)
    )
}

/** 0 when the image already has the target shape; 0.72 for a 16:9 image on a tall phone. */
fun freeSpaceFraction(srcW: Int, srcH: Int, targetAspect: Float): Float {
    val (bw, bh) = baseWindow(srcW, srcH, targetAspect)
    return max(1f - bw / srcW, 1f - bh / srcH)
}
