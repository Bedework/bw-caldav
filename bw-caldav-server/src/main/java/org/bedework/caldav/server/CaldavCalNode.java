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
import org.bedework.access.Acl.CurrentAccess;
import org.bedework.access.PrivilegeDefs;
import org.bedework.caldav.server.sysinterface.CalPrincipalInfo;
import org.bedework.caldav.server.sysinterface.SysIntf;
import org.bedework.caldav.server.sysinterface.SysIntf.MethodEmitted;
import org.bedework.caldav.util.sharing.InviteType;
import org.bedework.util.calendar.XcalUtil;
import org.bedework.util.misc.Util;
import org.bedework.util.timezones.DateTimeUtil;
import org.bedework.util.xml.XmlEmit;
import org.bedework.util.xml.XmlUtil;
import org.bedework.util.xml.tagdefs.AppleIcalTags;
import org.bedework.util.xml.tagdefs.AppleServerTags;
import org.bedework.util.xml.tagdefs.BedeworkServerTags;
import org.bedework.util.xml.tagdefs.CalWSSoapTags;
import org.bedework.util.xml.tagdefs.CalWSXrdDefs;
import org.bedework.util.xml.tagdefs.CaldavTags;
import org.bedework.util.xml.tagdefs.WebdavTags;
import org.bedework.webdav.servlet.shared.WdEntity;
import org.bedework.webdav.servlet.shared.WebdavBadRequest;
import org.bedework.webdav.servlet.shared.WebdavException;
import org.bedework.webdav.servlet.shared.WebdavForbidden;
import org.bedework.webdav.servlet.shared.WebdavNsIntf;
import org.bedework.webdav.servlet.shared.WebdavNsNode;

import ietf.params.xml.ns.icalendar_2.BaseComponentType;
import ietf.params.xml.ns.icalendar_2.ObjectFactory;
import ietf.params.xml.ns.icalendar_2.VavailabilityType;
import ietf.params.xml.ns.icalendar_2.VeventType;
import ietf.params.xml.ns.icalendar_2.VtodoType;
import org.oasis_open.docs.ws_calendar.ns.soap.CalendarCollectionType;
import org.oasis_open.docs.ws_calendar.ns.soap.ChildCollectionType;
import org.oasis_open.docs.ws_calendar.ns.soap.CollectionType;
import org.oasis_open.docs.ws_calendar.ns.soap.GetPropertiesBasePropertyType;
import org.oasis_open.docs.ws_calendar.ns.soap.InboxType;
import org.oasis_open.docs.ws_calendar.ns.soap.IntegerPropertyType;
import org.oasis_open.docs.ws_calendar.ns.soap.LastModifiedDateTimeType;
import org.oasis_open.docs.ws_calendar.ns.soap.MaxAttendeesPerInstanceType;
import org.oasis_open.docs.ws_calendar.ns.soap.MaxInstancesType;
import org.oasis_open.docs.ws_calendar.ns.soap.MaxResourceSizeType;
import org.oasis_open.docs.ws_calendar.ns.soap.OutboxType;
import org.oasis_open.docs.ws_calendar.ns.soap.PrincipalHomeType;
import org.oasis_open.docs.ws_calendar.ns.soap.ResourceDescriptionType;
import org.oasis_open.docs.ws_calendar.ns.soap.ResourceTimezoneIdType;
import org.oasis_open.docs.ws_calendar.ns.soap.ResourceTypeType;
import org.oasis_open.docs.ws_calendar.ns.soap.StringPropertyType;
import org.oasis_open.docs.ws_calendar.ns.soap.SupportedCalendarComponentSetType;
import org.w3c.dom.Element;

import java.io.Writer;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

import javax.servlet.http.HttpServletResponse;
import javax.xml.bind.JAXBElement;
import javax.xml.namespace.QName;

/** Class to represent a calendar in caldav.
 *
 *   @author Mike Douglass   douglm  rpi.edu
 */
public class CaldavCalNode extends CaldavBwNode {
  private CalDAVEvent ical;

  private AccessPrincipal owner;

  private CurrentAccess currentAccess;

  private final static HashMap<QName, PropertyTagEntry> propertyNames =
    new HashMap<QName, PropertyTagEntry>();

  private final static HashMap<String, PropertyTagXrdEntry> xrdNames =
    new HashMap<String, PropertyTagXrdEntry>();

