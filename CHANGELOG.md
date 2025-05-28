# Release Notes
## 4.0.9
* Remove errant content-type header from calws.
* Missing continue. Caused parse failure in invite-reply

## 4.0.10
* Update library versions
* Look for jscalendar content in ACCEPT

## 4.0.11
* Update library versions

## 4.0.12
* Update library versions

## 4.0.13
* Update library versions

## 4.0.14
* Update library versions

## 4.0.15
* Update library versions

## 4.0.16
* Update library versions

## 4.0.17
* Update library versions

## 4.0.18
* Update library versions

## 5.0.0
* Use bedework-parent for builds
*  Upgrade library versions

## 5.0.1
*  Upgrade library versions
* Process put with jscalendar content
* Add a limit to how long the sync token is valid. Will allow flushing of old tombstoned data

## 5.0.2
* Upgrade library versions
* Fix dependency on bw-calws-soap-xml module
* Add bw-caldav-xml module

## 5.0.3
* Release 5.0.2 failed because of missing name. Redo.

## 5.0.4
* Upgrade library versions
* Updated version of bw-calws-soap

## 5.0.5
* Upgrade library versions
* Make webdavexception subclass of runtimeexception and tidy up a bit. There should be no noticeable changes.

## 5.0.6
* Upgrade library versions

## 5.0.7
* Upgrade library versions

## 5.0.8
* Upgrade library versions
* Split jdkim into api and library. Use api only as a dependency. Obtain library by dynamic loading.

## 5.0.9
* Upgrade library versions
* Move response classes and ToString into bw-base module.
* Pre-jakarta

## 6.0.0-SNAPSHOT
* First jakarta release
* Use a more fluent approach for responses
* Fix for possible NPE for missing config values.
