package com.baiyue.medicine.data

import org.junit.Assert.assertEquals
import org.junit.Test

class ImageSizingTest {

    @Test
    fun `small images are not downsampled`() {
        assertEquals(1, ImageSizing.calculateInSampleSize(1200, 900, 1600))
        assertEquals(1, ImageSizing.calculateInSampleSize(1600, 1600, 1600))
    }

    @Test
    fun `large images use a power of two sample`() {
        assertEquals(4, ImageSizing.calculateInSampleSize(4000, 3000, 1600))
        assertEquals(8, ImageSizing.calculateInSampleSize(8000, 6000, 1600))
        assertEquals(2, ImageSizing.calculateInSampleSize(1601, 1000, 1600))
    }

    @Test
    fun `invalid dimensions fall back to one`() {
        assertEquals(1, ImageSizing.calculateInSampleSize(0, 3000, 1600))
        assertEquals(1, ImageSizing.calculateInSampleSize(4000, 0, 1600))
        assertEquals(1, ImageSizing.calculateInSampleSize(4000, 3000, 0))
    }
}