  private final static HashMap<QName, PropertyTagEntry> calWSSoapNames =
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
    addPropEntry(propertyNames, CaldavTags.timezoneServiceSet);
    addPropEntry(propertyNames, CaldavTags.vpollMaxActive);
    addPropEntry(propertyNames, CaldavTags.vpollMaxItems);
    addPropEntry(propertyNames, CaldavTags.vpollMaxVoters);
    addPropEntry(propertyNames, CaldavTags.vpollSupportedComponentSet);
    addPropEntry(propertyNames, AppleServerTags.allowedSharingModes);
    addPropEntry(propertyNames, AppleServerTags.getctag);
    addPropEntry(propertyNames, AppleServerTags.invite);
    addPropEntry(propertyNames, AppleServerTags.sharedUrl);
    addPropEntry(propertyNames, AppleIcalTags.calendarColor);
    addPropEntry(propertyNames, BedeworkServerTags.aliasUri);
    addPropEntry(propertyNames, BedeworkServerTags.remoteId);
    addPropEntry(propertyNames, BedeworkServerTags.remotePw);
    addPropEntry(propertyNames, BedeworkServerTags.deletionSuppressed);

    /* Default alarms */

    addPropEntry(propertyNames, CaldavTags.defaultAlarmVeventDate);
    addPropEntry(propertyNames, CaldavTags.defaultAlarmVeventDatetime);
    addPropEntry(propertyNames, CaldavTags.defaultAlarmVtodoDate);
    addPropEntry(propertyNames, CaldavTags.defaultAlarmVtodoDatetime);

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
    addXrdEntry(xrdNames, CalWSXrdDefs.supportedCalendarComponentSet, true, false);

