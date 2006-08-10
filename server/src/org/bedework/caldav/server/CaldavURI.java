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

  CaldavURI(BwCalendar cal, String entityName) {
    init(cal, entityName);
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
    return cal.getOwner().getAccount();
  }

  /**
   * @return String
   */
  public String getPath() {
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
   * @param cal
   * @param entityName
   * @return true if pars represent same URI
   */
  public boolean sameURI(BwCalendar cal, String entityName) {
    if (!getPath().equals(cal.getPath())) {
      return false;
    }

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
}

