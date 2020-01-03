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

import org.bedework.webdav.servlet.shared.WdCollection;

import java.util.List;

/** Class to represent a collection in CalDAV
 *
 * @author douglm
 *
 * @param <T>
 */
public abstract class CalDAVCollection <T extends CalDAVCollection<?>>
        extends WdCollection<T> {
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

  /** Outbox  */
  public final static int calTypeNotifications = 4;

  /** Constructor
   *
   */
  public CalDAVCollection() {
    super();
  }

  /* ====================================================================
   *                      Abstract methods
   * ==================================================================== */

  @Override
  public abstract T resolveAlias(final boolean resolveSubAlias);

  /**
   *  @param val type
   */
  public abstract void setCalType(int val);

  /**
   * @return int
   */
  public abstract int getCalType();

  /**
   * @return true if freebusy reports are allowed
   */
  public abstract boolean freebusyAllowed();

  /**
   * @return true if this represents a deleted collection.
   */
  public abstract boolean getDeleted();

  /**
   * @return true if entities can be stored
   */
  public abstract boolean entitiesAllowed();

  /**
   *
   *  @param val    true if the calendar takes part in free/busy calculations
   */
  public abstract void setAffectsFreeBusy(boolean val);

  /**
   *
   *  @return boolean    true if the calendar takes part in free/busy calculations
   */
  public abstract boolean getAffectsFreeBusy();

  /** Set the collection timezone property
   *
   * @param val the tzid
   */
  public abstract void setTimezone(String val);

  /** Get the collection timezone property
   *
   * @return String vtimezone spec
   */
  public abstract String getTimezone();

  /** Set the calendar color property
   *
   * @param val the color
   */
  public abstract void setColor(String val);

  /** Get the calendar color property
   *
   * @return String calendar color
   */
  public abstract String getColor();

  /** Set the calendar aliasUri property
   *
   * @param val alias uri
   */
  public abstract void setAliasUri(String val);

  /** Get the calendar aliasUri property
   *
   * @return String calendar AliasUri
   */
  public abstract String getAliasUri();

  /** Set the calendar refresh rate
   *
   * @param val - seconds
   */
  public abstract void setRefreshRate(int val);

  /** Get the calendar refresh rate
   *
   * @return int seconds
   */
  public abstract int getRefreshRate();

  /** Set the calendar remoteId property
   *
   * @param val remote id
   */
  public abstract void setRemoteId(String val);

  /** Get the calendar remoteId property
   *
   * @return String calendar RemoteId
   */
  public abstract String getRemoteId();

  /** Set the calendar remotePw property
   *
   * @param val calendar remotePw property
   */
  public abstract void setRemotePw(String val);

  /** Get the calendar remotePw property
   *
   * @return String calendar RemotePw
   */
  public abstract String getRemotePw();

  /** Set the deletions suppressed flag for synch
   *
   * @param val true if we suppress deletions during synch
   */
  public abstract void setSynchDeleteSuppressed(final boolean val)
         ;

  /** Get the deletions suppressed flag for synch
   *
   * @return boolean on/off
   */
  public abstract boolean getSynchDeleteSuppressed()
         ;

  /**
   * @param val the supported component names e.g. "VEVENT", "VTODO" etc.
   */
  public abstract void setSupportedComponents(List<String> val);

  /**
   * @return the supported component names e.g. "VEVENT", "VTODO" etc.
   */
  public abstract List<String> getSupportedComponents();

  /**
   * @return the vpoll supported component names e.g. "VEVENT", "VTODO" etc.
   */
  public abstract List<String> getVpollSupportedComponents();
}
