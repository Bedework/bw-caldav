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

import edu.rpi.cmt.access.AccessPrincipal;

import java.io.Serializable;

/**
 * @author douglm
 *
 */
public class CalPrincipalInfo implements Serializable {
  /** As supplied
   */
  public AccessPrincipal principal;

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

  private long quota;

  /**
   * @param principal
   * @param userHomePath
   * @param defaultCalendarPath
   * @param inboxPath
   * @param outboxPath
   */
  public CalPrincipalInfo(final AccessPrincipal principal,
                          final String userHomePath,
                          final String defaultCalendarPath, final String inboxPath,
                          final String outboxPath,
                          final long quota) {
    this.principal = principal;
    this.userHomePath = userHomePath;
    this.defaultCalendarPath = defaultCalendarPath;
    this.inboxPath = inboxPath;
    this.outboxPath = outboxPath;
    this.quota = quota;
  }

  /**
   * @return long
   */
  public long getQuota() {
    return quota;
  }
}
