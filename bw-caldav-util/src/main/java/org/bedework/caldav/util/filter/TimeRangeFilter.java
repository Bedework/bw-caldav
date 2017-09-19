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

import org.bedework.caldav.util.TimeRange;
import org.bedework.util.calendar.PropertyIndex.PropertyInfoIndex;

import java.util.List;

/** A filter that selects properties that have a date within a given timerange.
 * This does not include calendar entities which have a start and end time.
 *
 * @author Mike Douglass
 * @version 1.0
 */
public class TimeRangeFilter extends ObjectFilter<TimeRange> {
  /** Match a created date.
   *
   * @param name - null one will be created
   * @param propertyIndex of property
   * @param intKey non-null if property is indexed by the key
   * @param strKey non-null if ditto
   */
  public TimeRangeFilter(final String name,
                         final PropertyInfoIndex propertyIndex,
                         final Integer intKey,
                         final String strKey) {
    super(name, propertyIndex, intKey, strKey);
  }

  /** Match a created date.
   *
   * @param name - null one will be created
   * @param propertyIndexes of dot separated properties
   * @param intKey non-null if property is indexed by the key
   * @param strKey non-null if ditto
   */
  public TimeRangeFilter(final String name,
                         final List<PropertyInfoIndex> propertyIndexes,
                         final Integer intKey,
                         final String strKey) {
    super(name, propertyIndexes, intKey, strKey);
  }
}
