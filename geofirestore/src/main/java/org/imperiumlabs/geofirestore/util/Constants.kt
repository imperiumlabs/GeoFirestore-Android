package org.imperiumlabs.geofirestore.util

// FULLY TESTED

object Constants {

    // Length of a degree latitude at the equator
    const val METERS_PER_DEGREE_LATITUDE: Double = 110574.0

    // The equatorial circumference of the earth in meters
    const val EARTH_MERIDIONAL_CIRCUMFERENCE: Double = 40007860.0

    // The equatorial radius of the earth in meters
    const val EARTH_EQ_RADIUS: Double = 6378137.0

    // The meridional radius of the earth in meters
    const val EARTH_POLAR_RADIUS: Double = 6357852.3

    /* The following value assumes a polar radius of
     * r_p = 6356752.3
     * and an equatorial radius of
     * r_e = 6378137
     * The value is calculated as e2 == (r_e^2 - r_p^2)/(r_e^2)
     * Use exact value to avoid rounding errors
     */
    const val EARTH_E2: Double = 0.00669447819799

    // Cutoff for floating point calculations
    const val EPSILON: Double = 1e-12
}