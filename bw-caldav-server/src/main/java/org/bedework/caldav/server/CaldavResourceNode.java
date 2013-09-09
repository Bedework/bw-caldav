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
import org.bedework.access.Acl.CurrentAccess;
import org.bedework.access.PrivilegeDefs;
import org.bedework.caldav.server.sysinterface.SysIntf;
import org.bedework.caldav.util.notifications.BaseNotificationType.AttributeType;
import org.bedework.caldav.util.notifications.NotificationType.NotificationInfo;
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
 *   @author Mike Douglass   douglm bedework.edu
 */
public class CaldavResourceNode extends CaldavBwNode {
  private CalDAVResource resource;

  private AccessPrincipal owner;

  private String entityName;

  private CurrentAccess currentAccess;

  private final static HashMap<QName, PropertyTagEntry> propertyNames =
      new HashMap<QName, PropertyTagEntry>();

  static {
    addPropEntry(propertyNames, AppleServerTags.notificationtype);
  }

  /** Place holder for status
   *
   * @param sysi
   * @param status
   * @param uri
   */
  public CaldavResourceNode(final SysIntf sysi,
                            final int status,
                            final String uri) {
    super(true, sysi, uri);
    setStatus(status);
  }

  /** Constructor
   *
   * @param cdURI
   * @param sysi
   * @throws WebdavException
   */
  public CaldavResourceNode(final CaldavURI cdURI,
                             final SysIntf sysi) throws WebdavException {
    super(cdURI, sysi);

    resource = cdURI.getResource();
    col = cdURI.getCol();
    collection = false;
    allowsGet = true;
    entityName = cdURI.getEntityName();

    if (resource != null) {
      exists = !resource.isNew();
      //resource.setPrevLastmod(resource.getLastmod());
      //resource.setPrevSeq(resource.getPrevSeq());
    } else {
      exists = false;
    }
  }

  /**
   * @param resource
   * @param sysi
   * @throws WebdavException
   */
  public CaldavResourceNode(final CalDAVResource resource,
                            final SysIntf sysi) throws WebdavException {
    super(sysi, resource.getParentPath(), true, resource.getPath());

    allowsGet = false;

    this.resource = resource;
    exists = true;
  }

