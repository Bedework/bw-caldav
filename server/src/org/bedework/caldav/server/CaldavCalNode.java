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

import org.bedework.caldav.server.SysIntf.CalPrincipalInfo;
import org.bedework.calfacade.BwEvent;
import org.bedework.calfacade.RecurringRetrievalMode;
import org.bedework.calfacade.RecurringRetrievalMode.Rmode;
import org.bedework.icalendar.IcalTranslator;
import org.bedework.icalendar.Icalendar;
import org.bedework.icalendar.VFreeUtil;

import edu.rpi.cct.webdav.servlet.shared.WebdavBadRequest;
import edu.rpi.cct.webdav.servlet.shared.WebdavException;
import edu.rpi.cct.webdav.servlet.shared.WebdavForbidden;
import edu.rpi.cct.webdav.servlet.shared.WebdavNsIntf;
import edu.rpi.cmt.access.AccessPrincipal;
import edu.rpi.cmt.access.PrivilegeDefs;
import edu.rpi.cmt.access.Acl.CurrentAccess;
import edu.rpi.sss.util.DateTimeUtil;
import edu.rpi.sss.util.xml.XmlEmit;
import edu.rpi.sss.util.xml.XmlUtil;
import edu.rpi.sss.util.xml.tagdefs.AppleIcalTags;
import edu.rpi.sss.util.xml.tagdefs.AppleServerTags;
import edu.rpi.sss.util.xml.tagdefs.CaldavTags;
import edu.rpi.sss.util.xml.tagdefs.WebdavTags;

import net.fortuna.ical4j.model.Calendar;
import net.fortuna.ical4j.model.TimeZone;
import net.fortuna.ical4j.model.component.VFreeBusy;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;

import javax.servlet.http.HttpServletResponse;
import javax.xml.namespace.QName;

import org.w3c.dom.Element;

/** Class to represent a calendar in caldav.
 *
 *   @author Mike Douglass   douglm@rpi.edu
 */
public class CaldavCalNode extends CaldavBwNode {
  private Calendar ical;

  private AccessPrincipal owner;

  private String vfreeBusyString;

  private CurrentAccess currentAccess;

  private final static HashMap<QName, PropertyTagEntry> propertyNames =
    new HashMap<QName, PropertyTagEntry>();

  static {
    addPropEntry(propertyNames, CaldavTags.calendarDescription);
    addPropEntry(propertyNames, CaldavTags.calendarFreeBusySet);
    addPropEntry(propertyNames, CaldavTags.calendarTimezone);
    addPropEntry(propertyNames, CaldavTags.maxAttendeesPerInstance);
    addPropEntry(propertyNames, CaldavTags.maxDateTime);
    addPropEntry(propertyNames, CaldavTags.maxInstances);
    addPropEntry(propertyNames, CaldavTags.maxResourceSize);
    addPropEntry(propertyNames, CaldavTags.minDateTime);
    addPropEntry(propertyNames, CaldavTags.scheduleCalendarTransp);
    addPropEntry(propertyNames, CaldavTags.scheduleDefaultCalendarURL);
    addPropEntry(propertyNames, CaldavTags.supportedCalendarComponentSet);
    addPropEntry(propertyNames, CaldavTags.supportedCalendarData);
    addPropEntry(propertyNames, AppleServerTags.getctag);
    addPropEntry(propertyNames, AppleIcalTags.calendarColor);
  }

  /** Place holder for status
   *
   * @param sysi
   * @param status
   * @param uri
   * @param debug
   */
  public CaldavCalNode(SysIntf sysi, int status, String uri, boolean debug) {
    super(true, sysi, uri, debug);
    setStatus(status);
  }

  /**
   * @param cdURI
   * @param sysi
   * @param debug
   * @throws WebdavException
   */
  public CaldavCalNode(CaldavURI cdURI, SysIntf sysi, boolean debug) throws WebdavException {
    super(cdURI, sysi, debug);

    col = cdURI.getCol();
    collection = true;
    allowsGet = false;

    exists = cdURI.getExists();
  }

