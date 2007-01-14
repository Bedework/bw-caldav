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

package org.bedework.caldav.server;

import org.bedework.caldav.server.calquery.CalendarData;
import org.bedework.calfacade.BwEvent;
import org.bedework.calfacade.svc.EventInfo;

import org.bedework.davdefs.CaldavTags;
import org.bedework.davdefs.WebdavTags;
import org.bedework.icalendar.ComponentWrapper;
import org.w3c.dom.Element;

import edu.rpi.cct.webdav.servlet.shared.WebdavException;
import edu.rpi.cct.webdav.servlet.shared.WebdavNsIntf;
import edu.rpi.cct.webdav.servlet.shared.WebdavProperty;
import edu.rpi.cmt.access.Acl.CurrentAccess;
import edu.rpi.sss.util.xml.QName;
import edu.rpi.sss.util.xml.XmlEmit;

import net.fortuna.ical4j.model.Calendar;
import net.fortuna.ical4j.model.Component;
import net.fortuna.ical4j.model.component.VEvent;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;

/** Class to represent an entity such as events in caldav.
 *
 *   @author Mike Douglass   douglm@rpi.edu
 */
public class CaldavComponentNode extends CaldavBwNode {
  /* The event if this component is an event */
  private EventInfo eventInfo;

  // We also need a todo object and maybe a journal, freebusy and a timezone

  private boolean isTimezone;

  /** The Component object
   */
  private VEvent vevent;
  private Calendar ical;

  private String veventString;

  private ComponentWrapper comp;

  private final static HashMap<QName, PropertyTagEntry> propertyNames =
    new HashMap<QName, PropertyTagEntry>();

