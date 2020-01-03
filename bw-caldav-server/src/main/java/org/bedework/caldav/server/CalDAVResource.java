/* ********************************************************************
    Licensed to Jasig under one or more contributor license
    agreements. See the NOTICE file distributed with this work
    for additional information regarding copyright ownership.
    Jasig licenses this file to you under the Apache License,
    Version 2.0 (the "License"); you may not use this file
    except in compliance with the License. You may obtain a
    copy of the License at:

    http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing,
    software distributed under the License is distributed on
    an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
    KIND, either express or implied. See the License for the
    specific language governing permissions and limitations
    under the License.
*/
package org.bedework.caldav.server;

import org.bedework.caldav.util.notifications.NotificationType.NotificationInfo;
import org.bedework.webdav.servlet.shared.WdEntity;
import org.bedework.webdav.servlet.shared.WebdavException;

import java.io.InputStream;

/** Class to represent a resource in CalDAV
 *
 * @author douglm
 *
 * @param <T>
 */
public abstract class CalDAVResource <T> extends WdEntity<T> {
  /** Constructor
   *
   */
  public CalDAVResource() {
    super();
  }

  /**
   * @return boolean true if this will be created as a result of a Put
   * @throws WebdavException
   */
  public abstract boolean isNew() throws WebdavException;

  /**
   * @return true if this represents a deleted resource.
   * @throws WebdavException
   */
  public abstract boolean getDeleted() throws WebdavException;

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

  /** This can be different from the content length
   *
   * @return long quota size
   * @throws WebdavException
   */
  public abstract long getQuotaSize() throws WebdavException;

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

  /**
   * @return null if this is not a notification
   * @throws WebdavException
   */
  public abstract NotificationInfo getNotificationType() throws WebdavException;
}
