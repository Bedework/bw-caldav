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

import org.bedework.calfacade.BwCalendar;
import org.bedework.calfacade.BwFreeBusy;
import org.bedework.calfacade.RecurringRetrievalMode;
import org.bedework.davdefs.CaldavDefs;
import org.bedework.davdefs.CaldavTags;
import org.bedework.davdefs.WebdavTags;
import org.bedework.icalendar.IcalTranslator;
import org.bedework.icalendar.Icalendar;
import org.bedework.icalendar.VFreeUtil;

import edu.rpi.cct.webdav.servlet.shared.WebdavIntfException;
import edu.rpi.cct.webdav.servlet.shared.WebdavNsIntf;
import edu.rpi.cmt.access.PrivilegeDefs;
import edu.rpi.cmt.access.Acl.CurrentAccess;
import edu.rpi.sss.util.xml.QName;
import edu.rpi.sss.util.xml.XmlEmit;
import edu.rpi.sss.util.xml.XmlUtil;

import net.fortuna.ical4j.model.Calendar;
import net.fortuna.ical4j.model.component.VFreeBusy;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;

import org.w3c.dom.Element;

/** Class to represent a calendar in caldav.
 *
 *   @author Mike Douglass   douglm@rpi.edu
 */
public class CaldavCalNode extends CaldavBwNode {
  private Calendar ical;

  private String vfreeBusyString;

  private CurrentAccess currentAccess;

  private final static HashMap<QName, PropertyTagEntry> propertyNames =
    new HashMap<QName, PropertyTagEntry>();

  static {
    addPropEntry(propertyNames, CaldavTags.calendarDescription, false);
    addPropEntry(propertyNames, CaldavTags.calendarTimezone, false);
    addPropEntry(propertyNames, CaldavTags.maxAttendeesPerInstance);
    addPropEntry(propertyNames, CaldavTags.maxDateTime);
    addPropEntry(propertyNames, CaldavTags.maxInstances);
    addPropEntry(propertyNames, CaldavTags.maxResourceSize);
    addPropEntry(propertyNames, CaldavTags.minDateTime);
    addPropEntry(propertyNames, CaldavTags.supportedCalendarComponentSet, false);
    addPropEntry(propertyNames, CaldavTags.supportedCalendarData, false);
  }

  /** Place holder for status
   *
   * @param status
   * @param debug
   */
  public CaldavCalNode(int status, boolean debug) {
    super(null, null, debug);
    setStatus(status);
  }

  /**
   * @param cdURI
   * @param sysi
   * @param debug
   */
  public CaldavCalNode(CaldavURI cdURI, SysIntf sysi, boolean debug) {
    super(cdURI, sysi, debug);

    collection = true;
    allowsGet = false;

    if (!uri.endsWith("/")) {
      uri += "/";
    }
  }

  public void init(boolean content) throws WebdavIntfException {
    if (!content) {
      return;
    }
  }

  public String getEtagValue(boolean strong) throws WebdavIntfException {
    BwCalendar cal = getCDURI().getCal();
    if (cal == null) {
      return null;
    }

    String val = cal.getLastmod() + "-" + cal.getSeq();

    if (strong) {
      return "\"" + val + "\"";
    }

    return "W/\"" + val + "\"";
  }

  public boolean removeProperty(Element val) throws WebdavIntfException {
    warn("Unimplemented - removeProperty");
    return false;
  }

  public boolean setProperty(Element val) throws WebdavIntfException {
    BwCalendar cal = getCDURI().getCal();
    if (cal == null) {
      return false;
    }

    try {
      String str = XmlUtil.getElementContent(val);

      if (WebdavTags.description.nodeMatches(val)) {
        cal.setDescription(str);
      } else if (WebdavTags.displayname.nodeMatches(val)) {
        cal.setSummary(str);
      } else if (CaldavTags.calendarFreeBusySet.nodeMatches(val)) {
        // Only valid for inbox
        if (cal.getCalType() != BwCalendar.calTypeInbox) {
          throw WebdavIntfException.forbidden();
        }

        warn("Unimplemented - calendarFreeBusySet: got " + str);
      }
      return false;
    } catch (WebdavIntfException wie) {
      throw wie;
    } catch (Throwable t) {
      throw new WebdavIntfException(t);
    }
  }

