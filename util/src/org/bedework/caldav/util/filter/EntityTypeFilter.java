/* **********************************************************************
    Copyright 2007 Rensselaer Polytechnic Institute. All worldwide rights reserved.

    Redistribution and use of this distribution in source and binary forms,
    with or without modification, are permitted provided that:
       The above copyright notice and this permission notice appear in all
        copies and supporting documentation;

        The name, identifiers, and trademarks of Rensselaer Polytechnic
        Institute are not used in advertising or publicity without the
        express prior written permission of Rensselaer Polytechnic Institute;

    DISCLAIMER: The software is distributed" AS IS" without any express or
    implied warranty, including but not limited to, any implied warranties
    of merchantability or fitness for a particular purpose or any warrant)'
    of non-infringement of any current or pending patent rights. The authors
    of the software make no representations about the suitability of this
    software for any particular purpose. The entire risk as to the quality
    and performance of the software is with the user. Should the software
    prove defective, the user assumes the cost of all necessary servicing,
    repair or correction. In particular, neither Rensselaer Polytechnic
    Institute, nor the authors of the software are liable for any indirect,
    special, consequential, or incidental damages related to the software,
    to the maximum extent the law permits.
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
