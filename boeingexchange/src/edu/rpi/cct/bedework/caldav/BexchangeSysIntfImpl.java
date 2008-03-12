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
package edu.rpi.cct.bedework.caldav;

import org.bedework.caldav.server.PropertyHandler;
import org.bedework.caldav.server.SysIntf;
import org.bedework.caldav.server.PropertyHandler.PropertyType;
import org.bedework.calfacade.BwCalendar;
import org.bedework.calfacade.BwDateTime;
import org.bedework.calfacade.BwEvent;
import org.bedework.calfacade.BwEventProxy;
import org.bedework.calfacade.BwUser;
import org.bedework.calfacade.RecurringRetrievalMode;
import org.bedework.calfacade.ScheduleResult;
import org.bedework.calfacade.base.BwShareableDbentity;
import org.bedework.calfacade.configs.CalDAVConfig;
import org.bedework.calfacade.filter.BwFilter;
import org.bedework.calfacade.svc.EventInfo;
import org.bedework.calfacade.timezones.CalTimezones;
import org.bedework.calfacade.timezones.ResourceTimezones;
import org.bedework.calfacade.util.ChangeTable;
import org.bedework.http.client.dav.DavClient;
import org.bedework.http.client.dav.DavReq;
import org.bedework.http.client.dav.DavResp;
import org.bedework.icalendar.IcalTranslator;
import org.bedework.icalendar.Icalendar;
import org.bedework.icalendar.SAICalCallback;

import edu.rpi.cct.webdav.servlet.shared.PrincipalPropertySearch;
import edu.rpi.cct.webdav.servlet.shared.WebdavBadRequest;
import edu.rpi.cct.webdav.servlet.shared.WebdavException;
import edu.rpi.cct.webdav.servlet.shared.WebdavNotFound;
import edu.rpi.cct.webdav.servlet.shared.WebdavNsNode.PropertyTagEntry;
import edu.rpi.cct.webdav.servlet.shared.WebdavNsNode.UrlHandler;
import edu.rpi.cmt.access.Ace;
import edu.rpi.cmt.access.Acl;
import edu.rpi.cmt.access.PrincipalInfo;
import edu.rpi.cmt.access.Acl.CurrentAccess;
import edu.rpi.sss.util.xml.QName;

import net.fortuna.ical4j.model.Calendar;

import org.apache.commons.httpclient.NoHttpResponseException;
import org.apache.log4j.Logger;

