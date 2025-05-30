# bw-caldav [![Build Status](https://travis-ci.org/Bedework/bw-caldav.svg)](https://travis-ci.org/Bedework/bw-caldav)

A generic CalDAV server which interacts with a back end to access the
resources. Provides a CalDAV server for
[Bedework](https://www.apereo.org/projects/bedework).

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

## Using this project
Add the artifact(s) as a dependency to your project:
```
      <dependency>
        <groupId>org.bedework</groupId>
        <artifactId>bw-caldav-server</artifactId>
        <version>${bw-caldav.version}</version>
      </dependency>

      <dependency>
        <groupId>org.bedework</groupId>
        <artifactId>bw-caldav-util</artifactId>
        <version>${bw-caldav.version}</version>
      </dependency>

      <dependency>
        <groupId>org.bedework</groupId>
        <artifactId>bw-caldav-xml</artifactId>
        <version>${bw-caldav.version}</version>
      </dependency>
```

Fully implement the abstract
org.bedework.caldav.server.sysinterface.SysIntf class. Note that a subset of CalDAV - e.g. read-only, will only require a subset of the implemented methods to be functional. 

## Reporting Issues
Please report issues via the github issues tab at 
> https://github.com/Bedework/bw-caldav/issues 

## Contributing
See [CONTRIBUTING.md](CONTRIBUTING.md).

## Security - Vulnerability reporting
See [SECURITY.md](SECURITY.md).
