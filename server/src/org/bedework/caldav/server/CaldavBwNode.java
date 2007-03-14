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
import org.bedework.davdefs.CaldavTags;

import edu.rpi.cct.webdav.servlet.shared.WebdavException;
import edu.rpi.cct.webdav.servlet.shared.WebdavNsNode;
import edu.rpi.sss.util.xml.QName;

import java.util.ArrayList;
import java.util.Collection;

/** Class to represent a caldav node.
 *
 *   @author Mike Douglass   douglm - rpi.edu
 */
public abstract class CaldavBwNode extends WebdavNsNode {
//  protected CaldavURI cdURI;

  private final static Collection<QName> supportedReports = new ArrayList<QName>();

  static {
    supportedReports.add(CaldavTags.calendarMultiget); // Calendar access
    supportedReports.add(CaldavTags.calendarQuery);    // Calendar access
  }

  /* for accessing calendars */
  private SysIntf sysi;

  CaldavBwNode(CaldavURI cdURI, SysIntf sysi, boolean debug) {
    super(sysi.getUrlPrefix(), cdURI.getPath(), cdURI.isCollection(), debug);

    //this.cdURI = cdURI;
    this.sysi = sysi;

    if (cdURI != null) {
      uri = cdURI.getUri();
    }
  }

  /* ====================================================================
   *                         Public methods
   * ==================================================================== */

  /**
   * @return BwCalendar containing or represented by this entity
   */
  public abstract BwCalendar getCalendar();

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
