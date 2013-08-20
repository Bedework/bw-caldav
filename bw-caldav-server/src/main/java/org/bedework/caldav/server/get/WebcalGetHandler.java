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

import org.bedework.caldav.server.CalDAVEvent;
import org.bedework.caldav.server.CaldavBWIntf;
import org.bedework.caldav.server.CaldavComponentNode;
import org.bedework.caldav.server.RequestPars;
import org.bedework.caldav.server.sysinterface.CalDAVAuthProperties;
import org.bedework.caldav.server.sysinterface.SysIntf.MethodEmitted;
import org.bedework.caldav.util.ParseUtil;
import org.bedework.caldav.util.TimeRange;

import edu.rpi.cct.webdav.servlet.shared.WebdavException;
import edu.rpi.cct.webdav.servlet.shared.WebdavNsIntf;
import edu.rpi.cct.webdav.servlet.shared.WebdavNsNode;
import edu.rpi.sss.util.xml.tagdefs.XcalTags;

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
      CalDAVAuthProperties authp = getSysi().getAuthProperties();

      TimeRange tr = ParseUtil.getPeriod(req.getParameter("start"),
                                         req.getParameter("end"),
                                         java.util.Calendar.DATE,
                                         authp.getDefaultWebCalPeriod(),
                                         java.util.Calendar.DATE,
                                         authp.getMaxWebCalPeriod());

      if (tr == null) {
        resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Date/times");
        return;
      }

      String calPath;

      if (pars.webcalGetAccept) {
        calPath = pars.resourceUri;
      } else {
        calPath = req.getParameter("calPath");
        if (calPath == null) {
          resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "No calPath");
          return;
        }

        calPath = WebdavNsIntf.fixPath(calPath);
      }

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


      String suffix;
      String acceptType = pars.acceptType;
      if (acceptType == null) {
        acceptType = getSysi().getDefaultContentType();
      }

      if (acceptType.equals(XcalTags.mimetype)) {
        // No charset
        resp.setContentType(acceptType);
        suffix = ".xcs";
      } else {
        resp.setContentType(acceptType + "; charset=UTF-8");
        suffix = ".ics";
      }

      resp.setHeader("Content-Disposition",
                     "Attachment; Filename=\"" +
                     node.getDisplayname() + suffix + "\"");

      getSysi().writeCalendar(evs, MethodEmitted.publish,
                              null,
                              resp.getWriter(),
                              acceptType);
    } catch (WebdavException wde) {
      throw wde;
    } catch (Throwable t) {
      throw new WebdavException(t);
    }
  }
}
