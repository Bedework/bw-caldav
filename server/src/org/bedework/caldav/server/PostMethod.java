/* **********************************************************************
    Copyright 2008 Rensselaer Polytechnic Institute. All worldwide rights reserved.

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
import org.bedework.calfacade.BwOrganizer;
import org.bedework.calfacade.ScheduleResult;
import org.bedework.calfacade.ScheduleResult.ScheduleRecipientResult;
import org.bedework.calfacade.configs.CalDAVConfig;
import org.bedework.calfacade.exc.CalFacadeException;
import org.bedework.calfacade.svc.EventInfo;
import org.bedework.icalendar.IcalTranslator;
import org.bedework.icalendar.Icalendar;

import edu.rpi.cct.webdav.servlet.common.MethodBase;
import edu.rpi.cct.webdav.servlet.shared.WebdavException;
import edu.rpi.cct.webdav.servlet.shared.WebdavForbidden;
import edu.rpi.cct.webdav.servlet.shared.WebdavNsIntf;
import edu.rpi.cct.webdav.servlet.shared.WebdavNsNode;
import edu.rpi.sss.util.xml.tagdefs.CaldavTags;
import edu.rpi.sss.util.xml.tagdefs.WebdavTags;

import java.io.Reader;
import java.util.Collection;
import java.util.Enumeration;
import java.util.TreeSet;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/** Class called to handle POST for CalDAV scheduling.
 *
 *   @author Mike Douglass   douglm - rpi.edu
 */
public class PostMethod extends MethodBase {
  /* (non-Javadoc)
   * @see edu.rpi.cct.webdav.servlet.common.MethodBase#init()
   */
  public void init() {
  }

  /**
   */
  public static class RequestPars {
    String resourceUri;

    String contentType;

    /** value of the Originator header */
    public String originator;

    /** values of Recipient headers */
    public Collection<String> recipients = new TreeSet<String>();

    Reader reqRdr;

    Icalendar ic;

    BwCalendar cal;

    /* true if this is a realtime request from some other server */
    boolean realTime;

    /** true if this is a free busy request */
    public boolean freeBusy;

    /** true if this is a web calendar request */
    public boolean webcal;

    /**
     * @param req
     * @param intf
     * @param resourceUri
     * @throws WebdavException
     */
    public RequestPars(HttpServletRequest req, CaldavBWIntf intf,
                String resourceUri) throws WebdavException {
      SysIntf sysi = intf.getSysi();

      this.resourceUri = resourceUri;

      CalDAVConfig conf = intf.getConfig();

      if (conf.getRealTimeServiceURI() != null) {
        realTime = conf.getRealTimeServiceURI().equals(resourceUri);
      }

      if (!realTime && (conf.getFburlServiceURI() != null)) {
        freeBusy = conf.getFburlServiceURI().equals(resourceUri);
      }

      if (!realTime && !freeBusy && (conf.getWebcalServiceURI() != null)) {
        webcal = conf.getWebcalServiceURI().equals(resourceUri);
      }

      contentType = req.getContentType();

      if (!freeBusy && !webcal) {
        originator = adjustPrincipal(req.getHeader("Originator"), sysi);

        Enumeration rs = req.getHeaders("Recipient");

        if (rs != null) {
          while (rs.hasMoreElements()) {
            String[] rlist = ((String)rs.nextElement()).split(",");

            if (rlist != null) {
              for (String r: rlist) {
                recipients.add(adjustPrincipal(r.trim(), sysi));
              }
            }
          }
        }

        try {
          reqRdr = req.getReader();
        } catch (Throwable t) {
          throw new WebdavException(t);
        }
      }
    }

    /* We seem to be getting both absolute and relative principals as well as mailto
     * forms of calendar user.
     *
     * If we get an absolute principal - turn it into a relative
     */
    private String adjustPrincipal(String val,
                                   SysIntf sysi) throws WebdavException {
      if (val == null) {
        return null;
      }

      return sysi.getUrlHandler().unprefix(val);
      /*
      if (val.startsWith(sysi.getUrlPrefix())) {
        return val.substring(sysi.getUrlPrefix().length());
      }

      return val;
      */
    }
  }

