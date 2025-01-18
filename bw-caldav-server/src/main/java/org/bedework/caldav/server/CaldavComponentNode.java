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

import org.bedework.access.AccessPrincipal;
import org.bedework.access.CurrentAccess;
import org.bedework.access.PrivilegeDefs;
import org.bedework.caldav.server.sysinterface.SysIntf;
import org.bedework.caldav.server.sysinterface.SysIntf.MethodEmitted;
import org.bedework.base.ToString;
import org.bedework.util.timezones.DateTimeUtil;
import org.bedework.util.xml.XmlEmit;
import org.bedework.util.xml.tagdefs.AppleServerTags;
import org.bedework.util.xml.tagdefs.CaldavTags;
import org.bedework.util.xml.tagdefs.ICalTags;
import org.bedework.webdav.servlet.shared.WebdavException;
import org.bedework.webdav.servlet.shared.WebdavNsIntf;

import net.fortuna.ical4j.model.Calendar;
import net.fortuna.ical4j.model.Component;
import org.w3c.dom.Element;

import java.io.Writer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;

import javax.xml.namespace.QName;

/** Class to represent an entity such as events in caldav.
 *
 *   @author Mike Douglass   douglm  rpi.edu
 */
public class CaldavComponentNode extends CaldavBwNode {
  /* The event if this component is an event */
  private CalDAVEvent<?> event;

  private AccessPrincipal owner;

  private CurrentAccess currentAccess;

  private String entityName;

  // We also need a task object and maybe a journal, freebusy and a timezone

  private boolean isTimezone;

  private Calendar ical;

  /** The event Component object
   */
  private Component comp;

  private String compContentType;
  private String compString;

  private final static HashMap<QName, PropertyTagEntry> propertyNames =
    new HashMap<>();

  static {
    addPropEntry(propertyNames, CaldavTags.calendarData);
    addPropEntry(propertyNames, CaldavTags.originator);
    addPropEntry(propertyNames, CaldavTags.recipient);
    addPropEntry(propertyNames, CaldavTags.scheduleTag, true);

    addPropEntry(propertyNames, AppleServerTags.scheduleChanges);

    //addPropEntry(propertyNames, ICalTags.action);      /*     *     *     *        *            *   VALARM */
    addPropEntry(propertyNames, ICalTags.attach);        /* VEVENT VTODO VJOURNAL    *            *   VALARM */
    addPropEntry(propertyNames, ICalTags.attendee);      /* VEVENT VTODO VJOURNAL VFREEBUSY       *   VALARM */
    //addPropEntry(propertyNames, ICalTags.calscale);    /*     *     *     *        *            *     *    CALENDAR*/
    addPropEntry(propertyNames, ICalTags.categories);    /* VEVENT VTODO VJOURNAL */
    addPropEntry(propertyNames, ICalTags._class);        /* VEVENT VTODO VJOURNAL */
    addPropEntry(propertyNames, ICalTags.comment);       /* VEVENT VTODO VJOURNAL VFREEBUSY VTIMEZONE */
    //addPropEntry(propertyNames, ICalTags.completed);   /*     *  VTODO */
    addPropEntry(propertyNames, ICalTags.contact);       /* VEVENT VTODO VJOURNAL VFREEBUSY */
    addPropEntry(propertyNames, ICalTags.created);       /* VEVENT VTODO VJOURNAL */
    addPropEntry(propertyNames, ICalTags.description);   /* VEVENT VTODO VJOURNAL    *            *   VALARM */
    addPropEntry(propertyNames, ICalTags.dtend);         /* VEVENT    *     *     VFREEBUSY */
    addPropEntry(propertyNames, ICalTags.dtstamp);       /* VEVENT VTODO VJOURNAL VFREEBUSY */
    addPropEntry(propertyNames, ICalTags.dtstart);       /* VEVENT VTODO VJOURNAL VFREEBUSY VTIMEZONE */
    //addPropEntry(propertyNames, ICalTags.due);         /*     *  VTODO */
    addPropEntry(propertyNames, ICalTags.duration);      /* VEVENT VTODO    *     VFREEBUSY       *   VALARM */
    addPropEntry(propertyNames, ICalTags.exdate);        /* VEVENT VTODO VJOURNAL    *      VTIMEZONE */
    addPropEntry(propertyNames, ICalTags.exrule);        /* VEVENT VTODO VJOURNAL */
    //addPropEntry(propertyNames, ICalTags.freebusy);    /*     *     *     *     VFREEBUSY */
    addPropEntry(propertyNames, ICalTags.geo);           /* VEVENT VTODO */
    //addPropEntry(propertyNames, ICalTags.hasAlarm);
    //addPropEntry(propertyNames, ICalTags.hasAttachment);
    //addPropEntry(propertyNames, ICalTags.hasRecurrence);
    addPropEntry(propertyNames, ICalTags.lastModified);  /* VEVENT VTODO VJOURNAL    *      VTIMEZONE */
    addPropEntry(propertyNames, ICalTags.location);      /* VEVENT VTODO */
    addPropEntry(propertyNames, ICalTags.organizer);     /* VEVENT VTODO VJOURNAL VFREEBUSY */
    //addPropEntry(propertyNames, ICalTags.percentComplete);  /*     *  VTODO */
    addPropEntry(propertyNames, ICalTags.priority);      /* VEVENT VTODO */
    addPropEntry(propertyNames, ICalTags.rdate);         /* VEVENT VTODO VJOURNAL    *      VTIMEZONE */
    addPropEntry(propertyNames, ICalTags.recurrenceId);  /* VEVENT VTODO VJOURNAL    *      VTIMEZONE */
    addPropEntry(propertyNames, ICalTags.relatedTo);     /* VEVENT VTODO VJOURNAL */
    //addPropEntry(propertyNames, ICalTags.repeat);      /*     *     *     *        *            *   VALARM */
    addPropEntry(propertyNames, ICalTags.resources);     /* VEVENT VTODO */
    addPropEntry(propertyNames, ICalTags.requestStatus); /* VEVENT VTODO VJOURNAL VFREEBUSY */
    addPropEntry(propertyNames, ICalTags.rrule);         /* VEVENT VTODO VJOURNAL    *      VTIMEZONE */
    addPropEntry(propertyNames, ICalTags.sequence);      /* VEVENT VTODO VJOURNAL */
    addPropEntry(propertyNames, ICalTags.status);        /* VEVENT VTODO VJOURNAL */
    addPropEntry(propertyNames, ICalTags.summary);       /* VEVENT VTODO VJOURNAL    *            *   VALARM */
    addPropEntry(propertyNames, ICalTags.transp);        /* VEVENT */
    addPropEntry(propertyNames, ICalTags.trigger);       /* VEVENT VTODO    *        *            *   VALARM */
    //addPropEntry(propertyNames, ICalTags.tzid);        /*     *     *     *        *      VTIMEZONE */
    //addPropEntry(propertyNames, ICalTags.tzname);      /*     *     *     *               VTIMEZONE */
    //addPropEntry(propertyNames, ICalTags.tzoffsetfrom);  /*   *     *     *        *      VTIMEZONE */
    //addPropEntry(propertyNames, ICalTags.tzoffsetto);  /*     *     *     *        *      VTIMEZONE */
    //addPropEntry(propertyNames, ICalTags.tzurl);       /*     *     *     *        *      VTIMEZONE */
    addPropEntry(propertyNames, ICalTags.uid);           /* VEVENT VTODO VJOURNAL VFREEBUSY */
    addPropEntry(propertyNames, ICalTags.url);           /* VEVENT VTODO VJOURNAL VFREEBUSY */
    //addPropEntry(propertyNames, ICalTags.version);     /*     *     *     *        *            *          CALENDAR*/
  }

