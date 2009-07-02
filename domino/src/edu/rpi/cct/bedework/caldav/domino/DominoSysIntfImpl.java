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
package edu.rpi.cct.bedework.caldav.domino;

import org.bedework.caldav.server.CalDAVCollection;
import org.bedework.caldav.server.CalDAVCollectionBase;
import org.bedework.caldav.server.CalDAVEvent;
import org.bedework.caldav.server.CalDAVResource;
import org.bedework.caldav.server.PropertyHandler;
import org.bedework.caldav.server.SysiIcalendar;
import org.bedework.caldav.server.PostMethod.RequestPars;
import org.bedework.caldav.server.PropertyHandler.PropertyType;
import org.bedework.caldav.server.sysinterface.CalPrincipalInfo;
import org.bedework.caldav.server.sysinterface.RetrievalMode;
import org.bedework.caldav.server.sysinterface.SysIntf;
import org.bedework.caldav.server.sysinterface.SystemProperties;
import org.bedework.caldav.util.CalDAVConfig;
import org.bedework.caldav.util.TimeRange;
import org.bedework.calfacade.BwDateTime;
import org.bedework.calfacade.BwEvent;
import org.bedework.calfacade.BwEventObj;
import org.bedework.calfacade.BwFreeBusyComponent;
import org.bedework.calfacade.CalFacadeDefs;
import org.bedework.calfacade.filter.BwFilter;
import org.bedework.calfacade.timezones.CalTimezones;
import org.bedework.http.client.dav.DavClient;
import org.bedework.http.client.dav.DavReq;
import org.bedework.http.client.dav.DavResp;
import org.bedework.icalendar.IcalTranslator;
import org.bedework.icalendar.Icalendar;
import org.bedework.icalendar.SAICalCallback;
import org.bedework.icalendar.VFreeUtil;

import edu.rpi.cct.bedework.caldav.exchange.Group;
import edu.rpi.cct.bedework.caldav.exchange.User;
import edu.rpi.cct.webdav.servlet.shared.PrincipalPropertySearch;
import edu.rpi.cct.webdav.servlet.shared.WdEntity;
import edu.rpi.cct.webdav.servlet.shared.WebdavBadRequest;
import edu.rpi.cct.webdav.servlet.shared.WebdavException;
import edu.rpi.cct.webdav.servlet.shared.WebdavNotFound;
import edu.rpi.cct.webdav.servlet.shared.WebdavServerError;
import edu.rpi.cct.webdav.servlet.shared.WebdavNsNode.PropertyTagEntry;
import edu.rpi.cct.webdav.servlet.shared.WebdavNsNode.UrlHandler;
import edu.rpi.cmt.access.AccessPrincipal;
import edu.rpi.cmt.access.Ace;
import edu.rpi.cmt.access.Acl;
import edu.rpi.cmt.access.Acl.CurrentAccess;
import edu.rpi.sss.util.xml.XmlUtil;

import net.fortuna.ical4j.model.Calendar;
import net.fortuna.ical4j.model.DateTime;
import net.fortuna.ical4j.model.Period;
import net.fortuna.ical4j.model.component.VFreeBusy;

import org.apache.commons.httpclient.NoHttpResponseException;
import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import java.io.InputStreamReader;
import java.io.Reader;
import java.io.Serializable;
import java.io.StringReader;
import java.io.Writer;
import java.net.URI;
import java.net.URLDecoder;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

/** Domino implementation of SysIntf. This interacts with a servlet on Domino
 * which presents requested calendar information.
 *
 * @author Mike Douglass douglm at rpi.edu
 */
public class DominoSysIntfImpl implements SysIntf {
  /* There is one entry per host + port. Because we are likely to make a number
   * of calls to the same host + port combination it makes sense to preserve
   * the objects between calls.
   */
  private HashMap<String, DavClient> cioTable = new HashMap<String, DavClient>();

  // XXX get from properties
  private static String defaultTimezone = "America/Los_Angeles";

  private static HashMap<String, Integer> toWho = new HashMap<String, Integer>();
  private static HashMap<Integer, String> fromWho = new HashMap<Integer, String>();

