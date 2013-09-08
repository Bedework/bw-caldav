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
package org.bedework.caldav.util.sharing;

import org.bedework.util.misc.ToString;
import org.bedework.util.xml.XmlEmit;
import org.bedework.util.xml.XmlEmit.NameSpace;
import org.bedework.util.xml.tagdefs.AppleServerTags;
import org.bedework.util.xml.tagdefs.CaldavDefs;
import org.bedework.util.xml.tagdefs.WebdavTags;

import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

/** Class to represent current sharing status.
 *
 * @author Mike Douglass douglm
 */
public class InviteType {
  private List<UserType> users;

  /**
   * @return list of UserType - never null
   */
  public List<UserType> getUsers() {
    if (users == null) {
      users = new ArrayList<UserType>();
    }

    return users;
  }

  /* ====================================================================
   *                   Convenience methods
   * ==================================================================== */

  /**
   * @param href
   * @return null or corresponding entry
   */
  public UserType finduser(final String href) {
    for (UserType u: getUsers()) {
      if (u.getHref().equals(href)) {
        return u;
      }
    }

    return null;
  }

  /**
   * @return XML version of notification
   * @throws Throwable
   */
  public String toXml() throws Throwable {
    StringWriter str = new StringWriter();
    XmlEmit xml = new XmlEmit();

    xml.addNs(new NameSpace(WebdavTags.namespace, "DAV"), false);
    xml.addNs(new NameSpace(CaldavDefs.caldavNamespace, "C"), false);
    xml.addNs(new NameSpace(AppleServerTags.appleCaldavNamespace, "CSS"), false);

    xml.startEmit(str);
    toXml(xml);

    return str.toString();
  }

  /**
   * @param xml
   * @throws Throwable
   */
  public void toXml(final XmlEmit xml) throws Throwable {
    xml.openTag(AppleServerTags.invite);

    for (UserType u: getUsers()) {
      u.toXml(xml);
    }
    xml.closeTag(AppleServerTags.invite);
  }

  /** Add our stuff to the StringBuffer
   *
   * @param ts
   */
  protected void toStringSegment(final ToString ts) {
    ts.append("users", getUsers());
  }

  @Override
  public String toString() {
    ToString ts = new ToString(this);

    toStringSegment(ts);

    return ts.toString();
  }
}
