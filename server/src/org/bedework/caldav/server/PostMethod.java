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

import org.bedework.caldav.server.SysIntf.CalUserInfo;
import org.bedework.calfacade.BwCalendar;
import org.bedework.calfacade.BwEvent;
import org.bedework.calfacade.BwFreeBusy;
import org.bedework.calfacade.BwOrganizer;
import org.bedework.calfacade.ScheduleResult;
import org.bedework.calfacade.ScheduleResult.ScheduleRecipientResult;
import org.bedework.davdefs.CaldavTags;
import org.bedework.icalendar.Icalendar;
import org.bedework.icalendar.VFreeUtil;

import edu.rpi.cct.webdav.servlet.common.MethodBase;
import edu.rpi.cct.webdav.servlet.shared.WebdavBadRequest;
import edu.rpi.cct.webdav.servlet.shared.WebdavException;
import edu.rpi.cct.webdav.servlet.shared.WebdavForbidden;
import edu.rpi.cct.webdav.servlet.shared.WebdavNotFound;
import edu.rpi.cct.webdav.servlet.shared.WebdavNsIntf;
import edu.rpi.cct.webdav.servlet.shared.WebdavNsNode;
import edu.rpi.cmt.access.PrivilegeDefs;

import net.fortuna.ical4j.model.component.VFreeBusy;

import java.util.Collection;
import java.util.Enumeration;
import java.util.TreeSet;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/** Class called to handle POST
 *
 *   @author Mike Douglass   douglm@rpi.edu
 */
public class PostMethod extends MethodBase {
  /* (non-Javadoc)
   * @see edu.rpi.cct.webdav.servlet.common.MethodBase#init()
   */
  public void init() {
  }

  private static class RequestPars {
    BwCalendar cal;

    Icalendar ic;

    String originator;

    Collection<String> recipients = new TreeSet<String>();
  }

  public void doMethod(HttpServletRequest req,
                       HttpServletResponse resp) throws WebdavException {
    if (debug) {
      trace("PostMethod: doMethod");
    }

    try {
      CaldavBWIntf intf = (CaldavBWIntf)getNsIntf();
      WebdavNsNode node = intf.getNode(getResourceUri(req),
                                       WebdavNsIntf.existanceMust,
                                       WebdavNsIntf.nodeTypeCollection);

      if (node == null) {
        resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
        return;
      }

      RequestPars pars = new RequestPars();

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
      CaldavCalNode calnode = intf.getCalnode(node,
                                              HttpServletResponse.SC_FORBIDDEN);
      pars.cal = calnode.getCDURI().getCal();

      if (pars.cal.getCalType() != BwCalendar.calTypeOutbox) {
        if (debug) {
          debugMsg("Not targetted at Outbox");
        }
        throw new WebdavForbidden();
      }

      /* (CALDAV:supported-calendar-data) */
      if (!"text/calendar".equals(req.getContentType())) {
        if (debug) {
          debugMsg("Bad content type: " + req.getContentType());
        }
        throw new WebdavBadRequest("Bad content type: " + req.getContentType());
      }

      /* (CALDAV:valid-calendar-data) -- later */
      /* (CALDAV:valid-scheduling-message) -- later */

      /* (CALDAV:originator-specified) */
      pars.originator = req.getHeader("Originator");
      if (pars.originator == null) {
        if (debug) {
          debugMsg("No originator");
        }
        throw new WebdavNotFound("No originator");
      }

      /* (CALDAV:originator-allowed)
       * The authenticated user is the originator so we just check the current
       * user has schedule acess to the outbox.
       */
      if (intf.getSysi().checkAccess(pars.cal,
                                     PrivilegeDefs.privSchedule,
                                     true) == null) {
        if (debug) {
          debugMsg("No access for scheduling");
        }
        throw new WebdavForbidden("No access for scheduling");
      }

      /* (CALDAV:organizer-allowed) -- later */

      /* (CALDAV:recipient-specified) */
      Enumeration rs = req.getHeaders("Recipient");

      if ((rs == null) || (!rs.hasMoreElements())) {
        if (debug) {
          debugMsg("No recipient(s)");
        }
        throw new WebdavNotFound("No recipient(s)");
      } else {
        while (rs.hasMoreElements()) {
          pars.recipients.add((String)rs.nextElement());
        }
      }

      try {
        pars.ic = intf.getIcal(pars.cal, req);
      } catch (Throwable t) {
        if (debug) {
          error(t);
        }
      }

      /* (CALDAV:valid-calendar-data) -- exception above means invalid */
      if ((pars.ic == null) ||
          (pars.ic.size() != 1)) {
        if (debug) {
          debugMsg("Not icalendar");
        }
        throw new WebdavBadRequest("Not icalendar");
      }

      if (!pars.ic.validItipMethodType()) {
        if (debug) {
          debugMsg("Bad method: " + String.valueOf(pars.ic.getMethodType()));
        }
        throw new WebdavBadRequest("Bad method: " +
                                   String.valueOf(pars.ic.getMethodType()));
      }

      /* Do the stuff we deferred above */

      /* (CALDAV:valid-scheduling-message) -- later */

      /* (CALDAV:organizer-allowed) */
      /* There must be a valid organizer with an outbox. */
      BwOrganizer organizer = pars.ic.getOrganizer();

      if (organizer == null) {
        throw new WebdavForbidden("No access for scheduling");
      }

      /* See if it's a valid calendar user. */
      SysIntf sysi = intf.getSysi();
      String cn = organizer.getCn();
      CalUserInfo organizerInfo = sysi.getCalUserInfo(sysi.caladdrToUser(cn));

      if (debug) {
        if (organizerInfo == null) {
          trace("organizerInfo for " + cn + " is NULL");
        } else {
          trace("organizer cn = " + cn +
                ", reqPath = " + req.getPathTranslated() +
                ", outBoxPath = " + organizerInfo.outboxPath);
        }
      }

      if (organizerInfo == null) {
        throw new WebdavForbidden("No access for scheduling");
      }

      if (pars.ic.requestMethodType()) {
        /* This must be targetted at the organizers outbox. */
        if (!req.getPathTranslated().equals(organizerInfo.outboxPath)) {
          throw new WebdavForbidden("No access for scheduling");
        }
      } else {
        /* This must have only one attendee - request must be targetted at attendees outbox*/
      }

      startEmit(resp);

      resp.setStatus(HttpServletResponse.SC_OK);
      resp.setContentType("text/xml");

      openTag(CaldavTags.scheduleResponse);

      if (pars.ic.getComponentType() == Icalendar.ComponentType.event) {
        handleEvent(intf, pars);
      } else if (pars.ic.getComponentType() == Icalendar.ComponentType.freebusy) {
        handleFreeBusy(intf, pars);
      } else {
        if (debug) {
          debugMsg("Unsupported component type: " + pars.ic.getComponentType());
        }
        throw new WebdavBadRequest("org.bedework.caldav.unsupported.component " +
                                   pars.ic.getComponentType());
      }

      closeTag(CaldavTags.scheduleResponse);

      flush();
    } catch (WebdavException we) {
      throw we;
    } catch (Throwable t) {
      throw new WebdavException(t);
    }
  }

