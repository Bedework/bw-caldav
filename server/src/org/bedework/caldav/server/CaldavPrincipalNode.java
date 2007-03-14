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

import edu.rpi.cct.webdav.servlet.shared.WebdavException;
import edu.rpi.cct.webdav.servlet.shared.WebdavNsIntf;
import edu.rpi.cmt.access.Acl.CurrentAccess;
import edu.rpi.sss.util.xml.QName;

import org.bedework.calfacade.BwCalendar;
import org.bedework.davdefs.CaldavDefs;
import org.w3c.dom.Element;

import java.util.Collection;

import javax.servlet.http.HttpServletResponse;

/** Class to represent a principal in caldav.
 *
 *
 *   @author Mike Douglass   douglm@rpi.edu
 */
public class CaldavPrincipalNode extends CaldavBwNode {
  private String displayName;

  /**
   * @param cdURI
   * @param sysi
   * @param debug
   * @throws WebdavException
   */
  public CaldavPrincipalNode(CaldavURI cdURI, SysIntf sysi,
                             boolean debug) throws WebdavException {
    super(cdURI, sysi, debug);
    displayName = cdURI.getEntityName();
  }

  /**
   * @return BwCalendar containing this entity
   */
  public BwCalendar getCalendar() {
    return null;
  }

  /* (non-Javadoc)
   * @see edu.rpi.cct.webdav.servlet.shared.WebdavNsNode#getOwner()
   */
  public String getOwner() throws WebdavException {
    return displayName;
  }

  /* (non-Javadoc)
   * @see edu.rpi.cct.webdav.servlet.shared.WebdavNsNode#removeProperty(org.w3c.dom.Element)
   */
  public SetPropertyResult removeProperty(Element val) throws WebdavException {
    warn("Unimplemented - removeProperty");
    SetPropertyResult spr = new SetPropertyResult(val);
    spr.status = HttpServletResponse.SC_NOT_IMPLEMENTED;
    spr.message = "Unimplemented - removeProperty";

    return spr;
  }

  /* (non-Javadoc)
   * @see edu.rpi.cct.webdav.servlet.shared.WebdavNsNode#setProperty(org.w3c.dom.Element)
   */
  public SetPropertyResult setProperty(Element val) throws WebdavException {
    SetPropertyResult spr = new SetPropertyResult(val);

    spr.status = HttpServletResponse.SC_NOT_IMPLEMENTED;
    spr.message = "Unimplemented - setProperty";

    return spr;
  }

  public Collection getChildren() throws WebdavException {
    return null;
  }

  /* ====================================================================
   *                   Abstract methods
   * ==================================================================== */

  public CurrentAccess getCurrentAccess() throws WebdavException {
    return null;
  }

  public String getEtagValue(boolean strong) throws WebdavException {
    String val = "1234567890";

    if (strong) {
      return "\"" + val + "\"";
    }

    return "W/\"" + val + "\"";
  }

  /* (non-Javadoc)
   * @see edu.rpi.cct.webdav.servlet.shared.WebdavNsNode#trailSlash()
   */
  public boolean trailSlash() {
    return true;
  }

  /* ====================================================================
   *                   Required webdav properties
   * ==================================================================== */

  /* (non-Javadoc)
   * @see edu.rpi.cct.webdav.servlet.shared.WebdavNsNode#getContentLang()
   */
  public String getContentLang() throws WebdavException {
    return null;
  }

  /* (non-Javadoc)
   * @see edu.rpi.cct.webdav.servlet.shared.WebdavNsNode#getContentLen()
   */
  public int getContentLen() throws WebdavException {
    return 0;
  }

  /* (non-Javadoc)
   * @see edu.rpi.cct.webdav.servlet.shared.WebdavNsNode#getContentType()
   */
  public String getContentType() throws WebdavException {
    return null;
  }

  /* (non-Javadoc)
   * @see edu.rpi.cct.webdav.servlet.shared.WebdavNsNode#getCreDate()
   */
  public String getCreDate() throws WebdavException {
    return null;
  }

  /* (non-Javadoc)
   * @see edu.rpi.cct.webdav.servlet.shared.WebdavNsNode#getDisplayname()
   */
  public String getDisplayname() throws WebdavException {
    return displayName;
  }

  /* (non-Javadoc)
   * @see edu.rpi.cct.webdav.servlet.shared.WebdavNsNode#getLastmodDate()
   */
  public String getLastmodDate() throws WebdavException {
    return null;
  }

  /* ====================================================================
   *                   Property methods
   * ==================================================================== */

  /* (non-Javadoc)
   * @see edu.rpi.cct.webdav.servlet.shared.WebdavNsNode#generatePropertyValue(edu.rpi.sss.util.xml.QName, edu.rpi.cct.webdav.servlet.shared.WebdavNsIntf, boolean)
   */
  public boolean generatePropertyValue(QName tag,
                                       WebdavNsIntf intf,
                                       boolean allProp) throws WebdavException {
    String ns = tag.getNamespaceURI();

    /* Deal with webdav properties */
    if (!ns.equals(CaldavDefs.caldavNamespace)) {
      // Not ours
      return super.generatePropertyValue(tag, intf, allProp);
    }

    try {
      // Not known - try higher
      return super.generatePropertyValue(tag, intf, allProp);
    } catch (Throwable t) {
      throw new WebdavException(t);
    }
  }

  /* ====================================================================
   *                   Private methods
   * ==================================================================== */
}
