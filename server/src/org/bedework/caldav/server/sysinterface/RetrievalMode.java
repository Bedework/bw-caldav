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
