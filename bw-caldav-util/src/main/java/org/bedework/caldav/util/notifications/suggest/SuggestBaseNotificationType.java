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
package org.bedework.caldav.util.notifications.suggest;

import org.bedework.caldav.util.notifications.BaseNotificationType;
import org.bedework.util.misc.ToString;
import org.bedework.util.xml.XmlEmit;
import org.bedework.util.xml.tagdefs.AppleServerTags;
import org.bedework.util.xml.tagdefs.BedeworkServerTags;
import org.bedework.util.xml.tagdefs.WebdavTags;
import org.bedework.webdav.servlet.shared.UrlPrefixer;
import org.bedework.webdav.servlet.shared.UrlUnprefixer;

import java.util.ArrayList;
import java.util.List;

/** Base class fro suggestion and responses
 *
 * @author Mike Douglass douglm
 */
public abstract class SuggestBaseNotificationType extends
        BaseNotificationType {
  private String uid;
  private String href; // Of the entity
  private String suggesterHref; // Who's suggesting
  private String suggesteeHref; // Who's being suggested to
  private String comment;

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

  /** href of the group that sent the message
   *
   * @param val the href
   */
  public void setSuggesterHref(final String val) {
    suggesterHref = val;
  }

  /**
   * @return the href
   */
  public String getSuggesterHref() {
    return suggesterHref;
  }

  /** href of the group that received the message
   *
   * @param val the href
   */
  public void setSuggesteeHref(final String val) {
    suggesteeHref = val;
  }

  /**
   * @return the href
   */
  public String getSuggesteeHref() {
    return suggesteeHref;
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
    setSuggesterHref(prefixer.prefix(getSuggesterHref()));
    setSuggesteeHref(prefixer.prefix(getSuggesteeHref()));
  }

  @Override
  public void unprefixHrefs(final UrlUnprefixer unprefixer) {
    setHref(unprefixer.unprefix(getHref()));
    setSuggesterHref(unprefixer.unprefix(getSuggesterHref()));
    setSuggesteeHref(unprefixer.unprefix(getSuggesteeHref()));
  }

  protected void bodyToXml(final XmlEmit xml) {
    /* base notification fields */
    super.toXml(xml);

    xml.property(AppleServerTags.uid, getUid());
    xml.property(WebdavTags.href, getHref());
    xml.property(BedeworkServerTags.suggesterHref, getSuggesterHref());
    xml.property(BedeworkServerTags.suggesteeHref, getSuggesteeHref());
    xml.property(BedeworkServerTags.comment, getComment());
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
    ts.append("suggesterHref", getSuggesterHref());
    ts.append("suggesteeHref", getSuggesteeHref());
    ts.append("comment", getComment());
  }

  @Override
  public String toString() {
    final ToString ts = new ToString(this);

    toStringSegment(ts);

    return ts.toString();
  }
}
