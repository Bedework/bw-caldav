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

import org.bedework.util.calendar.IcalDefs;
import org.bedework.util.calendar.PropertyIndex.PropertyInfoIndex;
import org.bedework.webdav.servlet.shared.WebdavException;

/** A filter that selects entities of a given type.
 *
 * @author Mike Douglass
 * @version 2.0
 */
public class EntityTypeFilter extends ObjectFilter<Integer> {
  /** Match on the entity type.
   *
   * @param name - null one will be created
   */
  public EntityTypeFilter(final String name) {
    super(name, PropertyInfoIndex.ENTITY_TYPE);
  }

  /** Use ical names
   *
   * @param name
   * @param val - string val e.g. "VEVENT"
   * @param not true to test for inequality
   * @return a filter for events.
   */
  public static EntityTypeFilter makeIcalEntityTypeFilter(final String name,
                                                          final String val,
                                                          final boolean not)
          throws WebdavException {
    return makeEntityTypeFilter(name, val, not, IcalDefs.entityTypeIcalNames);
  }

  /** Use bedework names
   *
   * @param name
   * @param val - string val e.g. "event"
   * @param not true to test for inequality
   * @return a filter for events.
   */
  public static EntityTypeFilter makeEntityTypeFilter(final String name,
                                                      final String val,
                                                      final boolean not)
          throws WebdavException {
    return makeEntityTypeFilter(name, val, not, IcalDefs.entityTypeNames);
  }

  /**
   * @param name
   * @param val - string val e.g. "event"
   * @param not true to test for inequality
   * @return a filter for events.
   */
  public static EntityTypeFilter makeEntityTypeFilter(final String name,
                                                      final String val,
                                                      final boolean not,
                                                      final String[] names)
          throws WebdavException {
    int type = -1;

    for (int i = 0; i < names.length; i++) {
      if (names[i].equalsIgnoreCase(val)) {
        type = i;
        break;
      }
    }

    if (type < 0) {
      throw new WebdavException("Unknown entity type" + val);
    }

    EntityTypeFilter f = new EntityTypeFilter(name);
    f.setEntity(type);
    f.setNot(not);

    return f;
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder("(");

    sb.append(getPropertyIndex());
    stringOper(sb);
    sb.append(IcalDefs.entityTypeNames[getEntity()]);

    sb.append(")");

    return sb.toString();
  }
}