  protected AccessPrincipal currentPrincipal;

  /* These could come from a db
   */
  private static class DominoInfo implements Serializable {
    String host;
    int port;
    String urlPrefix;
    boolean secure;

    DominoInfo(String host, int port, String urlPrefix, boolean secure) {
      this.host = host;
      this.port = port;
      this.urlPrefix = urlPrefix;
      this.secure= secure;
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

  private static final DominoInfo egenconsultingInfo =
    new DominoInfo("t1.egenconsulting.com", 80, "/servlet/Freetime", false);

  private static final DominoInfo showcase2Info =
    new DominoInfo("showcase2.notes.net", 443, "/servlet/Freetime", true);

  private static final HashMap<String, DominoInfo> serversInfo =
    new HashMap<String, DominoInfo>();

  static {
    serversInfo.put("egenconsulting", egenconsultingInfo);
    serversInfo.put("showcase2", showcase2Info);

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

  private SystemProperties sysProperties = new SystemProperties();

  //private String urlPrefix;

  public void init(HttpServletRequest req,
                   String account,
                   CalDAVConfig conf,
                   boolean debug) throws WebdavException {
    try {
      this.debug = debug;
      this.currentPrincipal = new User(account);

      trans = new IcalTranslator(new SAICalCallback(null),
                                 debug);
      urlHandler = new UrlHandler(req, false);

      CalTimezones.setDefaultTzid(defaultTimezone);
    } catch (Throwable t) {
      throw new WebdavException(t);
    }
  }

  /* (non-Javadoc)
   * @see org.bedework.caldav.server.sysinterface.SysIntf#getSystemProperties()
   */
  public SystemProperties getSystemProperties() throws WebdavException {
    return sysProperties;
  }

  /* (non-Javadoc)
   * @see org.bedework.caldav.server.SysIntf#getPrincipal()
   */
  public AccessPrincipal getPrincipal() throws WebdavException {
    return currentPrincipal;
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
   * @see org.bedework.caldav.server.SysIntf#getPrincipal(java.lang.String)
   */
  public AccessPrincipal getPrincipal(String href) throws WebdavException {
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
      String who = null;
      int whoType;

      if (uri.startsWith(userRoot)) {
        start = userRoot.length();
        whoType = Ace.whoTypeUser;
      } else if (uri.startsWith(groupRoot)) {
        start = groupRoot.length();
        whoType = Ace.whoTypeGroup;
      } else {
        throw new WebdavNotFound(uri);
      }

      if (start == end) {
        // Trying to browse user principals?
      } else if (uri.charAt(start) != '/') {
        throw new WebdavNotFound(uri);
      } else {
        who = uri.substring(start + 1, end);
      }

      AccessPrincipal ap = null;

      if (who != null) {
        if (whoType == Ace.whoTypeUser) {
          ap = new User(who);
          ap.setPrincipalRef(uri);
        } else if (whoType == Ace.whoTypeGroup) {
          ap = new Group(who);
          ap.setPrincipalRef(uri);
        }
      }

      return ap;
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

  /* (non-Javadoc)
   * @see org.bedework.caldav.server.SysIntf#caladdrToPrincipal(java.lang.String)
   */
  public AccessPrincipal caladdrToPrincipal(String caladdr) throws WebdavException {
    return new User(caladdr);
  }

  /* (non-Javadoc)
   * @see org.bedework.caldav.server.SysIntf#userToCaladdr(java.lang.String)
   */
  public String userToCaladdr(String account) throws WebdavException {
    return account;
  }

  /* (non-Javadoc)
   * @see org.bedework.caldav.server.SysIntf#getCalPrincipalInfo(edu.rpi.cmt.access.AccessPrincipal)
   */
  public CalPrincipalInfo getCalPrincipalInfo(AccessPrincipal principal) throws WebdavException {
    return new CalPrincipalInfo(principal,
                                null, null, null, null);
  }

  public Collection<String> getPrincipalCollectionSet(String resourceUri)
          throws WebdavException {
    throw new WebdavException("unimplemented");
  }

  public Collection<CalPrincipalInfo> getPrincipals(String resourceUri,
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

  /* (non-Javadoc)
   * @see org.bedework.caldav.server.SysIntf#getFreebusySet()
   */
  public Collection<String> getFreebusySet() throws WebdavException {
    throw new WebdavException("unimplemented");
  }

  /* (non-Javadoc)
   * @see org.bedework.caldav.server.SysIntf#schedule(org.bedework.caldav.server.CalDAVEvent)
   */
  public Collection<SchedRecipientResult> schedule(CalDAVEvent ev) throws WebdavException {
    throw new WebdavException("unimplemented");
  }

  /* ====================================================================
   *                   Events
   * ==================================================================== */

  /* (non-Javadoc)
   * @see org.bedework.caldav.server.SysIntf#addEvent(org.bedework.caldav.server.CalDAVEvent, boolean, boolean)
   */
  public Collection<CalDAVEvent> addEvent(CalDAVEvent ev,
                                           boolean noInvites,
                                           boolean rollbackOnError) throws WebdavException {
    throw new WebdavException("unimplemented");
  }

  /* (non-Javadoc)
   * @see org.bedework.caldav.server.SysIntf#updateEvent(org.bedework.caldav.server.CalDAVEvent)
   */
  public void updateEvent(CalDAVEvent event) throws WebdavException {
    throw new WebdavException("unimplemented");
  }

  /* (non-Javadoc)
   * @see org.bedework.caldav.server.SysIntf#getEvents(org.bedework.caldav.server.CalDAVCollection, org.bedework.calfacade.filter.BwFilter, org.bedework.caldav.server.SysIntf.RetrievalMode)
   */
  public Collection<CalDAVEvent> getEvents(CalDAVCollection col,
                                           BwFilter filter,
                                           RetrievalMode recurRetrieval)
          throws WebdavException {
    throw new WebdavException("unimplemented");
  }

  /* (non-Javadoc)
   * @see org.bedework.caldav.server.SysIntf#getEvent(org.bedework.caldav.server.CalDAVCollection, java.lang.String, org.bedework.caldav.server.SysIntf.RetrievalMode)
   */
  public CalDAVEvent getEvent(CalDAVCollection col, String val,
                              RetrievalMode recurRetrieval)
              throws WebdavException {
    throw new WebdavException("unimplemented");
  }

  /* (non-Javadoc)
   * @see org.bedework.caldav.server.SysIntf#deleteEvent(org.bedework.caldav.server.CalDAVEvent, boolean)
   */
  public void deleteEvent(CalDAVEvent ev,
                          boolean scheduleReply) throws WebdavException {
    throw new WebdavException("unimplemented");
  }

  /* (non-Javadoc)
   * @see org.bedework.caldav.server.SysIntf#deleteCollection(org.bedework.caldav.server.CalDAVCollection)
   */
  public void deleteCollection(CalDAVCollection col) throws WebdavException {
    throw new WebdavException("unimplemented");
  }

  /* (non-Javadoc)
   * @see org.bedework.caldav.server.SysIntf#requestFreeBusy(org.bedework.caldav.server.CalDAVEvent)
   */
  public Collection<SchedRecipientResult> requestFreeBusy(CalDAVEvent ev) throws WebdavException {
    throw new WebdavException("unimplemented");
  }

  /* (non-Javadoc)
   * @see org.bedework.caldav.server.sysinterface.SysIntf#getSpecialFreeBusy(java.lang.String, java.lang.String, org.bedework.caldav.server.PostMethod.RequestPars, org.bedework.caldav.util.TimeRange, java.io.Writer)
   */
  public void getSpecialFreeBusy(String cua, String user,
                                 RequestPars pars,
                                 TimeRange tr,
                                 Writer wtr) throws WebdavException {
    throw new WebdavException("unimplemented");
  }

  /* (non-Javadoc)
   * @see org.bedework.caldav.server.sysinterface.SysIntf#getFreeBusy(org.bedework.caldav.server.CalDAVCollection, int, java.lang.String, org.bedework.caldav.util.TimeRange)
   */
  public Calendar getFreeBusy(final CalDAVCollection col,
                              final int depth,
                              final String account,
                              final TimeRange timeRange) throws WebdavException {
    /* Create a url something like:
     *  http://t1.egenconsulting.com:80/servlet/Freetime/John?start-min=2006-07-11T12:00:00Z&start-max=2006-07-16T12:00:00Z
     */
    try {
      String serviceName = getServiceName(col.getPath());

      DominoInfo di = serversInfo.get(serviceName);
      if (di == null) {
        throw new WebdavBadRequest("Unknwon service: " + serviceName);
      }

      DavReq req = new DavReq();

      req.setMethod("GET");
      req.setUrl(di.getUrlPrefix() + "/" +
                 col.getOwner().getPrincipalRef() + "?" +
                 "start-min=" + makeDateTime(timeRange.getStart()) + "&" +
                 "start-max=" + makeDateTime(timeRange.getEnd()));

      req.addHeader("Accept",
                    "text/xml,application/xml,application/xhtml+xml,text/html;q=0.9,text/plain;q=0.8,image/png,*/*;q=0.5");
      req.addHeader("Accept-Language", "en-us,en;q=0.7,es;q=0.3");
      req.addHeader("Accept-Encoding", "gzip,deflate");
      req.addHeader("Accept-Charset", "ISO-8859-1,utf-8;q=0.7,*;q=0.7");
      DavResp resp = send(req, di);

      /* He switched to XML! - parse back to a vfreebusy object */

      String vfb = makeVfb(new InputStreamReader(resp.getContentStream()));

      if (debug) {
        debugMsg(vfb);
      }

      Icalendar ic = trans.fromIcal(null, new StringReader(vfb));

      /* Domino returns free time - invert to get busy time
       * First we'll order all the periods in the result.
       */

      TreeSet<Period> periods = new TreeSet<Period>();
      Iterator fbit = ic.iterator();
      while (fbit.hasNext()) {
        Object o = fbit.next();

        if (o instanceof BwEvent) {
          BwEvent fb = (BwEvent)o;

          Collection<BwFreeBusyComponent> times = fb.getFreeBusyPeriods();

          if (times != null) {
            for (BwFreeBusyComponent fbcomp: times) {
              if (fbcomp.getType() != BwFreeBusyComponent.typeFree) {
                throw new WebdavServerError();
              }

              for (Period p: fbcomp.getPeriods()) {
                periods.add(p);
              }
            }
          }
        }
      }

      BwEvent fb = new BwEventObj();

      fb.setEntityType(CalFacadeDefs.entityTypeFreeAndBusy);
      fb.setDtstart(getBwDt(timeRange.getStart()));
      fb.setDtend(getBwDt(timeRange.getEnd()));

      BwFreeBusyComponent fbcomp = new BwFreeBusyComponent();

      fb.addFreeBusyPeriod(fbcomp);

      fbcomp.setType(BwFreeBusyComponent.typeBusy);

      /* Fill in the gaps between the free periods with busy time. */

      DateTime bstart = timeRange.getStart();

      for (Period p: periods) {
        if (!bstart.equals(p.getStart())) {
          /* First free period may be at start of requested time */
          Period busyp = new Period(bstart, p.getStart());
          fbcomp.addPeriod(busyp.getStart(), busyp.getEnd());
        }

        bstart = p.getEnd();
      }

      /* Fill in to end of requested period */
      DateTime bend = timeRange.getEnd();

      if (!bstart.equals(bend)) {
        Period busyp = new Period(bstart, bend);
        fbcomp.addPeriod(busyp.getStart(), busyp.getEnd());
      }

      VFreeBusy vfreeBusy = VFreeUtil.toVFreeBusy(fb);
      if (vfreeBusy != null) {
        Calendar ical = IcalTranslator.newIcal(Icalendar.methodTypeNone);
        ical.getComponents().add(vfreeBusy);

        return ical;
      }

      return null;
    } catch (WebdavException wde) {
      throw wde;
    } catch (Throwable t) {
      throw new WebdavException(t);
    }
  }

  /* (non-Javadoc)
   * @see org.bedework.caldav.server.SysIntf#checkAccess(edu.rpi.cct.webdav.servlet.shared.WdEntity, int, boolean)
   */
  public CurrentAccess checkAccess(WdEntity ent,
                                   int desiredAccess,
                                   boolean returnResult)
          throws WebdavException {
    throw new WebdavException("unimplemented");
  }

  public void updateAccess(CalDAVCollection col,
                           Acl acl) throws WebdavException {
    throw new WebdavException("unimplemented");
  }

  /* (non-Javadoc)
   * @see org.bedework.caldav.server.SysIntf#updateAccess(org.bedework.caldav.server.CalDAVEvent, edu.rpi.cmt.access.Acl)
   */
  public void updateAccess(CalDAVEvent ev,
                           Acl acl) throws WebdavException{
    throw new WebdavException("unimplemented");
  }

  /* (non-Javadoc)
   * @see org.bedework.caldav.server.SysIntf#newCollectionObject(boolean, java.lang.String)
   */
  public CalDAVCollection newCollectionObject(boolean calendarCollection,
                                              String parentPath) throws WebdavException {
    throw new WebdavException("unimplemented");
  }

  public int makeCollection(CalDAVCollection col) throws WebdavException {
    throw new WebdavException("unimplemented");
  }

  /* (non-Javadoc)
   * @see org.bedework.caldav.server.SysIntf#copyMove(org.bedework.calfacade.BwCalendar, org.bedework.calfacade.BwCalendar, boolean)
   */
  public void copyMove(CalDAVCollection from,
                       CalDAVCollection to,
                       boolean copy,
                       boolean overwrite) throws WebdavException {
    throw new WebdavException("unimplemented");
  }

  /* (non-Javadoc)
   * @see org.bedework.caldav.server.SysIntf#copyMove(org.bedework.caldav.server.CalDAVEvent, org.bedework.caldav.server.CalDAVCollection, java.lang.String, boolean, boolean)
   */
  public boolean copyMove(CalDAVEvent from,
                          CalDAVCollection to,
                          String name,
                          boolean copy,
                          boolean overwrite) throws WebdavException {
    throw new WebdavException("unimplemented");
  }

  public CalDAVCollection getCollection(String path) throws WebdavException {
    // XXX Just fake it up for the moment.
    /* The path should always start with /server-name/user
     */

    List<String> l = splitUri(path, true);

    String namePart = (String)l.get(l.size() - 1);

    CalDAVCollectionBase col = new CalDAVCollectionBase(CalDAVCollection.calTypeCalendarCollection,
                                                        true);
    col.setName(namePart);
    col.setPath(path);

    String owner = (String)l.get(1);

    col.setOwner(new User(owner));

    return col;
  }

  /* (non-Javadoc)
   * @see org.bedework.caldav.server.SysIntf#updateCalendar(org.bedework.calfacade.BwCalendar)
   */
  public void updateCollection(CalDAVCollection col) throws WebdavException {
    throw new WebdavException("unimplemented");
  }

  public Collection<CalDAVCollection> getCollections(CalDAVCollection col) throws WebdavException {
    throw new WebdavException("unimplemented");
  }

  /* (non-Javadoc)
   * @see org.bedework.caldav.server.SysIntf#resolveAlias(org.bedework.caldav.server.CalDAVCollection)
   */
  public void resolveAlias(CalDAVCollection col) throws WebdavException {
    throw new WebdavException("unimplemented");
  }

  /* ====================================================================
   *                   Files
   * ==================================================================== */

  /* (non-Javadoc)
   * @see org.bedework.caldav.server.SysIntf#newResourceObject(java.lang.String)
   */
  public CalDAVResource newResourceObject(String parentPath) throws WebdavException {
    throw new WebdavException("unimplemented");
  }

  /* (non-Javadoc)
   * @see org.bedework.caldav.server.SysIntf#putFile(org.bedework.caldav.server.CalDAVCollection, org.bedework.caldav.server.CalDAVResource)
   */
  public void putFile(CalDAVCollection col,
                      CalDAVResource val) throws WebdavException {
    throw new WebdavException("unimplemented");
  }

  /* (non-Javadoc)
   * @see org.bedework.caldav.server.SysIntf#getFile(org.bedework.caldav.server.CalDAVCollection, java.lang.String)
   */
  public CalDAVResource getFile(CalDAVCollection col,
                                String name) throws WebdavException {
    throw new WebdavException("unimplemented");
  }

  /* (non-Javadoc)
   * @see org.bedework.caldav.server.SysIntf#getFileContent(org.bedework.caldav.server.CalDAVResource)
   */
  public void getFileContent(CalDAVResource val) throws WebdavException {
    throw new WebdavException("unimplemented");
  }

  /* (non-Javadoc)
   * @see org.bedework.caldav.server.SysIntf#getFiles(org.bedework.caldav.server.CalDAVCollection)
   */
  public Collection<CalDAVResource> getFiles(CalDAVCollection coll) throws WebdavException {
    throw new WebdavException("unimplemented");
  }

  /* (non-Javadoc)
   * @see org.bedework.caldav.server.SysIntf#updateFile(org.bedework.caldav.server.CalDAVResource, boolean)
   */
  public void updateFile(CalDAVResource val,
                         boolean updateContent) throws WebdavException {
    throw new WebdavException("unimplemented");
  }

  /* (non-Javadoc)
   * @see org.bedework.caldav.server.SysIntf#deleteFile(org.bedework.caldav.server.CalDAVResource)
   */
  public void deleteFile(CalDAVResource val) throws WebdavException {
    throw new WebdavException("unimplemented");
  }

  /* (non-Javadoc)
   * @see org.bedework.caldav.server.SysIntf#copyMoveFile(org.bedework.caldav.server.CalDAVResource, java.lang.String, java.lang.String, boolean, boolean)
   */
  public boolean copyMoveFile(CalDAVResource from,
                              String toPath,
                              String name,
                              boolean copy,
                              boolean overwrite) throws WebdavException {
    throw new WebdavException("unimplemented");
  }

  /* (non-Javadoc)
   * @see org.bedework.caldav.server.SysIntf#toCalendar(org.bedework.caldav.server.CalDAVEvent)
   */
  public Calendar toCalendar(CalDAVEvent ev) throws WebdavException {
    throw new WebdavException("unimplemented");
  }

  /* (non-Javadoc)
   * @see org.bedework.caldav.server.SysIntf#writeCalendar(java.util.Collection, int, java.io.Writer)
   */
  public void writeCalendar(Collection<CalDAVEvent> evs,
                            int method,
                            Writer wtr) throws WebdavException {
    throw new WebdavException("unimplemented");
  }

  public SysiIcalendar fromIcal(CalDAVCollection col,
                            Reader rdr) throws WebdavException {
    throw new WebdavException("unimplemented");
  }

  public String toStringTzCalendar(String tzid) throws WebdavException {
    throw new WebdavException("unimplemented");
  }

  public String tzidFromTzdef(String val) throws WebdavException {
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

  /* <?xml version="1.0" encoding="UTF-8"?>
   <iCalendar>
   <vcalendar method="REPLY" version="2.0" prodid="-//IBM Domino Freetime//NONSGML Prototype//EN">
   <vfreebusy>
   <attendee>John</attendee>
   <url>http://t1.egenconsulting.com:80/servlet/Freetime/John</url>
   <dtstamp>20060713T185253Z</dtstamp>
   <dtstart>20060717T030000Z</dtstart>
   <dtend>20060723T030000Z</dtend>
   <freebusy fbtype="FREE">20060717T130000Z/20060717T160000Z</freebusy>
   <freebusy fbtype="FREE">20060717T170000Z/20060717T210000Z</freebusy>
   <freebusy fbtype="FREE">20060718T130000Z/20060718T160000Z</freebusy>
   <freebusy fbtype="FREE">20060718T170000Z/20060718T210000Z</freebusy>
   <freebusy fbtype="FREE">20060719T130000Z/20060719T160000Z</freebusy>
   <freebusy fbtype="FREE">20060719T170000Z/20060719T210000Z</freebusy>
   <freebusy fbtype="FREE">20060720T130000Z/20060720T160000Z</freebusy>
   <freebusy fbtype="FREE">20060720T170000Z/20060720T210000Z</freebusy>
   <freebusy fbtype="FREE">20060721T130000Z/20060721T160000Z</freebusy>
   <freebusy fbtype="FREE">20060721T170000Z/20060721T210000Z</freebusy>
   </vfreebusy>

   </vcalendar>
   </iCalendar>
   */
  private String makeVfb(Reader rdr) throws WebdavException{
    try {
      DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
      factory.setNamespaceAware(true);

      DocumentBuilder builder = factory.newDocumentBuilder();

      Document doc = builder.parse(new InputSource(rdr));

      StringBuffer sb = new StringBuffer();

      sb.append("BEGIN:VCALENDAR\n");
      sb.append("VERSION:2.0\n");
      sb.append("PRODID:-//Bedework Domino/caldav interface//EN\n");
      sb.append("BEGIN:VFREEBUSY\n");

      Element root = doc.getDocumentElement(); // </iCalendar>
      Element child = getOnlyChild(root);  //  </vcalendar>

      child = getOnlyChild(child);  //  </vfreebusy>

      Element[] children = getChildren(child);

      for (int i = 0; i < children.length; i++) {
        Element curnode = children[i];

        String nm = curnode.getLocalName();

        if (nm.equals("attendee")) {
          sb.append("ATTENDEE:");
          sb.append(getElementContent(curnode));
          sb.append("\n");
        } else if (nm.equals("url")) {
          sb.append("URL:");
          sb.append(getElementContent(curnode));
          sb.append("\n");
        } else if (nm.equals("dtstamp")) {
          sb.append("DTSTAMP:");
          sb.append(getElementContent(curnode));
          sb.append("\n");
        } else if (nm.equals("dtstart")) {
          sb.append("DTSTART:");
          sb.append(getElementContent(curnode));
          sb.append("\n");
        } else if (nm.equals("dtend")) {
          sb.append("DTEND:");
          sb.append(getElementContent(curnode));
          sb.append("\n");
        } else if (nm.equals("freebusy")) {
          sb.append("FREEBUSY;FBTYPE=FREE:");
          sb.append(getElementContent(curnode));
          sb.append("\n");
        }
      }
      sb.append("END:VFREEBUSY\n");
      sb.append("END:VCALENDAR\n");

      return sb.toString();
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

  private String makeDateTime(DateTime dt) throws WebdavException {
    try {
      /*
      String utcdt = dt.getDate();

      StringBuffer sb = new StringBuffer();

      // from 20060716T120000Z make 2006-07-16T12:00:00Z
      //      0   4 6    1 3
      sb.append(utcdt.substring(0, 4));
      sb.append("-");
      sb.append(utcdt.substring(4, 6));
      sb.append("-");
      sb.append(utcdt.substring(6, 11));
      sb.append(":");
      sb.append(utcdt.substring(11, 13));
      sb.append(":");
      sb.append(utcdt.substring(13));

      return sb.toString();
      */
      return dt.toString();
    } catch (Throwable t) {
      throw new WebdavException(t);
    }
  }

  private BwDateTime getBwDt(DateTime dt) throws WebdavException {
    try {
      if (dt == null) {
        return null;
      }

      BwDateTime bwdt = new BwDateTime();
      bwdt.init(false, dt.toString(), null, null);

      return bwdt;
    } catch (Throwable t) {
      throw new WebdavException(t);
    }
  }

  private List splitUri(String uri, boolean decoded) throws WebdavException {
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

      List l = Arrays.asList(ss);
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
  private DavResp send(DavReq r, DominoInfo di) throws Throwable {
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
