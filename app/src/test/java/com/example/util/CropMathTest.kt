package com.example.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CropMathTest {

    // 1080x2400 phone
    private val phone = 1080f / 2400f

    @Test
    fun portraitSourceCenteredAtZoomOne() {
        assertEquals(CropRect(108, 0, 864, 1920), computeCropRect(1080, 1920, phone, CropState()))
    }

    @Test
    fun wideSourceKeepsFullHeightAndTargetAspect() {
        assertEquals(CropRect(1380, 0, 1080, 2160), computeCropRect(3840, 2160, 0.5f, CropState()))
    }

    @Test
    fun centerIsClampedToTheImage() {
        assertEquals(0, computeCropRect(3840, 2160, 0.5f, CropState(centerX = -1f)).left)
        assertEquals(2760, computeCropRect(3840, 2160, 0.5f, CropState(centerX = 2f)).left)
    }

    @Test
    fun zoomTwoHalvesTheWindow() {
        assertEquals(CropRect(1650, 540, 540, 1080), computeCropRect(3840, 2160, 0.5f, CropState(zoom = 2f)))
    }

    @Test
    fun zoomIsClampedBetweenOneAndThree() {
        assertEquals(360, computeCropRect(3840, 2160, 0.5f, CropState(zoom = 10f)).width)
        assertEquals(1080, computeCropRect(3840, 2160, 0.5f, CropState(zoom = 0.2f)).width)
    }

    @Test
    fun panRightShowsTheLeftSide() {
        val moved = panCrop(CropState(), 3840, 2160, 0.5f, dx = 0.5f, dy = 0f)
        assertEquals(840, computeCropRect(3840, 2160, 0.5f, moved).left)
    }

    @Test
    fun panStopsAtTheEdges() {
        val farRight = panCrop(CropState(), 3840, 2160, 0.5f, dx = 100f, dy = 0f)
        assertEquals(0, computeCropRect(3840, 2160, 0.5f, farRight).left)
        val farLeft = panCrop(CropState(), 3840, 2160, 0.5f, dx = -100f, dy = 0f)
        assertEquals(2760, computeCropRect(3840, 2160, 0.5f, farLeft).left)
    }

    @Test
    fun slowDragsAddUpInsteadOfGettingStuck() {
        var crop = CropState()
        repeat(100) { crop = panCrop(crop, 3840, 2160, 0.5f, dx = 0.0004f, dy = 0f) }
        val left = computeCropRect(3840, 2160, 0.5f, crop).left
        assertTrue("left was $left", left in 1336..1338) // 1380 - 100 * 0.0004 * 1080
    }

    @Test
    fun zoomKeepsTheWindowCenter() {
        val zoomed = zoomCrop(CropState(), 3840, 2160, 0.5f, 2f)
        assertEquals(CropRect(1650, 540, 540, 1080), computeCropRect(3840, 2160, 0.5f, zoomed))
        val back = zoomCrop(zoomed, 3840, 2160, 0.5f, 0.5f)
        assertEquals(CropRect(1380, 0, 1080, 2160), computeCropRect(3840, 2160, 0.5f, back))
    }

    @Test
    fun rectNeverLeavesTheImage() {
        for (zoom in listOf(1f, 1.7f, 3f)) for (cx in listOf(0f, 0.3f, 1f)) for (cy in listOf(0f, 0.6f, 1f)) {
            val r = computeCropRect(2560, 1440, phone, CropState(zoom, cx, cy))
            assertTrue(r.left >= 0 && r.top >= 0)
            assertTrue(r.left + r.width <= 2560 && r.top + r.height <= 1440)
        }
    }

    @Test
    fun freeSpaceFractionIsZeroWhenShapeMatches() {
        assertEquals(0f, freeSpaceFraction(1080, 2400, phone), 0.001f)
        assertEquals(0.71875f, freeSpaceFraction(3840, 2160, 0.5f), 0.001f)
    }
}
