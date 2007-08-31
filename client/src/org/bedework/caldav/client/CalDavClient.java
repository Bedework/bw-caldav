/* **********************************************************************
    Copyright 2007 Rensselaer Polytechnic Institute. All worldwide rights reserved.

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
package org.bedework.caldav.client;

import org.bedework.caldav.client.FbResponse.FbResponseElement;
import org.bedework.calfacade.BwEvent;
import org.bedework.calfacade.exc.CalFacadeException;
import org.bedework.calfacade.svc.EventInfo;
import org.bedework.calfacade.svc.HostInfo;
import org.bedework.http.client.dav.DavClient;
import org.bedework.http.client.dav.DavReq;
import org.bedework.http.client.dav.DavResp;
import org.bedework.icalendar.IcalTranslator;
import org.bedework.icalendar.Icalendar;

import edu.rpi.sss.util.xml.XmlUtil;
import edu.rpi.sss.util.xml.tagdefs.CaldavTags;
import edu.rpi.sss.util.xml.tagdefs.WebdavTags;

import net.fortuna.ical4j.model.Calendar;

import org.apache.commons.httpclient.NoHttpResponseException;
import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;

import javax.servlet.http.HttpServletResponse;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

/** Handle interactions with caldav servers.
 *
 * @author Mike Douglass
 */
public class CalDavClient {
  private boolean debug;

  private transient Logger log;

  private transient IcalTranslator trans;

  /* There is one entry per host + port. Because we are likely to make a number
   * of calls to the same host + port combination it makes sense to preserve
   * the objects between calls.
   */
  private HashMap<String, DavClient> cioTable = new HashMap<String, DavClient>();

  /** Constructor
   *
   * @param trans
   * @param debug
   * @throws Throwable
   */
  public CalDavClient(IcalTranslator trans, boolean debug) throws Throwable {
    this.trans = trans;
    this.debug = debug;
  }

