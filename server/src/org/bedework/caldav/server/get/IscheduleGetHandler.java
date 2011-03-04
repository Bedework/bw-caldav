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
import org.bedework.caldav.server.PostMethod.RequestPars;
import org.bedework.caldav.server.sysinterface.SystemProperties;

import edu.rpi.cct.webdav.servlet.shared.WebdavException;
import edu.rpi.sss.util.Util;
import edu.rpi.sss.util.xml.tagdefs.IscheduleTags;

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
      String query = req.getParameter("query");

      if (Util.equalsString(query, "capabilities")) {
        doCapabilities(req, resp, pars);
        return;
      }

      resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Bad request parameters");

    //} catch (WebdavException wde) {
    //  throw wde;
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
      SystemProperties sysp = intf.getSysi().getSystemProperties();

      startEmit(resp);

      openTag(IscheduleTags.queryResult);
      openTag(IscheduleTags.capabilitySet);

      openTag(IscheduleTags.supportedVersionSet);
      property(IscheduleTags.version, "1");
      closeTag(IscheduleTags.supportedVersionSet);

      /* supported-scheduling-message-set */

      openTag(IscheduleTags.supportedSchedulingMessageSet);

      openTag(IscheduleTags.comp, "name", "VEVENT");
      supportedMethod("REQUEST");
      supportedMethod("ADD");
      supportedMethod("REPLY");
      supportedMethod("CANCEL");
      closeTag(IscheduleTags.comp);

      openTag(IscheduleTags.comp, "name", "VEVENT");
      supportedMethod("REQUEST");
      supportedMethod("ADD");
      supportedMethod("REPLY");
      supportedMethod("CANCEL");
      closeTag(IscheduleTags.comp);

      openTag(IscheduleTags.comp, "name", "VTODO");
      closeTag(IscheduleTags.comp);

      openTag(IscheduleTags.comp, "name", "VFREEBUSY");
      closeTag(IscheduleTags.comp);

      closeTag(IscheduleTags.supportedSchedulingMessageSet);

      /* supported-scheduling-message-set */

      openTag(IscheduleTags.supportedCalendarDataType);

      String[] calDataNames = {"content-type",
                               "version"
      };

      String[] calDataVals = {"text/calendar",
                               "2.0"
      };

      attrTag(IscheduleTags.calendarData, calDataNames, calDataVals);
      closeTag(IscheduleTags.supportedCalendarDataType);

      /* supported-attachment-values */

      openTag(IscheduleTags.supportedAttachmentValues);
      emptyTag(IscheduleTags.inlineAttachment);
      emptyTag(IscheduleTags.externalAttachment);
      closeTag(IscheduleTags.supportedAttachmentValues);

      /* supported-recipient-uri-scheme-set */

      openTag(IscheduleTags.supportedRecipientUriSchemeSet);
      property(IscheduleTags.scheme, "mailto");
      closeTag(IscheduleTags.supportedRecipientUriSchemeSet);

      prop(IscheduleTags.maxContentLength, sysp.getMaxUserEntitySize());
      prop(IscheduleTags.minDateTime, sysp.getMinDateTime());
      prop(IscheduleTags.maxDateTime, sysp.getMaxDateTime());
      prop(IscheduleTags.maxInstances, sysp.getMaxInstances());
      prop(IscheduleTags.maxRecipients, sysp.getMaxAttendeesPerInstance());
      prop(IscheduleTags.administrator, sysp.getAdminContact());

      closeTag(IscheduleTags.capabilitySet);
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
