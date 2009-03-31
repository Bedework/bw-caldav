/* **********************************************************************
    Copyright 2009 Rensselaer Polytechnic Institute. All worldwide rights reserved.

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

/** Class to represent a collection in CalDAV
 *
 * @author douglm
 *
 */
public abstract class CalDAVCollection extends WdCollection {
  /** Indicate unknown type */
  public final static int calTypeUnknown = -1;

  /** Normal folder */
  public final static int calTypeCollection = 0;

  /** Normal calendar collection */
  public final static int calTypeCalendarCollection = 1;

  /** Inbox  */
  public final static int calTypeInbox = 2;

  /** Outbox  */
  public final static int calTypeOutbox = 3;

  /** Constructor
   *
   * @throws WebdavException
   */
  public CalDAVCollection() throws WebdavException {
    super();
  }

  /* ====================================================================
   *                      Abstract methods
   * ==================================================================== */

  /**
   *  @param val
   * @throws WebdavException
   */
  public abstract void setCalType(int val) throws WebdavException;

  /**
   * @return int
   * @throws WebdavException
   */
  public abstract int getCalType() throws WebdavException;

  /**
   * @return true if freebusy reports are allowed
   * @throws WebdavException
   */
  public abstract boolean freebusyAllowed() throws WebdavException;

  /**
   * @return true if entities can be stored
   * @throws WebdavException
   */
  public abstract boolean entitiesAllowed() throws WebdavException;

  /**
   *
   *  @param val    true if the calendar takes part in free/busy calculations
   * @throws WebdavException
   */
  public abstract void setAffectsFreeBusy(boolean val) throws WebdavException;

  /**
   *
   *  @return boolean    true if the calendar takes part in free/busy calculations
   * @throws WebdavException
   */
  public abstract boolean getAffectsFreeBusy() throws WebdavException;

  /** Set the collection timezone property
   *
   * @param val
   * @throws WebdavException
   */
  public abstract void setTimezone(String val) throws WebdavException;

  /** Get the collection timezone property
   *
   * @return String vtimezone spec
   * @throws WebdavException
   */
  public abstract String getTimezone() throws WebdavException;

  /** Set the calendar color property
   *
   * @param val
   * @throws WebdavException
   */
  public abstract void setColor(String val) throws WebdavException;

  /** Get the calendar color property
   *
   * @return String calendar color
   * @throws WebdavException
   */
  public abstract String getColor() throws WebdavException;
}
