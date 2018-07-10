# GeoFirestore for Android — Realtime location queries with Firestore

GeoFirestore is an open-source library for Android that allows you to store and query a set of documents based on their geographic location.

At its heart, GeoFirestore simply stores locations with string keys. Its main benefit however, is the possibility of querying documents within a given geographic area - all in realtime.

GeoFirestore uses the Firestore database for data storage, allowing query results to be updated in realtime as they change. GeoFirestore selectively loads only the data near certain locations, keeping your applications light and responsive, even with extremely large datasets.

A compatible GeoFirestore client is also available for [IOS](https://github.com/imperiumlabs/GeoFirestore-IOS).

For a full example of an application using GeoFirestore to display realtime post data, see the example project in this repo.

### Integrating GeoFirestore with your data

GeoFirestore is designed as a lightweight add-on to Firestore. However, to keep things simple, GeoFirestore stores data in its own format and its own location within your Firestore database. This allows your existing data format and security rules to remain unchanged and for you to add GeoFirestore as an easy solution for geo queries without modifying your existing data.

### Example usage

Assume you are building an app to rate bars, and you store all information for a bar (e.g. name, business hours and price range) at `collection(bars).document(bar-id)`. Later, you want to add the possibility for users to search for bars in their vicinity. This is where GeoFirestore comes in. You can store the location for each bar document using GeoFirestore. GeoFirestore then allows you to easily query which bar are nearby.

## Including GeoFirestore in your Android project

In order to use GeoFirestore in your project, you need to [add the Firestore Android
SDK](https://firebase.google.com/docs/firestore/quickstart). After that you can include GeoFirestore in your project.

Follow [these instructions](https://jitpack.io/#imperiumlabs/GeoFirestore-Android/v1.1.0) to add GeoFirestore using gradle, maven, sbt, and leiningen. 

## Getting Started with Firestore

GeoFirestore requires the Firestore database in order to store location data. You can [learn more about Firestore here](https://firebase.google.com/docs/firestore/).


### GeoFirestore

A `GeoFirestore` object is used to read and write geo location data to your Firestore database and to create queries. To create a new `GeoFirestore` instance you need to attach it to a Firestore collection reference:

```java
CollectionReference geoFirestoreRef = FirebaseFirestore.getInstance().collection("my-collection");
GeoFirestore geoFirestore = new GeoFirestore(geoFirestoreRef);
```

#### Setting location data

To set the location of a document simply call the `setLocation` method:

```java
geoFirestore.setLocation("que8B9fxxjcvbC81h32VRjeBSUW2", new GeoPoint(37.7853889, -122.4056973));
```

To check if a write was successfully saved on the server, you can add a
`GeoFirestore.CompletionListener` to the `setLocation` call:

```java
geoFirestore.setLocation("que8B9fxxjcvbC81h32VRjeBSUW2", new GeoPoint(37.7853889, -122.4056973), new GeoFirestore.CompletionListener() {
    @Override
    public void onComplete(Exception exception) {
        if (exception == null){
            System.out.println("Location saved on server successfully!");
        }
    }
});
```

To remove a location and delete the location from your database simply call:

```java
geoFirestore.removeLocation("que8B9fxxjcvbC81h32VRjeBSUW2");
```

#### Retrieving a location

Retrieving locations happens with callbacks. If the document is not present in GeoFirestore, the callback will be called with `null`. If an error occurred, the callback is passed the error and the location will be `null`.

```java
geoFirestore.getLocation("que8B9fxxjcvbC81h32VRjeBSUW2", new GeoFirestore.LocationCallback() {
    @Override
    public void onComplete(GeoPoint location, Exception exception) {
        if (exception == null && location != null){
            System.out.println(String.format("The location for this document is [%f,%f]", location.getLatitude(), location.getLongitude()));
        }
    }
});
```

### Geo Queries

GeoFirestore allows you to query all documents within a geographic area using `GeoQuery`
objects. As the locations for documents change, the query is updated in realtime and fires events letting you know if any relevant documents have moved. `GeoQuery` parameters can be updated later to change the size and center of the queried area.

```java
// creates a new query around [37.7832, -122.4056] with a radius of 0.6 kilometers
GeoQuery geoQuery = geoFirestore.queryAtLocation(new GeoPoint(37.7832, -122.4056), 0.6);
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

```java
geoQuery.addGeoQueryEventListener(new GeoQueryEventListener() {
    @Override
    public void onKeyEntered(String documentID, GeoPoint location) {
        System.out.println(String.format("Document %s entered the search area at [%f,%f]", documentID, location.getLatitude(), location.getLongitude()));
    }

    @Override
    public void onKeyExited(String documentID) {
        System.out.println(String.format("Document %s is no longer in the search area", documentID));
    }

    @Override
    public void onKeyMoved(String documentID, GeoPoint location) {
        System.out.println(String.format("Document %s moved within the search area to [%f,%f]", documentID, location.getLatitude(), location.getLongitude()));
    }

    @Override
    public void onGeoQueryReady() {
        System.out.println("All initial data has been loaded and events have been fired!");
    }

    @Override
    public void onGeoQueryError(Exception exception) {
        System.err.println("There was an error with this query: " + exception.getLocalizedMessage());
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

```java
geoQuery.addGeoQueryDataEventListener(new GeoQueryDataEventListener() {
    @Override
    public void onDocumentEntered(DocumentSnapshot documentSnapshot, GeoPoint location) {
        // ...
    }

    @Override
    public void onDocumentExited(DocumentSnapshot documentSnapshot) {
        // ...
    }

    @Override
    public void onDocumentMoved(DocumentSnapshot documentSnapshot, GeoPoint location) { 
        // ...
    }

    @Override
    public void onDocumentChanged(DocumentSnapshot documentSnapshot, GeoPoint location) {
        // ...
    }

    @Override
    public void onGeoQueryReady() {
        // ...
    }

    @Override
    public void onGeoQueryError(Exception exception) {
        // ...
    }
});

```

#### Updating the query criteria

The `GeoQuery` search area can be changed with `setCenter` and `setRadius`. Key
exited and key entered events will be fired for documemts moving in and out of
the old and new search area, respectively. No key moved events will be
fired; however, key moved events might occur independently.

Updating the search area can be helpful in cases such as when you need to update
the query to the new visible map area after a user scrolls.

## API Reference & Documentation

Full API reference and documentation is available [here](https://imperiumlabs.github.io/GeoFirestore-Android/)

## License

GeoFirestore is available under the MIT license. See the LICENSE file for more info.

Copyright (c) 2018 Imperium Labs
