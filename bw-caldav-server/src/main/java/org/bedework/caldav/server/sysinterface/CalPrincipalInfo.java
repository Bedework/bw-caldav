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
package org.bedework.caldav.server.sysinterface;

import org.bedework.access.AccessPrincipal;

import net.fortuna.ical4j.vcard.Property;
import net.fortuna.ical4j.vcard.Property.Id;
import net.fortuna.ical4j.vcard.VCard;
import net.fortuna.ical4j.vcard.property.N;

import java.io.Serializable;

/**
 * @author douglm
 *
 */
public class CalPrincipalInfo implements Serializable {
  /** As supplied
   */
  public AccessPrincipal principal;

  /** As supplied
   */
  public VCard card;

  private final String cardStr;

  /** Path to user home
   */
  public String userHomePath;

  /** Path to default calendar
   */
  public String defaultCalendarPath;

  /** Path to inbox. null for no scheduling permitted (or supported)
   */
  public String inboxPath;

  /** Path to outbox. null for no scheduling permitted (or supported)
   */
  public String outboxPath;

  /**
   *
   */
  public String notificationsPath;

  private final long quota;

  /**
   * @param principal this represents
   * @param userHomePath path
   * @param defaultCalendarPath path
   * @param inboxPath path
   * @param outboxPath path
   * @param notificationsPath path
   * @param quota allowed
   */
  public CalPrincipalInfo(final AccessPrincipal principal,
                          final VCard card,
                          final String cardStr,
                          final String userHomePath,
                          final String defaultCalendarPath, final String inboxPath,
                          final String outboxPath,
                          final String notificationsPath,
                          final long quota) {
    this.principal = principal;
    this.card = card;
    this.cardStr = cardStr;
    this.userHomePath = userHomePath;
    this.defaultCalendarPath = defaultCalendarPath;
    this.inboxPath = inboxPath;
    this.outboxPath = outboxPath;
    this.notificationsPath = notificationsPath;
    this.quota = quota;
  }

  /**
   * @return  associated vcard
   */
  public VCard getCard() {
    return card;
  }

  /**
   * @return  associated vcard as a string
   */
  public String getCardStr() {
    return cardStr;
  }

  /**
   * @return long
   */
  public long getQuota() {
    return quota;
  }

  /**
   * @return displayname of entoty represented by this info
   */
  public String getDisplayname() {
    if (card == null) {
      return null;
    }

    final String nn = propertyVal(Id.NICKNAME);

    if (nn != null) {
      return nn;
    }

    final N n = (N)card.getProperty(Id.N);

    if (n == null) {
      return null;
    }

    return notNull(n.getGivenName()) + " " + notNull(n.getFamilyName());
  }

  private String notNull(final String val) {
    if (val == null) {
      return "";
    }

    return val;
  }

  private String propertyVal(final Id id) {
    final Property p = card.getProperty(id);

    if (p == null) {
      return null;
    }

    return p.getValue();
  }
}
