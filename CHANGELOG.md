# Release Notes

This project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## Unreleased (6.1.0-SNAPSHOT)

## [6.0.0-SNAPSHOT] - 2025-07-01
* First jakarta release
* Use a more fluent approach for responses
* Fix for possible NPE for missing config values.

## [5.0.9] - 2025-02-06
* Upgrade library versions
* Move response classes and ToString into bw-base module.
* Pre-jakarta

## [5.0.8] - 2024-11-23
* Upgrade library versions
* Split jdkim into api and library. Use api only as a dependency. Obtain library by dynamic loading.

## [5.0.7] - 2024-11-13
* Upgrade library versions

## [5.0.6] - 2024-11-10
* Upgrade library versions

## [5.0.5] - 2024-09-18
* Upgrade library versions
* Make webdavexception subclass of runtimeexception and tidy up a bit. There should be no noticeable changes.

## [5.0.4] - 2024-03-23
* Upgrade library versions
* Updated version of bw-calws-soap

## [5.0.3] - 2024-03-21
* Release 5.0.2 failed because of missing name. Redo.

## [5.0.2] - 2024-03-21
* Upgrade library versions
* Fix dependency on bw-calws-soap-xml module
* Add bw-caldav-xml module

## [5.0.1] - 2023-12-10
*  Upgrade library versions
* Process put with jscalendar content
* Add a limit to how long the sync token is valid. Will allow flushing of old tombstoned data

## [5.0.0] - 2022-02-12
* Use bedework-parent for builds
*  Upgrade library versions

## [4.0.18] - 2021-11-15
* Update library versions

## [4.0.17] - 2021-10-29
* Update library versions

## [4.0.16] - 2021-09-14
* Update library versions

## [4.0.15] - 2021-11-09
* Update library versions

## [4.0.14] - 2021-09-05
* Update library versions

## [4.0.13] - 2021-06-07
* Update library versions

## [4.0.12] - 2021-06-03
* Update library versions

## [4.0.11] - 2021-06-02
* Update library versions

## [4.0.10] - 2021-06-02
* Update library versions
* Look for jscalendar content in ACCEPT

## [4.0.9] - 2020-03-20
* Remove errant content-type header from calws.
* Missing continue. Caused parse failure in invite-reply
