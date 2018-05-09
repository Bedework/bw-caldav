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

import org.bedework.caldav.server.sysinterface.SysIntf;
import org.bedework.util.calendar.XcalUtil;
import org.bedework.util.xml.tagdefs.CalWSSoapTags;
import org.bedework.util.xml.tagdefs.CalWSXrdDefs;
import org.bedework.util.xml.tagdefs.CaldavTags;
import org.bedework.util.xml.tagdefs.XrdTags;
import org.bedework.webdav.servlet.shared.WdCollection;
import org.bedework.webdav.servlet.shared.WdEntity;
import org.bedework.webdav.servlet.shared.WebdavException;
import org.bedework.webdav.servlet.shared.WebdavNsIntf;
import org.bedework.webdav.servlet.shared.WebdavNsNode;

import org.oasis_open.docs.ns.xri.xrd_1.LinkType;
import org.oasis_open.docs.ns.xri.xrd_1.PropertyType;
import org.oasis_open.docs.ws_calendar.ns.soap.CalendarAccessFeatureType;
import org.oasis_open.docs.ws_calendar.ns.soap.CreationDateTimeType;
import org.oasis_open.docs.ws_calendar.ns.soap.DisplayNameType;
import org.oasis_open.docs.ws_calendar.ns.soap.GetPropertiesBasePropertyType;
import org.oasis_open.docs.ws_calendar.ns.soap.ResourceOwnerType;
import org.oasis_open.docs.ws_calendar.ns.soap.SupportedFeaturesType;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

import javax.xml.bind.JAXBElement;
import javax.xml.namespace.QName;

/** Class to represent a caldav node.
 *
 *   @author Mike Douglass   douglm - rpi.edu
 */
public abstract class CaldavBwNode extends WebdavNsNode {
//  protected CaldavURI cdURI;

  protected boolean rootNode;

  protected CalDAVCollection col;

  private final static HashMap<QName, PropertyTagEntry> propertyNames =
    new HashMap<>();

  private final static Collection<QName> supportedReports =
          new ArrayList<>();

  private final static HashMap<QName, PropertyTagEntry> calWSSoapNames =
    new HashMap<>();

  static {
    supportedReports.add(CaldavTags.calendarMultiget); // Calendar access
    supportedReports.add(CaldavTags.calendarQuery);    // Calendar access

    addCalWSSoapName(CalWSSoapTags.creationDateTime, true);
    addCalWSSoapName(CalWSSoapTags.displayName, true);
    addCalWSSoapName(CalWSSoapTags.lastModifiedDateTime, true);
    addCalWSSoapName(CalWSSoapTags.supportedFeatures, true);
  }

  /** Information about properties returned in an XRD object for the restful
   * protocol
   */
  public static final class PropertyTagXrdEntry extends PropertyTagEntry {
    /** */
    public String xrdName;

    /** */
    public boolean inLink;

    /**
     * @param tag
     * @param xrdName
     * @param inPropAll
     * @param inLink
     */
    public PropertyTagXrdEntry(final QName tag, final String xrdName,
                               final boolean inPropAll,
                               final boolean inLink) {
      super(tag, inPropAll);
      this.xrdName = xrdName;
      this.inLink = inLink;
    }
  }

  private final static HashMap<String, PropertyTagXrdEntry> xrdNames =
    new HashMap<String, PropertyTagXrdEntry>();

  static {
    addXrdEntry(xrdNames, CalWSXrdDefs.created, true, false);
    addXrdEntry(xrdNames, CalWSXrdDefs.displayname, true, true);
    addXrdEntry(xrdNames, CalWSXrdDefs.lastModified, true, false);
    addXrdEntry(xrdNames, CalWSXrdDefs.owner, true, false);
  }

  /* for accessing calendars */
  private SysIntf sysi;

  CaldavBwNode(final CaldavURI cdURI,
               final SysIntf sysi) throws WebdavException {
    this(sysi, cdURI.getPath(), cdURI.isCollection(),
          cdURI.getUri());
  }

  CaldavBwNode(final SysIntf sysi,
               final String path,
               final boolean collection,
               final String uri) {
    super(sysi, sysi.getUrlHandler(), path, collection, uri);

    this.sysi = sysi;

    rootNode = (uri != null) && uri.equals("/");
  }

  CaldavBwNode(final boolean collection,
               final SysIntf sysi,
               final String uri) {
    super(sysi, sysi.getUrlHandler(), null, collection, uri);

    //this.cdURI = cdURI;
    this.sysi = sysi;

    rootNode = (uri != null) && uri.equals("/");
  }

  /** Returns a string value suitable for the web service token
   *
   * @return String token
   * @throws WebdavException
   */
  public abstract String getEtokenValue() throws WebdavException;

  /* ====================================================================
   *                         Public methods
   * ==================================================================== */

  /**
   * @return the interface
   */
  public SysIntf getIntf() {
    return sysi;
  }