  private void handleEvent(CaldavBWIntf intf,
                           RequestPars pars) throws WebdavException {
    BwEvent event = pars.ic.getEvent();
    event.setRecipients(pars.recipients);
    event.setScheduleMethod(pars.ic.getMethodType());

    ScheduleResult sr = intf.getSysi().schedule(event);
    checkStatus(sr);

    for (ScheduleRecipientResult srr: sr.recipientResults) {
      openTag(CaldavTags.response);

      property(CaldavTags.recipient, srr.recipient);

      setReqstat(srr.status);
      closeTag(CaldavTags.response);
    }
  }

  private void handleFreeBusy(CaldavBWIntf intf,
                              RequestPars pars) throws WebdavException {
    BwFreeBusy fb = pars.ic.getFreeBusy();
    fb.setRecipients(pars.recipients);
    fb.setScheduleMethod(pars.ic.getMethodType());

    ScheduleResult sr = intf.getSysi().requestFreeBusy(fb);
    checkStatus(sr);

    for (ScheduleRecipientResult srr: sr.recipientResults) {
      openTag(CaldavTags.response);

      BwFreeBusy rfb = srr.freeBusy;
      if (rfb != null) {
        VFreeBusy vfreeBusy;
        try {
          vfreeBusy = VFreeUtil.toVFreeBusy(rfb);
        } catch (Throwable t) {
          if (debug) {
            error(t);
          }
          throw new WebdavException(t);
        }
        cdataProperty(CaldavTags.calendarData, vfreeBusy.toString());
      }

      setReqstat(srr.status);
      closeTag(CaldavTags.response);
    }
  }

  private boolean checkStatus(ScheduleResult sr) throws WebdavException {
    // XXX Needs to set a response on failure
    if (sr.badMethod) {
      if (debug) {
        debugMsg("ScheduleResult: badMethod");
      }

      return false;
    }

    if (sr.noRecipients) {
      if (debug) {
        debugMsg("ScheduleResult: noRecipients");
      }

      return false;
    }

    return true;
  }

  private void setReqstat(int status) throws WebdavException {
    String reqstat;

    if (status == ScheduleRecipientResult.scheduleDeferred) {
      reqstat = BwEvent.requestStatusDeferred;
    } else if (status == ScheduleRecipientResult.scheduleNoAccess) {
      reqstat = BwEvent.requestStatusNoAccess;
    } else {
      reqstat = BwEvent.requestStatusOK;
    }

    property(CaldavTags.requestStatus, reqstat);
  }
}
