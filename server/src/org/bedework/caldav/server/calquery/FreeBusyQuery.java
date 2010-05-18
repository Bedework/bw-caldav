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
package org.bedework.caldav.server.calquery;

import org.bedework.caldav.server.CalDAVCollection;
import org.bedework.caldav.server.sysinterface.SysIntf;
import org.bedework.caldav.util.ParseUtil;
import org.bedework.caldav.util.TimeRange;

import edu.rpi.cct.webdav.servlet.shared.WebdavBadRequest;
import edu.rpi.cct.webdav.servlet.shared.WebdavException;
import edu.rpi.sss.util.xml.XmlUtil;
import edu.rpi.sss.util.xml.tagdefs.CaldavTags;

import net.fortuna.ical4j.model.Calendar;

import org.apache.log4j.Logger;
import org.w3c.dom.Node;

/**
 * @author Mike Douglass douglm @ rpi.edu
 */
public class FreeBusyQuery {
  private boolean debug;

  protected transient Logger log;

  private TimeRange timeRange;

  /** Constructor
   *
   * @param debug
   */
  public FreeBusyQuery(final boolean debug) {
    this.debug = debug;
  }

  /** The given node is is the free-busy-query time-range element
   * Should have exactly one time-range element.
   *
   * @param nd
   * @throws WebdavException
   */
  public void parse(final Node nd) throws WebdavException {
    try {
      if (timeRange != null) {
        throw new WebdavBadRequest();
      }

      if (!XmlUtil.nodeMatches(nd, CaldavTags.timeRange)) {
        throw new WebdavBadRequest();
      }

      timeRange = ParseUtil.parseTimeRange(nd, false);

      if (debug) {
        trace("Parsed time range " + timeRange);
      }
    } catch (WebdavException wde) {
      throw wde;
    } catch (Throwable t) {
      throw new WebdavBadRequest();
    }
  }

  /**
   * @param sysi
   * @param col
   * @param depth
   * @return BwEvent
   * @throws WebdavException
   */
  public Calendar getFreeBusy(final SysIntf sysi, final CalDAVCollection col,
                             final int depth) throws WebdavException {
    try {
      return sysi.getFreeBusy(col, depth, timeRange);
    } catch (WebdavException wde) {
      throw wde;
    } catch (Throwable t) {
      throw new WebdavException(t);
    }
  }

  /** Debug method
   *
   */
  public void dump() {
    trace("<free-busy-query>");

    timeRange.dump(getLogger(), "  ");

    trace("</free-busy-query>");
  }

  /*
  private Element[] getChildren(Node nd) throws WebdavException {
    try {
      return XmlUtil.getElementsArray(nd);
    } catch (Throwable t) {
      if (debug) {
        getLogger().error("<filter>: parse exception: ", t);
      }

      throw new WebdavBadRequest();
    }
  }
  */

  /** ===================================================================
   *                   Logging methods
   *  =================================================================== */

  /**
   * @return Logger
   */
  protected Logger getLogger() {
    if (log == null) {
      log = Logger.getLogger(this.getClass());
    }

    return log;
  }

  protected void debugMsg(final String msg) {
    getLogger().debug(msg);
  }

  protected void error(final Throwable t) {
    getLogger().error(this, t);
  }

  protected void logIt(final String msg) {
    getLogger().info(msg);
  }

  protected void trace(final String msg) {
    getLogger().debug(msg);
  }
}