  public void doMethod(HttpServletRequest req,
                       HttpServletResponse resp) throws WebdavException {
    if (debug) {
      trace("PostMethod: doMethod");
    }

    CaldavBWIntf intf = (CaldavBWIntf)getNsIntf();

    RequestPars pars = new RequestPars(req, intf, getResourceUri(req));

    if (!pars.realTime) {
      // Standard CalDAV scheduling
      doSchedule(intf, pars, resp);
      return;
    }

    /* We have a potential incoming real time scheduling request.
     *
     * NOTE: Leaving this enabled is a security risk
     */
    warn("*****************************************************************");
    warn("* Realtime request - security hole");
    warn("*****************************************************************");

    pars.realTime = true;

    if (intf.getSysi().getAccount() == null) {
      intf.reAuth(req, "realtime01");
    }

    doSchedule(intf, pars, resp);
  }

  /** Handle a scheduling action
   *
   * @param intf
   * @param pars
   * @param resp
   * @throws WebdavException
   */
  public void doSchedule(CaldavBWIntf intf,
                         RequestPars pars,
                         HttpServletResponse resp) throws WebdavException {
    SysIntf sysi = intf.getSysi();

    try {
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
            //(CALDAV:organizer-allowed): The calendar user identified by the ORGANIZER
            //       property in the POST request's scheduling message MUST be the
            //       owner (or one of the owners) of the scheduling Outbox being
            //       targeted by the request;
        (CALDAV:organizer-allowed): The calendar user identified by the
                ORGANIZER property in the POST request's scheduling message MUST
                be the calendar user (or one of the calendar users) associated
                with the scheduling Outbox being targeted by the request when the
                scheduling message is an outgoing scheduling message;
        (CALDAV:recipient-specified): The POST request MUST include one or more
                valid Recipient request headers specifying the calendar user
                address of users to whom the scheduling message will be delivered.
      */

      if (!pars.realTime) {
        WebdavNsNode node = intf.getNode(pars.resourceUri,
                                         WebdavNsIntf.existanceMust,
                                         WebdavNsIntf.nodeTypeCollection);

        if (node == null) {
          resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
          return;
        }

        /* (CALDAV:supported-collection) */
        CaldavCalNode calnode = intf.getCalnode(node,
                                                HttpServletResponse.SC_FORBIDDEN);
        pars.cal = calnode.getCalendar();

        if (pars.cal.getCalType() != BwCalendar.calTypeOutbox) {
          if (debug) {
            debugMsg("Not targetted at Outbox");
          }
          throw new WebdavException(HttpServletResponse.SC_METHOD_NOT_ALLOWED,
          "Not targetted at Outbox");
        }
      }

      /* (CALDAV:supported-calendar-data) */
      if (!pars.contentType.startsWith("text/calendar")) {
        if (debug) {
          debugMsg("Bad content type: " + pars.contentType);
        }
        throw new WebdavForbidden(CaldavTags.supportedCalendarData,
                                  "Bad content type: " + pars.contentType);
      }

      /* (CALDAV:valid-calendar-data) -- later */
      /* (CALDAV:valid-scheduling-message) -- later */

      /* (CALDAV:originator-specified) */
      if (pars.originator == null) {
        if (debug) {
          debugMsg("No originator");
        }
        throw new WebdavForbidden(CaldavTags.originatorSpecified,
                                  "No originator");
      }

      if (pars.realTime) {
        // XXX Who is the originator?
      } else {
        /* (CALDAV:originator-allowed)
         *
         * Every POST request MUST include an Originator request header that
         * specifies the calendar user address of the originator of a given
         * scheduling message.  The value specified in this request header MUST
         * be a calendar user address specified in the CALDAV:calendar-user-
         * address-set property defined on the principal resource of the
         * currently authenticated user.  Also, the currently authenticated user
         * MUST have the CALDAV:schedule privilege or a suitable sub-privilege
         * granted on the targeted scheduling Outbox collection.
         *
         * Ensure the originator is a real calendar user
         */
        String originatorAccount = sysi.caladdrToUser(pars.originator);
        if ((originatorAccount == null) ||
            !sysi.validUser(originatorAccount)) {
          if (debug) {
            debugMsg("No access for scheduling");
          }
          throw new WebdavForbidden(CaldavTags.originatorAllowed,
          "No access for scheduling");
        }
      }

      /* (CALDAV:organizer-allowed) -- later */

      /* (CALDAV:recipient-specified) */
      if (pars.recipients.isEmpty()) {
        if (debug) {
          debugMsg("No recipient(s)");
        }
        throw new WebdavForbidden(CaldavTags.recipientSpecified,
                                  "No recipient(s)");
      }

      try {
        pars.ic = intf.getIcal(pars.cal, pars.reqRdr);
      } catch (Throwable t) {
        if (debug) {
          error(t);
        }

        pars.ic = null;
      }

      /* (CALDAV:valid-calendar-data) -- exception above means invalid */
      if ((pars.ic == null) || (pars.ic.size() != 1)) {
        if (debug) {
          debugMsg("Not icalendar");
        }
        throw new WebdavForbidden(CaldavTags.validCalendarData, "Not icalendar");
      }

      if (!pars.ic.validItipMethodType()) {
        if (debug) {
          debugMsg("Bad method: " + String.valueOf(pars.ic.getMethodType()));
        }
        throw new WebdavForbidden(CaldavTags.validCalendarData, "Bad METHOD");
      }

      /* Do the stuff we deferred above */

      /* (CALDAV:valid-scheduling-message) -- later */

      /* (CALDAV:organizer-allowed) */
      /* There must be a valid organizer with an outbox for outgoing. */
      if (!pars.realTime && pars.ic.requestMethodType()) {
        BwOrganizer organizer = pars.ic.getOrganizer();

        if (organizer == null) {
          throw new WebdavForbidden(CaldavTags.organizerAllowed,
          "No access for scheduling");
        }

        /* See if it's a valid calendar user. */
        String cn = organizer.getOrganizerUri();
        /*
        if (cn.startsWith(sysi.getUrlPrefix())) {
          cn = cn.substring(sysi.getUrlPrefix().length());
          organizer.setOrganizerUri(cn);
        }
        */
        organizer.setOrganizerUri(sysi.getUrlHandler().unprefix(cn));
        CalUserInfo organizerInfo = sysi.getCalUserInfo(sysi.caladdrToUser(cn),
                                                        false);

        if (debug) {
          if (organizerInfo == null) {
            trace("organizerInfo for " + cn + " is NULL");
          } else {
            trace("organizer cn = " + cn +
                  ", resourceUri = " + pars.resourceUri +
                  ", outBoxPath = " + organizerInfo.outboxPath);
          }
        }

        if (organizerInfo == null) {
          throw new WebdavForbidden(CaldavTags.organizerAllowed,
          "No access for scheduling");
        }

        /* This must be targetted at the organizers outbox. */
        if (!pars.resourceUri.equals(organizerInfo.outboxPath)) {
          throw new WebdavForbidden(CaldavTags.organizerAllowed,
                                    "No access for scheduling");
        }
      } else {
        /* This must have only one attendee - request must be targetted at attendees outbox*/
      }

      if (pars.ic.getComponentType() == Icalendar.ComponentType.event) {
        handleEvent(intf, pars, resp);
      } else if (pars.ic.getComponentType() == Icalendar.ComponentType.freebusy) {
        handleFreeBusy(intf, pars, resp);
      } else {
        if (debug) {
          debugMsg("Unsupported component type: " + pars.ic.getComponentType());
        }
        throw new WebdavForbidden("org.bedework.caldav.unsupported.component " +
                                  pars.ic.getComponentType());
      }

      flush();
    } catch (WebdavException we) {
      throw we;
    } catch (Throwable t) {
      throw new WebdavException(t);
    }
  }

