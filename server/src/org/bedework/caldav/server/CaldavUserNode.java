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

import edu.rpi.cct.webdav.servlet.shared.WebdavIntfException;
import edu.rpi.cct.webdav.servlet.shared.WebdavNsIntf;
import edu.rpi.cmt.access.Acl.CurrentAccess;
import edu.rpi.sss.util.xml.QName;
import edu.rpi.sss.util.xml.XmlEmit;

import org.bedework.davdefs.CaldavDefs;
import org.bedework.davdefs.CaldavTags;
import org.w3c.dom.Element;

import java.util.ArrayList;
import java.util.Collection;

/** Class to represent a user in caldav.
 *
 *
 *   @author Mike Douglass   douglm@rpi.edu
 */
public class CaldavUserNode extends CaldavBwNode {
  private SysIntf.CalUserInfo ui;

  private final static Collection<PropertyTagEntry> propertyNames =
    new ArrayList<PropertyTagEntry>();

  static {
    addPropEntry(propertyNames, CaldavTags.calendarHomeSet, false);
    addPropEntry(propertyNames, CaldavTags.calendarHomeURL);
    addPropEntry(propertyNames, CaldavTags.scheduleInboxURL);
    addPropEntry(propertyNames, CaldavTags.scheduleOutboxURL);
  }

  /**
   * @param cdURI
   * @param sysi
   * @param ui    User Info
   * @param debug
   * @throws WebdavIntfException
   */
  public CaldavUserNode(CaldavURI cdURI, SysIntf sysi,
                        SysIntf.CalUserInfo ui, boolean debug) throws WebdavIntfException {
    super(cdURI, sysi, debug);
    this.ui = ui;
    name = cdURI.getEntityName();

    if (ui == null) {
      this.ui = sysi.getCalUserInfo(name);
    }
    userPrincipal = true;
  }

  public boolean removeProperty(Element val) throws WebdavIntfException {
    warn("Unimplemented - removeProperty");
    return false;
  }

  public boolean setProperty(Element val) throws WebdavIntfException {
    warn("Unimplemented - setProperty");
    return false;
  }

  public Collection getChildren() throws WebdavIntfException {
    return null;
  }

  /* ====================================================================
   *                   Abstract methods
   * ==================================================================== */

  public CurrentAccess getCurrentAccess() throws WebdavIntfException {
    return null;
  }

  public String getEtagValue() throws WebdavIntfException {
    return "1234567890";
  }

  /* ====================================================================
   *                   Property methods
   * ==================================================================== */

  /* (non-Javadoc)
   * @see edu.rpi.cct.webdav.servlet.shared.WebdavNsNode#generatePropertyValue(edu.rpi.sss.util.xml.QName, edu.rpi.cct.webdav.servlet.shared.WebdavNsIntf, boolean)
   */
  public boolean generatePropertyValue(QName tag,
                                       WebdavNsIntf intf,
                                       boolean allProp) throws WebdavIntfException {
    String ns = tag.getNamespaceURI();
    XmlEmit xml = intf.getXmlEmit();

    /* Deal with webdav properties */
    if (!ns.equals(CaldavDefs.caldavNamespace)) {
      // Not ours
      return super.generatePropertyValue(tag, intf, allProp);
    }

    try {
      if (tag.equals(CaldavTags.calendarHomeSet)) {
        xml.openTag(tag);
        generateHref(xml, ui.userHomePath);
        xml.closeTag(tag);

        return true;
      }

      if (tag.equals(CaldavTags.calendarHomeURL)) {
        xml.property(tag, ui.userHomePath);

        return true;
      }

      if (tag.equals(CaldavTags.scheduleInboxURL)) {
        xml.property(tag, ui.inboxPath);
        return true;
      }

      if (tag.equals(CaldavTags.scheduleOutboxURL)) {
        xml.property(tag, ui.outboxPath);
        return true;
      }

      // Not known
      return false;
    } catch (Throwable t) {
      throw new WebdavIntfException(t);
    }
  }

  /** Return a set of QName defining properties this node supports.
   *
   * @return
   * @throws WebdavIntfException
   */
  public Collection<PropertyTagEntry> getPropertyNames()throws WebdavIntfException {
    Collection<PropertyTagEntry> res = new ArrayList<PropertyTagEntry>();

    res.addAll(super.getPropertyNames());
    res.addAll(propertyNames);

    return res;
  }

  /* ====================================================================
   *                   Private methods
   * ==================================================================== */
}
