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
package org.bedework.caldav.server;

import org.bedework.caldav.server.soap.exsynch.ExsynchwsHandler;
import org.bedework.caldav.server.sysinterface.CalPrincipalInfo;
import org.bedework.caldav.server.sysinterface.SysIntf;
import org.bedework.caldav.server.sysinterface.SysIntf.IcalResultType;
import org.bedework.caldav.server.sysinterface.SysIntf.SchedRecipientResult;
import org.bedework.caldav.util.CalDAVConfig;

import edu.rpi.cct.webdav.servlet.common.MethodBase;
import edu.rpi.cct.webdav.servlet.shared.WebdavException;
import edu.rpi.cct.webdav.servlet.shared.WebdavForbidden;
import edu.rpi.cct.webdav.servlet.shared.WebdavNsIntf;
import edu.rpi.cct.webdav.servlet.shared.WebdavNsNode;
import edu.rpi.cmt.calendar.IcalDefs;
import edu.rpi.cmt.calendar.ScheduleMethods;
import edu.rpi.cmt.calendar.IcalDefs.IcalComponentType;
import edu.rpi.sss.util.xml.XmlEmit.NameSpace;
import edu.rpi.sss.util.xml.tagdefs.CaldavTags;
import edu.rpi.sss.util.xml.tagdefs.IscheduleTags;
import edu.rpi.sss.util.xml.tagdefs.WebdavTags;
import edu.rpi.sss.util.xml.tagdefs.XcalTags;

import java.io.Reader;
import java.util.Collection;
import java.util.Enumeration;
import java.util.Set;
import java.util.TreeSet;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.namespace.QName;

/** Class called to handle POST for CalDAV scheduling.
 *
 *   @author Mike Douglass   douglm - rpi.edu
 */
public class PostMethod extends MethodBase {
  /* (non-Javadoc)
   * @see edu.rpi.cct.webdav.servlet.common.MethodBase#init()
   */
  @Override
  public void init() {
  }

  /**
   */
  public static class RequestPars {
    /** */
    public HttpServletRequest req;

    /** */
    public String resourceUri;

    /** from accept header */
    public String acceptType;

    /** type of request body */
    String contentType;

    /** Broken out content type */
    public String[] contentTypePars;

    /** value of the Originator header */
    public String originator;

    /** values of Recipient headers */
    public Set<String> recipients = new TreeSet<String>();

    Reader reqRdr;

    SysiIcalendar ic;

    CalDAVCollection cal;

    /* true if this is an iSchedule request */
    boolean iSchedule;

    /** true if this is a free busy request */
    public boolean freeBusy;

    /** true if this is a web calendar request */
    public boolean webcal;

    /** true if this is a web calendar request with GET + ACCEPT */
    public boolean webcalGetAccept;

    /** true if web service create of entity */
    public boolean entityCreate;

    /** true if this is an exsynch web service request */
    public boolean exsynchws;

    /**
     * @param req
     * @param intf
     * @param resourceUri
     * @throws WebdavException
     */
    public RequestPars(final HttpServletRequest req, final CaldavBWIntf intf,
                       final String resourceUri) throws WebdavException {
      SysIntf sysi = intf.getSysi();

      this.req = req;
      this.resourceUri = resourceUri;

      CalDAVConfig conf = intf.getConfig();

      acceptType = req.getHeader("ACCEPT");

      contentType = req.getContentType();

      if (contentType != null) {
        contentTypePars = contentType.split(";");
      }

      if (conf.getIscheduleURI() != null) {
        iSchedule = conf.getIscheduleURI().equals(resourceUri);
      }

      if (!iSchedule) {
        if (conf.getFburlServiceURI() != null) {
          freeBusy = conf.getFburlServiceURI().equals(resourceUri);
        }

        if (conf.getWebcalServiceURI() != null) {
          webcal = conf.getWebcalServiceURI().equals(resourceUri);
        }

        if (!freeBusy && !webcal) {
          if (intf.getConfig().getCalWS()) {
            // POST of entity for create?
            if ("create".equals(req.getParameter("action"))) {
              entityCreate = true;
            }
          } else if (conf.getExsynchWsURI() != null) {
            exsynchws = conf.getExsynchWsURI().equals(resourceUri);
          }
        }
      } else {
        /* Expect originator and recipient headers */
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
      }

      if (!freeBusy && !webcal && !entityCreate && !exsynchws) {
        try {
          reqRdr = req.getReader();
        } catch (Throwable t) {
          throw new WebdavException(t);
        }
      }
    }

