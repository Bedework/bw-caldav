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
package org.bedework.caldav.util.notifications.eventreg;

import org.bedework.util.xml.XmlEmit;
import org.bedework.util.xml.tagdefs.BedeworkServerTags;

import javax.xml.namespace.QName;

/** Public events registration cancel message.
 *
 * @author Mike Douglass douglm
 */
public class EventregRegisteredNotificationType
        extends EventregBaseNotificationType {
  private int numTickets;

  @Override
  public QName getElementName() {
    return BedeworkServerTags.eventregRegistered;
  }

  /**
   * @param val the number of tickets
   */
  public void setNumTickets(final int val) {
    numTickets = val;
  }

  /**
   * @return the number of tickets
   */
  public int getNumTickets() {
    return numTickets;
  }

  protected void bodyToXml(final XmlEmit xml) throws Throwable {
    /* base notification fields */
    super.bodyToXml(xml);

    xml.property(BedeworkServerTags.eventregNumTickets,
                 String.valueOf(getNumTickets()));
  }

  @Override
  public void toXml(final XmlEmit xml) throws Throwable {
    xml.openTag(getElementName());

    bodyToXml(xml);

    xml.closeTag(getElementName());
  }
}