    addCalWSSoapName(CalWSSoapTags.childCollection, true);
    addCalWSSoapName(CalWSSoapTags.maxAttendeesPerInstance, true);
    addCalWSSoapName(CalWSSoapTags.maxDateTime, true);
    addCalWSSoapName(CalWSSoapTags.maxInstances, true);
    addCalWSSoapName(CalWSSoapTags.maxResourceSize, true);
    addCalWSSoapName(CalWSSoapTags.minDateTime, true);
    addCalWSSoapName(CalWSSoapTags.principalHome, true);
    addCalWSSoapName(CalWSSoapTags.resourceDescription, true);
    addCalWSSoapName(CalWSSoapTags.resourceType, true);
    addCalWSSoapName(CalWSSoapTags.resourceTimezoneId, true);
    addCalWSSoapName(CalWSSoapTags.supportedCalendarComponentSet, true);
    addCalWSSoapName(CalWSSoapTags.timezoneServer, true);
  }

  /** Place holder for status
   *
   * @param sysi
   * @param status
   * @param uri
   */
  public CaldavCalNode(final SysIntf sysi,
                       final int status,
                       final String uri) {
    super(true, sysi, uri);
    setStatus(status);
  }

  /**
   * @param cdURI
   * @param sysi
   * @throws WebdavException
   */
  public CaldavCalNode(final CaldavURI cdURI,
                       final SysIntf sysi) throws WebdavException {
    super(cdURI, sysi);

    col = cdURI.getCol();
    collection = true;
    allowsGet = false;

    exists = cdURI.getExists();
  }

  /* *
   * @param col
   * @param sysi
   * @throws WebdavException
   * /
  public CaldavCalNode(final CalDAVCollection col,
                       final SysIntf sysi) throws WebdavException {
    super(sysi, col.getParentPath(), true, col.getPath());

    allowsGet = false;

    this.col = col;
    exists = true;
  }*/

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

  @Override
  public String getEtagValue(final boolean strong) throws WebdavException {
    /* We need the etag of the target if this is an alias */
    CalDAVCollection c = (CalDAVCollection)getCollection(true); // deref

    if (c == null) {
      return null;
    }

    String val = c.getEtag();

    if (strong) {
      return val;
    }

    return "W/" + val;
  }

  /* (non-Javadoc)
   * @see org.bedework.caldav.server.CaldavBwNode#getEtokenValue()
   */
  @Override
  public String getEtokenValue() throws WebdavException {
    return concatEtoken(getEtagValue(true), "");
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
   * @return sharing status or null if none.
   * @throws WebdavException
   */
  public String getSharingStatus() throws WebdavException {
    return getCollection(false).getProperty(AppleServerTags.invite);
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
  public String writeContent(final XmlEmit xml,
                             final Writer wtr,
                             final String contentType) throws WebdavException {
    try {
      Collection<CalDAVEvent> evs = new ArrayList<CalDAVEvent>();

      evs.add(ical);

      return getSysi().writeCalendar(evs,
                                     MethodEmitted.noMethod,
                                     xml,
                                     wtr,
                                     contentType);
    } catch (WebdavException we) {
      throw we;
    } catch (Throwable t) {
      throw new WebdavException(t);
    }
  }

  @Override
  public String getContentString(final String contentType) throws WebdavException {
    init(true);

    if (ical == null) {
      return null;
    }

    return ical.toString();
  }

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

  @Override
  public String getContentLang() throws WebdavException {
    return "en";
  }

  @Override
  public long getContentLen() throws WebdavException {
    String s = getContentString(getContentType());

    if (s == null) {
      return 0;
    }

    return s.getBytes().length;
  }

  @Override
  public String getContentType() throws WebdavException {
    if (ical != null) {
      return "text/calendar;charset=utf-8";
    }

    return null;
  }

  @Override
  public String getCreDate() throws WebdavException {
    return null;
  }

  @Override
  public String getDisplayname() throws WebdavException {
    if (col == null) {
      return null;
    }

    return col.getDisplayName();
  }

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

  @Override
  public boolean allowsSyncReport() throws WebdavException {
    return getSysi().allowsSyncReport(col);
  }

  @Override
  public boolean getDeleted() throws WebdavException {
    return col.getDeleted() | ((CalDAVCollection)getCollection(true)).getDeleted();
  }

  @Override
  public String getSyncToken() throws WebdavException {
    return getSysi().getSyncToken(col);
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

  @Override
  public boolean trailSlash() {
    return true;
  }

  /* ====================================================================
   *                   Property methods
   * ==================================================================== */

  @Override
  public boolean removeProperty(final Element val,
                                final SetPropertyResult spr) throws WebdavException {
    if (super.removeProperty(val, spr)) {
      return true;
    }

    try {
      if (XmlUtil.nodeMatches(val, WebdavTags.description)) {
        if (checkCalForSetProp(spr)) {
          col.setDescription(null);
        }
        return true;
      }

      if (XmlUtil.nodeMatches(val, CaldavTags.calendarTimezone)) {
        col.setTimezone(null);

        return true;
      }

      if (XmlUtil.nodeMatches(val, CaldavTags.defaultAlarmVeventDate) ||
          XmlUtil.nodeMatches(val, CaldavTags.defaultAlarmVeventDatetime) ||
          XmlUtil.nodeMatches(val, CaldavTags.defaultAlarmVtodoDate) ||
          XmlUtil.nodeMatches(val, CaldavTags.defaultAlarmVtodoDatetime)) {
        col.setProperty(new QName(val.getNamespaceURI(), val.getLocalName()),
                        null);

        return true;
      }

      return false;
    } catch (WebdavException wde) {
      throw wde;
    } catch (Throwable t) {
      throw new WebdavException(t);
    }
  }

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

      if (XmlUtil.nodeMatches(val, AppleIcalTags.calendarOrder)) {
        if (checkCalForSetProp(spr)) {
          col.setProperty(AppleIcalTags.calendarOrder,
                          XmlUtil.getElementContent(val));
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
            // A change is only valid for mkcalendar or an (extended) mkcol
            final CalDAVCollection c = (CalDAVCollection)getCollection(false); // Don't deref

            if (WebdavTags.mkcol.equals(spr.rootElement) ||
                    CaldavTags.mkcalendar.equals(spr.rootElement)) {
              c.setCalType(CalDAVCollection.calTypeCalendarCollection);
              continue;
            }

            if (c.getCalType() == CalDAVCollection.calTypeCalendarCollection) {
              // No change - ignore
              continue;
            }
            
            throw new WebdavForbidden();
          }

          if (XmlUtil.nodeMatches(pval, AppleServerTags.sharedOwner)) {
            return false; // Not allowed
          }
        }

        return true;
      }

      if (XmlUtil.nodeMatches(val, CaldavTags.supportedCalendarComponentSet)) {
        if (!WebdavTags.mkcol.equals(spr.rootElement) &&
                !CaldavTags.mkcalendar.equals(spr.rootElement)) {
          throw new WebdavForbidden();
        }

        Collection<Element> propVals = XmlUtil.getElements(val);

        List<String> comps = new ArrayList<>();

        for (Element pval: XmlUtil.getElements(val)) {
          if (!XmlUtil.nodeMatches(pval, CaldavTags.comp)) {
            throw new WebdavBadRequest("Only comp allowed");
          }

          comps.add(pval.getAttribute("name"));
        }

        col.setSupportedComponents(comps);

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
          col.setTimezone(getSysi().tzidFromTzdef(
                  XmlUtil.getElementContent(val)));
        } catch (final Throwable t) {
          spr.status = HttpServletResponse.SC_BAD_REQUEST;
          spr.message = t.getLocalizedMessage();
        }

        return true;
      }

      if (XmlUtil.nodeMatches(val, AppleIcalTags.calendarColor)) {
        col.setColor(XmlUtil.getElementContent(val));

        return true;
      }

      if (XmlUtil.nodeMatches(val, CaldavTags.defaultAlarmVeventDate) ||
          XmlUtil.nodeMatches(val, CaldavTags.defaultAlarmVeventDatetime) ||
          XmlUtil.nodeMatches(val, CaldavTags.defaultAlarmVtodoDate) ||
          XmlUtil.nodeMatches(val, CaldavTags.defaultAlarmVtodoDatetime)) {
        String al = XmlUtil.getElementContent(val, false);

        if (al == null) {
          return false;
        }

        if (al.length() > 0) {
          if (!getSysi().validateAlarm(al)) {
            return false;
          }
        }

        col.setProperty(new QName(val.getNamespaceURI(), val.getLocalName()),
                        al);

        return true;
      }

      if (XmlUtil.nodeMatches(val, BedeworkServerTags.aliasUri)) {
        col.setAliasUri(XmlUtil.getElementContent(val));

        return true;
      }

      if (XmlUtil.nodeMatches(val, BedeworkServerTags.remoteId)) {
        col.setRemoteId(XmlUtil.getElementContent(val));

        return true;
      }

      if (XmlUtil.nodeMatches(val, BedeworkServerTags.remotePw)) {
        col.setRemotePw(XmlUtil.getElementContent(val));

        return true;
      }

      if (XmlUtil.nodeMatches(val, BedeworkServerTags.deletionSuppressed)) {
        col.setSynchDeleteSuppressed(
                Boolean.valueOf(XmlUtil.getElementContent(val)));

        return true;
      }

      return false;
    } catch (WebdavException wde) {
      throw wde;
    } catch (Throwable t) {
      throw new WebdavException(t);
    }
  }

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
                                       final boolean allProp) throws WebdavException {
    XmlEmit xml = intf.getXmlEmit();

    try {
      int calType;
      CalDAVCollection c = (CalDAVCollection)getCollection(true); // deref this
      CalDAVCollection cundereffed =
          (CalDAVCollection)getCollection(false); // don't deref this
      if (c == null) {
        // Probably no access -- fake it up as a collection
        calType = CalDAVCollection.calTypeCollection;
        c = cundereffed; // Try to keep going.
      } else {
        calType = c.getCalType();
      }

      if (tag.equals(WebdavTags.owner)) {
        // access 5.1
        /* For shared collections this reflects the owner of the sahred collection
         * NOT the alias.
         */
        xml.openTag(tag);
        String href = intf.makeUserHref(c.getOwner().getPrincipalRef());
        if (!href.endsWith("/")) {
          href += "/";
        }
        xml.property(WebdavTags.href, href);
        xml.closeTag(tag);

        return true;
      }
      
      if (tag.equals(WebdavTags.description)) {
        xml.property(tag, col.getDescription());

        return true;
      }

      if (tag.equals(WebdavTags.resourcetype)) {
        // dav 13.9
        xml.openTag(tag);
        xml.emptyTag(WebdavTags.collection);
        if (debug) {
          debugMsg("generateProp resourcetype for " + col);
        }

        //boolean isCollection = cal.getCalendarCollection();

        if (calType == CalDAVCollection.calTypeInbox) {
          xml.emptyTag(CaldavTags.scheduleInbox);
        } else if (calType == CalDAVCollection.calTypeOutbox) {
          xml.emptyTag(CaldavTags.scheduleOutbox);
        } else if (calType == CalDAVCollection.calTypeCalendarCollection) {
          xml.emptyTag(CaldavTags.calendar);
        } else if (calType == CalDAVCollection.calTypeNotifications) {
          xml.emptyTag(AppleServerTags.notification);
        }

        String s = cundereffed.getProperty(AppleServerTags.shared);
        if ((s != null) && Boolean.valueOf(s)) {
          AccessPrincipal owner;
          if (c == null) {
            // probably lost access to the target
            owner = cundereffed.getOwner();
          } else {
            owner = c.getOwner();
          }
          if (owner.equals(getSysi().getPrincipal())) {
            xml.emptyTag(AppleServerTags.sharedOwner);
          } else {
            xml.emptyTag(AppleServerTags.shared);
          }
        }
        xml.closeTag(tag);

        return true;
      }

      if (tag.equals(AppleServerTags.invite)) {
        final CalDAVCollection imm =
                (CalDAVCollection)getImmediateTargetCollection();
        final InviteType inv = getSysi().getInviteStatus(imm);

        if (inv == null) {
          return false;
        }
        
        inv.toXml(xml);

        return true;
      }

      if (tag.equals(CaldavTags.scheduleCalendarTransp)) {
        xml.openTag(tag);

        if (col.getAffectsFreeBusy()) {
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
          xml.property(tag, c.getEtag());
        } else {
          xml.property(tag, col.getEtag());
        }

        return true;
      }

      if (tag.equals(AppleServerTags.sharedUrl)) {
        if (!cundereffed.isAlias()) {
          return false;
        }

        xml.openTag(tag);
        xml.property(WebdavTags.href, cundereffed.getAliasUri());
        xml.closeTag(tag);

        return true;
      }

      if (tag.equals(AppleServerTags.allowedSharingModes)) {
        // XXX what does publish imply?
        if (/*!col.getCanPublish() && */!col.getCanShare()) {
          return false;
        }

        xml.openTag(tag);
        //addPropEntry(propertyNames, AppleServerTags.canBePublished);
        if (col.getCanShare()) {
          xml.emptyTag(AppleServerTags.canBeShared);
        }
        xml.closeTag(tag);

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

      if (tag.equals(AppleIcalTags.calendarOrder)) {
        // TODO validate this - what if it's null?
        xml.property(tag, col.getProperty(tag));

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

        Integer val = getSysi().getAuthProperties().getMaxAttendeesPerInstance();

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

        Integer val = getSysi().getAuthProperties().getMaxAttendeesPerInstance();

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

        Integer val = getSysi().getAuthProperties().getMaxInstances();

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

        Integer val = getSysi().getAuthProperties().getMaxUserEntitySize();

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

        String val = getSysi().getAuthProperties().getMinDateTime();

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
        @SuppressWarnings("unchecked")
        List<String> comps = c.getSupportedComponents();

        if (Util.isEmpty(comps)) {
          return false;
        }

        xml.openTag(tag);
        for (String s: comps) {
          xml.startTag(CaldavTags.comp);
          xml.attribute("name", s);
          xml.endEmptyTag();
        }
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

        // TODO - need system property to define supported data
        xml.openTag(tag);

        xml.startTag(CaldavTags.calendarData);
        xml.attribute("content-type", "text/calendar");
        xml.attribute("version", "2.0");
        xml.endEmptyTag();
        xml.newline();

        xml.startTag(CaldavTags.calendarData);
        xml.attribute("content-type", "application/calendar+xml");
        xml.attribute("version", "2.0");
        xml.endEmptyTag();
        xml.newline();

        xml.startTag(CaldavTags.calendarData);
        xml.attribute("content-type", "application/calendar+json");
        xml.attribute("version", "2.0");
        xml.endEmptyTag();
        xml.newline();

        xml.closeTag(tag);
        return true;
      }

      if (tag.equals(CaldavTags.timezoneServiceSet)) {
        xml.openTag(tag);

        String href = getSysi().getSystemProperties().getTzServeruri();
        xml.property(WebdavTags.href, href);
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

      if (tag.equals(CaldavTags.defaultAlarmVeventDate) ||
          tag.equals(CaldavTags.defaultAlarmVeventDatetime) ||
          tag.equals(CaldavTags.defaultAlarmVtodoDate) ||
          tag.equals(CaldavTags.defaultAlarmVtodoDatetime)) {
        /* Private to user - look at alias only */
        if (cundereffed == null) {
          return false;
        }

        String val =  cundereffed.getProperty(tag);

        if (val == null) {
          return false;
        }

        xml.cdataProperty(tag, val);

        return true;
      }

      if (tag.equals(CaldavTags.vpollMaxActive)) {
        if ((calType != CalDAVCollection.calTypeCalendarCollection) &&
            (calType != CalDAVCollection.calTypeInbox) &&
            (calType != CalDAVCollection.calTypeOutbox)) {
          return false;
        }

        Integer val = getSysi().getSystemProperties().getVpollMaxActive();

        if (val == null) {
          return false;
        }

        xml.property(tag, String.valueOf(val));

        return true;
      }

      if (tag.equals(CaldavTags.vpollMaxItems)) {
        if ((calType != CalDAVCollection.calTypeCalendarCollection) &&
            (calType != CalDAVCollection.calTypeInbox) &&
            (calType != CalDAVCollection.calTypeOutbox)) {
          return false;
        }

        Integer val = getSysi().getSystemProperties().getVpollMaxItems();

        if (val == null) {
          return false;
        }

        xml.property(tag, String.valueOf(val));

        return true;
      }

      if (tag.equals(CaldavTags.vpollMaxVoters)) {
        if ((calType != CalDAVCollection.calTypeCalendarCollection) &&
            (calType != CalDAVCollection.calTypeInbox) &&
            (calType != CalDAVCollection.calTypeOutbox)) {
          return false;
        }

        Integer val = getSysi().getSystemProperties().getVpollMaxVoters();

        if (val == null) {
          return false;
        }

        xml.property(tag, String.valueOf(val));

        return true;
      }

      if (tag.equals(CaldavTags.vpollSupportedComponentSet)) {
        @SuppressWarnings("unchecked")
        List<String> comps = c.getVpollSupportedComponents();

        if (Util.isEmpty(comps)) {
          return false;
        }

        xml.openTag(tag);
        for (String s: comps) {
          xml.startTag(CaldavTags.comp);
          xml.attribute("name", s);
          xml.endEmptyTag();
        }
        xml.newline();
        xml.closeTag(tag);

        return true;
      }

      if (tag.equals (BedeworkServerTags.aliasUri)) {
        String alias = col.getAliasUri ();
        if(alias == null) {
          return false;
        }

        xml.property(tag, alias);

        return true;
      }

      if (tag.equals (BedeworkServerTags.remoteId)) {
        String id = col.getRemoteId ();
        if (id == null) {
          return false;
        }

        xml.property(tag, id);

        return true;
      }

      if (tag.equals (BedeworkServerTags.remotePw)) {
        final String pw = col.getRemotePw ();
        if (pw == null) {
          return false;
        }

        xml.property(tag, pw);

        return true;
      }

      if (tag.equals (BedeworkServerTags.deletionSuppressed)) {
        xml.property(tag,
                     String.valueOf(col.getSynchDeleteSuppressed()));

        return true;
      }

      // Not known - try higher
      return super.generatePropertyValue(tag, intf, allProp);
    } catch (final WebdavException wde) {
      throw wde;
    } catch (final Throwable t) {
      throw new WebdavException(t);
    }
  }

  @Override
  public boolean generateCalWsProperty(final List<GetPropertiesBasePropertyType> props,
                                       final QName tag,
                                       final WebdavNsIntf intf,
                                       final boolean allProp) throws WebdavException {
    try {
      /*

    addCalWSSoapName(CalWSSoapTags.timezoneServer, true);
       */

      if (tag.equals(CalWSSoapTags.childCollection)) {
        for (WebdavNsNode child: intf.getChildren(this)) {
          CaldavBwNode cn = (CaldavBwNode)child;

          ChildCollectionType cc = new ChildCollectionType();

          cc.setHref(cn.getUrlValue());

          List<Object> rtypes = cc.getCalendarCollectionOrCollection();

          if (!cn.isCollection()) {
            continue;
          }

          rtypes.add(new CollectionType());

          if (cn.isCalendarCollection()) {
            rtypes.add(new CalendarCollectionType());
          }

          props.add(cc);
        }

        return true;
      }

      if (tag.equals(CalWSSoapTags.lastModifiedDateTime)) {
        String val = col.getLastmod();
        if (val == null) {
          return true;
        }

        LastModifiedDateTimeType lmdt = new LastModifiedDateTimeType();
        lmdt.setDateTime(XcalUtil.fromDtval(val));
        props.add(lmdt);
        return true;
      }

      if (tag.equals(CalWSSoapTags.maxAttendeesPerInstance)) {
        if (!rootNode) {
          return true;
        }

        Integer val = getSysi().getAuthProperties().getMaxAttendeesPerInstance();

        if (val != null) {
          props.add(intProp(new MaxAttendeesPerInstanceType(),
                            val));
        }

        return true;
      }

      if (tag.equals(CalWSSoapTags.maxDateTime)) {
        return true;
      }

      /*
      if (name.equals(CalWSXrdDefs.maxDateTime)) {
        if (!rootNode) {
          return true;
        }

        Integer val = getSysi().getSystemProperties().getMaxAttendeesPerInstance();

        if (val == null) {
          return false;
        }

        props.add(prop);

        return true;
      }*/

      if (tag.equals(CalWSSoapTags.maxInstances)) {
        if (!rootNode) {
          return true;
        }

        Integer val = getSysi().getAuthProperties().getMaxInstances();

        if (val != null) {
          props.add(intProp(new MaxInstancesType(),
                            val));
        }

        return true;
      }

      if (tag.equals(CalWSSoapTags.maxResourceSize)) {
        if (!rootNode) {
          return true;
        }

        Integer val = getSysi().getAuthProperties().getMaxUserEntitySize();

        if (val != null) {
          props.add(intProp(new MaxResourceSizeType(),
                            val));
        }

        return true;
      }

      if (tag.equals(CalWSSoapTags.minDateTime)) {
        return true;
      }

      /*
      if (name.equals(CalWSXrdDefs.minDateTime)) {
        if (!rootNode) {
          return true;
        }

        String val = getSysi().getSystemProperties().getMinDateTime();

        if (val == null) {
          return false;
        }

        props.add(xrdProperty(name, val));

        return true;
      }
      */

      if (tag.equals(CalWSSoapTags.principalHome)) {
        if (!rootNode || intf.getAnonymous()) {
          return true;
        }

        SysIntf si = getSysi();
        CalPrincipalInfo cinfo = si.getCalPrincipalInfo(si.getPrincipal());
        if (cinfo.userHomePath != null) {
          props.add(strProp(new PrincipalHomeType(), cinfo.userHomePath));
        }

        return true;
      }

      if (tag.equals(CalWSSoapTags.resourceDescription)) {
        String s = col.getDescription();

        if (s != null) {
          props.add(strProp(new ResourceDescriptionType(), s));
        }

        return true;
      }

      if (tag.equals(CalWSSoapTags.resourceType)) {
        ResourceTypeType rt = new ResourceTypeType();

        int calType;
        CalDAVCollection c = (CalDAVCollection)getCollection(true); // deref this
        if (c == null) {
          // Probably no access -- fake it up as a collection
          calType = CalDAVCollection.calTypeCollection;
        } else {
          calType = c.getCalType();
        }

        List<Object> rtypes = rt.getCalendarCollectionOrCollectionOrInbox();

        rtypes.add(new CollectionType());

        if (calType == CalDAVCollection.calTypeInbox) {
          rtypes.add(new InboxType());
        } else if (calType == CalDAVCollection.calTypeOutbox) {
          rtypes.add(new OutboxType());
        } else if (calType == CalDAVCollection.calTypeCalendarCollection) {
          rtypes.add(new CalendarCollectionType());
        }

        props.add(rt);

        return true;
      }

      if (tag.equals(CalWSSoapTags.resourceTimezoneId)) {
        String tzid = col.getTimezone();

        if (tzid != null) {
          props.add(strProp(new ResourceTimezoneIdType(), tzid));
        }

        return true;
      }

      if (tag.equals(CalWSSoapTags.supportedCalendarComponentSet)) {
        SupportedCalendarComponentSetType sccs = new SupportedCalendarComponentSetType();

        CalDAVCollection c = (CalDAVCollection)getCollection(true); // deref this
        @SuppressWarnings("unchecked")
        List<String> comps = c.getSupportedComponents();

        if (Util.isEmpty(comps)) {
          return false;
        }

        ObjectFactory of = new ObjectFactory();

        for (String s: comps) {
          JAXBElement<? extends BaseComponentType> el = null;
          if (s.equals("VEVENT")) {
            el = of.createVevent(new VeventType());
          } else if (s.equals("VTODO")) {
            el = of.createVtodo(new VtodoType());
          } else if (s.equals("VAVAILABILITY")) {
            el = of.createVavailability(new VavailabilityType());
          };

          if (el != null) {
            sccs.getBaseComponent().add(el);
          }
        }

        props.add(sccs);

        return true;
      }

      // Not known - try higher
      return super.generateCalWsProperty(props, tag, intf, allProp);
    } catch (WebdavException wde) {
      throw wde;
    } catch (Throwable t) {
      throw new WebdavException(t);
    }
  }

  private GetPropertiesBasePropertyType intProp(final IntegerPropertyType prop,
                                                final Integer val) {
    prop.setInteger(BigInteger.valueOf(val.longValue()));
    return prop;
  }

  private GetPropertiesBasePropertyType strProp(final StringPropertyType prop,
                                                final String val) {
    prop.setString(val);
    return prop;
  }

  @Override
  public boolean generateXrdProperties(final List<Object> props,
                                       final String name,
                                       final WebdavNsIntf intf,
                                       final boolean allProp) throws WebdavException {
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
        props.add(xrdEmptyProperty(name));

        //boolean isCollection = cal.getCalendarCollection();

        if (calType == CalDAVCollection.calTypeInbox) {
          props.add(xrdEmptyProperty(CalWSXrdDefs.inbox));
        } else if (calType == CalDAVCollection.calTypeOutbox) {
          props.add(xrdEmptyProperty(CalWSXrdDefs.outbox));
        } else if (calType == CalDAVCollection.calTypeCalendarCollection) {
          props.add(xrdEmptyProperty(CalWSXrdDefs.calendarCollection));
        }

        return true;
      }

      if (name.equals(CalWSXrdDefs.description)) {
        String s = col.getDescription();

        if (s == null) {
          return true;
        }

        props.add(xrdProperty(name, s));

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

        props.add(xrdProperty(name,
                              getUrlValue(cinfo.userHomePath, true)));

        return true;
      }

      if (name.equals(CalWSXrdDefs.maxAttendeesPerInstance)) {
        if (!rootNode) {
          return true;
        }

        Integer val = getSysi().getAuthProperties().getMaxAttendeesPerInstance();

        if (val == null) {
          return true;
        }

        props.add(xrdProperty(name,
                                                            String.valueOf(val)));

        return true;
      }

      if (name.equals(CalWSXrdDefs.maxDateTime)) {
        if (!rootNode) {
          return true;
        }

        Integer val = getSysi().getAuthProperties().getMaxAttendeesPerInstance();

        if (val == null) {
          return false;
        }

        props.add(xrdProperty(name,
                                                            String.valueOf(val)));

        return true;
      }

      if (name.equals(CalWSXrdDefs.maxInstances)) {
        if (!rootNode) {
          return true;
        }

        Integer val = getSysi().getAuthProperties().getMaxInstances();

        if (val == null) {
          return false;
        }

        props.add(xrdProperty(name,
                                                            String.valueOf(val)));

        return true;
      }

      if (name.equals(CalWSXrdDefs.maxResourceSize)) {
        if (!rootNode) {
          return true;
        }

        Integer val = getSysi().getAuthProperties().getMaxUserEntitySize();

        if (val == null) {
          return false;
        }

        props.add(xrdProperty(name,
                                                            String.valueOf(val)));

        return true;
      }

      if (name.equals(CalWSXrdDefs.minDateTime)) {
        if (!rootNode) {
          return true;
        }

        String val = getSysi().getAuthProperties().getMinDateTime();

        if (val == null) {
          return false;
        }

        props.add(xrdProperty(name, val));

        return true;
      }

      if (name.equals(CalWSXrdDefs.timezone)) {
        String tzid = col.getTimezone();

        if (tzid == null) {
          return false;
        }

        props.add(xrdProperty(name, tzid));

        return true;
      }

      if (name.equals(CalWSXrdDefs.supportedCalendarComponentSet)) {
        SupportedCalendarComponentSetType sccs = new SupportedCalendarComponentSetType();

        @SuppressWarnings("unchecked")
        List<String> comps = c.getSupportedComponents();

        if (Util.isEmpty(comps)) {
          return false;
        }

        ObjectFactory of = new ObjectFactory();

        for (String s: comps) {
          JAXBElement<? extends BaseComponentType> el = null;
          if (s.equals("VEVENT")) {
            el = of.createVevent(new VeventType());
          } else if (s.equals("VTODO")) {
            el = of.createVtodo(new VtodoType());
          } else if (s.equals("VAVAILABILITY")) {
            el = of.createVavailability(new VavailabilityType());
          };

          if (el != null) {
            sccs.getBaseComponent().add(el);
          }
        }

        JAXBElement<SupportedCalendarComponentSetType> el =
              new JAXBElement<SupportedCalendarComponentSetType>(CalWSSoapTags.supportedCalendarComponentSet,
                                         SupportedCalendarComponentSetType.class,
                                         sccs);
        props.add(el);

        return true;
      }

      // Not known - try higher
      return super.generateXrdProperties(props, name, intf, allProp);
    } catch (WebdavException wde) {
      throw wde;
    } catch (Throwable t) {
      throw new WebdavException(t);
    }
  }

  @Override
  public Collection<PropertyTagEntry> getPropertyNames()throws WebdavException {
    Collection<PropertyTagEntry> res = new ArrayList<PropertyTagEntry>();

    res.addAll(super.getPropertyNames());
    res.addAll(propertyNames.values());

    return res;
  }

  @Override
  public Collection<PropertyTagEntry> getCalWSSoapNames() throws WebdavException {
    Collection<PropertyTagEntry> res = new ArrayList<PropertyTagEntry>();

    res.addAll(super.getCalWSSoapNames());
    res.addAll(calWSSoapNames.values());

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
