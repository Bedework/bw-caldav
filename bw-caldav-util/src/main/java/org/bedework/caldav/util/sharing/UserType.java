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
package org.bedework.caldav.util.sharing;

import org.bedework.util.misc.ToString;
import org.bedework.util.xml.XmlEmit;
import org.bedework.util.xml.tagdefs.AppleServerTags;
import org.bedework.util.xml.tagdefs.BedeworkServerTags;
import org.bedework.util.xml.tagdefs.WebdavTags;

import javax.xml.namespace.QName;

/** Class to represent a user in a sharing request.
 *
 * @author Mike Douglass douglm
 */
public class UserType {
  private String href;
  private String commonName;
  private QName inviteStatus;
  private AccessType access;
  private String summary;
  private boolean externalUser;

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
   * @param val status
   */
  public void setInviteStatus(final QName val) {
    inviteStatus = val;
  }

  /**
   * @return status
   */
  public QName getInviteStatus() {
    return inviteStatus;
  }

  /**
   * @param val access
   */
  public void setAccess(final AccessType val) {
    access = val;
  }

  /**
   * @return access
   */
  public AccessType getAccess() {
    return access;
  }

  /**
   * @param val the summary
   */
  public void setSummary(final String val) {
    summary = val;
  }

  /**
   * @return the summary
   */
  public String getSummary() {
    return summary;
  }

  /**
   * @param val true for an external user
   */
  public void setExternalUser(final boolean val) {
    externalUser = val;
  }

  /**
   * @return true for an external user
   */
  public boolean getExternalUser() {
    return externalUser;
  }

  /* ====================================================================
   *                   Convenience methods
   * ==================================================================== */

  /**
   * @param xml builder
   * @throws Throwable on fatal error
   */
  public void toXml(final XmlEmit xml) throws Throwable {
    xml.openTag(AppleServerTags.user);
    xml.property(WebdavTags.href, getHref());
    if (getCommonName() == null) {
      xml.property(AppleServerTags.commonName, getHref());
    } else {
      xml.property(AppleServerTags.commonName, getCommonName());
    }
    xml.emptyTag(getInviteStatus());
    getAccess().toXml(xml);
    xml.property(AppleServerTags.summary, getSummary());
    xml.property(BedeworkServerTags.externalUser,
                 String.valueOf(getExternalUser()));
    xml.closeTag(AppleServerTags.user);
  }

  /** Add our stuff to the StringBuffer
   *
   * @param ts builder
   */
  protected void toStringSegment(final ToString ts) {
    ts.append("href", getHref());
    ts.append("commonName", getCommonName());
    ts.append("status", getInviteStatus().toString());
    ts.append(getAccess().toString());
    ts.append("summary", getSummary());
    ts.append("externalUser", getExternalUser());
  }

  @Override
  public String toString() {
    final ToString ts = new ToString(this);

    toStringSegment(ts);

    return ts.toString();
  }

  /* ==============================================================
   *                   Object methods
   * ============================================================== */

  @Override
  public int hashCode() {
    return getHref().hashCode();
  }

  @Override
  public boolean equals(final Object o) {
    if (!(o instanceof final UserType that)) {
      return false;
    }

    return this.getHref().equals(that.getHref());
  }
}
