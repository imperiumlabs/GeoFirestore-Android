package org.imperiumlabs.geofirestore.core

import org.imperiumlabs.geofirestore.util.Base32Utils
import org.imperiumlabs.geofirestore.util.Base32Utils.Companion.BITS
import org.imperiumlabs.geofirestore.GeoLocation
import java.util.Locale.US


// FULLY TESTED

class GeoHash {

    //The GeoHash String value
    var geoHashString: String
        private set

    companion object {
        // The default precision of a geohash
        private const val DEFAULT_PRECISION = 10

        // The maximal precision of a geohash
        const val MAX_PRECISION = 22

        // The maximal number of bits precision for a geohash
        const val MAX_PRECISION_BITS = MAX_PRECISION * Base32Utils.BITS_PER_BASE32_CHAR

    }

    constructor(latitude: Double, longitude: Double): this(latitude, longitude, DEFAULT_PRECISION)

    constructor(location: GeoLocation): this(location.latitude, location.longitude, DEFAULT_PRECISION)

    constructor(latitude: Double, longitude: Double, precision: Int) {
        if (precision < 1)
            throw IllegalArgumentException("Precision of GeoHash must be larger than zero!")

        if (precision > MAX_PRECISION)
            throw IllegalArgumentException("Precision of a GeoHash must be less than " + (MAX_PRECISION + 1) + "!")

        if (!GeoLocation.coordinatesValid(latitude, longitude))
            throw IllegalArgumentException(String.format(US, "Not valid location coordinates: [%f, %f]", latitude, longitude))

        /*
         * The supplied data are valid... start creating the geohash
         *
         * We have two nested loop:
         *  - the inner loop cycle every bit from 0 to 4 and consider if it's in an even position
         *    if so we calculate the value based on the longitude
         *    else we calculate the value based on the latitude
         *    at the end we convert the value to the corresponding Base32 char using the Base32Utils
         *    method.
         *  - the outer loop repeat the value calculation until we obtain a word of length precision
         */
        val lat = arrayOf(-90.0, 90.0)
        val lon = arrayOf(-180.0, 180.0)
        val buffer = CharArray(precision)
        for (i in 0 until precision) {
            //for every letter-to-be
            var value = 0
            for (j in 0 until Base32Utils.BITS_PER_BASE32_CHAR) {
                val evenBit = (((i* Base32Utils.BITS_PER_BASE32_CHAR) + j) % 2) == 0
                if (evenBit) {
                    val mid = (lon[0] + lon[1]) / 2
                    if (longitude > mid) {
                        value = value or BITS[j]
                        lon[0] = mid
                    } else
                        lon[1] = mid
                } else {
                    val mid = (lat[0] + lat[1]) / 2
                    if (latitude > mid) {
                        value = value or BITS[j]
                        lat[0] = mid
                    } else
                        lat[1] = mid
                }
            }
            buffer[i] = Base32Utils.valueToBase32Char(value)
        }
        this.geoHashString = String(buffer)
    }


    constructor(hash: String) {
        if (hash.isEmpty() || !Base32Utils.isValidBase32String(hash))
            throw IllegalArgumentException("Not a valid geoHashString: $hash")
        this.geoHashString = hash
    }

    override fun equals(other: Any?): Boolean {
        if (other == null || other !is GeoHash) return false
        return geoHashString == other.geoHashString
    }

    override fun toString() = "GeoHash(geoHashString='$geoHashString')"

    override fun hashCode() = this.geoHashString.hashCode()
}