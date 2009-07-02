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
package edu.rpi.cct.bedework.caldav.google;

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
import org.bedework.caldav.util.TimeRange;
import org.bedework.calfacade.BwDateTime;
import org.bedework.calfacade.BwEvent;
import org.bedework.calfacade.BwEventObj;
import org.bedework.calfacade.BwFreeBusyComponent;
import org.bedework.calfacade.CalFacadeDefs;
import org.bedework.calfacade.configs.CalDAVConfig;
import org.bedework.calfacade.filter.BwFilter;
import org.bedework.calfacade.timezones.CalTimezones;
import org.bedework.icalendar.IcalTranslator;
import org.bedework.icalendar.Icalendar;
import org.bedework.icalendar.VFreeUtil;

import edu.rpi.cct.bedework.caldav.exchange.Group;
import edu.rpi.cct.bedework.caldav.exchange.User;
import edu.rpi.cct.webdav.servlet.shared.PrincipalPropertySearch;
import edu.rpi.cct.webdav.servlet.shared.WdEntity;
import edu.rpi.cct.webdav.servlet.shared.WebdavBadRequest;
import edu.rpi.cct.webdav.servlet.shared.WebdavException;
import edu.rpi.cct.webdav.servlet.shared.WebdavNotFound;
import edu.rpi.cct.webdav.servlet.shared.WebdavNsNode.PropertyTagEntry;
import edu.rpi.cct.webdav.servlet.shared.WebdavNsNode.UrlHandler;
import edu.rpi.cmt.access.AccessPrincipal;
import edu.rpi.cmt.access.Ace;
import edu.rpi.cmt.access.Acl;
import edu.rpi.cmt.access.Acl.CurrentAccess;

import net.fortuna.ical4j.model.Calendar;
import net.fortuna.ical4j.model.component.VFreeBusy;

import org.apache.log4j.Logger;

import com.google.gdata.client.calendar.CalendarQuery;
import com.google.gdata.client.calendar.CalendarService;
import com.google.gdata.data.DateTime;
import com.google.gdata.data.Entry;
import com.google.gdata.data.Feed;
import com.google.gdata.data.extensions.EventEntry;
import com.google.gdata.data.extensions.When;
import com.google.gdata.data.extensions.EventEntry.EventStatus;
import com.google.gdata.data.extensions.EventEntry.Transparency;

import java.io.Reader;
import java.io.Writer;
import java.net.URI;
import java.net.URL;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.xml.namespace.QName;

/** Bedework implementation of SysIntf.
 *
 * @author Mike Douglass douglm at rpi.edu
 */
public class GoogleSysIntfImpl implements SysIntf {
  private static final String feedUrlPrefix =
    "http://www.google.com/calendar/feeds/";

  private boolean debug;

  // XXX get from properties
  private static String defaultTimezone = "America/Los_Angeles";

  private transient Logger log;

  private UrlHandler urlHandler;

  private SystemProperties sysProperties = new SystemProperties();

  protected AccessPrincipal currentPrincipal;

  private static HashMap<String, Integer> toWho = new HashMap<String, Integer>();
  private static HashMap<Integer, String> fromWho = new HashMap<Integer, String>();

  static {
    initWhoMaps("/principals/users", Ace.whoTypeUser);
    initWhoMaps("/principals/groups", Ace.whoTypeGroup);
    initWhoMaps("/principals/tickets", Ace.whoTypeTicket);
    initWhoMaps("/principals/resources", Ace.whoTypeResource);
    initWhoMaps("/principals/venues", Ace.whoTypeVenue);
    initWhoMaps("/principals/hosts", Ace.whoTypeHost);
  }