  private void handleEvent(CaldavBWIntf intf,
                           RequestPars pars,
                           HttpServletResponse resp) throws WebdavException {
    EventInfo ei = pars.ic.getEventInfo();
    BwEvent ev = ei.getEvent();
    if (pars.recipients != null) {
      for (String r: pars.recipients) {
        ev.addRecipient(r);
      }
    }
    //event.setRecipients(pars.recipients);
    ev.setOriginator(pars.originator);
    ev.setScheduleMethod(pars.ic.getMethodType());

    ScheduleResult sr = intf.getSysi().schedule(ei);
    checkStatus(sr);

    startEmit(resp);

    resp.setStatus(HttpServletResponse.SC_OK);
    resp.setContentType("text/xml; charset=UTF-8");

    openTag(CaldavTags.scheduleResponse);

    for (ScheduleRecipientResult srr: sr.recipientResults) {
      openTag(CaldavTags.response);

      openTag(CaldavTags.recipient);
      property(WebdavTags.href, srr.recipient);
      closeTag(CaldavTags.recipient);

      setReqstat(srr.status);
      closeTag(CaldavTags.response);
    }

    closeTag(CaldavTags.scheduleResponse);
  }

  private void handleFreeBusy(CaldavBWIntf intf,
                              RequestPars pars,
                              HttpServletResponse resp) throws WebdavException {
    EventInfo ei = pars.ic.getEventInfo();
    BwEvent ev = ei.getEvent();
    ev.setRecipients(pars.recipients);
    ev.setOriginator(pars.originator);
    ev.setScheduleMethod(pars.ic.getMethodType());

    ScheduleResult sr = intf.getSysi().requestFreeBusy(ei);

    if (debug) {
      debugMsg("ScheduleResult: " + sr);
    }
    checkStatus(sr);

    startEmit(resp);

    resp.setStatus(HttpServletResponse.SC_OK);
    resp.setContentType("text/xml; charset=UTF-8");

    openTag(CaldavTags.scheduleResponse);

    for (ScheduleRecipientResult srr: sr.recipientResults) {
      openTag(CaldavTags.response);
      openTag(CaldavTags.recipient);
      property(WebdavTags.href, srr.recipient);
      closeTag(CaldavTags.recipient);
      setReqstat(srr.status);

      BwEvent rfb = srr.freeBusy;
      if (rfb != null) {
        rfb.setOrganizer(pars.ic.getOrganizer());

        try {
          cdataProperty(CaldavTags.calendarData,
                        IcalTranslator.toIcalString(Icalendar.methodTypeReply,
                                                    rfb));
        } catch (Throwable t) {
          if (debug) {
            error(t);
          }
          throw new WebdavException(t);
        }
      }

      closeTag(CaldavTags.response);
    }

    closeTag(CaldavTags.scheduleResponse);
  }

