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

import edu.rpi.cct.webdav.servlet.shared.WebdavException;
import edu.rpi.cct.webdav.servlet.shared.WebdavForbidden;

import java.util.Set;
import java.util.TreeSet;

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
      String originator = null;

      if (getAccount() != null) {
        originator = getSysi().principalToCaladdr(getSysi().getPrincipal());
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

        cua = getSysi().principalToCaladdr(getSysi().getPrincipal(user));
      }

      pars.setContentType("text/calendar; charset=UTF-8");

      CalDAVAuthProperties authp = getSysi().getAuthProperties();

      TimeRange tr = ParseUtil.getPeriod(req.getParameter("start"),
                                         req.getParameter("end"),
                                         java.util.Calendar.DATE,
                                         authp.getDefaultFBPeriod(),
                                         java.util.Calendar.DATE,
                                         authp.getMaxFBPeriod());

      if (tr == null) {
        resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Date/times");
        return;
      }

      Set<String> recipients = new TreeSet<String>();
      resp.setHeader("Content-Disposition",
                     "Attachment; Filename=\"freebusy.ics\"");
      resp.setContentType("text/calendar; charset=UTF-8");
      recipients.add(cua);

      getSysi().getSpecialFreeBusy(cua, recipients,
                                   originator,
                                   tr, resp.getWriter());
    } catch (WebdavForbidden wdf) {
      resp.setStatus(HttpServletResponse.SC_FORBIDDEN);
    } catch (WebdavException wde) {
      throw wde;
    } catch (Throwable t) {
      throw new WebdavException(t);
    }
  }
}
