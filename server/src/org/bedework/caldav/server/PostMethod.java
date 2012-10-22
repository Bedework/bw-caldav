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
import org.bedework.caldav.util.sharing.InviteReplyType;
import org.bedework.caldav.util.sharing.ShareResultType;
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
import edu.rpi.cct.webdav.servlet.shared.WebdavStatusCode;
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

import org.apache.james.jdkim.DKIMVerifier;
import org.apache.james.jdkim.IscheduleDKIMVerifier;
import org.apache.james.jdkim.api.BodyHasher;
import org.apache.james.jdkim.api.SignatureRecord;
import org.apache.james.jdkim.exceptions.FailException;
import org.w3c.dom.Element;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.Collection;

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
      /* It's a reply to a sharing invitation. Parse it then doctor the url to
       * leave just the resource path.
       *
       * A successful response is a shared-as xml response giving the url of
       * the new collection in the sharees home.
       */
      InviteReplyType reply = parse.parseInviteReply(root);
      reply.setHostUrl(intf.getUri(reply.getHostUrl()));

      String newUri = sysi.sharingReply(col, reply);

      if (newUri == null) {
        // Appropriate for declined
        resp.setStatus(HttpServletResponse.SC_OK);
        return;
      }

      SharedAsType sa = new SharedAsType(newUri);

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

      ShareResultType sr = sysi.share(col, share);

      if (sr.getBadSharees().isEmpty()) {
        /* No response needed when all OK */
        resp.setStatus(HttpServletResponse.SC_OK);
        return;
      }

      resp.setStatus(WebdavStatusCode.SC_MULTI_STATUS);
      resp.setContentType("text/xml; charset=UTF-8");

      startEmit(resp);
      XmlEmit xml = intf.getXmlEmit();

      try {
        xml.openTag(WebdavTags.multistatus);

        for (String s:sr.getGoodSharees()) {
          xml.openTag(WebdavTags.response);
          xml.property(WebdavTags.href, s);
          addStatus(HttpServletResponse.SC_OK, null);

          xml.closeTag(WebdavTags.response);
        }

        for (String s:sr.getBadSharees()) {
          xml.openTag(WebdavTags.response);
          xml.property(WebdavTags.href, s);
          addStatus(HttpServletResponse.SC_FORBIDDEN, null);

          xml.closeTag(WebdavTags.response);
        }

        xml.closeTag(WebdavTags.multistatus);
      } catch (Throwable t) {
        throw new WebdavException(t);
      }

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
        throw new WebdavForbidden(IscheduleTags.invalidCalendarDataType,
                                  "Bad content type: " + pars.contentType);
      }

      /* (CALDAV:valid-calendar-data) -- later */
      /* (CALDAV:valid-scheduling-message) -- later */

      /* (ISCHED:originator-specified)
       *  */
      if (pars.ischedRequest.getOriginator() == null) {
        if (debug) {
          debugMsg("No originator");
        }
        throw new WebdavForbidden(IscheduleTags.originatorMissing,
                                  "No originator");
      }

      /* (ISCHED:recipient-specified) */
      if (pars.ischedRequest.getRecipients().isEmpty()) {
        if (debug) {
          debugMsg("No recipient(s)");
        }
        throw new WebdavForbidden(IscheduleTags.recipientMissing,
                                  "No recipient(s)");
      }

      if (pars.ischedRequest.getIScheduleMessageId() == null) {
        if (debug) {
          debugMsg("No message id");
        }
        throw new WebdavForbidden(IscheduleTags.recipientMissing,
                                  "No message id");
      }

      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      streamCopy(pars.req.getInputStream(), baos);

      validateHost(intf, pars, resp,
                   new ByteArrayInputStream(baos.toByteArray()));

      pars.ic = sysi.fromIcal(pars.cal,
                              new InputStreamReader(new ByteArrayInputStream(baos.toByteArray())),
                              pars.contentTypePars[0],
                              IcalResultType.OneComponent,
                              false);

      /* (ISCHED:valid-calendar-data) -- checked in fromIcal */

      if (!pars.ic.validItipMethodType()) {
        if (debug) {
          debugMsg("Bad method: " + String.valueOf(pars.ic.getMethodType()));
        }
        throw new WebdavForbidden(IscheduleTags.invalidCalendarData, "Bad METHOD");
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

  private void validateHost(final CaldavBWIntf intf,
                            final RequestPars pars,
                            final HttpServletResponse resp,
                            final InputStream content) throws WebdavException {
    SignatureRecord sig = pars.ischedRequest.getDkimSignature();

    if (sig == null) {
      // Check to see if we allow this host -
      return;
    }

    try {
      /* Do DKIM validation */
      DKIMVerifier verifier = new IscheduleDKIMVerifier();
      BodyHasher bh = verifier.newBodyHasher(pars.ischedRequest);

      if (bh != null) {
        OutputStream os = bh.getOutputStream();
        streamCopy(content, os);
      }

      verifier.verify(bh);
    } catch (IOException e) {
      throw new WebdavException(e);
    } catch (FailException e) {
      if (debug) {
        error(e);
      }
      throw new WebdavForbidden(IscheduleTags.verificationFailed);
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

    if (pars.ischedRequest.getRecipients() != null) {
      for (String r: pars.ischedRequest.getRecipients()) {
        ev.addRecipient(r);
      }
    }

    validateOriginator(pars, ev);
    ev.setScheduleMethod(pars.ic.getMethodType());

    Collection<SchedRecipientResult> srrs = intf.schedule(ev);

    resp.setStatus(HttpServletResponse.SC_OK);
    resp.setContentType("text/xml; charset=UTF-8");

    startEmit(resp);

    openTag(IscheduleTags.scheduleResponse);

    for (SchedRecipientResult srr: srrs) {
      openTag(IscheduleTags.response);

      property(IscheduleTags.recipient, srr.recipient);

      setReqstat(srr.status, true);
      closeTag(IscheduleTags.response);
    }

    closeTag(IscheduleTags.scheduleResponse);
  }

  private void handleFreeBusy(final SysIntf intf,
                              final RequestPars pars,
                              final HttpServletResponse resp) throws WebdavException {
    CalDAVEvent<?> ev = pars.ic.getEvent();

    ev.setRecipients(pars.ischedRequest.getRecipients());
    validateOriginator(pars, ev);
    ev.setScheduleMethod(pars.ic.getMethodType());

    Collection<SchedRecipientResult> srrs = intf.requestFreeBusy(ev, true);

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

      if (pars.iSchedule) {
        property(recipientTag, srr.recipient);
      } else {
        openTag(recipientTag);
        property(WebdavTags.href, srr.recipient);
        closeTag(recipientTag);
      }

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

  private void validateOriginator(final RequestPars pars,
                                  final CalDAVEvent ev) throws WebdavException {
    String origUrl = pars.ischedRequest.getOriginator();
    Organizer org = ev.getOrganizer();

    if (org == null) {
      throw new WebdavBadRequest(IscheduleTags.invalidCalendarData,
                                 "Missing organizer");
    }

    if (!origUrl.equals(org.getOrganizerUri())) {
      throw new WebdavBadRequest(IscheduleTags.invalidCalendarData,
          "Organizer/originator mismatch");
    }

    ev.setOriginator(origUrl);

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
    } else if (status == SchedRecipientResult.scheduleUnprocessed) {
      reqstat = IcalDefs.requestStatusInvalidUser;
    } else if (status == SchedRecipientResult.scheduleError) {
      reqstat = IcalDefs.requestStatusUnavailable;
    } else {
      reqstat = IcalDefs.requestStatusOK;
    }

    if (iSchedule) {
      property(IscheduleTags.requestStatus, reqstat);
    } else {
      property(CaldavTags.requestStatus, reqstat);
    }
  }

  private void streamCopy(final InputStream in, final OutputStream out)
          throws IOException {
      byte[] buffer = new byte[2048];
      int read;
      while ((read = in.read(buffer)) > 0) {
          out.write(buffer, 0, read);
      }
      in.close();
      out.close();
  }
}
