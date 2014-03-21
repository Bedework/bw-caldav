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

import org.apache.commons.codec.binary.Base64;

import java.io.Writer;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.namespace.QName;

/** Handle ischedule GET requests.
 *
 * @author Mike Douglass
 */
public class IscheduleGetHandler extends GetHandler {
  /**
   * @param intf
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
                      final RequestPars pars) throws WebdavException {
    try {
      if (pars.getNoPrefixResourceUri().length() == 0) {
        String query = req.getParameter("action");

        if (Util.equalsString(query, "capabilities")) {
          doCapabilities(req, resp, pars);
          return;
        }

        resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Bad request parameters");
      }

      if (pars.getNoPrefixResourceUri().startsWith("/domainkey")) {
        String[] pe = pars.getNoPrefixResourceUri().split("/");

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
    } catch (Throwable t) {
      throw new WebdavException(t);
    }
  }

  private void makeDomainKey(final HttpServletResponse resp,
                             final String domain,
                             final String service) throws WebdavException {
    try {
      byte[] key = intf.getSysi().getPublicKey(domain, service);

      if ((key == null) || (key.length == 0)) {
        resp.sendError(HttpServletResponse.SC_NOT_FOUND);
        return;
      }

      resp.setContentType("text/plain");

      Writer wtr = resp.getWriter();

      wtr.write("v=DKIM1;p=");
      wtr.write(new String(new Base64().encode(key)));
      wtr.close();
    } catch (Throwable t) {
      throw new WebdavException(t);
    }
  }

  /** Generate an ischedule capabilities response
   *
   * @param req
   * @param resp
   * @param pars
   * @throws WebdavException
   */
  private void doCapabilities(final HttpServletRequest req,
                              final HttpServletResponse resp,
                              final RequestPars pars) throws WebdavException {
    try {
      startEmit(resp);

      openTag(IscheduleTags.queryResult);
      openTag(IscheduleTags.capabilities);

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

      openTag(IscheduleTags.component, "name", "VFREEBUSY");
      closeTag(IscheduleTags.component);

      closeTag(IscheduleTags.schedulingMessages);

      /* calendar-data-types */

      openTag(IscheduleTags.calendarDataTypes);

      String[] calDataNames = {"content-type",
                               "version"
      };

      String[] calDataVals = {"text/calendar",
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

      CalDAVAuthProperties authp = intf.getSysi().getAuthProperties();

      prop(IscheduleTags.maxContentLength, authp.getMaxUserEntitySize());
      prop(IscheduleTags.minDateTime, authp.getMinDateTime());
      prop(IscheduleTags.maxDateTime, authp.getMaxDateTime());
      prop(IscheduleTags.maxInstances, authp.getMaxInstances());
      prop(IscheduleTags.maxRecipients, authp.getMaxAttendeesPerInstance());
      prop(IscheduleTags.administrator,
           intf.getSysi().getSystemProperties().getAdminContact());

      closeTag(IscheduleTags.capabilities);
      closeTag(IscheduleTags.queryResult);
    } catch (WebdavException wde) {
      throw wde;
    } catch (Throwable t) {
      throw new WebdavException(t);
    }
  }

  private void prop(final QName tag,
                    final Object val) throws WebdavException {
    if (val == null) {
      return;
    }

    property(tag, String.valueOf(val));
  }

  private void supportedMethod(final String val) throws WebdavException {
    try {
      attrTag(IscheduleTags.method, "name", val);
    } catch (Throwable t) {
      throw new WebdavException(t);
    }
  }

  private void attrTag(final QName tag, final String attrName,
                       final String attrVal) throws WebdavException {
    try {
      xml.startTag(tag);
      xml.attribute(attrName, attrVal);
      xml.endEmptyTag();
    } catch (Throwable t) {
      throw new WebdavException(t);
    }
  }

  private void attrTag(final QName tag, final String[] attrNames,
                       final String[] attrVals) throws WebdavException {
    try {
      xml.startTag(tag);
      for (int i = 0; i < attrNames.length; i++) {
        xml.attribute(attrNames[i], attrVals[i]);
      }
      xml.endEmptyTag();
    } catch (Throwable t) {
      throw new WebdavException(t);
    }
  }
}
