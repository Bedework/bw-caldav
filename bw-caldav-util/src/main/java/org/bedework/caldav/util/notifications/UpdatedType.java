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

import org.bedework.webdav.servlet.shared.UrlPrefixer;
import org.bedework.webdav.servlet.shared.UrlUnprefixer;

import java.util.ArrayList;
import java.util.List;

/**
     <!ELEMENT updated (DAV:href, changed-by?, content?,
                        DAV:prop?, calendar-changes*)>
 *
 * @author Mike Douglass douglm
 */
public class UpdatedType {
  private String href;
  private ChangedByType changedBy;
  private boolean content;
  private PropType prop;
  private List<CalendarChangesType> calendarChanges;

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
   * @param val the changedBy
   */
  public void setChangedBy(final ChangedByType val) {
    changedBy = val;
  }

  /**
   * @return the first name
   */
  public ChangedByType getChangedBy() {
    return changedBy;
  }

  /**
   * @param val the content flag
   */
  public void setContent(final boolean val) {
    content = val;
  }

  /**
   * @return the content flag
   */
  public boolean getContent() {
    return content;
  }
  /** The DAV:prop element indicates a change to WebDAV properties on the
   * calendar resource.
   *
   * @param val the prop
   */
  public void setProp(final PropType val) {
    prop = val;
  }

  /**
   * @return the prop
   */
  public PropType getProp() {
    return prop;
  }

  /**
   * @return the calendarChanges list
   */
  public List<CalendarChangesType> getCalendarChanges() {
    if (calendarChanges == null) {
      calendarChanges = new ArrayList<CalendarChangesType>();
    }

    return calendarChanges;
  }

  /* ====================================================================
   *                   Convenience methods
   * ==================================================================== */

  /** Called before we send it out via caldav
   *
   * @param prefixer
   * @throws Throwable
   */
  public void prefixHrefs(final UrlPrefixer prefixer) throws Throwable {
    setHref(prefixer.prefix(getHref()));
  }

  /** Called after we obtain it via caldav
   *
   * @param unprefixer
   * @throws Throwable
   */
  public void unprefixHrefs(final UrlUnprefixer unprefixer) throws Throwable {
    setHref(unprefixer.unprefix(getHref()));
  }

  /**
   * @param xml
   * @throws Throwable
   */
  public void toXml(final XmlEmit xml) throws Throwable {
    xml.openTag(AppleServerTags.updated);
    xml.property(WebdavTags.href, getHref());
    if (getChangedBy() != null) {
      getChangedBy().toXml(xml);
    }

    if (getContent()) {
      xml.emptyTag(AppleServerTags.content);
    }

    if (getProp() != null) {
      getProp().toXml(xml);
    }

    for (CalendarChangesType cc: getCalendarChanges()) {
      cc.toXml(xml);
    }

    xml.closeTag(AppleServerTags.updated);
  }

  /** Add our stuff to the StringBuffer
   *
   * @param ts
   */
  protected void toStringSegment(final ToString ts) {
    ts.append("href", getHref());
    if (getChangedBy() != null) {
      getChangedBy().toStringSegment(ts);
    }
    if (getContent()) {
      ts.append("content", true);
    }

    if (getProp() != null) {
      getProp().toStringSegment(ts);
    }

    for (CalendarChangesType cc: getCalendarChanges()) {
      cc.toStringSegment(ts);
    }
  }

  @Override
  public String toString() {
    ToString ts = new ToString(this);

    toStringSegment(ts);

    return ts.toString();
  }
}
