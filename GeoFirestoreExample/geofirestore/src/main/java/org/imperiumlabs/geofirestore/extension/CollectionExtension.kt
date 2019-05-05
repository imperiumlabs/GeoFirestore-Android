package org.imperiumlabs.geofirestore.extension

/*
 * Map every object of T in an Iterable to a MutableCollection of R
 * excluding the non null's elements
 *
 * example:
 *      list = listOf(listOf(1, 2, null), listOf(3, 4, 5))
 *      list2 = arrayListOf<Int>()
 *      list.mapNotNullManyTo(list2) {it}
 *
 *      before:
 *          -list [[1,2,null],[3,4,5]]
 *          -list2 []
 *      after:
 *          -list [[1,2,null],[3,4,5]]
 *          -list2 [1,2,3,4,5]
 */
inline fun<T, R, C:MutableCollection<in R>> Iterable<T>.mapNotNullManyTo(destination: C, transform: (T)->Collection<R?>?): C {
    forEach { element -> transform(element)?.let { collection ->
        val c = arrayListOf<R>()
        collection.forEach {r -> r?.let { c.add(it) } }
        destination.addAll(c)
    }
    }
    return destination
}