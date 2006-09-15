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

import org.bedework.davdefs.CaldavDefs;
import org.bedework.davdefs.CaldavTags;
import org.bedework.davdefs.WebdavTags;
import org.bedework.icalendar.ComponentWrapper;
import org.w3c.dom.Element;

import edu.rpi.cct.webdav.servlet.shared.WebdavIntfException;
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
import java.util.Iterator;

/** Class to represent an entity such as events in caldav.
 *
 *   @author Mike Douglass   douglm@rpi.edu
 */
public class CaldavComponentNode extends CaldavBwNode {
  /* The only event or the master */
//  private BwEvent event;
  private EventInfo eventInfo;

  /* Collection of BwEvent for this node
   * Only 1 for non-recurring
   */
  private Collection events;

  /** The Component object
   */
  private VEvent vevent;
  private Calendar ical;

  private String veventString;

  private ComponentWrapper comp;

  private final static Collection propertyNames = new ArrayList();

  static {
    addPropEntry(CaldavTags.calendarData);

    addPropEntry(ICalTags.dtend);
    addPropEntry(ICalTags.dtstart);
    addPropEntry(ICalTags.due);
    addPropEntry(ICalTags.duration);
    addPropEntry(ICalTags.hasAlarm);
    addPropEntry(ICalTags.hasAttachment);
    addPropEntry(ICalTags.hasRecurrence);
    addPropEntry(ICalTags.sequence);
    addPropEntry(ICalTags.summary);
    addPropEntry(ICalTags.status);
    addPropEntry(ICalTags.transp);
    addPropEntry(ICalTags.uid);

    addPropEntry(WebdavTags.collection);
  }

  /** Constructor
   *
   * @param cdURI
   * @param sysi
   * @param debug
   * @throws WebdavIntfException
   */
  public CaldavComponentNode(CaldavURI cdURI,
                             SysIntf sysi, boolean debug) throws WebdavIntfException {
    super(cdURI, sysi, debug);

    collection = false;
    allowsGet = true;

    Object ent = cdURI.getEntity();

    if (ent instanceof EventInfo) {
      addEvent((EventInfo)ent);
    } else {
      setEvents((Collection)ent);
    }

    contentLang = "en";
    contentLen = -1;
    contentType = "text/calendar";
  }

  public boolean removeProperty(Element val) throws WebdavIntfException {
    warn("Unimplemented - removeProperty");
    return false;
  }

  public boolean setProperty(Element val) throws WebdavIntfException {
    warn("Unimplemented - setProperty");
    return false;
  }

