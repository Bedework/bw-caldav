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

import org.bedework.util.xml.XmlEmit;
import org.bedework.webdav.servlet.shared.WdEntity;

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
   */
  public CalDAVEvent() {
    super();
  }

  /**
   * @return String schedule-tag (unquoted)
   */
  public abstract String getScheduleTag();

  /** True if this is a valid organizer scheduling object. (See CalDAV
   * scheduling specification).
   *
   * @return boolean
   */
  public abstract boolean getOrganizerSchedulingObject();

  /** True if this is a valid attendee scheduling object.
   * (See CalDAV scheduling specification)
   *
   * @return boolean
   */
  public abstract boolean getAttendeeSchedulingObject();

  /**
   * @return String schedule-tag (unquoted)
   */
  public abstract String getPrevScheduleTag();

  /**
   * @return String summary
   */
  public abstract String getSummary();

  /**
   * @return boolean true if this will be created as a result of a Put
   */
  public abstract boolean isNew();

  /**
   * @return true if this represents a deleted event.
   */
  public abstract boolean getDeleted();

  /**
   * @return entity type defined in org.bedework.util.calendar.IcalDefs
   */
  public abstract int getEntityType();

  /**
   * @param val Organizer
   */
  public abstract void setOrganizer(Organizer val);

  /**
   * @return an  organizer if one is present.
   */
  public abstract Organizer getOrganizer();

  /** Set the event's originator
   *
   * @param val    String event's originator
   */
  public abstract void setOriginator(String val);

  /**
   * @param val set of recipients
   */
  public abstract void setRecipients(Set<String> val);

  /**
   * @return recipients
   */
  public abstract Set<String> getRecipients();

  /**
   * @param val a recipient
   */
  public abstract void addRecipient(String val);

  /**
   * @return attendee uris
   */
  public abstract Set<String> getAttendeeUris();

  /** Set the scheduleMethod for this event. Takes methodType values defined
   * in Icalendar
   *
   * @param val    scheduleMethod
   */
  public abstract void setScheduleMethod(int val);

  /** Get the scheduleMethod for this event. Takes methodType values defined
   * in Icalendar
   *
   * @return the method
   */
  public abstract int getScheduleMethod();

  /**
   * @return String uid
   */
  public abstract String getUid();

  /**
   * @param tag QName of property
   * @param xml emitter
   * @return boolean true if value emitted.
   */
  public abstract boolean generatePropertyValue(QName tag,
                                                XmlEmit xml);

  /** Return a complete representation of the event
   *
   * @param methodType iTip method indicator
   * @return String ical representation
   */
  public abstract String toIcalString(int methodType,
                                      String contentType);
}
