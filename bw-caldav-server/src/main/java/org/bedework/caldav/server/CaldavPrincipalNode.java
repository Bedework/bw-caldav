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
import org.bedework.util.xml.tagdefs.CarddavTags;
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
 *   @author Mike Douglass   douglm  rpi.edu
 */
public class CaldavPrincipalNode extends WebdavPrincipalNode {
  private CalPrincipalInfo ui;

  /* for accessing calendars */
  private final SysIntf sysi;

  private final static HashMap<QName, PropertyTagEntry> propertyNames =
    new HashMap<>();

  static {
    addPropEntry(propertyNames, CaldavTags.calendarHomeSet);
    addPropEntry(propertyNames, CaldavTags.calendarUserAddressSet);
    addPropEntry(propertyNames, CaldavTags.scheduleInboxURL);
    addPropEntry(propertyNames, CaldavTags.scheduleOutboxURL);
    addPropEntry(propertyNames, CarddavTags.addressData);
  }

  /**
   * @param cdURI represents the URI
   * @param sysi system interface
   * @param ui principal information
   * @param isUser true if this is a user
   * @throws WebdavException
   */
  public CaldavPrincipalNode(final CaldavURI cdURI,
                             final SysIntf sysi,
                             final CalPrincipalInfo ui,
                             @SuppressWarnings(
                                     "UnusedParameters") final boolean isUser) throws WebdavException {
    super(sysi, sysi.getUrlHandler(), cdURI.getPath(),
          cdURI.getPrincipal(),
          cdURI.isCollection(), cdURI.getUri());
    this.sysi = sysi;
    this.ui = ui;

    if (ui == null) {
      this.ui = sysi.getCalPrincipalInfo(cdURI.getPrincipal());
    }
  }

  @Override
  public String getDisplayname() throws WebdavException {
    final String dn = ui.getDisplayname();

    if (dn == null) {
      return super.getDisplayname();
    }

    return dn;
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
                                       final boolean allProp) throws WebdavException {
    final XmlEmit xml = intf.getXmlEmit();

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

      if (tag.equals(CarddavTags.addressData)) {
        if (ui == null) {
          return false;
        }

        xml.property(tag, ui.getCardStr());

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

      // Not known - try higher
      return super.generatePropertyValue(tag, intf, allProp);
    } catch (final Throwable t) {
      throw new WebdavException(t);
    }
  }

  @Override
  public Collection<PropertyTagEntry> getPropertyNames()throws WebdavException {
    final Collection<PropertyTagEntry> res = new ArrayList<>();

    res.addAll(super.getPropertyNames());
    res.addAll(propertyNames.values());

    return res;
  }
}
