package org.imperiumlabs.geofirestore

// FULLY TESTED

/**
 * A wrapper class for location coordinates.
 */
class GeoLocation(
    // The latitude of this location in the range of [-90, 90]
    val latitude: Double,
    // The longitude of this location in the range of [-180, 180]
    val longitude: Double) {

    companion object {
        /**
         * Checks if these coordinates are valid geo coordinates.
         * @param latitude The latitude must be in the range [-90, 90]
         * @param longitude The longitude must be in the range [-180, 180]
         * @return True if these are valid geo coordinates
         */
        fun coordinatesValid(latitude: Double, longitude: Double) =
            latitude >= -90 &&
                    latitude <= 90 &&
                    longitude >= -180 &&
                    longitude <= 180
    }

    /**
     * Creates a new GeoLocation with the given latitude and longitude.
     *
     * @throws IllegalArgumentException If the coordinates are not valid geo coordinates
     */
    init {
        if (!coordinatesValid(latitude, longitude))
            throw IllegalArgumentException("Not a valid geo location: $latitude, $longitude")
    }


    override fun toString() = "GeoLocation($latitude, $longitude)"

    override fun equals(other: Any?): Boolean {
        if (other == null || other !is GeoLocation)
            return false
        if (other.latitude.compareTo(this.latitude) != 0) return false
        if (other.longitude.compareTo(this.longitude) != 0) return false

        return true
    }

    override fun hashCode(): Int {
        var result: Int
        var temp = this.latitude.toLong()
        result = (temp xor (temp ushr 32)).toInt()
        temp = this.longitude.toLong()
        result = 31*result+(temp xor (temp ushr 32)).toInt()
        return result
    }
}