import java.io.InputStreamReader;
import java.io.Reader;
import java.io.Serializable;
import java.net.URI;
import java.net.URLDecoder;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

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
  private HashMap<String, DavClient> cioTable = new HashMap<String, DavClient>();

  // XXX get from properties
  private static String defaultTimezone = "America/Los_Angeles";

  private static HashMap<String, Integer> toWho = new HashMap<String, Integer>();
  private static HashMap<Integer, String> fromWho = new HashMap<Integer, String>();

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

  private static final HashMap<String, BexchangeInfo> serversInfo =
    new HashMap<String, BexchangeInfo>();

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

    initWhoMaps("/principals/users", Ace.whoTypeUser);
    initWhoMaps("/principals/groups", Ace.whoTypeGroup);
    initWhoMaps("/principals/tickets", Ace.whoTypeTicket);
    initWhoMaps("/principals/resources", Ace.whoTypeResource);
    initWhoMaps("/principals/venues", Ace.whoTypeVenue);
    initWhoMaps("/principals/hosts", Ace.whoTypeHost);
  }

  private boolean debug;

  private transient Logger log;

  private IcalTranslator trans;

  private UrlHandler urlHandler;

  //private String urlPrefix;

  private String account;

  public void init(HttpServletRequest req,
                   String envPrefix,
                   String account,
                   CalDAVConfig conf,
                   boolean debug) throws WebdavException {
    try {
      this.debug = debug;
      this.account = account;

      trans = new IcalTranslator(new SAICalCallback(null), debug);
      urlHandler = new UrlHandler(req, false);

      CalTimezones timezones = new ResourceTimezones(debug, null);
      CalTimezones.setTimezones(timezones);
      CalTimezones.setDefaultTzid(defaultTimezone);
    } catch (Throwable t) {
      throw new WebdavException(t);
    }
  }

  /* (non-Javadoc)
   * @see org.bedework.caldav.server.SysIntf#getAccount()
   */
  public String getAccount() throws WebdavException {
    return account;
  }

  private static class MyPropertyHandler extends PropertyHandler {
    private final static HashMap<QName, PropertyTagEntry> propertyNames =
      new HashMap<QName, PropertyTagEntry>();

    public Map<QName, PropertyTagEntry> getPropertyNames() {
      return propertyNames;
    }
  }

  /* (non-Javadoc)
   * @see org.bedework.caldav.server.SysIntf#getPropertyHandler(org.bedework.caldav.server.PropertyHandler.PropertyType)
   */
  public PropertyHandler getPropertyHandler(PropertyType ptype) throws WebdavException {
    return new MyPropertyHandler();
  }

  /* (non-Javadoc)
   * @see org.bedework.caldav.server.SysIntf#getUrlHandler()
   */
  public UrlHandler getUrlHandler() {
    return urlHandler;
  }

  /* (non-Javadoc)
   * @see org.bedework.caldav.server.SysIntf#getUrlPrefix()
   * /
  public String getUrlPrefix() {
    return urlPrefix;
  }

  /* (non-Javadoc)
   * @see org.bedework.caldav.server.SysIntf#getRelativeUrls()
   * /
  public boolean getRelativeUrls() {
    return false;
  }*/

  /* (non-Javadoc)
   * @see org.bedework.caldav.server.SysIntf#isPrincipal(java.lang.String)
   */
  public boolean isPrincipal(String val) throws WebdavException {
    return val.startsWith("/principals");
  }

  /* (non-Javadoc)
   * @see org.bedework.caldav.server.SysIntf#getPrincipalInfo(java.lang.String)
   */
  public PrincipalInfo getPrincipalInfo(String href) throws WebdavException {
    PrincipalInfo pi = new PrincipalInfo();

    try {
      String uri = new URI(href).getPath();

      if (!isPrincipal(uri)) {
        return null;
      }

      int start;

      int end = uri.length();
      if (uri.endsWith("/")) {
        end--;
      }

      String groupRoot = "/principals/groups";
      String userRoot = "/principals/users";

      if (uri.startsWith(userRoot)) {
        start = userRoot.length();
        pi.prefix = userRoot;
        pi.whoType = Ace.whoTypeUser;
      } else if (uri.startsWith(groupRoot)) {
        start = groupRoot.length();
        pi.prefix = groupRoot;
        pi.whoType = Ace.whoTypeGroup;
      } else {
        throw new WebdavNotFound(uri);
      }

      if (start == end) {
        // Trying to browse user principals?
        pi.who = null;
      } else if (uri.charAt(start) != '/') {
        throw new WebdavNotFound(uri);
      } else {
        pi.who = uri.substring(start + 1, end);
      }

      return pi;
    } catch (Throwable t) {
      throw new WebdavException(t);
    }
  }

  /* (non-Javadoc)
   * @see org.bedework.caldav.server.SysIntf#makeHref(java.lang.String, boolean)
   */
  public String makeHref(String id, int whoType) throws WebdavException {
    String root = fromWho.get(whoType);

    if (root == null) {
      throw new WebdavException("unknown who type " + whoType);
    }

    return root + "/" + id;
  }

  /* (non-Javadoc)
   * @see org.bedework.caldav.server.SysIntf#getGroups(java.lang.String, java.lang.String)
   */
  public Collection<String>getGroups(String rootUrl,
                                     String principalUrl) throws WebdavException {
    return Collections.emptySet();
  }

  public boolean getDirectoryBrowsingDisallowed() throws WebdavException {
    return false;
  }

  public String caladdrToUser(String caladdr) throws WebdavException {
    return caladdr;
  }

  /* (non-Javadoc)
   * @see org.bedework.caldav.server.SysIntf#userToCaladdr(java.lang.String)
   */
  public String userToCaladdr(String account) throws WebdavException {
    return account;
  }

  /* (non-Javadoc)
   * @see org.bedework.caldav.server.SysIntf#getCalUserInfo(java.lang.String, boolean)
   */
  public CalUserInfo getCalUserInfo(String account,
                                    boolean getDirInfo) throws WebdavException {
    return new CalUserInfo(account, "/principals/users", null, null, null, null, null);
  }

  public Collection<String> getPrincipalCollectionSet(String resourceUri)
          throws WebdavException {
    throw new WebdavException("unimplemented");
  }

  public Collection<CalUserInfo> getPrincipals(String resourceUri,
                                               PrincipalPropertySearch pps)
          throws WebdavException {
    throw new WebdavException("unimplemented");
  }

  public boolean validUser(String account) throws WebdavException {
    throw new WebdavException("unimplemented");
  }

  public boolean validGroup(String account) throws WebdavException {
    throw new WebdavException("unimplemented");
  }

  /* ====================================================================
   *                   Scheduling
   * ==================================================================== */

  public Collection<String> getFreebusySet() throws WebdavException {
    throw new WebdavException("unimplemented");
  }

  public ScheduleResult schedule(EventInfo ei) throws WebdavException {
    throw new WebdavException("unimplemented");
  }

  /* ====================================================================
   *                   Events
   * ==================================================================== */

  public Collection<BwEventProxy> addEvent(BwCalendar cal,
                                           EventInfo ei,
                                           boolean rollbackOnError) throws WebdavException {
    throw new WebdavException("unimplemented");
  }

  public void updateEvent(BwEvent event,
                          Collection<BwEventProxy> overrides,
                          ChangeTable changes) throws WebdavException {
    throw new WebdavException("unimplemented");
  }

  public Collection<EventInfo> getEvents(BwCalendar cal,
                                         BwFilter filter,
                                         RecurringRetrievalMode recurRetrieval)
            throws WebdavException {
    throw new WebdavException("unimplemented");
  }

  public EventInfo getEvent(BwCalendar cal, String val,
                            RecurringRetrievalMode recurRetrieval)
              throws WebdavException {
    throw new WebdavException("unimplemented");
  }

  public void deleteEvent(BwEvent ev) throws WebdavException {
    throw new WebdavException("unimplemented");
  }

  public void deleteCalendar(BwCalendar cal) throws WebdavException {
    throw new WebdavException("unimplemented");
  }

  public ScheduleResult requestFreeBusy(EventInfo ei) throws WebdavException {
    throw new WebdavException("unimplemented");
  }

  public BwEvent getFreeBusy(BwCalendar cal,
                             String account,
                             BwDateTime start,
                             BwDateTime end) throws WebdavException {
    /* Create a url something like:
     *  http://t1.egenconsulting.com:80/servlet/Freetime/John?start-min=2006-07-11T12:00:00Z&start-max=2006-07-16T12:00:00Z
     */
    try {
      String serviceName = getServiceName(cal.getPath());

      BexchangeInfo di = serversInfo.get(serviceName);
      if (di == null) {
        throw new WebdavBadRequest("Unknown service" + serviceName);
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
                 makeDate(start.addDur(BwDateTime.oneDayBack)) +
                 "&enddate=" +
                 makeDate(end.addDur(BwDateTime.oneDayBack)));

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
      BwEvent fb = makeFb(start, end,
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

        if (o instanceof BwEvent) {
          return (BwEvent)o;
        }
      }

      return null;
    } catch (WebdavException wde) {
      if (debug) {
        wde.printStackTrace();
      }
      throw wde;
    } catch (Throwable t) {
      if (debug) {
        t.printStackTrace();
      }
      throw new WebdavException(t);
    }
  }

  public CurrentAccess checkAccess(BwShareableDbentity ent,
                                   int desiredAccess,
                                   boolean returnResult)
          throws WebdavException {
    throw new WebdavException("unimplemented");
  }

  public void updateAccess(BwCalendar cal,
                           Acl acl) throws WebdavException {
    throw new WebdavException("unimplemented");
  }

  public void updateAccess(BwEvent ev,
                           Acl acl) throws WebdavException{
    throw new WebdavException("unimplemented");
  }

  /* (non-Javadoc)
   * @see org.bedework.caldav.server.SysIntf#makeCollection(java.lang.String, boolean, java.lang.String)
   */
  public int makeCollection(BwCalendar cal,
                            boolean calendarCollection,
                            String parentPath) throws WebdavException {
    throw new WebdavException("unimplemented");
  }

  /* (non-Javadoc)
   * @see org.bedework.caldav.server.SysIntf#copyMove(org.bedework.calfacade.BwCalendar, org.bedework.calfacade.BwCalendar, boolean)
   */
  public void copyMove(BwCalendar from,
                       BwCalendar to,
                       boolean copy,
                       boolean overwrite) throws WebdavException {
    throw new WebdavException("unimplemented");
  }

  /* (non-Javadoc)
   * @see org.bedework.caldav.server.SysIntf#copyMove(org.bedework.calfacade.BwEvent, org.bedework.calfacade.BwCalendar, java.lang.String, boolean)
   */
  public boolean copyMove(BwEvent from, Collection<BwEventProxy>overrides,
                          BwCalendar to,
                          String name,
                          boolean copy,
                          boolean overwrite) throws WebdavException {
    throw new WebdavException("unimplemented");
  }

  public BwCalendar getCalendar(String path) throws WebdavException {
    // XXX Just fake it up for the moment.
    /* The path should always start with /server-name/user
     */

    List<String> l = splitUri(path, true);

    String namePart = (String)l.get(l.size() - 1);

    BwCalendar cal = new BwCalendar();
    cal.setName(namePart);
    cal.setPath(path);

    String owner = (String)l.get(1);

    cal.setOwner(new BwUser(owner));

    return cal;
  }

  /* (non-Javadoc)
   * @see org.bedework.caldav.server.SysIntf#updateCalendar(org.bedework.calfacade.BwCalendar)
   */
  public void updateCalendar(BwCalendar val) throws WebdavException {
    throw new WebdavException("unimplemented");
  }

  public Collection<BwCalendar> getCalendars(BwCalendar cal) throws WebdavException {
    throw new WebdavException("unimplemented");
  }

  public Calendar toCalendar(EventInfo ev) throws WebdavException {
    throw new WebdavException("unimplemented");
  }

  public Calendar toCalendar(Collection<EventInfo> evs,
                             int method) throws WebdavException {
    throw new WebdavException("unimplemented");
  }

  public Icalendar fromIcal(BwCalendar cal, Reader rdr) throws WebdavException {
    throw new WebdavException("unimplemented");
  }

  public String toStringTzCalendar(String tzid) throws WebdavException {
    throw new WebdavException("unimplemented");
  }

  public int getMaxUserEntitySize() throws WebdavException {
    throw new WebdavException("unimplemented");
  }

  public void close() throws WebdavException {
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

  private String makeDate(BwDateTime dt) throws WebdavException {
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
      throw new WebdavException(t);
    }
  }

  /*
  private net.fortuna.ical4j.model.DateTime makeIcalDateTime(String val)
          throws WebdavException {
    try {
      net.fortuna.ical4j.model.DateTime icaldt =
        new net.fortuna.ical4j.model.DateTime(val);
      //icaldt.setUtc(true);
      return icaldt;
    } catch (Throwable t) {
      throw new WebdavException(t);
    }
  }
  */

  private List<String> splitUri(String uri, boolean decoded) throws WebdavException {
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
        throw new WebdavBadRequest("Bad uri: " + uri);
      }

      List<String> l = Arrays.asList(ss);
      return l.subList(1, l.size());
    } catch (Throwable t) {
      if (debug) {
        error(t);
      }
      throw new WebdavBadRequest("Bad uri: " + uri);
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
      cio.setCredentials(r.getUser(), r.getPw());

      responseCode = cio.sendRequest(r.getMethod(), r.getUrl(),
                                     r.getHeaders(), r.getDepth(),
                                     r.getContentType(), r.getContentLength(),
                                     r.getContentBytes());

      if (responseCode != HttpServletResponse.SC_OK) {
        if (debug) {
          debugMsg("Got response " + responseCode +
                   " for url " + r.getUrl() +
                   ", host " + di.getHost());
        }

        throw new WebdavException(responseCode);
      }
    } catch (WebdavException wde) {
      throw wde;
    } catch (NoHttpResponseException nhre) {
      throw new WebdavException(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
    } catch (Throwable t) {
      throw new WebdavException(t);
    }

    return cio.getResponse();
  }

  private DavClient getCio(String host, int port, boolean secure) throws Throwable {
    DavClient cio = cioTable.get(host + port + secure);

    if (cio == null) {
      cio = new DavClient(host, port, 30 * 1000, secure, debug);

      cioTable.put(host + port + secure, cio);
    }

    return cio;
  }

  private static void initWhoMaps(String prefix, int whoType) {
    toWho.put(prefix, whoType);
    fromWho.put(whoType, prefix);
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
