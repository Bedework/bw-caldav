/* **********************************************************************
    Copyright 2009 Rensselaer Polytechnic Institute. All worldwide rights reserved.

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
package org.bedework.caldav.server;

import org.bedework.caldav.server.sysinterface.SysIntf;

import edu.rpi.cct.webdav.servlet.shared.WebdavException;
import edu.rpi.cct.webdav.servlet.shared.WebdavNsIntf;
import edu.rpi.cmt.access.AccessPrincipal;
import edu.rpi.cmt.access.PrivilegeDefs;
import edu.rpi.cmt.access.Acl.CurrentAccess;
import edu.rpi.sss.util.DateTimeUtil;
import edu.rpi.sss.util.xml.XmlEmit;
import edu.rpi.sss.util.xml.tagdefs.AppleServerTags;
import edu.rpi.sss.util.xml.tagdefs.CaldavTags;
import edu.rpi.sss.util.xml.tagdefs.ICalTags;

import net.fortuna.ical4j.model.Calendar;
import net.fortuna.ical4j.model.Component;
import net.fortuna.ical4j.model.ComponentList;

import org.w3c.dom.Element;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;

import javax.xml.namespace.QName;

/** Class to represent an entity such as events in caldav.
 *
 *   @author Mike Douglass   douglm@rpi.edu
 */
public class CaldavComponentNode extends CaldavBwNode {
  /* The event if this component is an event */
  private CalDAVEvent event;

  private AccessPrincipal owner;

  private CurrentAccess currentAccess;

  private String entityName;

  // We also need a task object and maybe a journal, freebusy and a timezone

  private boolean isTimezone;

  private Calendar ical;

  /** The event Component object
   */
  private Component comp;

  private String compString;


  private final static HashMap<QName, PropertyTagEntry> propertyNames =
    new HashMap<QName, PropertyTagEntry>();

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
   * @param sysi
   * @param status
   * @param uri
   * @param debug
   */
  public CaldavComponentNode(final SysIntf sysi, final int status, final String uri, final boolean debug) {
    super(true, sysi, uri, debug);
    setStatus(status);
  }

  /** Constructor
   *
   * @param cdURI
   * @param sysi
   * @param debug
   * @throws WebdavException
   */
  public CaldavComponentNode(final CaldavURI cdURI,
                             final SysIntf sysi, final boolean debug) throws WebdavException {
    super(cdURI, sysi, debug);

    col = cdURI.getCol();
    collection = false;
    allowsGet = true;
    entityName = cdURI.getEntityName();

    event = cdURI.getEntity();
  }

  @Override
  public void init(final boolean content) throws WebdavException {
    if (!content) {
      return;
    }

    try {
      if ((event == null) && exists) {
        if (entityName == null) {
          exists = false;
          return;
        }
      }
    } catch (Throwable t) {
      throw new WebdavException(t);
    }
  }

  /* (non-Javadoc)
   * @see edu.rpi.cct.webdav.servlet.shared.WebdavNsNode#getOwner()
   */
  @Override
  public AccessPrincipal getOwner() throws WebdavException {
    if (owner == null) {
      if (event == null) {
        return null;
      }

      owner = event.getOwner();
    }

    return owner;
  }

  /* (non-Javadoc)
   * @see edu.rpi.cct.webdav.servlet.shared.WebdavNsNode#removeProperty(org.w3c.dom.Element)
   */
  @Override
  public boolean removeProperty(final Element val,
                                final SetPropertyResult spr) throws WebdavException {
    warn("Unimplemented - removeProperty");

    return false;
  }

  /* (non-Javadoc)
   * @see edu.rpi.cct.webdav.servlet.shared.WebdavNsNode#setProperty(org.w3c.dom.Element)
   */
  @Override
  public boolean setProperty(final Element val,
                             final SetPropertyResult spr) throws WebdavException {
    if (super.setProperty(val, spr)) {
      return true;
    }

    return false;
  }

  /** Get a Component form of the only or master event. Mainly for property
   * filters.
   *
   * @return Component
   * @throws WebdavException
   */
  public Component getComponent() throws WebdavException {
    init(true);

    try {
      if ((event != null) && (comp == null)) {
        if (ical == null) {
          ical = getSysi().toCalendar(event,
                                      (col.getCalType() == CalDAVCollection.calTypeInbox) ||
                                      (col.getCalType() == CalDAVCollection.calTypeOutbox));
        }
        ComponentList cl = ical.getComponents();

        if ((cl == null) || (cl.isEmpty())) {
          return null;
        }

        // XXX Wrong - should just use the BwEvent + overrides?
        comp = (Component)cl.get(0);
      }
    } catch (Throwable t) {
      throw new WebdavException(t);
    }

    return comp;
  }

