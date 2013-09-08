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

import edu.rpi.cct.webdav.servlet.shared.WdEntity;
import edu.rpi.cct.webdav.servlet.shared.WebdavException;
import org.bedework.util.xml.XmlEmit;

import java.util.Set;

import javax.xml.namespace.QName;

/** Class to represent an event/journal/task in CalDAV
 *
 * @author douglm
 *
 * @param <T>
 */
public abstract class CalDAVEvent <T> extends WdEntity<T> {
  /** Constructor
   *
   * @throws WebdavException
   */
  public CalDAVEvent() throws WebdavException {
    super();
  }

  /**
   * @return String schedule-tag (unquoted)
   * @throws WebdavException
   */
  public abstract String getScheduleTag() throws WebdavException;

  /** True if this is a valid organizer scheduling object. (See CalDAV
   * scheduling specification).
   *
   * @return boolean
   * @throws WebdavException
   */
  public abstract boolean getOrganizerSchedulingObject() throws WebdavException;

  /** True if this is a valid attendee scheduling object.
   * (See CalDAV scheduling specification)
   *
   * @return boolean
   * @throws WebdavException
   */
  public abstract boolean getAttendeeSchedulingObject() throws WebdavException;

  /**
   * @return String schedule-tag (unquoted)
   * @throws WebdavException
   */
  public abstract String getPrevScheduleTag() throws WebdavException;

  /**
   * @return String summary
   * @throws WebdavException
   */
  public abstract String getSummary() throws WebdavException;

  /**
   * @return boolean true if this will be created as a result of a Put
   * @throws WebdavException
   */
  public abstract boolean isNew() throws WebdavException;

  /**
   * @return true if this represents a deleted event.
   * @throws WebdavException
   */
  public abstract boolean getDeleted() throws WebdavException;

  /**
   * @return entity type defined in org.bedework.util.calendar.IcalDefs
   * @throws WebdavException
   */
  public abstract int getEntityType() throws WebdavException;

  /**
   * @param val Organizer
   * @throws WebdavException
   */
  public abstract void setOrganizer(Organizer val) throws WebdavException;

  /**
   * @return an  organizer if one is present.
   * @throws WebdavException
   */
  public abstract Organizer getOrganizer() throws WebdavException;

  /** Set the event's originator
   *
   * @param val    String event's originator
   * @throws WebdavException
   */
  public abstract void setOriginator(String val) throws WebdavException;

  /**
   * @param val
   * @throws WebdavException
   */
  public abstract void setRecipients(Set<String> val) throws WebdavException;

  /**
   * @return recipients
   * @throws WebdavException
   */
  public abstract Set<String> getRecipients() throws WebdavException;

  /**
   * @param val
   * @throws WebdavException
   */
  public abstract void addRecipient(String val) throws WebdavException;

  /**
   * @return attendee uris
   * @throws WebdavException
   */
  public abstract Set<String> getAttendeeUris() throws WebdavException;

  /** Set the scheduleMethod for this event. Takes methodType values defined
   * in Icalendar
   *
   * @param val    scheduleMethod
   * @throws WebdavException
   */
  public abstract void setScheduleMethod(int val) throws WebdavException;

  /**
   * @return String uid
   * @throws WebdavException
   */
  public abstract String getUid() throws WebdavException;

  /**
   * @param tag
   * @param xml
   * @return boolean true if value emitted.
   * @throws WebdavException
   */
  public abstract boolean generatePropertyValue(QName tag,
                                                XmlEmit xml) throws WebdavException;

  /** Return a complete representation of the event
   *
   * @param methodType
   * @return String ical representation
   * @throws WebdavException
   */
  public abstract String toIcalString(int methodType) throws WebdavException;
}
