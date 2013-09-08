/* ********************************************************************
    Licensed to Jasig under one or more contributor license
    agreements. See the NOTICE file distributed with this work
    for additional information regarding copyright ownership.
    Jasig licenses this file to you under the Apache License,
    Version 2.0 (the "License"); you may not use this file
    except in compliance with the License. You may obtain a
    copy of the License at:

    http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing,
    software distributed under the License is distributed on
    an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
    KIND, either express or implied. See the License for the
    specific language governing permissions and limitations
    under the License.
*/
package org.bedework.caldav.util.notifications;

import org.bedework.util.misc.ToString;
import org.bedework.util.xml.XmlEmit;
import org.bedework.util.xml.tagdefs.AppleServerTags;
import org.bedework.util.xml.tagdefs.WebdavTags;

/**  <!ELEMENT changed-by (common-name | (first-name, last-name),
                           dtstamp?, DAV:href)>
       <!ELEMENT common-name CDATA>
       <!ELEMENT first-name CDATA>
       <!ELEMENT last-name CDATA>
     <!-- CS:changed-by indicates who made the change that caused the
          notification. CS:first-name and CS:last-name are the first
          and last names of the corresponding user. or the
          CS:common-name is the overall display name. CS:dtstamp is the
          time in UTC when the change was made. The DAV:href element
          is the principal URI or email address of the user who made
          the change. -->

 *
 * @author Mike Douglass douglm
 */
public class ChangedByType {
  private String commonName;
  private String firstName;
  private String lastName;
  private String dtstamp;
  private String href;

  /**
   * @param val the first name
   */
  public void setFirstName(final String val) {
    firstName = val;
  }

  /**
   * @return the first name
   */
  public String getFirstName() {
    return firstName;
  }

  /**
   * @param val the last name
   */
  public void setLastName(final String val) {
    lastName = val;
  }

  /**
   * @return the last name
   */
  public String getLastName() {
    return lastName;
  }

  /**
   * @param val the common name
   */
  public void setCommonName(final String val) {
    commonName = val;
  }

  /**
   * @return the common name
   */
  public String getCommonName() {
    return commonName;
  }

  /**
   * @param val the dtstamp
   */
  public void setDtstamp(final String val) {
    dtstamp = val;
  }

  /**
   * @return the dtstamp
   */
  public String getDtstamp() {
    return dtstamp;
  }

  /**
   * @param val the href
   */
  public void setHref(final String val) {
    href = val;
  }

  /**
   * @return the href
   */
  public String getHref() {
    return href;
  }

  /* ====================================================================
   *                   Convenience methods
   * ==================================================================== */

  /**
   * @param xml
   * @throws Throwable
   */
  public void toXml(final XmlEmit xml) throws Throwable {
    xml.openTag(AppleServerTags.changedBy);

    if (getCommonName() != null) {
      xml.property(AppleServerTags.commonName, getCommonName());
    } else {
      xml.property(AppleServerTags.firstName, getFirstName());
      xml.property(AppleServerTags.lastName, getLastName());
    }
    if (getDtstamp() != null) {
      xml.property(AppleServerTags.dtstamp, getDtstamp());
    }
    xml.property(WebdavTags.href, getHref());
    xml.closeTag(AppleServerTags.changedBy);
  }

  /** Add our stuff to the StringBuffer
   *
   * @param ts
   */
  protected void toStringSegment(final ToString ts) {
    if (getCommonName() != null) {
      ts.append("commonName", getCommonName());
    } else {
      ts.append("firstName", getFirstName());
      ts.append("lastName", getLastName());
    }
    ts.append("dtstamp", getDtstamp());
    ts.append("href", getHref());
  }

  @Override
  public String toString() {
    ToString ts = new ToString(this);

    toStringSegment(ts);

    return ts.toString();
  }
}
