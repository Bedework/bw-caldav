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
package org.bedework.caldav.util.notifications;

import edu.rpi.sss.util.ToString;
import edu.rpi.sss.util.xml.XmlEmit;
import edu.rpi.sss.util.xml.tagdefs.AppleServerTags;

import java.io.StringWriter;

/** Class to represent notifications as defined by Apple.
 *
 * @author Mike Douglass douglm
 */
public class NotificationType {
  private String dtstamp;

  private BaseNotificationType notification;

  /**
   * @param val the dtstamp
   */
  public void setDtstamp(final String val) {
    dtstamp = val;
  }

  /**
   * @return the dtstamp
   */
  public String getDtstamp() {
    return dtstamp;
  }

  /**
   * @param val the notification
   */
  public void setNotification(final BaseNotificationType val) {
    notification = val;
  }

  /**
   * @return the notification
   */
  public BaseNotificationType getNotification() {
    return notification;
  }

  /* ====================================================================
   *                   Convenience methods
   * ==================================================================== */

  /**
   * @return XML version of notification
   * @throws Throwable
   */
  public String toXml() throws Throwable {
    StringWriter str = new StringWriter();
    XmlEmit xml = new XmlEmit();

    xml.startEmit(str);
    toXml(xml);

    return str.toString();
  }

  /**
   * @param xml
   * @throws Throwable
   */
  public void toXml(final XmlEmit xml) throws Throwable {
    xml.openTag(AppleServerTags.notification);
    xml.property(AppleServerTags.dtstamp, getDtstamp());
    getNotification().toXml(xml);
    xml.closeTag(AppleServerTags.notification);
  }

  /** Add our stuff to the StringBuffer
   *
   * @param ts
   */
  protected void toStringSegment(final ToString ts) {
    ts.append("dtstamp", getDtstamp());
    ts.append("notification", getNotification().toString());
  }

  @Override
  public String toString() {
    ToString ts = new ToString(this);

    toStringSegment(ts);

    return ts.toString();
  }
}