  /** Place holder for status
   *
   * @param sysi system interface
   * @param status from exception
   * @param uri of resource
   */
  public CaldavComponentNode(final SysIntf sysi,
                             final int status,
                             final String uri) {
    super(true, sysi, uri);
    setStatus(status);
  }

  /** Constructor
   *
   * @param cdURI referencing rsource
   * @param sysi system interface
   */
  public CaldavComponentNode(final CaldavURI cdURI,
                             final SysIntf sysi) {
    super(cdURI, sysi);

    col = cdURI.getCol();
    collection = false;
    allowsGet = true;
    entityName = cdURI.getEntityName();

    event = cdURI.getEntity();
  }

  @Override
  public void init(final boolean content) {
    if (!content) {
      return;
    }

    if ((event == null) && exists) {
      if (entityName == null) {
        exists = false;
      }
    }
  }

  @Override
  public AccessPrincipal getOwner() {
    if (owner == null) {
      if (event == null) {
        return null;
      }

      owner = event.getOwner();
    }

    return owner;
  }

  @Override
  public boolean removeProperty(final Element val,
                                final SetPropertyResult spr) {
    warn("Unimplemented - removeProperty");

    return false;
  }

  @Override
  public boolean setProperty(final Element val,
                             final SetPropertyResult spr) {
    if (super.setProperty(val, spr)) {
      return true;
    }

    return false;
  }

  /** Get a Component form of the only or master event. Mainly for property
   * filters.
   *
   * @return Component
   */
  public Component getComponent() {
    init(true);

    try {
      if ((event != null) && (comp == null)) {
        if (ical == null) {
          ical = getSysi().toCalendar(event,
                                      (col.getCalType() == CalDAVCollection.calTypeInbox) ||
                                      (col.getCalType() == CalDAVCollection.calTypeOutbox));
        }

        final var cl = ical.getComponents();

        if ((cl == null) || (cl.isEmpty())) {
          return null;
        }

        // XXX Wrong - should just use the BwEvent + overrides?
        comp = cl.get(0);
      }
    } catch (final Throwable t) {
      throw new WebdavException(t);
    }

    return comp;
  }

