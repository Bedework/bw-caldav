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

import org.bedework.access.AccessPrincipal;
import org.bedework.access.CurrentAccess;
import org.bedework.access.PrivilegeDefs;
import org.bedework.caldav.server.sysinterface.SysIntf;
import org.bedework.caldav.util.notifications.BaseNotificationType.AttributeType;
import org.bedework.caldav.util.notifications.NotificationType.NotificationInfo;
import org.bedework.util.misc.ToString;
import org.bedework.util.misc.Util;
import org.bedework.util.timezones.DateTimeUtil;
import org.bedework.util.xml.XmlEmit;
import org.bedework.util.xml.tagdefs.AppleServerTags;
import org.bedework.webdav.servlet.shared.WebdavException;
import org.bedework.webdav.servlet.shared.WebdavNsIntf;

import org.w3c.dom.Element;

import java.io.InputStream;
import java.io.Writer;
import java.util.HashMap;

import javax.xml.namespace.QName;

/** Class to represent a resource such as a file.
 *
 *   @author Mike Douglass   douglm rpi.edu
 */
public class CaldavResourceNode extends CaldavBwNode {
  private CalDAVResource<?> resource;

  private AccessPrincipal owner;

  private String entityName;

  private CurrentAccess currentAccess;

  private final static HashMap<QName, PropertyTagEntry> propertyNames =
          new HashMap<>();

  static {
    addPropEntry(propertyNames, AppleServerTags.notificationtype);
  }

  /** Place holder for status
   *
   * @param sysi system interface
   * @param status from exception
   * @param uri of resource
   */
  public CaldavResourceNode(final SysIntf sysi,
                            final int status,
                            final String uri) {
    super(true, sysi, uri);
    setStatus(status);
  }

  /** Constructor
   *
   * @param cdURI referencing resource
   * @param sysi system interface
   */
  public CaldavResourceNode(final CaldavURI cdURI,
                             final SysIntf sysi) {
    super(cdURI, sysi);

    resource = cdURI.getResource();
    col = cdURI.getCol();
    collection = false;
    allowsGet = true;
    entityName = cdURI.getEntityName();

    exists = cdURI.getExists();

    /*
    if (resource != null) {
      exists = !resource.isNew();
      //resource.setPrevLastmod(resource.getLastmod());
      //resource.setPrevSeq(resource.getPrevSeq());
    } else {
      exists = false;
    }*/
  }

  /**
   * @param resource we represent
   * @param sysi system interface
   */
  public CaldavResourceNode(final CalDAVResource<?> resource,
                            final SysIntf sysi) {
    super(sysi, resource.getParentPath(), true, resource.getPath());

    allowsGet = false;

    this.resource = resource;
    exists = true;
  }

  @Override
  public void init(final boolean content) {
    if (!content) {
      return;
    }

    if ((resource == null) && exists) {
      if (entityName == null) {
        exists = false;
      }
    }
  }

  @Override
  public AccessPrincipal getOwner() {
    if (owner == null) {
      if (resource == null) {
        return null;
      }

      owner = resource.getOwner();
    }

    return owner;
  }

  @Override
  public boolean removeProperty(final Element val,
                                final SetPropertyResult spr) {
    warn("Unimplemented - removeProperty");

    return false;
  }

  @Override
  public boolean setProperty(final Element val,
                             final SetPropertyResult spr) {
    if (super.setProperty(val, spr)) {
      return true;
    }

    return false;
  }

