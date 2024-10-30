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
package org.bedework.caldav.util;

import org.bedework.util.logging.BwLogger;
import org.bedework.util.logging.Logged;
import org.bedework.util.misc.ToString;

import net.fortuna.ical4j.model.DateTime;
import net.fortuna.ical4j.model.Dur;
import net.fortuna.ical4j.model.Property;
import net.fortuna.ical4j.model.property.DateProperty;

/** Express the CalDAV time-range element.
 * @author douglm
 *
 */
public class TimeRange implements Logged {
  /** UTC */
  private final DateTime start;
  /** UTC */
  private final DateTime end;

  /** start - 1 day */
  private DateTime startExpanded;
  /** end + 1 day */
  private DateTime endExpanded;

  private String tzid;

  private static final Dur oneDayForward = new Dur(1, 0, 0, 0);
  private static final Dur oneDayBack = new Dur(-1, 0, 0, 0);

  /**
   * @param start of range
   * @param end of range
   */
  public TimeRange(final DateTime start, final DateTime end) {
    this.start = start;
    this.end = end;

    if (start != null) {
      startExpanded = inc(start, oneDayBack);
    }

    if (end != null) {
      endExpanded = inc(end, oneDayForward);
    }
  }

  private DateTime inc(final DateTime dt, final Dur dur) {
    final java.util.Date jdt = dur.getTime(dt);

    return new DateTime(jdt);
  }

  /**
   * @return DateTime UTC start
   */
  public DateTime getStart() {
    return start;
  }

  /**
   * @return DateTime UTC end
   */
  public DateTime getEnd() {
    return end;
  }

  /**
   * @return DateTime UTC start - 1 day
   */
  public DateTime getStartExpanded() {
    return startExpanded;
  }

  /**
   * @return DateTime UTC end + 1 day
   */
  public DateTime getEndExpanded() {
    return endExpanded;
  }

  /**
   * @param val - possibly null tzid used to resolve floating times.
   */
  public void setTzid(final String val) {
    tzid = val;
  }

  /**
   * @return possibly null tzid used to resolve floating times
   */
  public String getTzid() {
    return tzid;
  }

  /** Test if the given property falls in the timerange
   *
   * @param candidate for test
   * @return boolean true if in range
   */
  public boolean matches(final Property candidate) {
    if (!(candidate instanceof DateProperty)) {
      return false;
    }

    // XXX later
    return true;
  }

  protected void toStringSegment(final ToString ts) {
    ts.append("start", start);
    ts.append("end", end);
  }

  public String toString() {
    final ToString ts = new ToString(this);

    toStringSegment(ts);

    return ts.toString();
  }

  /* =============================================================
   *                   Logged methods
   * ============================================================= */

  private final BwLogger logger = new BwLogger();

  @Override
  public BwLogger getLogger() {
    if ((logger.getLoggedClass() == null) && (logger.getLoggedName() == null)) {
      logger.setLoggedClass(getClass());
    }

    return logger;
  }
}
