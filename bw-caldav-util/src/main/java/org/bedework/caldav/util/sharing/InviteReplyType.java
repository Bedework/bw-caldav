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
import org.bedework.caldav.util.sharing.parse.Parser;
import org.bedework.base.ToString;
import org.bedework.util.xml.XmlEmit;
import org.bedework.util.xml.tagdefs.AppleServerTags;
import org.bedework.util.xml.tagdefs.WebdavTags;
import org.bedework.webdav.servlet.shared.UrlPrefixer;
import org.bedework.webdav.servlet.shared.UrlUnprefixer;

import java.util.List;

import javax.xml.namespace.QName;

/** Class to represent reply to a sharing request.
 *
 * @author Mike Douglass douglm
 */
public class InviteReplyType extends BaseNotificationType {
  /** This seems to be required by the caldav tests
   */
  public static final String sharedTypeCalendar = "calendar";

  private String sharedType;
  private String href;
  private Boolean accepted;
  private String hostUrl;
  private String inReplyTo;
  private String summary;
  private String commonName;

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
   * @param val accepted
   */
  public void setAccepted(final Boolean val) {
    accepted = val;
  }

  /**
   * @return accepted
   */
  public Boolean getAccepted() {
    return accepted;
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
   * @param val inReplyTo
   */
  public void setInReplyTo(final String val) {
    inReplyTo = val;
  }

  /**
   * @return inReplyTo
   */
  public String getInReplyTo() {
    return inReplyTo;
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

  /* ====================================================================
   *                   BaseNotificationType methods
   * ==================================================================== */

  @Override
  public QName getElementName() {
    return AppleServerTags.inviteReply;
  }

  @Override
  public String getName() {
    if (super.getName() == null) {
      setName(getInReplyTo() + "-reply");
    }

    return super.getName();
  }

  @Override
  public void setEncoding(final String val) {
  }

  @Override
  public String getEncoding() {
    return null;
  }

  @Override
  public List<AttributeType> getElementAttributes() {
    return null;
  }

  @Override
  public void prefixHrefs(final UrlPrefixer prefixer) {
    setHostUrl(prefixer.prefix(getHostUrl()));
  }

  @Override
  public void unprefixHrefs(final UrlUnprefixer unprefixer) {
    setHostUrl(unprefixer.unprefix(getHostUrl()));
  }

  /**
   * @param xml builder
   */
  @Override
  public void toXml(final XmlEmit xml) {
    if (getSharedType() != null) {
      xml.openTag(AppleServerTags.inviteReply,
                  "shared-type", getSharedType());
    } else {
      xml.openTag(AppleServerTags.inviteReply,
                  "shared-type", sharedTypeCalendar);
    }

    /* base notification fields */
    super.toXml(xml);

    xml.property(WebdavTags.href, getHref());
    if (testAccepted()) {
      xml.emptyTag(AppleServerTags.inviteAccepted);
    } else {
      xml.emptyTag(AppleServerTags.inviteDeclined);
    }

    if (getCommonName() == null) {
      xml.property(AppleServerTags.commonName, getHref());
    } else {
      xml.property(AppleServerTags.commonName, getCommonName());
    }

    xml.openTag(AppleServerTags.hosturl);
    xml.property(WebdavTags.href, getHostUrl());
    xml.closeTag(AppleServerTags.hosturl);
    xml.property(AppleServerTags.inReplyTo, getInReplyTo());
    xml.property(AppleServerTags.summary, getSummary());
    xml.closeTag(AppleServerTags.inviteReply);
  }

  /* ==============================================================
   *                   Convenience methods
   * ============================================================== */

  /**
   * @return true if accepted set and true, false otherwise
   */
  public boolean testAccepted() {
    final Boolean f = getAccepted();
    if (f == null) {
      return false;
    }

    return f;
  }

  /** Add our stuff to the ToString object
   *
   * @param ts to build in
   */
  protected void toStringSegment(final ToString ts) {
    ts.append("sharedType", getSharedType());
    ts.append("href", getHref());
    ts.append("accepted", getAccepted());
    ts.append("hostUrl", getHostUrl());
    ts.append("inReplyTo", getInReplyTo());
    ts.append("summary", getSummary());
    ts.append("commonName", getCommonName());
  }

  @Override
  public String toString() {
    final ToString ts = new ToString(this);

    toStringSegment(ts);

    return ts.toString();
  }

  @Override
  public Object clone() {
    try {
      final String xml = toXml();
      return new Parser().parseInviteReply(xml);
    } catch (final Throwable t) {
      throw new RuntimeException(t);
    }
  }
}
