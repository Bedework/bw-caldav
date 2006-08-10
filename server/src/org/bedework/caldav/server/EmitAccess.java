/*
 Copyright (c) 2000-2005 University of Washington.  All rights reserved.

 Redistribution and use of this distribution in source and binary forms,
 with or without modification, are permitted provided that:

   The above copyright notice and this permission notice appear in
   all copies and supporting documentation;

   The name, identifiers, and trademarks of the University of Washington
   are not used in advertising or publicity without the express prior
   written permission of the University of Washington;

   Recipients acknowledge that this distribution is made available as a
   research courtesy, "as is", potentially with defects, without
   any obligation on the part of the University of Washington to
   provide support, services, or repair;

   THE UNIVERSITY OF WASHINGTON DISCLAIMS ALL WARRANTIES, EXPRESS OR
   IMPLIED, WITH REGARD TO THIS SOFTWARE, INCLUDING WITHOUT LIMITATION
   ALL IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A
   PARTICULAR PURPOSE, AND IN NO EVENT SHALL THE UNIVERSITY OF
   WASHINGTON BE LIABLE FOR ANY SPECIAL, INDIRECT OR CONSEQUENTIAL
   DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR
   PROFITS, WHETHER IN AN ACTION OF CONTRACT, TORT (INCLUDING
   NEGLIGENCE) OR STRICT LIABILITY, ARISING OUT OF OR IN CONNECTION WITH
   THE USE OR PERFORMANCE OF THIS SOFTWARE.
 */
/* **********************************************************************
    Copyright 2005 Rensselaer Polytechnic Institute. All worldwide rights reserved.

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
package org.bedework.caldav.server;

import org.bedework.davdefs.CaldavTags;
import org.bedework.davdefs.WebdavTags;

import edu.rpi.cmt.access.AccessXmlUtil;
import edu.rpi.sss.util.xml.QName;
import edu.rpi.sss.util.xml.XmlEmit;

/**
 * @author douglm
 *
 */
public class EmitAccess extends AccessXmlUtil {
  private String namespacePrefix;

  /** xml rpivilege tags */
  private static final QName[] privTags = {
    WebdavTags.all,              // privAll = 0;
    WebdavTags.read,             // privRead = 1;
    WebdavTags.readAcl,          // privReadAcl = 2;
    WebdavTags.readCurrentUserPrivilegeSet,  // privReadCurrentUserPrivilegeSet = 3;
    CaldavTags.readFreeBusy,     // privReadFreeBusy = 4;
    WebdavTags.write,            // privWrite = 5;
    WebdavTags.writeAcl,         // privWriteAcl = 6;
    WebdavTags.writeProperties,  // privWriteProperties = 7;
    WebdavTags.writeContent,     // privWriteContent = 8;
    WebdavTags.bind,             // privBind = 9;

    CaldavTags.schedule,         // privSchedule = 10;
    CaldavTags.scheduleRequest,  // privScheduleRequest = 11;
    CaldavTags.scheduleReply,    // privScheduleReply = 12;
    CaldavTags.scheduleFreeBusy, // privScheduleFreeBusy = 13;

    WebdavTags.unbind,           // privUnbind = 14;
    WebdavTags.unlock,           // privUnlock = 15;
    null                         // privNone = 16;
  };

  /** Acls use tags in the webdav and caldav namespace. For use over caldav
   * we shoud supply the uris. Otherwise a null namespace will be used.
   *
   * @param namespacePrefix String prefix
   * @param xml   XmlEmit
   */
  public EmitAccess(String namespacePrefix, XmlEmit xml) {
    super(privTags, new WebdavTags(), xml);

    this.namespacePrefix = namespacePrefix;
  }

  /** Override this to construct urls from the parameter
   *
   * @param who String
   * @return String href
   */
  public String makeUserHref(String who) {
    return namespacePrefix + "/principals/users/" + who;
  }

  /** Override this to construct urls from the parameter
   *
   * @param who String
   * @return String href
   */
  public String makeGroupHref(String who) {
    return namespacePrefix + "/principals/groups/" + who;
  }

  /**
   * @return QName[]
   */
  public QName[] getPrivTags() {
    return privTags;
  }
}
