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

/** Simple implementation of class to represent a collection in CalDAV, used
 * by the simpler interfaces.
 *
 * @author douglm
 *
 * @param <T>
 */
public abstract class CalDAVCollectionBase <T extends CalDAVCollectionBase<?>> extends CalDAVCollection<T> {
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
   * @param calType collection type
   * @param freebusyAllowed true if freeebusy allowed on the collection
   */
  public CalDAVCollectionBase(final int calType,
                              final boolean freebusyAllowed) {
    super();

    this.calType = calType;
    this.freebusyAllowed = freebusyAllowed;
  }

  /* ====================================================================
   *                      Abstract method implementations
   * ==================================================================== */

  @Override
  public boolean isAlias() {
    return false;
  }

  @Override
  public T resolveAlias(final boolean resolveSubAlias) {
    return null;
  }

  /* (non-Javadoc)
   * @see org.bedework.caldav.server.CalDAVCollection#setCalType(int)
   */
  @Override
  public void setCalType(final int val) {
    calType = val;
  }

  @Override
  public int getCalType() {
    return calType;
  }

  @Override
  public boolean freebusyAllowed() {
    return freebusyAllowed;
  }

  /* (non-Javadoc)
   * @see org.bedework.caldav.server.CalDAVCollection#entitiesAllowed()
   */
  @Override
  public boolean entitiesAllowed() {
    return (calType == calTypeCalendarCollection) ||
           (calType == calTypeInbox) ||
           (calType == calTypeOutbox);
  }

  /* (non-Javadoc)
   * @see org.bedework.caldav.server.CalDAVCollection#setAffectsFreeBusy(boolean)
   */
  @Override
  public void setAffectsFreeBusy(final boolean val) {
    affectsFreeBusy = val;
  }

  /* (non-Javadoc)
   * @see org.bedework.caldav.server.CalDAVCollection#getAffectsFreeBusy()
   */
  @Override
  public boolean getAffectsFreeBusy() {
    return affectsFreeBusy;
  }

  /* (non-Javadoc)
   * @see org.bedework.caldav.server.CalDAVCollection#setTimezone(java.lang.String)
   */
  @Override
  public void setTimezone(final String val) {
    timezone = val;
  }

  /* (non-Javadoc)
   * @see org.bedework.caldav.server.CalDAVCollection#getTimezone()
   */
  @Override
  public String getTimezone() {
    return timezone;
  }

  /* (non-Javadoc)
   * @see org.bedework.caldav.server.CalDAVCollection#setColor(java.lang.String)
   */
  @Override
  public void setColor(final String val) {
    color = val;
  }

  @Override
  public String getColor() {
    return color;
  }

  @Override
  public void setAliasUri(final String val) {
    aliasUri = val;
  }

  @Override
  public String getAliasUri() {
    return aliasUri;
  }

  @Override
  public void setRemoteId(final String val) {
    remoteId = val;
  }

  @Override
  public String getRemoteId() {
    return remoteId;
  }

  @Override
  public void setRemotePw(final String val) {
    remotePw = val;
  }

  @Override
  public String getRemotePw() {
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
