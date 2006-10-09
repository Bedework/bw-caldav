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
package edu.rpi.cct.bedework.caldav;

import org.bedework.caldav.server.SysIntf;
import org.bedework.calfacade.BwCalendar;
import org.bedework.calfacade.BwDateTime;
import org.bedework.calfacade.BwEvent;
import org.bedework.calfacade.BwFreeBusy;
import org.bedework.calfacade.BwUser;
import org.bedework.calfacade.RecurringRetrievalMode;
import org.bedework.calfacade.ScheduleResult;
import org.bedework.calfacade.BwRecurrence.ChangeSet;
import org.bedework.calfacade.base.BwShareableDbentity;
import org.bedework.calfacade.svc.EventInfo;
import org.bedework.calfacade.timezones.CalTimezones;
import org.bedework.calfacade.timezones.ResourceTimezones;
import org.bedework.http.client.dav.DavClient;
import org.bedework.http.client.dav.DavReq;
import org.bedework.http.client.dav.DavResp;
import org.bedework.icalendar.IcalTranslator;
import org.bedework.icalendar.Icalendar;
import org.bedework.icalendar.SAICalCallback;

import edu.rpi.cct.webdav.servlet.common.WebdavUtils;
import edu.rpi.cct.webdav.servlet.shared.PrincipalPropertySearch;
import edu.rpi.cct.webdav.servlet.shared.WebdavException;
import edu.rpi.cct.webdav.servlet.shared.WebdavIntfException;
import edu.rpi.cmt.access.Acl.CurrentAccess;

import net.fortuna.ical4j.model.Calendar;
import net.fortuna.ical4j.model.TimeZone;

import org.apache.commons.httpclient.NoHttpResponseException;
import org.apache.log4j.Logger;

import java.io.InputStreamReader;
import java.io.Reader;
import java.io.Serializable;
import java.net.URI;
import java.net.URLDecoder;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/** Domino implementation of SysIntf. This interacts with a servlet on Domino
 * which presents requested calendar information.
 *
 * @author Mike Douglass douglm at rpi.edu
 */
public class BexchangeSysIntfImpl implements SysIntf {
  /* There is one entry per host + port. Because we are likely to make a number
   * of calls to the same host + port combination it makes sense to preserve
   * the objects between calls.
   */
  private HashMap cioTable = new HashMap();

  private ResourceTimezones timezones;

  // XXX get from properties
  private static String defaultTimezone = "America/Los_Angeles";

  /* These could come from a db
   */
  private static class BexchangeInfo implements Serializable {
    String account;
    String host;
    int port;
    String urlPrefix;
    boolean secure;

    BexchangeInfo(String account,
                  String host, int port, String urlPrefix, boolean secure) {
      this.account = account;
      this.host = host;
      this.port = port;
      this.urlPrefix = urlPrefix;
      this.secure= secure;
    }

    /**
     * @return String
     */
    public String getAccount() {
      return account;
    }

    /**
     * @return String
     */
    public String getHost() {
      return host;
    }

    /**
     * @return int
     */
    public int getPort() {
      return port;
    }

    /**
     * @return String
     */
    public boolean getSecure() {
      return secure;
    }

    /**
     * @return String
     */
    public String getUrlPrefix() {
      return urlPrefix;
    }
  }


  private static final HashMap serversInfo = new HashMap();

  static {
    serversInfo.put("wenfang@calnet.local",
                    new BexchangeInfo("wenfang@calnet.local",
                                      "207.145.218.101", 80,
                                      "/fbsrv/getfbsrv.asp?email=", false));

    serversInfo.put("fbtester1@calnet.local",
                    new BexchangeInfo("fbtester1@calnet.local",
                                      "207.145.218.101", 80,
                                      "/fbsrv/getfbsrv.asp?email=", false));

    serversInfo.put("fbtester2@calnet.local",
                    new BexchangeInfo("fbtester2@calnet.local",
                                      "207.145.218.101", 80,
                                      "/fbsrv/getfbsrv.asp?email=", false));
  }

  private boolean debug;

  private transient Logger log;

  private IcalTranslator trans;

  private String urlPrefix;