  @Override
  public void init(final boolean content) throws WebdavException {
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

  /* (non-Javadoc)
   * @see edu.bedework.cct.webdav.servlet.shared.WebdavNsNode#getOwner()
   */
  @Override
  public AccessPrincipal getOwner() throws WebdavException {
    if (owner == null) {
      if (resource == null) {
        return null;
      }

      owner = resource.getOwner();
    }

    return owner;
  }

  /* (non-Javadoc)
   * @see edu.bedework.cct.webdav.servlet.shared.WebdavNsNode#removeProperty(org.w3c.dom.Element)
   */
  @Override
  public boolean removeProperty(final Element val,
                                final SetPropertyResult spr) throws WebdavException {
    warn("Unimplemented - removeProperty");

    return false;
  }

  /* (non-Javadoc)
   * @see edu.bedework.cct.webdav.servlet.shared.WebdavNsNode#setProperty(org.w3c.dom.Element)
   */
  @Override
  public boolean setProperty(final Element val,
                             final SetPropertyResult spr) throws WebdavException {
    if (super.setProperty(val, spr)) {
      return true;
    }

    return false;
  }

  /* (non-Javadoc)
   * @see edu.bedework.cct.webdav.servlet.shared.WebdavNsNode#update()
   */
  @Override
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
   * @see edu.bedework.cct.webdav.servlet.shared.WebdavNsNode#trailSlash()
   */
  @Override
  public boolean trailSlash() {
    return false;
  }

  /* ====================================================================
   *                   Property methods
   * ==================================================================== */

  /* (non-Javadoc)
   * @see edu.bedework.cct.webdav.servlet.shared.WebdavNsNode#knownProperty(edu.bedework.sss.util.xml.QName)
   */
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
                                       final boolean allProp) throws WebdavException {
    XmlEmit xml = intf.getXmlEmit();

    try {

      if (tag.equals(AppleServerTags.notificationtype)) {
        if (resource == null) {
          return false;
        }

        NotificationInfo ni = resource.getNotificationType();
        if (ni == null) {
          return false;
        }

        xml.openTag(tag);

        xml.startTag(ni.type);

        if (!Util.isEmpty(ni.attrs)) {
          for (AttributeType at: ni.attrs) {
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
    } catch (WebdavException wde) {
      throw wde;
    } catch (Throwable t) {
      throw new WebdavException(t);
    }
  }

  /**
   * @param val
   */
  public void setResource(final CalDAVResource val) {
    resource = val;
  }

  /** Returns the resource object
   *
   * @return CalDAVResource
   * @throws WebdavException
   */
  public CalDAVResource getResource() throws WebdavException {
    init(true);

    return resource;
  }

  /* ====================================================================
   *                   Overridden property methods
   * ==================================================================== */

  @Override
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

  @Override
  public String getEtagValue(final boolean strong) throws WebdavException {
    init(true);

    if (resource == null) {
      return null;
    }

    String val = resource.getEtag();

    if (strong) {
      return val;
    }

    return "W/" + val;
  }

  /* (non-Javadoc)
   * @see org.bedework.caldav.server.CaldavBwNode#getEtokenValue()
   */
  @Override
  public String getEtokenValue() throws WebdavException {
    return concatEtoken(getEtagValue(true), "");
  }

  /* *
   * @param strong
   * @return etag before changes
   * @throws WebdavException
   * /
  public String getPrevEtagValue(boolean strong) throws WebdavException {
    init(true);

    if (resource == null) {
      return null;
    }

    return makeEtag(resource.getPrevLastmod(), resource.getPrevSeq(), strong);
  }*/

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();

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

  @Override
  public String writeContent(final XmlEmit xml,
                             final Writer wtr,
                             final String contentType) throws WebdavException {
    return null;
  }

  /* (non-Javadoc)
   * @see edu.bedework.cct.webdav.servlet.shared.WebdavNsNode#getContentBinary()
   */
  @Override
  public boolean getContentBinary() throws WebdavException {
    return true;
  }

  @Override
  public InputStream getContentStream() throws WebdavException {
    return resource.getBinaryContent();
  }

  /* (non-Javadoc)
   * @see edu.bedework.cct.webdav.servlet.shared.WebdavNsNode#getContentString()
   */
  @Override
  public String getContentString() throws WebdavException {
    init(true);
    throw new WebdavException("binary content");
  }

  /* (non-Javadoc)
   * @see edu.bedework.cct.webdav.servlet.shared.WebdavNsNode#getContentLang()
   */
  @Override
  public String getContentLang() throws WebdavException {
    return "en";
  }

  /* (non-Javadoc)
   * @see edu.bedework.cct.webdav.servlet.shared.WebdavNsNode#getContentLen()
   */
  @Override
  public long getContentLen() throws WebdavException {
    init(true);

    if (resource == null) {
      return 0;
    }

    return resource.getContentLen();
  }

  /* (non-Javadoc)
   * @see edu.bedework.cct.webdav.servlet.shared.WebdavNsNode#getContentType()
   */
  @Override
  public String getContentType() throws WebdavException {
    if (resource == null) {
      return null;
    }

    return resource.getContentType();
  }

  /* (non-Javadoc)
   * @see edu.bedework.cct.webdav.servlet.shared.WebdavNsNode#getCreDate()
   */
  @Override
  public String getCreDate() throws WebdavException {
    init(false);

    if (resource == null) {
      return null;
    }

    return resource.getCreated();
  }

  /* (non-Javadoc)
   * @see edu.bedework.cct.webdav.servlet.shared.WebdavNsNode#getDisplayname()
   */
  @Override
  public String getDisplayname() throws WebdavException {
    return getEntityName();
  }

  /* (non-Javadoc)
   * @see edu.bedework.cct.webdav.servlet.shared.WebdavNsNode#getLastmodDate()
   */
  @Override
  public String getLastmodDate() throws WebdavException {
    init(false);

    if (resource == null) {
      return null;
    }

    try {
      return DateTimeUtil.fromISODateTimeUTCtoRfc822(
              resource.getLastmod());
    } catch (Throwable t) {
      throw new WebdavException(t);
    }
  }

  @Override
  public boolean allowsSyncReport() throws WebdavException {
    return false;
  }

  @Override
  public boolean getDeleted() throws WebdavException {
    if (resource == null) {
      return false;
    }

    return resource.getDeleted();
  }
}
