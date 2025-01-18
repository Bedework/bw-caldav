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

/**
       <!ELEMENT child-created CDATA>
       <!ELEMENT child-updated CDATA>
       <!ELEMENT child-deleted CDATA>
       <!-- Each of the three elements above MUST contain a positive,
            non-zero integer value indicate the total number of changes
            being reported for the collection. -->
 *
 * @author Mike Douglass douglm
 */
public class ChildCreatedType {
  private int count;

  /**
   * @param val the count
   */
  public void setCount(final int val) {
    count = val;
  }

  /**
   * @return the count
   */
  public int getCount() {
    return count;
  }

  /* ==============================================================
   *                   Convenience methods
   * ============================================================== */

  /**
   * @param xml emitter
   */
  public void toXml(final XmlEmit xml) {
    xml.property(AppleServerTags.childCreated, String.valueOf(getCount()));
  }

  /** Add our stuff to the StringBuffer
   *
   * @param ts for output
   */
  protected void toStringSegment(final ToString ts) {
    ts.append("childCreated", getCount());
  }

  @Override
  public String toString() {
    final ToString ts = new ToString(this);

    toStringSegment(ts);

    return ts.toString();
  }
}
