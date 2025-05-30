# Contributing
Contributions may take the form of suggested updates, bug fixes or documentation or just discussion. Pull requests may be submitted via github.

## Coding Rules
Follow these Java coding guidelines:
* [Google Java Style Guide](https://google.github.io/styleguide/javaguide.html);
  * Limit line length to about 72 characters
  * Grouping parentheses always
  * Annotations always on their own line
* [Oracle Secure Coding Guidelines for Java SE](http://www.oracle.com/technetwork/java/seccodeguide-139067.html).

## Building Locally

> mvn clean install

## Releasing

Releases of this fork are published to Maven Central via Sonatype.

To create a release, you must have:

1. Permissions to publish to the `org.bedework` groupId.
2. `gpg` installed with a published key (release artifacts are signed).

To perform a new release, use the release script in the bedework repository at https://github.com/Bedework/bedework:

1. Clone this repository and the bedework repository into the same directory
2. In that directory do:

> ./bedework/build/quickstart/linux/util-scripts/release.sh <module-name> "<release-version>" "<new-version>-SNAPSHOT"

When prompted, indicate all updates are committed

For full details, see [Sonatype's documentation for using Maven to publish releases](http://central.sonatype.org/pages/apache-maven.html).
