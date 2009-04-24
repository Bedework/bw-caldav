/* **********************************************************************
    Copyright 2009 Rensselaer Polytechnic Institute. All worldwide rights reserved.

    Redistribution and use of this distribution in source and binary forms,
    with or without modification, are permitted provided that:
       The above copyright notice and this permission notice appear in all
        copies and supporting documentation;

        The name, identifiers, and trademarks of Rensselaer Polytechnic
        Institute are not used in advertising or publicity without the
        express prior written permission of Rensselaer Polytechnic Institute;

    DISCLAIMER: The software is distributed" AS IS" without any express or
    implied warranty, including but not limited to, any implied warranties
    of merchantability or fitness for a particular purpose or any warrant)'
    of non-infringement of any current or pending patent rights. The authors
    of the software make no representations about the suitability of this
    software for any particular purpose. The entire risk as to the quality
    and performance of the software is with the user. Should the software
    prove defective, the user assumes the cost of all necessary servicing,
    repair or correction. In particular, neither Rensselaer Polytechnic
    Institute, nor the authors of the software are liable for any indirect,
    special, consequential, or incidental damages related to the software,
    to the maximum extent the law permits.
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

  /**
   * @param principal
   * @param userHomePath
   * @param defaultCalendarPath
   * @param inboxPath
   * @param outboxPath
   */
  public CalPrincipalInfo(AccessPrincipal principal,
                          String userHomePath,
                          String defaultCalendarPath, String inboxPath,
                          String outboxPath) {
    this.principal = principal;
    this.userHomePath = userHomePath;
    this.defaultCalendarPath = defaultCalendarPath;
    this.inboxPath = inboxPath;
    this.outboxPath = outboxPath;
  }
}