  /* (non-Javadoc)
   * @see edu.rpi.cct.webdav.servlet.shared.WebdavNsNode#getOwner()
   */
  public AccessPrincipal getOwner() throws WebdavException {
    if (owner == null) {
      if (col == null) {
        return null;
      }

      owner = col.getOwner();
    }

    return owner;
  }

  public void init(boolean content) throws WebdavException {
    if (!content) {
      return;
    }
  }

  /* (non-Javadoc)
   * @see edu.rpi.cct.webdav.servlet.shared.WebdavNsNode#getEtagValue(boolean)
   */
  public String getEtagValue(boolean strong) throws WebdavException {
    /* We need the etag of the target if this is an alias */
    CalDAVCollection c = (CalDAVCollection)getCollection(true); // deref

    if (c == null) {
      return null;
    }

    String val = c.getTagValue();

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
    /* It's the alias target that matters */
    CalDAVCollection c = (CalDAVCollection)getCollection(true); // deref

    if (c == null) {
      return false;
    }

    int type = c.getCalType();
    if (type == CalDAVCollection.calTypeInbox) {
      return true;
    }

    if (type == CalDAVCollection.calTypeOutbox) {
      return true;
    }

    if (type == CalDAVCollection.calTypeCalendarCollection) {
      return true;
    }

    return false;
  }

  /**
   * @param methodTag - acts as a flag for the method type
   * @throws WebdavException
   */
  public void setDefaults(QName methodTag) throws WebdavException {
    if (!CaldavTags.mkcalendar.equals(methodTag)) {
      return;
    }

    CalDAVCollection c = (CalDAVCollection)getCollection(false); // Don't deref

    c.setCalType(CalDAVCollection.calTypeCalendarCollection);
  }

  public Collection getChildren() throws WebdavException {
    /* For the moment we're going to do this the inefficient way.
       We really need to have calendar defs that can be expressed as a search
       allowing us to retrieve all the ids of objects within a calendar.
       */

    try {
      CalDAVCollection c = (CalDAVCollection)getCollection(true); // deref

      if (c == null) { // no access?
        return null;
      }

      if (!c.entitiesAllowed()) {
        if (debug) {
          debugMsg("POSSIBLE SEARCH: getChildren for cal " + c.getPath());
        }

        ArrayList ch = new ArrayList();
        ch.addAll(getSysi().getCollections(c));
        ch.addAll(getSysi().getFiles(c));

        return ch;
      }

      /* Otherwise, return the events in this calendar */
      if (debug) {
        debugMsg("Get all resources in calendar " + c.getPath());
      }

      RecurringRetrievalMode rrm = new RecurringRetrievalMode(
                           Rmode.overrides);
      return getSysi().getEvents(c, null, rrm);
    } catch (Throwable t) {
      throw new WebdavException(t);
    }
  }

