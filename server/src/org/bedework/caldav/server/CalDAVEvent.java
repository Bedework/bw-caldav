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

import edu.rpi.cct.webdav.servlet.shared.WdEntity;
import edu.rpi.cct.webdav.servlet.shared.WebdavException;
import edu.rpi.sss.util.xml.XmlEmit;

import java.util.Set;

import javax.xml.namespace.QName;

/** Class to represent an event/journal/task in CalDAV
 *
 * @author douglm
 *
 */
public abstract class CalDAVEvent extends WdEntity {
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
   * @return entity type defined in edu.rpi.cmt.calendar.IcalDefs
   * @throws WebdavException
   */
  public abstract int getEntityType() throws WebdavException;

  /**
   * @param val Organizer
   * @throws WebdavException
   */
  public abstract void setOrganizer(Organizer val) throws WebdavException;

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
