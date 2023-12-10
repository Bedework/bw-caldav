# bw-caldav [![Build Status](https://travis-ci.org/Bedework/bw-caldav.svg)](https://travis-ci.org/Bedework/bw-caldav)

A generic CalDAV server which interacts with a back end to access the
resources. Provides a CalDAV server for
[Bedework](https://www.apereo.org/projects/bedework).

A functioning CalDAV server can be built by fully implementing the abstract
SysIntf class.

## Requirements

1. JDK 11
2. Maven 3

## Building Locally

> mvn clean install

## Releasing

Releases of this fork are published to Maven Central via Sonatype.

To create a release, you must have:

1. Permissions to publish to the `org.bedework` groupId.
2. `gpg` installed with a published key (release artifacts are signed).

To perform a new release:

> mvn -P bedework-dev release:clean release:prepare

When prompted, select the desired version; accept the defaults for scm tag and next development version.
When the build completes, and the changes are committed and pushed successfully, execute:

> mvn -P bedework-dev release:perform

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
    
