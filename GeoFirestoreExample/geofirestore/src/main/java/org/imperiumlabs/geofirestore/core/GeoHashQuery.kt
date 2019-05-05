package org.imperiumlabs.geofirestore.core

import org.imperiumlabs.geofirestore.GeoLocation
import org.imperiumlabs.geofirestore.util.Base32Utils
import org.imperiumlabs.geofirestore.util.Constants
import org.imperiumlabs.geofirestore.util.GeoUtils
import java.util.HashSet

// FULLY TESTED

class GeoHashQuery(var startValue: String, var endValue: String) {


    object Utils {

        fun bitsLatitude(resolution: Double) =
                Math.min(
                        Math.log(Constants.EARTH_MERIDIONAL_CIRCUMFERENCE / 2 / resolution) / Math.log(2.0),
                        GeoHash.MAX_PRECISION_BITS.toDouble()
                )

        fun bitsLongitude(resolution: Double, latitude: Double): Double {
            val degrees = GeoUtils.distanceToLongitudeDegrees(resolution, latitude)
            return if (Math.abs(degrees) > 0) Math.max(1.0, Math.log(360 / degrees) / Math.log(2.0)) else 1.0
        }

        fun bitsForBoundingBox(location: GeoLocation, size: Double): Int {
            val latitudeDegreesDelta = GeoUtils.distanceToLatitudeDegrees(size)
            val latitudeNorth = Math.min(90.0, location.latitude + latitudeDegreesDelta)
            val latitudeSouth = Math.max(-90.0, location.latitude - latitudeDegreesDelta)
            val bitsLatitude = (Math.floor(bitsLatitude(size)) * 2).toInt()
            val bitsLongitudeNorth = (Math.floor(bitsLongitude(size, latitudeNorth)) * 2 - 1).toInt()
            val bitsLongitudeSouth = (Math.floor(bitsLongitude(size, latitudeSouth)) * 2 - 1).toInt()
            return Math.min(bitsLatitude, Math.min(bitsLongitudeNorth, bitsLongitudeSouth))
        }
    }

