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
package org.bedework.caldav.util.notifications.parse;

import org.bedework.caldav.util.notifications.BaseNotificationType;

import org.w3c.dom.Element;

import javax.xml.namespace.QName;

/** Interface for parser for base notifications
 * (as defined by Apple).
 *
 * @author Mike Douglass douglm
 */
public interface BaseNotificationParser {
  /* Notifications we know about */

  /**
   * @return the element that wraps this notification type
   */
  QName getElement();

  /**
   * @param nd MUST be the notification xml element
   * @return populated ShareType object
   */
  BaseNotificationType parse(Element nd);
}
