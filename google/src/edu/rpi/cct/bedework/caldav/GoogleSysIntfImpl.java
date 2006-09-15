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
import org.bedework.calfacade.BwFreeBusyComponent;
import org.bedework.calfacade.BwUser;
import org.bedework.calfacade.ScheduleResult;
import org.bedework.calfacade.base.BwShareableDbentity;
import org.bedework.calfacade.timezones.CalTimezones;
import org.bedework.calfacade.timezones.ResourceTimezones;
import org.bedework.icalendar.Icalendar;

import edu.rpi.cct.webdav.servlet.common.WebdavUtils;
import edu.rpi.cct.webdav.servlet.shared.PrincipalPropertySearch;
import edu.rpi.cct.webdav.servlet.shared.WebdavException;
import edu.rpi.cct.webdav.servlet.shared.WebdavIntfException;
import edu.rpi.cmt.access.Acl.CurrentAccess;

import net.fortuna.ical4j.model.Calendar;
import net.fortuna.ical4j.model.Period;
import net.fortuna.ical4j.model.TimeZone;

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
import java.net.URL;
import java.util.Collection;
import java.util.Iterator;

import javax.servlet.http.HttpServletRequest;

/** Bedework implementation of SysIntf.
 *
 * @author Mike Douglass douglm at rpi.edu
 */
public class GoogleSysIntfImpl implements SysIntf {
  private static final String feedUrlPrefix =
    "http://www.google.com/calendar/feeds/";

  private boolean debug;

  private ResourceTimezones timezones;

  // XXX get from properties
  private static String defaultTimezone = "America/New_York";

  private transient Logger log;

  private String urlPrefix;

  public void init(HttpServletRequest req,
                   String envPrefix,
                   String account,
                   boolean debug) throws WebdavIntfException {
    try {
      this.debug = debug;
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

  public void updateEvent(BwEvent event) throws WebdavIntfException {
    throw new WebdavIntfException("unimplemented");
  }

  public Collection getEventsExpanded(BwCalendar cal) throws WebdavIntfException {
    throw new WebdavIntfException("unimplemented");
  }

  public Collection getEvents(BwCalendar cal,
                              int recurRetrieval) throws WebdavIntfException {
    throw new WebdavIntfException("unimplemented");
  }

  public Collection getEvents(BwCalendar cal,
                              BwDateTime startDate, BwDateTime endDate,
                              int recurRetrieval) throws WebdavIntfException {
    throw new WebdavIntfException("unimplemented");
  }

  public Collection findEventsByName(BwCalendar cal, String val)
              throws WebdavIntfException {
    throw new WebdavIntfException("unimplemented");
  }

  public void deleteEvent(BwEvent ev) throws WebdavIntfException {
    throw new WebdavIntfException("unimplemented");
  }

  public BwFreeBusy getFreeBusy(BwCalendar cal,
                                String account,
                                BwDateTime start,
                                BwDateTime end) throws WebdavException {
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
      q.setMinimumStartTime(makeDateTime(start));
      q.setMaximumStartTime(makeDateTime(end));

      CalendarService svc = getCalendarService();

      //       Send the request and receive the response:
      Feed resultFeed = (Feed)svc.query(q, Feed.class);

      BwFreeBusy fb = new BwFreeBusy(new BwUser(account), start, end);

      Iterator entries = resultFeed.getEntries().iterator();
      while (entries.hasNext()) {
        Entry e = (Entry)entries.next();

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

        Iterator times = ev.getTimes().iterator();
        while (times.hasNext()) {
          When w = (When)times.next();

          net.fortuna.ical4j.model.DateTime icalStart =
            makeIcalDateTime(w.getStartTime());

          net.fortuna.ical4j.model.DateTime icalEnd =
            makeIcalDateTime(w.getEndTime());

          Period per = new Period(icalStart, icalEnd);

          fbc.addPeriod(per);

          if (debug) {
            trace("Event entry: period " + per);
          }
        }

        fb.addTime(fbc);
      }

      return fb;
    } catch (Throwable t) {
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

    int pos = path.lastIndexOf("/");
    if (pos < 0) {
      // bad uri
      throw WebdavIntfException.badRequest();
    }

    String namePart = path.substring(pos + 1);

    BwCalendar cal = new BwCalendar();
    cal.setName(namePart);
    cal.setPath(path);

    String owner;

    if (pos == 0) {
      owner = namePart;
    } else {
      int endName = path.indexOf("/", 1);
      owner = path.substring(1, endName);
    }

    cal.setOwner(new BwUser(owner));

    return cal;
  }

  public Collection getCalendars(BwCalendar cal) throws WebdavIntfException {
    throw new WebdavIntfException("unimplemented");
  }

  public Calendar toCalendar(BwEvent ev) throws WebdavIntfException {
    throw new WebdavIntfException("unimplemented");
  }

  public Calendar toCalendar(Collection evs) throws WebdavIntfException {
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

  private CalendarService getCalendarService() {
    return new CalendarService("org.bedework-caldav-1");
  }

  private DateTime makeDateTime(BwDateTime dt) throws WebdavIntfException {
    try {
      TimeZone tz = getTimezones().getTimeZone(dt.getTzid());
      long millis = dt.makeDate().getTime();
      return new DateTime(millis, tz.getOffset(millis) / 60000);
    } catch (Throwable t) {
      throw new WebdavIntfException(t);
    }
  }

  private net.fortuna.ical4j.model.DateTime makeIcalDateTime(DateTime val)
          throws WebdavIntfException {
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
      throw new WebdavIntfException(t);
    }
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
