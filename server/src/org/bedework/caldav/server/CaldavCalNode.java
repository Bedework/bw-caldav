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

import org.bedework.caldav.server.sysinterface.CalPrincipalInfo;
import org.bedework.caldav.server.sysinterface.SysIntf;
import org.bedework.caldav.server.sysinterface.SysIntf.MethodEmitted;

import edu.rpi.cct.webdav.servlet.shared.WdEntity;
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
import edu.rpi.sss.util.xml.tagdefs.CalWSXrdDefs;
import edu.rpi.sss.util.xml.tagdefs.CaldavTags;
import edu.rpi.sss.util.xml.tagdefs.WebdavTags;

import org.w3c.dom.Element;

import java.io.Writer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;

import javax.servlet.http.HttpServletResponse;
import javax.xml.namespace.QName;

/** Class to represent a calendar in caldav.
 *
 *   @author Mike Douglass   douglm@rpi.edu
 */
public class CaldavCalNode extends CaldavBwNode {
  private CalDAVEvent ical;

  private AccessPrincipal owner;

  private CurrentAccess currentAccess;

  private final static HashMap<QName, PropertyTagEntry> propertyNames =
    new HashMap<QName, PropertyTagEntry>();

  private final static HashMap<String, PropertyTagXrdEntry> xrdNames =
    new HashMap<String, PropertyTagXrdEntry>();

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

