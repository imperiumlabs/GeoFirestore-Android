package org.imperiumlabs.geofirestore.util

import org.imperiumlabs.geofirestore.GeoFirestore
import org.imperiumlabs.geofirestore.GeoLocation

// FULLY TESTED

class GeoUtils {

    companion object {
        private const val MAX_SUPPORTED_RADIUS = 8587

        fun distance(location1: GeoLocation, location2: GeoLocation) =
            distance(location1.latitude, location1.longitude, location2.latitude, location2.longitude)

        fun distance(lat1: Double, long1: Double, lat2: Double, long2: Double): Double {
            // Earth's mean radius in meters
            val radius = (Constants.EARTH_EQ_RADIUS + Constants.EARTH_POLAR_RADIUS) / 2
            val latDelta = Math.toRadians(lat1 - lat2)
            val lonDelta = Math.toRadians(long1 - long2)

            val a = (Math.sin(latDelta / 2) * Math.sin(latDelta / 2)) +
                    (Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                            Math.sin(lonDelta / 2) * Math.sin(lonDelta / 2))
            return radius * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
        }

        fun distanceToLatitudeDegrees(distance: Double) = distance / Constants.METERS_PER_DEGREE_LATITUDE

        fun distanceToLongitudeDegrees(distance: Double, latitude: Double): Double {
            val radians = Math.toRadians(latitude)
            val numerator = Math.cos(radians) * Constants.EARTH_EQ_RADIUS * Math.PI / 180
            val denominator = 1 / Math.sqrt(1 - Constants.EARTH_E2 * Math.sin(radians) * Math.sin(radians))
            val deltaDegrees = numerator * denominator
            return if (deltaDegrees < Constants.EPSILON)
                if (distance > 0) 360.0 else distance
            else
                Math.min(360.0, distance / deltaDegrees)
        }

        fun wrapLongitude(longitude: Double): Double {
            if (longitude >= -180 && longitude <= 180)
                return longitude
            val adjusted = longitude +180
            return if (adjusted > 0) (adjusted % 360.0) - 180 else 180 - (-adjusted % 360)
        }

        fun capRadius(radius: Double): Double {
            if (radius > MAX_SUPPORTED_RADIUS) {
                GeoFirestore.LOGGER.warning("The radius is bigger than $MAX_SUPPORTED_RADIUS and hence we'll use that value")
                return MAX_SUPPORTED_RADIUS.toDouble()
            }
            return radius
        }
    }
}
