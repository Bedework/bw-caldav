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
package org.bedework.caldav.server;

import org.bedework.calfacade.base.TimeRange;
import org.bedework.calfacade.BwDateTime;
import org.bedework.calfacade.timezones.CalTimezones;
import org.bedework.calfacade.util.DateTimeUtil;

import edu.rpi.cct.webdav.servlet.shared.WebdavBadRequest;
import edu.rpi.cct.webdav.servlet.shared.WebdavException;

import net.fortuna.ical4j.model.TimeZone;

import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

/** Some utilities for parsing caldav
 *
 * @author Mike Douglass douglm @ rpi.edu
 */
public class CalDavParseUtil {
  /** The given node must be a time-range element
   *  <!ELEMENT time-range EMPTY>
   *
   *  <!ATTLIST time-range start CDATA
   *                       end CDATA>
   *
   * e.g.        <C:time-range start="20040902T000000Z"
   *                           end="20040902T235959Z"/>
   *
   * @param nd
   * @param tz - timezone to use if specified
   * @param timezones
   * @return TimeRange
   * @throws WebdavException
   */
  public static TimeRange parseTimeRange(Node nd,
                                         TimeZone tz,
                                         CalTimezones timezones) throws WebdavException {
    BwDateTime start = null;
    BwDateTime end = null;

    NamedNodeMap nnm = nd.getAttributes();

    /* draft 5 has neither attribute required - the intent is that either
       may be absent */

    if ((nnm == null) || (nnm.getLength() == 0)) {
      // Infinite time-range?
      throw new WebdavBadRequest();
    }

    int attrCt = nnm.getLength();
    String tzid = null;
    if (tz != null) {
      tzid = tz.getID();
    }

    try {
      Node nmAttr = nnm.getNamedItem("start");

      if (nmAttr != null) {
        attrCt--;
        start = DateTimeUtil.getDateTime(nmAttr.getNodeValue(),
                                         false,
                                         tz == null,   // utc
                                         false,
                                         tzid,
                                         tz,
                                         null,   // tzowner
                                         timezones);
      }

      nmAttr = nnm.getNamedItem("end");

      if (nmAttr != null) {
        attrCt--;
        end = DateTimeUtil.getDateTime(nmAttr.getNodeValue(),
                                       false,
                                       tz == null,   // utc
                                       false,
                                       tzid,
                                       tz,
                                       null,   // tzowner
                                       timezones);
      }
    } catch (Throwable t) {
      throw new WebdavBadRequest();
    }

    if (attrCt != 0) {
      throw new WebdavBadRequest();
    }

    return new TimeRange(start, end);
  }
}
