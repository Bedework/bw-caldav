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

import edu.rpi.cct.webdav.servlet.shared.WebdavBadRequest;
import edu.rpi.cct.webdav.servlet.shared.WebdavException;
import edu.rpi.sss.util.DateTimeUtil;
import edu.rpi.sss.util.xml.tagdefs.CaldavTags;

import net.fortuna.ical4j.model.DateTime;

import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import ietf.params.xml.ns.caldav.UTCTimeRangeType;

import java.util.Calendar;

/** Some utilities for parsing caldav
 *
 * @author Mike Douglass douglm @ rpi.edu
 */
public class ParseUtil {
  /** The given node must be a time-range style element
   *  <!ELEMENT time-range EMPTY>
   *
   *  <!ATTLIST time-range start CDATA
   *                       end CDATA>
   *
   *  Start and end are date with UTC time
   *
   * e.g.        <C:time-range start="20040902T000000Z"
   *                           end="20040902T235959Z"/>
   *
   * @param nd
   * @param required - if true start and end MUST be present
   * @return TimeRange
   * @throws WebdavException
   */
  public static TimeRange parseTimeRange(final Node nd,
                                         final boolean required) throws WebdavException {
    DateTime start = null;
    DateTime end = null;

    NamedNodeMap nnm = nd.getAttributes();

    if (nnm == null) {
      // Infinite time-range?
      throw new WebdavBadRequest(CaldavTags.validFilter, "Infinite time range");
    }

    int attrCt = nnm.getLength();

    if ((attrCt == 0) || (required && (attrCt != 2))) {
      // Infinite time-range?
      throw new WebdavBadRequest(CaldavTags.validFilter, "Infinite time range");
    }

    try {
      Node nmAttr = nnm.getNamedItem("start");

      if (nmAttr != null) {
        attrCt--;
        String dt = nmAttr.getNodeValue();
        if (!checkUTC(dt)){
          throw new WebdavBadRequest(CaldavTags.validFilter, "Not UTC");
        }

        start = new DateTime(dt);
      } else if (required) {
        throw new WebdavBadRequest(CaldavTags.validFilter, "Missing start");
      }

      nmAttr = nnm.getNamedItem("end");

      if (nmAttr != null) {
        attrCt--;
        String dt = nmAttr.getNodeValue();
        if (!checkUTC(dt)){
          throw new WebdavBadRequest(CaldavTags.validFilter, "Not UTC");
        }

        end = new DateTime(dt);
      } else if (required) {
        throw new WebdavBadRequest(CaldavTags.validFilter, "Missing end");
      }
    } catch (WebdavException wde) {
      throw wde;
    } catch (Throwable t) {
      throw new WebdavBadRequest(CaldavTags.validFilter, "Invalid time-range");
    }

    if (attrCt != 0) {
      throw new WebdavBadRequest(CaldavTags.validFilter);
    }

    return new TimeRange(start, end);
  }

  /** The given node must be a time-range style element
   *  <!ELEMENT time-range EMPTY>
   *
   *  <!ATTLIST time-range start CDATA
   *                       end CDATA>
   *
   *  Start and end are date with UTC time
   *
   * e.g.        <C:time-range start="20040902T000000Z"
   *                           end="20040902T235959Z"/>
   *
   * @param val - an object to populate or null for a new object
   * @param nd
   * @param required - if true start and end MUST be present
   * @return TimeRange
   * @throws WebdavException
   */
  public static UTCTimeRangeType parseUTCTimeRange(final UTCTimeRangeType val,
                                                   final Node nd,
                                                final boolean required) throws WebdavException {
    String st = null;
    String et = null;

    NamedNodeMap nnm = nd.getAttributes();

    if (nnm == null) {
      // Infinite time-range?
      throw new WebdavBadRequest(CaldavTags.validFilter, "Infinite time range");
    }

    int attrCt = nnm.getLength();

    if ((attrCt == 0) || (required && (attrCt != 2))) {
      // Infinite time-range?
      throw new WebdavBadRequest(CaldavTags.validFilter, "Infinite time range");
    }

    try {
      Node nmAttr = nnm.getNamedItem("start");

      if (nmAttr != null) {
        attrCt--;
        st = nmAttr.getNodeValue();
        if (!checkUTC(st)){
          throw new WebdavBadRequest(CaldavTags.validFilter, "Not UTC");
        }
      } else if (required) {
        throw new WebdavBadRequest(CaldavTags.validFilter, "Missing start");
      }

      nmAttr = nnm.getNamedItem("end");

      if (nmAttr != null) {
        attrCt--;
        et = nmAttr.getNodeValue();
        if (!checkUTC(et)){
          throw new WebdavBadRequest(CaldavTags.validFilter, "Not UTC");
        }
      } else if (required) {
        throw new WebdavBadRequest(CaldavTags.validFilter, "Missing end");
      }

      if (attrCt != 0) {
        throw new WebdavBadRequest(CaldavTags.validFilter);
      }

      if (val == null) {
        UTCTimeRangeType utr = new UTCTimeRangeType();

        utr.setStart(st);
        utr.setEnd(et);

        return utr;
      }

      if (st != null) {
        val.setStart(st);
      }

      if (et != null) {
        val.setEnd(et);
      }

      return val;
    } catch (WebdavException wde) {
      throw wde;
    } catch (Throwable t) {
      throw new WebdavBadRequest(CaldavTags.validFilter, "Invalid time-range");
    }
  }

  /** Get a date/time range given by the rfc formatted parameters and limited to
   * the given max range
   *
   * @param start
   * @param end
   * @param defaultField
   * @param defaultVal
   * @param maxField
   * @param maxVal - 0 for no max
   * @return TimeRange or null for bad request
   * @throws WebdavException
   */
  public static TimeRange getPeriod(final String start, final String end,
                                    final int defaultField, final int defaultVal,
                                    final int maxField,
                                    final int maxVal) throws WebdavException {
    Calendar startCal = Calendar.getInstance();
    startCal.set(Calendar.HOUR_OF_DAY, 0);
    startCal.set(Calendar.MINUTE, 0);
    startCal.set(Calendar.SECOND, 0);

    Calendar endCal = Calendar.getInstance();
    endCal.set(Calendar.HOUR_OF_DAY, 0);
    endCal.set(Calendar.MINUTE, 0);
    endCal.set(Calendar.SECOND, 0);

    try {
      if (start != null) {
        startCal.setTime(DateTimeUtil.fromDate(start));
      }

      if (end == null) {
        endCal.add(defaultField, defaultVal);
      } else {
        endCal.setTime(DateTimeUtil.fromDate(end));
      }
    } catch (DateTimeUtil.BadDateException bde) {
      throw new WebdavBadRequest();
    }

    // Don't allow more than the max
    if (maxVal > 0) {
      Calendar check = Calendar.getInstance();
      check.setTime(startCal.getTime());
      check.add(maxField, maxVal);

      if (check.before(endCal)) {
        return null;
      }
    }

    return new TimeRange(new DateTime(startCal.getTime()),
                         new DateTime(endCal.getTime()));
  }

  private static boolean checkUTC(final String val) {
    if (val.length() != 16) {
      return false;
    }

    byte[] b = val.getBytes();

    if (b[8] != 'T') {
      return false;
    }

    if (b[15] != 'Z') {
      return false;
    }

    //Parser will fail anything else.

    return true;
  }
}