  /** Get the freebusy for the recipients specified in the event object,
   * e.g. start, end, organizer etc.
   *
   * @param hi
   * @param ei
   * @return FbResponse
   * @throws Throwable
   */
  public FbResponse getFreeBusy(HostInfo hi,
                                EventInfo ei) throws Throwable {
    DavReq r = makeFreeBusyRequest(hi, ei);

    FbResponse resp = new FbResponse();

    send(r, hi, resp);

    Document doc = parseContent(resp.getCdresp());

    /* We expect something like...
     *
     *    <C:schedule-response xmlns:D="DAV:"
                xmlns:C="urn:ietf:params:xml:ns:caldav">
   <C:response>
     <C:recipient>mailto:bernard@example.com</C:recipient>
     <C:request-status>2.0;Success</C:request-status>
     <C:calendar-data>BEGIN:VCALENDAR
   VERSION:2.0
   PRODID:-//Example Corp.//CalDAV Server//EN
   METHOD:REPLY
   BEGIN:VFREEBUSY
   DTSTAMP:20040901T200200Z
   ORGANIZER:mailto:lisa@example.com
   DTSTART:20040902T000000Z
   DTEND:20040903T000000Z
   UID:34222-232@example.com
   ATTENDEE;CN=Bernard Desruisseaux:mailto:bernard@
    example.com
   FREEBUSY;FBTYPE=BUSY-UNAVAILABLE:20040902T000000Z/
    20040902T090000Z,20040902T170000Z/20040903T000000Z
   END:VFREEBUSY
   END:VCALENDAR
   </C:calendar-data>
   </C:response>
   <C:response>
     <C:recipient>mailto:cyrus@example.com</C:recipient>
     <C:request-status>2.0;Success</C:request-status>
     <C:calendar-data>BEGIN:VCALENDAR
   VERSION:2.0
   PRODID:-//Example Corp.//CalDAV Server//EN
   METHOD:REPLY
   BEGIN:VFREEBUSY
   DTSTAMP:20040901T200200Z
   ORGANIZER:mailto:lisa@example.com
   DTSTART:20040902T000000Z
   DTEND:20040903T000000Z
   UID:34222-232@example.com
   ATTENDEE;CN=Cyrus Daboo:mailto:cyrus@example.com
   FREEBUSY;FBTYPE=BUSY-UNAVAILABLE:20040902T000000Z/
    20040902T090000Z,20040902T170000Z/20040903T000000Z
   FREEBUSY;FBTYPE=BUSY:20040902T120000Z/20040902T130000Z
   END:VFREEBUSY
   END:VCALENDAR
   </C:calendar-data>
   </C:response>
   </C:schedule-response>
     */

    try {
      Element root = doc.getDocumentElement();

      if (!CaldavTags.scheduleResponse.nodeMatches(root)) {
        throw new CalFacadeException(CalFacadeException.badResponse);
      }

      for (Element el: getChildren(root)) {
        FbResponseElement fbel = new FbResponseElement();

        if (!CaldavTags.response.nodeMatches(el)) {
          throw new CalFacadeException(CalFacadeException.badResponse);
        }

        /* ================================================================
        11.2.  CALDAV:response XML Element

        Name:  response
        Namespace:  urn:ietf:params:xml:ns:caldav

        Purpose:  Contains a single response for a POST method request.
        Description:  See Section 6.1.4.
        Definition:

        <!ELEMENT response (recipient,
                            request-status,
                            calendar-data?,
                            DAV:error?,
                            DAV:responsedescription?)>
           ================================================================ */

        Iterator<Element> respels = getChildren(el).iterator();

        Element respel = respels.next();

        if (!CaldavTags.recipient.nodeMatches(respel)) {
          throw new CalFacadeException(CalFacadeException.badResponse);
        }

        fbel.setRecipient(getElementContent(respel));

        respel = respels.next();

        if (!CaldavTags.requestStatus.nodeMatches(respel)) {
          throw new CalFacadeException(CalFacadeException.badResponse);
        }

        fbel.setReqStatus(getElementContent(respel));

        respel = respels.next();

        if (CaldavTags.calendarData.nodeMatches(respel)) {
          String calData = getElementContent(respel);

          Reader rdr = new StringReader(calData);
          Icalendar ical = trans.fromIcal(null, rdr);

          fbel.setFreeBusy(ical.getEventInfo());
        } else if (WebdavTags.error.nodeMatches(respel)) {
          fbel.setDavError(respel.getFirstChild().getLocalName());
        } else {
          throw new CalFacadeException(CalFacadeException.badResponse);
        }

        resp.addResponse(fbel);
      }
    } catch (Throwable t) {
      if (debug) {
        error(t);
      }

      resp.setException(t);
    }

    return resp;
  }

  /** Schedule a meeting with the recipients specified in the event object,
   * e.g. start, end, organizer etc.
   *
   * @param hi
   * @param ei
   * @return FbResponse
   * @throws Throwable
   */
  public SchedResponse scheduleMeeting(HostInfo hi,
                                       EventInfo ei) throws Throwable {
    return null;
  }

  /**
   * @param r
   * @param hi
   * @param resp Response
   * @throws Throwable
   */
  public void send(DavReq r, HostInfo hi, Response resp) throws Throwable {
    DavClient cio = getCio(hi.getHostname(), hi.getPort(), hi.getSecure());
    resp.setHostInfo(hi);

    try {
      if (r.getAuth()) {
        resp.setResponseCode(cio.sendRequest(r.getMethod(), r.getUrl(),
                                             r.getUser(), r.getPw(),
                                             r.getHeaders(), r.getDepth(),
                                             r.getContentType(),
                                             r.getContentLength(),
                                             r.getContentBytes()));
      } else {
        resp.setResponseCode(cio.sendRequest(r.getMethod(), r.getUrl(),
                                             r.getHeaders(), r.getDepth(),
                                             r.getContentType(),
                                             r.getContentLength(),
                                             r.getContentBytes()));
      }

      if (resp.getResponseCode() != HttpServletResponse.SC_OK) {
        error("Got response " + resp.getResponseCode() +
              ", host " + hi.getHostname() +
              " and url " + hi.getCaldavUrl());
        return;
      }
    } catch (NoHttpResponseException nhre) {
      resp.setNoResponse(true);
      return;
    } catch (Throwable t) {
      resp.setException(t);
      return;
    }

    resp.setCdresp(cio.getResponse());

    return;
  }

