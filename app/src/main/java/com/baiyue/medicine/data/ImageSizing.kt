package com.baiyue.medicine.data

import kotlin.math.max

/** Pure image-size helper, kept free of Android imports so it is unit-testable. */
object ImageSizing {

    /**
     * Largest power-of-two [android.graphics.BitmapFactory.Options.inSampleSize] that keeps
     * the decoded bitmap close to [maxDimension] without going far below it.
     */
    fun calculateInSampleSize(width: Int, height: Int, maxDimension: Int): Int {
        if (width <= 0 || height <= 0 || maxDimension <= 0) return 1
        var sample = 1
        var longest = max(width, height)
        while (longest > maxDimension) {
            longest /= 2
            sample *= 2
        }
        return sample
    }
}
