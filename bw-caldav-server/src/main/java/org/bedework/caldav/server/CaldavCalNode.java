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
import org.bedework.caldav.server.sysinterface.CalPrincipalInfo;
import org.bedework.caldav.server.sysinterface.SysIntf;
import org.bedework.caldav.server.sysinterface.SysIntf.MethodEmitted;
import org.bedework.caldav.util.filter.FilterBase;
import org.bedework.caldav.util.sharing.InviteType;
import org.bedework.util.calendar.XcalUtil;
import org.bedework.base.ToString;
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
import java.util.function.Supplier;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.xml.bind.JAXBElement;
import javax.xml.namespace.QName;

/** Class to represent a calendar in caldav.
 *
 *   @author Mike Douglass   douglm  rpi.edu
 */
public class CaldavCalNode extends CaldavBwNode {
  private CalDAVEvent<?> ical;

  private AccessPrincipal owner;

  private CurrentAccess currentAccess;

  private final static HashMap<QName, PropertyTagEntry> propertyNames =
          new HashMap<>();

  private final static HashMap<String, PropertyTagXrdEntry> xrdNames =
          new HashMap<>();

  private final static HashMap<QName, PropertyTagEntry> calWSSoapNames =
          new HashMap<>();

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
    addPropEntry(propertyNames, BedeworkServerTags.refreshRate);
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
   * @param sysi system interface
   * @param status from exception
   * @param uri of resource
   */
  public CaldavCalNode(final SysIntf sysi,
                       final int status,
                       final String uri) {
    super(true, sysi, uri);
    setStatus(status);
  }

  /**
   * @param cdURI referencing resource
   * @param sysi system interface
   */
  public CaldavCalNode(final CaldavURI cdURI,
                       final SysIntf sysi) {
    super(cdURI, sysi);

    col = cdURI.getCol();
    collection = true;
    allowsGet = false;

    exists = cdURI.getExists();
  }

  /* *
   * @param col
   * @param sysi
   * /
  public CaldavCalNode(final CalDAVCollection col,
                       final SysIntf sysi) {
    super(sysi, col.getParentPath(), true, col.getPath());

    allowsGet = false;

    this.col = col;
    exists = true;
  }*/

  @Override
  public AccessPrincipal getOwner() {
    if (owner == null) {
      if (col == null) {
        return null;
      }

      owner = col.getOwner();
    }

    return owner;
  }

  @Override
  public void init(final boolean content) {
    //if (!content) {
    //  return;
    //}
  }

  @Override
  public String getEtagValue(final boolean strong) {
    /* We need the etag of the target if this is an alias */
    final CalDAVCollection<?> c =
            (CalDAVCollection<?>)getCollection(true); // deref

    if (c == null) {
      return null;
    }

    final String val = c.getEtag();

    if (strong) {
      return val;
    }

    return "W/" + val;
  }

  @Override
  public String getEtokenValue() {
    return concatEtoken(getEtagValue(true), "");
  }

