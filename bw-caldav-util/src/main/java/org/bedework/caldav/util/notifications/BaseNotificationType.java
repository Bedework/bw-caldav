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

import org.bedework.caldav.util.notifications.parse.Parser;
import org.bedework.util.xml.XmlEmit;
import org.bedework.util.xml.XmlEmit.NameSpace;
import org.bedework.util.xml.tagdefs.AppleServerTags;
import org.bedework.util.xml.tagdefs.BedeworkServerTags;
import org.bedework.util.xml.tagdefs.CaldavDefs;
import org.bedework.util.xml.tagdefs.WebdavTags;
import org.bedework.webdav.servlet.shared.UrlPrefixer;
import org.bedework.webdav.servlet.shared.UrlUnprefixer;

import java.io.StringWriter;
import java.util.List;

import javax.xml.namespace.QName;

/** Class to represent notification types as defined by Apple. Subtypes of this
 * appear inside a notification message
 *
 * @author Mike Douglass douglm
 */
public abstract class BaseNotificationType {
  private String name;

  /**
   * @return a name to identify the type of notification
   */
  public abstract QName getElementName();

  /**
   * @param val String used as (part of) a resource name
   */
  public void setName(final String val) {
    name = val;
  }

  /**
   * @return a String to use as (part of) a resource name
   */
  public String getName() {
    return name;
  }

  /** NOTE: this may not be a valid encoding value. Until we update the schema
   * this will have the encoded path of the resource for which we are getting a
   * notification. The resource name is too short to handle it at the moment.
   *
   * @param val encoding or path
   */
  public abstract void setEncoding(String val);

  /** NOTE: this may not be a valid encoding value. Until we update the schema
   * this will have the encoded path of the resource for which we are getting a
   * notification. The resource name is too short to handle it at the moment.
   *
   * @return value for the resource encoding or null.
   */
  public abstract String getEncoding();

  /**
   *
   */
    public record AttributeType(String name,
                                String value) {
    /**
     * @param name  attrname
     * @param value attrvalue
     */
    public AttributeType {
    }

      /**
       * @return the name
       */
      @Override
      public String name() {
        return name;
      }

      /**
       * @return the value
       */
      @Override
      public String value() {
        return value;
      }
    }

  /**
   * @return null or a list of attributes attached to the base notification.
   */
  public abstract List<AttributeType> getElementAttributes();

  /** Called before we send it out via caldav
   *
   * @param prefixer the prefixer
   */
  public abstract void prefixHrefs(UrlPrefixer prefixer);

  /** Called after we obtain it via caldav
   *
   * @param unprefixer the unprefixer
   */
  public abstract void unprefixHrefs(UrlUnprefixer unprefixer) ;

  /**
   * @return XML version of notification
   */
  public String toXml() {
    final StringWriter str = new StringWriter();
    final XmlEmit xml = new XmlEmit();

    xml.addNs(new NameSpace(WebdavTags.namespace, "DAV"), false);
    xml.addNs(new NameSpace(CaldavDefs.caldavNamespace, "C"), false);
    xml.addNs(new NameSpace(AppleServerTags.appleCaldavNamespace, "CSS"), false);
    xml.addNs(new NameSpace(BedeworkServerTags.bedeworkCaldavNamespace, "BSS"), false);
    xml.addNs(new NameSpace(BedeworkServerTags.bedeworkSystemNamespace, "BSYS"), false);

    xml.startEmit(str);
    toXml(xml);

    return str.toString();
  }

  /**
   * @param xml emitter
   */
  public void toXml(final XmlEmit xml) {
    if (Boolean.parseBoolean(xml.getProperty("withBedeworkElements")) &&
            (getName() != null)) {
      xml.property(BedeworkServerTags.name, getName());
    }
  }

  @Override
  public Object clone() {
    try {
      final String xml = toXml();
      final NotificationType note = Parser.fromXml(xml);
      
      return note.getNotification();
    } catch (final Throwable t) {
      throw new RuntimeException(t);
    }
  }
}