  /* (non-Javadoc)
   * @see edu.rpi.cct.webdav.servlet.shared.WebdavNsNode#update()
   */
  @Override
  public void update() throws WebdavException {
    if (event != null) {
      getSysi().updateEvent(event);
    }
  }

  /**
   * @return String
   */
  public String getEntityName() {
    return entityName;
  }

  /* (non-Javadoc)
   * @see edu.rpi.cct.webdav.servlet.shared.WebdavNsNode#trailSlash()
   */
  @Override
  public boolean trailSlash() {
    return false;
  }

  /* ====================================================================
   *                   Property methods
   * ==================================================================== */

  /* (non-Javadoc)
   * @see edu.rpi.cct.webdav.servlet.shared.WebdavNsNode#knownProperty(edu.rpi.sss.util.xml.QName)
   */
  @Override
  public boolean knownProperty(final QName tag) {
    if (propertyNames.get(tag) != null) {
      return true;
    }

    // Not ours
    return super.knownProperty(tag);
  }

 /* (non-Javadoc)
 * @see edu.rpi.cct.webdav.servlet.shared.WebdavNsNode#generatePropertyValue(edu.rpi.sss.util.xml.QName, edu.rpi.cct.webdav.servlet.shared.WebdavNsIntf, boolean)
 */
@Override
public boolean generatePropertyValue(final QName tag,
                                     final WebdavNsIntf intf,
                                     final boolean allProp) throws WebdavException {
    PropVal pv = new PropVal();
    XmlEmit xml = intf.getXmlEmit();

    if (propertyNames.get(tag) == null) {
      // Not ours
      return super.generatePropertyValue(tag, intf, allProp);
    }

    if (isTimezone) {
      return generateTZPropertyValue(tag, intf, allProp);
    }

    try {
      CalDAVEvent ev = checkEv(pv);
      if (ev == null) {
        return true;
      }

      return ev.generatePropertyValue(tag, xml);
    } catch (WebdavException wde) {
      throw wde;
    } catch (Throwable t) {
      throw new WebdavException(t);
    }
  }

  /* (non-Javadoc)
   * @see edu.rpi.cct.webdav.servlet.shared.WebdavNsNode#getPropertyNames()
   */
  @Override
  public Collection<PropertyTagEntry> getPropertyNames() throws WebdavException {
    Collection<PropertyTagEntry> res = new ArrayList<PropertyTagEntry>();

    res.addAll(super.getPropertyNames());
    res.addAll(propertyNames.values());

    return res;
  }

  /**
   * @param val
   */
  public void setEvent(final CalDAVEvent val) {
    event = val;
  }

  /** Returns the only event or the master event for a recurrence
   *
   * @return CalDAVEvent
   * @throws WebdavException
   */
  public CalDAVEvent getEvent() throws WebdavException {
    init(true);

    return event;
  }

  /**
   * @return Calendar
   * @throws WebdavException
   */
  public Calendar getIcal() throws WebdavException {
    init(true);

    try {
      if (ical == null) {
        ical = getSysi().toCalendar(event,
                                    (col.getCalType() == CalDAVCollection.calTypeInbox) ||
                                    (col.getCalType() == CalDAVCollection.calTypeOutbox));
      }
      if ((compString == null)) {
        compString = getSysi().toIcalString(ical);
      }
    } catch (Throwable t) {
      throw new WebdavException(t);
    }

    return ical;
  }

  /* UNUSED(non-Javadoc)
   * @see edu.rpi.cct.webdav.servlet.shared.WebdavNsNode#getProperties(java.lang.String)
   * /
  public Collection<WebdavProperty> getProperties(final String ns) throws WebdavException {
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
  public String getContentString() throws WebdavException {
    getIcal(); // init content

    return compString;
  }

  /* ====================================================================
   *                   Overridden property methods
   * ==================================================================== */