  /**
   * @param fb
   * @throws WebdavException
   */
  public void setFreeBusy(BwEvent fb) throws WebdavException {
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

  /* (non-Javadoc)
   * @see edu.rpi.cct.webdav.servlet.shared.WebdavNsNode#getContentString()
   */
  public String getContentString() throws WebdavException {
    init(true);

    if (ical == null) {
      return null;
    }

    return vfreeBusyString;
  }

  /* (non-Javadoc)
   * @see edu.rpi.cct.webdav.servlet.shared.WebdavNsNode#update()
   */
  public void update() throws WebdavException {
    // ALIAS probably not unaliasing here
    if (col != null) {
      getSysi().updateCollection(col);
    }
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
      return "text/calendar; charset=UTF-8";
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
    if (col == null) {
      return null;
    }

    return col.getName();
  }

  /* (non-Javadoc)
   * @see edu.rpi.cct.webdav.servlet.shared.WebdavNsNode#getLastmodDate()
   */
  public String getLastmodDate() throws WebdavException {
    init(false);
    if (col == null) {
      return null;
    }

    try {
      return DateTimeUtil.fromISODateTimeUTCtoRfc822(col.getLastmod());
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

    CalDAVCollection c = (CalDAVCollection)getCollection(true); // We want access of underlying object?

    if (c == null) {
      return null;
    }

    try {
      currentAccess = getSysi().checkAccess(c, PrivilegeDefs.privAny, true);
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
      if (XmlUtil.nodeMatches(val, WebdavTags.description)) {
        if (checkCalForSetProp(spr)) {
          col.setDescription(XmlUtil.getElementContent(val));
        }
        return true;
      }

      if (XmlUtil.nodeMatches(val, CaldavTags.calendarDescription)) {
        if (checkCalForSetProp(spr)) {
          col.setDescription(XmlUtil.getElementContent(val));
        }
        return true;
      }

      if (XmlUtil.nodeMatches(val, WebdavTags.displayname)) {
        if (checkCalForSetProp(spr)) {
          col.setDisplayName(XmlUtil.getElementContent(val));
        }
        return true;
      }

      if (XmlUtil.nodeMatches(val, WebdavTags.resourcetype)) {
        Collection<Element> propVals = XmlUtil.getElements(val);

        for (Element pval: propVals) {
          if (XmlUtil.nodeMatches(pval, WebdavTags.collection)) {
            // Fine
            continue;
          }

          if (XmlUtil.nodeMatches(pval, CaldavTags.calendar)) {
            // Fine again
            continue;
          }
        }

        return true;
      }

      if (XmlUtil.nodeMatches(val, CaldavTags.scheduleCalendarTransp)) {
        Element cval = XmlUtil.getOnlyElement(val);

        if (XmlUtil.nodeMatches(cval, CaldavTags.opaque)) {
          col.setAffectsFreeBusy(true);
        } else if (XmlUtil.nodeMatches(cval, CaldavTags.transparent)) {
          col.setAffectsFreeBusy(true);
        } else {
          throw new WebdavBadRequest();
        }

        return true;
      }

      if (XmlUtil.nodeMatches(val, CaldavTags.calendarFreeBusySet)) {
        // Only valid for inbox
        if (col.getCalType() != CalDAVCollection.calTypeInbox) {
          throw new WebdavForbidden("Not on inbox");
        }

        spr.status = HttpServletResponse.SC_NOT_IMPLEMENTED;
        spr.message = "Unimplemented - calendarFreeBusySet";
        warn("Unimplemented - calendarFreeBusySet");
        return true;
      }

      if (XmlUtil.nodeMatches(val, CaldavTags.calendarTimezone)) {
        try {
          StringReader sr = new StringReader(XmlUtil.getElementContent(val));

          // This call automatically saves the timezone in the db
          Icalendar ic = getSysi().fromIcal(col, sr);

          if ((ic == null) ||
              (ic.size() != 0) || // No components other than timezones
              (ic.getTimeZones().size() != 1)) {
            if (debug) {
              debugMsg("Not icalendar");
            }
            throw new WebdavForbidden(CaldavTags.validCalendarData, "Not icalendar");
          }

          TimeZone tz = ic.getTimeZones().iterator().next();

          col.setTimezone(tz.getID());
        } catch (WebdavException wde) {
          throw wde;
        } catch (Throwable t) {
          throw new WebdavForbidden(CaldavTags.validCalendarData, "Not icalendar");
        }

        return true;
      }

      if (XmlUtil.nodeMatches(val, AppleIcalTags.calendarColor)) {
        col.setColor(XmlUtil.getElementContent(val));

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
   * @see edu.rpi.cct.webdav.servlet.shared.WebdavNsNode#knownProperty(edu.rpi.sss.util.xml.QName)
   */
  public boolean knownProperty(QName tag) {
    if (propertyNames.get(tag) != null) {
      return true;
    }

    // Not ours
    return super.knownProperty(tag);
  }

  /* (non-Javadoc)
   * @see edu.rpi.cct.webdav.servlet.shared.WebdavNsNode#generatePropertyValue(edu.rpi.sss.util.xml.QName, edu.rpi.cct.webdav.servlet.shared.WebdavNsIntf, boolean)
   */
  public boolean generatePropertyValue(QName tag,
                                       WebdavNsIntf intf,
                                       boolean allProp) throws WebdavException {
    XmlEmit xml = intf.getXmlEmit();

    try {
      CalDAVCollection c = (CalDAVCollection)getCollection(true); // deref this
      if (c == null) {
        // Probably no access
        return false;
      }

      int calType = c.getCalType();

      if (tag.equals(WebdavTags.resourcetype)) {
        // dav 13.9
        xml.openTag(tag);
        xml.emptyTag(WebdavTags.collection);
        if (debug) {
          debugMsg("generatePropResourcetype for " + col);
        }

        //boolean isCollection = cal.getCalendarCollection();

        if (calType == CalDAVCollection.calTypeInbox) {
          xml.emptyTag(CaldavTags.scheduleInbox);
        } else if (calType == CalDAVCollection.calTypeOutbox) {
          xml.emptyTag(CaldavTags.scheduleOutbox);
        } else if (calType == CalDAVCollection.calTypeCalendarCollection) {
          xml.emptyTag(CaldavTags.calendar);
        }
        xml.closeTag(tag);

        return true;
      }

      if (tag.equals(CaldavTags.scheduleCalendarTransp)) {
        xml.openTag(tag);

        if (c.getAffectsFreeBusy()) {
          xml.emptyTag(CaldavTags.opaque);
        } else {
          xml.emptyTag(CaldavTags.transparent);
        }

        xml.closeTag(tag);

        return true;
      }

      if (tag.equals(CaldavTags.scheduleDefaultCalendarURL) &&
          (calType == CalDAVCollection.calTypeInbox)) {
        xml.openTag(tag);

        CalPrincipalInfo cinfo = getSysi().getCalPrincipalInfo(getOwner());
        if (cinfo.defaultCalendarPath != null) {
          generateHref(xml, cinfo.defaultCalendarPath);
        }

        xml.closeTag(tag);

        return true;
      }

      if (tag.equals(AppleServerTags.getctag)) {
        xml.property(tag, col.getTagValue());

        return true;
      }

      if (tag.equals(AppleIcalTags.calendarColor)) {
       String val = col.getColor();

        if (val == null) {
          return false;
        }

        xml.property(tag, val);

        return true;
      }

      if (tag.equals(CaldavTags.calendarDescription)) {
        xml.property(tag, col.getDescription());

        return true;
      }

      if ((col.getCalType() == CalDAVCollection.calTypeInbox) &&
          (tag.equals(CaldavTags.calendarFreeBusySet))) {
        xml.openTag(tag);

        Collection<String> hrefs = getSysi().getFreebusySet();

        for (String href: hrefs) {
          xml.property(WebdavTags.href, href);
        }
        xml.closeTag(tag);

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
        if (calType != CalDAVCollection.calTypeCalendarCollection) {
          return true;
        }

        xml.openTag(tag);
        xml.startTag(CaldavTags.comp);
        xml.attribute("name", "VEVENT");
        xml.endEmptyTag();
        xml.newline();
        xml.startTag(CaldavTags.comp);
        xml.attribute("name", "VTODO");
        xml.endEmptyTag();
        xml.newline();
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
        xml.attribute("content-type", "text/calendar");
        xml.attribute("version", "2.0");
        xml.endEmptyTag();
        xml.newline();
        xml.closeTag(tag);
        return true;
      }

      if (tag.equals(CaldavTags.calendarTimezone)) {
        String tzid = col.getTimezone();

        if (tzid == null) {
          return false;
        }

        String val = getSysi().toStringTzCalendar(tzid);

        if (val == null) {
          return false;
        }

        xml.cdataProperty(tag, val);

        return true;
      }

      // Not known - try higher
      return super.generatePropertyValue(tag, intf, allProp);
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

  /* (non-Javadoc)
   * @see org.bedework.caldav.server.CaldavBwNode#getSupportedReports()
   */
  public Collection<QName> getSupportedReports() throws WebdavException {
    Collection<QName> res = new ArrayList<QName>();
    CalDAVCollection c = (CalDAVCollection)getCollection(true); // deref

    if (c == null) {
      return res;
    }

    res.addAll(super.getSupportedReports());

    /* Cannot do free-busy on in and outbox */
    if (c.freebusyAllowed()) {
      res.add(CaldavTags.freeBusyQuery);    // Calendar access
    }

    return res;
  }

  /* ====================================================================
   *                   Object methods
   * ==================================================================== */

  public String toString() {
    StringBuilder sb = new StringBuilder();

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
    if (col != null) {
      return true;
    }

    spr.status = HttpServletResponse.SC_NOT_FOUND;
    spr.message = "Not found";
    return false;
  }
}
