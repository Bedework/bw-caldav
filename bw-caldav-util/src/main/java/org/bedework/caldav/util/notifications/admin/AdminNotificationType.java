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
package org.bedework.caldav.util.notifications.admin;

import org.bedework.caldav.util.notifications.BaseNotificationType;
import org.bedework.base.ToString;
import org.bedework.util.xml.XmlEmit;
import org.bedework.util.xml.tagdefs.AppleServerTags;
import org.bedework.util.xml.tagdefs.BedeworkServerTags;
import org.bedework.util.xml.tagdefs.WebdavTags;
import org.bedework.webdav.servlet.shared.UrlPrefixer;
import org.bedework.webdav.servlet.shared.UrlUnprefixer;

import java.util.ArrayList;
import java.util.List;

/** Base class for event administration notifications
 *
 * @author Mike Douglass
 */
public abstract class AdminNotificationType extends BaseNotificationType {
  private String uid;
  private String href; // Of the entity if any
  private String principalHref; // Of the registered user
  private String comment;
  private String calsuiteHref; // Of the calsuite group

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

  /** href of the entity
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

  /** href of the registered principal
   *
   * @param val the href
   */
  public void setPrincipalHref(final String val) {
    principalHref = val;
  }

  /**
   * @return the href
   */
  public String getPrincipalHref() {
    return principalHref;
  }

  /**
   * @param val the comment
   */
  public void setComment(final String val) {
    comment = val;
  }

  /**
   * @return the summary
   */
  public String getComment() {
    return comment;
  }

  public String getCalsuiteHref() {
    return calsuiteHref;
  }

  public void setCalsuiteHref(final String calsuiteHref) {
    this.calsuiteHref = calsuiteHref;
  }

  /* ====================================================================
   *                   BaseNotificationType methods
   * ==================================================================== */

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
    if (attrs != null) {
      return attrs;
    }

    attrs = new ArrayList<>();

    return attrs;
  }

  @Override
  public void prefixHrefs(final UrlPrefixer prefixer) {
    setHref(prefixer.prefix(getHref()));
    setPrincipalHref(prefixer.prefix(getPrincipalHref()));
    setCalsuiteHref(prefixer.prefix(getCalsuiteHref()));
  }

  @Override
  public void unprefixHrefs(final UrlUnprefixer unprefixer) {
    setHref(unprefixer.unprefix(getHref()));
    setPrincipalHref(unprefixer.unprefix(getPrincipalHref()));
    setCalsuiteHref(unprefixer.unprefix(getCalsuiteHref()));
  }

  protected void bodyToXml(final XmlEmit xml) {
    /* base notification fields */
    super.toXml(xml);

    xml.property(AppleServerTags.uid, getUid());
    xml.property(WebdavTags.href, getHref());
    xml.openTag(WebdavTags.principalURL);
    xml.property(WebdavTags.href, getPrincipalHref());
    xml.closeTag(WebdavTags.principalURL);
    xml.property(BedeworkServerTags.comment, getComment());
    xml.openTag(BedeworkServerTags.calsuiteURL);
    xml.property(WebdavTags.href, getCalsuiteHref());
    xml.closeTag(BedeworkServerTags.calsuiteURL);
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
    ts.append("principalHref", getPrincipalHref());
    ts.append("comment", getComment());
    ts.append("calsuiteHref", getCalsuiteHref());
  }

  @Override
  public String toString() {
    final ToString ts = new ToString(this);

    toStringSegment(ts);

    return ts.toString();
  }
}
