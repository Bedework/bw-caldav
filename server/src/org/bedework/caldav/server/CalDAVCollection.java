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
   * @return true if this represents a deleted collection.
   * @throws WebdavException
   */
  public abstract boolean getDeleted() throws WebdavException;

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