  @Override
  public WdCollection getCollection(final boolean deref) throws WebdavException {
    if (!deref) {
      return col;
    }

    return col.resolveAlias(true); // True to resolve all subaliases
  }

  @Override
  public WdCollection getImmediateTargetCollection() throws WebdavException {
    return col.resolveAlias(false); // False => don't resolve all subaliases
  }

  /**
   * @return boolean if this is a calendar
   * @throws WebdavException
   */
  public boolean isCalendarCollection() throws WebdavException {
    if (!isCollection()) {
      return false;
    }

    CalDAVCollection c = (CalDAVCollection)getCollection(true);
    if (c == null) {
      return false;
    }

    return c.getCalType() == CalDAVCollection.calTypeCalendarCollection;
  }

  /**
   * @return CalSvcI
   */
  public SysIntf getSysi() {
    return sysi;
  }

  /** Return a set of Qname defining reports this node supports.
   *
   * @return Collection of QName
   * @throws WebdavException
   */
  @Override
  public Collection<QName> getSupportedReports() throws WebdavException {
    Collection<QName> res = new ArrayList<QName>();
    res.addAll(super.getSupportedReports());
    res.addAll(supportedReports);

    return res;
  }

  @Override
  public String getSyncToken() throws WebdavException {
    return null;
  }

  /* ====================================================================
   *                   Required webdav properties
   * ==================================================================== */

  @Override
  public boolean getContentBinary() throws WebdavException {
    return false;
  }

  @Override
  public Collection<? extends WdEntity> getChildren() throws WebdavException {
    return null;
  }

