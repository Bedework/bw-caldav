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
import org.bedework.util.misc.ToString;
import org.bedework.util.misc.Util;
import org.bedework.util.xml.XmlEmit;
import org.bedework.util.xml.XmlEmit.NameSpace;
import org.bedework.util.xml.tagdefs.AppleServerTags;
import org.bedework.util.xml.tagdefs.BedeworkServerTags;
import org.bedework.util.xml.tagdefs.CaldavDefs;
import org.bedework.util.xml.tagdefs.WebdavTags;

import org.w3c.dom.Document;

import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

import javax.xml.namespace.QName;

/** Class to represent notifications as defined by Apple.
 *
 * @author Mike Douglass douglm
 */
public class NotificationType {
  private ProcessorsType processors;

  private String dtstamp;

  private BaseNotificationType notification;

  private Document parsed;

  /**
   * @param processors processor information
   */
  public void setProcessors(final ProcessorsType processors) {
    this.processors = processors;
  }

  /**
   * @return any processor information
   */
  public ProcessorsType getProcessors() {
    return processors;
  }

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
   *
   * @param val the parsed form of the XML
   */
  public void setParsed(final Document val) {
    parsed = val;
  }

  /**
   *
   * @return the parsed form of the XML
   */
  public Document getParsed() {
    return parsed;
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

  /**
   * @param val the name of the containing resource
   */
  public void setName(final String val) {
    if (val == null) {
      return;
    }

    final BaseNotificationType bn = getNotification();

    final String prefix = bn.getElementName().getLocalPart();

    if (val.length() < prefix.length()) {
      return;
    }

    bn.setName(val.substring(prefix.length()));
  }

  /**
   * @return an appropriate name for this object
   */
  public String getName() {
    final BaseNotificationType bn = getNotification();

    return bn.getElementName().getLocalPart() + bn.getName();
  }

  /* ====================================================================
   *                   Convenience methods
   * ==================================================================== */

  /**
   * @return an encoded content type - used internally
   */
  public String getContentType() {
    final StringBuilder sb = new StringBuilder("notification;type=");

    final QName qn = getNotification().getElementName();

    sb.append(qn);

    final List<AttributeType> attrs = getNotification().getElementAttributes();

    if (!Util.isEmpty(attrs)) {
      for (final AttributeType attr: attrs) {
        sb.append(";noteattr_");
        sb.append(attr.getName());
        sb.append("=");
        sb.append(attr.getValue());
      }
    }

    return sb.toString();
  }

  /**
   * @param val content type to decode and test
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
   * @param val to decode
   * @return decoded content type
   */
  public static NotificationInfo fromContentType(final String val) {
    if (val == null) {
      return null;
    }

    if (!isNotificationContentType(val)) {
      return null;
    }

    final String[] parts = val.split(";");

    if ((parts.length < 2) || !parts[1].startsWith("type=")) {
      return null;
    }

    final NotificationInfo ni = new NotificationInfo();

    ni.type = QName.valueOf(parts[1].substring(5));

    for (int i = 2; i < parts.length; i++) {
      if (!parts[i].startsWith("noteattr_")) {
        continue;
      }

      if (ni.attrs == null) {
        ni.attrs = new ArrayList<>();
      }

      final int pos = parts[i].indexOf("=");
      ni.attrs.add(new AttributeType(parts[i].substring(9, pos),
                                     parts[i].substring(pos + 1)));
    }

    return ni;
  }

  /**
   * @return XML version of notification without bedework elements
   */
  public String toXml() {
    return toXml(false);
  }

  /**
   * @param withBedeworkElements true if we should emit any extra elements.
   * @return XML version of notification
   */
  public String toXml(final boolean withBedeworkElements) {
    final StringWriter str = new StringWriter();
    final XmlEmit xml = new XmlEmit();

    if (withBedeworkElements) {
      xml.setProperty("withBedeworkElements", "true");
    }

    try {
      xml.addNs(new NameSpace(WebdavTags.namespace, "DAV"), false);
      xml.addNs(new NameSpace(CaldavDefs.caldavNamespace, "C"),
                false);
      xml.addNs(new NameSpace(AppleServerTags.appleCaldavNamespace,
                              "CSS"), false);
      xml.addNs(new NameSpace(
                        BedeworkServerTags.bedeworkCaldavNamespace, "BW"),
                false);
      xml.addNs(new NameSpace(
                        BedeworkServerTags.bedeworkSystemNamespace, "BSS"),
                false);

      xml.startEmit(str);
      toXml(xml);

      return str.toString();
    } catch (final Throwable t) {
      throw new RuntimeException(t);
    }
  }

  /**
   * @param xml emitter
   */
  public void toXml(final XmlEmit xml) {
    xml.openTag(AppleServerTags.notification);

    if (Boolean.parseBoolean(xml.getProperty("withBedeworkElements")) &&
            (getProcessors() != null)) {
      getProcessors().toXml(xml);
    }

    xml.property(AppleServerTags.dtstamp, getDtstamp());
    getNotification().toXml(xml);
    xml.closeTag(AppleServerTags.notification);
  }

  /** Add our stuff to the StringBuffer
   *
   * @param ts for output
   */
  protected void toStringSegment(final ToString ts) {
    ts.append("dtstamp", getDtstamp());
    ts.append("notification", getNotification().toString());
  }

  @Override
  public String toString() {
    final ToString ts = new ToString(this);

    toStringSegment(ts);

    return ts.toString();
  }
}