  private DavReq makeFreeBusyRequest(HostInfo hi,
                                     org.bedework.calfacade.svc.EventInfo ei) throws CalFacadeException {
    DavReq req;

    boolean realtime = hi.getSupportsRealtime();

    BwEvent ev = ei.getEvent();
    Collection<String> recipients = ev.getRecipients();

    if (!realtime && recipients.size() > 1) {
      throw new CalFacadeException(CalFacadeException.schedulingBadRecipients);
    }

    String url;
    String principal;
    String creds;

    if (realtime) {
      url = hi.getRtUrl();
      principal = hi.getRtPrincipal();
      creds = hi.getRtCredentials();
    } else {
      url = hi.getCaldavUrl();
      principal = hi.getCaldavPrincipal();
      creds = hi.getCaldavCredentials();
    }

    if (principal == null) {
      req = new DavReq();
    } else {
      req = new DavReq(principal, creds);
    }

    req.setUrl(url);

    req.setContentType("text/calendar");
    req.setMethod("POST");

    Calendar cal = trans.toIcal(ei, ev.getScheduleMethod());

    StringWriter sw = new StringWriter();
    IcalTranslator.writeCalendar(cal, sw);

    req.addContentLine(sw.toString());

    return req;
  }

  private DavClient getCio(String host, Integer port, boolean secure) throws Throwable {
    if (port == null) {
      port = 80;
    }

    DavClient cio = cioTable.get(host + port + secure);

    if (cio == null) {
      cio = new DavClient(host, port, 30 * 1000, secure, debug);

      cioTable.put(host + port + secure, cio);
    }

    return cio;
  }

  /** Parse the content, and return the DOM representation.
   *
   * @param resp       response from server
   * @return Document  Parsed body or null for no body
   * @exception CalFacadeException Some error occurred.
   */
  protected Document parseContent(DavResp resp) throws CalFacadeException{
    try {
      long len = resp.getContentLength();
      if (len <= 0) {
        return null;
      }

      DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
      factory.setNamespaceAware(true);

      DocumentBuilder builder = factory.newDocumentBuilder();

      InputStream in = resp.getContentStream();

      return builder.parse(new InputSource(new InputStreamReader(in)));
    } catch (SAXException e) {
      throw new CalFacadeException(CalFacadeException.badResponse);
    } catch (Throwable t) {
      throw new CalFacadeException(t);
    }
  }

  /* ====================================================================
   *                   XmlUtil wrappers
   * ==================================================================== */

  protected Collection<Element> getChildren(Node nd) throws CalFacadeException {
    try {
      return XmlUtil.getElements(nd);
    } catch (Throwable t) {
      if (debug) {
        getLogger().error(this, t);
      }

      throw new CalFacadeException(CalFacadeException.badResponse);
    }
  }

  protected String getElementContent(Element el) throws CalFacadeException {
    try {
      return XmlUtil.getElementContent(el);
    } catch (Throwable t) {
      if (debug) {
        getLogger().error(this, t);
      }

      throw new CalFacadeException(CalFacadeException.badResponse);
    }
  }

  protected Logger getLogger() {
    if (log == null) {
      log = Logger.getLogger(this.getClass());
    }

    return log;
  }

  protected void trace(String msg) {
    getLogger().debug(msg);
  }

  protected void debugMsg(String msg) {
    getLogger().debug(msg);
  }

  protected void warn(String msg) {
    getLogger().warn(msg);
  }

  protected void error(String msg) {
    getLogger().error(msg);
  }

  protected void error(Throwable t) {
    getLogger().error(this, t);
  }
}
