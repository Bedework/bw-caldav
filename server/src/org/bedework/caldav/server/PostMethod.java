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

import org.bedework.caldav.server.soap.calws.CalwsHandler;
import org.bedework.caldav.server.soap.synch.SynchwsHandler;
import org.bedework.caldav.server.sysinterface.CalPrincipalInfo;
import org.bedework.caldav.server.sysinterface.SysIntf;
import org.bedework.caldav.server.sysinterface.SysIntf.IcalResultType;
import org.bedework.caldav.server.sysinterface.SysIntf.SchedRecipientResult;
import org.bedework.caldav.util.CalDAVConfig;
import org.bedework.caldav.util.sharing.InviteReplyType;
import org.bedework.caldav.util.sharing.ShareType;
import org.bedework.caldav.util.sharing.SharedAsType;
import org.bedework.caldav.util.sharing.parse.Parser;

import edu.rpi.cct.webdav.servlet.common.Headers.IfHeaders;
import edu.rpi.cct.webdav.servlet.common.MethodBase;
import edu.rpi.cct.webdav.servlet.shared.WebdavBadRequest;
import edu.rpi.cct.webdav.servlet.shared.WebdavException;
import edu.rpi.cct.webdav.servlet.shared.WebdavForbidden;
import edu.rpi.cct.webdav.servlet.shared.WebdavNsIntf;
import edu.rpi.cct.webdav.servlet.shared.WebdavNsNode;
import edu.rpi.cmt.calendar.IcalDefs;
import edu.rpi.cmt.calendar.IcalDefs.IcalComponentType;
import edu.rpi.cmt.calendar.ScheduleMethods;
import edu.rpi.sss.util.xml.XmlEmit;
import edu.rpi.sss.util.xml.XmlEmit.NameSpace;
import edu.rpi.sss.util.xml.XmlUtil;
import edu.rpi.sss.util.xml.tagdefs.AppleServerTags;
import edu.rpi.sss.util.xml.tagdefs.CaldavTags;
import edu.rpi.sss.util.xml.tagdefs.IscheduleTags;
import edu.rpi.sss.util.xml.tagdefs.WebdavTags;
import edu.rpi.sss.util.xml.tagdefs.XcalTags;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import java.io.Reader;
import java.util.Collection;
import java.util.Enumeration;
import java.util.Set;
import java.util.TreeSet;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

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

    /** true if this is a CalDAV share request */
    public boolean share;

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

    /** true if this is an synch web service request */
    public boolean synchws;

    /** true if this is a calws soap web service request */
    public boolean calwsSoap;

    /** Set if the content type is xml */
    public Document xmlDoc;

    private boolean getTheReader = true;

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

      testRequest: {
        if (conf.getIscheduleURI() != null) {
          iSchedule = conf.getIscheduleURI().equals(resourceUri);
        }

        if (iSchedule) {
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

          break testRequest;
        }

        if (conf.getFburlServiceURI() != null) {
          freeBusy = conf.getFburlServiceURI().equals(resourceUri);
          if (freeBusy) {
            getTheReader = false;
            break testRequest;
          }
        }

        if (conf.getWebcalServiceURI() != null) {
          webcal = conf.getWebcalServiceURI().equals(resourceUri);
          if (webcal) {
            getTheReader = false;
            break testRequest;
          }
        }

        // not ischedule or freeBusy or webcal

        if (intf.getConfig().getCalWS()) {
          // POST of entity for create?
          if ("create".equals(req.getParameter("action"))) {
            entityCreate = true;
          }
          break testRequest;
        }

        if (conf.getSynchWsURI() != null) {
          synchws = conf.getSynchWsURI().equals(resourceUri);
          if (synchws) {
            getTheReader = false;
            break testRequest;
          }
        }

        if (conf.getCalSoapWsURI() != null) {
          calwsSoap = conf.getCalSoapWsURI().equals(resourceUri);
          if (calwsSoap) {
            getTheReader = false;
            break testRequest;
          }
        }

        /* Not any of the special URIs - this could be a post aimed at one of
         * our caldav resources.
         */
        if (isAppXml()) {
          try {
            reqRdr = req.getReader();
          } catch (Throwable t) {
            throw new WebdavException(t);
          }

          xmlDoc = parseXml(reqRdr);
          getTheReader = false;
        }
      } // testRequest

      if (getTheReader) {
        try {
          reqRdr = req.getReader();
        } catch (Throwable t) {
          throw new WebdavException(t);
        }
      }
    }

    /**
     * @param val
     * @return parsed Document
     * @throws WebdavException
     */
    private Document parseXml(final Reader rdr) throws WebdavException{
      if (rdr == null) {
        return null;
      }

      try {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);

        DocumentBuilder builder = factory.newDocumentBuilder();

        return builder.parse(new InputSource(rdr));
      } catch (SAXException e) {
        throw new WebdavBadRequest();
      } catch (Throwable t) {
        throw new WebdavException(t);
      }
    }

    /**
     * @return true if we have an xml content
     */
    public boolean isAppXml() {
      if (contentTypePars == null) {
        return false;
      }

      return contentTypePars[0].equals("application/xml") ||
          contentTypePars[0].equals("text/xml");

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

    if (pars.synchws) {
      new SynchwsHandler(intf).processPost(req, resp, pars);
      return;
    }

    if (pars.calwsSoap) {
      new CalwsHandler(intf).processPost(req, resp, pars);
      return;
    }

    if (!pars.iSchedule) {
      if (intf.getConfig().getCalWS()) {
        doWsQuery(intf, pars, resp);

        return;
      }

      // Standard CalDAV
      doCalDav(intf, pars, resp);
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
      intf.reAuth(req, "isched01", true);
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
    IfHeaders ih = new IfHeaders();
    ih.create = true;
    intf.putContent(pars.req, resp, true, ih);
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
    method.init(intf, true);
    method.doMethod(pars.req, resp);
  }

  private void doCalDav(final CaldavBWIntf intf,
                        final RequestPars pars,
                        final HttpServletResponse resp) throws WebdavException {
    if (!pars.isAppXml()) {
      // Assume scheduling

      doSchedule(intf, pars, resp);
    }

    WebdavNsNode node = intf.getNode(pars.resourceUri,
                                     WebdavNsIntf.existanceMust,
                                     WebdavNsIntf.nodeTypeCollection);

    if (node == null) {
      resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
      return;
    }

    if (!node.isCollection()) {
      throw new WebdavForbidden("Not a collection");
    }

    CaldavCalNode calnode = (CaldavCalNode)node;
    CalDAVCollection col = (CalDAVCollection)calnode.getCollection(false);

    Element root = pars.xmlDoc.getDocumentElement();

    SysIntf sysi = intf.getSysi();
    Parser parse = new Parser();

    if (XmlUtil.nodeMatches(root, AppleServerTags.inviteReply)) {
      InviteReplyType reply = parse.parseInviteReply(root);
      reply.setHostUrl(intf.getUri(reply.getHostUrl()));

      String newUri = sysi.sharingReply(col, reply);

      if (newUri == null) {
        // XXX Wrong response
        resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
        return;
      }

      SharedAsType sa = new SharedAsType();

      sa.setHref(newUri);

      resp.setStatus(HttpServletResponse.SC_OK);
      resp.setContentType("text/xml; charset=UTF-8");

      startEmit(resp);
      XmlEmit xml = intf.getXmlEmit();

      try {
        sa.toXml(xml);
      } catch (Throwable t) {
        throw new WebdavException(t);
      }

      return;
    }

    if (XmlUtil.nodeMatches(root, AppleServerTags.share)) {
      ShareType share = parse.parseShare(root);

      sysi.share(col, share);

      return;
    }
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

    WebdavNsNode node = intf.getNode(pars.resourceUri,
                                     WebdavNsIntf.existanceMust,
                                     WebdavNsIntf.nodeTypeCollection);

    if (node == null) {
      resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
      return;
    }

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
                                        IcalResultType.OneComponent,
                                        false);

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
                              IcalResultType.OneComponent,
                              false);

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
