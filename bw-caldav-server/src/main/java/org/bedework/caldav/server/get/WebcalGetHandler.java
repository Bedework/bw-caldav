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
import org.bedework.caldav.util.filter.EntityTimeRangeFilter;
import org.bedework.util.calendar.IcalDefs;
import org.bedework.util.xml.tagdefs.XcalTags;
import org.bedework.webdav.servlet.shared.WebdavException;
import org.bedework.webdav.servlet.shared.WebdavNsIntf;
import org.bedework.webdav.servlet.shared.WebdavNsNode;

import java.util.ArrayList;
import java.util.Collection;
import java.util.function.Supplier;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/** Handle web calendar GET requests.
 *
 * @author Mike Douglass
 */
public class WebcalGetHandler extends GetHandler {
  /**
   * @param intf the interface
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
      final CalDAVAuthProperties authp = getSysi().getAuthProperties();

      final TimeRange tr = ParseUtil.getPeriod(req.getParameter("start"),
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

      if (pars.isWebcalGetAccept()) {
        calPath = pars.getResourceUri();
      } else {
        calPath = req.getParameter("calPath");
        if (calPath == null) {
          resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "No calPath");
          return;
        }

        calPath = WebdavNsIntf.fixPath(calPath);
      }

      final WebdavNsNode node = getNode(calPath,
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

      final Collection<CalDAVEvent> evs = new ArrayList<>();

      final EntityTimeRangeFilter etrf =
              new EntityTimeRangeFilter(null,
                                        IcalDefs.entityTypeEvent,
                                        tr);

      final Supplier<Object> filters = () -> etrf;

      for (final WebdavNsNode child: getChildren(node, filters)) {
        if (child instanceof CaldavComponentNode) {
          evs.add(((CaldavComponentNode)child).getEvent());
        }
      }


      final String suffix;
      String acceptType = pars.getAcceptType();
      if (acceptType == null) {
        acceptType = getSysi().getDefaultContentType();
      }

      if (acceptType.equals(XcalTags.mimetype)) {
        // No charset
        resp.setContentType(acceptType);
        suffix = ".xcs";
      } else {
        resp.setContentType(acceptType + ";charset=utf-8");
        suffix = ".ics";
      }

      resp.setHeader("Content-Disposition",
                     "Attachment; Filename=\"" +
                     node.getDisplayname() + suffix + "\"");

      getSysi().writeCalendar(evs, MethodEmitted.publish,
                              null,
                              resp.getWriter(),
                              acceptType);
    } catch (final WebdavException wde) {
      throw wde;
    } catch (final Throwable t) {
      throw new WebdavException(t);
    }
  }
}
