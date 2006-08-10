/*
 Copyright (c) 2000-2005 University of Washington.  All rights reserved.

 Redistribution and use of this distribution in source and binary forms,
 with or without modification, are permitted provided that:

   The above copyright notice and this permission notice appear in
   all copies and supporting documentation;

   The name, identifiers, and trademarks of the University of Washington
   are not used in advertising or publicity without the express prior
   written permission of the University of Washington;

   Recipients acknowledge that this distribution is made available as a
   research courtesy, "as is", potentially with defects, without
   any obligation on the part of the University of Washington to
   provide support, services, or repair;

   THE UNIVERSITY OF WASHINGTON DISCLAIMS ALL WARRANTIES, EXPRESS OR
   IMPLIED, WITH REGARD TO THIS SOFTWARE, INCLUDING WITHOUT LIMITATION
   ALL IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A
   PARTICULAR PURPOSE, AND IN NO EVENT SHALL THE UNIVERSITY OF
   WASHINGTON BE LIABLE FOR ANY SPECIAL, INDIRECT OR CONSEQUENTIAL
   DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR
   PROFITS, WHETHER IN AN ACTION OF CONTRACT, TORT (INCLUDING
   NEGLIGENCE) OR STRICT LIABILITY, ARISING OUT OF OR IN CONNECTION WITH
   THE USE OR PERFORMANCE OF THIS SOFTWARE.
 */
/* **********************************************************************
    Copyright 2005 Rensselaer Polytechnic Institute. All worldwide rights reserved.

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

package org.bedework.caldav.server;

import org.bedework.calfacade.BwDateTime;

import net.fortuna.ical4j.model.property.DateProperty;
import net.fortuna.ical4j.model.Property;

import org.apache.log4j.Logger;

/** Timerange element for caldav filters. Either start or end may be absent but
 * not both.
 *
 *   @author Mike Douglass   douglm@rpi.edu
 */
public class TimeRange {
  private BwDateTime start;
  private BwDateTime end;

  /** Constructor
   */
  public TimeRange() {
  }

  /** Constructor
   *
   * @param start
   * @param end
   */
  public TimeRange(BwDateTime start, BwDateTime end) {
    this.start = start;
    this.end = end;
  }

  /**
   * @param val BwDateTime start
   */
  public void setStart(BwDateTime val) {
    start = val;
  }

  /**
   * @return BwDateTime start
   */
  public BwDateTime getStart() {
    return start;
  }

  /**
   * @param val BwDateTime end
   */
  public void setEnd(BwDateTime val) {
    end = val;
  }

  /**
   * @return BwDateTime end
   */
  public BwDateTime getEnd() {
    return end;
  }

  /** merge that into this
   *
   * @param that TimeRange to merge
   */
  public void merge(TimeRange that) {
    if (getStart().before(that.getStart())) {
      setStart(that.getStart());
    }

    if (getEnd().after(that.getEnd())) {
      setEnd(that.getEnd());
    }
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

  protected void toStringSegment(StringBuffer sb) {
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
    StringBuffer sb = new StringBuffer();

    sb.append("<time-range ");
    toStringSegment(sb);
    sb.append("/>");

    return sb.toString();
  }
}

