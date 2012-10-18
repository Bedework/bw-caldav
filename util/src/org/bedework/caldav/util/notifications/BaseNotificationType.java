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

import edu.rpi.cct.webdav.servlet.shared.UrlPrefixer;
import edu.rpi.cct.webdav.servlet.shared.UrlUnprefixer;
import edu.rpi.sss.util.xml.XmlEmit;
import edu.rpi.sss.util.xml.XmlEmit.NameSpace;
import edu.rpi.sss.util.xml.tagdefs.AppleServerTags;
import edu.rpi.sss.util.xml.tagdefs.CaldavDefs;
import edu.rpi.sss.util.xml.tagdefs.WebdavTags;

import java.io.StringWriter;
import java.util.List;

import javax.xml.namespace.QName;

/** Class to represent notification types as defined by Apple. Subtypes of this
 * appear inside a notification message
 *
 * @author Mike Douglass douglm
 */
public abstract class BaseNotificationType {
  /**
   * @return a name to identify the type of notification
   */
  public abstract QName getElementName();

  /**
   * @return a String to use as a resource name
   */
  public abstract String getName();

  /**
   */
  public interface AttributeType {
    /**
     * @return the name
     */
    String getName();
    /**
     * @return the value
     */
    String getValue();
  }

  /**
   * @return null or a list of attributes attached to the base notification.
   */
  public abstract List<AttributeType> getElementAttributes();

  /**
   * @param xml
   * @throws Throwable
   */
  /**
   * @param xml
   * @throws Throwable
   */
  public abstract void toXml(final XmlEmit xml) throws Throwable;

  /** Called before we send it out via caldav
   *
   * @param prefixer
   * @throws Throwable
   */
  public abstract void prefixHrefs(UrlPrefixer prefixer) throws Throwable;

  /** Called after we obtain it via caldav
   *
   * @param unprefixer
   * @throws Throwable
   */
  public abstract void unprefixHrefs(UrlUnprefixer unprefixer) throws Throwable;

  /**
   * @return XML version of notification
   * @throws Throwable
   */
  public String toXml() throws Throwable {
    StringWriter str = new StringWriter();
    XmlEmit xml = new XmlEmit();

    xml.addNs(new NameSpace(WebdavTags.namespace, "DAV"), false);
    xml.addNs(new NameSpace(CaldavDefs.caldavNamespace, "C"), false);
    xml.addNs(new NameSpace(AppleServerTags.appleCaldavNamespace, "CSS"), false);

    xml.startEmit(str);
    toXml(xml);

    return str.toString();
  }
}
