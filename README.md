# GeoFirestore for Android/Java — Realtime location queries with Firestore

GeoFirestore is an open-source library for Android/Java that allows you to store and query a set of documents based on their geographic location.

At its heart, GeoFirestore simply stores locations with string keys. Its main benefit however, is the possibility of querying documents within a given geographic area - all in realtime.

GeoFirestore uses the Firestore database for data storage, allowing query results to be updated in realtime as they change. GeoFirestore selectively loads only the data near certain locations, keeping your applications light and responsive, even with extremely large datasets.

A compatible GeoFirestore client is also available for [Swift](https://github.com/imperiumlabs/GeoFirestore-Swift).

### Integrating GeoFirestore with your data

GeoFirestore is designed as a lightweight add-on to Firestore. However, to keep things simple, GeoFirestore stores data in its own format and its own location within your Firestore database. This allows your existing data format and security rules to remain unchanged and for you to add GeoFirestore as an easy solution for geo queries without modifying your existing data.

### Example usage

Assume you are building an app to rate bars, and you store all information for a bar (e.g. name, business hours and price range) at `collection(bars).document(bar-id)`. Later, you want to add the possibility for users to search for bars in their vicinity. This is where GeoFirestore comes in. You can store the location for each bar document using GeoFirestore. GeoFirestore then allows you to easily query which bar are nearby.

## Including GeoFirestore in your Android/Java project 

In order to use GeoFirestore in your project, you need to [add the Firestore Android
SDK](https://firebase.google.com/docs/firestore/quickstart). After that you can include GeoFirestore as shown below.

### Gradle

Add a dependency for GeoFirestore to your `gradle.build` file:

```groovy
dependencies {
compile 'com.imperiumlabs:geofirestore:0.1.0'
}
```

## Getting Started with Firestore

GeoFirestore requires the Firestore database in order to store location data. You can [learn more about Firestore here](https://firebase.google.com/docs/firestore/).

## License

GeoFirestore is available under the MIT license. See the LICENSE file for more info.

Copyright (c) 2018 Imperium Labs

