package io.kaenlabs.uberclone.utils

import android.animation.ValueAnimator
import android.view.animation.LinearInterpolator

object AnimationUtils {

    fun polylineAnimator(): ValueAnimator = ValueAnimator.ofInt(0, 100).apply {
        interpolator = LinearInterpolator()
        duration = 3500
    }

}