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

import org.bedework.caldav.server.CalDAVCollection;
import org.bedework.calfacade.BwCalendar;

import edu.rpi.cct.webdav.servlet.shared.WdEntity;
import edu.rpi.cct.webdav.servlet.shared.WebdavException;
import edu.rpi.cmt.access.AccessPrincipal;

/**
 *
 * @author douglm
 *
 */
public class BwCalDAVCollection extends CalDAVCollection {
  private BwSysIntfImpl intf;

  private BwCalendar col;

  /**
   * @param intf
   * @param col
   * @throws WebdavException
   */
  BwCalDAVCollection(BwSysIntfImpl intf, BwCalendar col) throws WebdavException {
    this.intf = intf;
    this.col = col;
  }

  /* ====================================================================
   *                      Abstract method implementations
   * ==================================================================== */

  /* (non-Javadoc)
   * @see edu.rpi.cct.webdav.servlet.shared.WdEntity#isAlias()
   */
  public boolean isAlias() throws WebdavException {
    return getCol().getInternalAlias();
  }

  /* (non-Javadoc)
   * @see edu.rpi.cct.webdav.servlet.shared.WdEntity#getAliasTarget()
   */
  public WdEntity getAliasTarget() throws WebdavException {
    BwCalendar c = col.getAliasTarget();
    if (c == null) {
      return null;
    }

    return new BwCalDAVCollection(intf, c);
  }

  /* (non-Javadoc)
   * @see org.bedework.caldav.server.CalDAVCollection#setCalType(int)
   */
  public void setCalType(int val) throws WebdavException {
    getCol().setCalType(val);
  }

  public int getCalType() throws WebdavException {
    int calType = getCol().getCalType();

    if (calType == BwCalendar.calTypeFolder) {
      // Broken alias
      return CalDAVCollection.calTypeCollection;
    }

    if (calType == BwCalendar.calTypeCalendarCollection) {
      // Broken alias
      return CalDAVCollection.calTypeCalendarCollection;
    }

    if (calType == BwCalendar.calTypeInbox) {
      // Broken alias
      return CalDAVCollection.calTypeInbox;
    }

    if (calType == BwCalendar.calTypeOutbox) {
      // Broken alias
      return CalDAVCollection.calTypeOutbox;
    }

    return CalDAVCollection.calTypeUnknown;
  }

  public boolean freebusyAllowed() throws WebdavException {
    return getCol().getCollectionInfo().allowFreeBusy;
  }

  /* (non-Javadoc)
   * @see org.bedework.caldav.server.CalDAVCollection#entitiesAllowed()
   */
  public boolean entitiesAllowed() throws WebdavException {
    return getCol().getCollectionInfo().entitiesAllowed;
  }

  /* (non-Javadoc)
   * @see org.bedework.caldav.server.CalDAVCollection#setAffectsFreeBusy(boolean)
   */
  public void setAffectsFreeBusy(boolean val) throws WebdavException {
    getCol().setAffectsFreeBusy(val);
  }

  /* (non-Javadoc)
   * @see org.bedework.caldav.server.CalDAVCollection#getAffectsFreeBusy()
   */
  public boolean getAffectsFreeBusy() throws WebdavException {
    return getCol().getAffectsFreeBusy();
  }

  /* (non-Javadoc)
   * @see org.bedework.caldav.server.CalDAVCollection#setTimezone(java.lang.String)
   */
  public void setTimezone(String val) throws WebdavException {
    getCol().setTimezone(val);
  }

  /* (non-Javadoc)
   * @see org.bedework.caldav.server.CalDAVCollection#getTimezone()
   */
  public String getTimezone() throws WebdavException {
    return getCol().getTimezone();
  }

  /* (non-Javadoc)
   * @see org.bedework.caldav.server.CalDAVCollection#setColor(java.lang.String)
   */
  public void setColor(String val) throws WebdavException {
    getCol().setColor(val);
  }

