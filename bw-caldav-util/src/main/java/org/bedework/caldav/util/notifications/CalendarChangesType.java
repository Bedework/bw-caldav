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
   <!ELEMENT calendar-changes (recurrence+) >
 *
 * @author Mike Douglass douglm
 */
public class CalendarChangesType {
  private List<RecurrenceType> recurrence;

  /**
   * @return the recurrence list
   */
  public List<RecurrenceType> getRecurrence() {
    if (recurrence == null) {
      recurrence = new ArrayList<RecurrenceType>();
    }

    return recurrence;
  }

  /* ====================================================================
   *                   Convenience methods
   * ==================================================================== */

  /**
   * @param xml
   * @throws Throwable
   */
  public void toXml(final XmlEmit xml) throws Throwable {
    xml.openTag(AppleServerTags.calendarChanges);
    for (RecurrenceType r: getRecurrence()) {
      r.toXml(xml);
    }
    xml.closeTag(AppleServerTags.calendarChanges);
  }

  /** Add our stuff to the StringBuffer
   *
   * @param ts
   */
  protected void toStringSegment(final ToString ts) {
    for (RecurrenceType r: getRecurrence()) {
      r.toStringSegment(ts);
    }
  }

  @Override
  public String toString() {
    ToString ts = new ToString(this);

    toStringSegment(ts);

    return ts.toString();
  }
}
