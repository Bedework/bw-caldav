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

import org.bedework.util.calendar.IcalDefs.IcalComponentType;
import org.bedework.util.calendar.ScheduleMethods;
import org.bedework.base.ToString;
import org.bedework.webdav.servlet.shared.WdEntity;

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
                                               Iterator<WdEntity<?>>,
                                               Iterable<WdEntity<?>>, Serializable {
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

  /** Ensure no method supplied. Throws exception if non-null
   *
   * @param operation HTTP operation - PUT etc
   */
  public abstract void assertNoMethod(String operation);

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
  public abstract Collection<?> getComponents();

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
   * @param mt method index
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
   */
  public abstract CalDAVEvent<?> getEvent();

  /**
   * @return CalDAVEvent - must be only one
   */
  public abstract CalDAVEvent<?> getOnlyEvent();

  /**
   * @return Iterator
   */
  public abstract Iterator<WdEntity<?>> iterator();

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
   * @param val method index
   * @return boolean
   */
  public abstract boolean validItipMethodType(int val);

  @Override
  public String toString() {
    return new ToString(this)
            .append("prodid", getProdid())
            .append("version", getVersion())
            .newLine()
            .append("method", String.valueOf(getMethod()))
            .append("methodType", getMethodType())
            .append("componentType", getComponentType())
            .toString();
  }
}
