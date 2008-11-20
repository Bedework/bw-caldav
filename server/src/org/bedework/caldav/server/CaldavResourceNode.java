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
import org.bedework.calfacade.BwResource;
import org.bedework.calfacade.BwUser;

import org.w3c.dom.Element;

import edu.rpi.cct.webdav.servlet.shared.WebdavException;
import edu.rpi.cmt.access.AccessPrincipal;
import edu.rpi.cmt.access.PrivilegeDefs;
import edu.rpi.cmt.access.Acl.CurrentAccess;
import edu.rpi.sss.util.DateTimeUtil;

import javax.xml.namespace.QName;

/** Class to represent a resource such as a file.
 *
 *   @author Mike Douglass   douglm rpi.edu
 */
public class CaldavResourceNode extends CaldavBwNode {
  private BwResource resource;

  private BwCalendar cal;

  private BwUser owner;

  private String entityName;

  private CurrentAccess currentAccess;

  /** Place holder for status
   *
   * @param sysi
   * @param status
   * @param uri
   * @param debug
   */
  public CaldavResourceNode(SysIntf sysi, int status, String uri, boolean debug) {
    super(true, sysi, debug);
    setStatus(status);
    this.uri = uri;
  }

  /** Constructor
   *
   * @param cdURI
   * @param sysi
   * @param debug
   * @throws WebdavException
   */
  public CaldavResourceNode(CaldavURI cdURI,
                             SysIntf sysi, boolean debug) throws WebdavException {
    super(cdURI, sysi, debug);

    resource = cdURI.getResource();
    cal = cdURI.getCal();
    collection = false;
    allowsGet = true;
    entityName = cdURI.getEntityName();

    if (resource != null) {
      exists = !resource.unsaved();
      resource.setPrevLastmod(resource.getLastmod());
      resource.setPrevSeq(resource.getPrevSeq());
    } else {
      exists = false;
    }
  }

  public void init(boolean content) throws WebdavException {
    if (!content) {
      return;
    }

    try {
      if ((resource == null) && exists) {
        if (entityName == null) {
          exists = false;
          return;
        }
      }
    } catch (Throwable t) {
      throw new WebdavException(t);
    }
  }

  /**
   * @return BwCalendar containing this entity
   */
  public BwCalendar getCalendar() throws WebdavException {
    BwCalendar curCal = cal;

    if ((curCal != null) &&
        (curCal.getCalType() == BwCalendar.calTypeAlias)) {
      curCal = cal.getAliasTarget();
      if (curCal == null) {
        getSysi().resolveAlias(cal);
        curCal = cal.getAliasTarget();
      }
    }

    return curCal;
  }

  /* (non-Javadoc)
   * @see edu.rpi.cct.webdav.servlet.shared.WebdavNsNode#getOwner()
   */
  public AccessPrincipal getOwner() throws WebdavException {
    if (owner == null) {
      if (resource == null) {
        return null;
      }

      owner = resource.getOwner();
    }

    if (owner != null) {
      return owner;
    }

    return null;
  }

  /* (non-Javadoc)
   * @see edu.rpi.cct.webdav.servlet.shared.WebdavNsNode#removeProperty(org.w3c.dom.Element)
   */
  public boolean removeProperty(Element val,
                                SetPropertyResult spr) throws WebdavException {
    warn("Unimplemented - removeProperty");

    return false;
  }

  /* (non-Javadoc)
   * @see edu.rpi.cct.webdav.servlet.shared.WebdavNsNode#setProperty(org.w3c.dom.Element)
   */
  public boolean setProperty(Element val,
                             SetPropertyResult spr) throws WebdavException {
    if (super.setProperty(val, spr)) {
      return true;
    }

    return false;
  }

  /* (non-Javadoc)
   * @see edu.rpi.cct.webdav.servlet.shared.WebdavNsNode#update()
   */
  public void update() throws WebdavException {
    if (resource != null) {
      getSysi().updateFile(resource, true);
    }
  }

  /**
   * @return String
   */
  public String getEntityName() {
    return entityName;
  }

  /* (non-Javadoc)
   * @see edu.rpi.cct.webdav.servlet.shared.WebdavNsNode#trailSlash()
   */
  public boolean trailSlash() {
    return false;
  }

  /* ====================================================================
   *                   Property methods
   * ==================================================================== */

  /* (non-Javadoc)
   * @see edu.rpi.cct.webdav.servlet.shared.WebdavNsNode#knownProperty(edu.rpi.sss.util.xml.QName)
   */
  public boolean knownProperty(QName tag) {
    // Not ours
    return super.knownProperty(tag);
  }