  @Override
  public void update() {
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

  @Override
  public boolean trailSlash() {
    return false;
  }

  /* ====================================================================
   *                   Property methods
   * ==================================================================== */

  @Override
  public boolean knownProperty(final QName tag) {
    if (propertyNames.get(tag) != null) {
      return true;
    }

    // Not ours
    return super.knownProperty(tag);
  }

  @Override
  public boolean generatePropertyValue(final QName tag,
                                       final WebdavNsIntf intf,
                                       final boolean allProp) {
    final XmlEmit xml = intf.getXmlEmit();

    try {

      if (tag.equals(AppleServerTags.notificationtype)) {
        if (resource == null) {
          return false;
        }

        final NotificationInfo ni = resource.getNotificationType();
        if (ni == null) {
          return false;
        }

        xml.openTag(tag);

        xml.startTag(ni.type);

        if (!Util.isEmpty(ni.attrs)) {
          for (final AttributeType at: ni.attrs) {
            xml.attribute(at.getName(), at.getValue());
          }
        }
        xml.endEmptyTag();

        // XXX attrs
        xml.closeTag(tag);

        return true;
      }

      // Not known - try higher
      return super.generatePropertyValue(tag, intf, allProp);
    } catch (final WebdavException wde) {
      throw wde;
    } catch (final Throwable t) {
      throw new WebdavException(t);
    }
  }

  /**
   * @param val the resource
   */
  public void setResource(final CalDAVResource<?> val) {
    resource = val;
  }

  /** Returns the resource object
   *
   * @return CalDAVResource
   */
  public CalDAVResource<?> getResource() {
    init(true);

    return resource;
  }

  /* ====================================================================
   *                   Overridden property methods
   * ==================================================================== */

  @Override
  public CurrentAccess getCurrentAccess() {
    if (currentAccess != null) {
      return currentAccess;
    }

    if (resource == null) {
      return null;
    }

    try {
      currentAccess = getSysi().checkAccess(resource, PrivilegeDefs.privAny, true);
    } catch (final Throwable t) {
      throw new WebdavException(t);
    }

    return currentAccess;
  }

  @Override
  public String getEtagValue(final boolean strong) {
    init(true);

    if (resource == null) {
      return null;
    }

    final String val = resource.getEtag();

    if (strong) {
      return val;
    }

    return "W/" + val;
  }

  /* (non-Javadoc)
   * @see org.bedework.caldav.server.CaldavBwNode#getEtokenValue()
   */
  @Override
  public String getEtokenValue() {
    return concatEtoken(getEtagValue(true), "");
  }

  /* *
   * @param strong
   * @return etag before changes
   * /
  public String getPrevEtagValue(boolean strong) {
    init(true);

    if (resource == null) {
      return null;
    }

    return makeEtag(resource.getPrevLastmod(), resource.getPrevSeq(), strong);
  }*/

  @Override
  public String toString() {
    return new ToString(this)
            .append("path", getPath())
            .append("entityName", String.valueOf(entityName))
            .toString();
  }

  /* ==============================================================
   *                   Required webdav properties
   * ============================================================== */

  @Override
  public String writeContent(final XmlEmit xml,
                             final Writer wtr,
                             final String contentType) {
    return null;
  }

  @Override
  public boolean getContentBinary() {
    return true;
  }

  @Override
  public InputStream getContentStream() {
    return resource.getBinaryContent();
  }

  @Override
  public String getContentString(final String contentType) {
    init(true);
    throw new WebdavException("binary content");
  }

  @Override
  public String getContentLang() {
    return "en";
  }

  @Override
  public long getContentLen() {
    init(true);

    if (resource == null) {
      return 0;
    }

    return resource.getContentLen();
  }

  @Override
  public String getContentType() {
    if (resource == null) {
      return null;
    }

    return resource.getContentType();
  }

  @Override
  public String getCreDate() {
    init(false);

    if (resource == null) {
      return null;
    }

    return resource.getCreated();
  }

  @Override
  public String getDisplayname() {
    return getEntityName();
  }

  @Override
  public String getLastmodDate() {
    init(false);

    if (resource == null) {
      return null;
    }

    try {
      return DateTimeUtil.fromISODateTimeUTCtoRfc822(
              resource.getLastmod());
    } catch (final Throwable t) {
      throw new WebdavException(t);
    }
  }

  @Override
  public boolean allowsSyncReport() {
    return false;
  }

  @Override
  public boolean getDeleted() {
    if (resource == null) {
      return false;
    }

    return resource.getDeleted();
  }
}