  public Collection getChildren() throws WebdavIntfException {
    /* For the moment we're going to do this the inefficient way.
       We really need to have calendar defs that can be expressed as a search
       allowing us to retrieve all the ids of objects within a calendar.
       */

    try {
      BwCalendar cal = cdURI.getCal();

      if (cal.hasChildren()) {
        if (debug) {
          debugMsg("POSSIBLE SEARCH: getChildren for cal " + cal.getId());
        }
        return getSysi().getCalendars(cal);
      }

      /* Othewise, return the events in this calendar */
      if (debug) {
        debugMsg("Get all resources in calendar " + cal.getPath());
      }

      RecurringRetrievalMode rrm = new RecurringRetrievalMode(
                           RecurringRetrievalMode.overrides);
      return getSysi().getEvents(cal, true, true, true, null, null, rrm);
    } catch (Throwable t) {
      throw new WebdavIntfException(t);
    }
  }

  /**
   * @param fb
   * @throws WebdavIntfException
   */
  public void setFreeBusy(BwFreeBusy fb) throws WebdavIntfException {
    try {
      VFreeBusy vfreeBusy = VFreeUtil.toVFreeBusy(fb);
      if (vfreeBusy != null) {
        ical = IcalTranslator.newIcal(Icalendar.methodTypeNone);
        ical.getComponents().add(vfreeBusy);
        vfreeBusyString = ical.toString();
      } else {
        vfreeBusyString = null;
      }
      allowsGet = true;
    } catch (Throwable t) {
      if (debug) {
        error(t);
      }
      throw new WebdavIntfException(t);
    }
  }

  public String getContentString() throws WebdavIntfException {
    init(true);

    if (ical == null) {
      return null;
    }

    return vfreeBusyString;
  }

  /* ====================================================================
   *                   Required webdav properties
   * ==================================================================== */

  /* (non-Javadoc)
   * @see edu.rpi.cct.webdav.servlet.shared.WebdavNsNode#getContentLang()
   */
  public String getContentLang() throws WebdavIntfException {
    return "en";
  }

  /* (non-Javadoc)
   * @see edu.rpi.cct.webdav.servlet.shared.WebdavNsNode#getContentLen()
   */
  public int getContentLen() throws WebdavIntfException {
    if (vfreeBusyString != null) {
      return vfreeBusyString.length();
    }

    return 0;
  }

  /* (non-Javadoc)
   * @see edu.rpi.cct.webdav.servlet.shared.WebdavNsNode#getContentType()
   */
  public String getContentType() throws WebdavIntfException {
    if (vfreeBusyString != null) {
      return "text/calendar";
    }

    return null;
  }

  /* (non-Javadoc)
   * @see edu.rpi.cct.webdav.servlet.shared.WebdavNsNode#getCreDate()
   */
  public String getCreDate() throws WebdavIntfException {
    return null;
  }

  /* (non-Javadoc)
   * @see edu.rpi.cct.webdav.servlet.shared.WebdavNsNode#getDisplayname()
   */
  public String getDisplayname() throws WebdavIntfException {
    if (cdURI == null) {
      return null;
    }

    return cdURI.getCalName();
  }

  /* (non-Javadoc)
   * @see edu.rpi.cct.webdav.servlet.shared.WebdavNsNode#getLastmodDate()
   */
  public String getLastmodDate() throws WebdavIntfException {
    init(false);
    BwCalendar cal = getCDURI().getCal();
    if (cal == null) {
      return null;
    }

    return cal.getLastmod();
  }

  /* ====================================================================
   *                   Abstract methods
   * ==================================================================== */

  public CurrentAccess getCurrentAccess() throws WebdavIntfException {
    if (currentAccess != null) {
      return currentAccess;
    }

    BwCalendar cal = getCDURI().getCal();
    if (cal == null) {
      return null;
    }

    try {
      currentAccess = getSysi().checkAccess(cal, PrivilegeDefs.privAny, true);
    } catch (Throwable t) {
      throw new WebdavIntfException(t);
    }

    return currentAccess;
  }

