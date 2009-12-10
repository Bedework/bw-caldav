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

import org.bedework.caldav.server.CaldavBWIntf;
import org.bedework.caldav.server.PostMethod.RequestPars;
import org.bedework.caldav.util.ParseUtil;
import org.bedework.caldav.util.TimeRange;

import edu.rpi.cct.webdav.servlet.shared.WebdavException;
import edu.rpi.cct.webdav.servlet.shared.WebdavForbidden;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/** Handle freebusy GET requests.
 *
 * @author Mike Douglass
 */
public class FreeBusyGetHandler extends GetHandler {
  /**
   * @param intf
   */
  public FreeBusyGetHandler(final CaldavBWIntf intf) {
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
      if (getAccount() != null) {
        pars.originator = getSysi().userToCaladdr(getAccount());
      }

      String cua = req.getParameter("cua");
      String user = null;

      if (cua == null) {
        user = req.getParameter("user");
        if (user == null) {
          if (getAccount() == null) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing user/cua");
            return;
          }

          user = getAccount();
        }
      }

      pars.setContentType("text/calendar; charset=UTF-8");

      TimeRange tr = ParseUtil.getPeriod(req.getParameter("start"),
                                         req.getParameter("end"),
                                         java.util.Calendar.DATE, 31,
                                         java.util.Calendar.DATE, 32);

      if (tr == null) {
        resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Date/times");
        return;
      }

      resp.setHeader("Content-Disposition",
                     "Attachment; Filename=\"freebusy.ics\"");
      resp.setContentType("text/calendar; charset=UTF-8");

      getSysi().getSpecialFreeBusy(cua, user, pars, tr, resp.getWriter());
    } catch (WebdavForbidden wdf) {
      resp.setStatus(HttpServletResponse.SC_FORBIDDEN);
    } catch (WebdavException wde) {
      throw wde;
    } catch (Throwable t) {
      throw new WebdavException(t);
    }
  }
}
