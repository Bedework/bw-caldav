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
public abstract class SysiIcalendar implements ScheduleMethods,
                                               Iterator<WdEntity>,
                                               Iterable<WdEntity>, Serializable {
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
