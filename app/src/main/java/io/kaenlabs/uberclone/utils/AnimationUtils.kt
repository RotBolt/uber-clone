package io.kaenlabs.uberclone.utils

import android.animation.ValueAnimator
import android.view.animation.LinearInterpolator

object AnimationUtils {

    fun polylineAnimator(): ValueAnimator = ValueAnimator.ofInt(0, 100).apply {
        interpolator = LinearInterpolator()
        duration = 3500
    }

    fun cabAnimator(): ValueAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
        duration = 3000
        interpolator = LinearInterpolator()
    }

}