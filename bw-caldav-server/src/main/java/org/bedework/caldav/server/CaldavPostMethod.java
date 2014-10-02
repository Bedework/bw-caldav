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
import org.bedework.util.calendar.IcalDefs;
import org.bedework.util.calendar.IcalDefs.IcalComponentType;
import org.bedework.util.calendar.ScheduleMethods;
import org.bedework.util.xml.XmlEmit;
import org.bedework.util.xml.XmlEmit.NameSpace;
import org.bedework.util.xml.XmlUtil;
import org.bedework.util.xml.tagdefs.AppleServerTags;
import org.bedework.util.xml.tagdefs.CaldavTags;
import org.bedework.util.xml.tagdefs.IscheduleTags;
import org.bedework.util.xml.tagdefs.WebdavTags;
import org.bedework.util.xml.tagdefs.XcalTags;
import org.bedework.webdav.servlet.common.PostMethod;
import org.bedework.webdav.servlet.shared.WebdavBadRequest;
import org.bedework.webdav.servlet.shared.WebdavException;
import org.bedework.webdav.servlet.shared.WebdavForbidden;
import org.bedework.webdav.servlet.shared.WebdavNsIntf;
import org.bedework.webdav.servlet.shared.WebdavNsNode;
import org.bedework.webdav.servlet.shared.WebdavStatusCode;

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
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.namespace.QName;

/** Class called to handle POST for CalDAV scheduling.
 *
 *   @author Mike Douglass   douglm - rpi.edu
 */
public class CaldavPostMethod extends PostMethod {
  @Override
  public void init() {
  }