  public void init(HttpServletRequest req,
                   String envPrefix,
                   String account,
                   boolean debug) throws WebdavIntfException {
    try {
      this.debug = debug;

      trans = new IcalTranslator(new SAICalCallback(getTimezones(), null),
                                 debug);
      urlPrefix = WebdavUtils.getUrlPrefix(req);
    } catch (Throwable t) {
      throw new WebdavIntfException(t);
    }
  }

  public String getUrlPrefix() {
    return urlPrefix;
  }

  public boolean getDirectoryBrowsingDisallowed() throws WebdavIntfException {
    return false;
  }

  public String caladdrToUser(String caladdr) throws WebdavIntfException {
    return caladdr;
  }

  public CalUserInfo getCalUserInfo(String caladdr) throws WebdavIntfException {
    return new CalUserInfo(caladdrToUser(caladdr),
                           null, null, null, null);
  }

  public Collection getPrincipalCollectionSet(String resourceUri)
          throws WebdavIntfException {
    throw new WebdavIntfException("unimplemented");
  }

  public Collection getPrincipals(String resourceUri,
                           PrincipalPropertySearch pps)
          throws WebdavIntfException {
    throw new WebdavIntfException("unimplemented");
  }

  public boolean validUser(String account) throws WebdavIntfException {
    throw new WebdavIntfException("unimplemented");
  }

  public boolean validGroup(String account) throws WebdavIntfException {
    throw new WebdavIntfException("unimplemented");
  }

  /* ====================================================================
   *                   Scheduling
   * ==================================================================== */

  public ScheduleResult schedule(BwEvent event) throws WebdavIntfException {
    throw new WebdavIntfException("unimplemented");
  }

  public void addEvent(BwCalendar cal,
                       BwEvent event,
                       Collection overrides) throws WebdavIntfException {
    throw new WebdavIntfException("unimplemented");
  }

  public void updateEvent(BwEvent event,
                          Collection overrides,
                          ChangeSet recurrenceChanges) throws WebdavIntfException {
    throw new WebdavIntfException("unimplemented");
  }

  public Collection getEvents(BwCalendar cal,
                              BwDateTime startDate, BwDateTime endDate,
                              RecurringRetrievalMode recurRetrieval) throws WebdavIntfException {
    throw new WebdavIntfException("unimplemented");
  }

  public EventInfo getEvent(BwCalendar cal, String val,
                            RecurringRetrievalMode recurRetrieval)
              throws WebdavIntfException {
    throw new WebdavIntfException("unimplemented");
  }

  public void deleteEvent(BwEvent ev) throws WebdavIntfException {
    throw new WebdavIntfException("unimplemented");
  }

  public void deleteCalendar(BwCalendar cal) throws WebdavIntfException {
    throw new WebdavIntfException("unimplemented");
  }

  public BwFreeBusy getFreeBusy(BwCalendar cal,
                                String account,
                                BwDateTime start,
                                BwDateTime end) throws WebdavException {
    /* Create a url something like:
     *  http://t1.egenconsulting.com:80/servlet/Freetime/John?start-min=2006-07-11T12:00:00Z&start-max=2006-07-16T12:00:00Z
     */
    try {
      String serviceName = getServiceName(cal.getPath());

      BexchangeInfo di = (BexchangeInfo)serversInfo.get(serviceName);
      if (di == null) {
        throw WebdavIntfException.badRequest();
      }

      DavReq req = new DavReq();

      req.setMethod("GET");

      /* At the moment we the requested date range is in local time - local to
       * the exchange server.
       * Get an extra day at front/back.
       */
      req.setUrl(di.getUrlPrefix() +
                 serviceName + "&" +       // Really email
                 "startdate=" +
                 makeDate(start.addDur(BwDateTime.oneDayBack, getTimezones())) +
                 "&enddate=" +
                 makeDate(end.addDur(BwDateTime.oneDayBack, getTimezones())));

//      req.addHeader("Accept",
//                    "text/xml,application/xml,application/xhtml+xml,text/html;q=0.9,text/plain;q=0.8,image/png,*/*;q=0.5");
//      req.addHeader("Accept-Language", "en-us,en;q=0.7,es;q=0.3");
//      req.addHeader("Accept-Encoding", "gzip,deflate");
//      req.addHeader("Accept-Charset", "ISO-8859-1,utf-8;q=0.7,*;q=0.7");

      DavResp resp = send(req, di);

      if (debug) {
        debugMsg("Got response \n" + resp.getResponseBodyAsString());
      }
      /*
      BwFreeBusy fb = makeFb(start, end,
                             "000010110000111100001101" +
                             "000010110000111100001101" +
                             "000010110000111100001101" +
                             "000010110000111100001101" +
                             "000010110000111100001101",
                             60);
                             */
/*      String vfb = makeVfb(new InputStreamReader(resp.getContentStream()));
      if (debug) {
        debugMsg(vfb);
      }

      Collection fbs = getTrans().getFreeBusy(new StringReader(vfb));
      */
      Icalendar ic = trans.fromIcal(null, new InputStreamReader(resp.getContentStream()));
      Iterator fbit = ic.iterator();
      while (fbit.hasNext()) {
        Object o = fbit.next();

        if (o instanceof BwFreeBusy) {
          return (BwFreeBusy)o;
        }
      }

      return null;
    } catch (WebdavIntfException wie) {
      if (debug) {
        wie.printStackTrace();
      }
      throw wie;
    } catch (Throwable t) {
      if (debug) {
        t.printStackTrace();
      }
      throw new WebdavIntfException(t);
    }
  }