  /* ====================================================================
   *                   Property methods
   * ==================================================================== */

  /* (non-Javadoc)
   * @see edu.rpi.cct.webdav.servlet.shared.WebdavNsNode#generatePropertyValue(edu.rpi.sss.util.xml.QName, edu.rpi.cct.webdav.servlet.shared.WebdavNsIntf, boolean)
   */
  public boolean generatePropertyValue(QName tag,
                                       WebdavNsIntf intf,
                                       boolean allProp) throws WebdavIntfException {
    String ns = tag.getNamespaceURI();
    XmlEmit xml = intf.getXmlEmit();

    BwCalendar cal = getCDURI().getCal();

    try {
      if (tag.equals(WebdavTags.resourcetype)) {
        // dav 13.9
        xml.openTag(WebdavTags.resourcetype);
        xml.emptyTag(WebdavTags.collection);
        if (debug) {
          debugMsg("generatePropResourcetype for " + cal);
        }

        int calType = cal.getCalType();
        boolean isCollection = cal.getCalendarCollection();

        if (calType == BwCalendar.calTypeInbox) {
          xml.emptyTag(CaldavTags.scheduleInbox);
        } else if (calType == BwCalendar.calTypeOutbox) {
          xml.emptyTag(CaldavTags.scheduleOutbox);
        } else if (isCollection) {
          xml.emptyTag(CaldavTags.calendar);
        }
        xml.closeTag(WebdavTags.resourcetype);

        return true;
      }

      /* Deal with webdav properties */
      if ((!ns.equals(CaldavDefs.caldavNamespace) &&
          !ns.equals(CaldavDefs.icalNamespace))) {
        // Not ours
        return super.generatePropertyValue(tag, intf, allProp);
      }

      if (tag.equals(CaldavTags.calendarDescription)) {
        xml.property(tag, cal.getDescription());

        return true;
      }

      if (tag.equals(CaldavTags.supportedCalendarComponentSet)) {
        /* e.g.
         *          <C:supported-calendar-component-set
         *                 xmlns:C="urn:ietf:params:xml:ns:caldav">
         *            <C:comp name="VEVENT"/>
         *            <C:comp name="VTODO"/>
         *         </C:supported-calendar-component-set>
         */
        if (!cal.getCalendarCollection()) {
          return true;
        }

        xml.openTag(tag);
        xml.startTag(CaldavTags.comp);
        xml.atribute("name", "VEVENT");
        xml.closeTag(tag);
        return true;
      }

      if (tag.equals(CaldavTags.supportedCalendarData)) {
        /* e.g.
         * <C:supported-calendar-data
         *              xmlns:C="urn:ietf:params:xml:ns:caldav">
         *   <C:calendar-data content-type="text/calendar" version="2.0"/>
         * </C:supported-calendar-data>
         */
        xml.openTag(tag);
        xml.startTag(CaldavTags.calendarData);
        xml.atribute("content-type", "text/calendar");
        xml.atribute("version", "2.0");
        xml.closeTag(tag);
        return true;
      }

      return false;
    } catch (WebdavIntfException wde) {
      throw wde;
    } catch (Throwable t) {
      throw new WebdavIntfException(t);
    }
  }

  /** Return a set of QName defining properties this node supports.
   *
   * @return
   * @throws WebdavIntfException
   */
  public Collection<PropertyTagEntry> getPropertyNames()throws WebdavIntfException {
    Collection<PropertyTagEntry> res = new ArrayList<PropertyTagEntry>();

    res.addAll(super.getPropertyNames());
    res.addAll(propertyNames.values());

    return res;
  }

  /* ====================================================================
   *                   Object methods
   * ==================================================================== */

  public String toString() {
    StringBuffer sb = new StringBuffer();

    sb.append("CaldavCalNode{cduri=");
    sb.append(getCDURI());
    sb.append(", isCalendar()=");
    sb.append(isCollection());
    sb.append("}");

    return sb.toString();
  }

  public Object clone() {
    return new CaldavCalNode(getCDURI(), getSysi(), debug);
  }

  /* ====================================================================
   *                   Private methods
   * ==================================================================== */
}
