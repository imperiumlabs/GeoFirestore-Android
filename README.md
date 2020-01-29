# GeoFirestore for Android — Realtime location queries with Firestore

[![](https://jitpack.io/v/imperiumlabs/GeoFirestore-Android.svg)](https://jitpack.io/#imperiumlabs/GeoFirestore-Android)


GeoFirestore is an open-source library for Android that allows you to store and query a set of documents based on their geographic location.

At its heart, GeoFirestore simply stores locations with string keys. Its main benefit however, is the possibility of querying documents within a given geographic area - all in realtime.

GeoFirestore uses the Firestore database for data storage, allowing query results to be updated in realtime as they change. GeoFirestore selectively loads only the data near certain locations, keeping your applications light and responsive, even with extremely large datasets.

A compatible GeoFirestore client is also available for [iOS](https://github.com/imperiumlabs/GeoFirestore-iOS).

For a full example of an application using GeoFirestore to display realtime post data, see the example project in this repo.

### Integrating GeoFirestore with your data

GeoFirestore is designed as a lightweight add-on to Firestore. However, to keep things simple, GeoFirestore stores data in its own format and its own location within your Firestore database. This allows your existing data format and security rules to remain unchanged and for you to add GeoFirestore as an easy solution for geo queries without modifying your existing data.

### Example usage

Assume you are building an app to rate bars, and you store all information for a bar (e.g. name, business hours and price range) at `collection(bars).document(bar-id)`. Later, you want to add the possibility for users to search for bars in their vicinity. This is where GeoFirestore comes in. You can store the location for each bar document using GeoFirestore. GeoFirestore then allows you to easily query which bar are nearby.

## Including GeoFirestore in your Android project

In order to use GeoFirestore in your project, you need to [add the Firestore Android SDK](https://firebase.google.com/docs/firestore/quickstart). 
After that you can include GeoFirestore in your project.

### Enable Jitpack
Add it in your root build.gradle at the end of repositories:
```gradle
allprojects {
    repositories {
        ...
        maven { url 'https://jitpack.io' }
    }
}
```
Using **Gradle**

```gradle
implementation 'com.github.imperiumlabs:GeoFirestore-Android:v1.5.0'
```
Using **Maven**
```xml
<dependency>
    <groupId>com.github.imperiumlabs</groupId>
    <artifactId>GeoFirestore-Android</artifactId>
    <version>v1.5.0</version>
</dependency>
```

## Getting Started with Firestore

GeoFirestore requires the Firestore database in order to store location data. You can [learn more about Firestore here](https://firebase.google.com/docs/firestore/).


### GeoFirestore

A `GeoFirestore` object is used to read and write geo location data to your Firestore database and to create queries. To create a new `GeoFirestore` instance you need to attach it to a Firestore collection reference:

```kotlin
val collectionRef = FirebaseFirestore.getInstance().collection("my-collection")
val geoFirestore = GeoFirestore(collectionRef)
```

#### Setting location data

To set the location of a document simply call the `setLocation` method:

```kotlin
geoFirestore.setLocation("que8B9fxxjcvbC81h32VRjeBSUW2", GeoPoint(37.7853889, -122.4056973)) { exception ->
    if (exception == null)
        Log.d(TAG, "Location saved on server successfully!")
	}
	else{
    	Log.d(TAG, "An error has occurred: $exception")	
	}
}
```

To remove a location and delete the location from your database simply call:

```kotlin
geoFirestore.removeLocation("que8B9fxxjcvbC81h32VRjeBSUW2")
```

#### Retrieving a location

Retrieving locations happens with callbacks. If the document is not present in GeoFirestore, the callback will be called with `null`. If an error occurred, the callback is passed the error and the location will be `null`.

```kotlin
geoFirestore.getLocation("que8B9fxxjcvbC81h32VRjeBSUW2") { location, exception ->
    if (exception == null && location != null){
        Log.d(TAG, "The location for this document is $location")
    }
};
```

### Geo Queries

GeoFirestore allows you to query all documents within a geographic area using `GeoQuery`
objects. As the locations for documents change, the query is updated in realtime and fires events letting you know if any relevant documents have moved. 
`GeoQuery` parameters can be updated later to change the size and center of the queried area.

```kotlin
// creates a new query around [37.7832, -122.4056] with a radius of 0.6 kilometers
val geoQuery = geoFirestore.queryAtLocation(GeoPoint(37.7832, -122.4056), 0.6)
```

#### Receiving events for geo queries

##### Key Events

There are five kinds of "key" events that can occur with a geo query:

1. **Key Entered**: The location of a document now matches the query criteria.
2. **Key Exited**: The location of a document no longer matches the query criteria.
3. **Key Moved**: The location of a document changed but the location still matches the query criteria.
4. **Query Ready**: All current data has been loaded from the server and all
   initial events have been fired.
5. **Query Error**: There was an error while performing this query, e.g. a
   violation of security rules.

Key entered events will be fired for all documents initially matching the query as well as any time afterwards that a document enters the query. Key moved and key exited events are guaranteed to be preceded by a key entered event.

Sometimes you want to know when the data for all the initial documents has been
loaded from the server and the corresponding events for those documents have been
fired. For example, you may want to hide a loading animation after your data has
fully loaded. This is what the "ready" event is used for.

Note that locations might change while initially loading the data and key moved and key
exited events might therefore still occur before the ready event is fired.

When the query criteria is updated, the existing locations are re-queried and the
ready event is fired again once all events for the updated query have been
fired. This includes key exited events for documents that no longer match the query.

To listen for events you must add a `GeoQueryEventListener` to the `GeoQuery`:

```kotlin
geoQuery.addGeoQueryEventListener(object : GeoQueryEventListener {
    
    override fun onKeyEntered(documentID: String, location: GeoPoint) {
        Log.d(TAG, "Document $documentID entered the search area at $location")
    }

    override fun onKeyExited(documentID: String) {
        Log.d(TAG, "Document $documentID is no longer in the search area")
    }

    override fun onKeyMoved(documentID: String, location: GeoPoint) {
        Log.d(TAG, "Document $documentID moved within the search area to $location")
    }

    override fun onGeoQueryReady() {
        Log.d(TAG, "All initial data has been loaded and events have been fired!")
    }

    override fun onGeoQueryError(exception: Exception) {
        Log.d(TAG, "There was an error with this query: $exception")
    }
});
```

You can call either `removeGeoQueryEventListener` to remove a
single event listener or `removeAllListeners` to remove all event listeners
for a `GeoQuery`.

##### Data Events

If you are storing model data and geo data in the same document, you may
want access to the `DocumentSnapshot` as part of geo events. In this case, use a
`GeoQueryDataEventListener` rather than a key listener.

These "data event" listeners have all of the same events as the key listeners with
one additional event type:

  6. **Document Changed**: the underlying `DocumentSnapshot` has changed. 

  Every document moved event is followed by a document changed event but you can also get change events without a move if the document changed does not affect the location.

Adding a data event listener is similar to adding a key event listener:

```kotlin
geoQuery.addGeoQueryDataEventListener(object : GeoQueryDataEventListener {
    override fun onDocumentEntered(documentSnapshot: DocumentSnapshot, location: GeoPoint) {
        // ...
    }

    override fun onDocumentExited(documentSnapshot: DocumentSnapshot) {
        // ...
    }

    override fun onDocumentMoved(documentSnapshot: DocumentSnapshot, location: GeoPoint) {
        // ...
    }

    override fun onDocumentChanged(documentSnapshot: DocumentSnapshot, location: GeoPoint) {
        // ...
    }

    override fun onGeoQueryReady() {
        // ...
    }

    override fun onGeoQueryError(exception: Exception) {
        // ...
    }
})
```
#### Query the location "one-shot"

Sometimes it's useful to have the possibility to search for all the documents present in a geographical area without, however, listening to data variations; 
to do so simply call:

```kotlin
geoFirestore.getAtLocation(QUERY_CENTER, QUERY_RADIUS) { docs, ex ->
    if (ex != null) {
        Log.e(TAG, "onError: ", ex)
        return@getAtLocation
    } else {
        // ...
    }
}
```

This will return to the `SingleGeoQueryDataEventCallback` a list of all the documents presents in the area and an exception if something goes wrong.

#### Updating the query criteria

The `GeoQuery` search area can be changed with `setCenter` and `setRadius`. Key
exited and key entered events will be fired for documents moving in and out of
the old and new search area, respectively. No key moved events will be
fired; however, key moved events might occur independently.

Updating the search area can be helpful in cases such as when you need to update
the query to the new visible map area after a user scrolls.

## Apps using GeoFirestore
There's hundreds of apps using GeoFirestore. Feel free to contact us or submit a pull request to add yours to this list.

* [Petify](https://play.google.com/store/apps/details?id=com.supercaly.petify)

## Changelog
[See the changelog](CHANGELOG.md) to be aware of latest improvements and fixes.

## License

GeoFirestore is available under the MIT license. [See the LICENSE file](LICENSE) for more info.

Copyright (c) 2018 Imperium Labs