  public CurrentAccess checkAccess(BwShareableDbentity ent,
                                   int desiredAccess,
                                   boolean returnResult)
          throws WebdavException {
    throw new WebdavIntfException("unimplemented");
  }

  public void updateAccess(BwCalendar cal,
                           Collection aces) throws WebdavIntfException {
    throw new WebdavIntfException("unimplemented");
  }

  public void updateAccess(BwEvent ev,
                           Collection aces) throws WebdavIntfException{
    throw new WebdavIntfException("unimplemented");
  }

  public void makeCollection(String name, boolean calendarCollection,
                             String parentPath) throws WebdavIntfException {
    throw new WebdavIntfException("unimplemented");
  }

  public BwCalendar getCalendar(String path) throws WebdavIntfException {
    // XXX Just fake it up for the moment.
    /* The path should always start with /server-name/user
     */

    List l = splitUri(path, true);

    String namePart = (String)l.get(l.size() - 1);

    BwCalendar cal = new BwCalendar();
    cal.setName(namePart);
    cal.setPath(path);

    String owner = (String)l.get(1);

    cal.setOwner(new BwUser(owner));

    return cal;
  }

  public Collection getCalendars(BwCalendar cal) throws WebdavIntfException {
    throw new WebdavIntfException("unimplemented");
  }

  public Calendar toCalendar(EventInfo ev) throws WebdavIntfException {
    throw new WebdavIntfException("unimplemented");
  }

  public Icalendar fromIcal(BwCalendar cal, Reader rdr) throws WebdavIntfException {
    throw new WebdavIntfException("unimplemented");
  }

  public CalTimezones getTimezones() throws WebdavIntfException {
    try {
      if (timezones == null) {
        timezones = new ResourceTimezones(debug, null);
        timezones.setDefaultTimeZoneId(defaultTimezone);
      }

      return timezones;
    } catch (Throwable t) {
      throw new WebdavIntfException(t);
    }
  }

  public TimeZone getDefaultTimeZone() throws WebdavIntfException {
    try {
      return getTimezones().getDefaultTimeZone();
    } catch (Throwable t) {
      throw new WebdavIntfException(t);
    }
  }

  public String toStringTzCalendar(String tzid) throws WebdavIntfException {
    throw new WebdavIntfException("unimplemented");
  }

  public int getMaxUserEntitySize() throws WebdavIntfException {
    throw new WebdavIntfException("unimplemented");
  }

  public void close() throws WebdavIntfException {
  }

  /* ====================================================================
   *                         Private methods
   * ==================================================================== */

//  private static final char exchangeFBFree = '0';
//  private static final char exchangeFBBusy = '1';
//  private static final char exchangeFBBusyTentative = '2';
//  private static final char exchangeFBOutOfOffice = '3';

