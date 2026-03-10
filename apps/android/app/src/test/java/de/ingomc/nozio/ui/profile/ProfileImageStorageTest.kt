package de.ingomc.nozio.ui.profile

import org.junit.Assert.assertEquals
import org.junit.Test

class ProfileImageStorageTest {

    @Test
    fun computeProfileImageCropWindow_centeredAtDefault() {
        val crop = computeProfileImageCropWindow(
            imageWidth = 1200,
            imageHeight = 800,
            cropSpec = ProfileImageCropSpec()
        )

        assertEquals(200, crop.left)
        assertEquals(0, crop.top)
        assertEquals(800, crop.size)
    }

    @Test
    fun computeProfileImageCropWindow_zoomInReducesSize() {
        val crop = computeProfileImageCropWindow(
            imageWidth = 1200,
            imageHeight = 800,
            cropSpec = ProfileImageCropSpec(zoom = 2f)
        )

        assertEquals(400, crop.left)
        assertEquals(200, crop.top)
        assertEquals(400, crop.size)
    }

    @Test
    fun computeProfileImageCropWindow_offsetsMoveToEdges() {
        val crop = computeProfileImageCropWindow(
            imageWidth = 1200,
            imageHeight = 800,
            cropSpec = ProfileImageCropSpec(zoom = 2f, offsetX = 1f, offsetY = -1f)
        )

        assertEquals(800, crop.left)
        assertEquals(0, crop.top)
        assertEquals(400, crop.size)
    }
}
