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

import org.bedework.caldav.util.filter.ObjectFilter;

import edu.rpi.cmt.calendar.IcalDefs;
import edu.rpi.cmt.calendar.PropertyIndex.PropertyInfoIndex;

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
  public EntityTypeFilter(String name) {
    super(name, PropertyInfoIndex.ENTITY_TYPE);
  }

  /**
   * @param name
   * @param not true to test for inequality
   * @return a filter for events.
   */
  public static EntityTypeFilter eventFilter(String name, boolean not) {
    EntityTypeFilter f = new EntityTypeFilter(name);
    f.setEntity(IcalDefs.entityTypeEvent);
    f.setNot(not);

    return f;
  }

  /**
   * @param name
   * @param not true to test for inequality
   * @return a filter for todos.
   */
  public static EntityTypeFilter todoFilter(String name, boolean not) {
    EntityTypeFilter f = new EntityTypeFilter(name);
    f.setEntity(IcalDefs.entityTypeTodo);
    f.setNot(not);

    return f;
  }

  /**
   * @param name
   * @param not true to test for inequality
   * @return a filter for todos.
   */
  public static EntityTypeFilter freebusyFilter(String name, boolean not) {
    EntityTypeFilter f = new EntityTypeFilter(name);
    f.setEntity(IcalDefs.entityTypeFreeAndBusy);
    f.setNot(not);

    return f;
  }

  /**
   * @param name
   * @param not true to test for inequality
   * @return a filter for journals.
   */
  public static EntityTypeFilter journalFilter(String name, boolean not) {
    EntityTypeFilter f = new EntityTypeFilter(name);
    f.setEntity(IcalDefs.entityTypeJournal);
    f.setNot(not);

    return f;
  }

  /**
   * @param name
   * @param not true to test for inequality
   * @return a filter for events.
   */
  public static EntityTypeFilter alarmFilter(String name, boolean not) {
    EntityTypeFilter f = new EntityTypeFilter(name);
    f.setEntity(IcalDefs.entityTypeAlarm);
    f.setNot(not);

    return f;
  }

  public String toString() {
    StringBuilder sb = new StringBuilder("(");

    sb.append(getPropertyIndex());
    stringOper(sb);
    sb.append(IcalDefs.entityTypeNames[getEntity()]);

    sb.append(")");

    return sb.toString();
  }
}