  @Override
  public void doMethod(final HttpServletRequest req,
                       final HttpServletResponse resp) throws WebdavException {
    if (debug) {
      trace("PostMethod: doMethod");
    }

    final CaldavBWIntf intf = (CaldavBWIntf)getNsIntf();

    final RequestPars pars = new RequestPars(req, intf, getResourceUri(req));

    if (pars.isAddMember()) {
      handleAddMember(pars, resp);
      return;
    }

    if (pars.isSynchws()) {
      new SynchwsHandler(intf).processPost(req, resp, pars);
      return;
    }

    if (pars.isCalwsSoap()) {
      new CalwsHandler(intf).processPost(req, resp, pars);
      return;
    }

    if (!pars.isiSchedule()) {
      if (intf.getCalWS()) {
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
    } catch (final Throwable t) {
      throw new WebdavException(t);
    }

    if (intf.getSysi().getPrincipal() == null) {
      intf.reAuth(req, "isched01", true);
    }

    doISchedule(intf, pars, resp);
  }

  private void doWsQuery(final CaldavBWIntf intf,
                         final RequestPars pars,
                         final HttpServletResponse resp) throws WebdavException {
    if (!pars.getContentTypePars()[0].equals("text/xml")) {
      resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
      return;
    }

    /* We should parse the query to see what we got
     * For the moment just hand it over to REPORT
     */

    final CaldavReportMethod method = new CaldavReportMethod();
    method.init(intf, true);
    method.doMethod(pars.getReq(), resp);
  }

  private void doCalDav(final CaldavBWIntf intf,
                        final RequestPars pars,
                        final HttpServletResponse resp) throws WebdavException {
    if (!pars.isAppXml()) {
      // Assume scheduling

      doSchedule(intf, pars, resp);
      return;
    }

    final WebdavNsNode node = intf.getNode(pars.getResourceUri(),
                                           WebdavNsIntf.existanceMust,
                                           WebdavNsIntf.nodeTypeCollection,
                                           false);

    if (node == null) {
      resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
      return;
    }

    if (!node.isCollection()) {
      throw new WebdavForbidden("Not a collection");
    }

    final CaldavCalNode calnode = (CaldavCalNode)node;
    final CalDAVCollection col = (CalDAVCollection)calnode.getCollection(false);

    final Element root = pars.getXmlDoc().getDocumentElement();

    final SysIntf sysi = intf.getSysi();
    final Parser parse = new Parser();

    if (XmlUtil.nodeMatches(root, AppleServerTags.inviteReply)) {
      /* It's a reply to a sharing invitation. Parse it then doctor the url to
       * leave just the resource path.
       *
       * A successful response is a shared-as xml response giving the url of
       * the new collection in the sharees home.
       */
      final InviteReplyType reply = parse.parseInviteReply(root);
      reply.setHostUrl(intf.getUri(reply.getHostUrl()));

      final String newUri = sysi.sharingReply(col, reply);

      if (newUri == null) {
        // Appropriate for declined
        resp.setStatus(HttpServletResponse.SC_OK);
        return;
      }

      final SharedAsType sa = new SharedAsType(newUri);

      resp.setStatus(HttpServletResponse.SC_OK);
      resp.setContentType("text/xml; charset=UTF-8");

      startEmit(resp);
      final XmlEmit xml = intf.getXmlEmit();

      try {
        sa.toXml(xml);
      } catch (final Throwable t) {
        throw new WebdavException(t);
      }

      return;
    }

    if (XmlUtil.nodeMatches(root, AppleServerTags.share)) {
      final ShareType share = parse.parseShare(root);

      final ShareResultType sr = sysi.share(col, share);

      if (sr.getBadSharees().isEmpty()) {
        /* No response needed when all OK */
        resp.setStatus(HttpServletResponse.SC_OK);
        return;
      }

      resp.setStatus(WebdavStatusCode.SC_MULTI_STATUS);
      resp.setContentType("text/xml; charset=UTF-8");

      startEmit(resp);
      final XmlEmit xml = intf.getXmlEmit();

      try {
        xml.openTag(WebdavTags.multistatus);

        for (final String s:sr.getGoodSharees()) {
          xml.openTag(WebdavTags.response);
          xml.property(WebdavTags.href, s);
          addStatus(HttpServletResponse.SC_OK, null);

          xml.closeTag(WebdavTags.response);
        }

        for (final String s:sr.getBadSharees()) {
          xml.openTag(WebdavTags.response);
          xml.property(WebdavTags.href, s);
          addStatus(HttpServletResponse.SC_FORBIDDEN, null);

          xml.closeTag(WebdavTags.response);
        }

        xml.closeTag(WebdavTags.multistatus);
      } catch (final Throwable t) {
        throw new WebdavException(t);
      }
    }
  }

  /** Handle a scheduling action. The Only non-iSchedule regular action we see
   * this way should be freebusy requests posted at the authenticated user Outbox.
   *
   * @param intf the interface
   * @param pars POST parameters
   * @param resp response
   * @throws WebdavException
   */
  public void doSchedule(final CaldavBWIntf intf,
                         final RequestPars pars,
                         final HttpServletResponse resp) throws WebdavException {
    final SysIntf sysi = intf.getSysi();

    final WebdavNsNode node = intf.getNode(pars.getResourceUri(),
                                           WebdavNsIntf.existanceMust,
                                           WebdavNsIntf.nodeTypeCollection,
                                           false);

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
      pars.setCol((CalDAVCollection)node.getCollection(false));

      if (pars.getCol().getCalType() != CalDAVCollection.calTypeOutbox) {
        if (debug) {
          debugMsg("Not targetted at Outbox");
        }
        throw new WebdavException(HttpServletResponse.SC_METHOD_NOT_ALLOWED,
        "Not targetted at Outbox");
      }

      /* (CALDAV:valid-calendar-data) -- later */
      /* (CALDAV:valid-scheduling-message) -- later */

      /* (CALDAV:organizer-allowed) -- later */

      pars.setIcalendar(intf.getSysi().fromIcal(pars.getCol(),
                                                pars.getReader(),
                                                pars.getContentTypePars()[0],
                                                IcalResultType.OneComponent,
                                                false));

      /* (CALDAV:valid-calendar-data) -- checjed in fromIcal */

      if (!pars.getIcalendar().validItipMethodType()) {
        if (debug) {
          debugMsg("Bad method: " +
                           String.valueOf(pars.getIcalendar().getMethodType()));
        }
        throw new WebdavForbidden(CaldavTags.validCalendarData, "Bad METHOD");
      }

      /* Do the stuff we deferred above */

      /* (CALDAV:valid-scheduling-message) -- later */

      /* (CALDAV:organizer-allowed) */
      /* There must be a valid organizer with an outbox for outgoing. */
      if (pars.getIcalendar().requestMethodType()) {
        final Organizer organizer = pars.getIcalendar().getOrganizer();

        if (organizer == null) {
          throw new WebdavForbidden(CaldavTags.organizerAllowed,
                                    "No access for scheduling");
        }

        /* See if it's a valid calendar user. */
        final String cn = organizer.getOrganizerUri();
        organizer.setOrganizerUri(sysi.getUrlHandler().unprefix(cn));
        final CalPrincipalInfo organizerInfo =
                sysi.getCalPrincipalInfo(sysi.caladdrToPrincipal(cn));

        if (debug) {
          if (organizerInfo == null) {
            trace("organizerInfo for " + cn + " is NULL");
          } else {
            trace("organizer cn = " + cn +
                  ", resourceUri = " + pars.getResourceUri() +
                  ", outBoxPath = " + organizerInfo.outboxPath);
          }
        }

        if (organizerInfo == null) {
          throw new WebdavForbidden(CaldavTags.organizerAllowed,
                                    "No access for scheduling");
        }

        /* This must be targeted at the organizers outbox. */
        if (!pars.getResourceUri().equals(organizerInfo.outboxPath)) {
          throw new WebdavForbidden(CaldavTags.organizerAllowed,
                                    "No access for scheduling");
        }
        //      } else {
        //      /* This must have only one attendee - request must be targeted at attendees outbox*/
      }

      if (pars.getIcalendar().getComponentType() == IcalComponentType.freebusy) {
        handleFreeBusy(sysi, pars, resp);
      } else {
        if (debug) {
          debugMsg("Unsupported component type: " +
                           pars.getIcalendar().getComponentType());
        }
        throw new WebdavForbidden("org.bedework.caldav.unsupported.component " +
                                  pars.getIcalendar().getComponentType());
      }

      flush();
    } catch (final WebdavException we) {
      throw we;
    } catch (final Throwable t) {
      throw new WebdavException(t);
    }
  }

