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
import org.bedework.calfacade.BwUser;
import org.bedework.calfacade.RecurringRetrievalMode;
import org.bedework.calfacade.RecurringRetrievalMode.Rmode;
import org.bedework.calfacade.util.CalFacadeUtil;
import org.bedework.davdefs.CaldavDefs;
import org.bedework.davdefs.CaldavTags;
import org.bedework.davdefs.WebdavTags;
import org.bedework.icalendar.IcalTranslator;
import org.bedework.icalendar.Icalendar;
import org.bedework.icalendar.VFreeUtil;

import edu.rpi.cct.webdav.servlet.shared.WebdavException;
import edu.rpi.cct.webdav.servlet.shared.WebdavForbidden;
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

import javax.servlet.http.HttpServletResponse;

import org.w3c.dom.Element;

/** Class to represent a calendar in caldav.
 *
 *   @author Mike Douglass   douglm@rpi.edu
 */
public class CaldavCalNode extends CaldavBwNode {
  private Calendar ical;

  private BwCalendar cal;

  private BwUser owner;

  private String vfreeBusyString;

  private CurrentAccess currentAccess;

  private final static HashMap<QName, PropertyTagEntry> propertyNames =
    new HashMap<QName, PropertyTagEntry>();

  static {
    addPropEntry(propertyNames, CaldavTags.calendarDescription);
    addPropEntry(propertyNames, CaldavTags.calendarTimezone);
    addPropEntry(propertyNames, CaldavTags.maxAttendeesPerInstance);
    addPropEntry(propertyNames, CaldavTags.maxDateTime);
    addPropEntry(propertyNames, CaldavTags.maxInstances);
    addPropEntry(propertyNames, CaldavTags.maxResourceSize);
    addPropEntry(propertyNames, CaldavTags.minDateTime);
    addPropEntry(propertyNames, CaldavTags.supportedCalendarComponentSet);
    addPropEntry(propertyNames, CaldavTags.supportedCalendarData);
  }

  /** Place holder for status
   *
   * @param sysi
   * @param status
   * @param uri
   * @param debug
   */
  public CaldavCalNode(SysIntf sysi, int status, String uri, boolean debug) {
    super(true, sysi, debug);
    setStatus(status);
    this.uri = uri;
  }

  /**
   * @param cdURI
   * @param sysi
   * @param debug
   */
  public CaldavCalNode(CaldavURI cdURI, SysIntf sysi, boolean debug) {
    super(cdURI, sysi, debug);

    cal = cdURI.getCal();
    collection = true;
    allowsGet = false;

    if (!uri.endsWith("/")) {
      uri += "/";
    }
  }

  /**
   * @return BwCalendar this node represents
   */
  public BwCalendar getCalendar() {
    return cal;
  }

  /* (non-Javadoc)
   * @see edu.rpi.cct.webdav.servlet.shared.WebdavNsNode#getOwner()
   */
  public String getOwner() throws WebdavException {
    if (owner == null) {
      if (cal == null) {
        return null;
      }

      owner = cal.getOwner();
    }

    if (owner != null) {
      return owner.getAccount();
    }

    return null;
  }

  public void init(boolean content) throws WebdavException {
    if (!content) {
      return;
    }
  }

  public String getEtagValue(boolean strong) throws WebdavException {
    if (cal == null) {
      return null;
    }

    String val = cal.getLastmod() + "-" + cal.getSeq();

    if (strong) {
      return "\"" + val + "\"";
    }

    return "W/\"" + val + "\"";
  }

  /**
   * @return true if scheduling allowed
   * @throws WebdavException
   */
  public boolean getSchedulingAllowed() throws WebdavException {
    if (cal == null) {
      return false;
    }

    int type = cal.getCalType();
    if (type == BwCalendar.calTypeInbox) {
      return true;
    }

    if (type == BwCalendar.calTypeOutbox) {
      return true;
    }

    return false;
  }

