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

import org.bedework.base.ToString;
import org.bedework.util.xml.XmlEmit;
import org.bedework.util.xml.tagdefs.AppleServerTags;

import java.util.ArrayList;
import java.util.List;

/**
       <!ELEMENT changes changed-property*>
       <!-- Detailed changes in the iCalendar data -->
 *
 * @author Mike Douglass douglm
 */
public class ChangesType {
  private List<ChangedPropertyType> changedProperty;

  /**
   * @return the changedProperty list
   */
  public List<ChangedPropertyType> getChangedProperty() {
    if (changedProperty == null) {
      changedProperty = new ArrayList<>();
    }

    return changedProperty;
  }

  /* ==============================================================
   *                   Convenience methods
   * ============================================================== */

  /**
   * @param xml emitter
   */
  public void toXml(final XmlEmit xml) {
    xml.openTag(AppleServerTags.changes);
    for (final ChangedPropertyType cp: getChangedProperty()) {
      cp.toXml(xml);
    }
    xml.closeTag(AppleServerTags.changes);
  }

  /** Add our stuff to the StringBuffer
   *
   * @param ts emitter
   */
  protected void toStringSegment(final ToString ts) {
    for (final ChangedPropertyType cp: getChangedProperty()) {
      cp.toStringSegment(ts);
    }
  }

  @Override
  public String toString() {
    final ToString ts = new ToString(this);

    toStringSegment(ts);

    return ts.toString();
  }
}