    companion object {

        fun queryForGeoHash(geohash: GeoHash, bits: Int): GeoHashQuery {
            var hash = geohash.geoHashString
            val precision = (Math.ceil(bits.toDouble() / Base32Utils.BITS_PER_BASE32_CHAR)).toInt()
            if (hash.length < precision) return GeoHashQuery(hash, "$hash~")
            hash = hash.substring(0, precision)
            val base = hash.substring(0, hash.length - 1)
            val lastValue = Base32Utils.base32CharToValue(hash[hash.length - 1])
            val significantBits = bits - (base.length * Base32Utils.BITS_PER_BASE32_CHAR)
            val unusedBits = Base32Utils.BITS_PER_BASE32_CHAR - significantBits
            // delete unused bits
            val startValue = (lastValue shr unusedBits) shl unusedBits
            val endValue = startValue + (1 shl unusedBits)
            val startHash = base + Base32Utils.valueToBase32Char(startValue)
            val endHash = if (endValue > 31) "$base~" else base + Base32Utils.valueToBase32Char(endValue)
            return GeoHashQuery(startHash, endHash)
        }

        fun queriesAtLocation(location: GeoLocation, radius: Double): Set<GeoHashQuery> {
            val queryBits = Math.max(1, Utils.bitsForBoundingBox(location, radius))
            val geoHashPrecision = Math.ceil(queryBits.toDouble() / Base32Utils.BITS_PER_BASE32_CHAR).toInt()

            val latitude = location.latitude
            val longitude = location.longitude
            val latitudeDegrees = radius / Constants.METERS_PER_DEGREE_LATITUDE
            val latitudeNorth = Math.min(90.0, latitude + latitudeDegrees)
            val latitudeSouth = Math.max(-90.0, latitude - latitudeDegrees)
            val longitudeDeltaNorth = GeoUtils.distanceToLongitudeDegrees(radius, latitudeNorth)
            val longitudeDeltaSouth = GeoUtils.distanceToLongitudeDegrees(radius, latitudeSouth)
            val longitudeDelta = Math.max(longitudeDeltaNorth, longitudeDeltaSouth)

            val queries = HashSet<GeoHashQuery>()

            val geoHash = GeoHash(latitude, longitude, geoHashPrecision)
            val geoHashW = GeoHash(latitude, GeoUtils.wrapLongitude(longitude - longitudeDelta), geoHashPrecision)
            val geoHashE = GeoHash(latitude, GeoUtils.wrapLongitude(longitude + longitudeDelta), geoHashPrecision)
            val geoHashN = GeoHash(latitudeNorth, longitude, geoHashPrecision)
            val geoHashNW = GeoHash(latitudeNorth, GeoUtils.wrapLongitude(longitude - longitudeDelta), geoHashPrecision)
            val geoHashNE = GeoHash(latitudeNorth, GeoUtils.wrapLongitude(longitude + longitudeDelta), geoHashPrecision)
            val geoHashS = GeoHash(latitudeSouth, longitude, geoHashPrecision)
            val geoHashSW = GeoHash(latitudeSouth, GeoUtils.wrapLongitude(longitude - longitudeDelta), geoHashPrecision)
            val geoHashSE = GeoHash(latitudeSouth, GeoUtils.wrapLongitude(longitude + longitudeDelta), geoHashPrecision)

            queries.add(queryForGeoHash(geoHash, queryBits))
            queries.add(queryForGeoHash(geoHashE, queryBits))
            queries.add(queryForGeoHash(geoHashW, queryBits))
            queries.add(queryForGeoHash(geoHashN, queryBits))
            queries.add(queryForGeoHash(geoHashNE, queryBits))
            queries.add(queryForGeoHash(geoHashNW, queryBits))
            queries.add(queryForGeoHash(geoHashS, queryBits))
            queries.add(queryForGeoHash(geoHashSE, queryBits))
            queries.add(queryForGeoHash(geoHashSW, queryBits))

            // Join queries
            var didJoin: Boolean
            do {
                var query1: GeoHashQuery? = null
                var query2: GeoHashQuery? = null
                for (query in queries) {
                    for (other in queries) {
                        if (query != other && query.canJoinWith(other)) {
                            query1 = query
                            query2 = other
                            break
                        }
                    }
                }
                didJoin = if (query1 != null && query2 != null) {
                    queries.remove(query1)
                    queries.remove(query2)
                    queries.add(query1.joinWith(query2))
                    true
                } else {
                    false
                }
            } while (didJoin)

            return queries
        }
    }

    private fun isPrefix(other: GeoHashQuery) =
            (other.endValue >= this.startValue) &&
                    (other.startValue < this.startValue) &&
                    (other.endValue < this.endValue)

    private fun isSuperQuery(other: GeoHashQuery): Boolean {
        val startCompare = other.startValue.compareTo(this.startValue)
        return startCompare <= 0 && other.endValue >= this.endValue
    }

    fun canJoinWith(other: GeoHashQuery) =
            this.isPrefix(other) ||
                    other.isPrefix(this) ||
                    this.isSuperQuery(other) ||
                    other.isSuperQuery(this)

    fun joinWith(other: GeoHashQuery) =
            when {
                other.isPrefix(this) -> GeoHashQuery(this.startValue, other.endValue)
                this.isPrefix(other) -> GeoHashQuery(other.startValue, this.endValue)
                this.isSuperQuery(other) -> other
                other.isSuperQuery(this) -> this
                else -> throw IllegalArgumentException("Can't join these two queries: $this, $other")
            }

    fun containsGeoHash(hash: GeoHash): Boolean {
        val hashStr = hash.geoHashString
        return this.startValue <= hashStr && this.endValue > hashStr
    }

    override fun equals(other: Any?): Boolean {
        if (other == null || other !is GeoHashQuery) return false
        if (endValue != other.endValue || startValue != other.startValue) return false
        return true
    }

    override fun hashCode() = (31 * startValue.hashCode() + endValue.hashCode())

    override fun toString() = "GeoHashQuery(startValue='$startValue', endValue='$endValue')"
}
