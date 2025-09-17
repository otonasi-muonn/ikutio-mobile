package com.example.ikutio_mobile.utils

import com.example.ikutio_mobile.data.local.entity.LocationPointEntity
import kotlin.math.*

private const val EARTH_RADIUS_METERS = 6371000.0 // 地球の半径（メートル）

fun calculateDistanceBetweenPoints(
    lat1: Double, lon1: Double,
    lat2: Double, lon2: Double
): Double {
    val dLat = Math.toRadians(lat2 - lat1)
    val dLon = Math.toRadians(lon2 - lon1)

    val radLat1 = Math.toRadians(lat1)
    val radLat2 = Math.toRadians(lat2)

    val a = sin(dLat / 2).pow(2) +
            cos(radLat1) * cos(radLat2) *
            sin(dLon / 2).pow(2)
    val c = 2 * asin(sqrt(a))

    return EARTH_RADIUS_METERS * c
}

fun List<LocationPointEntity>.calculateTotalDistance(): Double {
    if (this.size < 2) {
        return 0.0
    }
    var totalDistance = 0.0
    for (i in 0 until this.size - 1) {
        val point1 = this[i]
        val point2 = this[i + 1]
        totalDistance += calculateDistanceBetweenPoints(
            point1.latitude, point1.longitude,
            point2.latitude, point2.longitude
        )
    }
    return totalDistance
}