  /**
   * @return true if scheduling allowed
   */
  public boolean getSchedulingAllowed() {
    /* It's the alias target that matters */
    final CalDAVCollection<?> c =
            (CalDAVCollection<?>)getCollection(true); // deref

    if (c == null) {
      return false;
    }

    final int type = c.getCalType();
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
   */
  public String getSharingStatus() {
    return getCollection(false).getProperty(AppleServerTags.invite);
  }

  /**
   * @param methodTag - acts as a flag for the method type
   */
  @Override
  public void setDefaults(final QName methodTag) {
    if (!CaldavTags.mkcalendar.equals(methodTag)) {
      return;
    }

    final CalDAVCollection<?> c =
            (CalDAVCollection<?>)getCollection(false); // Don't deref

    c.setCalType(CalDAVCollection.calTypeCalendarCollection);
  }

  @Override
  public Collection<? extends WdEntity<?>> getChildren(
          final Supplier<Object> filterGetter) {
    /* For the moment we're going to do this the inefficient way.
       We really need to have calendar defs that can be expressed as a search
       allowing us to retrieve all the ids of objects within a calendar.
       */

    try {
      CalDAVCollection<?> c =
              (CalDAVCollection<?>)getCollection(true); // deref

      if (c == null) { // no access?
        return null;
      }

      if (!c.entitiesAllowed()) {
        if (debug()) {
          debug("POSSIBLE SEARCH: getChildren for cal " + c.getPath());
        }

        final Collection<WdEntity<?>> ch = new ArrayList<>();
        ch.addAll(getSysi().getCollections(c));
        ch.addAll(getSysi().getFiles(c));

        return ch;
      }

      /* Otherwise, return the events in this calendar */

      /* Note we use the undereferenced version for the fetch */
      c = (CalDAVCollection<?>)getCollection(false); // don't deref

      if (debug()) {
        debug("Get all resources in calendar " + c.getPath());
      }

      final FilterBase filter;

      if (filterGetter == null) {
        filter = null;
      } else {
        filter = (FilterBase)filterGetter.get();
      }

      return getSysi().getEvents(c,
                                 filter,
                                 null, null);
    } catch (final Throwable t) {
      throw new WebdavException(t);
    }
  }

  /**
   * @param fbcal freebusy resource
   */
  public void setFreeBusy(final CalDAVEvent<?> fbcal) {
    try {
      ical = fbcal;

      allowsGet = true;
    } catch (final Throwable t) {
      if (debug()) {
        error(t);
      }
      throw new WebdavException(t);
    }
  }

  @Override
  public String writeContent(final XmlEmit xml,
                             final Writer wtr,
                             final String contentType) {
    try {
      final Collection<CalDAVEvent<?>> evs = new ArrayList<>();

      evs.add(ical);

      return getSysi().writeCalendar(evs,
                                     MethodEmitted.noMethod,
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
    init(true);

    if (ical == null) {
      return null;
    }

    return ical.toString();
  }

  @Override
  public void update() {
    // ALIAS probably not unaliasing here
    if (col != null) {
      getSysi().updateCollection(col);
    }
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
    final String s = getContentString(getContentType());

    if (s == null) {
      return 0;
    }

    return s.getBytes().length;
  }

  @Override
  public String getContentType() {
    if (ical != null) {
      return "text/calendar;charset=utf-8";
    }

    return null;
  }

  @Override
  public String getCreDate() {
    return null;
  }

  @Override
  public String getDisplayname()  {
    if (col == null) {
      return null;
    }

    return col.getDisplayName();
  }

  @Override
  public String getLastmodDate() {
    init(false);
    if (col == null) {
      return null;
    }

    try {
      return DateTimeUtil.fromISODateTimeUTCtoRfc822(col.getLastmod());
    } catch (final Throwable t) {
      throw new WebdavException(t);
    }
  }

  @Override
  public boolean allowsSyncReport() {
    return getSysi().allowsSyncReport(col);
  }

  @Override
  public boolean getDeleted() {
    return col.getDeleted() | ((CalDAVCollection<?>)getCollection(true)).getDeleted();
  }

  @Override
  public String getSyncToken() {
    return getSysi().getSyncToken(col);
  }

  /* ==============================================================
   *                   Abstract methods
   * ============================================================== */

  @Override
  public CurrentAccess getCurrentAccess() {
    if (currentAccess != null) {
      return currentAccess;
    }

    final CalDAVCollection<?> c =
            (CalDAVCollection<?>)getCollection(true); // We want access of underlying object?

    if (c == null) {
      return null;
    }

    try {
      currentAccess = getSysi().checkAccess(c,
                                            PrivilegeDefs.privAny,
                                            true);
    } catch (final Throwable t) {
      throw new WebdavException(t);
    }

    return currentAccess;
  }

  @Override
  public boolean trailSlash() {
    return true;
  }

  /* ==============================================================
   *                   Property methods
   * ============================================================== */

  @Override
  public boolean removeProperty(final Element val,
                                final SetPropertyResult spr) {
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
    } catch (final Throwable t) {
      throw new WebdavException(t);
    }
  }

  @Override
  public boolean setProperty(final Element val,
                             final SetPropertyResult spr) {
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
        for (final var pval: XmlUtil.getElements(val)) {
          if (XmlUtil.nodeMatches(pval, WebdavTags.collection)) {
            // Fine
            continue;
          }

          if (XmlUtil.nodeMatches(pval, CaldavTags.calendar)) {
            // A change is only valid for mkcalendar or an (extended) mkcol
            final CalDAVCollection<?> c =
                    (CalDAVCollection<?>)getCollection(false); // Don't deref

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

        final List<String> comps = new ArrayList<>();

        for (final Element pval: XmlUtil.getElements(val)) {
          if (!XmlUtil.nodeMatches(pval, CaldavTags.comp)) {
            throw new WebdavBadRequest("Only comp allowed");
          }

          comps.add(pval.getAttribute("name"));
        }

        col.setSupportedComponents(comps);

        return true;
      }

      if (XmlUtil.nodeMatches(val, CaldavTags.scheduleCalendarTransp)) {
        final Element cval = XmlUtil.getOnlyElement(val);

        if (XmlUtil.nodeMatches(cval, CaldavTags.opaque)) {
          col.setAffectsFreeBusy(true);
        } else if (XmlUtil.nodeMatches(cval, CaldavTags.transparent)) {
          col.setAffectsFreeBusy(false);
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
        final String al = XmlUtil.getElementContent(val, false);

        if (al == null) {
          return false;
        }

        if (!al.isEmpty()) {
          if (!getSysi().validateAlarm(al)) {
            return false;
          }
        }

        col.setProperty(new QName(val.getNamespaceURI(),
                                  val.getLocalName()),
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

      return false;
    } catch (final WebdavException wde) {
      throw wde;
    } catch (final Throwable t) {
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
                                       final boolean allProp) {
    final XmlEmit xml = intf.getXmlEmit();

    try {
      final int calType;
      CalDAVCollection<?> c =
              (CalDAVCollection<?>)getCollection(true); // deref this
      final CalDAVCollection<?> cundereffed =
          (CalDAVCollection<?>)getCollection(false); // don't deref this
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
        if (debug()) {
          debug("generateProp resourcetype for " + col);
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

        final String s = cundereffed.getProperty(AppleServerTags.shared);
        if (Boolean.parseBoolean(s)) {
          final AccessPrincipal owner;
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
        final CalDAVCollection<?> imm =
                (CalDAVCollection<?>)getImmediateTargetCollection();
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

        final CalPrincipalInfo cinfo =
                getSysi().getCalPrincipalInfo(getOwner());
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
       final String val = col.getColor();

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

        final Collection<String> hrefs = getSysi().getFreebusySet();

        for (final String href: hrefs) {
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

        final Integer val = getSysi().getAuthProperties()
                                     .getMaxAttendeesPerInstance();

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

        final Integer val = getSysi().getAuthProperties()
                                     .getMaxAttendeesPerInstance();

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

        final Integer val = getSysi().getAuthProperties()
                                     .getMaxInstances();

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

        final Integer val = getSysi().getAuthProperties()
                                     .getMaxUserEntitySize();

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

        final String val = getSysi().getAuthProperties()
                                    .getMinDateTime();

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
        final List<String> comps = c.getSupportedComponents();

        if (Util.isEmpty(comps)) {
          return false;
        }

        xml.openTag(tag);
        for (final String s: comps) {
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

        xml.startTag(CaldavTags.calendarData);
        xml.attribute("content-type", "application/jscalendar+json");
        xml.endEmptyTag();
        xml.newline();

        xml.closeTag(tag);
        return true;
      }

      if (tag.equals(CaldavTags.timezoneServiceSet)) {
        xml.openTag(tag);

        final String href = getSysi().getSystemProperties()
                                     .getTzServeruri();
        xml.property(WebdavTags.href, href);
        xml.newline();
        xml.closeTag(tag);
        return true;
      }

      if (tag.equals(CaldavTags.calendarTimezone)) {
        final String tzid = col.getTimezone();

        if (tzid == null) {
          return false;
        }

        final String val = getSysi().toStringTzCalendar(tzid);

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

        final String val =  cundereffed.getProperty(tag);

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

        final Integer val = getSysi().getSystemProperties().getVpollMaxActive();

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

        final Integer val = getSysi().getSystemProperties()
                                     .getVpollMaxItems();

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

        final Integer val = getSysi().getSystemProperties()
                                     .getVpollMaxVoters();

        if (val == null) {
          return false;
        }

        xml.property(tag, String.valueOf(val));

        return true;
      }

      if (tag.equals(CaldavTags.vpollSupportedComponentSet)) {
        final List<String> comps = c.getVpollSupportedComponents();

        if (Util.isEmpty(comps)) {
          return false;
        }

        xml.openTag(tag);
        for (final String s: comps) {
          xml.startTag(CaldavTags.comp);
          xml.attribute("name", s);
          xml.endEmptyTag();
        }
        xml.newline();
        xml.closeTag(tag);

        return true;
      }

      if(tag.equals (BedeworkServerTags.aliasUri)) {
        final String alias = col.getAliasUri ();
        if(alias == null) {
          return false;
        }

        xml.property (tag, alias);

        return true;
      }

      if(tag.equals (BedeworkServerTags.remoteId)) {
        final String id = col.getRemoteId ();
        if(id == null) {
          return false;
        }

        xml.property (tag, id);

        return true;
      }

      if(tag.equals (BedeworkServerTags.remotePw)) {
        final String pw = col.getRemotePw ();
        if(pw == null) {
          return false;
        }

        xml.property (tag, pw);

        return true;
      }

      if(tag.equals (BedeworkServerTags.deletionSuppressed)) {
        xml.property (tag, String.valueOf(col.getSynchDeleteSuppressed()));

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
  public boolean generateCalWsProperty(
          final List<GetPropertiesBasePropertyType> props,
          final QName tag,
          final WebdavNsIntf intf,
          final boolean allProp) {
    try {
      /*

    addCalWSSoapName(CalWSSoapTags.timezoneServer, true);
       */

      if (tag.equals(CalWSSoapTags.childCollection)) {
        for (final WebdavNsNode child: intf.getChildren(this, null)) {
          final CaldavBwNode cn = (CaldavBwNode)child;

          final ChildCollectionType cc = new ChildCollectionType();

          cc.setHref(cn.getUrlValue());

          final List<Object> rtypes =
                  cc.getCalendarCollectionOrCollection();

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
        final String val = col.getLastmod();
        if (val == null) {
          return true;
        }

        final LastModifiedDateTimeType lmdt =
                new LastModifiedDateTimeType();
        lmdt.setDateTime(XcalUtil.fromDtval(val));
        props.add(lmdt);
        return true;
      }

      if (tag.equals(CalWSSoapTags.maxAttendeesPerInstance)) {
        if (!rootNode) {
          return true;
        }

        final Integer val = getSysi().getAuthProperties()
                                     .getMaxAttendeesPerInstance();

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

        final Integer val = getSysi().getAuthProperties()
                                     .getMaxInstances();

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

        final Integer val = getSysi().getAuthProperties()
                                     .getMaxUserEntitySize();

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

        final SysIntf si = getSysi();
        final CalPrincipalInfo cinfo = si.getCalPrincipalInfo(si.getPrincipal());
        if (cinfo.userHomePath != null) {
          props.add(strProp(new PrincipalHomeType(), cinfo.userHomePath));
        }

        return true;
      }

      if (tag.equals(CalWSSoapTags.resourceDescription)) {
        final String s = col.getDescription();

        if (s != null) {
          props.add(strProp(new ResourceDescriptionType(), s));
        }

        return true;
      }

      if (tag.equals(CalWSSoapTags.resourceType)) {
        final ResourceTypeType rt = new ResourceTypeType();

        final int calType;
        final CalDAVCollection<?> c =
                (CalDAVCollection<?>)getCollection(true); // deref this
        if (c == null) {
          // Probably no access -- fake it up as a collection
          calType = CalDAVCollection.calTypeCollection;
        } else {
          calType = c.getCalType();
        }

        final List<Object> rtypes =
                rt.getCalendarCollectionOrCollectionOrInbox();

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
        final String tzid = col.getTimezone();

        if (tzid != null) {
          props.add(strProp(new ResourceTimezoneIdType(), tzid));
        }

        return true;
      }

      if (tag.equals(CalWSSoapTags.supportedCalendarComponentSet)) {
        final SupportedCalendarComponentSetType sccs =
                new SupportedCalendarComponentSetType();

        final CalDAVCollection<?> c =
                (CalDAVCollection<?>)getCollection(true); // deref this
        final List<String> comps = c.getSupportedComponents();

        if (Util.isEmpty(comps)) {
          return false;
        }

        final ObjectFactory of = new ObjectFactory();

        for (final String s: comps) {
          final JAXBElement<? extends BaseComponentType> el =
                  switch (s) {
                    case "VEVENT" -> of.createVevent(new VeventType());
                    case "VTODO" -> of.createVtodo(new VtodoType());
                    case "VAVAILABILITY" ->
                            of.createVavailability(new VavailabilityType());
                    default -> null;
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
    } catch (final WebdavException wde) {
      throw wde;
    } catch (final Throwable t) {
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
                                       final boolean allProp) {
    try {
      final int calType;
      final CalDAVCollection<?> c =
              (CalDAVCollection<?>)getCollection(true); // deref this
      if (c == null) {
        // Probably no access -- fake it up as a collection
        calType = CalDAVCollection.calTypeCollection;
      } else {
        calType = c.getCalType();
      }

      switch (name) {
        case CalWSXrdDefs.collection -> {
          props.add(xrdEmptyProperty(name));

          //boolean isCollection = cal.getCalendarCollection();

          if (calType == CalDAVCollection.calTypeInbox) {
            props.add(xrdEmptyProperty(CalWSXrdDefs.inbox));
          } else if (calType == CalDAVCollection.calTypeOutbox) {
            props.add(xrdEmptyProperty(CalWSXrdDefs.outbox));
          } else if (calType == CalDAVCollection.calTypeCalendarCollection) {
            props.add(xrdEmptyProperty(
                    CalWSXrdDefs.calendarCollection));
          }

          return true;
        }
        case CalWSXrdDefs.description -> {
          final String s = col.getDescription();

          if (s == null) {
            return true;
          }

          props.add(xrdProperty(name, s));

          return true;
        }
        case CalWSXrdDefs.principalHome -> {
          if (!rootNode || intf.getAnonymous()) {
            return true;
          }

          final SysIntf si = getSysi();
          final CalPrincipalInfo cinfo = si.getCalPrincipalInfo(
                  si.getPrincipal());
          if (cinfo.userHomePath == null) {
            return true;
          }

          props.add(xrdProperty(name,
                                getUrlValue(cinfo.userHomePath,
                                            true)));

          return true;
        }
        case CalWSXrdDefs.maxAttendeesPerInstance -> {
          if (!rootNode) {
            return true;
          }

          final Integer val = getSysi().getAuthProperties()
                                       .getMaxAttendeesPerInstance();

          if (val == null) {
            return true;
          }

          props.add(xrdProperty(name,
                                String.valueOf(val)));

          return true;
        }
        case CalWSXrdDefs.maxDateTime -> {
          if (!rootNode) {
            return true;
          }

          final Integer val = getSysi().getAuthProperties()
                                       .getMaxAttendeesPerInstance();

          if (val == null) {
            return false;
          }

          props.add(xrdProperty(name,
                                String.valueOf(val)));

          return true;
        }
        case CalWSXrdDefs.maxInstances -> {
          if (!rootNode) {
            return true;
          }

          final Integer val = getSysi().getAuthProperties()
                                       .getMaxInstances();

          if (val == null) {
            return false;
          }

          props.add(xrdProperty(name,
                                String.valueOf(val)));

          return true;
        }
        case CalWSXrdDefs.maxResourceSize -> {
          if (!rootNode) {
            return true;
          }

          final Integer val = getSysi().getAuthProperties()
                                       .getMaxUserEntitySize();

          if (val == null) {
            return false;
          }

          props.add(xrdProperty(name,
                                String.valueOf(val)));

          return true;
        }
        case CalWSXrdDefs.minDateTime -> {
          if (!rootNode) {
            return true;
          }

          final String val = getSysi().getAuthProperties()
                                      .getMinDateTime();

          if (val == null) {
            return false;
          }

          props.add(xrdProperty(name, val));

          return true;
        }
        case CalWSXrdDefs.timezone -> {
          final String tzid = col.getTimezone();

          if (tzid == null) {
            return false;
          }

          props.add(xrdProperty(name, tzid));

          return true;
        }
        case CalWSXrdDefs.supportedCalendarComponentSet -> {
          final SupportedCalendarComponentSetType sccs = new SupportedCalendarComponentSetType();

          final List<String> comps = c.getSupportedComponents();

          if (Util.isEmpty(comps)) {
            return false;
          }

          final ObjectFactory of = new ObjectFactory();

          for (final String s: comps) {
            final JAXBElement<? extends BaseComponentType> el = switch (s) {
              case "VEVENT" -> of.createVevent(new VeventType());
              case "VTODO" -> of.createVtodo(new VtodoType());
              case "VAVAILABILITY" ->
                      of.createVavailability(new VavailabilityType());
              default -> null;
            };

            if (el != null) {
              sccs.getBaseComponent().add(el);
            }
          }

          final JAXBElement<SupportedCalendarComponentSetType> el =
                  new JAXBElement<>(
                          CalWSSoapTags.supportedCalendarComponentSet,
                          SupportedCalendarComponentSetType.class,
                          sccs);
          props.add(el);

          return true;
        }
      }

      // Not known - try higher
      return super.generateXrdProperties(props, name, intf, allProp);
    } catch (final WebdavException wde) {
      throw wde;
    } catch (final Throwable t) {
      throw new WebdavException(t);
    }
  }

  @Override
  public Collection<PropertyTagEntry> getPropertyNames()throws WebdavException {
    final Collection<PropertyTagEntry> res = new ArrayList<>();

    res.addAll(super.getPropertyNames());
    res.addAll(propertyNames.values());

    return res;
  }

  @Override
  public Collection<PropertyTagEntry> getCalWSSoapNames() {
    final Collection<PropertyTagEntry> res = new ArrayList<>();

    res.addAll(super.getCalWSSoapNames());
    res.addAll(calWSSoapNames.values());

    return res;
  }

  @Override
  public Collection<PropertyTagXrdEntry> getXrdNames()throws WebdavException {
    final Collection<PropertyTagXrdEntry> res = new ArrayList<>();

    res.addAll(super.getXrdNames());
    res.addAll(xrdNames.values());

    return res;
  }

  @Override
  public Collection<QName> getSupportedReports() {
    final Collection<QName> res = new ArrayList<>();
    final CalDAVCollection<?> c =
            (CalDAVCollection<?>)getCollection(true); // deref

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

  /* ==============================================================
   *                   Object methods
   * ============================================================== */

  @Override
  public String toString() {
    final ToString ts = new ToString(this);

    ts.append("path", getPath());
    try {
      ts.append("isCalendarCollection()", isCalendarCollection());
    } catch (final Throwable t) {
      ts.append(t);
    }

    return ts.toString();
  }

  /* ==============================================================
   *                   Private methods
   * ============================================================== */

  private boolean checkCalForSetProp(final SetPropertyResult spr) {
    if (col != null) {
      return true;
    }

    spr.status = HttpServletResponse.SC_NOT_FOUND;
    spr.message = "Not found";
    return false;
  }
}
