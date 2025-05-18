# bw-caldav [![Build Status](https://travis-ci.org/Bedework/bw-caldav.svg)](https://travis-ci.org/Bedework/bw-caldav)

A generic CalDAV server which interacts with a back end to access the
resources. Provides a CalDAV server for
[Bedework](https://www.apereo.org/projects/bedework).

A functioning CalDAV server can be built by fully implementing the abstract
SysIntf class.

Supports most of the CalDAV and related RFCs:

* RFC4791 - CalDAV access
* RFC 5397 - WebDAV Current Principal Extension
* RFC 5689 - Extended MKCOL for Web Distributed Authoring and Versioning (WebDAV)
* RFC 5995 - Using POST to Add Members to Web Distributed Authoring and Versioning (WebDAV) Collections
* RFC 6638 - Scheduling Extensions to CalDAV
* RFC 7809 - Calendaring Extensions to WebDAV (CalDAV): Time Zones by Reference

Additionally, supports calendar sharing as implemented by other services.

## Requirements

1. JDK 21
2. Maven 3

## Building Locally

> mvn clean install

## Releasing

Releases of this fork are published to Maven Central via Sonatype.

To create a release, you must have:

1. Permissions to publish to the `org.bedework` groupId.
2. `gpg` installed with a published key (release artifacts are signed).

To perform a new release use the release script:

> ./bedework/build/quickstart/linux/util-scripts/release.sh <module-name> "<release-version>" "<new-version>-SNAPSHOT"

When prompted, indicate all updates are committed

For full details, see [Sonatype's documentation for using Maven to publish releases](http://central.sonatype.org/pages/apache-maven.html).

## Release Notes
### 4.0.9
* Remove errant content-type header from calws.
* Missing continue. Caused parse failure in invite-reply

#### 4.0.10
* Update library versions
* Look for jscalendar content in ACCEPT

#### 4.0.11
* Update library versions

#### 4.0.12
* Update library versions

#### 4.0.13
* Update library versions

#### 4.0.14
* Update library versions

#### 4.0.15
* Update library versions

#### 4.0.16
* Update library versions

#### 4.0.17
* Update library versions

#### 4.0.18
* Update library versions

#### 5.0.0
* Use bedework-parent for builds
*  Upgrade library versions

#### 5.0.1
*  Upgrade library versions
* Process put with jscalendar content
* Add a limit to how long the sync token is valid. Will allow flushing of old tombstoned data

#### 5.0.2
* Upgrade library versions
* Fix dependency on bw-calws-soap-xml module
* Add bw-caldav-xml module

#### 5.0.3
* Release 5.0.2 failed because of missing name. Redo.

#### 5.0.4
* Upgrade library versions
* Updated version of bw-calws-soap    

#### 5.0.5
* Upgrade library versions
* Make webdavexception subclass of runtimeexception and tidy up a bit. Should be no noticable changes.

#### 5.0.6
* Upgrade library versions

#### 5.0.7
* Upgrade library versions

#### 5.0.8
* Upgrade library versions
* Split jdkim into api and library. Use api only as a dependency. Obtain library by dynamic loading.

#### 5.0.9
* Upgrade library versions
* Move response classes and ToString into bw-base module.
* Pre-jakarta
