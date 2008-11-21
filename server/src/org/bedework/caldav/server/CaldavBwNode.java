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

import edu.rpi.cct.webdav.servlet.shared.WebdavException;
import edu.rpi.cct.webdav.servlet.shared.WebdavNsIntf;
import edu.rpi.cct.webdav.servlet.shared.WebdavNsNode;
import edu.rpi.sss.util.xml.XmlEmit;
import edu.rpi.sss.util.xml.tagdefs.CaldavTags;
import edu.rpi.sss.util.xml.tagdefs.WebdavTags;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;

import javax.xml.namespace.QName;

/** Class to represent a caldav node.
 *
 *   @author Mike Douglass   douglm - rpi.edu
 */
public abstract class CaldavBwNode extends WebdavNsNode {
//  protected CaldavURI cdURI;

  private final static HashMap<QName, PropertyTagEntry> propertyNames =
    new HashMap<QName, PropertyTagEntry>();

  private final static Collection<QName> supportedReports = new ArrayList<QName>();

  static {
    addPropEntry(propertyNames, CaldavTags.calendarHomeSet);
    addPropEntry(propertyNames, CaldavTags.calendarUserAddressSet);
    addPropEntry(propertyNames, CaldavTags.calendarHomeURL);
    addPropEntry(propertyNames, CaldavTags.scheduleInboxURL);
    addPropEntry(propertyNames, CaldavTags.scheduleOutboxURL);

    supportedReports.add(CaldavTags.calendarMultiget); // Calendar access
    supportedReports.add(CaldavTags.calendarQuery);    // Calendar access
  }

  /* for accessing calendars */
  private SysIntf sysi;

  CaldavBwNode(CaldavURI cdURI, SysIntf sysi, boolean debug) {
    super(sysi.getUrlHandler(), cdURI.getPath(), cdURI.isCollection(),
          cdURI.getUri(), debug);

    //this.cdURI = cdURI;
    this.sysi = sysi;
  }

  CaldavBwNode(boolean collection, SysIntf sysi, String uri, boolean debug) {
    super(sysi.getUrlHandler(), null, collection, uri, debug);

    //this.cdURI = cdURI;
    this.sysi = sysi;
  }

  /* ====================================================================
   *                         Public methods
   * ==================================================================== */

  /**
   * @return BwCalendar containing or represented by this entity
   * @throws WebdavException
   */
  public abstract BwCalendar getCalendar() throws WebdavException ;

  /**
   * @return boolean if this is a calendar
   * @throws WebdavException
   */
  public boolean isCalendarCollection() throws WebdavException {
    return (isCollection() && getCalendar().getCalendarCollection());
  }

  /** Return a collection of children objects. These will all be calendar
   * entities.
   *
   * <p>Default is to return null
   *
   * @return Collection
   * @throws WebdavException
   */
  public Collection getChildren() throws WebdavException {
    return null;
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
  public Collection<QName> getSupportedReports() throws WebdavException {
    Collection<QName> res = new ArrayList<QName>();
    res.addAll(super.getSupportedReports());
    res.addAll(supportedReports);

    return res;
  }

  /* ====================================================================
   *                   Required webdav properties
   * ==================================================================== */

  /* (non-Javadoc)
   * @see edu.rpi.cct.webdav.servlet.shared.WebdavNsNode#getContentBinary()
   */
  public boolean getContentBinary() throws WebdavException {
    return false;
  }

  /* ====================================================================
   *                   Property methods
   * ==================================================================== */

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
      if (tag.equals(CaldavTags.calendarUserAddressSet)) {
        xml.openTag(tag);
        xml.property(WebdavTags.href, sysi.userToCaladdr(getOwner().getAccount()));
        xml.closeTag(tag);

        return true;
      }

      if (tag.equals(CaldavTags.calendarHomeSet)) {
        xml.openTag(tag);
        generateHref(xml, sysi.getCalUserInfo(getOwner().getAccount(), false).userHomePath);
        xml.closeTag(tag);

        return true;
      }

      if (tag.equals(CaldavTags.calendarHomeURL)) {
        xml.openTag(tag);
        generateHref(xml, sysi.getCalUserInfo(intf.getAccount(), false).userHomePath);
        xml.closeTag(tag);

        return true;
      }

      if (tag.equals(CaldavTags.scheduleInboxURL)) {
        xml.openTag(tag);
        generateHref(xml, sysi.getCalUserInfo(intf.getAccount(), false).inboxPath);
        xml.closeTag(tag);

        return true;
      }

      if (tag.equals(CaldavTags.scheduleOutboxURL)) {
        xml.openTag(tag);
        generateHref(xml, sysi.getCalUserInfo(intf.getAccount(), false).outboxPath);
        xml.closeTag(tag);

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

  /* ====================================================================
   *                   Object methods
   * ==================================================================== */

  public String toString() {
    StringBuffer sb = new StringBuffer(this.getClass().getName());

    sb.append("{");
    sb.append("path=");
    sb.append(getPath());
    sb.append("}");

    return sb.toString();
  }
}
