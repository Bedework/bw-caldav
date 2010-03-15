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
package org.bedework.caldav.server;

import edu.rpi.cct.webdav.servlet.shared.WdEntity;
import edu.rpi.cct.webdav.servlet.shared.WebdavException;

import java.io.InputStream;

/** Class to represent a resource in CalDAV
 *
 * @author douglm
 *
 */
public abstract class CalDAVResource extends WdEntity {
  /** Constructor
   *
   * @throws WebdavException
   */
  public CalDAVResource() throws WebdavException {
    super();
  }

  /**
   * @return boolean true if this will be created as a result of a Put
   * @throws WebdavException
   */
  public abstract boolean isNew() throws WebdavException;

  /** Set the value
   *
   *  @param  val   InputStream
   * @throws WebdavException
   */
  public abstract void setBinaryContent(InputStream val) throws WebdavException;

  /** Return binary content
   *
   * @return InputStream       content.
   * @throws WebdavException
   */
  public abstract InputStream getBinaryContent() throws WebdavException;

  /**
   * @return long content length
   * @throws WebdavException
   */
  public abstract long getContentLen() throws WebdavException;

  /** Set the contentType - may be null for unknown
   *
   *  @param  val   String contentType
   * @throws WebdavException
   */
  public abstract void setContentType(String val) throws WebdavException;

  /** A content type of null implies no content (or we don't know)
   *
   * @return String content type
   * @throws WebdavException
   */
  public abstract String getContentType() throws WebdavException;
}