  public void init(HttpServletRequest req,
                   String account,
                   CalDAVConfig conf,
                   boolean debug) throws WebdavException {
    try {
      this.debug = debug;
      this.currentPrincipal = new User(account);
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
  public void updateEvent(CalDAVEvent ev) throws WebdavException {
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
    /* We get something like:
     *
<feed>
-
  <id>
    http://www.google.com/calendar/feeds/MikeADouglass%40gmail.com/public/free-busy
  </id>
  <updated>2006-07-06T19:49:37.000Z</updated>
  <title type="text">Mike Douglass</title>
  <subtitle type="text">Mike Douglass</subtitle>
  <link rel="http://schemas.google.com/g/2005#feed" type="application/atom+xml" href="http://www.google.com/calendar/feeds/MikeADouglass%40gmail.com/public/free-busy"/>
  <link rel="self" type="application/atom+xml" href="http://www.google.com/calendar/feeds/MikeADouglass%40gmail.com/public/free-busy?max-results=25"/>
  <link rel="next" type="application/atom+xml" href="http://www.google.com/calendar/feeds/MikeADouglass%40gmail.com/public/free-busy?start-index=26&max-results=25"/>
  -
  <author>
    <name>Mike Douglass</name>
    <email>mikeadouglass@gmail.com</email>
  </author>
  <generator version="1.0" uri="http://www.google.com/calendar">Google Calendar</generator>
  <openSearch:itemsPerPage>25</openSearch:itemsPerPage>
  <gCal:timezone value="America/New_York"/>
  -
  <entry>
    -
    <id>
      http://www.google.com/calendar/feeds/MikeADouglass%40gmail.com/public/free-busy/ae497f7ae5577dd899262ce1a6881fe2592f937c
    </id>
    <published>1970-01-01T00:00:00.000Z</published>
    <updated>2006-07-06T19:32:56.000Z</updated>
    <category scheme="http://schemas.google.com/g/2005#kind" term="http://schemas.google.com/g/2005#event"/>
    <link rel="alternate" type="text/html" href="http://www.google.com/calendar/event?eid=YWU0OTdmN2FlNTU3N2RkODk5MjYyY2UxYTY4ODFmZTI1OTJmOTM3YyBtaWtlYWRvdWdsYXNzQGdtYWlsLmNvbQ" title="alternate"/>
    <link rel="self" type="application/atom+xml" href="http://www.google.com/calendar/feeds/MikeADouglass%40gmail.com/public/free-busy/ae497f7ae5577dd899262ce1a6881fe2592f937c"/>
    -
    <gd:when startTime="2006-06-25T09:30:00.000-04:00" endTime="2006-06-25T22:30:00.000-04:00">
      <gd:reminder minutes="10"/>
    </gd:when>
  </entry>
</feed>
     */
    try {
      URL feedUrl = new URL(feedUrlPrefix + account + "@gmail.com/public/free-busy");

      CalendarQuery q = new CalendarQuery(feedUrl);
      q.setMinimumStartTime(new DateTime(timeRange.getStart().getTime()));
      q.setMaximumStartTime(new DateTime(timeRange.getEnd().getTime()));

      CalendarService svc = getCalendarService();

      //       Send the request and receive the response:
      Feed resultFeed = (Feed)svc.query(q, Feed.class);

      BwEvent fb = new BwEventObj();

      fb.setEntityType(CalFacadeDefs.entityTypeFreeAndBusy);
      fb.setOwnerHref(account);
      fb.setDtstart(getBwDt(timeRange.getStart()));
      fb.setDtend(getBwDt(timeRange.getEnd()));
      //assignGuid(fb);

      for (Entry e: resultFeed.getEntries()) {

        /* I should probably check the Category here
        if (!(o instanceof EventEntry)) {
          continue;
        }*/

        EventEntry ev = new EventEntry(e);

        if (debug) {
          trace("Found event entry");
        }

        Transparency tr = ev.getTransparency();
        if ((tr != null) && (tr.equals(Transparency.TRANSPARENT))) {
          continue;
        }

        EventStatus st = ev.getStatus();

        if ((st != null) && (st.equals(EventStatus.CANCELED))) {
          continue;
        }

        BwFreeBusyComponent fbc = new BwFreeBusyComponent();
        if (st == null) {
          fbc.setType(BwFreeBusyComponent.typeBusy);
        } else if (st.equals(EventStatus.CONFIRMED)) {
          fbc.setType(BwFreeBusyComponent.typeBusy);
        } else {
          fbc.setType(BwFreeBusyComponent.typeBusyTentative);
        }

        if (debug) {
          trace("Event entry: status " + fbc.getType());
        }

        for (When w: ev.getTimes()) {
          net.fortuna.ical4j.model.DateTime icalStart =
            makeIcalDateTime(w.getStartTime());

          net.fortuna.ical4j.model.DateTime icalEnd =
            makeIcalDateTime(w.getEndTime());

          fbc.addPeriod(icalStart, icalEnd);
        }

        fb.addFreeBusyPeriod(fbc);
      }

      VFreeBusy vfreeBusy = VFreeUtil.toVFreeBusy(fb);
      if (vfreeBusy != null) {
        Calendar ical = IcalTranslator.newIcal(Icalendar.methodTypeNone);
        ical.getComponents().add(vfreeBusy);

        return ical;
      }

      return null;
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

  public CalDAVCollection newCollectionObject(boolean isCalendarCollection,
                                              String parentPath) throws WebdavException {
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

  /* (non-Javadoc)
   * @see org.bedework.caldav.server.SysIntf#getCollection(java.lang.String)
   */
  public CalDAVCollection getCollection(String path) throws WebdavException {
    // XXX Just fake it up for the moment.

    int pos = path.lastIndexOf("/");
    if (pos < 0) {
      // bad uri
      throw new WebdavBadRequest("Bad uri:" + path);
    }

    String namePart = path.substring(pos + 1);

    String owner;

    if (pos == 0) {
      owner = namePart;
    } else {
      int endName = path.indexOf("/", 1);
      owner = path.substring(1, endName);
    }

    CalDAVCollectionBase col = new CalDAVCollectionBase(CalDAVCollection.calTypeCalendarCollection,
                                                        true);
    col.setName(namePart);
    col.setPath(path);

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

  /* (non-Javadoc)
   * @see org.bedework.caldav.server.SysIntf#fromIcal(org.bedework.caldav.server.CalDAVCollection, java.io.Reader)
   */
  public SysiIcalendar fromIcal(CalDAVCollection col, Reader rdr) throws WebdavException {
    throw new WebdavException("unimplemented");
  }

  /* (non-Javadoc)
   * @see org.bedework.caldav.server.SysIntf#toStringTzCalendar(java.lang.String)
   */
  public String toStringTzCalendar(String tzid) throws WebdavException {
    throw new WebdavException("unimplemented");
  }

  public String tzidFromTzdef(String val) throws WebdavException {
    throw new WebdavException("unimplemented");
  }

  /* (non-Javadoc)
   * @see org.bedework.caldav.server.SysIntf#getMaxUserEntitySize()
   */
  public int getMaxUserEntitySize() throws WebdavException {
    throw new WebdavException("unimplemented");
  }

  /* (non-Javadoc)
   * @see org.bedework.caldav.server.SysIntf#close()
   */
  public void close() throws WebdavException {
  }

  /* ====================================================================
   *                         Private methods
   * ==================================================================== */

  private CalendarService getCalendarService() {
    return new CalendarService("org.bedework-caldav-1");
  }

  private BwDateTime getBwDt(net.fortuna.ical4j.model.DateTime dt) throws WebdavException {
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

  private net.fortuna.ical4j.model.DateTime makeIcalDateTime(DateTime val)
          throws WebdavException {
    try {
      long millis = val.getValue();
      /*
      Integer offset = val.getTzShift();
      if (offset != null) {
        millis -= (offset.intValue() * 60000);
      }*/

      net.fortuna.ical4j.model.DateTime icaldt =
        new net.fortuna.ical4j.model.DateTime(true);
      icaldt.setTime(millis);
      return icaldt;
    } catch (Throwable t) {
      throw new WebdavException(t);
    }
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

  protected void error(Throwable t) {
    getLogger().error(this, t);
  }

  protected void logIt(String msg) {
    getLogger().info(msg);
  }
}
