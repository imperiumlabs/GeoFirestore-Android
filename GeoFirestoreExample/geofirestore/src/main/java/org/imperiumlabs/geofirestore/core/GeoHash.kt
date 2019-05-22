package org.imperiumlabs.geofirestore.core

import org.imperiumlabs.geofirestore.util.Base32Utils
import org.imperiumlabs.geofirestore.util.Base32Utils.BITS
import org.imperiumlabs.geofirestore.GeoLocation
import java.util.Locale.US

// TODO: 05/05/19 Test if makeGeoHash() work correctly

/**
 * A GeoHash instance is used to generate and store geohash strings
 */
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

    //Constructor with latitude, longitude and DEFAULT_PRECISION
    constructor(latitude: Double, longitude: Double): this(latitude, longitude, DEFAULT_PRECISION)

    //Constructor with GeoLocation and DEFAULT_PRECISION
    constructor(location: GeoLocation): this(location.latitude, location.longitude, DEFAULT_PRECISION)

    //Constructor with GeoLocation and precision
    constructor(location: GeoLocation, precision: Int): this(location.latitude, location.longitude, precision)

    //Constructor with latitude, longitude and precision
    constructor(latitude: Double, longitude: Double, precision: Int) {
        if (precision < 1)
            throw IllegalArgumentException("Precision of GeoHash must be larger than zero!")

        if (precision > MAX_PRECISION)
            throw IllegalArgumentException("Precision of a GeoHash must be less than " + (MAX_PRECISION + 1) + "!")

        if (!GeoLocation.coordinatesValid(latitude, longitude))
            throw IllegalArgumentException(String.format(US, "Not valid location coordinates: [%f, %f]", latitude, longitude))

        //The supplied data are valid... start creating the geo hash
        this.geoHashString = makeGeoHash(latitude, longitude, precision)
    }

    //Constructor with hash string
    constructor(hash: String) {
        if (hash.isEmpty() || !Base32Utils.isValidBase32String(hash))
            throw IllegalArgumentException("Not a valid geoHashString: $hash")
        this.geoHashString = hash
    }

    /*
     * Make the geohash string from supplied latitude, longitude, precision
     */
    private fun makeGeoHash(latitude: Double, longitude: Double, precision: Int): String {
        val lat = arrayOf(-90.0, 90.0)
        val lon = arrayOf(-180.0, 180.0)
        val buffer = CharArray(precision)

        //Calculate the value for every letter until we obtain a word of length precision
        for (i in 0 until precision) {
            var value = 0
            //Cycle every bit from 0 to BITS_PER_BASE32_CHAR (4)
            for (j in 0 until Base32Utils.BITS_PER_BASE32_CHAR) {
                val evenBit = (((i* Base32Utils.BITS_PER_BASE32_CHAR) + j) % 2) == 0
                if (evenBit) {
                    //If it's in an even position we calculate the value based on the longitude
                    val mid = (lon[0] + lon[1]) / 2
                    if (longitude > mid) {
                        value = value or BITS[j]
                        lon[0] = mid
                    } else
                        lon[1] = mid
                } else {
                    //If it's in an odd position we calculate the value based on the latitude
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
        return String(buffer)
    }

    override fun equals(other: Any?): Boolean {
        if (other == null || other !is GeoHash) return false
        return geoHashString == other.geoHashString
    }

    override fun toString() = "GeoHash(geoHashString='$geoHashString')"

    override fun hashCode() = this.geoHashString.hashCode()
}