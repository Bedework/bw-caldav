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

      openTag(IscheduleTags.supportedCalendarData);

      String[] calDataNames = {"content-type",
                               "version"
      };

      String[] calDataVals = {"text/calendar",
                               "2.0"
      };

      attrTag(IscheduleTags.calendarData, calDataNames, calDataVals);
      closeTag(IscheduleTags.supportedCalendarData);

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
