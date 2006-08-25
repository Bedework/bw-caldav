/*
 Copyright (c) 2000-2005 University of Washington.  All rights reserved.

 Redistribution and use of this distribution in source and binary forms,
 with or without modification, are permitted provided that:

   The above copyright notice and this permission notice appear in
   all copies and supporting documentation;

   The name, identifiers, and trademarks of the University of Washington
   are not used in advertising or publicity without the express prior
   written permission of the University of Washington;

   Recipients acknowledge that this distribution is made available as a
   research courtesy, "as is", potentially with defects, without
   any obligation on the part of the University of Washington to
   provide support, services, or repair;

   THE UNIVERSITY OF WASHINGTON DISCLAIMS ALL WARRANTIES, EXPRESS OR
   IMPLIED, WITH REGARD TO THIS SOFTWARE, INCLUDING WITHOUT LIMITATION
   ALL IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A
   PARTICULAR PURPOSE, AND IN NO EVENT SHALL THE UNIVERSITY OF
   WASHINGTON BE LIABLE FOR ANY SPECIAL, INDIRECT OR CONSEQUENTIAL
   DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR
   PROFITS, WHETHER IN AN ACTION OF CONTRACT, TORT (INCLUDING
   NEGLIGENCE) OR STRICT LIABILITY, ARISING OUT OF OR IN CONNECTION WITH
   THE USE OR PERFORMANCE OF THIS SOFTWARE.
 */
/* **********************************************************************
    Copyright 2005 Rensselaer Polytechnic Institute. All worldwide rights reserved.

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
package org.bedework.caldav.server;

import org.bedework.calfacade.BwCalendar;
import org.bedework.icalendar.Icalendar;

import edu.rpi.cct.webdav.servlet.common.MethodBase;
import edu.rpi.cct.webdav.servlet.shared.WebdavBadRequest;
import edu.rpi.cct.webdav.servlet.shared.WebdavException;
import edu.rpi.cct.webdav.servlet.shared.WebdavForbidden;
import edu.rpi.cct.webdav.servlet.shared.WebdavNotFound;
import edu.rpi.cct.webdav.servlet.shared.WebdavNsNode;
import edu.rpi.cmt.access.PrivilegeDefs;

import java.util.Enumeration;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/** Class called to handle POST
 *
 *   @author Mike Douglass   douglm@rpi.edu
 */
public class PostMethod extends MethodBase {
  /** Called at each request
   */
  public void init() {
  }

  public void doMethod(HttpServletRequest req,
                       HttpServletResponse resp) throws WebdavException {
    if (debug) {
      trace("PostMethod: doMethod");
    }

    try {
      CaldavBWIntf intf = (CaldavBWIntf)getNsIntf();
      WebdavNsNode node = intf.getNode(getResourceUri(req));

      if (node == null) {
        resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
        return;
      }

      /* Preconditions:
        (CALDAV:supported-collection):
               The Request-URI MUST identify the location of a scheduling Outbox collection;
        (CALDAV:supported-calendar-data):
               The resource submitted in the POST request MUST be a supported
               media type (i.e., text/calendar) for scheduling or free-busy messages;
        (CALDAV:valid-calendar-data): The resource submitted in the POST request
                MUST be valid data for the media type being specified (i.e.,
                valid iCalendar object) ;
        (CALDAV:valid-scheduling-message): The resource submitted in the POST
                request MUST obey all restrictions specified for the POST request
                (e.g., scheduling message follows the restriction of iTIP);
        (CALDAV:originator-specified): The POST request MUST include a valid
                Originator request header specifying a calendar user address of
                the currently authenticated user;
        (CALDAV:originator-allowed): The calendar user identified by the
                Originator request header in the POST request MUST be granted the
                CALDAV:schedule privilege or a suitable sub-privilege on the
                scheduling Outbox collection being targeted by the request;
        (CALDAV:organizer-allowed): The calendar user identified by the ORGANIZER
                property in the POST request's scheduling message MUST be the
                owner (or one of the owners) of the scheduling Outbox being
                targeted by the request;
        (CALDAV:recipient-specified): The POST request MUST include one or more
                valid Recipient request headers specifying the calendar user
                address of users to whom the scheduling message will be delivered.
      */

      /* (CALDAV:supported-collection) */
      CaldavCalNode calnode = intf.getCalnode(node, HttpServletResponse.SC_FORBIDDEN);
      BwCalendar cal = calnode.getCDURI().getCal();

      if (cal.getCalType() != BwCalendar.calTypeOutbox) {
        throw new WebdavForbidden();
      }

      /* (CALDAV:supported-calendar-data) */
      if (req.getContentType() != "text/calendar") {
        throw new WebdavForbidden();
      }

      /* (CALDAV:valid-calendar-data) -- later */
      /* (CALDAV:valid-scheduling-message) -- later */

      /* (CALDAV:originator-specified) */
      String originator = req.getHeader("Originator");
      if (originator == null) {
        throw new WebdavNotFound();
      }

      /* (CALDAV:originator-allowed)
       * The authenticated user is the originator so we just check the current
       * user has schedule acess to the outbox.
       */
      if (intf.getSysi().checkAccess(cal,
                                     PrivilegeDefs.privSchedule,
                                     true) == null) {
        throw new WebdavForbidden();
      }

      /* (CALDAV:organizer-allowed) -- later */

      /* (CALDAV:recipient-specified) */
      Enumeration rs = req.getHeaders("Recipient");
      if ((rs == null) || (!rs.hasMoreElements())) {
        throw new WebdavNotFound();
      }

      Icalendar ic = null;

      try {
        ic = intf.getIcal(cal, req);
      } catch (Throwable t) {
        if (debug) {
          error(t);
        }
      }

      /* (CALDAV:valid-calendar-data) -- exception above means invalid */
      if ((ic == null) ||
          (ic.size() != 1)) {
        throw new WebdavBadRequest();
      }

      if ((ic.getMethodType() != Icalendar.methodTypePublish) &&
          (ic.getMethodType() != Icalendar.methodTypeRequest)) {
        throw new WebdavBadRequest();
      }

      /* Do the stuff we deferred above */

      /* (CALDAV:valid-scheduling-message) -- later */
      /* (CALDAV:organizer-allowed) -- later */

      if (ic.getComponentType() != Icalendar.componentTypeEvent) {
        throw new WebdavBadRequest();
      }

      intf.getSysi().scheduleRequest(ic.getEvent());
    } catch (WebdavException we) {
      throw we;
    } catch (Throwable t) {
      throw new WebdavException(t);
    }

  }
}
