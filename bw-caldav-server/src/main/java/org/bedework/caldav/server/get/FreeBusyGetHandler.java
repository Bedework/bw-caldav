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
package org.bedework.caldav.server.get;

import org.bedework.caldav.server.CaldavBWIntf;
import org.bedework.caldav.server.RequestPars;
import org.bedework.caldav.server.sysinterface.CalDAVAuthProperties;
import org.bedework.caldav.util.ParseUtil;
import org.bedework.caldav.util.TimeRange;
import org.bedework.webdav.servlet.shared.WebdavException;
import org.bedework.webdav.servlet.shared.WebdavForbidden;

import java.util.Set;
import java.util.TreeSet;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/** Handle freebusy GET requests.
 *
 * @author Mike Douglass
 */
public class FreeBusyGetHandler extends GetHandler {
  /**
   * @param intf the interface
   */
  public FreeBusyGetHandler(final CaldavBWIntf intf) {
    super(intf);
  }

  @Override
  public void process(final HttpServletRequest req,
                      final HttpServletResponse resp,
                      final RequestPars pars) {
    try {
      String originator = null;

      if (getAccount() != null) {
        originator = getSysi().principalToCaladdr(getSysi().getPrincipal());
      }

      String cua = req.getParameter("cua");
      String user;

      if (cua == null) {
        user = req.getParameter("user");
        if (user == null) {
          if (getAccount() == null) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing user/cua");
            return;
          }

          user = getAccount();
        }

        cua = getSysi().principalToCaladdr(getSysi().getPrincipalForUser(user));
      }

      pars.setContentType("text/calendar;charset=utf-8");

      final CalDAVAuthProperties authp = getSysi().getAuthProperties();

      final TimeRange tr = ParseUtil.getPeriod(req.getParameter("start"),
                                               req.getParameter("end"),
                                               java.util.Calendar.DATE,
                                               authp.getDefaultFBPeriod(),
                                               java.util.Calendar.DATE,
                                               authp.getMaxFBPeriod());

      if (tr == null) {
        resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Date/times");
        return;
      }

      final Set<String> recipients = new TreeSet<>();
      resp.setHeader("Content-Disposition",
                     "Attachment; Filename=\"freebusy.ics\"");
      resp.setContentType("text/calendar;charset=utf-8");
      recipients.add(cua);

      getSysi().getSpecialFreeBusy(cua, recipients,
                                   originator,
                                   tr, resp.getWriter());
    } catch (final WebdavForbidden wdf) {
      resp.setStatus(HttpServletResponse.SC_FORBIDDEN);
    } catch (final WebdavException wde) {
      throw wde;
    } catch (final Throwable t) {
      throw new WebdavException(t);
    }
  }
}
