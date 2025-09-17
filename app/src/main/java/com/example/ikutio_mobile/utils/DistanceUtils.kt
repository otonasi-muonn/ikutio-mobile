package com.example.ikutio_mobile.utils

import android.location.Location
import com.example.ikutio_mobile.data.remote.dto.PathDataItem

fun List<PathDataItem>.calculatePathDataItemDistance(): Double {
    if (this.size < 2) {
        return 0.0
    }
    var totalDistance = 0.0
    for (i in 0 until this.size - 1) {
        val startPoint = this[i]
        val endPoint = this[i + 1]
        val results = FloatArray(1)
        try {
            Location.distanceBetween(
                startPoint.latitude, startPoint.longitude,
                endPoint.latitude, endPoint.longitude,
                results
            )
            totalDistance += results[0]
        } catch (e: IllegalArgumentException) {
        }
    }
    return totalDistance
}
