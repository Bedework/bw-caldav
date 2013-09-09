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

import org.bedework.caldav.server.sysinterface.CalPrincipalInfo;
import org.bedework.caldav.server.sysinterface.SysIntf;
import org.bedework.util.xml.XmlEmit;
import org.bedework.util.xml.tagdefs.AppleServerTags;
import org.bedework.util.xml.tagdefs.CaldavTags;
import org.bedework.util.xml.tagdefs.WebdavTags;
import org.bedework.webdav.servlet.shared.WebdavException;
import org.bedework.webdav.servlet.shared.WebdavNsIntf;
import org.bedework.webdav.servlet.shared.WebdavPrincipalNode;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;

import javax.xml.namespace.QName;

/** Class to represent a user in caldav.
 *
 *
 *   @author Mike Douglass   douglm  bedework.edu
 */
public class CaldavPrincipalNode extends WebdavPrincipalNode {
  private CalPrincipalInfo ui;

  /* for accessing calendars */
  private SysIntf sysi;

  private final static HashMap<QName, PropertyTagEntry> propertyNames =
    new HashMap<QName, PropertyTagEntry>();

  static {
    addPropEntry(propertyNames, CaldavTags.calendarHomeSet);
    addPropEntry(propertyNames, CaldavTags.calendarUserAddressSet);
    addPropEntry(propertyNames, CaldavTags.scheduleInboxURL);
    addPropEntry(propertyNames, CaldavTags.scheduleOutboxURL);
    addPropEntry(propertyNames, AppleServerTags.notificationURL);
  }

  /**
   * @param cdURI
   * @param sysi
   * @param ui
   * @param isUser
   * @throws WebdavException
   */
  public CaldavPrincipalNode(final CaldavURI cdURI, final SysIntf sysi,
                             final CalPrincipalInfo ui,
                             final boolean isUser) throws WebdavException {
    super(sysi.getUrlHandler(), cdURI.getPath(),
          cdURI.getPrincipal(),
          cdURI.isCollection(), cdURI.getUri());
    this.sysi = sysi;
    this.ui = ui;

    if (ui == null) {
      this.ui = sysi.getCalPrincipalInfo(cdURI.getPrincipal());
    }
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

  /* (non-Javadoc)
   * @see edu.bedework.cct.webdav.servlet.shared.WebdavNsNode#generatePropertyValue(edu.bedework.sss.util.xml.QName, edu.bedework.cct.webdav.servlet.shared.WebdavNsIntf, boolean)
   */
  @Override
  public boolean generatePropertyValue(final QName tag,
                                       final WebdavNsIntf intf,
                                       final boolean allProp) throws WebdavException {
    XmlEmit xml = intf.getXmlEmit();

    try {
      if (tag.equals(CaldavTags.calendarHomeSet)) {
        if (ui == null) {
          return false;
        }

        xml.openTag(tag);
        generateHref(xml, ui.userHomePath);
        xml.closeTag(tag);

        return true;
      }

      if (tag.equals(CaldavTags.calendarUserAddressSet)) {
        xml.openTag(tag);
        xml.property(WebdavTags.href, sysi.principalToCaladdr(getOwner()));
        xml.closeTag(tag);

        return true;
      }

      if (tag.equals(CaldavTags.scheduleInboxURL)) {
        if (ui == null) {
          return false;
        }

        xml.openTag(tag);
        generateHref(xml, ui.inboxPath);
        xml.closeTag(tag);

        return true;
      }

      if (tag.equals(CaldavTags.scheduleOutboxURL)) {
        if (ui == null) {
          return false;
        }

        xml.openTag(tag);
        generateHref(xml, ui.outboxPath);
        xml.closeTag(tag);

        return true;
      }

      if (tag.equals(AppleServerTags.notificationURL)) {
        if ((ui == null) || (ui.notificationsPath == null)) {
          return false;
        }

        xml.openTag(tag);
        generateHref(xml, ui.notificationsPath);
        xml.closeTag(tag);

        return true;
      }

      // Not known - try higher
      return super.generatePropertyValue(tag, intf, allProp);
    } catch (Throwable t) {
      throw new WebdavException(t);
    }
  }

  /* (non-Javadoc)
   * @see edu.bedework.cct.webdav.servlet.shared.WebdavNsNode#getPropertyNames()
   */
  @Override
  public Collection<PropertyTagEntry> getPropertyNames()throws WebdavException {
    Collection<PropertyTagEntry> res = new ArrayList<PropertyTagEntry>();

    res.addAll(super.getPropertyNames());
    res.addAll(propertyNames.values());

    return res;
  }
}