  /**
   * @param sr
   * @return boolean
   * @throws WebdavException
   */
  public static boolean checkStatus(ScheduleResult sr) throws WebdavException {
    if (sr.errorCode == null) {
      return true;
    }

    if (sr.errorCode == CalFacadeException.schedulingBadMethod) {
      throw new WebdavForbidden(CaldavTags.validCalendarData, "Bad METHOD");
    }

    if (sr.errorCode == CalFacadeException.schedulingBadAttendees) {
      throw new WebdavForbidden(CaldavTags.attendeeAllowed, "Bad attendees");
    }

    if (sr.errorCode == CalFacadeException.schedulingAttendeeAccessDisallowed) {
      throw new WebdavForbidden(CaldavTags.attendeeAllowed, "attendeeAccessDisallowed");
    }

    if (sr.errorCode == CalFacadeException.schedulingNoRecipients) {
      return false;
    }

    throw new WebdavForbidden(sr.errorCode);
  }

  private void setReqstat(int status) throws WebdavException {
    String reqstat;

    if (status == ScheduleRecipientResult.scheduleDeferred) {
      reqstat = BwEvent.requestStatusDeferred;
    } else if (status == ScheduleRecipientResult.scheduleNoAccess) {
      propertyTagVal(WebdavTags.error, CaldavTags.recipientPermissions);
      reqstat = BwEvent.requestStatusNoAccess;
    } else {
      reqstat = BwEvent.requestStatusOK;
    }

    property(CaldavTags.requestStatus, reqstat);
  }
}
