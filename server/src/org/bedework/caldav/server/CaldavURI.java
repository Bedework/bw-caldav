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

import org.bedework.calfacade.BwCalendar;

/** We map uris onto an object which may be a calendar or an
 * entity contained within that calendar.
 *
 * <p>The response to webdav actions obviously depends upon the type of
 * referenced entity.
 *
 * <p>The server will have to determine whether a name represents a publicly
 * available calendar or a user and the access to a calendar will, of course,
 * depend upon the authentication state of the user.
 *
 *   @author Mike Douglass   douglm@rpi.edu
 */
public class CaldavURI {
  BwCalendar cal;

  String entityName;

  boolean userUri; // entityname is user

  boolean groupUri;// entityname is group

  CaldavURI(BwCalendar cal, String entityName) {
    init(cal, entityName);
  }

  CaldavURI(String entityName, boolean isUser) {
    cal = null;
    this.entityName = entityName;
    if (isUser) {
      userUri = true;
    } else {
      groupUri = true;
    }
  }

  private void init(BwCalendar cal, String entityName) {
    this.cal = cal;
    this.entityName = entityName;
  }

  /**
   * @return BwCalendar
   */
  public BwCalendar getCal() {
    return cal;
  }

  /**
   * @return String
   */
  public String getCalName() {
    return cal.getName();
  }

  /**
   * @return String
   */
  public String getEntityName() {
    return entityName;
  }

  /**
   * @return String
   */
  public String getOwner() {
    if (userUri || groupUri){
      return entityName;
    }
    return cal.getOwner().getAccount();
  }

  /**
   * @return String
   */
  public String getPath() {
    if (userUri) {
      return SysIntf.userPrincipalPrefix;
    }

    if (groupUri) {
      return SysIntf.groupPrincipalPrefix;
    }

    return cal.getPath();
  }

  /**
   * @return String
   */
  public String getUri() {
    if (entityName == null) {
      return getPath();
    }
    return getPath() + "/" + entityName;
  }

  /**
   * @return true if this represents a calendar
   */
  public boolean isCalendar() {
    return entityName == null;
  }

  /**
   * @return true if this represents a user
   */
  public boolean isUser() {
    return userUri;
  }

  /**
   * @return true if this represents a group
   */
  public boolean isGroup() {
    return groupUri;
  }

  /**
   * @param entityName
   * @return true if has same name
   */
  public boolean sameName(String entityName) {
    if ((entityName == null) && (getEntityName() == null)) {
      return true;
    }

    if ((entityName != null) || (getEntityName() != null)) {
      return false;
    }

    return entityName.equals(getEntityName());
  }

  public String toString() {
    StringBuffer sb = new StringBuffer("CaldavURI{path=");

    sb.append(getPath());
    sb.append(", entityName=");
    sb.append(String.valueOf(entityName));
    sb.append("}");

    return sb.toString();
  }

  public int hashCode() {
    int hc = entityName.hashCode();

    if (userUri) {
      return hc * 1;
    }

    if (groupUri) {
      return hc * 2;
    }

    return hc * 3 + cal.getPath().hashCode();
  }

  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }

    if (!(o instanceof CaldavURI)) {
      return false;
    }

    CaldavURI that = (CaldavURI)o;

    if (that.userUri != userUri) {
      return false;
    }

    if (that.groupUri != groupUri) {
      return false;
    }

    if (cal == null) {
      if (that.cal != null) {
        return false;
      }

      return true;
    }

    if (that.cal == null) {
      return false;
    }

    if (!cal.equals(that.cal)) {
      return false;
    }

    return sameName(that.entityName);
  }
}

