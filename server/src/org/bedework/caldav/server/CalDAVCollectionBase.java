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

import edu.rpi.cct.webdav.servlet.shared.WdCollection;
import edu.rpi.cct.webdav.servlet.shared.WebdavException;

/** Simple implementation of class to represent a collection in CalDAV
 *
 * @author douglm
 *
 */
public class CalDAVCollectionBase extends CalDAVCollection {
  private int calType;

  /** Resource collection. According to the CalDAV spec a collection may exist
   * inside a calendar collection but no calendar collection must be so
   * contained at any depth. (RFC 4791 Section 4.2) */
  public final static int calTypeResource = 9;

  private boolean freebusyAllowed;

  private boolean affectsFreeBusy = true;

  private String timezone;

  private String color;

  /** Constructor
   *
   * @param calType
   * @param freebusyAllowed
   * @throws WebdavException
   */
  public CalDAVCollectionBase(int calType,
                              boolean freebusyAllowed) throws WebdavException {
    super();

    this.calType = calType;
    this.freebusyAllowed = freebusyAllowed;
  }

  /* ====================================================================
   *                      Abstract method implementations
   * ==================================================================== */

  /* (non-Javadoc)
   * @see edu.rpi.cct.webdav.servlet.shared.WdCollection#isAlias()
   */
  public boolean isAlias() throws WebdavException {
    return false;
  }

  /* (non-Javadoc)
   * @see edu.rpi.cct.webdav.servlet.shared.WdCollection#getAliasTarget()
   */
  public WdCollection getAliasTarget() throws WebdavException {
    return null;
  }

  /* (non-Javadoc)
   * @see org.bedework.caldav.server.CalDAVCollection#setCalType(int)
   */
  public void setCalType(int val) throws WebdavException {
    calType = val;
  }

  public int getCalType() throws WebdavException {
    return calType;
  }

  public boolean freebusyAllowed() throws WebdavException {
    return freebusyAllowed;
  }

  /* (non-Javadoc)
   * @see org.bedework.caldav.server.CalDAVCollection#entitiesAllowed()
   */
  public boolean entitiesAllowed() throws WebdavException {
    return (calType == calTypeCalendarCollection) ||
           (calType == calTypeInbox) ||
           (calType == calTypeOutbox);
  }

  /* (non-Javadoc)
   * @see org.bedework.caldav.server.CalDAVCollection#setAffectsFreeBusy(boolean)
   */
  public void setAffectsFreeBusy(boolean val) throws WebdavException {
    affectsFreeBusy = val;
  }

  /* (non-Javadoc)
   * @see org.bedework.caldav.server.CalDAVCollection#getAffectsFreeBusy()
   */
  public boolean getAffectsFreeBusy() throws WebdavException {
    return affectsFreeBusy;
  }

  /* (non-Javadoc)
   * @see org.bedework.caldav.server.CalDAVCollection#setTimezone(java.lang.String)
   */
  public void setTimezone(String val) throws WebdavException {
    timezone = val;
  }

  /* (non-Javadoc)
   * @see org.bedework.caldav.server.CalDAVCollection#getTimezone()
   */
  public String getTimezone() throws WebdavException {
    return timezone;
  }

  /* (non-Javadoc)
   * @see org.bedework.caldav.server.CalDAVCollection#setColor(java.lang.String)
   */
  public void setColor(String val) throws WebdavException {
    color = val;
  }

  /* (non-Javadoc)
   * @see org.bedework.caldav.server.CalDAVCollection#getColor()
   */
  public String getColor() throws WebdavException {
    return color;
  }
}
