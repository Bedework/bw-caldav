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

import edu.rpi.sss.util.ToString;
import edu.rpi.sss.util.xml.XmlEmit;
import edu.rpi.sss.util.xml.tagdefs.AppleServerTags;

import java.util.ArrayList;
import java.util.List;

/**
     <!ELEMENT recurrence
         ((master | recurrenceid), added?, removed?, changes?)>
     <!-- Which instances were affected by the change,
          and details on the per-instance changes -->

       <!ELEMENT master EMPTY>
       <!-- The "master" instance was affected -->

       <!ELEMENT recurrenceid CDATA>
       <!-- RECURRENCE-ID value in iCalendar form (in UTC if a
            non-floating DATE-TIME value) for the affected instance -->

       <!ELEMENT added EMPTY>
       <!-- The component was added -->

       <!ELEMENT removed EMPTY>
       <!-- The component was removed -->
 *
 * @author Mike Douglass douglm
 */
public class RecurrenceType {
  private String recurrenceid;

  private boolean added;

  private boolean removed;

  private List<ChangesType> changes;

  /**
   * @param val the recurrenceid
   */
  public void setRecurrenceid(final String val) {
    recurrenceid = val;
  }

  /**
   * @return the recurrenceid
   */
  public String getRecurrenceid() {
    return recurrenceid;
  }

  /**
   * @param val the added falg
   */
  public void setAdded(final boolean val) {
    added = val;
  }

  /**
   * @return the added falg
   */
  public boolean getAdded() {
    return added;
  }

  /**
   * @param val the removed flag
   */
  public void setRemoved(final boolean val) {
    removed = val;
  }

  /**
   * @return the removed flag
   */
  public boolean getRemoved() {
    return removed;
  }

  /**
   * @return the ChangesType list
   */
  public List<ChangesType> getChanges() {
    if (changes == null) {
      changes = new ArrayList<ChangesType>();
    }

    return changes;
  }

  /* ====================================================================
   *                   Convenience methods
   * ==================================================================== */

  /**
   * @param xml
   * @throws Throwable
   */
  public void toXml(final XmlEmit xml) throws Throwable {
    xml.openTag(AppleServerTags.recurrence);

    if (getRecurrenceid() == null) {
      xml.emptyTag(AppleServerTags.master);
    } else {
      xml.property(AppleServerTags.recurrenceid, getRecurrenceid());
    }

    if (getAdded()) {
      xml.emptyTag(AppleServerTags.added);
    } else if (getRemoved()) {
      xml.emptyTag(AppleServerTags.removed);
    } else {
      for (ChangesType c: getChanges()) {
        c.toXml(xml);
      }
    }

    xml.closeTag(AppleServerTags.recurrence);
  }

  /** Add our stuff to the StringBuffer
   *
   * @param ts
   */
  protected void toStringSegment(final ToString ts) {
    if (getRecurrenceid() == null) {
      ts.append("master");
    } else {
      ts.append("recurrenceid", getRecurrenceid());
    }

    if (getAdded()) {
      ts.append("added");
    } else if (getRemoved()) {
      ts.append("removed");
    } else {
      for (ChangesType c: getChanges()) {
        c.toStringSegment(ts);
      }
    }
  }

  @Override
  public String toString() {
    ToString ts = new ToString(this);

    toStringSegment(ts);

    return ts.toString();
  }
}
