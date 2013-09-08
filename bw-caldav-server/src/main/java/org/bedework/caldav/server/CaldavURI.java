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

import org.bedework.util.misc.Util;

import edu.rpi.cct.webdav.servlet.shared.WebdavException;
import edu.rpi.cmt.access.AccessPrincipal;

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
  boolean exists;

  /* For a resource or an entity, this is the containing collection
   */
  CalDAVCollection col;

  CalDAVResource resource;

  CalDAVEvent entity;

  AccessPrincipal principal;

  String entityName;
  String path;  // for principals

  boolean nameless; // Has no name yet
  boolean resourceUri; // entityname is resource

  /** Reference to a collection
   *
   * @param col
   * @param exists        true if the referenced object exists
   */
  CaldavURI(final CalDAVCollection col, final boolean exists) {
    init(col, null, null, null, exists, false);
  }

  /** Reference to a contained entity
   *
   * @param col
   * @param entity
   * @param entityName
   * @param exists        true if the referenced object exists
   * @param nameless      true if doesn't exist and we have no name yet
   */
  CaldavURI(final CalDAVCollection col, final CalDAVEvent entity, final String entityName,
            final boolean exists, final boolean nameless) {
    init(col, null, entity, entityName, exists, nameless);
  }

  /** Reference to a contained resource
   *
   * @param col
   * @param res
   * @param exists        true if the referenced object exists
   * @throws WebdavException
   */
  CaldavURI(final CalDAVCollection col,
            final CalDAVResource res,
            final boolean exists) throws WebdavException {
    init(col, res, null, res.getName(), exists, false);
    resourceUri = true;
  }

  /**
   * @param pi
   */
  CaldavURI(final AccessPrincipal pi) {
    principal = pi;
    exists = true;
    col = null;
    entityName = pi.getAccount();
    path = pi.getPrincipalRef();
  }

  private void init(final CalDAVCollection col, final CalDAVResource res,
                    final CalDAVEvent entity, final String entityName,
                    final boolean exists, final boolean nameless) {
    this.col = col;
    resource = res;
    this.entity = entity;
    this.entityName = entityName;
    this.exists = exists;
    this.nameless = nameless;
  }

  /**
   * @return boolean
   */
  public boolean getExists() {
    return exists;
  }

  /**
   * @return CalDAVCollection
   */
  public CalDAVCollection getCol() {
    return col;
  }

  /**
   * @return CalDAVResource
   */
  public CalDAVResource getResource() {
    return resource;
  }

  /**
   * @return CalDAVEvent
   */
  public CalDAVEvent getEntity() {
    return entity;
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
  public String getPath() {
    if (principal != null) {
      return path;
    }

    try {
      return col.getPath();
    } catch (WebdavException wde) {
      throw new RuntimeException(wde);
    }
  }

  /**
   * @return String
   * @throws WebdavException
   */
  public String getUri() throws WebdavException {
    if ((entityName == null) ||
        (principal != null)) {
      return getPath();
    }
    return Util.buildPath(false, getPath(), "/", entityName);
  }

  /**
   * @return true if this represents a resource
   */
  public boolean isResource() {
    return resourceUri;
  }

  /**
   * @return true if this represents a calendar
   */
  public boolean isCollection() {
    return !nameless && (entityName == null);
  }

  /**
   * @return AccessPrincipal or null if not a principal uri
   */
  public AccessPrincipal getPrincipal() {
    if (principal == null) {
      return null;
    }

    return principal;
  }

  /**
   * @param entityName
   * @return true if has same name
   */
  public boolean sameName(final String entityName) {
    if ((entityName == null) && (getEntityName() == null)) {
      return true;
    }

    if ((entityName == null) || (getEntityName() == null)) {
      return false;
    }

    return entityName.equals(getEntityName());
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder("CaldavURI{path=");

    try {
      sb.append(getPath());
    } catch (Throwable t) {
      sb.append("Exception: ");
      sb.append(t.getMessage());
    }
    sb.append(", entityName=");
    sb.append(String.valueOf(entityName));
    sb.append("}");

    return sb.toString();
  }

  @Override
  public int hashCode() {
    try {
      if (principal != null) {
        return  principal.hashCode();
      }

      int hc = entityName.hashCode();

      return (hc * 3) + getPath().hashCode();
    } catch (Throwable t) {
      throw new RuntimeException(t);
    }
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) {
      return true;
    }

    if (!(o instanceof CaldavURI)) {
      return false;
    }

    CaldavURI that = (CaldavURI)o;

    if (principal != null) {
      return  principal.equals(that.principal);
    }

    if (!getPath().equals(that.getPath())) {
      return false;
    }

    return sameName(that.entityName);
  }
}

