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

import org.bedework.util.calendar.PropertyIndex.PropertyInfoIndex;
import org.bedework.util.misc.ToString;

import java.util.List;

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
   * @param propertyIndex identifies property
   * @param testPresent   false for not present
   */
  public PresenceFilter(final String name,
                        final PropertyInfoIndex propertyIndex,
                        final boolean testPresent) {
    super(name, propertyIndex);
    this.testPresent = testPresent;
  }

  /** Match on any of the entities.
   *
   * @param name - null one will be created
   * @param propertyIndexes identifies dot separated refs to property
   * @param testPresent   false for not present
   */
  public PresenceFilter(final String name,
                        final List<PropertyInfoIndex> propertyIndexes,
                        final boolean testPresent) {
    super(name, propertyIndexes);
    this.testPresent = testPresent;
  }

  /** Match on any of the entities.
   *
   * @param name - null one will be created
   * @param propertyIndexes identifies dot separated refs to property
   * @param testPresent   false for not present
   * @param intKey non-null if property is indexed by the key
   * @param strKey non-null if ditto
   */
  public PresenceFilter(final String name,
                        final List<PropertyInfoIndex> propertyIndexes,
                        final boolean testPresent,
                        final Integer intKey,
                        final String strKey) {
    super(name, propertyIndexes, intKey, strKey);
    this.testPresent = testPresent;
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
    final ToString ts = new ToString(this);

    super.toStringSegment(ts);
    
    ts.append("testPresent", getTestPresent());

    return ts.toString();
  }
}
