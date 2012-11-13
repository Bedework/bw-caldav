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

import org.bedework.caldav.util.notifications.BaseNotificationType.AttributeType;

import edu.rpi.sss.util.ToString;
import edu.rpi.sss.util.Util;
import edu.rpi.sss.util.xml.XmlEmit;
import edu.rpi.sss.util.xml.XmlEmit.NameSpace;
import edu.rpi.sss.util.xml.tagdefs.AppleServerTags;
import edu.rpi.sss.util.xml.tagdefs.BedeworkServerTags;
import edu.rpi.sss.util.xml.tagdefs.CaldavDefs;
import edu.rpi.sss.util.xml.tagdefs.WebdavTags;

import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

import javax.xml.namespace.QName;

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
   * @return an encoded content type - used internally
   */
  public String getContentType() {
    StringBuilder sb = new StringBuilder("notification;type=");

    QName qn = getNotification().getElementName();

    sb.append(qn);

    List<AttributeType> attrs = getNotification().getElementAttributes();

    if (!Util.isEmpty(attrs)) {
      for (AttributeType attr: attrs) {
        sb.append(";noteattr_");
        sb.append(attr.getName());
        sb.append("=");
        sb.append(attr.getValue());
      }
    }

    return sb.toString();
  }

  /**
   * @param val
   * @return true if this is one of ours
   */
  public static boolean isNotificationContentType(final String val) {
    return (val != null) && (val.startsWith("notification;type="));
  }

  /** Info from the encode content type.
   */
  public static class NotificationInfo {
    /** The QName for the enclosed notification */
    public QName type;

    /** The attributes for the enclosed notification */
    public List<AttributeType> attrs;
  }

  /**
   * @param val
   * @return decoded content type
   */
  public static NotificationInfo fromContentType(final String val) {
    if (val == null) {
      return null;
    }

    if (!isNotificationContentType(val)) {
      return null;
    }

    String[] parts = val.split(";");

    if ((parts.length < 2) || !parts[1].startsWith("type=")) {
      return null;
    }

    NotificationInfo ni = new NotificationInfo();

    ni.type = QName.valueOf(parts[1].substring(5));

    for (int i = 2; i < parts.length; i++) {
      if (!parts[i].startsWith("noteattr_")) {
        continue;
      }

      if (ni.attrs == null) {
        ni.attrs = new ArrayList<AttributeType>();
      }

      int pos = parts[i].indexOf("=");
      ni.attrs.add(new AttributeType(parts[i].substring(9, pos),
                                     parts[i].substring(pos + 1)));
    }

    return ni;
  }

  /**
   * @return XML version of notification without bedework elements
   * @throws Throwable
   */
  public String toXml() throws Throwable {
    return toXml(false);
  }

  /**
   * @param withBedeworkElements true if we should emit any extra elements.
   * @return XML version of notification
   * @throws Throwable
   */
  public String toXml(final boolean withBedeworkElements) throws Throwable {
    StringWriter str = new StringWriter();
    XmlEmit xml = new XmlEmit();

    if (withBedeworkElements) {
      xml.setProperty("withBedeworkElements", "true");
    }

    xml.addNs(new NameSpace(WebdavTags.namespace, "DAV"), false);
    xml.addNs(new NameSpace(CaldavDefs.caldavNamespace, "C"), false);
    xml.addNs(new NameSpace(AppleServerTags.appleCaldavNamespace, "CSS"), false);
    xml.addNs(new NameSpace(BedeworkServerTags.bedeworkCaldavNamespace, "BW"), false);

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
