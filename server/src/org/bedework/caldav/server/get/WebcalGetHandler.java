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
package org.bedework.caldav.server.get;

import org.bedework.caldav.server.CalDAVEvent;
import org.bedework.caldav.server.CaldavBWIntf;
import org.bedework.caldav.server.CaldavComponentNode;
import org.bedework.caldav.server.PostMethod.RequestPars;
import org.bedework.caldav.server.sysinterface.SystemProperties;
import org.bedework.caldav.util.ParseUtil;
import org.bedework.caldav.util.TimeRange;

import edu.rpi.cct.webdav.servlet.shared.WebdavException;
import edu.rpi.cct.webdav.servlet.shared.WebdavNsIntf;
import edu.rpi.cct.webdav.servlet.shared.WebdavNsNode;
import edu.rpi.cmt.calendar.ScheduleMethods;

import java.util.ArrayList;
import java.util.Collection;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/** Handle web calendar GET requests.
 *
 * @author Mike Douglass
 */
public class WebcalGetHandler extends GetHandler {
  /**
   * @param intf
   */
  public WebcalGetHandler(final CaldavBWIntf intf) {
    super(intf);
  }

  /* (non-Javadoc)
   * @see org.bedework.caldav.server.get.GetHandler#process(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse, org.bedework.caldav.server.PostMethod.RequestPars)
   */
  @Override
  public void process(final HttpServletRequest req,
                      final HttpServletResponse resp,
                      final RequestPars pars) throws WebdavException {
    try {
      SystemProperties sysp = getSysi().getSystemProperties();

      TimeRange tr = ParseUtil.getPeriod(req.getParameter("start"),
                                         req.getParameter("end"),
                                         java.util.Calendar.DATE,
                                         sysp.getDefaultWebCalPeriod(),
                                         java.util.Calendar.DATE,
                                         sysp.getMaxWebCalPeriod());

      if (tr == null) {
        resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Date/times");
        return;
      }

      String calPath = req.getParameter("calPath");
      if (calPath == null) {
        resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "No calPath");
        return;
      }

      calPath = WebdavNsIntf.fixPath(calPath);

      WebdavNsNode node = getNode(calPath,
                                  WebdavNsIntf.existanceMust,
                                  WebdavNsIntf.nodeTypeUnknown);

      if ((node == null) || !node.getExists()) {
        resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
        return;
      }

      if (!node.isCollection()) {
        resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Not collection");
        return;
      }

      Collection<CalDAVEvent> evs = new ArrayList<CalDAVEvent>();

      for (WebdavNsNode child: getChildren(node)) {
        if (child instanceof CaldavComponentNode) {
          evs.add(((CaldavComponentNode)child).getEvent());
        }
      }


      resp.setHeader("Content-Disposition",
                     "Attachment; Filename=\"" +
                     node.getDisplayname() + ".ics\"");
      resp.setContentType("text/calendar; charset=UTF-8");

      getSysi().writeCalendar(evs, ScheduleMethods.methodTypePublish,
                              resp.getWriter());
    } catch (WebdavException wde) {
      throw wde;
    } catch (Throwable t) {
      throw new WebdavException(t);
    }
  }
}