  /* (non-Javadoc)
   * @see org.bedework.caldav.server.CalDAVCollection#getColor()
   */
  public String getColor() throws WebdavException {
    return getCol().getColor();
  }

  /* ====================================================================
   *                      Overrides
   * ==================================================================== */

  /* (non-Javadoc)
   * @see edu.rpi.cct.webdav.servlet.shared.WdCollection#setName(java.lang.String)
   */
  public void setName(String val) throws WebdavException {
    getCol().setName(val);
  }

  /* (non-Javadoc)
   * @see edu.rpi.cct.webdav.servlet.shared.WdCollection#getName()
   */
  public String getName() throws WebdavException {
    return getCol().getName();
  }

  /* (non-Javadoc)
   * @see edu.rpi.cct.webdav.servlet.shared.WdCollection#setDisplayName(java.lang.String)
   */
  public void setDisplayName(String val) throws WebdavException {
    getCol().setSummary(val);
  }

  /* (non-Javadoc)
   * @see edu.rpi.cct.webdav.servlet.shared.WdCollection#getDisplayName()
   */
  public String getDisplayName() throws WebdavException {
    return getCol().getSummary();
  }

  /* (non-Javadoc)
   * @see edu.rpi.cct.webdav.servlet.shared.WdCollection#setPath(java.lang.String)
   */
  public void setPath(String val) throws WebdavException {
    getCol().setPath(val);
  }

  /* (non-Javadoc)
   * @see edu.rpi.cct.webdav.servlet.shared.WdCollection#getPath()
   */
  public String getPath() throws WebdavException {
    return getCol().getPath();
  }

  /* (non-Javadoc)
   * @see edu.rpi.cct.webdav.servlet.shared.WdCollection#setParentPath(java.lang.String)
   */
  public void setParentPath(String val) throws WebdavException {
    getCol().setColPath(val);
  }

  /* (non-Javadoc)
   * @see edu.rpi.cct.webdav.servlet.shared.WdCollection#getParentPath()
   */
  public String getParentPath() throws WebdavException {
    return getCol().getColPath();
  }

  /* (non-Javadoc)
   * @see edu.rpi.cct.webdav.servlet.shared.WdCollection#setOwner(edu.rpi.cmt.access.AccessPrincipal)
   */
  public void setOwner(AccessPrincipal val) throws WebdavException {
    getCol().setOwnerHref(val.getPrincipalRef());
  }

  /* (non-Javadoc)
   * @see edu.rpi.cct.webdav.servlet.shared.WdCollection#getOwner()
   */
  public AccessPrincipal getOwner() throws WebdavException {
    return intf.getPrincipal(getCol().getOwnerHref());
  }

  public void setCreated(String val) throws WebdavException {
    getCol().setCreated(val);
  }

  public String getCreated() throws WebdavException {
    return getCol().getCreated();
  }

  public void setLastmod(String val) throws WebdavException {
    getCol().getLastmod().setTimestamp(val);
  }

  public String getLastmod() throws WebdavException {
    return getCol().getLastmod().getTimestamp();
  }

  public void setSequence(int val) throws WebdavException {
    getCol().getLastmod().setSequence(val);
  }

  public int getSequence() throws WebdavException {
    return getCol().getLastmod().getSequence();
  }

  /* (non-Javadoc)
   * @see edu.rpi.cct.webdav.servlet.shared.WdCollection#setDescription(java.lang.String)
   */
  public void setDescription(String val) throws WebdavException {
    getCol().setDescription(val);
  }

  /* (non-Javadoc)
   * @see edu.rpi.cct.webdav.servlet.shared.WdCollection#getDescription()
   */
  public String getDescription() throws WebdavException {
    return getCol().getDescription();
  }

  /* ====================================================================
   *                      Private methods
   * ==================================================================== */

  BwCalendar getCol() throws WebdavException {
    if (col == null) {
      col = new BwCalendar();
    }

    return col;
  }
}
