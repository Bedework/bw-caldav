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

import java.util.ArrayList;
import java.util.List;

/**
     <!ELEMENT updated (DAV:href, changed-by?, content?,
                        DAV:prop?, calendar-changes*)>
 *
 * @author Mike Douglass douglm
 */
public class UpdatedType extends BaseEntityChangeType {
  private boolean content;
  private PropType prop;
  private List<CalendarChangesType> calendarChanges;

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
      calendarChanges = new ArrayList<>();
    }

    return calendarChanges;
  }

  /* ====================================================================
   *                   Convenience methods
   * ==================================================================== */

  public UpdatedType copyForAlias(final String collectionHref) {
    final UpdatedType copy = new UpdatedType();

    copyForAlias(copy, collectionHref);

    copy.content = content;
    copy.prop = prop;
    
    if (calendarChanges != null) {
      copy.calendarChanges = new ArrayList<>(calendarChanges);
    }

    return copy;
  }

  /**
   * @param xml builder
   * @throws Throwable
   */
  public void toXml(final XmlEmit xml) throws Throwable {
    xml.openTag(AppleServerTags.updated);
    toXmlSegment(xml);

    if (getContent()) {
      xml.emptyTag(AppleServerTags.content);
    }

    if (getProp() != null) {
      getProp().toXml(xml);
    }

    for (final CalendarChangesType cc: getCalendarChanges()) {
      cc.toXml(xml);
    }

    xml.closeTag(AppleServerTags.updated);
  }

  /** Add our stuff to the StringBuffer
   *
   * @param ts builder
   */
  protected void toStringSegment(final ToString ts) {
    super.toStringSegment(ts);
    if (getContent()) {
      ts.append("content", true);
    }

    if (getProp() != null) {
      getProp().toStringSegment(ts);
    }

    for (final CalendarChangesType cc: getCalendarChanges()) {
      cc.toStringSegment(ts);
    }
  }
}
