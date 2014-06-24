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

import org.bedework.caldav.util.notifications.BaseNotificationType;
import org.bedework.util.misc.ToString;
import org.bedework.util.xml.XmlEmit;
import org.bedework.util.xml.tagdefs.AppleServerTags;
import org.bedework.util.xml.tagdefs.CaldavTags;
import org.bedework.util.xml.tagdefs.WebdavTags;

import org.bedework.webdav.servlet.shared.UrlPrefixer;
import org.bedework.webdav.servlet.shared.UrlUnprefixer;

import java.util.ArrayList;
import java.util.List;

import javax.xml.namespace.QName;

/** Class to represent a sharing request.
 *
 * @author Mike Douglass douglm
 */
public class InviteNotificationType extends BaseNotificationType {
  /**
   */
  public static final String sharedTypeCalendar = "calendar";

  private String sharedType;
  private String uid;
  private String href;
  private QName inviteStatus;
  private AccessType access;
  private String hostUrl;
  private OrganizerType organizer;
  private String summary;
  private List<String> supportedComponents;

  private QName previousStatus;

  /**
   * @param val the sharedType
   */
  public void setSharedType(final String val) {
    sharedType = val;
  }

  /**
   * @return the sharedType
   */
  public String getSharedType() {
    return sharedType;
  }

  /**
   * @param val the uid
   */
  public void setUid(final String val) {
    uid = val;
  }

  /**
   * @return the uid
   */
  public String getUid() {
    return uid;
  }

  /** calendar user address of the sharee to whom the message was sent
   *
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
   * @param val the hostUrl
   */
  public void setHostUrl(final String val) {
    hostUrl = val;
  }

  /**
   * @return the hostUrl
   */
  public String getHostUrl() {
    return hostUrl;
  }

  /**
   * @param val organizer
   */
  public void setOrganizer(final OrganizerType val) {
    organizer = val;
  }

  /**
   * @return organizer
   */
  public OrganizerType getOrganizer() {
    return organizer;
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
   * @return the supported components
   */
  public List<String> getSupportedComponents() {
    if (supportedComponents == null) {
      supportedComponents = new ArrayList<>();
    }

    return supportedComponents;
  }

  /**
   * @param val status we found in the shared resource
   */
  public void setPreviousStatus(final QName val) {
    previousStatus = val;
  }

  /**
   * @return status we found in the shared resource
   */
  public QName getPreviousStatus() {
    return previousStatus;
  }

  /* ====================================================================
   *                   BaseNotificationType methods
   * ==================================================================== */

  @Override
  public QName getElementName() {
    return AppleServerTags.inviteNotification;
  }

  @Override
  public void setName(final String val) {
  }

  @Override
  public String getName() {
    return getUid();
  }

  @Override
  public void setEncoding(final String val) {
  }

  @Override
  public String getEncoding() {
    return null;
  }

  private List<AttributeType> attrs;

  @Override
  public List<AttributeType> getElementAttributes() {
    if ((attrs != null) || (getSharedType() == null)) {
      return attrs;
    }

    attrs = new ArrayList<>();

    attrs.add(new AttributeType("shared-type", getSharedType()));

    return attrs;
  }

  @Override
  public void prefixHrefs(final UrlPrefixer prefixer) throws Throwable {
    setHostUrl(prefixer.prefix(getHostUrl()));
  }

  @Override
  public void unprefixHrefs(final UrlUnprefixer unprefixer) throws Throwable {
    setHostUrl(unprefixer.unprefix(getHostUrl()));
  }

  @Override
  public void toXml(final XmlEmit xml) throws Throwable {
    if (getSharedType() != null) {
      xml.openTag(AppleServerTags.inviteNotification,
                  "shared-type", getSharedType());
    } else {
      xml.openTag(AppleServerTags.inviteNotification);
    }

    xml.property(AppleServerTags.uid, getUid());
    xml.property(WebdavTags.href, getHref());
    xml.emptyTag(getInviteStatus());
    if (getAccess() != null) {
      getAccess().toXml(xml);
    }
    xml.openTag(AppleServerTags.hosturl);
    xml.property(WebdavTags.href, getHostUrl());
    xml.closeTag(AppleServerTags.hosturl);
    if (getOrganizer() != null) {
      getOrganizer().toXml(xml);
    }
    xml.property(AppleServerTags.summary, getSummary());

    xml.openTag(CaldavTags.supportedCalendarComponentSet);
    for (final String s: getSupportedComponents()) {
      xml.emptyTag(CaldavTags.comp, "name", s);
    }
    xml.closeTag(CaldavTags.supportedCalendarComponentSet);

    xml.closeTag(AppleServerTags.inviteNotification);
  }

  /* ====================================================================
   *                   Convenience methods
   * ==================================================================== */

  /** Add our stuff to the StringBuffer
   *
   * @param ts to build result
   */
  protected void toStringSegment(final ToString ts) {
    ts.append("uid", getUid());
    ts.append("href", getHref());
    ts.append("status", getInviteStatus());
    ts.append("access", getAccess());
    ts.append("hostUrl", getHostUrl());
    ts.append("organizer", getOrganizer());
    ts.append("summary", getSummary());
    ts.append("supportedComponents", getSupportedComponents());
  }

  @Override
  public String toString() {
    final ToString ts = new ToString(this);

    toStringSegment(ts);

    return ts.toString();
  }
}
