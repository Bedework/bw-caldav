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
  private String href;
  private Boolean accepted;
  private String hostUrl;
  private String inReplyTo;
  private String summary;

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

  /* ====================================================================
   *                   BaseNotificationType methods
   * ==================================================================== */

  @Override
  public QName getElementName() {
    return AppleServerTags.inviteReply;
  }

  @Override
  public void setName(final String val) {
  }

  @Override
  public String getName() {
    return getInReplyTo() + "-reply";
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
  public void prefixHrefs(final UrlPrefixer prefixer) throws Throwable {
    setHostUrl(prefixer.prefix(getHostUrl()));
  }

  @Override
  public void unprefixHrefs(final UrlUnprefixer unprefixer) throws Throwable {
    setHostUrl(unprefixer.unprefix(getHostUrl()));
  }

  /**
   * @param xml
   * @throws Throwable
   */
  @Override
  public void toXml(final XmlEmit xml) throws Throwable {
    xml.openTag(AppleServerTags.inviteReply);
    xml.property(WebdavTags.href, getHref());
    if (testAccepted()) {
      xml.emptyTag(AppleServerTags.inviteAccepted);
    } else {
      xml.emptyTag(AppleServerTags.inviteDeclined);
    }

    xml.openTag(AppleServerTags.hosturl);
    xml.property(WebdavTags.href, getHostUrl());
    xml.closeTag(AppleServerTags.hosturl);
    xml.property(AppleServerTags.inReplyTo, getInReplyTo());
    xml.property(AppleServerTags.summary, getSummary());
    xml.closeTag(AppleServerTags.inviteReply);
  }

  /* ====================================================================
   *                   Convenience methods
   * ==================================================================== */

  /**
   * @return true if accepted set and true, false otherwise
   */
  public boolean testAccepted() {
    Boolean f = getAccepted();
    if (f == null) {
      return false;
    }

    return f;
  }

  /** Add our stuff to the StringBuffer
   *
   * @param ts
   */
  protected void toStringSegment(final ToString ts) {
    ts.append("href", getHref());
    ts.append("accepted", getAccepted());
    ts.append("hostUrl", getHostUrl());
    ts.append("inReplyTo", getInReplyTo());
    ts.append("summary", getSummary());
  }

  @Override
  public String toString() {
    ToString ts = new ToString(this);

    toStringSegment(ts);

    return ts.toString();
  }
}