    /**
     * @param val
     */
    public void setContentType(final String val) {
      contentType = val;
    }

    /* We seem to be getting both absolute and relative principals as well as mailto
     * forms of calendar user.
     *
     * If we get an absolute principal - turn it into a relative
     */
    private String adjustPrincipal(final String val,
                                   final SysIntf sysi) throws WebdavException {
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

  @Override
  public void doMethod(final HttpServletRequest req,
                       final HttpServletResponse resp) throws WebdavException {
    if (debug) {
      trace("PostMethod: doMethod");
    }

    CaldavBWIntf intf = (CaldavBWIntf)getNsIntf();

    RequestPars pars = new RequestPars(req, intf, getResourceUri(req));

    if (pars.entityCreate) {
      /* Web Service create */
      doEntityCreate(intf, pars, resp);
      return;
    }

    if (pars.exsynchws) {
      new ExsynchwsHandler(intf).processPost(req, resp, pars);

      return;
    }

    if (!pars.iSchedule) {
      if (intf.getConfig().getCalWS()) {
        doWsQuery(intf, pars, resp);

        return;
      }

      // Standard CalDAV scheduling
      doSchedule(intf, pars, resp);
      return;
    }

    /* We have a potential incoming iSchedule request.
     *
     * NOTE: Leaving this enabled could be a security risk
     */

    try {
      xml.addNs(new NameSpace(IscheduleTags.namespace, "IS"), true);
    } catch (Throwable t) {
      throw new WebdavException(t);
    }

    if (intf.getSysi().getPrincipal() == null) {
      intf.reAuth(req, "realtime01");
    }

    doISchedule(intf, pars, resp);
  }

  /** Handle entity creation for the web service.
   *
   * @param intf
   * @param pars
   * @param resp
   * @throws WebdavException
   */
  public void doEntityCreate(final CaldavBWIntf intf,
                             final RequestPars pars,
                             final HttpServletResponse resp) throws WebdavException {
    intf.putContent(pars.req, resp, true, true, null);
  }

  private void doWsQuery(final CaldavBWIntf intf,
                         final RequestPars pars,
                         final HttpServletResponse resp) throws WebdavException {
    if (!pars.contentTypePars[0].equals("text/xml")) {
      resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
      return;
    }

    /* We should parse the query to see what we got
     * For the moment just hand it over to REPORT
     */

    CaldavReportMethod method = new CaldavReportMethod();
    method.init(intf, debug, true);
    method.doMethod(pars.req, resp);
  }

  /** Handle a scheduling action. The Only non-iSchedule regular action we see
   * this way should be freebusy requests posted at the authenticated user Outbox.
   *
   * @param intf
   * @param pars
   * @param resp
   * @throws WebdavException
   */
  public void doSchedule(final CaldavBWIntf intf,
                         final RequestPars pars,
                         final HttpServletResponse resp) throws WebdavException {
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

      WebdavNsNode node = intf.getNode(pars.resourceUri,
                                       WebdavNsIntf.existanceMust,
                                       WebdavNsIntf.nodeTypeCollection);

      if (node == null) {
        resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
        return;
      }

      /* (CALDAV:supported-collection) */
      if (!(node instanceof CaldavCalNode)) {
        throw new WebdavException(HttpServletResponse.SC_FORBIDDEN);
      }

      /* Don't deref - this should be targetted at a real outbox */
      pars.cal = (CalDAVCollection)node.getCollection(false);

      if (pars.cal.getCalType() != CalDAVCollection.calTypeOutbox) {
        if (debug) {
          debugMsg("Not targetted at Outbox");
        }
        throw new WebdavException(HttpServletResponse.SC_METHOD_NOT_ALLOWED,
        "Not targetted at Outbox");
      }

      /* (CALDAV:supported-calendar-data) */
      if (!pars.contentTypePars[0].equals("text/calendar")) {
        if (debug) {
          debugMsg("Bad content type: " + pars.contentType);
        }
        throw new WebdavForbidden(CaldavTags.supportedCalendarData,
                                  "Bad content type: " + pars.contentType);
      }

      /* (CALDAV:valid-calendar-data) -- later */
      /* (CALDAV:valid-scheduling-message) -- later */

      /* (CALDAV:organizer-allowed) -- later */

      pars.ic = intf.getSysi().fromIcal(pars.cal, pars.reqRdr,
                                        pars.contentTypePars[0],
                                        IcalResultType.OneComponent);

      /* (CALDAV:valid-calendar-data) -- checjed in fromIcal */

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
      if (pars.ic.requestMethodType()) {
        Organizer organizer = pars.ic.getOrganizer();

        if (organizer == null) {
          throw new WebdavForbidden(CaldavTags.organizerAllowed,
          "No access for scheduling");
        }

        /* See if it's a valid calendar user. */
        String cn = organizer.getOrganizerUri();
        organizer.setOrganizerUri(sysi.getUrlHandler().unprefix(cn));
        CalPrincipalInfo organizerInfo = sysi.getCalPrincipalInfo(sysi.caladdrToPrincipal(cn));

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

        /* This must be targeted at the organizers outbox. */
        if (!pars.resourceUri.equals(organizerInfo.outboxPath)) {
          throw new WebdavForbidden(CaldavTags.organizerAllowed,
                                    "No access for scheduling");
        }
      } else {
        /* This must have only one attendee - request must be targeted at attendees outbox*/
      }

      if (pars.ic.getComponentType() == IcalComponentType.freebusy) {
        handleFreeBusy(sysi, pars, resp);
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

  /** Handle an iSchedule action
   *
   * @param intf
   * @param pars
   * @param resp
   * @throws WebdavException
   */
  public void doISchedule(final CaldavBWIntf intf,
                          final RequestPars pars,
                          final HttpServletResponse resp) throws WebdavException {
    SysIntf sysi = intf.getSysi();

    try {
      /* Preconditions:
        (ISCHED:supported-calendar-data):
               The resource submitted in the POST request MUST be a supported
               media type (i.e., text/calendar) for scheduling or free-busy messages;
        (ISCHED:originator-specified): The POST request MUST include a valid
                Originator request header specifying a calendar user address of
                the currently authenticated user;
        (ISCHED:recipient-specified): The POST request MUST include one or more
                valid Recipient request headers specifying the calendar user
                address of users to whom the scheduling message will be delivered.
        (ISCHED:valid-calendar-data): The resource submitted in the POST request
                MUST be valid data for the media type being specified (i.e.,
                valid iCalendar object) ;

        (CALDAV:supported-collection):
               The Request-URI MUST identify the location of a scheduling Outbox collection;
        (CALDAV:valid-scheduling-message): The resource submitted in the POST
                request MUST obey all restrictions specified for the POST request
                (e.g., scheduling message follows the restriction of iTIP);
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
      */

      /* (ISCHED:supported-calendar-data) */
      if (!pars.contentTypePars[0].equals("text/calendar") &&
          !pars.contentTypePars[0].equals(XcalTags.mimetype)) {
        if (debug) {
          debugMsg("Bad content type: " + pars.contentType);
        }
        throw new WebdavForbidden(IscheduleTags.supportedCalendarDataType,
                                  "Bad content type: " + pars.contentType);
      }

      /* (CALDAV:valid-calendar-data) -- later */
      /* (CALDAV:valid-scheduling-message) -- later */

      /* (ISCHED:originator-specified)
       *  */
      if (pars.originator == null) {
        if (debug) {
          debugMsg("No originator");
        }
        throw new WebdavForbidden(IscheduleTags.originatorSpecified,
                                  "No originator");
      }

      /* (ISCHED:recipient-specified) */
      if (pars.recipients.isEmpty()) {
        if (debug) {
          debugMsg("No recipient(s)");
        }
        throw new WebdavForbidden(IscheduleTags.recipientSpecified,
                                  "No recipient(s)");
      }

      pars.ic = sysi.fromIcal(pars.cal, pars.reqRdr,
                              pars.contentTypePars[0],
                              IcalResultType.OneComponent);

      /* (ISCHED:valid-calendar-data) -- checked in fromIcal */

      if (!pars.ic.validItipMethodType()) {
        if (debug) {
          debugMsg("Bad method: " + String.valueOf(pars.ic.getMethodType()));
        }
        throw new WebdavForbidden(IscheduleTags.validCalendarData, "Bad METHOD");
      }

      /* Do the stuff we deferred above */

      /* (CALDAV:valid-scheduling-message) -- later */
      IcalComponentType ctype = pars.ic.getComponentType();

      if (ctype == IcalComponentType.event) {
        handleEvent(sysi, pars, resp);
      } else if (ctype == IcalComponentType.freebusy) {
        handleFreeBusy(sysi, pars, resp);
      } else {
        if (debug) {
          debugMsg("Unsupported component type: " + ctype);
        }
        throw new WebdavForbidden("org.bedework.caldav.unsupported.component " +
                                  ctype);
      }

      flush();
    } catch (WebdavException we) {
      throw we;
    } catch (Throwable t) {
      throw new WebdavException(t);
    }
  }

  /** Only for iSchedule - handle incoming event.
   *
   * @param intf
   * @param pars
   * @param resp
   * @throws WebdavException
   */
  private void handleEvent(final SysIntf intf,
                           final RequestPars pars,
                           final HttpServletResponse resp) throws WebdavException {
    CalDAVEvent ev = pars.ic.getEvent();

    if (pars.recipients != null) {
      for (String r: pars.recipients) {
        ev.addRecipient(r);
      }
    }

    ev.setOriginator(pars.originator);
    ev.setScheduleMethod(pars.ic.getMethodType());

    Collection<SchedRecipientResult> srrs = intf.schedule(ev);

    resp.setStatus(HttpServletResponse.SC_OK);
    resp.setContentType("text/xml; charset=UTF-8");

    startEmit(resp);

    openTag(IscheduleTags.scheduleResponse);

    for (SchedRecipientResult srr: srrs) {
      openTag(IscheduleTags.response);

      openTag(IscheduleTags.recipient);
      property(WebdavTags.href, srr.recipient);
      closeTag(IscheduleTags.recipient);

      setReqstat(srr.status, true);
      closeTag(IscheduleTags.response);
    }

    closeTag(IscheduleTags.scheduleResponse);
  }

  private void handleFreeBusy(final SysIntf intf,
                              final RequestPars pars,
                              final HttpServletResponse resp) throws WebdavException {
    CalDAVEvent ev = pars.ic.getEvent();

    ev.setRecipients(pars.recipients);
    ev.setOriginator(pars.originator);
    ev.setScheduleMethod(pars.ic.getMethodType());

    Collection<SchedRecipientResult> srrs = intf.requestFreeBusy(ev);

    resp.setStatus(HttpServletResponse.SC_OK);
    resp.setContentType("text/xml; charset=UTF-8");

    startEmit(resp);

    QName sresponseTag;
    QName responseTag;
    QName recipientTag;
    QName calendarDataTag;

    if (pars.iSchedule) {
      sresponseTag = IscheduleTags.scheduleResponse;
      responseTag = IscheduleTags.response;
      recipientTag = IscheduleTags.recipient;
      calendarDataTag = IscheduleTags.calendarData;
    } else {
      sresponseTag = CaldavTags.scheduleResponse;
      responseTag = CaldavTags.response;
      recipientTag = CaldavTags.recipient;
      calendarDataTag = CaldavTags.calendarData;
    }

    openTag(sresponseTag);

    for (SchedRecipientResult srr: srrs) {
      openTag(responseTag);
      openTag(recipientTag);
      property(WebdavTags.href, srr.recipient);
      closeTag(recipientTag);

      setReqstat(srr.status, pars.iSchedule);

      CalDAVEvent rfb = srr.freeBusy;
      if (rfb != null) {
        rfb.setOrganizer(pars.ic.getOrganizer());

        try {
          cdataProperty(calendarDataTag,
                        rfb.toIcalString(ScheduleMethods.methodTypeReply));
        } catch (Throwable t) {
          if (debug) {
            error(t);
          }
          throw new WebdavException(t);
        }
      }

      closeTag(responseTag);
    }

    closeTag(sresponseTag);
  }

  private void setReqstat(final int status,
                          final boolean iSchedule) throws WebdavException {
    String reqstat;

    if (status == SchedRecipientResult.scheduleDeferred) {
      reqstat = IcalDefs.requestStatusDeferred;
    } else if (status == SchedRecipientResult.scheduleNoAccess) {
      if (iSchedule) {
        propertyTagVal(WebdavTags.error, IscheduleTags.recipientPermissions);
      } else {
        propertyTagVal(WebdavTags.error, CaldavTags.recipientPermissions);
      }
      reqstat = IcalDefs.requestStatusNoAccess;
    } else {
      reqstat = IcalDefs.requestStatusOK;
    }

    if (iSchedule) {
      property(IscheduleTags.requestStatus, reqstat);
    } else {
      property(CaldavTags.requestStatus, reqstat);
    }
  }
}
