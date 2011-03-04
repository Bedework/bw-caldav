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

import edu.rpi.cct.webdav.servlet.shared.WebdavException;
import edu.rpi.cct.webdav.servlet.shared.WebdavNsIntf;
import edu.rpi.cct.webdav.servlet.shared.WebdavPrincipalNode;
import edu.rpi.sss.util.xml.XmlEmit;
import edu.rpi.sss.util.xml.tagdefs.CaldavDefs;
import edu.rpi.sss.util.xml.tagdefs.CaldavTags;
import edu.rpi.sss.util.xml.tagdefs.WebdavTags;

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
  private SysIntf sysi;

  private final static HashMap<QName, PropertyTagEntry> propertyNames =
    new HashMap<QName, PropertyTagEntry>();

  static {
    addPropEntry(propertyNames, CaldavTags.calendarHomeSet);
    addPropEntry(propertyNames, CaldavTags.calendarUserAddressSet);
    addPropEntry(propertyNames, CaldavTags.scheduleInboxURL);
    addPropEntry(propertyNames, CaldavTags.scheduleOutboxURL);
  }

  /**
   * @param cdURI
   * @param sysi
   * @param ui
   * @param isUser
   * @param debug
   * @throws WebdavException
   */
  public CaldavPrincipalNode(final CaldavURI cdURI, final SysIntf sysi,
                             final CalPrincipalInfo ui,
                             final boolean isUser,
                             final boolean debug) throws WebdavException {
    super(sysi.getUrlHandler(), cdURI.getPath(),
          cdURI.getPrincipal(),
          cdURI.isCollection(), cdURI.getUri(), debug);
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
   * @see edu.rpi.cct.webdav.servlet.shared.WebdavNsNode#knownProperty(edu.rpi.sss.util.xml.QName)
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
   * @see edu.rpi.cct.webdav.servlet.shared.WebdavNsNode#generatePropertyValue(edu.rpi.sss.util.xml.QName, edu.rpi.cct.webdav.servlet.shared.WebdavNsIntf, boolean)
   */
  @Override
  public boolean generatePropertyValue(final QName tag,
                                       final WebdavNsIntf intf,
                                       final boolean allProp) throws WebdavException {
    String ns = tag.getNamespaceURI();
    XmlEmit xml = intf.getXmlEmit();

    /* Deal with webdav properties */
    if (!ns.equals(CaldavDefs.caldavNamespace)) {
      // Not ours
      return super.generatePropertyValue(tag, intf, allProp);
    }

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

      // Not known - try higher
      return super.generatePropertyValue(tag, intf, allProp);
    } catch (Throwable t) {
      throw new WebdavException(t);
    }
  }

  /* (non-Javadoc)
   * @see edu.rpi.cct.webdav.servlet.shared.WebdavNsNode#getPropertyNames()
   */
  @Override
  public Collection<PropertyTagEntry> getPropertyNames()throws WebdavException {
    Collection<PropertyTagEntry> res = new ArrayList<PropertyTagEntry>();

    res.addAll(super.getPropertyNames());
    res.addAll(propertyNames.values());

    return res;
  }
}
