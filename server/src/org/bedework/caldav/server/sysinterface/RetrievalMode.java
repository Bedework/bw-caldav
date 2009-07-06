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
package org.bedework.caldav.server.sysinterface;

import net.fortuna.ical4j.model.DateTime;

import java.io.Serializable;

/**
 * @author douglm
 *
 */
public class RetrievalMode implements Serializable {
  /**
   * Values which define how to retrieve recurring events. We have the
   * following choices
   *   No limits
   *   Limit Recurrence set - with range
   *   Expand recurrences
   */

  /** */
  public boolean expanded;

  /** */
  public boolean limitRecurrenceSet;

  /** Limit expansion and recurrences.
   */
  public DateTime start;

  /** Limit expansion and recurrences.
   */
  public DateTime end;

  /** Factory
   *
   * @param start
   * @param end
   * @return RetrievalMode
   */
  public static RetrievalMode getExpanded(DateTime start, DateTime end) {
    RetrievalMode rm = new RetrievalMode();

    rm.expanded = true;
    rm.start = start;
    rm.end = end;

    return rm;
  }

  /** Factory
   *
   * @param start
   * @param end
   * @return RetrievalMode
   */
  public static RetrievalMode getLimited(DateTime start, DateTime end) {
    RetrievalMode rm = new RetrievalMode();

    rm.limitRecurrenceSet = true;
    rm.start = start;
    rm.end = end;

    return rm;
  }

  public String toString() {
    StringBuilder sb = new StringBuilder("RetrievalMode{");

    if (expanded) {
      sb.append("expanded, ");
    } else {
      sb.append("limited, ");
    }

    sb.append(", start=");
    sb.append(start);

    sb.append(", end=");
    sb.append(end);

    sb.append("}");
    return sb.toString();
  }
}