  static {
    addPropEntry(propertyNames, CaldavTags.calendarData);
    addPropEntry(propertyNames, CaldavTags.originator);
    addPropEntry(propertyNames, CaldavTags.recipient);
    addPropEntry(propertyNames, CaldavTags.scheduleState);

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

  /** Constructor
   *
   * @param cdURI
   * @param sysi
   * @param debug
   * @throws WebdavException
   */
  public CaldavComponentNode(CaldavURI cdURI,
                             SysIntf sysi, boolean debug) throws WebdavException {
    super(cdURI, sysi, debug);

    collection = false;
    allowsGet = true;

    eventInfo = cdURI.getEntity();
  }

  public boolean removeProperty(Element val) throws WebdavException {
    warn("Unimplemented - removeProperty");
    return false;
  }

  public boolean setProperty(Element val) throws WebdavException {
    warn("Unimplemented - setProperty");
    return false;
  }

  /** Get a vevent form of the only or master event. Mainly for property
   * filters.
   *
   * @return Component
   * @throws WebdavException
   */
  public Component getVevent() throws WebdavException {
    init(true);

    try {
      if ((eventInfo != null) && (vevent == null)) {
        ical = getSysi().toCalendar(eventInfo);
        vevent = (VEvent)ical.getComponents().getComponent(Component.VEVENT);

        /*
         vevent = trans.toIcalEvent(event);
         ical = trans.newIcal();
         IcalUtil.addComponent(ical, vevent);
         */

        // XXX Add the timezones if needed

        comp = new ComponentWrapper(vevent);
      }
    } catch (Throwable t) {
      throw new WebdavException(t);
    }

    return vevent;
  }

  /* ====================================================================
   *                   Property methods
   * ==================================================================== */

 /* (non-Javadoc)
 * @see edu.rpi.cct.webdav.servlet.shared.WebdavNsNode#generatePropertyValue(edu.rpi.sss.util.xml.QName, edu.rpi.cct.webdav.servlet.shared.WebdavNsIntf, boolean)
 */
public boolean generatePropertyValue(QName tag,
                                      WebdavNsIntf intf,
                                      boolean allProp) throws WebdavException {
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
      BwEvent ev = checkEv(pv);
      if (ev == null) {
        return true;
      }

      if (tag.equals(CaldavTags.scheduleState)) {
        xml.openTag(tag);
        if (ev.getScheduleState() == BwEvent.scheduleStateNotProcessed) {
          xml.emptyTag(CaldavTags.notProcessed);
        } else {
          xml.emptyTag(CaldavTags.processed);
        }
        xml.closeTag(tag);
        return true;
      }

      if (tag.equals(CaldavTags.originator)) {
        if (ev.getOriginator() != null) {
          xml.openTag(tag);
          xml.property(WebdavTags.href, ev.getOriginator());
          xml.closeTag(tag);
        }
        return true;
      }

      if (tag.equals(CaldavTags.recipient)) {
        Collection r = ev.getRecipients();
        if ((r == null) || (r.size() == 0)) {
          return true;
        }

        xml.openTag(tag);
        Iterator it = r.iterator();
        while (it.hasNext()) {
          xml.property(WebdavTags.href, (String)it.next());
        }
        xml.closeTag(tag);
        return true;
      }

      /* =============== ICalTags follow ================= */

      if (tag.equals(ICalTags.action)) {
        // PROPTODO
        return true;
      }

      if (tag.equals(ICalTags.attach)) {
        // PROPTODO
        return true;
      }

      if (tag.equals(ICalTags.attendee)) {
        // PROPTODO
        return true;
      }

      if (tag.equals(ICalTags.categories)) {
        // PROPTODO
        return true;
      }

      if (tag.equals(ICalTags._class)) {
        // PROPTODO
        return true;
      }

      if (tag.equals(ICalTags.comment)) {
        // PROPTODO
        return true;
      }

      if (tag.equals(ICalTags.completed)) {
        // PROPTODO
        return true;
      }

      if (tag.equals(ICalTags.contact)) {
        // PROPTODO
        return true;
      }

      if (tag.equals(ICalTags.created)) {
        xml.property(tag, ev.getCreated());
        return true;
      }

      if (tag.equals(ICalTags.description)) {
        if (ev.getDescription() != null) {
          xml.property(tag, ev.getDescription());
        }
        return true;
      }

      if (tag.equals(ICalTags.dtend)) {
        xml.property(tag, ev.getDtend().getDate());
        return true;
      }

      if (tag.equals(ICalTags.dtstamp)) {
        xml.property(tag, ev.getDtstamp());
        return true;
      }

      if (tag.equals(ICalTags.dtstart)) {
        xml.property(tag, ev.getDtstart().getDate());
        return true;
      }

      /* TODO
       if (tag.equals(ICalTags.due)) {
       pv.val = ev.
       return pv;
       }
       */

      if (tag.equals(ICalTags.duration)) {
        xml.property(tag, ev.getDuration());
        return true;
      }

      if (tag.equals(ICalTags.exdate)) {
        // PROPTODO
        return true;
      }

      if (tag.equals(ICalTags.exrule)) {
        // PROPTODO
        return true;
      }

      if (tag.equals(ICalTags.freebusy)) {
        // PROPTODO
        return true;
      }

      if (tag.equals(ICalTags.geo)) {
        // PROPTODO
        return true;
      }

      /*
       if (tag.equals(ICalTags.hasRecurrence)) {
       pv.val = ev
       return pv;
       }

       if (tag.equals(ICalTags.hasAlarm)) {
       pv.val = ev
       return pv;
       }

       if (tag.equals(ICalTags.hasAttachment)) {
       pv.val = ev
       return pv;
       }*/

      if (tag.equals(ICalTags.lastModified)) {
        // PROPTODO
        return true;
      }

      if (tag.equals(ICalTags.lastModified)) {
        xml.property(tag, ev.getLastmod());
        return true;
      }

      if (tag.equals(ICalTags.location)) {
        // PROPTODO
        return true;
      }

      if (tag.equals(ICalTags.organizer)) {
        // PROPTODO
        return true;
      }

      if (tag.equals(ICalTags.organizer)) {
        if (ev.getOrganizer() != null) {
          xml.property(tag, ev.getOrganizer().getOrganizerUri());
        }
        return true;
      }

      if (tag.equals(ICalTags.percentComplete)) {
        // PROPTODO
        return true;
      }

      if (tag.equals(ICalTags.priority)) {
        Integer val = ev.getPriority();
        if ((val != null) && (val != 0)) {
          xml.property(tag, String.valueOf(val));
        }

        return true;
      }

      if (tag.equals(ICalTags.rdate)) {
        // PROPTODO
        return true;
      }

      if (tag.equals(ICalTags.recurrenceId)) {
        // PROPTODO
        return true;
      }

      if (tag.equals(ICalTags.recurrenceId)) {
        if (ev.getRecurrenceId() != null) {
          xml.property(tag, ev.getRecurrenceId());
        }
        return true;
      }

      if (tag.equals(ICalTags.relatedTo)) {
        // PROPTODO
        return true;
      }

      if (tag.equals(ICalTags.repeat)) {
        // PROPTODO
        return true;
      }

      if (tag.equals(ICalTags.resources)) {
        // PROPTODO
        return true;
      }

      if (tag.equals(ICalTags.requestStatus)) {
        // PROPTODO
        /*
        if (ev.getRequestStatus() != null) {
          xml.property(tag, ev.getRequestStatus().strVal());
        }
        */
        return true;
      }

      if (tag.equals(ICalTags.rrule)) {
        // PROPTODO
        return true;
      }

      if (tag.equals(ICalTags.sequence)) {
        xml.property(tag, String.valueOf(ev.getSequence()));

        return true;
      }

      if (tag.equals(ICalTags.status)) {
        xml.property(tag, ev.getStatus());
        return true;
      }

      if (tag.equals(ICalTags.summary)) {
        xml.property(tag, ev.getSummary());
        return true;
      }

      if (tag.equals(ICalTags.transp)) {
        xml.property(tag, ev.getTransparency());
        return true;
      }

      if (tag.equals(ICalTags.trigger)) {
        // PROPTODO
        return true;
      }

      if (tag.equals(ICalTags.uid)) {
        xml.property(tag, ev.getUid());
        return true;
      }

      if (tag.equals(ICalTags.url)) {
        if (ev.getLink() != null) {
          xml.property(tag, ev.getLink());
        }
        return true;
      }

      if (tag.equals(ICalTags.version)) {
        // PROPTODO
        return true;
      }

      return false;
    } catch (WebdavException wde) {
      throw wde;
    } catch (Throwable t) {
      throw new WebdavException(t);
    }
  }