  /* The String is a number of digits representing the given time period
   * from start to end in cellsize minute increments.
   *
   * <p>0 means free, 1 means busy, 2 means tentative, and 3 means out-of-office
   *
   * We return a BwFreeBusy object representing the information
   * /
  private BwFreeBusy makeFb(BwDateTime start,
                            BwDateTime end,
                            String val,
                            int cellSize) throws WebdavException{
    BwFreeBusy fb = new BwFreeBusy();

    fb.setStart(start);
    fb.setEnd(end);

    char lastDigit = 0;

    SimpleTimeZone utctz = new SimpleTimeZone(SimpleTimeZone.UTC_TIME, "UTC");
    java.util.Calendar startCal = java.util.Calendar.getInstance(utctz);

    java.util.Calendar endCal = java.util.Calendar.getInstance(utctz);

    try {
      startCal.setTime(start.makeDate());

      endCal.setTime(start.makeDate());

      DateTime startDt = null;

      char[] digits = val.toCharArray();

      for (int i = 0; i < digits.length; i++) {
        char digit = digits[i];
        endCal.add(java.util.Calendar.MINUTE, cellSize);

        if ((lastDigit != digit) || (i == digits.length)) {
          // End of period or end of freebusy

          DateTime endDt = new DateTime(endCal.getTime());
          endDt.setUtc(true);

          if (startDt != null) {
            if (lastDigit != exchangeFBFree) {
              /* Just finished a non-free period * /
              BwFreeBusyComponent fbcomp = new BwFreeBusyComponent();

              fb.addTime(fbcomp);

              int type = -1;
              if (lastDigit == exchangeFBBusy) {
                type = BwFreeBusyComponent.typeBusy;
              } else if (lastDigit == exchangeFBBusyTentative) {
                type = BwFreeBusyComponent.typeBusyTentative;
              } else if (lastDigit == exchangeFBOutOfOffice) {
                type = BwFreeBusyComponent.typeBusyUnavailable;
              }

              fbcomp.setType(type);

              fbcomp.addPeriod(new Period(startDt, endDt));
            }
          }

          startDt = endDt;
        }

        lastDigit = digit;
      }

      return fb;
    } catch (Throwable t) {
      if (debug) {
        getLogger().error(this, t);
      }

      throw new WebdavException(t);
    }
  }*/

  /* <?xml version='1.0' encoding='utf-8'?>
<D:multistatus xmlns:D='DAV:' xmlns:C='urn:ietf:params:xml:ns:caldav'>
<D:response>
<D:href>http://207.145.218.101/fbsrv/</D:href>
<D:status>HTTP/1.1 200 OK</D:status>
<C:calendar-data>BEGIN:VCALENDAR
VERSION:2.0
PRODID:-//CALNET DEMO//CalDAV Client//EN
BEGIN:VFREEBUSY
DTSTAMP:2006718T080000Z
DTSTART:2006718T080000Z
DTEND:2006722T080000Z
FREEBUSE:2006718T090000Z/PT1H30M,
2006719T100000Z/PT30M,
2006719T110000Z/PT1H,
2006720T100000Z/PT30M,
2006720T133000Z/PT2H,
2006721T100000Z/PT30M,
END:VFREEBUSY
END:VCALENDAR
</C:calendar-data>
</D:response>
</D:multistatus>

  private String makeVfb(Reader rdr) throws WebdavException{
    try {
      DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
      factory.setNamespaceAware(true);

      DocumentBuilder builder = factory.newDocumentBuilder();

      Document doc = builder.parse(new InputSource(rdr));

      Element root = doc.getDocumentElement(); // <D:multistatus>
      Element child = getOnlyChild(root);  //  <D:response>

      Element[] children = getChildren(child);

      for (int i = 0; i < children.length; i++) {
        Element curnode = children[i];

        String nm = curnode.getLocalName();

        if (nm.equals("calendar-data")) {
          return getElementContent(curnode);
        }
      }

      return null;
    } catch (SAXException e) {
      throw new WebdavException(HttpServletResponse.SC_BAD_REQUEST);
    } catch (Throwable t) {
      throw new WebdavException(t);
    } finally {
      if (rdr != null) {
        try {
          rdr.close();
        } catch (Throwable t) {}
      }
    }
  }

  protected Element[] getChildren(Node nd) throws WebdavException {
    try {
      return XmlUtil.getElementsArray(nd);
    } catch (Throwable t) {
      if (debug) {
        getLogger().error(this, t);
      }

      throw new WebdavException(t);
    }
  }

  protected Element getOnlyChild(Node nd) throws WebdavException {
    try {
      return XmlUtil.getOnlyElement(nd);
    } catch (Throwable t) {
      if (debug) {
        getLogger().error(this, t);
      }

      throw new WebdavException(t);
    }
  }

  protected String getElementContent(Element el) throws WebdavException {
    try {
      return XmlUtil.getElementContent(el);
    } catch (Throwable t) {
      if (debug) {
        getLogger().error(this, t);
      }

      throw new WebdavException(t);
    }
  }
  */

