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
package org.bedework.caldav.bwserver;

import org.bedework.caldav.server.CalDAVResource;
import org.bedework.calfacade.BwResource;
import org.bedework.calfacade.BwResourceContent;

import edu.rpi.cct.webdav.servlet.shared.WdEntity;
import edu.rpi.cct.webdav.servlet.shared.WebdavException;
import edu.rpi.cmt.access.AccessPrincipal;

/**
 *
 * @author douglm
 *
 */
public class BwCalDAVResource extends CalDAVResource {
  private BwSysIntfImpl intf;

  private BwResource rsrc;

  /**
   * @param intf
   * @param rsrc
   * @throws WebdavException
   */
  BwCalDAVResource(BwSysIntfImpl intf, BwResource rsrc) throws WebdavException {
    this.intf = intf;
    this.rsrc = rsrc;
  }

  /* ====================================================================
   *                      Abstract method implementations
   * ==================================================================== */

  /* (non-Javadoc)
   * @see edu.rpi.cct.webdav.servlet.shared.WdEntity#isAlias()
   */
  public boolean isAlias() throws WebdavException {
    return false;
  }

  /* (non-Javadoc)
   * @see edu.rpi.cct.webdav.servlet.shared.WdEntity#getAliasTarget()
   */
  public WdEntity getAliasTarget() throws WebdavException {
    return this;
  }

  /* (non-Javadoc)
   * @see org.bedework.caldav.server.CalDAVEvent#getNewEvent()
   */
  public boolean isNew() throws WebdavException {
    return getRsrc().unsaved();
  }

  public void setBinaryContent(byte[] val) throws WebdavException {
    BwResource r = getRsrc();

    BwResourceContent rc = r.getContent();

    if (rc == null) {
      if (!isNew()) {
        intf.getFileContent(this);
        rc = r.getContent();
      }

      if (rc == null) {
        rc = new BwResourceContent();
        r.setContent(rc);
      }
    }

    rc.setValue(val);
    r.setContentLength(val.length);
  }

  /* (non-Javadoc)
   * @see org.bedework.caldav.server.CalDAVResource#getBinaryContent()
   */
  public byte[] getBinaryContent() throws WebdavException {
    if (rsrc == null) {
      return null;
    }

    if (rsrc.getContent() == null) {
      intf.getFileContent(this);
    }

    return rsrc.getContent().getValue();
  }

  /* (non-Javadoc)
   * @see org.bedework.caldav.server.CalDAVResource#getContentLen()
   */
  public int getContentLen() throws WebdavException {
    if (rsrc == null) {
      return 0;
    }

    return rsrc.getContentLength();
  }

  /* (non-Javadoc)
   * @see org.bedework.caldav.server.CalDAVResource#setContentType(java.lang.String)
   */
  public void setContentType(String val) throws WebdavException {
    getRsrc().setContentType(val);
  }

  /* (non-Javadoc)
   * @see org.bedework.caldav.server.CalDAVResource#getContentType()
   */
  public String getContentType() throws WebdavException {
    if (rsrc == null) {
      return null;
    }

    return rsrc.getContentType();
  }

  /* ====================================================================
   *                      Overrides
   * ==================================================================== */

  /* (non-Javadoc)
   * @see edu.rpi.cct.webdav.servlet.shared.WdCollection#setName(java.lang.String)
   */
  public void setName(String val) throws WebdavException {
    getRsrc().setName(val);
  }

  /* (non-Javadoc)
   * @see edu.rpi.cct.webdav.servlet.shared.WdCollection#getName()
   */
  public String getName() throws WebdavException {
    return getRsrc().getName();
  }

  /* (non-Javadoc)
   * @see edu.rpi.cct.webdav.servlet.shared.WdCollection#setDisplayName(java.lang.String)
   */
  public void setDisplayName(String val) throws WebdavException {
    // No display name
  }

  /* (non-Javadoc)
   * @see edu.rpi.cct.webdav.servlet.shared.WdEntity#getDisplayName()
   */
  public String getDisplayName() throws WebdavException {
    return getRsrc().getName();
  }

  /* (non-Javadoc)
   * @see edu.rpi.cct.webdav.servlet.shared.WdCollection#setPath(java.lang.String)
   */
  public void setPath(String val) throws WebdavException {
    // Not actually saved
  }

  /* (non-Javadoc)
   * @see edu.rpi.cct.webdav.servlet.shared.WdCollection#getPath()
   */
  public String getPath() throws WebdavException {
    return getRsrc().getColPath() + "/" + getRsrc().getName();
  }

  /* (non-Javadoc)
   * @see edu.rpi.cct.webdav.servlet.shared.WdCollection#setParentPath(java.lang.String)
   */
  public void setParentPath(String val) throws WebdavException {
    getRsrc().setColPath(val);
  }

  /* (non-Javadoc)
   * @see edu.rpi.cct.webdav.servlet.shared.WdCollection#getParentPath()
   */
  public String getParentPath() throws WebdavException {
    return getRsrc().getColPath();
  }

  /* (non-Javadoc)
   * @see edu.rpi.cct.webdav.servlet.shared.WdCollection#setOwner(edu.rpi.cmt.access.AccessPrincipal)
   */
  public void setOwner(AccessPrincipal val) throws WebdavException {
    getRsrc().setOwnerHref(val.getPrincipalRef());
  }

  /* (non-Javadoc)
   * @see edu.rpi.cct.webdav.servlet.shared.WdCollection#getOwner()
   */
  public AccessPrincipal getOwner() throws WebdavException {
    return intf.getPrincipal(getRsrc().getOwnerHref());
  }

  public void setCreated(String val) throws WebdavException {
    getRsrc().setCreated(val);
  }

  public String getCreated() throws WebdavException {
    return getRsrc().getCreated();
  }

  public void setLastmod(String val) throws WebdavException {
    getRsrc().setLastmod(val);
  }

  public String getLastmod() throws WebdavException {
    return getRsrc().getLastmod();
  }

  public void setSequence(int val) throws WebdavException {
  }

  public int getSequence() throws WebdavException {
    return 1;
  }

  /* (non-Javadoc)
   * @see edu.rpi.cct.webdav.servlet.shared.WdEntity#setDescription(java.lang.String)
   */
  public void setDescription(String val) throws WebdavException {
    // No description
  }

  /* (non-Javadoc)
   * @see edu.rpi.cct.webdav.servlet.shared.WdEntity#getDescription()
   */
  public String getDescription() throws WebdavException {
    return getRsrc().getName();
  }

  /* ====================================================================
   *                      Private methods
   * ==================================================================== */

  BwResource getRsrc() throws WebdavException {
    if (rsrc == null) {
      rsrc = new BwResource();
    }

    return rsrc;
  }
}