  public void init(boolean content) throws WebdavException {
    if (!content) {
      return;
    }

    try {
      if ((eventInfo == null) && exists) {
        String entityName = cdURI.getEntityName();

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
   * @see edu.rpi.cct.webdav.servlet.shared.WebdavNsNode#getPropertyNames()
   */
  public Collection<PropertyTagEntry> getPropertyNames() throws WebdavException {
    Collection<PropertyTagEntry> res = new ArrayList<PropertyTagEntry>();

    res.addAll(super.getPropertyNames());
    res.addAll(propertyNames.values());

    return res;
  }

  /**
   * @param val
   */
  public void setEventInfo(EventInfo val) {
    eventInfo = val;
  }

  /** Returns the only event or the master event for a recurrence
   *
   * @return EventInfo
   * @throws WebdavException
   */
  public EventInfo getEventInfo() throws WebdavException {
    init(true);

    return eventInfo;
  }

  /**
   * @return Calendar
   * @throws WebdavException
   */
  public Calendar getIcal() throws WebdavException {
    init(true);

    try {
      if (ical == null) {
        ical = getSysi().toCalendar(eventInfo);
      }
      if ((veventString == null)) {
        veventString = ical.toString();
      }
    } catch (Throwable t) {
      throw new WebdavException(t);
    }

    return ical;
  }

  public Collection<WebdavProperty> getProperties(String ns) throws WebdavException {
    init(true);
    ArrayList<WebdavProperty> al = new ArrayList<WebdavProperty>();

    getVevent(); // init comp
    if (comp == null) {
      throw new WebdavException("getProperties, comp == null");
    }

    addProp(al, ICalTags.summary, comp.getSummary());
    addProp(al, ICalTags.dtstart, comp.getDtstart());
    addProp(al, ICalTags.dtend, comp.getDtend());
    addProp(al, ICalTags.duration, comp.getDuration());
    addProp(al, ICalTags.transp, comp.getTransp());
    addProp(al, ICalTags.due, comp.getDue());
//    addProp(v, ICalTags.completed,        | date-time from RFC2518
    addProp(al, ICalTags.status, comp.getStatus());
//    addProp(v, ICalTags.priority,         | integer
//    addProp(v, ICalTags.percentComplete, | integer
    addProp(al, ICalTags.uid, comp.getUid());
    addProp(al, ICalTags.sequence, comp.getSequence());
//    addProp(v, ICalTags.recurrenceId,    | date-time from RFC2518
//    addProp(v, ICalTags.trigger,          | see below TODO

// FIXME FIX FIX
    addProp(al, ICalTags.hasRecurrence, "0");
    addProp(al, ICalTags.hasAlarm, "0");
    addProp(al, ICalTags.hasAttachment, "0");

    /* Default property calendar-data returns all of the object */
    al.add(new CalendarData(CaldavTags.calendarData, debug));

    return al;
  }

  public String getContentString() throws WebdavException {
    getIcal(); // init content

    return veventString;
  }

  /* ====================================================================
   *                   Overridden property methods
   * ==================================================================== */

  public CurrentAccess getCurrentAccess() throws WebdavException {
    if (eventInfo == null) {
      return null;
    }

    return eventInfo.getCurrentAccess();
  }

  public String getEtagValue(boolean strong) throws WebdavException {
    init(true);

    BwEvent ev = getEvent();
    if (ev == null) {
      return null;
    }

    String val = ev.getLastmod() + "-" + ev.getSeq();

    if (strong) {
      return "\"" + val + "\"";
    }

    return "W/\"" + val + "\"";
  }

  public String toString() {
    StringBuffer sb = new StringBuffer();

    sb.append("CaldavComponentNode{cduri=");
    sb.append(getCDURI());
    sb.append("}");

    return sb.toString();
  }

  /* ====================================================================
   *                   Required webdav properties
   * ==================================================================== */

  /* (non-Javadoc)
   * @see edu.rpi.cct.webdav.servlet.shared.WebdavNsNode#getContentLang()
   */
  public String getContentLang() throws WebdavException {
    return "en";
  }

  /* (non-Javadoc)
   * @see edu.rpi.cct.webdav.servlet.shared.WebdavNsNode#getContentLen()
   */
  public int getContentLen() throws WebdavException {
    getIcal(); // init length
    if (veventString != null) {
      return veventString.length();
    }
    return 0;
  }

  /* (non-Javadoc)
   * @see edu.rpi.cct.webdav.servlet.shared.WebdavNsNode#getContentType()
   */
  public String getContentType() throws WebdavException {
    return "text/calendar";
  }

  /* (non-Javadoc)
   * @see edu.rpi.cct.webdav.servlet.shared.WebdavNsNode#getCreDate()
   */
  public String getCreDate() throws WebdavException {
    init(false);
    BwEvent ev = getEvent();
    if (ev == null) {
      return null;
    }

    return ev.getCreated();
  }

  /* (non-Javadoc)
   * @see edu.rpi.cct.webdav.servlet.shared.WebdavNsNode#getDisplayname()
   */
  public String getDisplayname() throws WebdavException {
    if (cdURI == null) {
      return null;
    }

    return cdURI.getEntityName();
  }

  /* (non-Javadoc)
   * @see edu.rpi.cct.webdav.servlet.shared.WebdavNsNode#getLastmodDate()
   */
  public String getLastmodDate() throws WebdavException {
    init(false);
    BwEvent ev = getEvent();
    if (ev == null) {
      return null;
    }

    return ev.getLastmod();
  }

  /* ====================================================================
   *                   Private methods
   * ==================================================================== */

  private boolean generateTZPropertyValue(QName tag,
                                          WebdavNsIntf intf,
                                          boolean allProp) throws WebdavException {
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

  private BwEvent getEvent() throws WebdavException {
    EventInfo ei = getEventInfo();

    if (ei == null) {
      return null;
    }

    return ei.getEvent();
  }

  private BwEvent checkEv(PropVal pv) throws WebdavException {
    BwEvent ev = getEvent();

    if (ev == null) {
      pv.notFound = true;
      return null;
    }

    return ev;
  }

  private void addProp(Collection<WebdavProperty> c, QName tag, Object val) {
    if (val != null) {
      c.add(new WebdavProperty(tag, String.valueOf(val)));
    }
  }
}