  private String makeDate(BwDateTime dt) throws WebdavIntfException {
    try {
      String utcdt = dt.getDate();

      StringBuffer sb = new StringBuffer();

      // from 20060716T120000Z make 07/16/2006
      //      0   4 6    1 3
      sb.append(utcdt.substring(4, 6));
      sb.append("/");
      sb.append(utcdt.substring(6, 8));
      sb.append("/");
      sb.append(utcdt.substring(0, 4));

      return sb.toString();
    } catch (Throwable t) {
      throw new WebdavIntfException(t);
    }
  }

  /*
  private net.fortuna.ical4j.model.DateTime makeIcalDateTime(String val)
          throws WebdavIntfException {
    try {
      net.fortuna.ical4j.model.DateTime icaldt =
        new net.fortuna.ical4j.model.DateTime(val);
      //icaldt.setUtc(true);
      return icaldt;
    } catch (Throwable t) {
      throw new WebdavIntfException(t);
    }
  }
  */

  private List splitUri(String uri, boolean decoded) throws WebdavIntfException {
    try {
      /*Remove all "." and ".." components */
      if (decoded) {
        uri = new URI(null, null, uri, null).toString();
      }

      uri = new URI(uri).normalize().getPath();
      if (debug) {
        debugMsg("Normalized uri=" + uri);
      }

      uri = URLDecoder.decode(uri, "UTF-8");

      if (!uri.startsWith("/")) {
        return null;
      }

      if (uri.endsWith("/")) {
        uri = uri.substring(0, uri.length() - 1);
      }

      String[] ss = uri.split("/");
      int pathLength = ss.length - 1;  // First element is empty string

      if (pathLength < 2) {
        throw WebdavIntfException.badRequest();
      }

      List l = Arrays.asList(ss);
      return l.subList(1, l.size());
    } catch (Throwable t) {
      if (debug) {
        error(t);
      }
      throw WebdavIntfException.badRequest();
    }
  }

  private String getServiceName(String path) {
    int pos = path.indexOf("/", 1);

    if (pos < 0) {
      return path.substring(1);
    }

    return path.substring(1, pos);
  }

  /**
   * @param r
   * @param di
   * @return DavResp
   * @throws Throwable
   */
  private DavResp send(DavReq r, BexchangeInfo di) throws Throwable {
    DavClient cio = getCio(di.getHost(), di.getPort(), di.getSecure());

    int responseCode;

    try {
      if (r.getAuth()) {
        responseCode = cio.sendRequest(r.getMethod(), r.getUrl(),
                                       r.getUser(), r.getPw(),
                                       r.getHeaders(), r.getDepth(),
                                       r.getContentType(),
                                       r.getContentLength(), r.getContentBytes());
      } else {
        responseCode = cio.sendRequest(r.getMethod(), r.getUrl(),
                                       r.getHeaders(), r.getDepth(),
                                       r.getContentType(), r.getContentLength(),
                                       r.getContentBytes());
      }

      if (responseCode != HttpServletResponse.SC_OK) {
        if (debug) {
          debugMsg("Got response " + responseCode +
                   " for url " + r.getUrl() +
                   ", host " + di.getHost());
        }

        throw new WebdavIntfException(responseCode);
      }
    } catch (WebdavIntfException wie) {
      throw wie;
    } catch (NoHttpResponseException nhre) {
      throw new WebdavIntfException(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
    } catch (Throwable t) {
      throw new WebdavIntfException(t);
    }

    return cio.getResponse();
  }

  private DavClient getCio(String host, int port, boolean secure) throws Throwable {
    DavClient cio = (DavClient)cioTable.get(host + port + secure);

    if (cio == null) {
      cio = new DavClient(host, port, 30 * 1000, secure, debug);

      cioTable.put(host + port + secure, cio);
    }

    return cio;
  }

  /* ====================================================================
   *                        Protected methods
   * ==================================================================== */

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

  protected void logIt(String msg) {
    getLogger().info(msg);
  }
}