  /** Handle an iSchedule action
   *
   * @param intf the interface
   * @param pars POST parameters
   * @param resp response
   * @throws WebdavException
   */
  public void doISchedule(final CaldavBWIntf intf,
                          final RequestPars pars,
                          final HttpServletResponse resp) throws WebdavException {
    final SysIntf sysi = intf.getSysi();

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
      if (!pars.getContentTypePars()[0].equals("text/calendar") &&
          !pars.getContentTypePars()[0].equals(XcalTags.mimetype)) {
        if (debug) {
          debugMsg("Bad content type: " + pars.getContentType());
        }
        throw new WebdavForbidden(IscheduleTags.invalidCalendarDataType,
                                  "Bad content type: " + pars.getContentType());
      }

      /* (CALDAV:valid-calendar-data) -- later */
      /* (CALDAV:valid-scheduling-message) -- later */

      /* (ISCHED:originator-specified)
       *  */

      final IscheduleIn isi = pars.getIschedRequest();
      if (isi.getOriginator() == null) {
        if (debug) {
          debugMsg("No originator");
        }
        throw new WebdavForbidden(IscheduleTags.originatorMissing,
                                  "No originator");
      }

      /* (ISCHED:recipient-specified) */
      if (isi.getRecipients().isEmpty()) {
        if (debug) {
          debugMsg("No recipient(s)");
        }
        throw new WebdavForbidden(IscheduleTags.recipientMissing,
                                  "No recipient(s)");
      }

      if (isi.getIScheduleMessageId() == null) {
        if (debug) {
          debugMsg("No message id");
        }
        throw new WebdavForbidden(IscheduleTags.recipientMissing,
                                  "No message id");
      }

      final ByteArrayOutputStream baos = new ByteArrayOutputStream();
      streamCopy(pars.getReq().getInputStream(), baos);

      validateHost(pars,
                   new ByteArrayInputStream(baos.toByteArray()));

      pars.setIcalendar(sysi.fromIcal(pars.getCol(),
                                      new InputStreamReader(
                                              new ByteArrayInputStream(
                                                      baos.toByteArray())),
                                      pars.getContentTypePars()[0],
                                      IcalResultType.OneComponent,
                                      false));

      /* (ISCHED:valid-calendar-data) -- checked in fromIcal */

      if (!pars.getIcalendar().validItipMethodType()) {
        if (debug) {
          debugMsg("Bad method: " +
                           String.valueOf(pars.getIcalendar().getMethodType()));
        }
        throw new WebdavForbidden(IscheduleTags.invalidCalendarData, "Bad METHOD");
      }

      /* Do the stuff we deferred above */

      /* (CALDAV:valid-scheduling-message) -- later */
      final IcalComponentType ctype = pars.getIcalendar().getComponentType();

      if ((ctype == IcalComponentType.event) ||
          (ctype == IcalComponentType.vpoll)) {
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
    } catch (final WebdavException we) {
      throw we;
    } catch (final Throwable t) {
      throw new WebdavException(t);
    }
  }

