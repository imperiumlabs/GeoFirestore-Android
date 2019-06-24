# Changelog
All notable changes to this project will be documented in this file.

## [Unreleased] -
### Added
- Methods in GeoUtils to convert a radius in m or km
- The ability to pass a filter query to GeoQuery in order to filter the documents obtained in the query
- Some Unit Test class

### Changed
- Converted the GeoQuery class to Kotlin
- GeoQuery constructor work with a radius in km and without the need to cap-it
- GeoLocation constructor accept a GeoPoint 
- Updated some external dependency

### Removed
- Ability to  get the Firestore query(s) from the GeoQuery

## [v1.5.0] - 2019-06-24
### Added
- Ability to parse document with "g" as List or GeoPoint
- Added this CHANGELOG.md
- method getAtLocation to GeoFirestore replacing the SingleGeoQuery class
- SingleGeoQueryDataEventCallback to GeoFirestore

### Changed
- Reorganized the entire project structure
- Type of the "g" parameter from List<Doube> to GeoPoint
- The SingleGeoQuery is a method in the GeoFirestore class
- Updated the README.md with the newest features
- Changed package "callbacks" to "listeners"

### Removed
- SingleGeoQuery class
- SingleGeoQueryDataEventCallback 

## [v1.4.0] - 2019-05-22
### Added
- Ability to make a "one-shot" geo query using SingleGeoQuery

### Changed
- Moved all the callbacks in to a separate package "/callbacks"

### Fixed
- Build failure of v1.3.0

## [v1.3.0] - 2019-03-28
Most of the classes are converted to Kotlin

### Added
- Method setLocation accept Kotlin's lambda function 
- Method getLocation accept Kotlin's lambda function 
- Method removeLocation accept Kotlin's lambda function
- Ability to  get the Firestore query(s) from the GeoQuery

### Fixed
- Geo Hashing algorithm

### Changed
- Migration to AndroidX

## [v1.2.1] - 2019-02-26
### Removed
- GeoQuery throw assertionError

## [v1.2.0] - 2019-02-26
### Fixed
- issue with handle being null

### Updated
- firestore dependency

## [v1.1.1] - 2018-07-11
### Fixed
- Small changes to README.MD

## [v1.1.0] - 2018-07-10
### Fixed
- Small changes to README.MD

## [v1.0.0] - 2018-07-10
### Fixed
- Small changes to README.MD

## [v0.1.0] - 2018-07-10
First version. 

### Added 
- All the core functionality 