    //addXrdEntry(xrdNames, CalWSXrdDefs.calendarCollection, true);
    addXrdEntry(xrdNames, CalWSXrdDefs.collection, true, true); // for all resource types
    addXrdEntry(xrdNames, CalWSXrdDefs.description, true, false);
    addXrdEntry(xrdNames, CalWSXrdDefs.maxAttendeesPerInstance, true, false);
    addXrdEntry(xrdNames, CalWSXrdDefs.maxDateTime, true, false);
    addXrdEntry(xrdNames, CalWSXrdDefs.maxInstances, true, false);
    addXrdEntry(xrdNames, CalWSXrdDefs.maxResourceSize, true, false);
    addXrdEntry(xrdNames, CalWSXrdDefs.minDateTime, true, false);
    addXrdEntry(xrdNames, CalWSXrdDefs.principalHome, true, true);
    addXrdEntry(xrdNames, CalWSXrdDefs.timezone, true, false);
  }

  /** Place holder for status
   *
   * @param sysi
   * @param status
   * @param uri
   * @param debug
   */
  public CaldavCalNode(final SysIntf sysi, final int status,
                       final String uri, final boolean debug) {
    super(true, sysi, uri, debug);
    setStatus(status);
  }

  /**
   * @param cdURI
   * @param sysi
   * @param debug
   * @throws WebdavException
   */
  public CaldavCalNode(final CaldavURI cdURI,
                       final SysIntf sysi,
                       final boolean debug) throws WebdavException {
    super(cdURI, sysi, debug);

    col = cdURI.getCol();
    collection = true;
    allowsGet = false;

    exists = cdURI.getExists();
  }

  /* (non-Javadoc)
   * @see edu.rpi.cct.webdav.servlet.shared.WebdavNsNode#getOwner()
   */
  @Override
  public AccessPrincipal getOwner() throws WebdavException {
    if (owner == null) {
      if (col == null) {
        return null;
      }

      owner = col.getOwner();
    }

    return owner;
  }

  @Override
  public void init(final boolean content) throws WebdavException {
    if (!content) {
      return;
    }
  }

  /* (non-Javadoc)
   * @see edu.rpi.cct.webdav.servlet.shared.WebdavNsNode#getEtagValue(boolean)
   */
  @Override
  public String getEtagValue(final boolean strong) throws WebdavException {
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
  @Override
  public void setDefaults(final QName methodTag) throws WebdavException {
    if (!CaldavTags.mkcalendar.equals(methodTag)) {
      return;
    }

    CalDAVCollection c = (CalDAVCollection)getCollection(false); // Don't deref

    c.setCalType(CalDAVCollection.calTypeCalendarCollection);
  }

  /* (non-Javadoc)
   * @see org.bedework.caldav.server.CaldavBwNode#getChildren()
   */
  @Override
  public Collection<? extends WdEntity> getChildren() throws WebdavException {
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

        Collection<WdEntity> ch = new ArrayList<WdEntity>();
        ch.addAll(getSysi().getCollections(c));
        ch.addAll(getSysi().getFiles(c));

        return ch;
      }

      /* Otherwise, return the events in this calendar */

      /* Note we use the undereferenced version for the fetch */
      c = (CalDAVCollection)getCollection(false); // don't deref

      if (debug) {
        debugMsg("Get all resources in calendar " + c.getPath());
      }

      return getSysi().getEvents(c, null, null, null);
    } catch (Throwable t) {
      throw new WebdavException(t);
    }
  }

  /**
   * @param fbcal
   * @throws WebdavException
   */
  public void setFreeBusy(final CalDAVEvent fbcal) throws WebdavException {
    try {
      ical = fbcal;

      allowsGet = true;
    } catch (Throwable t) {
      if (debug) {
        error(t);
      }
      throw new WebdavException(t);
    }
  }

  @Override
  public boolean writeContent(final XmlEmit xml,
                              final Writer wtr,
                              final String contentType) throws WebdavException {
    try {
      Collection<CalDAVEvent> evs = new ArrayList<CalDAVEvent>();

      evs.add(ical);

      getSysi().writeCalendar(evs,
                              MethodEmitted.noMethod,
                              xml,
                              wtr,
                              contentType);
    } catch (WebdavException we) {
      throw we;
    } catch (Throwable t) {
      throw new WebdavException(t);
    }

    return true;
  }

  /* (non-Javadoc)
   * @see edu.rpi.cct.webdav.servlet.shared.WebdavNsNode#getContentString()
   */
  @Override
  public String getContentString() throws WebdavException {
    init(true);

    if (ical == null) {
      return null;
    }

    return ical.toString();
  }

  /* (non-Javadoc)
   * @see edu.rpi.cct.webdav.servlet.shared.WebdavNsNode#update()
   */
  @Override
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
  @Override
  public String getContentLang() throws WebdavException {
    return "en";
  }

  /* (non-Javadoc)
   * @see edu.rpi.cct.webdav.servlet.shared.WebdavNsNode#getContentLen()
   */
  @Override
  public long getContentLen() throws WebdavException {
    String s = getContentString();

    if (s == null) {
      return 0;
    }

    return s.getBytes().length;
  }

  /* (non-Javadoc)
   * @see edu.rpi.cct.webdav.servlet.shared.WebdavNsNode#getContentType()
   */
  @Override
  public String getContentType() throws WebdavException {
    if (ical != null) {
      return "text/calendar; charset=UTF-8";
    }

    return null;
  }

  /* (non-Javadoc)
   * @see edu.rpi.cct.webdav.servlet.shared.WebdavNsNode#getCreDate()
   */
  @Override
  public String getCreDate() throws WebdavException {
    return null;
  }

  /* (non-Javadoc)
   * @see edu.rpi.cct.webdav.servlet.shared.WebdavNsNode#getDisplayname()
   */
  @Override
  public String getDisplayname() throws WebdavException {
    if (col == null) {
      return null;
    }

    return col.getName();
  }

  /* (non-Javadoc)
   * @see edu.rpi.cct.webdav.servlet.shared.WebdavNsNode#getLastmodDate()
   */
  @Override
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

  @Override
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
  @Override
  public boolean trailSlash() {
    return true;
  }

  /* ====================================================================
   *                   Property methods
   * ==================================================================== */

  /* (non-Javadoc)
   * @see edu.rpi.cct.webdav.servlet.shared.WebdavNsNode#removeProperty(org.w3c.dom.Element)
   */
  @Override
  public boolean removeProperty(final Element val,
                                final SetPropertyResult spr) throws WebdavException {
    if (super.removeProperty(val, spr)) {
      return true;
    }

    try {
      if (XmlUtil.nodeMatches(val, CaldavTags.calendarTimezone)) {
        col.setTimezone(null);

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
   * @see edu.rpi.cct.webdav.servlet.shared.WebdavNsNode#setProperty(org.w3c.dom.Element)
   */
  @Override
  public boolean setProperty(final Element val,
                             final SetPropertyResult spr) throws WebdavException {
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
            // This is only valid for an (extended) mkcol
            if (!WebdavTags.mkcol.equals(spr.rootElement)) {
              throw new WebdavForbidden();
            }

            CalDAVCollection c = (CalDAVCollection)getCollection(false); // Don't deref

            c.setCalType(CalDAVCollection.calTypeCalendarCollection);
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
        col.setTimezone(getSysi().tzidFromTzdef(XmlUtil.getElementContent(val)));

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
    XmlEmit xml = intf.getXmlEmit();

    try {
      int calType;
      CalDAVCollection c = (CalDAVCollection)getCollection(true); // deref this
      if (c == null) {
        // Probably no access -- fake it up as a collection
        calType = CalDAVCollection.calTypeCollection;
      } else {
        calType = c.getCalType();
      }

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

        if ((c == null) || c.getAffectsFreeBusy()) {
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
        if (c != null) {
          xml.property(tag, c.getTagValue());
        } else {
          xml.property(tag, col.getTagValue());
        }

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

      if (tag.equals(CaldavTags.maxAttendeesPerInstance)) {
        if ((calType != CalDAVCollection.calTypeCalendarCollection) &&
            (calType != CalDAVCollection.calTypeInbox) &&
            (calType != CalDAVCollection.calTypeOutbox)) {
          return false;
        }

        Integer val = getSysi().getSystemProperties().getMaxAttendeesPerInstance();

        if (val == null) {
          return false;
        }

        xml.property(tag, String.valueOf(val));

        return true;
      }

      if (tag.equals(CaldavTags.maxDateTime)) {
        if ((calType != CalDAVCollection.calTypeCalendarCollection) &&
            (calType != CalDAVCollection.calTypeInbox) &&
            (calType != CalDAVCollection.calTypeOutbox)) {
          return false;
        }

        Integer val = getSysi().getSystemProperties().getMaxAttendeesPerInstance();

        if (val == null) {
          return false;
        }

        xml.property(tag, String.valueOf(val));

        return true;
      }

      if (tag.equals(CaldavTags.maxInstances)) {
        if ((calType != CalDAVCollection.calTypeCalendarCollection) &&
            (calType != CalDAVCollection.calTypeInbox) &&
            (calType != CalDAVCollection.calTypeOutbox)) {
          return false;
        }

        Integer val = getSysi().getSystemProperties().getMaxInstances();

        if (val == null) {
          return false;
        }

        xml.property(tag, String.valueOf(val));

        return true;
      }

      if (tag.equals(CaldavTags.maxResourceSize)) {
        if ((calType != CalDAVCollection.calTypeCalendarCollection) &&
            (calType != CalDAVCollection.calTypeInbox) &&
            (calType != CalDAVCollection.calTypeOutbox)) {
          return false;
        }

        Integer val = getSysi().getSystemProperties().getMaxUserEntitySize();

        if (val == null) {
          return false;
        }

        xml.property(tag, String.valueOf(val));

        return true;
      }

      if (tag.equals(CaldavTags.minDateTime)) {
        if ((calType != CalDAVCollection.calTypeCalendarCollection) &&
            (calType != CalDAVCollection.calTypeInbox) &&
            (calType != CalDAVCollection.calTypeOutbox)) {
          return false;
        }

        String val = getSysi().getSystemProperties().getMinDateTime();

        if (val == null) {
          return false;
        }

        xml.property(tag, val);

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
        if ((calType != CalDAVCollection.calTypeCalendarCollection) &&
            (calType != CalDAVCollection.calTypeInbox) &&
            (calType != CalDAVCollection.calTypeOutbox)) {
          return false;
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

  @Override
  public boolean generateXrdValue(final String name,
                                  final WebdavNsIntf intf,
                                  final boolean allProp) throws WebdavException {
    XmlEmit xml = intf.getXmlEmit();

    try {
      int calType;
      CalDAVCollection c = (CalDAVCollection)getCollection(true); // deref this
      if (c == null) {
        // Probably no access -- fake it up as a collection
        calType = CalDAVCollection.calTypeCollection;
      } else {
        calType = c.getCalType();
      }

      if (name.equals(CalWSXrdDefs.collection)) {
        xrdEmptyProperty(xml, name);

        //boolean isCollection = cal.getCalendarCollection();

        if (calType == CalDAVCollection.calTypeInbox) {
          xrdEmptyProperty(xml, CalWSXrdDefs.inbox);
        } else if (calType == CalDAVCollection.calTypeOutbox) {
          xrdEmptyProperty(xml, CalWSXrdDefs.outbox);
        } else if (calType == CalDAVCollection.calTypeCalendarCollection) {
          xrdEmptyProperty(xml, CalWSXrdDefs.calendarCollection);
        }

        return true;
      }

      if (name.equals(CalWSXrdDefs.description)) {
        String s = col.getDescription();

        if (s == null) {
          return true;
        }

        xrdProperty(xml, name, s);

        return true;
      }

      if (name.equals(CalWSXrdDefs.principalHome)) {
        if (!rootNode || intf.getAnonymous()) {
          return true;
        }

        SysIntf si = getSysi();
        CalPrincipalInfo cinfo = si.getCalPrincipalInfo(si.getPrincipal());
        if (cinfo.userHomePath == null) {
          return true;
        }

        xrdProperty(xml, name, getUrlValue(cinfo.userHomePath, true));

        return true;
      }

      if (name.equals(CalWSXrdDefs.maxAttendeesPerInstance)) {
        if (!rootNode) {
          return true;
        }

        Integer val = getSysi().getSystemProperties().getMaxAttendeesPerInstance();

        if (val == null) {
          return true;
        }

        xrdProperty(xml, name, String.valueOf(val));

        return true;
      }

      if (name.equals(CalWSXrdDefs.maxDateTime)) {
        if (!rootNode) {
          return true;
        }

        Integer val = getSysi().getSystemProperties().getMaxAttendeesPerInstance();

        if (val == null) {
          return false;
        }

        xrdProperty(xml, name, String.valueOf(val));

        return true;
      }

      if (name.equals(CalWSXrdDefs.maxInstances)) {
        if (!rootNode) {
          return true;
        }

        Integer val = getSysi().getSystemProperties().getMaxInstances();

        if (val == null) {
          return false;
        }

        xrdProperty(xml, name, String.valueOf(val));

        return true;
      }

      if (name.equals(CalWSXrdDefs.maxResourceSize)) {
        if (!rootNode) {
          return true;
        }

        Integer val = getSysi().getSystemProperties().getMaxUserEntitySize();

        if (val == null) {
          return false;
        }

        xrdProperty(xml, name, String.valueOf(val));

        return true;
      }

      if (name.equals(CalWSXrdDefs.minDateTime)) {
        if (!rootNode) {
          return true;
        }

        String val = getSysi().getSystemProperties().getMinDateTime();

        if (val == null) {
          return false;
        }

        xrdProperty(xml, name, val);

        return true;
      }

      if (name.equals(CalWSXrdDefs.timezone)) {
        String tzid = col.getTimezone();

        if (tzid == null) {
          return false;
        }

        xrdProperty(xml, name, tzid);

        return true;
      }

      // Not known - try higher
      return super.generateXrdValue(name, intf, allProp);
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
  public Collection<PropertyTagEntry> getPropertyNames()throws WebdavException {
    Collection<PropertyTagEntry> res = new ArrayList<PropertyTagEntry>();

    res.addAll(super.getPropertyNames());
    res.addAll(propertyNames.values());

    return res;
  }

  /* (non-Javadoc)
   * @see org.bedework.caldav.server.CaldavBwNode#getXrdNames()
   */
  @Override
  public Collection<PropertyTagXrdEntry> getXrdNames()throws WebdavException {
    Collection<PropertyTagXrdEntry> res = new ArrayList<PropertyTagXrdEntry>();

    res.addAll(super.getXrdNames());
    res.addAll(xrdNames.values());

    return res;
  }

  /* (non-Javadoc)
   * @see org.bedework.caldav.server.CaldavBwNode#getSupportedReports()
   */
  @Override
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

  @Override
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

  private boolean checkCalForSetProp(final SetPropertyResult spr) {
    if (col != null) {
      return true;
    }

    spr.status = HttpServletResponse.SC_NOT_FOUND;
    spr.message = "Not found";
    return false;
  }
}
