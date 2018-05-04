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

import org.bedework.webdav.servlet.shared.WebdavException;

/** Simple implementation of class to represent a collection in CalDAV, used
 * by the simpler interfaces.
 *
 * @author douglm
 *
 * @param <T>
 */
public abstract class CalDAVCollectionBase <T extends CalDAVCollectionBase> extends CalDAVCollection<T> {
  private int calType;

  /** Resource collection. According to the CalDAV spec a collection may exist
   * inside a calendar collection but no calendar collection must be so
   * contained at any depth. (RFC 4791 Section 4.2) */
  public final static int calTypeResource = 9;

  private final boolean freebusyAllowed;

  private boolean affectsFreeBusy = true;

  private String timezone;

  private String color;

  private String aliasUri;

  private String remoteId;

  private String remotePw;

  private boolean synchDeleteSuppressed;

  /** Constructor
   *
   * @param calType
   * @param freebusyAllowed
   * @throws WebdavException
   */
  public CalDAVCollectionBase(final int calType,
                              final boolean freebusyAllowed) throws WebdavException {
    super();

    this.calType = calType;
    this.freebusyAllowed = freebusyAllowed;
  }

  /* ====================================================================
   *                      Abstract method implementations
   * ==================================================================== */

  @Override
  public boolean isAlias() throws WebdavException {
    return false;
  }

  @Override
  public T resolveAlias(final boolean resolveSubAlias) throws WebdavException {
    return null;
  }

  /* (non-Javadoc)
   * @see org.bedework.caldav.server.CalDAVCollection#setCalType(int)
   */
  @Override
  public void setCalType(final int val) throws WebdavException {
    calType = val;
  }

  @Override
  public int getCalType() throws WebdavException {
    return calType;
  }

  @Override
  public boolean freebusyAllowed() throws WebdavException {
    return freebusyAllowed;
  }

  /* (non-Javadoc)
   * @see org.bedework.caldav.server.CalDAVCollection#entitiesAllowed()
   */
  @Override
  public boolean entitiesAllowed() throws WebdavException {
    return (calType == calTypeCalendarCollection) ||
           (calType == calTypeInbox) ||
           (calType == calTypeOutbox);
  }

  /* (non-Javadoc)
   * @see org.bedework.caldav.server.CalDAVCollection#setAffectsFreeBusy(boolean)
   */
  @Override
  public void setAffectsFreeBusy(final boolean val) throws WebdavException {
    affectsFreeBusy = val;
  }

  /* (non-Javadoc)
   * @see org.bedework.caldav.server.CalDAVCollection#getAffectsFreeBusy()
   */
  @Override
  public boolean getAffectsFreeBusy() throws WebdavException {
    return affectsFreeBusy;
  }

  /* (non-Javadoc)
   * @see org.bedework.caldav.server.CalDAVCollection#setTimezone(java.lang.String)
   */
  @Override
  public void setTimezone(final String val) throws WebdavException {
    timezone = val;
  }

  /* (non-Javadoc)
   * @see org.bedework.caldav.server.CalDAVCollection#getTimezone()
   */
  @Override
  public String getTimezone() throws WebdavException {
    return timezone;
  }

  /* (non-Javadoc)
   * @see org.bedework.caldav.server.CalDAVCollection#setColor(java.lang.String)
   */
  @Override
  public void setColor(final String val) throws WebdavException {
    color = val;
  }

  @Override
  public String getColor() throws WebdavException {
    return color;
  }

  @Override
  public void setAliasUri(final String val) throws WebdavException {
    aliasUri = val;
  }

  @Override
  public String getAliasUri() throws WebdavException {
    return aliasUri;
  }

  @Override
  public void setRemoteId(final String val) throws WebdavException {
    remoteId = val;
  }

  @Override
  public String getRemoteId() throws WebdavException {
    return remoteId;
  }

  @Override
  public void setRemotePw(final String val) throws WebdavException {
    remotePw = val;
  }

  @Override
  public String getRemotePw() throws WebdavException {
    return remotePw;
  }

  @Override
  public void setSynchDeleteSuppressed(final boolean val) {
    synchDeleteSuppressed= val;
  }

  @Override
  public boolean getSynchDeleteSuppressed() {
    return synchDeleteSuppressed;
  }
}
