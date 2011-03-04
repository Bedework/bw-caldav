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

/** Simple implementation of class to represent a collection in CalDAV, used
 * by the simpler interfaces.
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