  public Collection getChildren() throws WebdavException {
    /* For the moment we're going to do this the inefficient way.
       We really need to have calendar defs that can be expressed as a search
       allowing us to retrieve all the ids of objects within a calendar.
       */

    try {
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
                           Rmode.overrides);
      return getSysi().getEvents(cal, null, null, null, rrm);
    } catch (Throwable t) {
      throw new WebdavException(t);
    }
  }

  /**
   * @param fb
   * @throws WebdavException
   */
  public void setFreeBusy(BwFreeBusy fb) throws WebdavException {
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
      throw new WebdavException(t);
    }
  }

  public String getContentString() throws WebdavException {
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
  public String getContentLang() throws WebdavException {
    return "en";
  }

  /* (non-Javadoc)
   * @see edu.rpi.cct.webdav.servlet.shared.WebdavNsNode#getContentLen()
   */
  public int getContentLen() throws WebdavException {
    if (vfreeBusyString != null) {
      return vfreeBusyString.length();
    }

    return 0;
  }

  /* (non-Javadoc)
   * @see edu.rpi.cct.webdav.servlet.shared.WebdavNsNode#getContentType()
   */
  public String getContentType() throws WebdavException {
    if (vfreeBusyString != null) {
      return "text/calendar";
    }

    return null;
  }

  /* (non-Javadoc)
   * @see edu.rpi.cct.webdav.servlet.shared.WebdavNsNode#getCreDate()
   */
  public String getCreDate() throws WebdavException {
    return null;
  }

  /* (non-Javadoc)
   * @see edu.rpi.cct.webdav.servlet.shared.WebdavNsNode#getDisplayname()
   */
  public String getDisplayname() throws WebdavException {
    if (cal == null) {
      return null;
    }

    return cal.getName();
  }

  /* (non-Javadoc)
   * @see edu.rpi.cct.webdav.servlet.shared.WebdavNsNode#getLastmodDate()
   */
  public String getLastmodDate() throws WebdavException {
    init(false);
    if (cal == null) {
      return null;
    }

    try {
      return CalFacadeUtil.fromISODateTimeUTCtoRfc822(cal.getLastmod());
    } catch (Throwable t) {
      throw new WebdavException(t);
    }
  }

  /* ====================================================================
   *                   Abstract methods
   * ==================================================================== */

  public CurrentAccess getCurrentAccess() throws WebdavException {
    if (currentAccess != null) {
      return currentAccess;
    }

    if (cal == null) {
      return null;
    }

    try {
      currentAccess = getSysi().checkAccess(cal, PrivilegeDefs.privAny, true);
    } catch (Throwable t) {
      throw new WebdavException(t);
    }

    return currentAccess;
  }

  /* (non-Javadoc)
   * @see edu.rpi.cct.webdav.servlet.shared.WebdavNsNode#trailSlash()
   */
  public boolean trailSlash() {
    return true;
  }

  /* ====================================================================
   *                   Property methods
   * ==================================================================== */

  /* (non-Javadoc)
   * @see edu.rpi.cct.webdav.servlet.shared.WebdavNsNode#removeProperty(org.w3c.dom.Element)
   */
  public boolean removeProperty(Element val,
                                SetPropertyResult spr) throws WebdavException {
    warn("Unimplemented - removeProperty");

    return false;
  }

  /* (non-Javadoc)
   * @see edu.rpi.cct.webdav.servlet.shared.WebdavNsNode#setProperty(org.w3c.dom.Element)
   */
  public boolean setProperty(Element val,
                             SetPropertyResult spr) throws WebdavException {
    if (super.setProperty(val, spr)) {
      return true;
    }

    try {
      if (WebdavTags.description.nodeMatches(val)) {
        if (checkCalForSetProp(spr)) {
          cal.setDescription(XmlUtil.getElementContent(val));
        }
        return true;
      }

      if (CaldavTags.calendarDescription.nodeMatches(val)) {
        if (checkCalForSetProp(spr)) {
          cal.setDescription(XmlUtil.getElementContent(val));
        }
        return true;
      }

      if (WebdavTags.displayname.nodeMatches(val)) {
        if (checkCalForSetProp(spr)) {
          cal.setSummary(XmlUtil.getElementContent(val));
        }
        return true;
      }

      if (CaldavTags.calendarFreeBusySet.nodeMatches(val)) {
        // Only valid for inbox
        if (cal.getCalType() != BwCalendar.calTypeInbox) {
          throw new WebdavForbidden("Not on inbox");
        }

        spr.status = HttpServletResponse.SC_NOT_IMPLEMENTED;
        spr.message = "Unimplemented - calendarFreeBusySet";
        warn("Unimplemented - calendarFreeBusySet");
        return true;
      }

      if (CaldavTags.calendarTimezone.nodeMatches(val)) {
        warn("Unimplemented - calendarTimezone");
        return true;
      }

      return false;
    } catch (WebdavException wde) {
      throw wde;
    } catch (Throwable t) {
      throw new WebdavException(t);
    }
  }

  /* (non-Javadoc)
   * @see edu.rpi.cct.webdav.servlet.shared.WebdavNsNode#generatePropertyValue(edu.rpi.sss.util.xml.QName, edu.rpi.cct.webdav.servlet.shared.WebdavNsIntf, boolean)
   */
  public boolean generatePropertyValue(QName tag,
                                       WebdavNsIntf intf,
                                       boolean allProp) throws WebdavException {
    String ns = tag.getNamespaceURI();
    XmlEmit xml = intf.getXmlEmit();

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
    } catch (WebdavException wde) {
      throw wde;
    } catch (Throwable t) {
      throw new WebdavException(t);
    }
  }

  /** Return a set of PropertyTagEntry defining properties this node supports.
   *
   * @return Collection of PropertyTagEntry
   * @throws WebdavException
   */
  public Collection<PropertyTagEntry> getPropertyNames()throws WebdavException {
    Collection<PropertyTagEntry> res = new ArrayList<PropertyTagEntry>();

    res.addAll(super.getPropertyNames());
    res.addAll(propertyNames.values());

    return res;
  }

  /** Return a set of Qname defining reports this node supports.
   *
   * @return Collection of QName
   * @throws WebdavException
   */
  public Collection<QName> getSupportedReports() throws WebdavException {
    Collection<QName> res = new ArrayList<QName>();
    res.addAll(super.getSupportedReports());

    /* Cannot do free-busy on in and outbox */
    if ((cal.getCalType() == BwCalendar.calTypeCollection) ||
        (cal.getCalType() == BwCalendar.calTypeFolder)) {
      res.add(CaldavTags.freeBusyQuery);    // Calendar access
    }

    return res;
  }

  /* ====================================================================
   *                   Object methods
   * ==================================================================== */

  public String toString() {
    StringBuffer sb = new StringBuffer();

    sb.append("CaldavCalNode{cduri=");
    sb.append("path=");
    sb.append(getPath());
    sb.append(", isCalendarCollection()=");
    try {
      sb.append(isCalendarCollection());
    } catch (Throwable t) {
      sb.append("exception(" + t.getMessage() + ")");
    }
    sb.append("}");

    return sb.toString();
  }

  /* ====================================================================
   *                   Private methods
   * ==================================================================== */

  private boolean checkCalForSetProp(SetPropertyResult spr) {
    if (cal != null) {
      return true;
    }

    spr.status = HttpServletResponse.SC_NOT_FOUND;
    spr.message = "Not found";
    return false;
  }
}