  /**
   * @param val
   */
  public void setResource(BwResource val) {
    resource = val;
  }

  /** Returns the resource object
   *
   * @return BwResource
   * @throws WebdavException
   */
  public BwResource getResource() throws WebdavException {
    init(true);

    return resource;
  }

  /* ====================================================================
   *                   Overridden property methods
   * ==================================================================== */

  public CurrentAccess getCurrentAccess() throws WebdavException {
    if (currentAccess != null) {
      return currentAccess;
    }

    if (resource == null) {
      return null;
    }

    try {
      currentAccess = getSysi().checkAccess(resource, PrivilegeDefs.privAny, true);
    } catch (Throwable t) {
      throw new WebdavException(t);
    }

    return currentAccess;
  }

  public String getEtagValue(boolean strong) throws WebdavException {
    init(true);

    if (resource == null) {
      return null;
    }

    return makeEtag(resource.getLastmod(), resource.getSeq(), strong);
  }

  /**
   * @param strong
   * @return etag before changes
   * @throws WebdavException
   */
  public String getPrevEtagValue(boolean strong) throws WebdavException {
    init(true);

    if (resource == null) {
      return null;
    }

    return makeEtag(resource.getPrevLastmod(), resource.getPrevSeq(), strong);
  }

  private String makeEtag(String lastmod, int seq, boolean strong) {
    StringBuilder val = new StringBuilder();
    if (!strong) {
      val.append("W");
    }

    val.append("\"");
    val.append(lastmod);
    val.append("-");
    val.append(seq);
    val.append("\"");

    return val.toString();
  }

  public String toString() {
    StringBuffer sb = new StringBuffer();

    sb.append("CaldavResourceNode{");
    sb.append("path=");
    sb.append(getPath());
    sb.append(", entityName=");
    sb.append(String.valueOf(entityName));
    sb.append("}");

    return sb.toString();
  }

  /* ====================================================================
   *                   Required webdav properties
   * ==================================================================== */

  /* (non-Javadoc)
   * @see edu.rpi.cct.webdav.servlet.shared.WebdavNsNode#getContentBinary()
   */
  public boolean getContentBinary() throws WebdavException {
    return true;
  }

  /* (non-Javadoc)
   * @see edu.rpi.cct.webdav.servlet.shared.WebdavNsNode#getContentString()
   */
  public String getContentString() throws WebdavException {
    init(true);
    throw new WebdavException("binary content");
  }

  /* (non-Javadoc)
   * @see edu.rpi.cct.webdav.servlet.shared.WebdavNsNode#getBinaryContent()
   */
  public byte[] getBinaryContent() throws WebdavException {
    init(true);

    if ((resource == null) || (resource.getContent() == null)) {
      return null;
    }

    return resource.getContent().getValue();
  }

  /* (non-Javadoc)
   * @see edu.rpi.cct.webdav.servlet.shared.WebdavNsNode#getContentLang()
   */
  public String getContentLang() throws WebdavException {
    return "en";
  }

  /* (non-Javadoc)
   * @see edu.rpi.cct.webdav.servlet.shared.WebdavNsNode#getContentLen()
   */
  public int getContentLen() throws WebdavException {
    init(true);

    if (resource == null) {
      return 0;
    }

    return resource.getContentLength();
  }

  /* (non-Javadoc)
   * @see edu.rpi.cct.webdav.servlet.shared.WebdavNsNode#getContentType()
   */
  public String getContentType() throws WebdavException {
    if (resource == null) {
      return null;
    }

    return resource.getContentType();
  }

  /* (non-Javadoc)
   * @see edu.rpi.cct.webdav.servlet.shared.WebdavNsNode#getCreDate()
   */
  public String getCreDate() throws WebdavException {
    init(false);

    if (resource == null) {
      return null;
    }

    return resource.getCreated();
  }

  /* (non-Javadoc)
   * @see edu.rpi.cct.webdav.servlet.shared.WebdavNsNode#getDisplayname()
   */
  public String getDisplayname() throws WebdavException {
    return getEntityName();
  }

  /* (non-Javadoc)
   * @see edu.rpi.cct.webdav.servlet.shared.WebdavNsNode#getLastmodDate()
   */
  public String getLastmodDate() throws WebdavException {
    init(false);

    if (resource == null) {
      return null;
    }

    try {
      return DateTimeUtil.fromISODateTimeUTCtoRfc822(resource.getLastmod());
    } catch (Throwable t) {
      throw new WebdavException(t);
    }
  }
}