  @Override
  public void update() {
    if (event != null) {
      getSysi().updateEvent(event);
    }
  }

  /**
   * @param val String name
   */
  public void setEntityName(final String val) {
    if (entityName != null) {
      throw new WebdavException("Cannot change entity name");
    }

    entityName = val;
    uri = uri + "/" + val;
  }

  /**
   * @return String
   */
  public String getEntityName() {
    return entityName;
  }

  @Override
  public boolean trailSlash() {
    return false;
  }

  /* ==============================================================
   *                   Property methods
   * ============================================================== */

  @Override
  public boolean knownProperty(final QName tag) {
    if (propertyNames.get(tag) != null) {
      return true;
    }

    // Not ours
    return super.knownProperty(tag);
  }

  @Override
  public boolean generatePropertyValue(final QName tag,
                                       final WebdavNsIntf intf,
                                       final boolean allProp) {
    final PropVal pv = new PropVal();
    final XmlEmit xml = intf.getXmlEmit();

    if (propertyNames.get(tag) == null) {
      // Not ours
      return super.generatePropertyValue(tag, intf, allProp);
    }

    if (isTimezone) {
      return generateTZPropertyValue(tag, intf, allProp);
    }

    final CalDAVEvent<?> ev = checkEv(pv);
    if (ev == null) {
      return true;
    }

    return ev.generatePropertyValue(tag, xml);
  }

  @Override
  public Collection<PropertyTagEntry> getPropertyNames() {
    final Collection<PropertyTagEntry> res = new ArrayList<>();

    res.addAll(super.getPropertyNames());
    res.addAll(propertyNames.values());

    return res;
  }

  /**
   * @param val event
   */
  public void setEvent(final CalDAVEvent<?> val) {
    event = val;
  }

  /** Returns the only event or the master event for a recurrence
   *
   * @return CalDAVEvent
   */
  public CalDAVEvent<?> getEvent() {
    init(true);

    return event;
  }

  /**
   * @return Calendar
   */
  public Calendar getIcal() {
    init(true);

    try {
      if (ical == null) {
        ical = getSysi().toCalendar(event,
                                    (col.getCalType() == CalDAVCollection.calTypeInbox) ||
                                    (col.getCalType() == CalDAVCollection.calTypeOutbox));
      }
    } catch (final Throwable t) {
      throw new WebdavException(t);
    }

    return ical;
  }

  /* UNUSED(non-Javadoc)
   * @see edu.rpi.cct.webdav.servlet.shared.WebdavNsNode#getProperties(java.lang.String)
   * /
  public Collection<WebdavProperty> getProperties(final String ns) {
    init(true);
    ArrayList<WebdavProperty> al = new ArrayList<WebdavProperty>();

    getComponent(); // init comp
    if (comp == null) {
      throw new WebdavException("getProperties, comp == null");
    }

    addProp(al, ICalTags.summary, compw.getSummary());
    addProp(al, ICalTags.dtstart, compw.getDtstart());
    addProp(al, ICalTags.dtend, compw.getDtend());
    addProp(al, ICalTags.duration, compw.getDuration());
    addProp(al, ICalTags.transp, compw.getTransp());
    addProp(al, ICalTags.due, compw.getDue());
//    addProp(v, ICalTags.completed,        | date-time from RFC2518
    addProp(al, ICalTags.status, compw.getStatus());
//    addProp(v, ICalTags.priority,         | integer
//    addProp(v, ICalTags.percentComplete, | integer
    addProp(al, ICalTags.uid, compw.getUid());
    addProp(al, ICalTags.sequence, compw.getSequence());
//    addProp(v, ICalTags.recurrenceId,    | date-time from RFC2518
//    addProp(v, ICalTags.trigger,          | see below TODO

// FIXME FIX FIX
    addProp(al, ICalTags.hasRecurrence, "0");
    addProp(al, ICalTags.hasAlarm, "0");
    addProp(al, ICalTags.hasAttachment, "0");

    /* Default property calendar-data returns all of the object * /
    al.add(new CalendarData(CaldavTags.calendarData, debug));

    return al;
  }
  */