  /** Get a vevent form of the only or master event. Mainly for property
   * filters.
   *
   * @return Component
   * @throws WebdavIntfException
   */
  public Component getVevent() throws WebdavIntfException {
    init(true);

    try {
      if ((eventInfo != null) && (vevent == null)) {
        Calendar ical = getSysi().toCalendar(eventInfo.getEvent());
        if (events.size() == 1) {
          this.ical = ical; // Save doing it again
        }
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
      throw new WebdavIntfException(t);
    }

    return vevent;
  }

  /* ====================================================================
   *                   Property methods
   * ==================================================================== */

  /** Emit the property indicated by the tag.
  *
  * @param tag  QName defining property
  * @param intf WebdavNsIntf
  * @return boolean   true if emitted
  * @throws WebdavIntfException
  */
 public boolean generatePropertyValue(QName tag,
                                      WebdavNsIntf intf) throws WebdavIntfException {
    PropVal pv = new PropVal();
    XmlEmit xml = intf.getXmlEmit();

    String ns = tag.getNamespaceURI();

    /* Deal with webdav properties */
    if ((!ns.equals(CaldavDefs.caldavNamespace) &&
        !ns.equals(CaldavDefs.icalNamespace))) {
      // Not ours
      return super.generatePropertyValue(tag, intf);
    }

    try {
      BwEvent ev = checkEv(pv);
      if (ev == null) {
        return false;
      }

      if (tag.equals(ICalTags.summary)) {
        xml.property(tag, ev.getSummary());
        return true;
      }

      if (tag.equals(ICalTags.dtstart)) {
        xml.property(tag, ev.getDtstart().getDate());
        return true;
      }

      if (tag.equals(ICalTags.dtend)) {
        xml.property(tag, ev.getDtend().getDate());
        return true;
      }

      if (tag.equals(ICalTags.duration)) {
        xml.property(tag, ev.getDuration());
        return true;
      }

      if (tag.equals(ICalTags.transp)) {
        xml.property(tag, ev.getTransparency());
        return true;
      }

      /* TODO
       if (tag.equals(ICalTags.due)) {
       pv.val = ev.
       return pv;
       }
       */

      if (tag.equals(ICalTags.status)) {
        xml.property(tag, ev.getStatus());
        return true;
      }

      if (tag.equals(ICalTags.uid)) {
        xml.property(tag, ev.getGuid());
        return true;
      }

      if (tag.equals(ICalTags.sequence)) {
        xml.property(tag, String.valueOf(ev.getSequence()));

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

      return false;
    } catch (WebdavIntfException wde) {
      throw wde;
    } catch (Throwable t) {
      throw new WebdavIntfException(t);
    }
  }

  public void init(boolean content) throws WebdavIntfException {
    name = cdURI.getEntityName();

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
      throw new WebdavIntfException(t);
    }
  }

  /**
   * @param evs
   * @throws WebdavIntfException
   */
  public void setEvents(Collection evs) throws WebdavIntfException {
    events = evs;

    if ((events == null) || (events.size() == 0)) {
      exists = false;
    } else if (events.size() == 1) {
      /* Non recurring or no overrides */
      eventInfo = (EventInfo)events.iterator().next();
    } else {
      /* Find the master */
      // XXX Check the guids here?
      Iterator it = events.iterator();
      while (it.hasNext()) {
        EventInfo ei = (EventInfo)it.next();

        if (ei.getEvent().getRecurring()) {
          eventInfo = ei;
        }
      }

      if (eventInfo == null) {
        throw new WebdavIntfException("Missing master for " + cdURI);
      }
    }
  }

  /** Return a set o QName defining properties this node supports.
   *
   * @return
   * @throws WebdavIntfException
   */
  public Collection getPropertyNames() throws WebdavIntfException {
    Collection res = new ArrayList();

    res.addAll(super.getPropertyNames());
    res.addAll(propertyNames);

    return res;
  }

  /** Add an event to our collection.
   *
   * @param val
   */
  public void addEvent(EventInfo val) {
    if (events == null) {
      events = new ArrayList();
    }

    events.add(val);
    eventInfo = val;
  }

  /** Returns the only event or the master event for a recurrence
   *
   * @return EventInfo
   * @throws WebdavIntfException
   */
  public EventInfo getEventInfo() throws WebdavIntfException {
    init(true);

    return eventInfo;
  }

  /**
   * @return Calendar
   * @throws WebdavIntfException
   */
  public Calendar getIcal() throws WebdavIntfException {
    init(true);

    try {
      if (ical == null) {
        if (events.size() == 1) {
          ical = getSysi().toCalendar(eventInfo.getEvent());
        } else {
          // recurring
          ical = getSysi().toCalendar(events);
        }
      }
      if ((veventString == null)) {
        veventString = ical.toString();
        contentLen = veventString.length();
      }
    } catch (Throwable t) {
      throw new WebdavIntfException(t);
    }

    return ical;
  }

  public Collection getProperties(String ns) throws WebdavIntfException {
    init(true);
    ArrayList al = new ArrayList();

    getVevent(); // init comp
    if (comp == null) {
      throw new WebdavIntfException("getProperties, comp == null");
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

  public String getContentString() throws WebdavIntfException {
    getIcal(); // init content

    return veventString;
  }

  /* ====================================================================
   *                   Overridden property methods
   * ==================================================================== */

  public CurrentAccess getCurrentAccess() throws WebdavIntfException {
    if (eventInfo == null) {
      return null;
    }

    return eventInfo.getCurrentAccess();
  }

  public void setLastmodDate(String val) throws WebdavIntfException {
    init(true);
    super.setLastmodDate(val);
  }

  public String getLastmodDate() throws WebdavIntfException {
    init(true);
    return super.getLastmodDate();
  }

  public int getContentLen() throws WebdavIntfException {
    getIcal(); // init length
    return contentLen;
  }

  public String toString() {
    StringBuffer sb = new StringBuffer();

    sb.append("CaldavComponentNode{cduri=");
    sb.append(getCDURI());
    sb.append("}");

    return sb.toString();
  }

  /* ====================================================================
   *                   Private methods
   * ==================================================================== */

  private BwEvent getEvent() throws WebdavIntfException {
    EventInfo ei = getEventInfo();

    if (ei == null) {
      return null;
    }

    return ei.getEvent();
  }

  private BwEvent checkEv(PropVal pv) throws WebdavIntfException {
    BwEvent ev = getEvent();

    if (ev == null) {
      pv.notFound = true;
      return null;
    }

    return ev;
  }

  private void addProp(Collection c, QName tag, Object val) {
    if (val != null) {
      c.add(new WebdavProperty(tag, String.valueOf(val)));
    }
  }
}
