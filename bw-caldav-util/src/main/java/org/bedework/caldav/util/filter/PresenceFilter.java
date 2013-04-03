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
package org.bedework.caldav.util.filter;

import edu.rpi.cmt.calendar.PropertyIndex.PropertyInfoIndex;

/** A filter that filters for property presence.
 *
 * The name should be unique at least for a set of filters and unique for a
 * given owner if persisted.
 *
 * @author Mike Douglass douglm
 */
public class PresenceFilter extends PropertyFilter {
  private boolean testPresent;

  /** Match on any of the entities.
   *
   * @param name - null one will be created
   * @param propertyIndex
   * @param testPresent   false for not present
   */
  public PresenceFilter(String name, PropertyInfoIndex propertyIndex,
                          boolean testPresent) {
    super(name, propertyIndex);
    this.testPresent = testPresent;
  }

  /** Set a test for present
   */
  public void setTestPresent() {
    testPresent = true;
  }

  /** See if we're testing for present
   *
   *  @return boolean true if test for present
   */
  public boolean getTestPresent() {
    return testPresent;
  }

  /* ====================================================================
   *                   Object methods
   * ==================================================================== */

  public String toString() {
    StringBuilder sb = new StringBuilder("PresenceFilter{");

    super.toStringSegment(sb);
    if (getTestPresent()) {
      sb.append("\nproperty not null");
    } else {
      sb.append("\nproperty null");
    }

    sb.append("}");

    return sb.toString();
  }
}
