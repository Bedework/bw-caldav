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
package org.bedework.caldav.server.calquery;

import org.bedework.caldav.server.CalDAVCollection;
import org.bedework.caldav.server.CalDAVEvent;
import org.bedework.caldav.server.sysinterface.SysIntf;
import org.bedework.caldav.util.ParseUtil;
import org.bedework.caldav.util.TimeRange;
import org.bedework.util.xml.XmlUtil;
import org.bedework.util.xml.tagdefs.CaldavTags;
import org.bedework.webdav.servlet.shared.WebdavBadRequest;
import org.bedework.webdav.servlet.shared.WebdavException;

import org.apache.log4j.Logger;
import org.w3c.dom.Node;

/**
 * @author Mike Douglass douglm  rpi.edu
 */
public class FreeBusyQuery {
  private boolean debug;

  protected transient Logger log;

  private TimeRange timeRange;

  /** Constructor
   *
   */
  public FreeBusyQuery() {
    debug = getLogger().isDebugEnabled();
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
  public CalDAVEvent getFreeBusy(final SysIntf sysi, final CalDAVCollection col,
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

