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

import edu.rpi.cct.webdav.servlet.shared.WebdavException;
import edu.rpi.cmt.calendar.ScheduleMethods;
import edu.rpi.cmt.calendar.IcalDefs.IcalComponentType;

import net.fortuna.ical4j.model.TimeZone;

import java.io.Serializable;
import java.util.Collection;
import java.util.Iterator;

/** Class to represent an RFC icalendar object converted to an internal form.
 *
 * @author Mike Douglass douglm   rpi.edu
 * @version 1.0
 */
public abstract class SysiIcalendar implements ScheduleMethods, Serializable {
  /**
   * @return String
   */
  public abstract String getProdid();

  /**
   * @return String
   */
  public abstract String getVersion();

  /**
   * @return String
   */
  public abstract String getCalscale();

  /**
   * @return String
   */
  public abstract String getMethod();

  /**
   * @return Collection
   */
  public abstract Collection<TimeZone> getTimeZones();

  /**
   * @return Collection
   */
  public abstract Collection<Object> getComponents();

  /**
   * @return ComponentType
   */
  public abstract IcalComponentType getComponentType();

  /**
   * @return int
   */
  public abstract int getMethodType();

  /**
   * @param val   String method name
   * @return int
   */
  public abstract int getMethodType(String val);

  /**
   * @param mt
   * @return A string value for the method
   */
  public abstract String getMethodName(int mt);

  /** An event or a free-busy request may contain an organizer. Return it if
   * it is present.
   *
   * @return organizer object if present.
   */
  public abstract Organizer getOrganizer();

  /**
   * @return CalDAVEvent
   * @throws WebdavException
   */
  public abstract CalDAVEvent getEvent() throws WebdavException;

  /**
   * @return Iterator
   */
  public abstract Iterator iterator();

  /**
   * @return int
   */
  public abstract int size();

  /** True for valid itip method
   *
   * @return boolean
   */
  public abstract boolean validItipMethodType();

  /** True for itip request type method
   *
   * @return boolean
   */
  public abstract boolean requestMethodType();

  /** True for itip reply type method
   *
   * @return boolean
   */
  public abstract boolean replyMethodType();

  /** True for itip request type method
   *
   * @param mt  method
   * @return boolean
   */
  public abstract boolean itipRequestMethodType(int mt);

  /** True for itip reply type method
   *
   * @param mt  method
   * @return boolean
   */
  public abstract boolean itipReplyMethodType(int mt);

  /** True for valid itip method
   *
   * @param val
   * @return boolean
   */
  public abstract boolean validItipMethodType(int val);

  public String toString() {
    StringBuilder sb = new StringBuilder("SysiIcalendar{prodid=");
    sb.append(getProdid());
    sb.append(", version=");
    sb.append(getVersion());

    sb.append("\n, method=");
    sb.append(String.valueOf(getMethod()));
    sb.append(", methodType=");
    sb.append(getMethodType());
    sb.append(", componentType=");
    sb.append(getComponentType());

    sb.append("}");

    return sb.toString();
  }
}
