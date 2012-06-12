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

import edu.rpi.sss.util.xml.XmlEmit;

import java.io.StringWriter;

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
   * @param xml
   * @throws Throwable
   */
  public abstract void toXml(final XmlEmit xml) throws Throwable;

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
}