  @Override
  public CurrentAccess getCurrentAccess() throws WebdavException {
    if (currentAccess != null) {
      return currentAccess;
    }

    if (event == null) {
      return null;
    }

    try {
      currentAccess = getSysi().checkAccess(event, PrivilegeDefs.privAny, true);
    } catch (Throwable t) {
      throw new WebdavException(t);
    }

    return currentAccess;
  }

  /**
   * @return stag value
   * @throws WebdavException
   */
  public String getStagValue() throws WebdavException {
    init(true);

    CalDAVEvent ev = getEvent();
    if (ev == null) {
      return null;
    }

    String val = ev.getScheduleTag();

    return "\"" + val + "\"";
  }

  /**
   * @return stag before changes
   * @throws WebdavException
   */
  public String getPrevStagValue() throws WebdavException {
    init(true);

    CalDAVEvent ev = getEvent();
    if (ev == null) {
      return null;
    }

    String val = ev.getPrevScheduleTag();

    return "\"" + val + "\"";
  }

  @Override
  public String getEtagValue(final boolean strong) throws WebdavException {
    init(true);

    CalDAVEvent ev = getEvent();
    if (ev == null) {
      return null;
    }

    String val = ev.getTagValue();

    if (strong) {
      return "\"" + val + "\"";
    }

    return "W/\"" + val + "\"";
  }

  /**
   * @param strong
   * @return etag before changes
   * @throws WebdavException
   */
  public String getPrevEtagValue(final boolean strong) throws WebdavException {
    init(true);

    CalDAVEvent ev = getEvent();
    if (ev == null) {
      return null;
    }

    String val = ev.getPrevTagValue();

    if (strong) {
      return "\"" + val + "\"";
    }

    return "W/\"" + val + "\"";
  }

  @Override
  public String toString() {
    StringBuffer sb = new StringBuffer();

    sb.append("CaldavComponentNode{");
    sb.append("path=");
    sb.append(getPath());
    sb.append(", entityName=");
    sb.append(String.valueOf(entityName));
    sb.append("}");

    return sb.toString();
  }

  /* ====================================================================
   *                   Required webdav properties
   * ==================================================================== */

  /* (non-Javadoc)
   * @see edu.rpi.cct.webdav.servlet.shared.WebdavNsNode#getContentLang()
   */
  @Override
  public String getContentLang() throws WebdavException {
    return "en";
  }

  /* (non-Javadoc)
   * @see edu.rpi.cct.webdav.servlet.shared.WebdavNsNode#getContentLen()
   */
  @Override
  public long getContentLen() throws WebdavException {
    getIcal(); // init length
    if (compString != null) {
      return compString.getBytes().length;
    }
    return 0;
  }

  /* (non-Javadoc)
   * @see edu.rpi.cct.webdav.servlet.shared.WebdavNsNode#getContentType()
   */
  @Override
  public String getContentType() throws WebdavException {
    return "text/calendar; charset=UTF-8";
  }

  /* (non-Javadoc)
   * @see edu.rpi.cct.webdav.servlet.shared.WebdavNsNode#getCreDate()
   */
  @Override
  public String getCreDate() throws WebdavException {
    init(false);
    CalDAVEvent ev = getEvent();
    if (ev == null) {
      return null;
    }

    return ev.getCreated();
  }

  /* (non-Javadoc)
   * @see edu.rpi.cct.webdav.servlet.shared.WebdavNsNode#getDisplayname()
   */
  @Override
  public String getDisplayname() throws WebdavException {
    return getEntityName();
  }

  /* (non-Javadoc)
   * @see edu.rpi.cct.webdav.servlet.shared.WebdavNsNode#getLastmodDate()
   */
  @Override
  public String getLastmodDate() throws WebdavException {
    init(false);
    CalDAVEvent ev = getEvent();
    if (ev == null) {
      return null;
    }

    try {
      return DateTimeUtil.fromISODateTimeUTCtoRfc822(ev.getLastmod());
    } catch (Throwable t) {
      throw new WebdavException(t);
    }
  }

  /* ====================================================================
   *                   Private methods
   * ==================================================================== */

  private boolean generateTZPropertyValue(final QName tag,
                                          final WebdavNsIntf intf,
                                          final boolean allProp) throws WebdavException {
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

  private CalDAVEvent checkEv(final PropVal pv) throws WebdavException {
    CalDAVEvent ev = getEvent();

    if (ev == null) {
      pv.notFound = true;
      return null;
    }

    return ev;
  }
}
