package org.imperiumlabs.geofirestore.util


// FULLY TESTED
object Base32Utils {

    //number of bits per base 32 character
    const val BITS_PER_BASE32_CHAR = 5

    val BITS = arrayOf(16, 8, 4, 2, 1)

    //String representing the Base32 character map
    private const val BASE32_CHARS = "0123456789bcdefghjkmnpqrstuvwxyz"

    /*
     * This method convert a given value to his corresponding Base32 character
     */
    fun valueToBase32Char(value: Int): Char {
        if (value < 0 || value >= BASE32_CHARS.length)
            throw IllegalArgumentException("Not a valid base32 value: $value")
        return BASE32_CHARS[value]
    }

    /*
     * This method convert a given Base32 character to his corresponding value
     */
    fun base32CharToValue(base32Char: Char): Int {
        val value = BASE32_CHARS.indexOf(base32Char)
        if (value == -1)
            throw IllegalArgumentException("Not a valid base32 char: $base32Char")
        return value
    }

    /*
     * This method check if a given geo hash is valid
     */
    fun isValidBase32String(string: String) =
            if (string.isNotEmpty())
                string.matches("^[$BASE32_CHARS]*$".toRegex())
            else false
}