  @Override
  public String writeContent(final XmlEmit xml,
                             final Writer wtr,
                             final String contentType) {
    try {
      final Collection<CalDAVEvent<?>> evs = new ArrayList<>();

      evs.add(event);

      final MethodEmitted method;

      if ((col.getCalType() == CalDAVCollection.calTypeInbox) ||
      (col.getCalType() == CalDAVCollection.calTypeOutbox)) {
        method = MethodEmitted.eventMethod;
      } else {
        method = MethodEmitted.noMethod;
      }

      return getSysi().writeCalendar(evs,
                                     method,
                                     xml,
                                     wtr,
                                     contentType);
    } catch (final WebdavException we) {
      throw we;
    } catch (final Throwable t) {
      throw new WebdavException(t);
    }
  }

  @Override
  public String getContentString(final String contentType) {
    return getCompString(contentType);
  }

  /* ==============================================================
   *                   Overridden property methods
   * ============================================================== */

  @Override
  public CurrentAccess getCurrentAccess() {
    if (currentAccess != null) {
      return currentAccess;
    }

    if (event == null) {
      return null;
    }

    try {
      currentAccess = getSysi().checkAccess(event, PrivilegeDefs.privAny, true);
    } catch (final Throwable t) {
      throw new WebdavException(t);
    }

    return currentAccess;
  }

  /**
   * @return stag value
   */
  public String getStagValue() {
    init(true);

    final CalDAVEvent<?> ev = getEvent();
    if (ev == null) {
      return null;
    }

    return ev.getScheduleTag();
  }

  /**
   * @return stag before changes
   */
  public String getPrevStagValue() {
    init(true);

    final CalDAVEvent<?> ev = getEvent();
    if (ev == null) {
      return null;
    }

    return ev.getPrevScheduleTag();
  }

  @Override
  public String getEtagValue(final boolean strong) {
    init(true);

    final CalDAVEvent<?> ev = getEvent();
    if (ev == null) {
      return null;
    }

    final String val = ev.getEtag();

    if (strong) {
      return val;
    }

    return "W/" + val;
  }

  /**
   * @param strong true for strong etag
   * @return etag before changes
   */
  public String getPrevEtagValue(final boolean strong) {
    init(true);

    final CalDAVEvent<?> ev = getEvent();
    if (ev == null) {
      return null;
    }

    final String val = ev.getPreviousEtag();

    if (strong) {
      return val;
    }

    return "W/" + val;
  }

  @Override
  public String getEtokenValue() {
    return concatEtoken(getEtagValue(true), getStagValue());
  }

  @Override
  public String toString() {
    final ToString ts = new ToString(this);

    ts.append("path", getPath());
    ts.append("entityName", entityName);

    return ts.toString();
  }

  /* ==============================================================
   *                   Required webdav properties
   * ============================================================== */

  @Override
  public String getContentLang() {
    return "en";
  }

  @Override
  public long getContentLen() {
    return getCompString(getContentType()).length();
  }

  @Override
  public String getContentType() {
    return "text/calendar;charset=utf-8";
  }

  @Override
  public String getCreDate() {
    init(false);
    final CalDAVEvent<?> ev = getEvent();
    if (ev == null) {
      return null;
    }

    return ev.getCreated();
  }

  @Override
  public String getDisplayname() {
    return getEntityName();
  }

  @Override
  public String getLastmodDate() {
    init(false);
    final CalDAVEvent<?> ev = getEvent();
    if (ev == null) {
      return null;
    }

    try {
      return DateTimeUtil.fromISODateTimeUTCtoRfc822(ev.getLastmod());
    } catch (final Throwable t) {
      throw new WebdavException(t);
    }
  }

  @Override
  public boolean allowsSyncReport() {
    return false;
  }

  @Override
  public boolean getDeleted() {
    return getEvent().getDeleted();
  }

  /* ====================================================================
   *                   Private methods
   * ==================================================================== */

  private String getCompString(final String contentType) {
    String ctype = contentType;
    if (ctype == null) {
      ctype = getSysi().getDefaultContentType();
    }

    if (ctype.equals(compContentType)) {
      return compString;
    }

    getIcal();

    compContentType = ctype;
    compString = getSysi().toIcalString(ical, ctype);

    return compString;
  }

  private boolean generateTZPropertyValue(final QName tag,
                                          final WebdavNsIntf intf,
                                          final boolean allProp) {
    if (tag.equals(ICalTags.tzid)) {
      // PROPTODO
      return true;
    }

    if (tag.equals(ICalTags.tzname)) {
      // PROPTODO
      return true;
    }

    if (tag.equals(ICalTags.tzoffsetfrom)) {
      // PROPTODO
      return true;
    }

    if (tag.equals(ICalTags.tzoffsetto)) {
      // PROPTODO
      return true;
    }

    if (tag.equals(ICalTags.tzurl)) {
      // PROPTODO
      return true;
    }

    return false;
  }

  private CalDAVEvent<?> checkEv(final PropVal pv) {
    final CalDAVEvent<?> ev = getEvent();

    if (ev == null) {
      pv.notFound = true;
      return null;
    }

    return ev;
  }
}
