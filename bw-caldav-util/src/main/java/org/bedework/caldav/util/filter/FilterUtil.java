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

import ietf.params.xml.ns.caldav.PropFilterType;
import ietf.params.xml.ns.caldav.TextMatchType;
import ietf.params.xml.ns.caldav.UTCTimeRangeType;
import net.fortuna.ical4j.model.Component;
import net.fortuna.ical4j.model.Property;
import net.fortuna.ical4j.model.PropertyList;
import net.fortuna.ical4j.model.property.DateProperty;

/**
 * Help for filtering
 *
 * @author Mike Douglass
 * @version 2.0
 */
public class FilterUtil {

  /** Return true if the given component matches the property filter
   * <br/>
   * NOTE *********** Not handling params yet
   *
   * @param pf - property filter
   * @param c component
   * @return boolean true if the given component matches the property filter
   */
  public static boolean filter(final PropFilterType pf,
                               final Component c) {
    final PropertyList<Property> pl = c.getProperties();

    if (pl == null) {
      return false;
    }

    final Property prop = pl.getProperty(pf.getName());

    if (prop == null) {
      return pf.getIsNotDefined() != null;
    }

    final TextMatchType match = pf.getTextMatch();
    if (match != null) {
      return matches(match, prop.getValue());
    }

    final UTCTimeRangeType tr = pf.getTimeRange();
    if (tr == null) {
      // invalid state?
      return true;
    }

    return matches(tr, prop);
  }

  /**
   * @param tm text match type
   * @param candidate String
   * @return boolean true if matches
   */
  public static boolean matches(final TextMatchType tm,
                                final String candidate) {
    if (candidate == null) {
      return false;
    }

    final boolean isThere;

    final boolean upperMatch = tm.getCollation().equals("i;ascii-casemap");

    if (!upperMatch) {
      isThere = candidate.contains(tm.getValue());
    } else {
      isThere = candidate.toUpperCase().contains(tm.getValue());
    }

    if (tm.getNegateCondition().equals("yes")) {
      return !isThere;
    }

    return isThere;
  }

  /** Test if the given property falls in the timerange
   *
   * @param tr UTC timerange
   * @param candidate property
   * @return boolean true if in range
   */
  public static boolean matches(final UTCTimeRangeType tr,
                                final Property candidate) {
    if (!(candidate instanceof DateProperty)) {
      return false;
    }

    // XXX later
    return true;
  }
}