  /* ====================================================================
   *                   Property methods
   * ==================================================================== */

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
    try {
      // Not known - try higher
      return super.generatePropertyValue(tag, intf, allProp);
    } catch (WebdavException wde) {
      throw wde;
    } catch (Throwable t) {
      throw new WebdavException(t);
    }
  }

  /**
   * @param props
   * @param tag
   * @param intf
   * @param allProp
   * @return true if property emitted
   * @throws WebdavException
   */
  public boolean generateCalWsProperty(final List<GetPropertiesBasePropertyType> props,
                                       final QName tag,
                                       final WebdavNsIntf intf,
                                       final boolean allProp) throws WebdavException {
    try {
      if (tag.equals(CalWSSoapTags.creationDateTime)) {
        String val = getCreDate();
        if (val == null) {
          return true;
        }

        CreationDateTimeType cdt = new CreationDateTimeType();
        cdt.setDateTime(XcalUtil.fromDtval(val));
        props.add(cdt);
        return true;
      }

      if (tag.equals(CalWSSoapTags.displayName)) {
        String val = getDisplayname();
        if (val == null) {
          return true;
        }

        DisplayNameType dn = new DisplayNameType();
        dn.setString(val);
        props.add(dn);
        return true;
      }

      if (tag.equals(CalWSSoapTags.supportedFeatures)) {
        SupportedFeaturesType sf = new SupportedFeaturesType();

        sf.getCalendarAccessFeature().add(new CalendarAccessFeatureType());

        props.add(sf);
        return true;
      }

      if (tag.equals(CalWSSoapTags.resourceOwner)) {
        String href = intf.makeUserHref(getOwner().getPrincipalRef());
        if (!href.endsWith("/")) {
          href += "/";
        }

        ResourceOwnerType ro = new ResourceOwnerType();

        ro.setString(href);
        props.add(ro);

        return true;
      }

      return false;
    } catch (WebdavException wde) {
      throw wde;
    } catch (Throwable t) {
      throw new WebdavException(t);
    }
  }

  /**
   * @param props
   * @param name
   * @param intf
   * @param allProp
   * @return true if proeprty emitted
   * @throws WebdavException
   */
  public boolean generateXrdProperties(final List<Object> props,
                                       final String name,
                                       final WebdavNsIntf intf,
                                       final boolean allProp) throws WebdavException {
    try {
      if (name.equals(CalWSXrdDefs.created)) {
        String val = getCreDate();
        if (val == null) {
          return true;
        }

        props.add(xrdProperty(name, val));
        return true;
      }

      if (name.equals(CalWSXrdDefs.displayname)) {
        String val = getDisplayname();
        if (val == null) {
          return true;
        }

        props.add(xrdProperty(name, val));
        return true;
      }

      if (name.equals(CalWSXrdDefs.lastModified)) {
        String val = getLastmodDate();
        if (val == null) {
          return true;
        }

        props.add(xrdProperty(name, val));
        return true;
      }

      if (name.equals(CalWSXrdDefs.owner)) {
        String href = intf.makeUserHref(getOwner().getPrincipalRef());
        if (!href.endsWith("/")) {
          href += "/";
        }
        props.add(xrdProperty(name, href));

        return true;
      }

      return false;
    } catch (WebdavException wde) {
      throw wde;
    } catch (Throwable t) {
      throw new WebdavException(t);
    }
  }

  /* ====================================================================
   *                   Protected methods
   * ==================================================================== */

  /** Return a set of PropertyTagEntry defining properties this node supports.
   *
   * @return Collection of PropertyTagEntry
   * @throws WebdavException
   */
  public Collection<PropertyTagXrdEntry> getXrdNames() throws WebdavException {
    return xrdNames.values();
  }

  /** Return a set of PropertyTagEntry defining CalWS-SOAP properties this node
   * supports.
   *
   * @return Collection of PropertyTagEntry
   * @throws WebdavException
   */
  public Collection<PropertyTagEntry> getCalWSSoapNames() throws WebdavException {
    return calWSSoapNames.values();
  }

  @SuppressWarnings("unchecked")
  protected JAXBElement<PropertyType> xrdProperty(final String name,
                                     final String val) throws WebdavException {
    PropertyType p = new PropertyType();
    p.setType(name);
    p.setValue(val);

    return new JAXBElement(XrdTags.property, PropertyType.class, p);
  }

  @SuppressWarnings("unchecked")
  protected JAXBElement<LinkType> xrdLink(final String name,
                                          final Object val) throws WebdavException {
    LinkType l = new LinkType();
    l.setType(name);
    l.getTitleOrPropertyOrAny().add(val);

    return new JAXBElement(XrdTags.link, LinkType.class, l);
  }

  @SuppressWarnings("unchecked")
  protected JAXBElement<PropertyType> xrdEmptyProperty(final String name) throws WebdavException {
    PropertyType p = new PropertyType();
    p.setType(name);

    return new JAXBElement(XrdTags.property, PropertyType.class, p);
  }

  /**
   * @return formatted url value for the node
   * @throws WebdavException
   */
  public String getUrlValue() throws WebdavException {
    return getUrlValue(uri, exists);
  }

  /**
   * @param uri
   * @param exists - true if we KNOW it exists
   * @return formatted url value
   * @throws WebdavException
   */
  public String getUrlValue(final String uri,
                            final boolean exists) throws WebdavException {
    try {
      String prefixed = urlHandler.prefix(uri);

      if (exists) {
        if (prefixed.endsWith("/")) {
          if (!trailSlash()) {
            prefixed = prefixed.substring(0, prefixed.length() - 1);
          }
        } else {
          if (trailSlash()) {
            prefixed = prefixed + "/";
          }
        }
      }

      return prefixed;
    } catch (Throwable t) {
      throw new WebdavException(t);
    }
  }

  protected static void addCalWSSoapName(final QName tag,
                                         final boolean inAllProp) {
    PropertyTagEntry pte = new PropertyTagEntry(tag, inAllProp);
    calWSSoapNames.put(tag, pte);
  }

  protected static void addPropEntry(final HashMap<QName, PropertyTagEntry> propertyNames,
                                     final HashMap<String, PropertyTagXrdEntry> xrdNames,
                                     final QName tag,
                                     final String xrdName) {
    PropertyTagXrdEntry pte = new PropertyTagXrdEntry(tag, xrdName, false,
                                                      false);
    propertyNames.put(tag, pte);
    xrdNames.put(xrdName, pte);
  }

  protected static void addPropEntry(final HashMap<QName, PropertyTagEntry> propertyNames,
                                     final HashMap<String, PropertyTagXrdEntry> xrdNames,
                                     final QName tag,
                                     final String xrdName,
                                     final boolean inAllProp) {
    PropertyTagXrdEntry pte = new PropertyTagXrdEntry(tag, xrdName, inAllProp,
                                                      false);
    propertyNames.put(tag, pte);
    xrdNames.put(xrdName, pte);
  }

  protected static void addXrdEntry(final HashMap<String, PropertyTagXrdEntry> xrdNames,
                                    final String xrdName) {
    PropertyTagXrdEntry pte = new PropertyTagXrdEntry(null, xrdName, false,
                                                      false);
    xrdNames.put(xrdName, pte);
  }

  protected static void addXrdEntry(final HashMap<String, PropertyTagXrdEntry> xrdNames,
                                    final String xrdName,
                                    final boolean inAllProp,
                                    final boolean inLink) {
    PropertyTagXrdEntry pte = new PropertyTagXrdEntry(null, xrdName,
                                                      inAllProp, inLink);
    xrdNames.put(xrdName, pte);
  }

  protected String concatEtoken(final String... val) {
    StringBuilder sb = new StringBuilder();

    for (int i = 0; i < val.length; i++) {
      sb.append(val[i]);

      if ((i + 1) < val.length) {
        sb.append('\t');
      }
    }

    return sb.toString();
  }

  protected String[] splitEtoken(final String val) {
    return val.split("\t");
  }

  /* ====================================================================
   *                   Object methods
   * ==================================================================== */

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder(this.getClass().getName());

    sb.append("{");
    sb.append("path=");
    sb.append(getPath());
    sb.append("}");

    return sb.toString();
  }
}