  private void validateHost(final RequestPars pars,
                            final InputStream content) throws WebdavException {
    final IscheduleIn isi = pars.getIschedRequest();
    final SignatureRecord sig = isi.getDkimSignature();

    if (sig == null) {
      // Check to see if we allow this host -
      warn("Unchecked host - no dkim signature:");
      return;
    }

    try {
      /* Do DKIM validation */
      final DKIMVerifier verifier = new IscheduleDKIMVerifier();
      final BodyHasher bh = verifier.newBodyHasher(isi);

      if (bh != null) {
        final OutputStream os = bh.getOutputStream();
        streamCopy(content, os);
      }

      verifier.verify(bh);
    } catch (final IOException e) {
      throw new WebdavException(e);
    } catch (final FailException e) {
      if (debug) {
        error(e);
      }
      throw new WebdavForbidden(IscheduleTags.verificationFailed);
    }
  }

  /** Only for iSchedule - handle incoming event.
   *
   * @param intf the interface
   * @param pars POST parameters
   * @param resp response
   * @throws WebdavException
   */
  private void handleEvent(final SysIntf intf,
                           final RequestPars pars,
                           final HttpServletResponse resp) throws WebdavException {
    final CalDAVEvent ev = pars.getIcalendar().getEvent();

    if (pars.getIschedRequest().getRecipients() != null) {
      for (final String r: pars.getIschedRequest().getRecipients()) {
        ev.addRecipient(r);
      }
    }

    ev.setScheduleMethod(pars.getIcalendar().getMethodType());
    validateOriginator(pars, ev);

    final Collection<SchedRecipientResult> srrs = intf.schedule(ev);

    resp.setStatus(HttpServletResponse.SC_OK);
    resp.setContentType("text/xml; charset=UTF-8");
    resp.addHeader("iSchedule-Capabilities",
                   String.valueOf(RequestPars.iScheduleSerialNumber));

    startEmit(resp);

    openTag(IscheduleTags.scheduleResponse);

    for (final SchedRecipientResult srr: srrs) {
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
    final CalDAVEvent<?> ev = pars.getIcalendar().getEvent();

    if (pars.isiSchedule()) {
      ev.setRecipients(pars.getIschedRequest().getRecipients());
    } else {
      /* Set recipients from attendees */
      ev.setRecipients(ev.getAttendeeUris());
    }

    ev.setScheduleMethod(pars.getIcalendar().getMethodType());
    validateOriginator(pars, ev);

    final Collection<SchedRecipientResult> srrs = intf.requestFreeBusy(ev, true);

    resp.setStatus(HttpServletResponse.SC_OK);
    resp.setContentType("application/xml; charset=UTF-8");
    resp.addHeader("iSchedule-Capabilities",
                   String.valueOf(RequestPars.iScheduleSerialNumber));

    startEmit(resp);

    final QName sresponseTag;
    final QName responseTag;
    final QName recipientTag;
    final QName calendarDataTag;

    if (pars.isiSchedule()) {
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

    for (final SchedRecipientResult srr: srrs) {
      openTag(responseTag);

      if (pars.isiSchedule()) {
        property(recipientTag, srr.recipient);
      } else {
        openTag(recipientTag);
        property(WebdavTags.href, srr.recipient);
        closeTag(recipientTag);
      }

      setReqstat(srr.status, pars.isiSchedule());

      final CalDAVEvent rfb = srr.freeBusy;
      if (rfb != null) {
        rfb.setOrganizer(pars.getIcalendar().getOrganizer());

        try {
          cdataProperty(calendarDataTag,
                        "content-type",
                        pars.getContentType(),
                        rfb.toIcalString(ScheduleMethods.methodTypeReply,
                                         pars.getContentTypePars()[0]));
        } catch (final Throwable t) {
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

  /**
   *
   +----------------+----------------------------------+
   | Method         | Originator Requirement           |
   +----------------+----------------------------------+
   | PUBLISH        | None                             |
   | REQUEST        | MUST match ORGANIZER or ATTENDEE |
   | REPLY          | MUST match ATTENDEE              |
   | ADD            | MUST match ORGANIZER             |
   | CANCEL         | MUST match ORGANIZER             |
   | REFRESH        | None                             |
   | COUNTER        | MUST match ATTENDEE              |
   | DECLINECOUNTER | MUST match ORGANIZER             |
   +----------------+----------------------------------+
   * @param pars for request
   * @param ev object to be validated
   * @throws WebdavException
   */
  private void validateOriginator(final RequestPars pars,
                                  final CalDAVEvent ev) throws WebdavException {
    final int meth = ev.getScheduleMethod();

    if (meth == ScheduleMethods.methodTypePublish) {
      return;
    }

    final boolean matchOrganizer =
            (meth == ScheduleMethods.methodTypeAdd) ||
                    (meth == ScheduleMethods.methodTypeCancel) ||
                    (meth == ScheduleMethods.methodTypePollStatus) ||
                    (meth == ScheduleMethods.methodTypeDeclineCounter);

    final boolean request = meth == ScheduleMethods.methodTypeRequest;

    final Organizer org = ev.getOrganizer();

    if (org == null) {
      throw new WebdavBadRequest(IscheduleTags.invalidCalendarData,
                                 "Missing organizer");
    }

    if (pars.isiSchedule()) {
      final String origUrl = pars.getIschedRequest().getOriginator();
      boolean matchAttendee = true;

      if (matchOrganizer || request) {
        if (!origUrl.equals(org.getOrganizerUri())) {
          if (!request) {
            throw new WebdavBadRequest(IscheduleTags.invalidCalendarData,
                                       "Organizer/originator mismatch");
          }
        } else {
          matchAttendee = false;
        }
      }

      ev.setOriginator(origUrl);

      if (matchAttendee){
        @SuppressWarnings("unchecked") final Set<String> attUris = ev.getAttendeeUris();

        if (attUris.size() != 1) {
          throw new WebdavBadRequest(IscheduleTags.invalidCalendarData,
                                     "Attendee/originator mismatch");
        }

        if (!attUris.contains(origUrl)) {
          throw new WebdavBadRequest(IscheduleTags.invalidCalendarData,
                                     "Attendee/originator mismatch");
        }
      }
    } else{
      ev.setOriginator(org.getOrganizerUri());
    }
  }

  private void setReqstat(final int status,
                          final boolean iSchedule) throws WebdavException {
    final String reqstat;

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
    final byte[] buffer = new byte[2048];
    int read;
    while ((read = in.read(buffer)) > 0) {
      out.write(buffer, 0, read);
    }
    in.close();
    out.close();
  }
}
