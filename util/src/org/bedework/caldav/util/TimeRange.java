/* **********************************************************************
    Copyright 2009 Rensselaer Polytechnic Institute. All worldwide rights reserved.

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
package org.bedework.caldav.util;

import org.apache.log4j.Logger;

import net.fortuna.ical4j.model.DateTime;
import net.fortuna.ical4j.model.Property;
import net.fortuna.ical4j.model.property.DateProperty;

/** Express the CalDAV time-range element.
 * @author douglm
 *
 */
public class TimeRange {
  /** */
  public DateTime start;
  /** */
  public DateTime end;

  /**
   * @param start
   * @param end
   */
  public TimeRange(DateTime start, DateTime end) {
    this.start = start;
    this.end = end;
  }

  /**
   * @return DateTime start
   */
  public DateTime getStart() {
    return start;
  }

  /**
   * @return DateTime end
   */
  public DateTime getEnd() {
    return end;
  }

  /** Test if the given property falls in the timerange
   *
   * @param candidate
   * @return boolean true if in range
   */
  public boolean matches(Property candidate) {
    if (!(candidate instanceof DateProperty)) {
      return false;
    }

    // XXX later
    return true;
  }

  /** Debug
   *
   * @param log
   * @param indent
   */
  public void dump(Logger log, String indent) {
    log.debug(indent + toString());
  }

  protected void toStringSegment(StringBuilder sb) {
    if (start != null) {
      sb.append("start=");
      sb.append(start);
    }

    if (end != null) {
      if (start != null) {
        sb.append(" ");
      }
      sb.append("end=");
      sb.append(end);
    }
  }

  public String toString() {
    StringBuilder sb = new StringBuilder();

    sb.append("<time-range ");
    toStringSegment(sb);
    sb.append("/>");

    return sb.toString();
  }
}
