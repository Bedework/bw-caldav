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
import org.bedework.util.misc.Util;
import org.bedework.util.xml.tagdefs.IscheduleTags;
import org.bedework.webdav.servlet.shared.WebdavException;

import java.io.Writer;
import java.util.Base64;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.namespace.QName;

/** Handle ischedule GET requests.
 *
 * @author Mike Douglass
 */
public class IscheduleGetHandler extends GetHandler {
  /**
   * @param intf system interface
   */
  public IscheduleGetHandler(final CaldavBWIntf intf) {
    super(intf);
  }

  /* (non-Javadoc)
   * @see org.bedework.caldav.server.get.GetHandler#process(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse, org.bedework.caldav.server.PostMethod.RequestPars)
   */
  @Override
  public void process(final HttpServletRequest req,
                      final HttpServletResponse resp,
                      final RequestPars pars) {
    try {
      if (pars.getNoPrefixResourceUri().length() == 0) {
        final String query = req.getParameter("action");

        if (Util.equalsString(query, "capabilities")) {
          doCapabilities(resp);
          return;
        }

        resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Bad request parameters");
      }

      if (pars.getNoPrefixResourceUri().startsWith("/domainkey/")) {
        final String[] pe = pars.getNoPrefixResourceUri().split("/");

        if (pe.length < 3) {
          resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Bad request parameters");
          return;
        }

        makeDomainKey(resp, pe[1], pe[2]);
        return;
      }

      resp.sendError(HttpServletResponse.SC_FORBIDDEN);

    //} catch (WebdavException wde) {
    //  throw wde;
    } catch (final Throwable t) {
      throw new WebdavException(t);
    }
  }

  private void makeDomainKey(final HttpServletResponse resp,
                             final String domain,
                             final String service) {
    try {
      final byte[] key = intf.getSysi().getPublicKey(domain, service);

      if ((key == null) || (key.length == 0)) {
        resp.sendError(HttpServletResponse.SC_NOT_FOUND);
        return;
      }

      resp.setContentType("text/plain");

      final Writer wtr = resp.getWriter();

      wtr.write("v=DKIM1;p=");
      wtr.write(Base64.getUrlEncoder().encodeToString(key));
      wtr.close();
    } catch (final Throwable t) {
      throw new WebdavException(t);
    }
  }

  /** Generate an ischedule capabilities response
   *
   * @param resp the response
   */
  private void doCapabilities(final HttpServletResponse resp) {
    try {
      startEmit(resp);

      openTag(IscheduleTags.queryResult);
      openTag(IscheduleTags.capabilities);

      property(IscheduleTags.serialNumber,
               String.valueOf(RequestPars.iScheduleSerialNumber));

      openTag(IscheduleTags.versions);
      property(IscheduleTags.version, "1");
      closeTag(IscheduleTags.versions);

      /* scheduling-messages */

      openTag(IscheduleTags.schedulingMessages);

      openTag(IscheduleTags.component, "name", "VEVENT");
      supportedMethod("REQUEST");
      supportedMethod("ADD");
      supportedMethod("REPLY");
      supportedMethod("CANCEL");
      closeTag(IscheduleTags.component);

      openTag(IscheduleTags.component, "name", "VTODO");
      supportedMethod("REQUEST");
      supportedMethod("ADD");
      supportedMethod("REPLY");
      supportedMethod("CANCEL");
      closeTag(IscheduleTags.component);

      openTag(IscheduleTags.component, "name", "VPOLL");
      supportedMethod("POLLSTATUS");
      supportedMethod("REQUEST");
      supportedMethod("ADD");
      supportedMethod("REPLY");
      supportedMethod("CANCEL");
      closeTag(IscheduleTags.component);

      openTag(IscheduleTags.component, "name", "VFREEBUSY");
      supportedMethod("REQUEST");
      closeTag(IscheduleTags.component);

      closeTag(IscheduleTags.schedulingMessages);

      /* calendar-data-types */

      openTag(IscheduleTags.calendarDataTypes);

      final String[] calDataNames = {"content-type",
                                     "version"
      };

      final String[] calDataVals = {"text/calendar",
                                    "2.0"
      };

      attrTag(IscheduleTags.calendarData, calDataNames, calDataVals);
      closeTag(IscheduleTags.calendarDataTypes);

      /* attachments */

      openTag(IscheduleTags.attachments);
      emptyTag(IscheduleTags.inline);
      emptyTag(IscheduleTags.external);
      closeTag(IscheduleTags.attachments);

      /* supported-recipient-uri-scheme-set * /

      openTag(IscheduleTags.supportedRecipientUriSchemeSet);
      property(IscheduleTags.scheme, "mailto");
      closeTag(IscheduleTags.supportedRecipientUriSchemeSet);
      */

      final CalDAVAuthProperties authp = intf.getSysi().getAuthProperties();

      prop(IscheduleTags.maxContentLength, authp.getMaxUserEntitySize());
      prop(IscheduleTags.minDateTime, authp.getMinDateTime());
      prop(IscheduleTags.maxDateTime, authp.getMaxDateTime());
      prop(IscheduleTags.maxInstances, authp.getMaxInstances());
      prop(IscheduleTags.maxRecipients, authp.getMaxAttendeesPerInstance());
      prop(IscheduleTags.administrator,
           intf.getSysi().getSystemProperties().getAdminContact());

      closeTag(IscheduleTags.capabilities);
      closeTag(IscheduleTags.queryResult);
    } catch (final WebdavException wde) {
      throw wde;
    } catch (final Throwable t) {
      throw new WebdavException(t);
    }
  }

  private void prop(final QName tag,
                    final Object val) {
    if (val == null) {
      return;
    }

    property(tag, String.valueOf(val));
  }

  private void supportedMethod(final String val) {
    try {
      attrTag(IscheduleTags.method, "name", val);
    } catch (final Throwable t) {
      throw new WebdavException(t);
    }
  }

  private void attrTag(final QName tag, final String attrName,
                       final String attrVal) {
    try {
      xml.startTag(tag);
      xml.attribute(attrName, attrVal);
      xml.endEmptyTag();
    } catch (final Throwable t) {
      throw new WebdavException(t);
    }
  }

  private void attrTag(final QName tag, final String[] attrNames,
                       final String[] attrVals) {
    try {
      xml.startTag(tag);
      for (int i = 0; i < attrNames.length; i++) {
        xml.attribute(attrNames[i], attrVals[i]);
      }
      xml.endEmptyTag();
    } catch (final Throwable t) {
      throw new WebdavException(t);
    }
  }
}
