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
   */
  public abstract boolean isNew();

  /**
   * @return true if this represents a deleted resource.
   */
  public abstract boolean getDeleted();

  /** Set the value
   *
   *  @param  val   InputStream
   */
  public abstract void setBinaryContent(InputStream val);

  /** Return binary content
   *
   * @return InputStream       content.
   */
  public abstract InputStream getBinaryContent();

  /**
   * @return long content length
   */
  public abstract long getContentLen();

  /** This can be different from the content length
   *
   * @return long quota size
   */
  public abstract long getQuotaSize();

  /** Set the contentType - may be null for unknown
   *
   *  @param  val   String contentType
   */
  public abstract void setContentType(String val);

  /** A content type of null implies no content (or we don't know)
   *
   * @return String content type
   */
  public abstract String getContentType();

  /**
   * @return null if this is not a notification
   */
  public abstract NotificationInfo getNotificationType();
}
